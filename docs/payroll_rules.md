# Quy tắc tính lương HNQ Media

| Thuộc tính | Giá trị |
|---|---|
| Phiên bản | 3.0 — bản chuẩn nghiệp vụ payroll làm nguồn cho các tài liệu liên quan |
| Phạm vi | HRFlow, chính sách HNQ từ 01/07/2026 |
| Căn cứ nội bộ | Hồ sơ Công ty và Quy chế vận hành HNQ Media (bản 2.1); các quyết định chính sách nêu trong tài liệu này |
| Đơn vị và thời gian | VND; Asia/Ho_Chi_Minh; tháng dương lịch |
| Chủ sở hữu nghiệp vụ | Bộ phận Nhân sự phối hợp bộ phận Tài chính; người có thẩm quyền của HNQ phê duyệt |
| Mục đích | Quy định chính sách và công thức payroll làm căn cứ cho quy trình, dữ liệu, API, phân quyền và kiểm thử |

Tài liệu này là nguồn nghiệp vụ gốc của HRFlow đối với cách xác định thu nhập, khấu trừ, thực nhận và kiểm soát kỳ lương tại HNQ Media. `company_profile.md` là nguồn gốc về cơ cấu công ty, chức danh, lịch làm việc, mức lương/phụ cấp và quy trình kinh doanh; nếu có khác biệt về công thức payroll, tài liệu này quy định cách tính nhưng không được âm thầm thay đổi mức/chính sách công ty. Tài liệu ưu tiên chính sách nội bộ đã nêu trong Hồ sơ công ty; các mức pháp luật thay đổi theo thời gian phải được quản lý bằng bộ tham số có ngày hiệu lực và nguồn chính thức. Đây là quy định cho doanh nghiệp mô phỏng trong phạm vi dự án; trước khi dùng để trả lương thực tế, bộ phận Nhân sự/Tài chính phải xác nhận các tham số pháp lý, địa bàn áp dụng và hồ sơ từng người.

## 1. Các quyết định thống nhất

1. Không cố định 22/26 công. Từng kỳ lưu lịch và giờ chuẩn riêng của từng nhân viên. 22 ngày chỉ là dữ liệu ví dụ.
2. Làm đủ công nhận đủ lương tháng có hiệu lực. Ngày lễ hưởng lương không làm giảm lương tháng và không cộng trùng công.
3. Ghi nhận phút trễ/sớm từ phút đầu, đối soát giải trình và giờ bù. Không phạt tiền đi trễ, không cắt lương để kỷ luật, không có dung sai tự động 15 phút.
4. OT trả theo thời gian thực tế đủ căn cứ, không lấy nguyên thời lượng lịch/đăng ký; tách ngày thường, nghỉ tuần, lễ và đêm.
5. Gross gồm lương theo công + OT + phụ cấp + hoa hồng + thưởng + khoản bổ sung hợp lệ, mỗi khoản một lần. Net không cộng lại phụ cấp.
6. Bảo hiểm có căn cứ riêng; Gross không tự trở thành mức đóng bảo hiểm. Thuế có phân loại miễn/không tính thuế theo từng khoản.
7. Khấu trừ và điều chỉnh phải có căn cứ, chứng từ, nguồn duy nhất và phê duyệt.
8. Làm tròn HALF_UP về đồng tại từng dòng tiền cuối cùng, cộng các dòng đã làm tròn ra tổng.
9. `DRAFT → CALCULATED → APPROVED → LOCKED`. Không tính lại kỳ Approved/Locked; sai sót sau Locked điều chỉnh tại kỳ mở tiếp theo.
10. HR chịu trách nhiệm tập hợp, kiểm tra đầu vào, tính và rà soát bảng lương; HR không mặc nhiên được phê duyệt hoặc khóa kỳ. Finance Manager đối soát khoản thu và căn cứ tài chính. Giám đốc được phân quyền `APPROVE` bảng lương và Finance Manager được phân quyền `LOCK` kỳ lương sau phê duyệt, theo Company Profile. Không suy quyền từ chức danh, role hoặc tuyến duyệt PTO/OT. Nếu thiếu người có quyền, kỳ lương dừng chờ xác nhận thẩm quyền. Người lập không tự duyệt bảng do mình lập; Admin không mặc nhiên được xem, duyệt hoặc khóa lương.

Thứ tự áp dụng: (1) pháp luật bắt buộc có hiệu lực và thỏa thuận hợp pháp có lợi hơn cho người lao động; (2) hợp đồng lao động/quyết định lương và quy chế đã được HNQ phê duyệt; (3) Hồ sơ công ty HNQ Media; (4) công thức chi tiết trong tài liệu này. Tài liệu kỹ thuật, dữ liệu demo, seed, SRS hoặc API không được tự ý thay đổi chính sách nghiệp vụ. Khi phát hiện mâu thuẫn, ghi nhận thành vấn đề cần phê duyệt; không tự chọn cách tính có lợi cho phần mềm.

### 1.1. Ranh giới thẩm quyền payroll (quy tắc bắt buộc)

| Công việc | Người chịu trách nhiệm nghiệp vụ | Quy tắc cấp quyền |
|---|---|---|
| Thu thập/kiểm tra hồ sơ, công, phép, OT và khoản thu nhập | HR phối hợp Manager/Finance theo nguồn dữ liệu | HR có quyền chuẩn bị trong phạm vi công việc; dữ liệu nguồn phải được người có trách nhiệm xác nhận |
| Tính và rà soát bảng lương | HR | Grant `PREPARE`; có thể xem dữ liệu lương cần thiết để làm nhiệm vụ |
| Đối soát khoản thu, doanh thu đủ điều kiện và căn cứ tài chính | Finance Manager; Sales Manager xác nhận Sales owner/KPI | Không đồng nghĩa với quyền duyệt hoặc khóa payroll |
| Phê duyệt bảng lương | Giám đốc (Director) | Grant `APPROVE` cấp riêng cho tài khoản Giám đốc; không suy từ role `Manager` |
| Khóa kỳ lương | Finance Manager | Grant `LOCK` riêng, thực hiện sau khi Giám đốc phê duyệt; không tự động có `APPROVE` |
| Quản trị kỹ thuật | Admin | Không tự có quyền đọc chi tiết lương, `APPROVE` hoặc `LOCK` |

**Tách biệt nhiệm vụ:** người tạo/tính bảng lương không được duyệt bảng đó. Chỉ tài khoản có grant hợp lệ mới được duyệt/khóa. Mức lương hoặc điều chỉnh lương của Giám đốc và trường hợp có xung đột phải được cấp có thẩm quyền chấp thuận riêng trước khi tính lương. Quy trình phê duyệt kỳ có lương của chính Giám đốc vẫn cần biện pháp xử lý xung đột được HNQ xác nhận; không tự phê duyệt phần lương của mình, không tự lập tuyến thay thế hoặc cơ chế duyệt từng phiếu.

