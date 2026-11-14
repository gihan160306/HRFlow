# Payroll code: bản triển khai backend (đồng bộ baseline v3.0)

Quy tắc nghiệp vụ đầy đủ ở [payroll_rules.md](payroll_rules.md); lược đồ ở [database_design.md](database_design.md); đường dẫn API ở [openapi-spec.yaml](openapi-spec.yaml).
Backend **không còn nhận bảng đầu vào JSON do client gửi** (endpoint `POST /payroll/calculate` đã bỏ). Mọi số liệu tính lương đọc từ CSDL.

## Luồng

1. `POST /payroll/periods` (grant `PREPARE`) tạo kỳ `DRAFT`; `payDate` mặc định ngày 05 tháng sau, lùi qua cuối tuần/lễ.
2. `GET /payroll/periods/{id}/precheck`: lỗi chặn (`BLOCKING`) và cảnh báo (`WARNING`) theo nhân viên.
3. `POST .../close-timesheet`: kỳ phải đã kết thúc và hết lỗi chặn (thiếu công, đơn nghỉ chờ duyệt, OT chưa xác nhận giờ thực tế, hồ sơ/hợp đồng thiếu) → ghi `timesheet_snapshot` vN (JSON bất biến) + `timesheet_closed`. Từ đây `PeriodGuard` chặn sửa nguồn của tháng (409 `PERIOD_FROZEN`).
4. `POST .../calculate`: đọc snapshot + tham số trong `payroll_parameter`/`tax_bracket` → `payroll_line` + `payroll_line_item` (số trước/sau làm tròn, nguồn, quy tắc) trong một giao dịch; chạy lại cho cùng kết quả. Dòng thiếu căn cứ (thuế/bảo hiểm/phân loại thuế chưa xác nhận, tham số `NEEDS_REVIEW`, thực chi âm...) có trạng thái `NEEDS_REVIEW`, không bị đổi âm thầm thành 0.
5. `POST .../reconcile` (grant `VIEW`, khác người tính): kiểm tra Gross/BH/Net/Payable khớp công thức và đủ dòng.
6. `POST .../conflict-resolution` (HR có `PREPARE`, Giám đốc không tự ghi nhận): ghi quyết định xử lý xung đột lợi ích (OPEN-05).
7. `POST .../approve` (grant `APPROVE`): khác người tính/chốt; đã đối soát; không còn `NEEDS_REVIEW`; kỳ có lương của Giám đốc/người duyệt mà chưa có quyết định → 409 `CONFLICT_OF_INTEREST`.
8. `POST .../lock` (grant `LOCK`): khóa kỳ, xếp `background_job GENERATE_PAYSLIPS`; PDF tạo **sau** commit khóa (retry khi lỗi, không đảo trạng thái kỳ). `POST .../mark-paid` ghi nhận chi trả (số chứng từ bắt buộc).
9. Sai sót sau khóa: `POST /payroll/adjustments` (kỳ gốc `LOCKED`, lý do, minh chứng, `sourceRef` duy nhất) → người khác có `APPROVE` duyệt → tự áp vào kỳ mở kế tiếp khi tính (GROSS qua phân loại thuế; TAX/INSURANCE/NET là điều chỉnh tiền thuần).

## Quyền

Grant `VIEW/PREPARE/APPROVE/LOCK` đọc từ `payroll_grant` (còn hiệu lực, chưa thu hồi, đúng kỳ nếu giới hạn kỳ). Tài khoản `ADMIN` không bao giờ có quyền lương, kể cả có bản ghi grant. Role/chức danh không suy ra grant. Mã lỗi: 401 chưa đăng nhập, 403 thiếu quyền/tự duyệt, 404 ngoài phạm vi, 409 sai trạng thái/kỳ đã chốt, 422 dữ liệu không hợp lệ.

## Công thức đã cài (PayrollEngine)

- Lương theo công `Σ L × H_paid / H_std` theo từng đoạn đổi lương/lịch; `H_std` là giờ chuẩn **cả tháng** theo lịch cá nhân (loại lễ hưởng lương, nghỉ tuần); phút tính chính xác.
- Phụ cấp `Σ A × H_eligible / H_std`, làm tròn một lần cho mỗi khoản; OT theo giờ **thực tế** xác nhận, tách đoạn 00:00/06:00/22:00 và ngày thường/nghỉ tuần/lễ, hệ số lấy từ tham số (đêm: `k + 0,3 + 0,2 × k_day`); phút trùng giờ làm theo lịch không trả hai lần.
- Hoa hồng/KPI chỉ cho Sales Executive từ doanh thu `CONFIRMED`; bảo hiểm theo hồ sơ đã xác nhận và trần từ tham số (`INSURANCE_REFERENCE_SALARY`, `BHXH_BHYT_CAP_MULTIPLIER`, `MIN_WAGE_MONTHLY` theo vùng của nhân viên); thuế theo `tax_profile` (lũy tiến/10%/không cư trú) và biểu `tax_bracket`; giảm trừ từ tham số.
- Không còn giá trị viết cứng (trần bảo hiểm, giảm trừ, `regional-min-wage`, dung sai đi trễ 15 phút) trong code hay `application.yml`.
- Kiểm thử đối soát P01–P08, B06, B08–B14, B22 ở `PayrollEngineTest`.

## Hạn chế đã biết

- PDF dùng font Helvetica chuẩn nên văn bản tiếng Việt được chuyển về không dấu.
- Chưa gửi email (SIM-03), chưa có job cộng dồn phép năm tự động (HR điều chỉnh quỹ phép), dashboard/báo cáo ở mức tối thiểu.
- Làm đêm thường (22:00–06:00 thuộc lịch làm thường) chưa áp dụng vì lịch HNQ chỉ có ca ban ngày.
