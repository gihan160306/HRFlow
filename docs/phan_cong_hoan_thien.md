# PHÂN CÔNG KIỂM TRA VÀ HOÀN THIỆN HRFLOW

> GiHan đã làm xong phần của mình. Tài liệu này giao việc cho 4 bạn còn lại: **Hiếu, Hoa, Ngân, Quan**.
> Chưa làm phần viết báo cáo. Chỉ tập trung kiểm tra code và hoàn thiện web.

| Bạn | Phụ trách | Khu vực code chính (chỉ sửa trong khu vực của mình) |
|---|---|---|
| Hiếu | Phần lương | `backend/.../payroll/`, `backend/.../income/`, `frontend/src/features/payroll`, `payslip`, `income` |
| Hoa | Cơ sở dữ liệu | `backend/src/main/resources/db/migration/` (chỉ thêm file mới), `database/`, `backend/.../hr/OrgController.java` |
| Ngân | Giao diện (frontend) | `frontend/src/` (trừ các trang lương, làm cùng Hiếu) |
| Quan | Kiểm thử | `backend/src/test/`, `test/`, `.github/workflows/ci.yml` |

---

## 0. Tình trạng hiện tại của web (để mọi người nắm)

Web đã khá đầy đủ, ước chừng **85–90%**. Đã có:

- Đăng nhập, phân quyền 4 vai trò (Admin, HR, Manager, Employee).
- Quản lý nhân viên, phòng ban, hợp đồng.
- Chấm công, nghỉ phép, làm thêm giờ (OT).
- Tính lương, duyệt lương, khóa kỳ lương, phiếu lương (xem và tải PDF).
- Thông báo, nhật ký hệ thống, báo cáo cơ bản.
- 21 file migration cơ sở dữ liệu, có sẵn dữ liệu mẫu 100 nhân viên.

**Đã kiểm tra:** giao diện build được, 23 bài test hiện có đều chạy qua.

**Chưa kiểm tra:** chưa chạy web với cơ sở dữ liệu thật để thử toàn bộ luồng. Vì vậy mỗi bạn phải tự chạy thử phần của mình trước khi kết luận là "đúng" hay "sai".

**Những chỗ còn thiếu đã biết:**

| Chỗ thiếu | Ai làm |
|---|---|
| Chưa có form xử lý "xung đột lợi ích" khi tính lương Giám đốc | Hiếu và Ngân |
| Chức danh và lịch làm việc chỉ xem được, chưa thêm/sửa được | Hoa (backend) và Ngân (giao diện) |
| Phòng ban chưa có chức năng ngừng hoạt động | Hoa và Ngân |
| Báo cáo còn ít (chỉ có số lượng nhân viên và giờ OT) | Hiếu và Ngân |
| Chỉ xuất lương ra CSV, chưa có Excel | Hiếu |
| Giao diện chưa dùng chức năng làm mới đăng nhập (refresh token) | Ngân |
| Chưa có màn hình HR xem và chỉnh quỹ phép cả công ty | Ngân |
| Ít bài test (chỉ 2 file), chưa có test phân quyền, test tích hợp | Quan |
| File CI (`.github/workflows/ci.yml`) đang rỗng | Quan |
| Thư mục `database/` và `test/` đang trống | Hoa và Quan |

---

## 1. Quy tắc chung cho cả 4 bạn

1. **Không cần tạo nhánh riêng.** Nhóm đã có nhánh backup nên mọi người làm trực tiếp trên nhánh `dev`. Để không đè code của nhau:
   - **Trước khi bắt đầu làm mỗi ngày:** chạy `git pull` để lấy code mới nhất.
   - **Chỉ sửa file trong khu vực của mình** (xem bảng đầu trang). Cần sửa file của người khác thì nhắn người đó hoặc GiHan trước.
   - **Commit nhỏ và thường xuyên**, mỗi commit một việc, viết tin nhắn commit rõ ràng (ví dụ: `fix: sửa tính OT ngày lễ`).
   - **Push ngay sau khi commit** và trước khi push luôn `git pull` lại. Nếu bị xung đột (conflict) thì dừng lại, đừng tự ép đẩy lên.
   - **Không dùng** `git push --force` và không xóa lịch sử commit.