### 1.2. Các thông tin nghiệp vụ lấy từ Company Profile

- Đơn vị: Công ty TNHH Truyền thông HNQ (HNQ Media), hoạt động tại TP.HCM; HRFlow chỉ quản lý nhân sự và payroll, không thay CRM, quản lý dự án hoặc kế toán.
- Quy mô mẫu: 40 người thuộc 7 phòng ban, gồm 34 nhân viên chính thức toàn thời gian, 3 bán thời gian, 2 thử việc và 1 thực tập sinh. Director dùng role Manager; Admin kỹ thuật không thuộc 40 người. Loại nhân sự không tự quyết định nghĩa vụ thuế/bảo hiểm; phải xét hợp đồng và điều kiện thực tế.
- Kỳ lương theo tháng dương lịch; chính sách nội bộ áp dụng từ 01/07/2026; tiền VND và múi giờ `Asia/Ho_Chi_Minh`.
- Full-time: thứ Hai–thứ Sáu, 08:30–12:00 và 13:00–17:30, 8 giờ/ngày. Part-time mẫu: thứ Hai–thứ Sáu, 08:30–12:30, 4 giờ/ngày. Lịch cá nhân có hiệu lực là nguồn xác định giờ chuẩn; không mặc định mọi tháng 22/26 ngày.
- Tuyến PTO/OT một cấp theo Company Profile. Không dùng tuyến duyệt PTO/OT để tự suy ra quyền phê duyệt payroll.
- Chỉ Sales Executive được giao sở hữu doanh thu mới áp dụng hoa hồng/KPI theo quy tắc Sales; Sales Manager xác nhận sở hữu/đủ điều kiện và Finance Manager xác nhận tiền thực nhận. HR dùng dữ liệu đã xác nhận, không tự suy doanh thu từ khoản chi hộ ngân sách quảng cáo.


## 2. Đầu vào và tham số theo hiệu lực

### 2.1. Dữ liệu bắt buộc

| Nhóm | Nội dung phải chốt |
|---|---|
| Nhân sự/hợp đồng | Mã, loại hợp đồng, ngày vào/nghỉ, khoảng hiệu lực, chức danh, lương tháng, lịch cá nhân |
| Công | Ca chuẩn, ngày lễ/nghỉ bù, phút làm thường, nghỉ có/không lương, điều chỉnh được xác nhận |
| OT | Lịch/đăng ký, sự đồng ý, giờ duyệt, giờ thực tế xác nhận, loại ngày, đoạn đêm, phần bổ sung |
| Thu nhập | Phụ cấp đủ điều kiện, doanh thu Sales đã đối soát, thưởng được duyệt, mã nguồn duy nhất |
| Bảo hiểm | Đối tượng từng quỹ, mức đóng tháng được HR xác nhận, báo tăng/giảm, ngoại lệ |
| Thuế | Cư trú, phương pháp khấu trừ, ngày chi trả, người phụ thuộc hợp lệ, căn cứ miễn/không tính thuế |
| Khấu trừ/adjustment | Loại, số tiền, kỳ gốc/kỳ thanh toán, chứng từ, người tạo và người duyệt |

Không suy ra nghĩa vụ thuế/bảo hiểm từ role hoặc `employee_type`. Không gán lương mẫu khi thiếu hợp đồng. Thiếu dữ liệu phải báo theo nhân viên/khoản mục để xử lý trước phê duyệt.

### 2.2. Bộ tham số HNQ

| Tham số | Giá trị | Hiệu lực/phạm vi |
|---|---:|---|
| Full-time | 8 giờ/ngày, thứ Hai–thứ Sáu | 08:30–12:00 và 13:00–17:30 |
| Part-time mẫu | 4 giờ/ngày, thứ Hai–thứ Sáu | 08:30–12:30, không trừ giờ trưa full-time |
| Ngày/giờ chuẩn tháng | Theo lịch cá nhân | Lưu snapshot, không dùng mặc định 22 |
| OT ban ngày | 1,5 / 2 / 3 | Ngày thường / nghỉ tuần / lễ |
| Phần trả thêm làm đêm | 0,3 | 22:00–06:00 |
| BHXH/BHYT/BHTN NLĐ | 8% / 1,5% / 1% | Từng đối tượng/quỹ, xem Mục 7 |
| Mức tham chiếu BHXH/BHYT | 2.530.000 | Từ 01/07/2026; trần 20 lần = 50.600.000 [L3] |
| Lương tối thiểu vùng | Vùng I: 5.310.000/tháng; 25.500/giờ | Từ 01/01/2026; phải tra theo địa bàn làm việc cụ thể [L4] |
| Trần căn cứ BHTN | 20 × mức lương tối thiểu tháng áp dụng | Không cố định theo tên thành phố; xác định theo địa bàn và thời điểm |
| Giảm trừ bản thân/người phụ thuộc | 15.500.000 / 6.200.000/tháng | Bộ thuế năm 2026 [L1] |
| Thuế cư trú lũy tiến | 5 bậc | Mục 8 [L2] |
| Hoa hồng/chỉ tiêu Sales đủ tháng | 2% / 100.000.000 | Doanh thu đủ điều kiện |
| Thưởng KPI đạt chỉ tiêu | 1.500.000/tháng | Không tự tăng khi vượt chỉ tiêu |

Mỗi tham số có mã, giá trị/đơn vị, phạm vi, `effective_from`, `effective_to`, văn bản/quyết định nguồn, người xác nhận và phiên bản. Không chồng khoảng hiệu lực; thiếu bộ phù hợp thì báo lỗi, không tự lấy bộ mới nhất. Không suy ra địa bàn lương tối thiểu vùng chỉ từ thông tin “TP.HCM”; hồ sơ công ty cần lưu địa chỉ nơi làm việc áp dụng và ánh xạ địa bàn theo phụ lục văn bản pháp luật hiện hành.

Lương, lịch, phụ cấp và OT tra tại ngày phát sinh, chia đoạn khi đổi giữa kỳ. Bảo hiểm tra theo tháng tham gia/hồ sơ. Thuế tra theo thời điểm chi trả và kỳ tính thuế tương ứng; lương tháng 12 trả tháng 1 không mặc định dùng bộ tháng 12. Snapshot lưu kỳ công, ngày chi trả và phiên bản tham số. Kỳ lịch sử trước 2026 dùng bộ lịch sử riêng; không áp ngược bộ 2026.

## 3. Ngày công và lương theo công

### 3.1. Xác định thời gian

