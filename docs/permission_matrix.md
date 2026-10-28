# Ma trận quyền nghiệp vụ HRFlow

Tài liệu này trả lời câu hỏi **ai được phép thực hiện từng nghiệp vụ** tại HNQ Media. Ma trận được xây dựng từ [`company_profile.md`](./company_profile.md), [`erd_database_schema.md`](./uml/erd/erd_database_schema.md), [`database-outline.md`](./database-outline.md) và các migration trong [`backend/src/main/resources/db/migration/`](../backend/src/main/resources/db/migration/).

> `database_design.md` không tồn tại trong workspace tại thời điểm đối chiếu. `database-outline.md` được dùng làm tài liệu thiết kế thay thế; các điểm không khớp với ERD/migration được ghi ở Mục 6.

## 1. Nguyên tắc phân quyền

- Hệ thống có 4 role kỹ thuật: `Employee`, `Manager`, `HR`, `Admin` (`V2__org_and_accounts.sql`). Chức danh Director, Operations Manager, Finance Manager, HR Lead, Sales Manager và các Lead không phải role mới; chúng được biểu diễn bằng `employee_type`, `manager_id` và grant cụ thể.
- `Employee` chỉ truy cập dữ liệu của mình. `Manager` truy cập dữ liệu của mình và nhân viên có `manager_id` trỏ trực tiếp đến mình. `HR` truy cập dữ liệu nhân sự toàn công ty theo nhiệm vụ. `Admin` quản trị tài khoản, cấu hình, quyền và audit; không mặc nhiên xem lương cá nhân.
- `employee_id` phải lấy từ token/session và quan hệ quản lý trong DB, không tin giá trị client gửi.
- Không ai được tự duyệt đơn do mình tạo. PTO/OT dùng một cấp duyệt; người dự phòng phải được cấu hình trước và quyết định phải ghi `approval_log`.
- Lương, thông tin định danh và tài khoản ngân hàng chỉ trả về đúng phạm vi quyền. Kỳ lương `LOCKED` chỉ đọc; sai sót dùng `payroll_adjustment`.
- `audit_log` và `payroll_audit` là append-only; không có API sửa/xóa và phải chặn `UPDATE/DELETE` ở DB.

Ký hiệu: `✓` = được thực hiện trong phạm vi; `own` = dữ liệu của mình; `team` = nhân viên cấp dưới trực tiếp; `R` = chỉ đọc; `—` = không được phép.

## 2. Role hệ thống và chức danh nghiệp vụ

| Role hệ thống | Người dùng áp dụng tại HNQ | Phạm vi chính |
|---|---|---|
| `Employee` | Nhân viên Content, Design & Video, Digital Marketing, Account, Sales và nhân sự khác | Hồ sơ được phép của mình, chấm công, tạo PTO/OT, xem công và phiếu lương của mình |
| `Manager` | Các Lead, Sales Manager, Operations Manager, Finance Manager và người được bổ nhiệm quản lý trực tiếp | Duyệt đơn của cấp dưới, xem công và dữ liệu vận hành không nhạy cảm của team |
| `HR` | HR Executive/HR Lead | Hồ sơ, hợp đồng, công, phép, OT, dữ liệu đầu vào và xử lý lương theo quyền được cấp |
| `Admin` | Người quản trị kỹ thuật được ủy quyền | Tài khoản, role, cấu hình, audit và quyền lương được cấp |

Director là `Manager` ở tuyến cao nhất; HR Lead duyệt đơn của Director. Finance Manager xác nhận tiền đã nhận và đối soát; Sales Manager xác nhận sở hữu doanh thu. Hai chức danh này không tự có thêm role hoặc quyền payroll nếu chưa được cấp.

## 3. Ma trận quyền theo nghiệp vụ

### 3.1. Tài khoản và hồ sơ nhân sự