2. **Không sửa các file migration cũ** (từ `V1` đến `V21`). Nếu cần đổi cơ sở dữ liệu, tạo file mới (`V22__...`) và báo GiHan trước.
3. **Ghi lỗi vào file chung `docs/bug_log.md`.** Mỗi lỗi ghi: mã lỗi, mô tả, cách làm ra lỗi, ai tìm thấy, ai sửa, đã sửa chưa. **Thấy lỗi ở phần của bạn khác thì ghi vào đây, đừng tự sửa code của họ.**
4. **Nghiệp vụ phải theo 3 tài liệu đã chốt:**
   - `docs/company_profile.md` (hồ sơ công ty)
   - `docs/permission_matrix.md` (ai được làm gì)
   - `docs/payroll_rules.md` (quy tắc tính lương)

   Nếu code và tài liệu khác nhau, **hỏi GiHan**, đừng tự quyết.
5. **Việc đầu tiên của mọi người:** chạy được web bằng Docker theo `HuongDanCaiDat.md`, rồi thử đăng nhập bằng cả 4 vai trò.
6. **Cuối mỗi ngày làm việc**, nhắn nhóm 2–3 dòng: hôm nay làm gì, đang kẹt gì, cần ai giúp.

### Mẫu ghi lỗi trong `docs/bug_log.md`

| Mã | Mô tả lỗi | Cách làm ra lỗi | Người tìm | Người sửa | Trạng thái |
|---|---|---|---|---|---|
| BUG-001 | Ví dụ: Nút "Duyệt" hiện với nhân viên thường | Đăng nhập EMP, vào trang Nghỉ phép | Ngân | Hiếu | Chưa sửa |

---

## 2. HIẾU – PHẦN LƯƠNG

### 2.1. Việc này là gì?

Kiểm tra xem hệ thống **tính lương có đúng quy tắc công ty không**, sửa chỗ sai và làm nốt các phần còn thiếu của lương.

### 2.2. Code cần đọc

**Backend** (`backend/src/main/java/com/hrflow/payroll/`):

- `PayrollEngine.java` – nơi tính lương (công thức)
- `PayrollService.java` – xử lý kỳ lương (tính, duyệt, khóa…)
- `SnapshotBuilder.java` – gom dữ liệu đầu vào trước khi tính
- `ParameterService.java` – các tham số (thuế, bảo hiểm…)
- `PayslipService.java`, `SimplePdf.java` – phiếu lương và file PDF
- `PayrollController.java` – các đường dẫn API của lương

Ngoài ra: `income/IncomeController.java` (thu nhập thêm, doanh thu Sales) và `security/PayrollAccess.java` (quyền lương).

**Frontend:** `frontend/src/features/payroll/`, `payslip/`, `income/`.

**Tài liệu:** `docs/payroll_rules.md` (đọc kỹ từ đầu đến cuối), `docs/payroll_implementation.md`.

### 2.3. Việc 1 – Kiểm tra code đã có

**(a) Kiểm tra công thức lương.** Mở `payroll_rules.md`, với mỗi quy tắc hãy tìm đoạn code tương ứng trong `PayrollEngine` và xem có khớp không. Cần soát các mục sau:

- [ ] Lương theo ngày công: làm đủ công thì nhận đủ lương tháng.
- [ ] Nhân viên vào làm hoặc nghỉ việc giữa tháng: lương tính theo phần tháng đã làm.
- [ ] Nghỉ không lương: bị trừ đúng.
- [ ] Tiền làm thêm (OT): hệ số khác nhau cho ngày thường, ngày nghỉ tuần, ngày lễ; cộng thêm tiền làm đêm; chỉ trả theo giờ làm thực tế đã được xác nhận.
- [ ] Phụ cấp: khoản nào tính thuế, khoản nào miễn thuế; không cộng một khoản hai lần.
- [ ] Bảo hiểm (BHXH, BHYT, BHTN): đúng tỷ lệ, đúng mức trần.
- [ ] Thuế thu nhập cá nhân: biểu thuế lũy tiến, giảm trừ bản thân, giảm trừ người phụ thuộc, trường hợp người không cư trú, thuế 10% cho khoản nhỏ.
- [ ] Hoa hồng và thưởng KPI của nhân viên Sales.
- [ ] Làm tròn đến đồng, làm tròn kiểu HALF_UP (từ 0,5 trở lên thì làm tròn lên) ở từng dòng tiền.

**(b) Kiểm tra quy trình của một kỳ lương.** Kỳ lương đi qua các bước: `DRAFT` (nháp) → `CALCULATED` (đã tính) → `APPROVED` (đã duyệt) → `LOCKED` (đã khóa). Thử trên web:

- [ ] Kỳ đã duyệt hoặc đã khóa thì **không** cho tính lại.
- [ ] Mở lại kỳ lương hoạt động đúng.
- [ ] Đánh dấu "đã trả lương" hoạt động đúng.
- [ ] Nút "kiểm tra trước khi tính" (precheck) báo đúng khi thiếu dữ liệu: có nói rõ **ai** thiếu **gì** không.

**(c) Kiểm tra phân quyền lương (rất quan trọng).** Dùng tài khoản thật để thử từng trường hợp:

- [ ] Người lập bảng lương **không được** tự duyệt bảng lương mình lập.
- [ ] Chỉ Giám đốc mới duyệt được.
- [ ] Chỉ Finance Manager mới khóa được kỳ lương.
- [ ] Admin **không** xem được chi tiết lương.
- [ ] Nhân viên chỉ xem được phiếu lương **của mình**.

**(d) Kiểm tra phiếu lương:**

- [ ] Số liệu trong phiếu lương khớp với bảng lương.
- [ ] File PDF hiển thị đúng tiếng Việt có dấu, số tiền định dạng đẹp.

### 2.4. Việc 2 – Hoàn thiện phần còn thiếu

1. **Xử lý xung đột lợi ích.** Khi kỳ lương có lương của chính Giám đốc (người duyệt), hệ thống đang chặn lại nhưng **chưa có chỗ nhập quyết định xử lý** (xem `frontend/src/features/payroll/PayrollDetailPage.jsx`, dòng khoảng 97, và `PayrollService.java` dòng khoảng 625). Hiếu hoàn thiện phần backend, Ngân làm form giao diện.
2. **Xuất bảng lương ra Excel (.xlsx)**, hiện chỉ có CSV. Làm nếu còn thời gian.
3. **Cải thiện file PDF phiếu lương** nếu chữ bị lỗi hoặc bố cục xấu.
4. **Thêm API báo cáo lương:** tổng chi phí lương theo phòng ban, theo tháng; tổng tiền bảo hiểm và thuế. Báo Ngân khi xong để Ngân làm giao diện.
5. **Làm thông báo lỗi dễ hiểu hơn** khi kỳ lương thiếu dữ liệu.

### 2.5. Kết quả cần nộp

- Một bảng đối chiếu: *Quy tắc trong `payroll_rules.md` → Đoạn code → Đúng/Sai*.
- **Ít nhất 10 trường hợp tính lương bằng tay** và so với kết quả hệ thống. Gợi ý các trường hợp: nhân viên bình thường; vào làm giữa tháng; nghỉ việc giữa tháng; có OT ngày lễ; có OT ban đêm; có người phụ thuộc; Sales vượt KPI; thực tập sinh; bán thời gian; nghỉ không lương.
- Các lỗi đã sửa (đã commit và push lên `dev`).
- Danh sách trường hợp trên đưa cho Quan để làm thành test tự động.