- `H_std`: tổng giờ phải làm theo lịch cá nhân TOÀN THÁNG, loại lễ nghỉ hưởng lương và nghỉ tuần. Không rút mẫu số vì vào/nghỉ việc giữa tháng.
- `H_paid`: hợp các khoảng làm thường xác nhận, công tác/làm ngoài được xác nhận và nghỉ hưởng lương hợp lệ, trong thời gian hợp đồng có hiệu lực và lịch phải làm.
- Loại chồng lặp; một phút chỉ trả một lần. OT và giờ làm lễ không cộng vào `H_paid` khi mẫu số đã loại lễ.
- Tính bằng phút chính xác, `H = phút / 60`. Công tương đương để hiển thị: full-time `H_paid/8`, part-time `H_paid/4`; không làm tròn công rồi tính tiền.
- Nghỉ 0,5 ngày tương ứng 4 giờ full-time/2 giờ part-time theo ca được duyệt. Sáng 3,5 giờ và chiều 4,5 giờ không tự coi mỗi buổi là 4 giờ; đơn phải lưu thời lượng nghỉ thực tế.
- Đi trễ/sớm được đối soát giải trình, nghỉ có lương và giờ bù xác nhận. Giờ bù không tự thành OT, không đếm cùng phút ở công thường và OT.
- Thiếu check-in/out hoặc đơn Pending: `Needs Review` ở nghiệp vụ, không tự coi 0 công/nghỉ không lương. HR xác nhận phần được trả và phần cần xử lý, không tự giữ toàn bộ lương.

### 3.2. Công thức

```text
R_hour = L_month / H_std
Salary_attendance = ROUND_VND(L_month × H_paid / H_std)
0 ≤ H_paid ≤ H_std
```

Đủ công cả tháng nhận đủ `L_month`, kể cả tháng có lễ. Không cộng thêm lương lễ lần nữa vào lương tháng đã đủ; làm ngày lễ được OT riêng. Khi thay lương/lịch/chức danh hoặc thử việc sang chính thức giữa tháng:

```text
Salary_attendance = ROUND_VND(Σ_s L_month_s × H_paid_s / H_std_s)
```

`s` là đoạn hiệu lực không chồng lấn. `H_std_s` là giờ chuẩn TOÀN THÁNG theo lịch tương ứng, không chỉ giờ của đoạn. Vào/nghỉ giữa tháng chỉ có tử số thuộc thời gian hợp đồng. Cộng các đoạn chưa làm tròn, làm tròn một lần ở dòng lương theo công.

Thử việc: mức mô phỏng nội bộ HNQ là 85% lương chức danh tương ứng, chỉ áp dụng khi phù hợp hợp đồng/thỏa thuận và quy định bắt buộc hiện hành; không được thấp hơn mức tối thiểu pháp luật yêu cầu. Part-time: mức mô phỏng 5.000.000/tháng theo lịch riêng. Thực tập sinh đào tạo: trợ cấp mô phỏng 4.000.000/tháng theo thỏa thuận thực chất; nếu quan hệ thực tế là quan hệ lao động thì phải phân loại theo bản chất, không theo nhãn “thực tập”. Kiểm tra mức tối thiểu hợp pháp theo địa bàn, hình thức và thời giờ làm việc; không so máy móc lương tháng part-time với sàn tháng full-time.

Nếu `H_std = 0`, không chia cho 0 hoặc tự trả 0: HR xác định chế độ lương/lễ/thỏa thuận áp dụng và nhập kết quả có căn cứ trước duyệt.

## 4. OT và làm đêm

### 4.1. Điều kiện, đơn giá và giới hạn

Luồng chuẩn: công bố lịch → nhân viên đủ điều kiện đồng ý/đăng ký → duyệt một cấp → làm thực tế → xác nhận → tính lương. Duyệt 3 giờ nhưng thực tế 2 giờ thì trả 2 giờ. Phần vượt/ngoài lịch phải xác minh bổ sung; thiếu thủ tục duyệt trước không tự xóa nghĩa vụ trả lương thực tế đủ căn cứ.

`R_ot` là đơn giá giờ thực trả công việc đang làm theo Điều 55 Nghị định 145/2020/NĐ-CP [L5]: tiền lương thực trả thuộc căn cứ của tháng/tuần/ngày chia tổng giờ thực tế làm thường tương ứng, đã loại OT và thời gian nghỉ hưởng lương theo quy định. Khoản chức vụ/lương gắn trực tiếp công việc thuộc căn cứ phải được đưa vào, không luôn chỉ lấy `base_salary`. Loại tiền OT, phần trả thêm đêm, thưởng, ăn ca, hỗ trợ điện thoại/đi lại và khoản không liên quan công việc theo căn cứ áp dụng. Khoản phụ cấp chức vụ chỉ đưa vào căn cứ `R_ot` khi thực chất là tiền lương gắn trực tiếp với công việc theo quy định áp dụng; không mặc định tính toàn bộ phụ cấp vào đơn giá OT. Từng khoản lưu `included_in_ot_base` và nguồn xác định. Với lương giờ không đổi, chỉ có lương công việc, phép chia này tương đương `R_ot = L_month/H_std`; không lấy lương gồm tiền nghỉ phép chia chỉ giờ đi làm để đẩy sai đơn giá. Nếu có nhiều mức trong tháng, tách khoảng mức lương và căn cứ tương ứng.

HNQ kiểm soát ca 8 giờ không quá 4 giờ OT/ngày; 40 giờ/tháng; mặc định 200 giờ/năm. Chỉ dùng 300 giờ/năm khi có căn cứ thuộc trường hợp được phép và thủ tục tương ứng. Kiểm tra cả lịch, giờ duyệt và giờ thực tế, theo thời giờ pháp luật áp dụng. Từ chối lập lịch mới vượt giới hạn chưa có căn cứ; vi phạm đã phát sinh phải cảnh báo/xử lý trách nhiệm, không xóa tiền công. Không tự loại Manager/Lead khỏi quyền OT chỉ dựa chức danh.

### 4.2. Công thức theo đoạn

Tách đoạn tại 00:00, 06:00, 22:00, đổi loại ngày/lương/lịch. Ưu tiên lễ, rồi nghỉ tuần, rồi ngày thường; không cộng chồng 200% và 300%. Ngày nghỉ bù tra lịch có hiệu lực.

```text
OT_day_segment = R_ot × H_ot_day × k
Night_regular_extra = R_ot × H_night_regular × 0,3
OT_night_segment = R_ot × H_ot_night × (k + 0,3 + 0,2 × k_day)
```

| Đoạn OT đêm | k | k_day | Tổng hệ số tối thiểu |
|---|---:|---:|---:|
| Ngày thường, chưa OT ban ngày trước đó trong ngày | 1,5 | 1 | 2,0 |
| Ngày thường, đã OT ban ngày trước đó trong ngày | 1,5 | 1,5 | 2,1 |
| Nghỉ tuần | 2 | 2 | 2,7 |
| Lễ | 3 | 3 | 3,9 |

