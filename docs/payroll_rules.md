# Quy tắc tính lương (Payroll Rules)

Tài liệu chốt công thức cho engine lương (FR-07 đến FR-11, BR-01 đến BR-04). Viết **trước khi code** và dùng làm nguồn cho unit test. Mọi con số pháp lý là **tham số cấu hình** theo ngày hiệu lực, không hard-code.

> **Cần xác minh trước khi nộp:** các giá trị pháp lý ở Mục 2 là giá trị mẫu tại thời điểm viết (tháng 10/2026). Hãy đối chiếu văn bản chính thức (Luật Thuế TNCN 2025, Nghị quyết 110/2025/UBTVQH15, quy định về mức lương cơ sở, lương tối thiểu vùng, đóng BHXH). Các nguồn đọc được còn khác nhau về mốc áp dụng biểu thuế 5 bậc (01/01/2026 hay 01/07/2026), nên cần lưu `effective_from` để mô hình hóa đúng.

## 1. Nguyên tắc chung

1. Tiền là **số nguyên VND**. Tính trung gian bằng kiểu thập phân chính xác (decimal/BigDecimal), **không dùng float**.
2. Làm tròn **half-up về đồng** ở từng dòng tiền cuối cùng (lương ngày công, tiền OT, từng khoản bảo hiểm, thuế). Không làm tròn đơn giá trung gian.
3. Tham số pháp luật tra theo **ngày hiệu lực** của kỳ lương (BR-04): kỳ lương dùng bộ tham số có `effective_from` ≤ ngày cuối kỳ và mới nhất.
4. Dữ liệu kỳ lương đã `Locked` là bất biến (BR-01). Sai sót xử lý bằng adjustment (Mục 4.9).
5. Tính lương cả kỳ chạy trong **một transaction**, lỗi giữa chừng thì rollback toàn bộ (NFR-12).

## 2. Tham số cấu hình

### 2.1 Tham số hệ thống (bảng `system_params`, có `effective_from`)

| Khóa | Ý nghĩa | Giá trị mẫu | Ghi chú |
|---|---|---|---|
| `standard_working_days` | Ngày công chuẩn của kỳ | 22 | Nhóm chốt: cố định 22 hay tính theo lịch tháng (xem Mục 8) |
| `standard_hours_per_day` | Giờ chuẩn mỗi ngày | 8 | |
| `ot_rate_weekday` | Hệ số OT ngày thường | 1.5 | BR-03 |
| `ot_rate_weekend` | Hệ số OT ngày nghỉ hằng tuần | 2.0 | |
| `ot_rate_holiday` | Hệ số OT ngày lễ | 3.0 | |
| `pit_personal_deduction` | Giảm trừ bản thân/tháng | 15,500,000 | Nghị quyết 110/2025/UBTVQH15, áp dụng từ kỳ thuế 2026 |
| `pit_dependent_deduction` | Giảm trừ mỗi người phụ thuộc/tháng | 6,200,000 | cùng nguồn |
| `base_salary_ref` (lương cơ sở) | Dùng tính trần BHXH, BHYT | 2,340,000 | Cần xác minh mức hiện hành |
| `regional_min_wage` | Lương tối thiểu vùng của đơn vị | 5,310,000 (vùng I) | Cần xác minh; dùng tính trần BHTN |
| `insurance_cap_multiplier` | Bội số trần đóng bảo hiểm | 20 | |
| `late_penalty_enabled` | Có trừ lương đi trễ/về sớm | false | Mặc định chỉ ghi nhận phút, không trừ tiền |

### 2.2 Tỷ lệ bảo hiểm phần người lao động (bảng `insurance_rates`)

| Loại | Tỷ lệ | Trần mức lương đóng |
|---|---|---|
| BHXH | 8% | 20 × lương cơ sở |
| BHYT | 1.5% | 20 × lương cơ sở |
| BHTN | 1% | 20 × lương tối thiểu vùng |

Phần doanh nghiệp đóng (không trừ vào lương nhân viên) có thể lưu để báo cáo nhưng ngoài phạm vi MVP.

### 2.3 Biểu thuế TNCN lũy tiến từng phần (bảng `tax_brackets`)

**Bộ A, biểu 5 bậc (mẫu, đối chiếu Điều 9 Luật Thuế TNCN 2025):**

