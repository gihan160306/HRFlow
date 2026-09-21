# Permission Matrix (RBAC)

Dựa trên Mục 2.3 + UR-19 + SR-19.1 + SR-21.2 của SRS.

## Ma trận quyền (Permission Matrix)

| Chức năng / Endpoint nhóm | Employee | Manager | HR | Admin | Ghi chú Row-Level |
| :--- | :---: | :---: | :---: | :---: | :--- |
| Đăng nhập / Refresh token | ✓ | ✓ | ✓ | ✓ | - |
| Xem / sửa thông tin cá nhân của mình | ✓ | ✓ | ✓ | ✓ | Chỉ dữ liệu của chính mình |
| Xem danh sách nhân viên | - | ✓ (team) | ✓ | ✓ | Manager chỉ thấy nhân viên mình quản lý |
| Tạo / cập nhật / terminate nhân viên | - | - | ✓ | ✓ | - |
| Quản lý hợp đồng (CRUD + renew) | - | - | ✓ | ✓ | - |
| Chấm công (check-in/out) của mình | ✓ | ✓ | ✓ | ✓ | - |
| Xem bảng công team / toàn công ty | - | ✓ (team) | ✓ | ✓ | - |
| Tạo đơn PTO / OT | ✓ | ✓ | ✓ | ✓ | - |
| Duyệt / từ chối PTO & OT | - | ✓ (team) | ✓* | ✓* | HR duyệt khi nhân viên không có Manager |
| Xem lịch sử duyệt của mình | ✓ | ✓ | ✓ | ✓ | - |
| Tạo / công bố lịch OT | - | ✓ | ✓ | ✓ | - |
| Chạy tính lương (batch) | - | - | ✓ | ✓ | - |
| Xem trước (preview) & chốt kỳ lương | - | - | ✓ | ✓ | - |
| Khóa kỳ lương (Locked) | - | - | - | ✓ | Không cho sửa sau khi Locked |
| Xem / tải phiếu lương của mình | ✓ | ✓ | ✓ | ✓ | Employee chỉ thấy của chính mình |
| Xem phiếu lương nhân viên khác | - | - | ✓ | ✓ | - |
| Dashboard & báo cáo tổng hợp | - | ✓ (team) | ✓ | ✓ | - |
| Xuất báo cáo Excel/PDF | - | - | ✓ | ✓ | - |
| Xem Audit Log | - | - | - | ✓ | - |
| Quản lý role / gán quyền | - | - | - | ✓ | - |
| Cấu hình hệ thống (OT rate, thuế, BHXH…) | - | - | - | ✓ | - |
| Quản lý tài khoản người dùng | - | - | - | ✓ | - |

## Quy tắc Row-Level Access Control (SR-21.2)

- **Employee**: Chỉ đọc/ghi dữ liệu gắn với `employee_id` của chính mình.
- **Manager**: Chỉ thấy nhân viên có `manager_id` = mình + dữ liệu của chính mình.
- **HR & Admin**: Toàn quyền trên dữ liệu nhân sự (trừ một số hành động hệ thống chỉ Admin).