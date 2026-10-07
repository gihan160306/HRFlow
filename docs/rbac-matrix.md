# Ma trận phân quyền (Permission Matrix)

Đáp ứng SR-19.1, SR-19.2, SR-21.2, NFR-01, NFR-02, BR-06. Mọi endpoint nhạy cảm phải kiểm tra **vai trò** (middleware JWT) và **phạm vi dữ liệu** (kiểm soát cấp dòng) trước khi xử lý.

## 1. Vai trò và phạm vi dữ liệu

| Vai trò | Phạm vi dữ liệu |
|---|---|
| **Employee** | Chỉ dữ liệu của chính mình (`employee_id` lấy từ token, không nhận từ client để lọc quyền) |
| **Manager** | Dữ liệu của chính mình + nhân viên có `manager_id` = mình (quản lý trực tiếp) |
| **HR** | Toàn bộ nhân viên, hợp đồng, công, lương, báo cáo |
| **Admin** | Cấu hình hệ thống, tài khoản, phân quyền, nhật ký kiểm toán; đọc được dữ liệu nhân sự và lương theo các quyền nêu dưới |

Ký hiệu: `✔` toàn quyền trong phạm vi hàng · `own` chỉ của mình · `team` nhân viên thuộc quyền quản lý · `—` không được phép · `R/C/U/D` đọc/tạo/sửa/xóa.

## 2. Ma trận theo chức năng

### Xác thực và tài khoản

| Chức năng | Endpoint | Employee | Manager | HR | Admin |
|---|---|---|---|---|---|
| Đăng nhập, đăng xuất | `POST /auth/login`, `/auth/logout` | ✔ | ✔ | ✔ | ✔ |
| Xem thông tin tài khoản mình | `GET /auth/me` | ✔ | ✔ | ✔ | ✔ |
| Đổi mật khẩu của mình | `POST /auth/change-password` | ✔ | ✔ | ✔ | ✔ |
| Quản lý tài khoản, vai trò, khóa/mở | `/users` | — | — | — | ✔ |

### Hồ sơ nhân sự (FR-01, FR-02, FR-03)

| Chức năng | Endpoint | Employee | Manager | HR | Admin |
|---|---|---|---|---|---|
| Xem danh sách nhân viên | `GET /employees` | — | team (chỉ trường cơ bản) | ✔ | ✔ |
| Xem hồ sơ chi tiết | `GET /employees/{id}` | own | own; team (không có lương, CCCD, bank) | ✔ | ✔ |
| Tạo hồ sơ + tài khoản (onboarding) | `POST /employees` | — | — | ✔ | ✔ |
| Cập nhật hồ sơ | `PUT /employees/{id}` | — | — | ✔ (ghi audit log) | ✔ (ghi audit log) |
| Cho nghỉ việc | `POST /employees/{id}/terminate` | — | — | ✔ | — |
| Phòng ban | `GET/POST /departments` | R | R | ✔ | ✔ |
| Hợp đồng | `GET /contracts`, `POST/PUT /contracts` | R own | — | ✔ | R |
| Gia hạn hợp đồng | `POST /contracts/{id}/renew` | — | — | ✔ | — |
| Phụ cấp/khấu trừ của nhân viên | `/employees/{id}/allowances`, `/allowance-types` | R own | — | ✔ | R |

### Chấm công, nghỉ phép, tăng ca (FR-04, FR-05, FR-06, FR-13)

| Chức năng | Endpoint | Employee | Manager | HR | Admin |
|---|---|---|---|---|---|
| Check-in / check-out | `POST /attendance/check-in`, `/check-out` | own | own | own | — |
| Xem bảng công | `GET /attendance` | own | own + team | ✔ | R |
| Tạo đơn PTO | `POST /pto-requests` | own | own | own | — |
| Xem đơn PTO | `GET /pto-requests` | own | own + team | ✔ | R |
| Duyệt / từ chối PTO | `PATCH /pto-requests/{id}` | — | team | nhân viên không có Manager | — |
| Hủy đơn của mình (khi còn Pending) | `PATCH /pto-requests/{id}` (cancel) | own | own | own | — |
| Số ngày phép còn lại | `GET /employees/{id}/leave-balance` | own | own + team | ✔ | R |
| Tạo và công bố lịch OT | `POST /ot-schedules` | — | team | ✔ | — |
| Xem lịch OT đã công bố | `GET /ot-schedules` | ✔ (đúng đối tượng) | ✔ | ✔ | R |
| Đăng ký OT | `POST /ot-registrations` | own | own | own | — |
| Duyệt / từ chối đăng ký OT | `PATCH /ot-registrations/{id}` | — | team | nhân viên không có Manager | — |
| Lịch sử duyệt (approval log) | `GET /approval-logs` | own | team | ✔ | R |
| QR chấm công động | `GET /attendance/qr-token` | — | — | ✔ | — |

### Lương và phiếu lương (FR-07 đến FR-11)