`k_day` theo Điều 57 [L5], không luôn bằng 1. Giờ đêm thường đã có lương công việc ở lương theo công, chỉ cộng 30%; OT đêm dùng công thức đầy đủ, không cộng 30% lần nữa. Lương tháng đã có quyền lợi ngày lễ thì OT lễ cộng riêng; hình thức trả lương khác phải xác định thêm khoản lương lễ còn phải trả.

Cộng đoạn chưa làm tròn theo dòng OT thường/nghỉ tuần/lễ/đêm, rồi ROUND_VND từng dòng. Không làm tròn giờ thành nửa giờ, không tự loại dưới 30 phút. Giờ đêm thuộc lịch làm thường chỉ được ghi nhận sau khi lịch thay đổi hợp lệ, không áp ca đêm giả định cho lịch HNQ ban ngày.

## 5. Phụ cấp và phân loại thu nhập

### 5.1. Mức và phân bổ

| Khoản | Full-time đủ điều kiện | Part-time mẫu | Đối tượng |
|---|---:|---:|---|
| Ăn trưa | 730.000 | 365.000 | Theo điều kiện chính sách |
| Gửi xe | 200.000 | 100.000 | Theo điều kiện chính sách |
| Điện thoại | 300.000 | Theo quyết định riêng | Sales, Account, Lead/Manager cần liên hệ công việc |
| Chức vụ | 2.000.000 | Theo bổ nhiệm nếu có | Lead, Manager, Giám đốc, đúng hiệu lực |

Thử việc hưởng theo thỏa thuận, không tự hưởng phụ cấp chính thức; thực tập đào tạo không có phụ cấp thường xuyên theo profile.

```text
Allowance_i = ROUND_VND(Σ_s A_i_s × H_eligible_i_s / H_std_s)
```

`H_eligible` là giờ THEO LỊCH trong thời gian đồng thời có hợp đồng và đủ điều kiện hưởng, trừ nghỉ không lương. Trễ/sớm hoặc phép hưởng lương không tự giảm phụ cấp. Vào/nghỉ/bổ nhiệm giữa kỳ tính theo hiệu lực; part-time dùng cả mức và mẫu số riêng. Cộng đoạn của từng khoản rồi làm tròn một lần, không cộng đồng thời mức đủ tháng và mức phân bổ.

### 5.2. Căn cứ thuế, bảo hiểm

| Khoản | Gross | Thuế bộ HNQ | Căn cứ bảo hiểm |
|---|---|---|---|
| Lương theo công | Có | Chịu thuế, trừ phần miễn có căn cứ | Lương công việc và khoản thường xuyên theo hợp đồng |
| Ăn trưa trả tiền | Có | Không tính thuế trong điều kiện/hạn mức có hiệu lực | Hỗ trợ tách riêng, không tự cộng |
| Gửi xe trả tiền | Có | Chịu thuế trong bộ HNQ | Hỗ trợ tách riêng, không tự cộng |
| Điện thoại công việc | Có nếu trả tiền | Không tính khi đủ quy chế/căn cứ và hạn mức; phần vượt chịu thuế | Hỗ trợ tách riêng, không tự cộng |
| Chức vụ thường xuyên | Có | Chịu thuế | Có khi thuộc căn cứ hợp đồng |
| OT/làm đêm | Có | Miễn trong điều kiện/mức hợp pháp bộ 2026 [L2] | Không cộng căn cứ thường xuyên |
| Hoa hồng/KPI/thưởng dự án | Có | Chịu thuế | Khoản biến động kết quả không tự cộng; xét bản chất hợp đồng |
| Hoàn chi công tác có chứng từ | Ngoài Gross | Theo bản chất chi phí công vụ | Không tự cộng |

730.000 là MỨC CHI NỘI BỘ, không phải trần miễn thuế cố định vĩnh viễn. Bộ HNQ chỉ chi 730.000/365.000; việc không tính thuế phải có quy chế và căn cứ đáp ứng điểm g khoản 2 Điều 8 Nghị định 253/2026/NĐ-CP [L2]. Hạn mức pháp lý của khoản tiền ăn lưu trong bộ tham số được HR/Finance đối chiếu văn bản gốc theo ngày chi; không dùng mức chi nội bộ làm hạn mức pháp luật và không nhân đôi hạn mức khi chia lần trả. Điện thoại và ngoại lệ cũng phải có quy tắc thuế, quy chế/căn cứ và phiên bản. Chưa có phân loại thì báo HR xác nhận trước approve, không tự miễn hay mặc định mọi khoản chịu thuế. Hoàn chi không cộng Gross rồi trừ như khấu trừ lương.

## 6. Hoa hồng và thưởng

Chỉ Sales Executive được giao sở hữu doanh thu hưởng chính sách Sales. Doanh thu phải có hợp đồng dịch vụ, nghiệm thu và thực nhận; lấy phí dịch vụ chưa VAT sau chiết khấu, loại chi hộ quảng cáo, hoàn/hủy. Không trùng khoản thu/kỳ/nhân viên. Sales Manager và Finance xác nhận, HR dùng bảng đã xác nhận.

```text
Revenue_eligible = Σ doanh thu dịch vụ đủ điều kiện chưa ghi nhận ở kỳ khác
Commission = ROUND_VND(Revenue_eligible × 2%)
Target = 100.000.000 × ngày theo lịch giữ vị trí Sales / ngày chuẩn cả tháng
KPI_bonus = 1.500.000 nếu Target > 0 và Revenue_eligible ≥ Target; ngược lại 0
```

So sánh chính xác, không làm tròn tỷ lệ hiển thị trước xét đạt. Dưới KPI vẫn trả hoa hồng; vượt KPI không tự tăng thưởng. Vào/rời Sales giữa tháng giảm chỉ tiêu theo lịch; nghỉ phép có/không lương không tự hạ chỉ tiêu nếu không có quyết định riêng. Thưởng 1.500.000 không chia đôi do chỉ tiêu nửa tháng. Sales Manager không nhận 2% doanh thu nhóm; phòng khác chỉ có thưởng theo quyết định, không giảm lương cơ bản vì đánh giá/KPI thấp.

Hoàn tiền sau khóa lập adjustment đối soát, không xóa kỳ cũ hoặc tự cấn trái căn cứ. Khoản thu/nguồn thưởng có mã duy nhất, chạy lại không cộng hai lần.

## 7. Bảo hiểm người lao động

HR xác nhận đối tượng từng quỹ và mức đóng theo tháng. Căn cứ gồm lương công việc/chức danh, phụ cấp và khoản bổ sung thuộc diện đóng theo hợp đồng/tính chất chi trả. Không dùng lương theo công đã giảm vì nghỉ ngắn ngày thay mức đóng. `insurance_salary` là mức đã đối soát, không mặc định bằng `base_salary` nếu có chức vụ/khoản thường xuyên thuộc căn cứ.

