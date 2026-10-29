# Payroll code: bản triển khai đầu tiên

Quy tắc nghiệp vụ đầy đủ ở [payroll_rules.md](payroll_rules.md). Backend hiện nhận **bảng đầu vào HR đã đối soát**, tính tiền bằng `BigDecimal`, lưu phiếu và dòng lương cùng snapshot nguồn trong một giao dịch. Hệ thống không tự tạo số tiền giả khi thiếu dữ liệu. Màn hình kỳ lương đọc dữ liệu thật và có nút nhập tệp JSON cho toàn kỳ.

## Quy trình

1. HR chuẩn bị một tệp JSON có `employees` chứa **mọi nhân viên có hợp đồng chồng với kỳ**, kể cả người đã nghỉ trong kỳ. Mỗi khoản cần nguồn đối soát; tiền ăn/điện thoại miễn thuế, OT và bảo hiểm phải được HR xác nhận trước khi nhập.
2. Tài khoản có grant `PREPARE` gửi `POST /api/v1/payroll/calculate` với `periodId` và `employees`, hoặc nhập tệp JSON trên màn hình kỳ lương. Có thể tính lại ở `DRAFT`/`CALCULATED`; dữ liệu cũ được thay trong cùng giao dịch.
3. Khi cần sửa nguồn trước duyệt, tài khoản `PREPARE` trả kỳ `CALCULATED` về `DRAFT` kèm lý do; bản preview bị xóa và hành động được ghi audit. Tài khoản khác có grant `APPROVE` duyệt kỳ `CALCULATED`. `APPROVE` kiểm tra lại tổng Gross, BH, Net và snapshot. Người tính không tự duyệt.
4. Tài khoản có grant `LOCK` khóa kỳ `APPROVED`. Đến ngày chi trả ghi trên kỳ, gọi `POST /api/v1/payroll/periods/{id}/publish` để phát hành phiếu. Nhân viên chỉ đọc phiếu `ISSUED` của chính mình trong kỳ `LOCKED`.

Quyền xem, chuẩn bị, duyệt và khóa nằm trong bảng `payroll_permission`; role `ADMIN` hay `MANAGER` không tự có quyền lương. Migration V15 cấp grant tường minh cho hai tài khoản demo HR và Giám đốc/Admin. Môi trường khác phải cấp grant theo ủy quyền thực tế.

## Mẫu đầu vào một nhân viên

Tệp dùng trên màn hình có cấu trúc dưới đây. Gửi trực tiếp qua API cần thêm `periodId`. Khi một kỳ có nhiều nhân viên, phải thêm đủ phần tử vào `employees`.

```json
{
  "periodId": 3,
  "employees": [{
    "employeeId": 4,
    "sourceReference": "HR-2026-11-approved-input-v1",
    "salarySegments": [{
      "sourceReference": "HD-2024-004 / attendance-confirmed",
      "monthlySalary": 18000000,
      "standardHours": 168,
      "paidHours": 168
    }],
    "allowances": [
      {"code": "MEAL", "monthlyAmount": 730000, "standardHours": 168, "eligibleHours": 168, "taxable": false},
      {"code": "PARKING", "monthlyAmount": 200000, "standardHours": 168, "eligibleHours": 168, "taxable": true}
    ],
    "overtime": [],
    "nightWork": [],
    "commission": 0,
    "bonus": 0,
    "otherTaxableIncome": 0,
    "insurance": {
      "bhxhBase": 18000000, "bhytBase": 18000000, "bhtnBase": 18000000,
      "bhxhParticipating": true, "bhytParticipating": true, "bhtnParticipating": true
    },
    "tax": {
      "method": "PROGRESSIVE", "validDependents": 0,
      "otherLegalRelief": 0, "validShortTermCommitment": false
    },
    "legalDeductions": 0,
    "advanceRecovery": 0,
    "reimbursements": 0,
    "netAdjustment": 0
  }]
}
```

Tiền trong API là VND; số cuối làm tròn `HALF_UP` tới 1 VND. `salarySegments[].standardHours` là giờ chuẩn **cả tháng** cho mức lương đó, kể cả khi vào việc giữa tháng. `paidHours` gồm công và nghỉ hưởng lương đã đối soát, không gồm OT. Phụ cấp có `eligibleHours` riêng; OT dùng giờ thực tế và đơn giá đã xác nhận. `netSalary` trong API là **thực chi** sau thu hồi tạm ứng và cộng hoàn chi.

## Phần chưa tự động hóa

Bản này chưa tự gom và xác nhận giờ công/nghỉ, giờ OT thực tế, doanh thu Sales, căn cứ từng quỹ bảo hiểm, tình trạng phụ thuộc hoặc chứng từ miễn thuế từ các bảng nguồn. JSON là đầu vào HR xác nhận; `sourceReference` và snapshot giữ dấu truy vết nhưng chưa chứng minh tự động rằng chứng từ đúng. Các bảng nguồn riêng chưa bị khóa theo kỳ. Chưa tự tạo PDF/email, chưa có màn hình nhập từng khoản, chưa hỗ trợ kỳ trước 07/2026 hoặc kỳ chi trả ngoài năm 2026. Không sử dụng dữ liệu chưa đối soát để duyệt kỳ.