| Nghiệp vụ | Employee | Manager | HR | Admin | Dữ liệu/migration |
|---|---:|---:|---:|---:|---|
| Đăng nhập, đăng xuất, đổi mật khẩu, xem tài khoản mình | own | own | own | own | `app_user` – V2 |
| Tạo hồ sơ và tài khoản onboarding | — | — | ✓ | — | `employee`, `app_user` – V2 |
| Xem danh sách nhân viên | — | team, trường cơ bản | ✓ | ✓ | `employee`, `department` – V2 |
| Xem hồ sơ chi tiết | own | own/team, không lương và dữ liệu nhạy cảm | ✓ | ✓ theo quyền | `employee` – V2 |
| Cập nhật hồ sơ cá nhân được phép | own, không trường nhạy cảm | — | ✓, audit | — | `employee`, `audit_log` – V2/V12 |
| Cập nhật hồ sơ nhân viên | — | — | ✓, audit | — | `employee`, `audit_log` – V2/V12 |
| Cho nghỉ việc và lưu lý do | — | — | ✓ | — | `termination_record` – V3 |
| Xem/tạo phòng ban | R | R | ✓ | ✓ | `department` – V2 |
| Xem hợp đồng của mình | own | — | ✓ | R | `contract` – V3 |
| Tạo/sửa/gia hạn hợp đồng | — | — | ✓ | R | `contract` – V3 |
| Quản lý phụ cấp/khấu trừ | R own | — | ✓ | R | V4 |
| Quản lý tài khoản, khóa/mở, role | — | — | — | ✓ | `app_user.role`, `is_active` – V2 |

### 3.2. Chấm công, nghỉ phép và OT

| Nghiệp vụ | Employee | Manager | HR | Admin | Dữ liệu/migration |
|---|---:|---:|---:|---:|---|
| Check-in/check-out | own | own | own | — | `attendance` – V6 |
| Xem bảng công | own | own/team | ✓ | R | V6 |
| Xác minh/chỉnh công | — | team, được ủy quyền | ✓ | — | `decided_by`, `confirmed_by` – V6 |
| Tạo/hủy PTO khi còn `PENDING` | own | own | own | — | `leave_request` – V7 |
| Xem PTO và số dư phép | own | own/team | ✓ | R | V7 |
| Duyệt/từ chối PTO | — | team | HR Lead nếu không có manager | — | V7/V8 |
| Tạo và công bố lịch OT | — | team | ✓ | — | `overtime_schedule` – V8 |
| Xem lịch OT đã công bố | đúng đối tượng | đúng team | ✓ | R | V8 |
| Đăng ký OT | own | own | own | — | `overtime_registration` – V8 |
| Duyệt/từ chối đăng ký OT | — | team | HR Lead nếu không có manager | — | V8 |
| Xem lịch sử phê duyệt | own | team | ✓ | R | `approval_log` – V8 |
| Cấp QR chấm công | — | — | ✓ | — | Chưa có bảng/token riêng |

Tuyến duyệt mặc định: nhân viên Content/Design/Digital/Account → Lead; Sales → Sales Manager; HR → HR Lead; các Lead/Sales Manager/Operations Manager/Finance Manager/HR Lead → Director; Director → HR Lead.

Tuyến dự phòng phải khớp đúng quy chế HNQ: Operations Manager thay Lead các phòng nghiệp vụ; Director thay Sales Manager hoặc HR Lead; HR Executive do HR Lead chỉ định trước thay HR Lead khi duyệt đơn của Director. Đơn của Sales Manager, Operations Manager và Finance Manager không có tuyến thay thế mặc định; HR Lead chỉ tiếp nhận/chuyển xử lý, không tự duyệt thay Director. Finance Manager chỉ thay Director khi được Director chỉ định trước để duyệt đơn của HR Lead; HR Executive chỉ thay HR Lead khi được HR Lead chỉ định trước để duyệt đơn của Director.

### 3.3. Sales, doanh thu, KPI và hoa hồng

| Nghiệp vụ | Sales Executive | Sales Manager | Finance Manager | HR | Admin | Dữ liệu/migration |
|---|---:|---:|---:|---:|---:|---|
| Xem/chỉnh doanh thu do mình phụ trách | own | team | R xác nhận tiền | — | — | `revenue_record` – V9 |
| Nhập khoản thu/chứng từ | own, nháp | ✓ kiểm tra | ✓ xác nhận đã thu | — | — | V9 |
| Xác nhận doanh thu đủ điều kiện | — | ✓ | ✓ | — | — | `verified_by` – V9 |
| Xác định Sales sở hữu, loại trùng/hoàn tiền | — | ✓ | ✓ phối hợp | — | — | V9 chưa đủ trường |
| Tính/xem KPI, hoa hồng | own kết quả | ✓ team | R đối soát | ✓ đưa khoản xác nhận vào lương | — | `commission_rate`, `kpi_result` – V9 |
| Phê duyệt dữ liệu chuyển sang lương | — | ✓ | ✓ | ✓ tiếp nhận | — | V9/V5 |