```text
B_bhxh = min(mức đóng BHXH hợp lệ, 20 × mức tham chiếu)
B_bhyt = min(mức đóng BHYT hợp lệ, 20 × mức tham chiếu)
B_bhtn = min(mức đóng BHTN hợp lệ, 20 × tối thiểu vùng)
BHXH = ROUND_VND(B_bhxh × 8%) nếu tham gia; ngược lại 0
BHYT = ROUND_VND(B_bhyt × 1,5%) nếu tham gia; ngược lại 0
BHTN = ROUND_VND(B_bhtn × 1%) nếu tham gia; ngược lại 0
Insurance_employee = BHXH + BHYT + BHTN
```

Bộ từ 07/2026: trần BHXH/BHYT 50.600.000; BHTN vùng I 106.200.000. Mức tham chiếu không phải lương cơ bản doanh nghiệp phải trả. Kiểm tra cả sàn áp dụng theo đối tượng/địa bàn.

Nghỉ không lương dưới 14 ngày làm việc không tự chia tỷ lệ bảo hiểm theo giờ. Từ 14 ngày cần hồ sơ báo giảm hoặc thỏa thuận tiếp tục đóng BHXH; ốm/thai sản, tháng đầu/tháng quay lại và từng quỹ có ngoại lệ [L6]. Không dùng một cờ miễn cả ba quỹ. Thiếu hồ sơ phải nêu rõ trước approve. Thử việc/thực tập/part-time xét quan hệ lao động, hợp đồng, mức lương và điều kiện luật định, không tự miễn theo tên loại nhân sự. Phần doanh nghiệp đóng lưu riêng, không trừ Net. Đổi mức giữa tháng dùng mức tháng được HR xác nhận, không tự bình quân.

## 8. Thuế thu nhập cá nhân

### 8.1. Phương pháp

| Đối tượng | Khấu trừ |
|---|---|
| Cá nhân cư trú thuộc diện lũy tiến | Biểu 5 bậc và giảm trừ hợp lệ |
| Cư trú không HĐLĐ/HĐLĐ dưới 3 tháng | Bộ từ 01/07/2026: từ 5.000.000/lần, khấu trừ 10% trên căn cứ hợp lệ; cam kết chỉ khi đủ điều kiện và hồ sơ [L2] |
| Không cư trú | Phương pháp riêng, thông thường 20% tiền lương chịu thuế; HR xác nhận quy tắc áp dụng |

Không suy phương pháp từ Net hoặc role, không chia nhỏ lần trả để tránh ngưỡng. Tổng hợp các lần trả cùng tháng và đối soát thuế đã khấu trừ để không dùng giảm trừ nhiều lần. `tax_commitment_08 = true` không thay hồ sơ cam kết. Trả sau nghỉ việc phân loại theo thời điểm chi và quy định, không mặc định phương pháp hợp đồng cũ. Nhánh engine chưa hỗ trợ phải nhận kết quả HR xác nhận có chứng từ, không âm thầm dùng lũy tiến.

### 8.2. Công thức bộ năm 2026

```text
Gross = Salary_attendance + OT + Night_extra + Allowances
        + Commission + Approved_bonuses + Other_income
Taxable_income = Gross − Exempt_income − Non_taxable_income
Family_relief = 15.500.000 + N_dependents_valid × 6.200.000
T = max(0, Taxable_income − Insurance_deductible − Family_relief − Other_legal_relief)
PIT = ROUND_VND(Σ_b max(0, min(T, upper_b) − lower_b) × rate_b)
```

| Bậc | Phần T/tháng (VND) | Thuế suất | Rút gọn đối soát |
|---|---|---:|---|
| 1 | Đến 10.000.000 | 5% | `T × 5%` |
| 2 | Trên 10.000.000 đến 30.000.000 | 10% | `T × 10% − 500.000` |
| 3 | Trên 30.000.000 đến 60.000.000 | 20% | `T × 20% − 3.500.000` |
| 4 | Trên 60.000.000 đến 100.000.000 | 30% | `T × 30% − 9.500.000` |
| 5 | Trên 100.000.000 | 35% | `T × 35% − 14.500.000` |

Người phụ thuộc dùng hồ sơ đủ điều kiện theo tháng. Không chia giảm trừ theo tỷ lệ công. Payroll là khấu trừ, không thay quyết toán thuế năm.

Bộ 2026 phân loại miễn toàn bộ khoản OT/làm đêm đủ điều kiện, trong mức pháp luật cho phép theo Điều 26 Nghị định 253/2026; phần vượt/không đủ điều kiện phân loại riêng [L2]. Không dùng quy tắc lịch sử chỉ miễn chênh lệch cho mọi kỳ. Không miễn toàn bộ lương tháng chỉ vì có ca đêm: chỉ phần thực sự thuộc thời gian/điều kiện được miễn, không trùng khoản. Bộ trước 2026 giữ quy tắc lịch sử riêng.

## 9. Khấu trừ, điều chỉnh và thực nhận

```text
Net_entitlement = Gross − Insurance_employee − PIT − Legal_deductions
                 + Net_adjustment_credit − Net_adjustment_debit
Payable = Net_entitlement − Advance_recovery + Reimbursements
```

Phiếu lương phân biệt Net sau nghĩa vụ, thu hồi tạm ứng, hoàn chi và thực chi. Nếu hệ thống chỉ có `net_salary`, chốt trường này là `Payable` và lưu thành phần đối soát; không thu hồi tạm ứng ở cả `other_deductions` và `Advance_recovery`.

- Khấu trừ hợp lệ gồm khoản có chứng từ/căn cứ như bồi thường, khoản phải thu được phép; không có phạt tiền đi trễ hoặc KPI kém.
- Bồi thường khấu trừ lương tuân giới hạn pháp luật, gồm mức 30% lương thực trả sau bảo hiểm và thuế trong trường hợp Điều 102 Bộ luật Lao động [L5]. Không áp 30% máy móc cho mọi loại thanh toán; tách số thu kỳ này/số còn lại, không tự ép thu toàn bộ.
- Adjustment có kỳ gốc/kỳ ghi nhận, loại GROSS/TAX/INSURANCE/NET, số tiền có dấu, nguồn duy nhất, lý do, chứng từ, người tạo và duyệt. Gross adjustment chạy qua thuế/bảo hiểm tương ứng, chỉ Net adjustment thuần túy cộng/trừ cuối. Không vừa cộng Gross vừa cộng Net cùng nguồn.
- Sai bảo hiểm/thuế kỳ cũ cần hồ sơ nghĩa vụ tương ứng; adjustment tiền kỳ sau không thay tờ khai điều chỉnh. Không sửa PDF/snapshot kỳ gốc.
- Không trả âm hay tự ép âm về 0 rồi mất dấu. Lưu nghĩa vụ, phải thu, hoãn thu và quyết định phân bổ hợp lệ trước khóa; không tự cấn hết lương vì khoản tranh chấp.
- Người nghỉ việc vẫn được quyết toán khoản còn phải trả, không bị loại chỉ vì `TERMINATED`. Phép chưa nghỉ/trợ cấp khi đủ điều kiện dùng căn cứ riêng, phân loại thuế; không dùng tùy tiện đơn giá tháng hiện tại cho mọi chế độ.

