# Danh sách bảng đề xuất (theo mức độ ưu tiên)

| STT | Tên bảng | Mục đích chính | Ghi chú quan trọng từ SRS |
|---|---|---|---|
| 1 | users | Tài khoản đăng nhập | Liên kết 1-1 với employees |
| 2 | roles | 4 role cố định | Employee, Manager, HR, Admin |
| 3 | user_roles | Gán role cho user | Nhiều role nếu cần (thường 1) |
| 4 | departments | Phòng ban | Dùng cho báo cáo & phân quyền Manager |
| 5 | employees | Hồ sơ nhân viên | Core của hệ thống |
| 6 | contracts | Hợp đồng lao động | Cảnh báo hết hạn (UR-02) |
| 7 | attendances | Chấm công hàng ngày | check_in, check_out, late_minutes |
| 8 | pto_requests | Đơn nghỉ phép | Trạng thái: Pending → Approved/Rejected |
| 9 | ot_schedules | Lịch tăng ca do HR/Manager công bố | Mở rộng |
| 10 | ot_requests | Đơn đăng ký OT của nhân viên | Mở rộng |
| 11 | payroll_periods | Kỳ lương | Draft → Calculated → Approved → Locked (BR-01) |
| 12 | payrolls | Bảng lương chi tiết từng nhân viên | Gross, BHXH, thuế, Net… |
| 13 | payslips | Phiếu lương PDF | Liên kết với payroll |
| 14 | approval_logs | Lịch sử duyệt đơn | request_id, approver_id, action, reason |
| 15 | audit_logs | Nhật ký kiểm toán | Append-only, không cho sửa/xóa (SR-20.2) |
| 16 | system_configs | Tham số hệ thống | Hệ số OT, tỷ lệ BHXH, biểu thuế… (không hard-code) |

## Quan hệ chính (tóm tắt)

- `users` 1 ── 1 `employees`
- `employees` N ── 1 `departments`
- `employees` 1 ── N `contracts`
- `employees` 1 ── N `attendances`
- `employees` 1 ── N `pto_requests`
- `employees` 1 ── N `ot_requests`
- `employees` 1 ── N `payrolls`
- `payroll_periods` 1 ── N `payrolls`
- `payrolls` 1 ── 1 `payslips`
- `pto_requests` / `ot_requests` ── `approval_logs`
- Mọi bảng nhạy cảm ── `audit_logs`

## Cột quan trọng cần ghi nhớ sớm

- **employees**: 
  `id`, `user_id`, `employee_code`, `full_name`, `department_id`, `manager_id`, `base_salary`, `tax_code`, `bank_account`, `status` (Active/Terminated), `hire_date`, `terminate_date`

- **pto_requests**: 
  `id`, `employee_id`, `start_date`, `end_date`, `leave_type`, `reason`, `status`, `approved_by`, `approved_at`

- **payroll_periods**: 
  `id`, `period_name`, `start_date`, `end_date`, `status` (Draft/Calculated/Approved/Locked)

- **audit_logs** (bắt buộc theo SR-20.2): 
  `id`, `user_id`, `action`, `table_name`, `record_id`, `old_value` (JSON), `new_value` (JSON), `timestamp`  
  *(Không có cột updated_at / deleted_at → append-only)*