# HRFlow — Bộ tài liệu nghiệp vụ đã chuẩn hóa

Ba file trong thư mục này là các bản đề xuất chốt làm baseline để bắt đầu SRS:

1. `company_profile.md` — công ty, cơ cấu, chức danh, lịch làm việc, nghiệp vụ, mức lương/phụ cấp và tuyến PTO/OT.
2. `payroll_rules.md` — công thức tính lương, OT, phụ cấp, hoa hồng, bảo hiểm/thuế, làm tròn, trạng thái kỳ và thẩm quyền payroll.
3. `permission_matrix.md` — 4 role nền tảng, scope dữ liệu, tuyến duyệt PTO/OT, grant payroll, audit và ca kiểm thử phân quyền.

## Thứ tự ưu tiên

- Company Profile là nguồn cho cơ cấu tổ chức, chức danh, lịch làm việc và mức/chính sách công ty.
- Payroll Rules là nguồn cho cách tính Gross/Net, điều kiện dữ liệu, khấu trừ, làm tròn và trạng thái kỳ lương.
- Permission Matrix là nguồn cho quyền truy cập, phạm vi dữ liệu, quyền phê duyệt và kiểm soát xung đột lợi ích.
- Nếu có xung đột giữa các file, không tự đoán: ghi thành issue và sửa nguồn gốc trước khi chốt SRS.
- Các mục pháp lý `OPEN-01` đến `OPEN-06` trong Payroll Rules là nội dung cần xác minh trước khi trả lương thực tế; trong đồ án phải được biểu diễn như tham số/cờ cần xác nhận, không hard-code giả định.

## Lưu ý trước khi chốt chính thức

Các file này chuẩn hóa baseline nghiệp vụ. Chúng chưa thay thế việc đối chiếu với `database_design.md`, code/migration và SRS hiện có. Khi bắt đầu tài liệu kỹ thuật, nếu phát hiện khác biệt, ưu tiên ghi nhận và sửa có kiểm soát thay vì âm thầm thay đổi quy tắc.