### 2.6. Khi nào coi là xong?

- Mọi trường hợp tính tay đều khớp kết quả hệ thống.
- Quyền duyệt/khóa lương hoạt động đúng như quy định.
- Form xử lý xung đột lợi ích chạy được.

---

## 3. HOA – CƠ SỞ DỮ LIỆU

### 3.1. Việc này là gì?

Kiểm tra cơ sở dữ liệu có **dựng được sạch, đúng thiết kế, dữ liệu mẫu hợp lý** không, và bổ sung phần còn thiếu.

### 3.2. Code và tài liệu cần đọc

- `backend/src/main/resources/db/migration/` (21 file `V1` đến `V21`)
- `docs/database_design.md` (thiết kế chuẩn)
- `docs/database-outline.md`
- `docs/company_profile.md` (để kiểm tra dữ liệu mẫu)
- `docs/uml/erd/` (sơ đồ quan hệ bảng)

### 3.3. Việc 1 – Kiểm tra code đã có

**(a) Chạy lại cơ sở dữ liệu từ đầu.**

- [ ] Xóa sạch và dựng lại: `docker compose down -v` rồi `docker compose up`.
- [ ] Cả 21 migration chạy xong, không báo lỗi.
- [ ] **Lưu ý:** `V16` xóa schema cũ rồi `V17` dựng lại schema mới. Vậy `V2` đến `V15` (schema cũ) có còn tác dụng gì không? Báo GiHan để quyết định giữ nguyên hay dọn gọn. **Không tự xóa.**

**(b) So khớp bảng thực tế với thiết kế.**

- [ ] Đối chiếu từng bảng trong `database_design.md` với bảng thật: tên bảng, tên cột, kiểu dữ liệu, khóa chính, khóa ngoại, ràng buộc.
- [ ] Chỗ nào lệch thì ghi lại: hoặc sửa, hoặc ghi lý do chấp nhận.

**(c) Thử "phá" bằng dữ liệu sai** (migration `V18` có các trigger và ràng buộc). Thử cố tình tạo dữ liệu sai, xem cơ sở dữ liệu có chặn không:

- [ ] Hai hợp đồng chồng thời gian của cùng một người.
- [ ] Hai lịch làm việc trùng nhau.
- [ ] Sửa dữ liệu của kỳ lương đã khóa.
- [ ] Xóa phòng ban còn nhân viên.
- [ ] Nhập lương âm, ngày kết thúc trước ngày bắt đầu.

**(d) Kiểm tra index (chỉ mục).** Xem các truy vấn hay dùng (chấm công theo ngày, nghỉ phép, kỳ lương, tìm nhân viên) có index phù hợp không.

**(e) Kiểm tra dữ liệu tham chiếu (`V19`):** biểu thuế, tỷ lệ bảo hiểm, lương tối thiểu vùng, ngày lễ 2026, các loại nghỉ phép, loại phụ cấp. Có khớp `payroll_rules.md` không?

**(f) Kiểm tra dữ liệu mẫu (`V20`, `V21`).** Phải khớp `company_profile.md`:

- [ ] Đủ 100 nhân viên, 7 phòng ban.
- [ ] 85 chính thức toàn thời gian, 8 bán thời gian, 5 thử việc, 2 thực tập sinh.
- [ ] Có đủ các tình huống: nhân viên mới, hợp đồng sắp hết hạn, đã nghỉ việc, có người phụ thuộc, Sales có doanh thu, có OT, có đơn nghỉ đang chờ duyệt.
- [ ] Mỗi nhân viên đều có: tài khoản, hợp đồng, lịch làm việc, lương, bảo hiểm, thuế hợp lệ.
- [ ] Quan hệ quản lý – nhân viên không bị vòng tròn (A quản lý B, B lại quản lý A).

### 3.4. Việc 2 – Hoàn thiện phần còn thiếu