| Chức năng | Endpoint | Employee | Manager | HR | Admin |
|---|---|---|---|---|---|
| Tạo kỳ lương | `POST /payroll/periods` | — | — | ✔ | ✔ |
| Xem kỳ lương, bảng lương cả kỳ | `GET /payroll/periods`, `/{id}/items` | — | — | ✔ | ✔ |
| Tính lương / xem trước (preview) | `POST /payroll/periods/{id}/calculate` | — | — | ✔ | ✔ |
| Duyệt bảng lương | `POST /payroll/periods/{id}/approve` | — | — | ✔ | ✔ |
| Khóa kỳ lương (Locked) | `POST /payroll/periods/{id}/lock` | — | — | ✔ | ✔ |
| Tạo bản ghi điều chỉnh (adjustment) | `POST /payroll/adjustments` | — | — | ✔ | — |
| Xem phiếu lương | `GET /employees/{id}/payslips`, `/payslips/{id}`, `/pdf` | own | own | ✔ | — |
| Cấu hình thuế, bảo hiểm, hệ số OT, tham số | `/config/*` | — | — | R | ✔ |

> Manager **không** xem lương của nhân viên trong team (BR-06 "Need-to-know"). Admin không xem phiếu lương cá nhân.

### Thông báo, báo cáo, kiểm toán (FR-12, FR-14 đến FR-17)

| Chức năng | Endpoint | Employee | Manager | HR | Admin |
|---|---|---|---|---|---|
| Thông báo của mình | `GET /notifications`, `PATCH /{id}/read` | own | own | own | own |
| Tùy chọn nhận email | `GET/PUT /notifications/preferences` | own | own | own | own |
| Dashboard tổng quan | `GET /dashboard/summary` | — | — | ✔ | ✔ |
| Xuất báo cáo Excel/PDF | `GET /reports/export` | — | — (báo cáo công team nếu triển khai) | ✔ | ✔ |
| Nhật ký kiểm toán | `GET /audit-logs` | — | — | R | R |
| Sửa / xóa nhật ký kiểm toán | (không có endpoint) | — | — | — | — |

## 3. Quy tắc bắt buộc

1. **Không tin tham số từ client để xác định quyền.** `employee_id` của Employee lấy từ token; nếu path chứa `{id}` khác `employee_id` của token thì xử lý theo quy tắc 3.
2. **401** khi thiếu/hết hạn token; **403** khi vai trò không được dùng endpoint.
3. **404** (không phải 403) khi vai trò hợp lệ nhưng bản ghi nằm ngoài phạm vi dữ liệu, để không lộ sự tồn tại của dữ liệu (nhóm có thể chọn 403 nhưng phải thống nhất và ghi vào OpenAPI).
4. **Không tự duyệt:** không ai được duyệt đơn PTO/OT do chính mình gửi (HR gửi đơn thì Admin hoặc HR khác duyệt; thống nhất cách xử lý với nhóm).
5. **Kỳ lương Locked** là chỉ đọc với mọi vai trò, kể cả Admin; sai sót xử lý bằng adjustment (BR-01).
6. **Audit log append-only** (NFR-03): không có endpoint sửa/xóa; ở tầng DB nên thu hồi quyền `UPDATE/DELETE` của user ứng dụng trên bảng này.
7. Dữ liệu nhạy cảm (lương, CCCD, tài khoản ngân hàng) chỉ trả về cho vai trò có quyền; Resource/Serializer riêng cho từng vai trò, không dùng chung một serializer rồi ẩn trường ở frontend.
8. Mọi `UPDATE/DELETE` trên bảng lương, hợp đồng, thông tin cá nhân tự động ghi audit log (SR-20.1) gồm `user_id, action, table, old_value, new_value, timestamp`.

## 4. Test phân quyền tối thiểu (đưa vào TEST-PLAN)

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| PM-01 | Gọi API không có token | 401 |
| PM-02 | Employee gọi `GET /employees` | 403 |
| PM-03 | Employee A gọi `GET /employees/{B}/payslips` | 404 (hoặc 403 theo quyết định nhóm), không lộ dữ liệu |
| PM-04 | Employee A gọi `GET /payslips/{id của B}/pdf` | 404/403 |
| PM-05 | Manager xem hồ sơ nhân viên ngoài team | 404/403 |
| PM-06 | Manager xem lương nhân viên trong team | 403 |
| PM-07 | Manager duyệt đơn PTO của nhân viên ngoài team | 404/403 |
| PM-08 | Employee tự duyệt đơn của mình (`PATCH` approve) | 403 |
| PM-09 | Manager gọi `POST /payroll/periods/{id}/calculate` | 403 |
| PM-10 | HR sửa dữ liệu kỳ lương Locked | 409 |
| PM-11 | Mọi vai trò gọi `PUT/DELETE /audit-logs/{id}` | 404/405, bảng log không đổi |
| PM-12 | HR sửa hồ sơ nhân viên | 200 và có 1 bản ghi audit log (old/new) |
| PM-13 | Nhân viên đã terminate đăng nhập | 401/403 (tài khoản deactivate), dữ liệu lịch sử vẫn còn |
| PM-14 | Employee gửi `employee_id` người khác trong body để tạo PTO | Bị bỏ qua, đơn gắn với chính mình hoặc 422 |