## 10. Làm tròn và khóa kỳ

### 10.1. Làm tròn

`ROUND_VND(x) = x.setScale(0, HALF_UP)`, kể cả số âm. Tiền cuối là số nguyên VND. Trung gian dùng BigDecimal/decimal, phép chia tối thiểu 12 chữ số thập phân, không làm tròn đơn giá để tính tiếp; không float/double.

Làm tròn từng dòng cuối: lương sau cộng đoạn, từng phụ cấp, từng nhóm OT, từng quỹ bảo hiểm, hoa hồng, từng quyết định thưởng, thuế sau cộng bậc. Gross/Net cộng dòng đã chốt. Không làm tròn từng bậc thuế/từng phút/tổng toàn công ty rồi phân bổ chênh cho nhân viên. Lưu đầu vào, công thức, tham số, số trước/sau làm tròn và phiên bản.

### 10.2. Chốt nguồn và trạng thái

| Trạng thái | Hành vi | Kiểm soát |
|---|---|---|
| DRAFT | Chuẩn bị nguồn, xử lý công/đơn, chốt công, tính | Chốt công có người/thời điểm/phiên bản snapshot riêng |
| CALCULATED | Preview, rà soát, tính lại cùng nguồn hoặc trả DRAFT sửa nguồn | Sửa nguồn phải mở chốt công có lý do, vô hiệu kết quả cũ |
| APPROVED | Khóa đúng phiên bản đã duyệt | Nguồn/kết quả bất biến, không tính lại/sửa trực tiếp |
| LOCKED | Đọc, phát hành/xem PDF theo quyền, ghi nhận chi trả riêng | Không mở/sửa/xóa công, kết quả, snapshot kỳ gốc |

`CALCULATED → DRAFT` là đường sửa trước duyệt có audit. Không có đường mở lại Approved/Locked trong bản chốt; chỉ approve sau rà soát đầy đủ. `PAID` là trạng thái chi trả riêng, không thay Locked hoặc mở quyền sửa lương.

Trước calculate: kiểm tra quyền, kỳ không chồng, hợp đồng/lịch/thu nhập đủ dữ liệu và nguồn đã chốt. Trước approve/lock: tổng dòng khớp, đúng thẩm quyền/phiên bản, không còn ngoại lệ trọng yếu chưa phân loại. Nếu còn phần chờ xác minh, HR lập hồ sơ chi phần hợp lệ và điều chỉnh riêng, không để phần mềm tự giữ toàn bộ lương hay coi Pending là 0.

Thẩm quyền và nguyên tắc tách biệt nhiệm vụ áp dụng theo Mục 1.1. Trước khi duyệt/khóa, kiểm tra grant còn hiệu lực và người lập bảng; nếu chưa có người được phân quyền hợp lệ thì không chuyển trạng thái. Không tự suy quyền từ role nền tảng hoặc chức danh.

Batch ghi toàn bộ kết quả trong một transaction; lỗi rollback cả batch, không nửa kỳ mới/nửa kỳ cũ. Chạy lại cùng nhân viên/kỳ/phiên bản idempotent, không trùng phiếu/hoa hồng/adjustment. Approve/lock kiểm soát phiên bản và khóa giao dịch chống đồng thời sửa nguồn/duyệt PTO/OT. Trái trạng thái trả 409, trái quyền 403; bảo vệ backend và DB, không chỉ ẩn nút.

Sau commit khóa, tạo việc PDF/thông báo chống trùng. Lỗi PDF/email retry có log, không đảo trạng thái kỳ, không gửi trước commit. Nhân viên chỉ xem phiếu đã phát hành của mình; Manager không mặc nhiên xem lương cấp dưới; Admin không tự có quyền lương. Audit append-only cho chốt công, tính, duyệt, khóa, adjustment và thay đổi nguồn.

Theo profile: ngày 01–02 tháng sau đối soát; ngày 03 tính; ngày 04 duyệt/khóa; ngày 05 trả, nếu nghỉ thì ngày làm việc liền trước. Dời các mốc chuẩn bị sớm tương ứng khi ngày trả sớm hơn ngày 05. Khóa kích hoạt chuẩn bị PDF, phát hành theo lịch chi và lưu thời điểm phát hành riêng.

## 11. Ví dụ tính lương và tiêu chí đối soát

### 11.1. Ví dụ chuẩn

Ví dụ full-time có 22 ngày/176 giờ; không xác lập mọi tháng là 22 ngày. Thuế bộ 2026, cư trú/lũy tiến, không người phụ thuộc trừ khi ghi khác; tham gia đủ ba quỹ với mức HR xác nhận; ăn trưa đủ căn cứ không tính thuế, gửi xe chịu thuế. Không tạm ứng/hoàn chi thì Net bằng Payable.

| Mã | Đầu vào | Kết quả kỳ vọng (VND) |
|---|---|---|
| P01 Content đủ công | Lương 10 triệu; ăn 730.000; xe 200.000; mức BH 10 triệu | Gross 10.930.000; BH 1.050.000; PIT 0; Net 9.880.000 |
| P02 Nghỉ 2 ngày không lương | Như P01, công 160/176, phụ cấp cùng tỷ lệ | Lương 9.090.909; ăn 663.636; xe 181.818; Gross 9.936.363; BH 1.050.000; Net 8.886.363 |
| P03 Vào giữa kỳ | Như P01, 88/176 giờ; HR xác nhận tháng này vẫn BH 10 triệu | Lương 5 triệu; phụ cấp 465.000; Gross 5.465.000; BH 1.050.000; PIT 0; Net 4.415.000 |
| P04 Part-time nửa kỳ | Lịch 20 ngày/80 giờ; lương 5 triệu; 40 giờ; ăn 365.000/xe 100.000 phân bổ; BH 5 triệu | Lương 2,5 triệu; phụ cấp 232.500; Gross 2.732.500; BH 525.000; PIT 0; Net 2.207.500 |
| P05 Sales đạt KPI | Lương 9 triệu; phụ cấp 1.230.000 gồm điện thoại đủ căn cứ; doanh thu 100 triệu; BH 9 triệu | Hoa hồng 2 triệu; KPI 1,5 triệu; Gross 13.730.000; BH 945.000; PIT 0; Net 12.785.000 |
| P06 OT và thuế | Lương/BH 35 triệu; 168/176 giờ; OT thường 8 giờ với R_ot = 35 triệu/176; phụ cấp chịu thuế 1,5 triệu; OT đủ điều kiện miễn | Lương 33.409.091; OT 2.386.364; Gross 37.295.455; BH 3.675.000; thu nhập chịu thuế 34.909.091; T 15.734.091; PIT 1.073.409; Net 32.547.046 |
| P07 Trần BH từ 07/2026 | Căn cứ BHXH/BHYT 60 triệu; căn cứ BHTN 60 triệu, giả định địa bàn vùng I và đủ điều kiện tham gia | BHXH 4.048.000; BHYT 759.000; BHTN 600.000; tổng 5.407.000 |
| P08 Đổi lương | 88 giờ mức 10 triệu và 88 giờ mức 12 triệu; H_std cùng 176 | Lương theo công 11 triệu, không dùng mức cuối kỳ cho toàn tháng |