Chỉ doanh thu dịch vụ đã nghiệm thu và thực nhận, sau chiết khấu, không gồm VAT/chi hộ quảng cáo/hoàn tiền, mới được chuyển sang KPI và hoa hồng. Không tự động chia hoa hồng cho hai Sales.

### 3.4. Payroll và phiếu lương

| Nghiệp vụ | Employee | Manager | HR | Admin | Điều kiện |
|---|---:|---:|---:|---:|---|
| Tạo kỳ lương | — | — | `PREPARE` | `PREPARE` nếu được cấp | Kỳ mới `DRAFT` |
| Chuẩn bị dữ liệu/tính thử | — | — | `PREPARE` | `PREPARE` nếu được cấp | Dữ liệu đã xác nhận |
| Xem kỳ và bảng chi tiết toàn kỳ | — | — | `VIEW` | `VIEW` nếu được cấp | Đọc `payroll_permission` |
| Phê duyệt bảng lương | — | — | `APPROVE` | `APPROVE` nếu được cấp | `CALCULATED` → `APPROVED` |
| Khóa kỳ lương | — | — | `LOCK` | `LOCK` nếu được cấp | `APPROVED` → `LOCKED` |
| Tạo adjustment cho kỳ khóa | — | — | ✓ | — | Có kỳ gốc, lý do, chứng từ, người duyệt |
| Xem/tải phiếu lương | own | own | ✓ theo nhiệm vụ | — mặc định | `payslip` – V11 |
| Cấu hình thuế, BH, OT, tham số | — | — | R/đề xuất | ✓ | V4; có effective date và audit |

`V15__payroll_execution.sql` chỉ cấp mẫu: HR `VIEW/PREPARE`, Admin `VIEW/APPROVE/LOCK`. Đây là dữ liệu demo; quyền thật phải cấp theo người được ủy quyền. Role không tự động cấp quyền lương.

### 3.5. Thông báo, báo cáo và audit

| Nghiệp vụ | Employee | Manager | HR | Admin | Dữ liệu/migration |
|---|---:|---:|---:|---:|---|
| Xem/đánh dấu thông báo | own | own | own | own | `notification` – V12 |
| Cấu hình nhận email | own | own | own | own | V12; cấu trúc preference chưa rõ |
| Dashboard nhân sự/lương | — | — | ✓ | ✓ theo phạm vi | Tổng hợp nhiều bảng |
| Xuất báo cáo | — | team nếu bật | ✓ | ✓ | Lọc trường theo role |
| Xem audit log | — | — | R | R | `audit_log` – V12 |
| Sửa/xóa audit log hoặc payroll audit | — | — | — | — | Trigger append-only – V15 |

## 4. Quy tắc kiểm soát bắt buộc

1. Thiếu/hết token trả `401`; đúng role nhưng ngoài phạm vi dữ liệu trả `404` hoặc `403` theo một quy ước thống nhất, không làm lộ bản ghi.
2. Không nhận diện quyền từ body/query/path nếu khác user hoặc manager scope trong token và DB.
3. Người gửi không được approve/reject chính đơn của mình, kể cả có role HR/Manager/Admin.
4. Manager không xem lương team; Admin không mặc định xem phiếu lương; serializer phải loại trường nhạy cảm.
5. Cập nhật hồ sơ, hợp đồng, lương và chính sách phải ghi actor, thời điểm, bảng, bản ghi, old/new value và lý do khi có.
6. Kỳ lương chỉ đi `DRAFT → CALCULATED → APPROVED → LOCKED`; `LOCKED` không update/delete.
7. Công, phép, OT, doanh thu Sales, phụ cấp và thưởng phải có nguồn xác nhận trước khi vào lương.

