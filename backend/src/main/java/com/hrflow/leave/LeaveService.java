package com.hrflow.leave;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.*;
import java.util.*;
@Service
public class LeaveService {
 private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
 private static final Set<String> TYPES=Set.of("ANNUAL","MARRIAGE","FUNERAL","UNPAID","SOCIAL_INSURANCE");
 private static final List<LeaveStatus> ACTIVE=List.of(LeaveStatus.PENDING,LeaveStatus.APPROVED);
 private final LeaveRepository repo; private final JdbcTemplate jdbc;
 public LeaveService(LeaveRepository repo,JdbcTemplate jdbc){this.repo=repo;this.jdbc=jdbc;}
 @Transactional public LeaveResponse createDraft(String email,LeaveCreateRequest r){
  Long eid=employeeId(email); if(r.endDate().isBefore(r.startDate()))throw err(HttpStatus.BAD_REQUEST,"endDate không được trước startDate.");
  String type=r.leaveType().trim().toUpperCase(Locale.ROOT); if(!TYPES.contains(type))throw err(HttpStatus.BAD_REQUEST,"Loại nghỉ không hợp lệ.");
  String half=r.halfDaySession()==null||r.halfDaySession().isBlank()?null:r.halfDaySession().trim().toUpperCase(Locale.ROOT);
  if(half!=null&&!Set.of("AM","PM").contains(half))throw err(HttpStatus.BAD_REQUEST,"halfDaySession chỉ nhận AM hoặc PM.");
  if(half!=null&&!r.startDate().equals(r.endDate()))throw err(HttpStatus.BAD_REQUEST,"Nghỉ nửa ngày phải có cùng startDate và endDate.");
  BigDecimal days=workingDays(r.startDate(),r.endDate(),half); if(days.signum()<=0)throw err(HttpStatus.BAD_REQUEST,"Khoảng nghỉ không có ngày làm việc.");
  if(repo.hasOverlap(eid,r.startDate(),r.endDate(),ACTIVE,null))throw err(HttpStatus.CONFLICT,"Khoảng nghỉ bị trùng với đơn đang chờ hoặc đã duyệt.");
  return LeaveResponse.from(repo.save(new LeaveRequest(eid,type,r.startDate(),r.endDate(),half,days,blankToNull(r.reason()))));
 }
 @Transactional public LeaveResponse submit(String email,Long id){Long eid=employeeId(email);LeaveRequest l=own(id,eid);if(l.getStatus()!=LeaveStatus.DRAFT)throw err(HttpStatus.CONFLICT,"Chỉ gửi được đơn DRAFT.");
  if(repo.hasOverlap(eid,l.getStartDate(),l.getEndDate(),ACTIVE,l.getId()))throw err(HttpStatus.CONFLICT,"Khoảng nghỉ bị trùng.");
  if("ANNUAL".equals(l.getLeaveType()))checkBalance(eid,l.getStartDate().getYear(),l.getDurationDays());
  l.submit(OffsetDateTime.now(ZONE));repo.save(l);log(l.getId(),userId(email),"SUBMIT",null);return LeaveResponse.from(l);}
 @Transactional public LeaveResponse cancel(String email,Long id){Long eid=employeeId(email);LeaveRequest l=own(id,eid);if(l.getStatus()!=LeaveStatus.DRAFT&&l.getStatus()!=LeaveStatus.PENDING)throw err(HttpStatus.CONFLICT,"Chỉ hủy nháp hoặc rút đơn PENDING.");l.cancel();repo.save(l);log(l.getId(),userId(email),"CANCEL",null);return LeaveResponse.from(l);}
 @Transactional public LeaveResponse action(String email,Set<String> roles,Long id,String action,String reason){LeaveRequest l=repo.findById(id).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Không tìm thấy đơn nghỉ."));String a=action==null?"":action.trim().toUpperCase(Locale.ROOT);if(a.equals("CANCEL"))return cancel(email,id);if(l.getStatus()!=LeaveStatus.PENDING)throw err(HttpStatus.CONFLICT,"Chỉ xử lý đơn PENDING.");authorize(email,roles,l.getEmployeeId());Long uid=userId(email);
  if(a.equals("REJECT")){String why=blankToNull(reason);if(why==null)throw err(HttpStatus.BAD_REQUEST,"Bắt buộc nhập lý do từ chối.");l.decide(LeaveStatus.REJECTED,uid,OffsetDateTime.now(ZONE),why);log(id,uid,"REJECT",why);}
  else if(a.equals("APPROVE")){if("ANNUAL".equals(l.getLeaveType())){int n=jdbc.update("update leave_balance set used_days=used_days+?, updated_at=now() where employee_id=? and leave_year=? and accrued_days-used_days>=?",l.getDurationDays(),l.getEmployeeId(),l.getStartDate().getYear(),l.getDurationDays());if(n!=1)throw err(HttpStatus.CONFLICT,"Không đủ số dư phép năm.");}l.decide(LeaveStatus.APPROVED,uid,OffsetDateTime.now(ZONE),null);log(id,uid,"APPROVE",null);}
  else throw err(HttpStatus.BAD_REQUEST,"action phải là APPROVE, REJECT hoặc CANCEL.");repo.save(l);return LeaveResponse.from(l);}
 @Transactional(readOnly=true) public List<LeaveResponse> mine(String email){return repo.findByEmployeeIdOrderByCreatedAtDesc(employeeId(email)).stream().map(LeaveResponse::from).toList();}
 private void authorize(String email,Set<String> roles,Long requesterId){Long manager=jdbc.query("select manager_id from employee where employee_id=?",rs->rs.next()?(Long)rs.getObject(1):null,requesterId);boolean hr=roles.contains("ROLE_HR")||roles.contains("HR"),mgr=roles.contains("ROLE_MANAGER")||roles.contains("MANAGER");if(manager==null){if(!hr)throw err(HttpStatus.FORBIDDEN,"Không có Manager trực tiếp; chỉ HR được duyệt.");return;}if(!mgr||!manager.equals(employeeId(email)))throw err(HttpStatus.FORBIDDEN,"Chỉ Manager trực tiếp được duyệt đơn này.");}
 private void checkBalance(Long eid,int year,BigDecimal days){BigDecimal remain=jdbc.query("select accrued_days-used_days from leave_balance where employee_id=? and leave_year=?",rs->rs.next()?rs.getBigDecimal(1):null,eid,year);if(remain==null)throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Chưa có số dư phép năm.");BigDecimal pending=jdbc.queryForObject("select coalesce(sum(duration_days),0) from leave_request where employee_id=? and leave_type='ANNUAL' and status='PENDING' and extract(year from start_date)=?",BigDecimal.class,eid,year);if(remain.subtract(pending==null?BigDecimal.ZERO:pending).compareTo(days)<0)throw err(HttpStatus.UNPROCESSABLE_ENTITY,"Số ngày nghỉ vượt số dư phép năm.");}
 private BigDecimal workingDays(LocalDate s,LocalDate e,String half){if(half!=null)return new BigDecimal("0.5");List<LocalDate> holidays=jdbc.query("select calendar_date from calendar_exception where calendar_date between ? and ?",(rs,n)->rs.getDate(1).toLocalDate(),Date.valueOf(s),Date.valueOf(e));Set<LocalDate> hs=Set.copyOf(holidays);int count=0;for(LocalDate d=s;!d.isAfter(e);d=d.plusDays(1)){DayOfWeek w=d.getDayOfWeek();if(w!=DayOfWeek.SATURDAY&&w!=DayOfWeek.SUNDAY&&!hs.contains(d))count++;}return BigDecimal.valueOf(count);}
 private Long employeeId(String email){Long id=jdbc.query("select employee_id from employee where lower(work_email)=lower(?)",rs->rs.next()?rs.getLong(1):null,email);if(id==null)throw err(HttpStatus.FORBIDDEN,"Tài khoản chưa liên kết hồ sơ nhân viên.");return id;}
 private Long userId(String email){Long id=jdbc.query("select user_id from app_user where lower(email)=lower(?)",rs->rs.next()?rs.getLong(1):null,email);if(id==null)throw err(HttpStatus.UNAUTHORIZED,"Không tìm thấy tài khoản trong app_user.");return id;}
 private LeaveRequest own(Long id,Long eid){return repo.findByIdAndEmployeeId(id,eid).orElseThrow(()->err(HttpStatus.NOT_FOUND,"Không tìm thấy đơn nghỉ của bạn."));}
 private void log(Long id,Long uid,String action,String reason){jdbc.update("insert into approval_log(leave_request_id,actor_user_id,action,reason) values(?,?,?,?)",id,uid,action,reason);}
 private static String blankToNull(String s){return s==null||s.isBlank()?null:s.trim();}
 private static ResponseStatusException err(HttpStatus s,String m){return new ResponseStatusException(s,m);}
}