| Bậc | Thu nhập tính thuế/tháng (VND) | Thuế suất |
|---|---|---|
| 1 | đến 10,000,000 | 5% |
| 2 | trên 10,000,000 đến 30,000,000 | 10% |
| 3 | trên 30,000,000 đến 60,000,000 | 20% |
| 4 | trên 60,000,000 đến 100,000,000 | 30% |
| 5 | trên 100,000,000 | 35% |

**Bộ B, biểu 7 bậc cũ (giữ làm bản có `effective_to` để test hồi quy và kỳ lương cũ):**

| Bậc | Thu nhập tính thuế/tháng (VND) | Thuế suất |
|---|---|---|
| 1 | đến 5,000,000 | 5% |
| 2 | trên 5,000,000 đến 10,000,000 | 10% |
| 3 | trên 10,000,000 đến 18,000,000 | 15% |
| 4 | trên 18,000,000 đến 32,000,000 | 20% |
| 5 | trên 32,000,000 đến 52,000,000 | 25% |
| 6 | trên 52,000,000 đến 80,000,000 | 30% |
| 7 | trên 80,000,000 | 35% |

Việc hệ thống tồn tại **hai bộ biểu thuế theo ngày hiệu lực** chính là minh chứng cho BR-04 (đổi luật không sửa code).

### 2.4 Hợp đồng và nhân viên (đầu vào tính lương)

| Trường | Dùng để |
|---|---|
| `base_salary` | Lương cơ bản tháng |
| `insurance_salary` | Mức lương đóng bảo hiểm (mặc định = `base_salary`) |
| `dependents_count` | Số người phụ thuộc đã đăng ký giảm trừ |
| Phụ cấp (`allowances`) | Các khoản cộng thêm theo nhân viên/phòng ban (ăn trưa, xăng xe, điện thoại, chức vụ) |
| Khấu trừ khác | Các khoản trừ tùy chỉnh (tạm ứng, phạt...) |

## 3. Kỳ lương và trạng thái (BR-01)

```
Draft ──calculate──▶ Calculated ──approve──▶ Approved ──lock──▶ Locked
   ▲                     │ (tính lại)
   └─────────────────────┘
```

| Trạng thái | Cho phép | Không cho phép |
|---|---|---|
| Draft | Sửa công, đơn từ, cấu hình; chạy preview | |
| Calculated | Tính lại; xem và rà soát bảng lương | |
| Approved | Chỉ khóa | Sửa số liệu |
| Locked | Chỉ xem, tải phiếu lương | **Mọi chỉnh sửa/xóa** (trả 409) |

Sinh phiếu lương PDF và gửi thông báo diễn ra **sau khi** kỳ chuyển sang `Locked` (SR-12.1).

## 4. Công thức

### 4.1 Đơn giá

```
đơn_giá_ngày = base_salary / standard_working_days
đơn_giá_giờ  = đơn_giá_ngày / standard_hours_per_day
```

### 4.2 Ngày công thực tế

```
ngày_công_thực_tế = số ngày đi làm + số ngày nghỉ phép có lương đã duyệt (+ nửa ngày nếu có)
```

Ngày nghỉ không lương không được tính. Giới hạn: `ngày_công_thực_tế ≤ standard_working_days`.

### 4.3 Lương theo ngày công

```
lương_ngày_công = round( đơn_giá_ngày × ngày_công_thực_tế )
```

### 4.4 Tiền OT (chỉ tính đơn đã duyệt trong kỳ)

```
tiền_OT = round( Σ ( giờ_OT × đơn_giá_giờ × hệ_số_OT(loại ngày) ) )
```

`loại ngày` ∈ {thường, cuối tuần, lễ} lấy từ lịch OT đã công bố.

### 4.5 Phụ cấp

```
tổng_phụ_cấp = Σ phụ cấp đang hiệu lực trong kỳ của nhân viên
```

### 4.6 Gross

```
Gross = lương_ngày_công + tiền_OT + tổng_phụ_cấp
```

(Khớp SR-08.1.)

### 4.7 Bảo hiểm người lao động

```
mức_đóng_BHXH_BHYT = min(insurance_salary, 20 × lương_cơ_sở)
mức_đóng_BHTN      = min(insurance_salary, 20 × lương_tối_thiểu_vùng)

BHXH = round(mức_đóng_BHXH_BHYT × 8%)
BHYT = round(mức_đóng_BHXH_BHYT × 1.5%)
BHTN = round(mức_đóng_BHTN × 1%)
tổng_bảo_hiểm = BHXH + BHYT + BHTN
```

Bảo hiểm tính trên **mức lương đóng**, không phải Gross của tháng.