## 5. Ca kiểm thử phân quyền tối thiểu

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| PM-01 | API không có token | `401` |
| PM-02 | Employee xem danh sách hoặc lương người khác | `403`/`404`, không lộ dữ liệu |
| PM-03 | Manager xem ngoài team hoặc duyệt ngoài team | `403`/`404` |
| PM-04 | Employee tự approve PTO/OT | `403` |
| PM-05 | Manager gọi payroll không có grant | `403` |
| PM-06 | Sửa kỳ/phiếu lương `LOCKED` | `409`, DB không đổi |
| PM-07 | HR sửa hồ sơ | Thành công và có audit old/new |
| PM-08 | Update/delete `audit_log` hoặc `payroll_audit` | Bị chặn |
| PM-09 | Sales ghi doanh thu chưa nghiệm thu/chưa thu | Không vào KPI/lương |
| PM-10 | Hai Sales nhận cùng khoản thu | Chỉ một owner sau xác nhận |
| PM-11 | Gửi `employee_id` người khác khi tạo PTO/OT | Bỏ qua hoặc `422` |
| PM-12 | Employee đã terminate đăng nhập | `401`/`403`; lịch sử vẫn còn |

## 6. Đối chiếu ERD, database-outline và migration

| Nhóm | ERD/database-outline | Migration | Kết luận |
|---|---|---|---|
| Account/role | `users`, `roles`, `user_roles` | V2 dùng `app_user.role` với check `EMPLOYEE/MANAGER/HR/ADMIN` | Ma trận phải bám `app_user.role`; chưa triển khai RBAC nhiều role như ERD |
| Nhân sự/phòng ban | `employee`, `department`, `manager_id` | Có ở V2; status chỉ `ACTIVE/TERMINATED` | Khớp lõi; Onboarding/On Leave cần biểu diễn qua account/contract/leave hoặc bổ sung enum |
| Hợp đồng/nghỉ việc | `contract`, termination | V3 có cả hai | Khớp; bảo toàn lịch sử và audit khi sửa |
| Chấm công | `attendance` | V6 có check-in/out, trễ/sớm, người quyết định/xác nhận | Thiếu liên kết khóa công trực tiếp với kỳ lương và lịch sử chỉnh đầy đủ |
| PTO | `leave_request`, `leave_balance`, approval | V7/V8 có bảng và người quyết định | Khớp phần lớn; service phải kiểm tra số dư, ngày lễ, tuyến duyệt, tự duyệt |
| OT | schedule, registration | V8 có lịch/đăng ký/phê duyệt | Chưa đủ giờ thực tế, ca đêm/vượt giờ và xác nhận hoàn thành |
| Sales/KPI | revenue, commission, KPI | V9 có 3 bảng | Thiếu mã hợp đồng, ngày nghiệm thu/thực nhận, hoàn tiền và phân bổ |
| Payroll | period, payroll line, payslip, adjustment | V5, V10, V11, V15 có bảng chính và state guard | Khớp; thao tác phải đọc `payroll_permission`, không chỉ role |
| Audit/approval | `approval_logs`, `audit_logs` | V8, V12, V15 có log và trigger | Khớp ở DB; xác nhận service ghi đủ actor/reason/old-new |
| Notification/config | notification, system config | V4, V12 | Khớp lõi; preference email chưa có cấu trúc rõ |

V14 là dữ liệu mẫu kỹ thuật, không phải danh sách 40 nhân sự chính thức của HNQ Media; không dùng dữ liệu mẫu để suy ra quyền production.

## 7. Việc cần làm tiếp theo

- Backend phải kiểm tra đồng thời role, scope quản lý, trạng thái tài khoản và `payroll_permission`.
- Cập nhật OpenAPI/controller/service theo ma trận này, nhất là Sales/KPI, adjustment và quyền `APPROVE/LOCK`.
- Dùng đúng tên bảng migration (`app_user`, `employee`, `leave_request`, `overtime_registration`, `audit_log`...), không dùng tên khái quát của ERD khi viết query.
- Bổ sung migration hoặc cơ chế nghiệp vụ cho OT thực tế, đối soát Sales, tuyến dự phòng và thông báo preference trước khi tự động hóa hoàn chỉnh.