1. **Làm API thêm/sửa chức danh và lịch làm việc.** Hiện `job-titles` và `work-schedules` chỉ có chức năng xem (GET). Thêm POST (tạo mới) và PATCH (sửa) trong `backend/src/main/java/com/hrflow/hr/OrgController.java`. Làm xong báo Ngân để làm giao diện.
2. **Chức năng ngừng hoạt động phòng ban** (không xóa hẳn nếu phòng còn nhân viên hoặc còn dữ liệu liên quan). Cũng báo Ngân khi xong.
3. **Thư mục `database/` đang trống.** Bổ sung: script khởi tạo, script sao lưu/khôi phục, script đưa dữ liệu demo về trạng thái ban đầu.
4. **Cập nhật sơ đồ ERD** (`docs/uml/erd`) cho khớp với cơ sở dữ liệu thật.
5. **Dữ liệu demo cho kỳ lương:** chuẩn bị dữ liệu chấm công, nghỉ phép, OT cho 1–2 tháng liên tiếp để khi demo bấm "Tính lương" ra kết quả đẹp. Làm bằng migration mới (`V22` trở đi) hoặc script riêng, **không sửa `V20`, `V21`**.
6. **Viết `docs/employee_data_dictionary.md`:** giải thích từng trường dữ liệu nhân sự (tên trường, ý nghĩa, kiểu, bắt buộc hay không). Đây là yêu cầu có trong `Task.md`.

### 3.5. Kết quả cần nộp

- Danh sách chỗ lệch giữa bảng thật và thiết kế, mỗi dòng ghi "đã sửa" hoặc "chấp nhận vì…".
- Dựng lại từ đầu chạy sạch, dữ liệu mẫu khớp hồ sơ công ty.
- Các migration mới (nếu có) đã được GiHan duyệt.
- Thư mục `database/` có nội dung, ERD đã cập nhật, từ điển dữ liệu.

### 3.6. Khi nào coi là xong?

- Xóa hết rồi dựng lại, web chạy được ngay, đăng nhập được.
- API chức danh, lịch làm việc, ngừng phòng ban chạy được.
- Có dữ liệu demo đủ để chạy thử tính lương.

---

## 4. NGÂN – GIAO DIỆN (FRONTEND)

### 4.1. Việc này là gì?

Đi qua **tất cả các trang** của web bằng từng vai trò, tìm lỗi giao diện, sửa và làm nốt các màn hình còn thiếu.

### 4.2. Code cần đọc

Toàn bộ `frontend/src/`:

- `features/` – các trang (khoảng 30 trang)
- `components/common/` – các thành phần dùng chung (bảng, nút, form…)
- `layouts/` – khung trang, menu bên trái (`Sidebar.jsx`)
- `routes/AppRoutes.jsx` – danh sách trang và ai được vào
- `api/` – nơi gọi backend
- `docs/permission_matrix.md` và `docs/openapi-spec.yaml` – để đối chiếu

### 4.3. Việc 1 – Kiểm tra code đã có

**(a) Lập bảng "trang × vai trò" và thử từng ô.** Dùng 4 vai trò (Admin, HR, Manager, Employee) cộng thêm tài khoản Giám đốc và Finance Manager (có quyền lương). Với mỗi trang kiểm tra:

- [ ] Menu có hiện đúng với quyền không? (Đối chiếu `permission_matrix.md`.)
- [ ] Vào trang không có quyền thì bị chặn đúng không?
- [ ] Trang tải dữ liệu bình thường; khi đang tải, khi lỗi, khi không có dữ liệu đều hiển thị hợp lý.
- [ ] Form có kiểm tra dữ liệu nhập không? (Bắt buộc nhập, đúng định dạng email/số điện thoại, ngày bắt đầu không sau ngày kết thúc, số tiền không âm.)
- [ ] Thông báo lỗi từ backend hiển thị bằng tiếng Việt dễ hiểu.