### 11.2. Ca biên bắt buộc

| Mã | Tình huống | Kết quả/tiêu chí |
|---|---|---|
| B01 | Tháng 20/22/23 ngày, đủ công | Cùng lương tháng đều nhận đủ |
| B02 | Nghỉ lễ và làm ngày lễ | Không trừ lương tháng/cộng trùng công; OT lễ riêng |
| B03 | 4 giờ làm + 4 giờ PTO/không lương | 8/4 giờ hưởng lương, không đếm trùng |
| B04 | Thiếu checkout/Pending | Needs Review, có xử lý trước duyệt, không tự 0 |
| B05 | Trễ 10 phút có/không giờ bù xác nhận | Không phạt, chỉ chênh công đã đối soát |
| B06 | Ăn/xe phân bổ 72/176 | 298.636 + 81.818 = 380.454, không phải 380.455 |
| B07 | OT duyệt 3, thực tế 2; vượt đã xác nhận | Trả 2; phần vượt có dòng xác nhận riêng |
| B08 | R_ot 100.000, 2 giờ OT ngày | Thường 300.000; nghỉ tuần 400.000; lễ 600.000 |
| B09 | R_ot 100.000, 2 giờ OT đêm | Thường chưa OT ngày 400.000; đã OT ngày 420.000; nghỉ tuần 540.000; lễ 780.000 |
| B10 | Qua 22:00/00:00/đổi loại ngày | Chia đoạn chính xác, không trùng phút |
| B11 | KPI 80/100/150 triệu | Hoa hồng 1,6/2/3 triệu; KPI 0/1,5/1,5 triệu |
| B12 | Sales nửa tháng, doanh thu 60 triệu | Chỉ tiêu 50 triệu; hoa hồng 1,2 triệu; KPI 1,5 triệu |
| B13 | T = 0/10/30/60/100 triệu | PIT 0/500.000/2.500.000/8.500.000/20.500.000 |
| B14 | Một phụ thuộc hợp lệ | Giảm trừ 21.700.000, không chia công |
| B15 | Nghỉ không lương 13/14 ngày, ốm/thai sản | Đúng hồ sơ từng quỹ, không tự miễn cả ba |
| B16 | Phạt trễ/khấu trừ không căn cứ/adjustment trùng | Từ chối, không trùng nguồn |
| B17 | Tính lại/sửa nguồn Approved/Locked | 409, snapshot không đổi |
| B18 | Tính hai lần, lỗi giữa batch | Idempotent; rollback toàn batch khi lỗi |
| B19 | Duyệt PTO đồng thời khóa | Một phiên bản nhất quán, không đổi nguồn sau khóa |
| B20 | Nghỉ việc còn phải trả | Có quyết toán, không loại bởi trạng thái |
| B21 | Chi qua năm/short-term/không cư trú | Đúng bộ ngày chi và phương pháp |
| B22 | HALF_UP | 0,5 → 1; -0,5 → -1; tổng khớp dòng |
| B23 | 500 nhân viên, tải 50 người đồng thời | Batch ≤ 5 phút; không bỏ/trùng; có báo cáo đo |
| B24 | Gross/Net adjustment, tạm ứng và hoàn chi | Phân loại thuế đúng, mỗi khoản cộng/trừ một lần |

### 11.3. Quy tắc sử dụng ví dụ

- P01–P08 là bộ ví dụ đối soát nghiệp vụ cho HRFlow, không phải bảng lương của người lao động thực tế.
- Mỗi ví dụ chỉ có giá trị khi các giả định đầu vào được ghi rõ: lịch chuẩn, hợp đồng, đối tượng bảo hiểm, căn cứ đóng, tình trạng cư trú, người phụ thuộc, phân loại thuế của từng khoản và ngày chi trả.
- Nếu tham số pháp lý thay đổi, không sửa âm thầm kết quả lịch sử. Tạo bộ tham số mới theo ngày hiệu lực và cập nhật expected result của kỳ áp dụng tương ứng.
- Các ca kiểm thử kỹ thuật về API, database, quyền truy cập, transaction, migration và hiệu năng thuộc kế hoạch kiểm thử/đặc tả kỹ thuật riêng; tài liệu này chỉ xác định kết quả nghiệp vụ mong đợi.

## 12. Căn cứ pháp lý và quản lý tham số

Ngày rà soát nguồn: 29/10/2026. Các liên kết dưới đây là điểm bắt đầu để đối chiếu văn bản; khi vận hành thật, HR/Tài chính phải kiểm tra toàn văn, điều khoản, văn bản sửa đổi/bổ sung và hiệu lực tại ngày phát sinh/chi trả. Không dùng bài giải thích thứ cấp thay cho văn bản gốc khi xác lập tham số.