### 4.8 Thuế TNCN

```
giảm_trừ_gia_cảnh = pit_personal_deduction + dependents_count × pit_dependent_deduction
thu_nhập_tính_thuế = max(0, thu_nhập_chịu_thuế − tổng_bảo_hiểm − giảm_trừ_gia_cảnh)
thuế_TNCN = round( Σ ( phần thu nhập rơi vào bậc × thuế suất bậc ) )
```

`thu_nhập_chịu_thuế` ở MVP = Gross (xem Mục 8 về phần OT/ăn trưa được miễn thuế). Thuế tính lũy tiến **từng phần**: mỗi phần thu nhập chịu thuế suất của bậc đó.

### 4.9 Net

```
Net = Gross − tổng_bảo_hiểm − thuế_TNCN − khấu_trừ_khác  (± adjustment của kỳ)
```

> **Lưu ý SRS:** BR-02 hiện ghi `... + Phụ cấp` nhưng SR-08.1 đã đưa phụ cấp vào Gross, nếu cộng nữa sẽ bị tính hai lần. Tài liệu này theo hướng phụ cấp chỉ nằm trong Gross. Cần sửa BR-02 trong SRS cho khớp.

### 4.10 Tính theo tỷ lệ khi vào/nghỉ giữa tháng (SR-11.1)

Chỉ đếm ngày công từ ngày vào làm đến cuối kỳ (hoặc từ đầu kỳ đến ngày nghỉ việc). Công thức 4.3 giữ nguyên, `ngày_công_thực_tế` tự phản ánh tỷ lệ. Phụ cấp cố định có thể cũng tính tỷ lệ theo cấu hình từng loại.

### 4.11 Điều chỉnh hồi tố (SR-11.2)

1. Không sửa dữ liệu kỳ `Locked`.
2. Tạo bản ghi `adjustment` có `reference_period_id`, số tiền (+/−), lý do, người tạo.
3. Bản ghi tự động cộng/trừ vào kỳ lương **tháng kế tiếp** và hiển thị riêng trên phiếu lương.
4. Mọi adjustment ghi audit log.

### 4.12 Nghỉ việc (SR-03.2)

Kỳ lương cuối: tính tỷ lệ theo ngày công đến ngày nghỉ, cộng trợ cấp thôi việc (nếu có theo hợp đồng/quy định), chốt sổ BHXH. Tài khoản ESS deactivate, giữ nguyên dữ liệu lịch sử (SR-03.3).

## 5. Ví dụ tính chi tiết

**Dữ liệu vào (tháng bất kỳ, áp dụng bộ A):**

| Mục | Giá trị |
|---|---|
| Lương cơ bản = mức lương đóng bảo hiểm | 35,000,000 |
| Ngày công chuẩn / thực tế | 22 / 21 |
| OT ngày thường | 8 giờ |
| Phụ cấp (xăng xe + điện thoại) | 1,500,000 |
| Người phụ thuộc | 0 |
| Khấu trừ khác | 0 |

**Tính:**

| Bước | Công thức | Kết quả (VND) |
|---|---|---|
| Đơn giá ngày | 35,000,000 / 22 | 1,590,909.0909… |
| Đơn giá giờ | 1,590,909.0909… / 8 | 198,863.6363… |
| Lương ngày công | 1,590,909.0909… × 21 | **33,409,091** |
| Tiền OT | 198,863.6363… × 8 × 1.5 | **2,386,364** |
| Phụ cấp | | **1,500,000** |
| **Gross** | 33,409,091 + 2,386,364 + 1,500,000 | **37,295,455** |
| BHXH | 35,000,000 × 8% (dưới trần 46,800,000) | 2,800,000 |
| BHYT | 35,000,000 × 1.5% | 525,000 |
| BHTN | 35,000,000 × 1% (dưới trần 106,200,000) | 350,000 |
| Tổng bảo hiểm | | **3,675,000** |
| Giảm trừ gia cảnh | 15,500,000 + 0 × 6,200,000 | 15,500,000 |
| Thu nhập tính thuế | 37,295,455 − 3,675,000 − 15,500,000 | **18,120,455** |
| Thuế bậc 1 | 10,000,000 × 5% | 500,000 |
| Thuế bậc 2 | (18,120,455 − 10,000,000) × 10% | 812,045.5 |
| **Thuế TNCN** | 500,000 + 812,045.5, làm tròn half-up | **1,312,046** |
| **Net** | 37,295,455 − 3,675,000 − 1,312,046 | **32,308,409** |