**(b) Thử trọn các luồng chính từ đầu đến cuối:**

1. Tạo nhân viên → tạo hợp đồng → cấp tài khoản.
2. Chấm công vào/ra → xin điều chỉnh công → quản lý duyệt.
3. Xin nghỉ phép → quản lý duyệt → quỹ phép cập nhật.
4. Đăng ký OT → duyệt → nhập giờ thực tế.
5. Tạo kỳ lương → tính lương → duyệt → khóa → nhân viên xem phiếu lương.

Mở thanh công cụ trình duyệt (F12), xem tab **Console** và **Network** xem có lỗi đỏ không.

**(c) Kiểm tra gọi API.** Mở `docs/openapi-spec.yaml` đối chiếu với các file trong `src/api/`. Xem có tên trường nào gửi/đọc sai không.

**(d) Kiểm tra trên điện thoại và máy tính bảng** (co nhỏ cửa sổ trình duyệt). Kiểm tra cách hiển thị ngày và tiền (`utils/formatDate.js`, `formatCurrency.js`).

**(e) Kiểm tra với dữ liệu nhiều** (100 nhân viên): bảng, phân trang, lọc, tìm kiếm có chạy đúng và không bị chậm không.

### 4.4. Việc 2 – Hoàn thiện phần còn thiếu

1. **Tự làm mới đăng nhập (refresh token).** Backend đã có `/auth/refresh` nhưng `src/api/httpClient.js` chưa dùng. Khi gặp lỗi 401, tự xin token mới; chỉ khi thất bại mới đưa người dùng về trang đăng nhập.
2. **Chấm công:** thêm xem ảnh chấm công (`GET /attendance/{id}/photo`) và xác thực mã QR (`/attendance/qr/verify`) nếu luồng cần.
3. **Quỹ phép:** làm màn hình cho HR xem số ngày phép của cả công ty và điều chỉnh (`/leave-balances`, `/leave-balances/{id}/adjust`). Hiện chỉ có phần "của tôi".
4. **Chức danh và lịch làm việc:** form thêm/sửa trong trang Danh mục (`CatalogPage.jsx`). **Chờ Hoa làm xong API.**
5. **Phòng ban:** nút "Ngừng hoạt động". **Chờ Hoa làm xong API.**
6. **Hợp đồng sắp hết hạn:** hiện cảnh báo (ví dụ còn dưới 30 ngày) ở danh sách hợp đồng và trang tổng quan.
7. **Form xử lý xung đột lợi ích** trong `PayrollDetailPage.jsx`. **Làm cùng Hiếu.**
8. **Báo cáo:** thêm biểu đồ và bộ lọc theo tháng, theo phòng ban cho `ReportsPage.jsx`. **Chờ API báo cáo lương của Hiếu.**
9. **Làm gọn giao diện:**
   - Thống nhất màu sắc, khoảng cách, cỡ chữ, nhãn trạng thái.
   - Soát chính tả tiếng Việt.
   - Trang 404 thân thiện.
   - Hỏi xác nhận trước các thao tác nguy hiểm (xóa, khóa kỳ lương, chấm dứt hợp đồng).
   - Các ô nhập có nhãn rõ ràng, dùng được bằng bàn phím.

### 4.5. Kết quả cần nộp

- Bảng "trang × vai trò" đã tích đủ.
- Chạy `npm run build` không lỗi; đi hết các luồng chính mà Console không báo lỗi đỏ.
- Ảnh chụp màn hình các trang chính (dùng cho demo).
- Danh sách chức năng đã bổ sung (đã commit và push lên `dev`).

### 4.6. Khi nào coi là xong?

- Mỗi vai trò chỉ thấy đúng những gì được phép thấy.
- 5 luồng chính ở mục 4.3(b) chạy trọn vẹn, không lỗi.
- Các màn hình thiếu ở mục 4.4 đã có.

---