| Mã | Nguồn tham chiếu | Dùng để xác minh |
|---|---|---|
| L1 | [Luật Thuế thu nhập cá nhân số 109/2025/QH15 – Cổng văn bản Chính phủ](https://vanban.chinhphu.vn/?classid=1&docid=216495&pageid=27160&typegroupid=1); [Nghị quyết 110/2025/UBTVQH15 về giảm trừ gia cảnh](https://vanban.chinhphu.vn/?docid=215927&pageid=27160) | Cơ chế thuế, giảm trừ gia cảnh và thời điểm áp dụng; phải đọc cùng văn bản sửa đổi có hiệu lực tại kỳ chi trả. |
| L2 | [Nghị định 253/2026/NĐ-CP](https://vanban.chinhphu.vn/?docid=218684&pageid=27160&typegroupid=4); [Thông tư 87/2026/TT-BTC](https://vanban.chinhphu.vn/?docid=218772&pageid=27160&typegroupid=6) | Quy định chi tiết về thuế TNCN, khấu trừ, phân loại thu nhập và hồ sơ liên quan từ ngày hiệu lực. |
| L3 | [Nghị định 161/2026/NĐ-CP](https://vanban.chinhphu.vn/?docid=218107&pageid=27160) | Tham chiếu mức lương cơ sở từ 01/07/2026; không tự động coi đây là căn cứ đóng cho mọi quỹ/người lao động nếu pháp luật áp dụng yêu cầu điều kiện khác. |
| L4 | [Nghị định 293/2025/NĐ-CP về lương tối thiểu](https://vanban.chinhphu.vn/?classid=1&docid=215832&pageid=27160&orggroupid=2) | Mức lương tối thiểu tháng/giờ theo địa bàn từ 01/01/2026; phải xác định đúng địa bàn làm việc. |
| L5 | [Bộ luật Lao động 45/2019/QH14](https://vanban.chinhphu.vn/); [Nghị định 145/2020/NĐ-CP](https://vbpl.moj.gov.vn/TW/Pages/vbpq-toanvan.aspx?ItemID=152668&dvid=13) | Tiền lương, làm thêm giờ, làm đêm, giới hạn thời giờ làm thêm, khấu trừ và xử lý kỷ luật. Cần tra điều khoản cụ thể theo tình huống. |
| L6 | [Luật Bảo hiểm xã hội 2024 – Cổng văn bản Chính phủ](https://vanban.chinhphu.vn/); [cổng thông tin Bảo hiểm xã hội Việt Nam](https://baohiemxahoi.gov.vn/) | Đối tượng tham gia, căn cứ đóng, mức trần, nghỉ không lương, báo tăng/giảm và ngoại lệ từng quỹ. |

### 12.1. Nguyên tắc quản lý bộ tham số pháp lý

1. Mỗi tham số cần có tên, giá trị, đơn vị, đối tượng áp dụng, địa bàn (nếu có), ngày bắt đầu/kết thúc hiệu lực, mã văn bản/điều khoản, URL nguồn chính thức, người xác nhận và ngày xác nhận.
2. Không gộp “tham số công ty” với “tham số pháp luật”. Ví dụ: phụ cấp ăn trưa 730.000 VND là mức chi nội bộ của HNQ; nó không tự trở thành hạn mức miễn thuế pháp luật.
3. Không dùng cùng một cờ miễn/được tham gia cho toàn bộ BHXH, BHYT, BHTN hoặc toàn bộ thu nhập chịu thuế. Xét từng quỹ và từng khoản theo quy định tương ứng.
4. Nếu chưa xác minh được căn cứ hoặc hồ sơ cá nhân, đánh dấu `NEEDS_REVIEW`, lưu người chịu trách nhiệm và thời hạn xử lý; không tự suy đoán để tính hoặc phê duyệt.
5. Mọi thay đổi bộ tham số phải có lịch sử phiên bản. Kỳ đã khóa giữ nguyên snapshot; kỳ chưa khóa áp dụng bộ có hiệu lực đúng thời điểm phát sinh/chi trả.

## 13. Vấn đề cần xác nhận trước khi áp dụng trả lương thực tế

Các mục dưới đây không cản trở việc dùng tài liệu này làm baseline nghiệp vụ cho dự án HRFlow, nhưng phải được xác nhận trước khi HNQ dùng để trả lương ngoài đời thực:

| Mã | Nội dung cần xác nhận | Trách nhiệm đề xuất | Cách xử lý trong HRFlow |
|---|---|---|---|
| OPEN-01 | Địa chỉ/nơi làm việc cụ thể để xác định vùng lương tối thiểu và trần BHTN tương ứng | HR + Finance | Cấu hình địa bàn hiệu lực; không hard-code “vùng I” chỉ từ tên TP.HCM. |
| OPEN-02 | Toàn bộ tham số thuế TNCN năm 2026, thời điểm áp dụng theo năm tính thuế/ngày chi trả, phương pháp khấu trừ theo từng loại hợp đồng | HR/Finance xác minh theo văn bản gốc | Lưu thành bộ tham số có phiên bản; các nhánh chưa được xác nhận không tự tính. |
| OPEN-03 | Căn cứ đóng và tình trạng tham gia từng quỹ BHXH/BHYT/BHTN của full-time, part-time, thử việc và thực tập | HR xác nhận theo hợp đồng/hồ sơ; Finance đối soát | Không suy từ `employee_type`; lưu kết quả xác nhận theo nhân viên và tháng. |
| OPEN-04 | Quy chế/điều kiện thuế đối với ăn trưa, gửi xe, điện thoại, chức vụ, thưởng và khoản hoàn chi | HR + Finance | Mỗi loại khoản có mã phân loại thuế/bảo hiểm riêng, không áp một quy tắc cho tất cả. |
| OPEN-05 | Company Profile đã giao Giám đốc quyền `APPROVE` và Finance Manager quyền `LOCK`. Còn thiếu biện pháp xử lý xung đột khi Giám đốc phê duyệt kỳ lương có phần lương của chính mình. | Cấp có thẩm quyền HNQ | Xác nhận biện pháp xử lý trước khi duyệt kỳ có xung đột; không tự duyệt phần lương của mình, không tự đặt tuyến thay thế hoặc cơ chế duyệt từng phiếu. |
| OPEN-06 | Căn cứ, thủ tục và ngoại lệ OT; các mức giới hạn theo nhóm công việc/ngành nghề và hồ sơ đồng ý của người lao động | HR + Manager | Kiểm tra trước khi lập lịch và trước khi duyệt; nếu có giờ thực tế phát sinh thì vẫn phải xử lý nghĩa vụ trả lương theo quy định. |

## 14. Lịch sử phiên bản

| Phiên bản | Ngày | Nội dung thay đổi |
|---|---|---|
| 2.0 | 26/10/2026 | Bản quy tắc payroll chi tiết ban đầu. |
| 2.1 | 29/10/2026 | Đồng bộ một phần thông tin với Company Profile, làm rõ dữ liệu Sales và quyền payroll. |
| 3.0 | 29/10/2026 | Chuyển thành baseline nghiệp vụ gốc; bỏ phụ thuộc SRS/API/RBAC chưa được đồng bộ; phân biệt chính sách nội bộ với tham số pháp lý; yêu cầu xác minh địa bàn, thuế, bảo hiểm và quản lý phiên bản trước khi dùng thực tế. |

**Điều kiện để các tài liệu khác sử dụng bản này làm nguồn:** công thức và chính sách mô phỏng trong Mục 1–10 là baseline nghiệp vụ của dự án. Các mục `OPEN-01` đến `OPEN-06` là tham số/câu hỏi cần xác nhận trước khi áp dụng trả lương thực tế; trong SRS/ERD/API/test case phải thể hiện là cấu hình có hiệu lực hoặc trạng thái `NEEDS_REVIEW`, không được tự giải quyết bằng giả định. Dữ liệu mô phỏng để kiểm thử không được trình bày như tư vấn pháp lý hoặc bảng lương đã xác minh cho người lao động thật.