(Giá trị trần và giảm trừ lấy từ Mục 2; nếu bạn đổi tham số mẫu thì tính lại ví dụ.)

## 6. Bộ test case gợi ý cho unit test

| Mã | Tình huống | Đầu vào chính | Kết quả mong đợi |
|---|---|---|---|
| TC-PAY-01 | Ví dụ ở Mục 5 | như Mục 5 | Gross 37,295,455 · BH 3,675,000 · Thuế 1,312,046 · Net 32,308,409 |
| TC-PAY-02 | Proration | base 22,000,000, chuẩn 22, thực tế 11 | lương ngày công 11,000,000 |
| TC-PAY-03 | OT cuối tuần | base 35,000,000, 4 giờ, hệ số 2.0 | 1,590,909 |
| TC-PAY-04 | OT ngày lễ | base 35,000,000, 8 giờ, hệ số 3.0 | 4,772,727 |
| TC-PAY-05 | Trần BHXH/BHYT | lương đóng 60,000,000 | BHXH 3,744,000 · BHYT 702,000 · BHTN 600,000 |
| TC-PAY-06 | Thu nhập tính thuế ≤ 0 | Gross thấp hơn tổng giảm trừ | thuế = 0, Net = Gross − bảo hiểm |
| TC-PAY-07 | Biên bậc thuế | thu nhập tính thuế 10,000,000 | 500,000 |
| TC-PAY-08 | Qua bậc 2 | thu nhập tính thuế 30,000,000 | 500,000 + 2,000,000 = 2,500,000 |
| TC-PAY-09 | Có người phụ thuộc | 1 người phụ thuộc | giảm trừ 21,700,000 |
| TC-PAY-10 | Đổi biểu thuế theo ngày hiệu lực | cùng thu nhập, hai kỳ thuộc hai bộ biểu | kết quả thuế khác nhau, đúng từng bộ |
| TC-PAY-11 | Nghỉ không lương | 2 ngày unpaid | ngày công thực tế giảm 2 |
| TC-PAY-12 | Kỳ Locked | sửa công/lương của kỳ Locked | 409, dữ liệu không đổi |
| TC-PAY-13 | Adjustment | thiếu 1 ngày công kỳ trước, base 35,000,000 | +1,590,909 vào kỳ sau, kỳ cũ không đổi |
| TC-PAY-14 | Chạy lại tính lương | tính 2 lần cùng đầu vào | kết quả giống nhau (idempotent) |
| TC-PAY-15 | Rollback | lỗi giữa lúc tính 500 nhân viên | không có bản ghi lương nào được lưu |
| TC-PAY-16 | Hiệu năng | 500 nhân viên | hoàn tất ≤ 5 phút (SR-08.2) |

## 7. Liên hệ với SRS

| Nội dung | Yêu cầu |
|---|---|
| Gross, batch, preview | SR-08.1, SR-08.2, SR-08.3 |
| Bảo hiểm, thuế, cấu hình động | SR-09.1, SR-09.2, SR-09.3, BR-04 |
| OT và phụ cấp | SR-10.1, SR-10.2, BR-03 |
| Proration, adjustment | SR-11.1, SR-11.2 |
| Kỳ lương và khóa | BR-01 |
| Công thức Net | BR-02 (cần sửa như ghi chú ở 4.9) |

## 8. Các điểm nhóm cần chốt và ghi vào SRS

1. **Ngày công chuẩn:** cố định 22 hay tính theo lịch tháng (trừ thứ Bảy/Chủ nhật/lễ)?
2. **Phụ cấp chịu thuế hay không:** theo quy định có khoản miễn thuế (ví dụ tiền ăn giữa ca trong hạn mức). MVP có thể coi toàn bộ chịu thuế và ghi rõ giả định này.
3. **Tiền OT miễn thuế:** theo quy định, phần chênh lệch so với tiền lương giờ bình thường được miễn thuế TNCN. MVP có thể bỏ qua và ghi chú là giới hạn đã biết, hoặc làm ở tính năng mở rộng.
4. **Trừ lương đi trễ/về sớm:** mặc định không trừ tiền (chỉ ghi nhận), bật bằng `late_penalty_enabled`.
5. **Làm tròn:** xác nhận half-up về đồng như Mục 1.
6. **Mức lương đóng bảo hiểm** có luôn bằng lương cơ bản không, hay nhập riêng theo hợp đồng.