## 5. QUAN – KIỂM THỬ

### 5.1. Việc này là gì?

Viết bộ kiểm thử để **chứng minh web chạy đúng**, và bắt lỗi giúp cả nhóm. Hiện chỉ có 2 file test (23 bài, đều là test nhỏ), chưa đủ.

### 5.2. Code và tài liệu cần đọc

- `backend/src/test/java/com/hrflow/` – 2 file test hiện có (`AttendanceCalcTest`, `PayrollEngineTest`)
- `docs/SRS.md` – yêu cầu của hệ thống
- `docs/permission_matrix.md` – ai được làm gì
- `docs/payroll_rules.md` – quy tắc tính lương
- Thư mục `test/` (đang trống) và `.github/workflows/ci.yml` (đang rỗng)

### 5.3. Việc 1 – Kiểm tra hiện trạng

- [ ] Chạy `mvn test` trong thư mục `backend/`, xác nhận 23 bài test hiện tại đều qua.
- [ ] Đọc `SRS.md`, `permission_matrix.md`, `payroll_rules.md`, rút ra danh sách "những điều web phải làm đúng".
- [ ] Lập bảng truy vết: *Yêu cầu → Bài test tương ứng → Đã có test tự động chưa*.

### 5.4. Việc 2 – Viết test và hoàn thiện

**(a) Kế hoạch test.** Tạo `test/test_plan.md` và danh sách test case (file `test/test_cases.md` hoặc Excel). Mỗi test case ghi: mã, chức năng, điều kiện, các bước làm, kết quả mong đợi, kết quả thực tế, đạt/không đạt.

**(b) Test nhỏ cho từng phần tính toán** (unit test). Hiện chưa có test cho:

- `LeaveCalc` (tính ngày nghỉ phép)
- `WorkCalendar` (lịch làm việc, ngày lễ)
- `TimesheetService` (bảng công)
- `OvertimeService` (tách OT ngày thường, nghỉ tuần, lễ, ban đêm)
- Bổ sung thêm cho `PayrollEngine`, **làm cùng Hiếu**: vào làm giữa tháng, thực tập sinh, người không cư trú, mức trần bảo hiểm, làm tròn tiền.

**(c) Test phân quyền (quan trọng nhất).** Với mỗi API, gọi bằng từng vai trò và kiểm tra bị chặn hay cho phép đúng như `permission_matrix.md`. Bắt buộc có:

- [ ] Admin không xem được lương.
- [ ] Người lập bảng lương không duyệt được bảng của mình.
- [ ] Nhân viên không xem được phiếu lương, hợp đồng của người khác.
- [ ] Manager chỉ thấy nhân viên trong nhóm của mình.
- [ ] Chưa đăng nhập hoặc token hết hạn thì bị từ chối.

**(d) Test cả luồng với cơ sở dữ liệu thật** (test tích hợp). Nên dùng Testcontainers (tự bật PostgreSQL khi chạy test). Kiểm tra file `backend/pom.xml` đã có chưa; chưa có thì thêm. Các luồng cần test:

1. Đăng nhập → tạo nhân viên.
2. Chấm công.
3. Xin nghỉ → duyệt.
4. Đăng ký OT → duyệt → nhập giờ thực tế.
5. Kỳ lương đi từ DRAFT tới LOCKED.

**(e) Test các tình huống bất thường:**

- Xin nghỉ nhiều hơn số ngày phép còn lại.
- Hai đơn nghỉ trùng ngày.
- OT trùng ca làm việc.
- Chấm công vào hai lần liên tiếp.
- Sửa dữ liệu của kỳ lương đã khóa.
- Dữ liệu nhập sai định dạng, thiếu trường bắt buộc.

**(f) Viết CI** (kiểm tra tự động mỗi khi có người push code lên `dev`) trong `.github/workflows/ci.yml`:

- Backend: chạy `mvn test`.
- Frontend: chạy `npm ci` và `npm run build`.

