package com.hrflow.common;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.util.Vn;
import com.hrflow.security.Principals;

/**
 * Duyệt PTO/OT một cấp theo approval_route (company_profile Mục 4, permission_matrix Mục 3.2):
 * <ul>
 *   <li>Người duyệt chính duyệt; người dự phòng chỉ duyệt khi người chính vắng (approver_unavailability)
 *       hoặc không còn làm việc, và bắt buộc có lý do thay thế.</li>
 *   <li>Không bao giờ tự duyệt đơn của chính mình (kể cả là người dự phòng).</li>
 *   <li>Không có người duyệt hợp lệ -&gt; đơn giữ PENDING, không tự chuyển cho HR hay người khác.</li>
 * </ul>
 */
@Service
public class Approvals {

    public record Route(Long primaryId, Long backupId) {
    }

    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public Approvals(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    /** Tuyến duyệt hiệu lực của người gửi tại ngày {@code date}. */
    public Route routeOf(long requesterId, LocalDate date) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT primary_approver_id, backup_approver_id FROM approval_route
                WHERE requester_id = ? AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?)
                ORDER BY effective_from DESC, id DESC LIMIT 1
                """, requesterId, date, date);
        if (rows.isEmpty()) {
            return new Route(null, null);
        }
        Map<String, Object> r = rows.get(0);
        return new Route(asLong(r.get("primary_approver_id")), asLong(r.get("backup_approver_id")));
    }

    /** Người duyệt không khả dụng: đã khai báo vắng hôm nay hoặc hồ sơ đã nghỉ việc. */
    public boolean unavailable(Long employeeId) {
        if (employeeId == null) {
            return true;
        }
        Integer n = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM approver_unavailability WHERE employee_id = ? AND from_date <= ? AND to_date >= ?)
                     + (SELECT COUNT(*) FROM employee WHERE id = ? AND status = 'TERMINATED')
                """, Integer.class, employeeId, Vn.today(), Vn.today(), employeeId);
        return n != null && n > 0;
    }

    /** Người dùng hiện tại có quyền duyệt đơn này không (không ném lỗi) – dùng cho cờ canApprove. */
    public boolean canApprove(Long requesterId, Long primaryId, Long backupId) {
        Long me = Principals.current().employeeId();
        if (me == null || me.equals(requesterId)) {
            return false;
        }
        if (me.equals(primaryId)) {
            return true;
        }
        return me.equals(backupId) && unavailable(primaryId);
    }

    /**
     * Kiểm tra quyền duyệt và trả về true nếu người duyệt là người dự phòng.
     * Ném 403 khi tự duyệt/không thuộc tuyến, 422 khi dự phòng thiếu lý do.
     */
    public boolean authorize(String entityType, long entityId, Long requesterId, Long primaryId, Long backupId,
                             String backupReason) {
        Long me = Principals.current().employeeId();
        if (me == null) {
            throw new AccessDeniedException("Tài khoản không gắn hồ sơ nhân viên");
        }
        if (me.equals(requesterId)) {
            audit.denied("SELF_APPROVAL", entityType, entityId, "Người gửi tự duyệt/từ chối đơn của mình");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Không được tự duyệt đơn do mình tạo");
        }
        if (me.equals(primaryId)) {
            return false;
        }
        if (me.equals(backupId)) {
            if (!unavailable(primaryId)) {
                throw BusinessException.forbidden("PRIMARY_APPROVER_AVAILABLE",
                        "Người duyệt chính đang khả dụng; người dự phòng chưa được duyệt thay");
            }
            if (backupReason == null || backupReason.isBlank()) {
                throw BusinessException.unprocessable("BACKUP_REASON_REQUIRED",
                        "Duyệt thay người duyệt chính phải ghi lý do");
            }
            return true;
        }
        audit.denied("APPROVE_OUT_OF_ROUTE", entityType, entityId, "Không thuộc tuyến duyệt");
        throw new AccessDeniedException("Không thuộc tuyến duyệt của đơn");
    }

    private static Long asLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }
}