**(g) Test giao diện.** Tối thiểu: một bộ kịch bản thử tay theo các luồng chính. Nếu còn thời gian: viết thêm test tự động bằng Playwright cho luồng *đăng nhập → xin nghỉ → duyệt → xem phiếu lương*.

**(h) Đo tốc độ cơ bản:** tải danh sách 100 nhân viên và tính lương cả công ty mất bao lâu.

### 5.5. Kết quả cần nộp

- `test/test_plan.md`, danh sách test case, bảng truy vết yêu cầu.
- Test tự động chạy thành công (màu xanh) trong CI.
- Tổng kết kiểm thử: bao nhiêu case đạt, bao nhiêu không đạt, danh sách lỗi (ghi trong `docs/bug_log.md`), lỗi nào đã sửa.
- Kịch bản demo đã chạy thử từ đầu đến cuối ít nhất một lần.

### 5.6. Khi nào coi là xong?

- Có test phân quyền cho các API quan trọng, đều chạy qua.
- Có test tích hợp cho ít nhất 3 luồng chính.
- CI chạy được và báo xanh.
- Mọi lỗi tìm thấy đều đã ghi vào `bug_log.md`.

---

## 6. Ai cần chờ ai?

| Người cần | Phải chờ | Việc cần chờ |
|---|---|---|
| Ngân | Hoa | API thêm/sửa chức danh, lịch làm việc; API ngừng phòng ban |
| Ngân | Hiếu | API báo cáo lương; backend xử lý xung đột lợi ích |
| Quan | Hiếu | Danh sách trường hợp tính lương bằng tay (để làm test) |
| Quan | Hoa | Dữ liệu mẫu ổn định (để test không bị đổi theo) |
| Hiếu | Hoa | Dữ liệu chấm công, OT, nghỉ phép demo để chạy thử tính lương |

**Mẹo:** trong lúc chờ, hãy làm các việc "Kiểm tra code đã có" trước (mục Việc 1 của mỗi người), vì phần này không phụ thuộc ai.

---

## 7. Lịch gợi ý

| Giai đoạn | Việc chính |
|---|---|
| **Tuần 1** | Cả 4 bạn chạy được web, làm **Việc 1 (kiểm tra)** của mình, ghi lỗi vào `bug_log.md`. Hoa kiểm tra migration và dữ liệu mẫu **trước tiên** vì mọi người dùng chung dữ liệu này. |
| **Tuần 2** | Hiếu sửa lỗi lương, làm báo cáo và form xung đột. Hoa làm API chức danh, lịch, phòng ban. Ngân làm các màn hình còn thiếu khi API sẵn sàng. Quan viết test cho các phần đã ổn định và viết CI. |
| **Tuần 3** | Quan chạy toàn bộ test, báo lỗi ngược lại cho từng người. Mọi người sửa lỗi. GiHan xem lại các commit trên `dev`, chạy thử kịch bản demo từ đầu đến cuối. |

---

## 8. Danh sách kiểm tra cuối cùng (GiHan dùng để nghiệm thu)

- [ ] Dựng lại cơ sở dữ liệu từ đầu và chạy web thành công.
- [ ] Đăng nhập được bằng cả 6 loại tài khoản (Admin, HR, Manager, Employee, Giám đốc, Finance Manager).
- [ ] Mỗi vai trò chỉ thấy và làm được đúng việc của mình.
- [ ] Luồng chấm công, nghỉ phép, OT chạy trọn vẹn.
- [ ] Tính lương, duyệt, khóa, xem phiếu lương chạy trọn vẹn; kết quả khớp tính tay.
- [ ] Không còn lỗi mức nghiêm trọng trong `bug_log.md`.
- [ ] Test tự động và CI báo xanh.
- [ ] Các commit của 4 bạn trên `dev` đã được xem lại, không còn xung đột (conflict).
- [ ] Kịch bản demo chạy được từ đầu đến cuối.
