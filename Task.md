# KẾ HOẠCH PHÂN CÔNG HOÀN THIỆN DỰ ÁN HRFlow

## 1. Mục tiêu dự án

Hoàn thiện HRFlow – hệ thống web quản lý nhân sự, chấm công và tính lương tự động cho một công ty Marketing giả lập.

Dự án cần hoàn thành các chức năng chính:

- Quản lý thông tin công ty, phòng ban, chức vụ và nhân sự.
- Quản lý hợp đồng, lương cơ bản, phụ cấp và chính sách lương.
- Quản lý chấm công, nghỉ phép và đăng ký làm thêm giờ.
- Tính lương tự động theo dữ liệu chấm công và quy định của công ty.
- Quản lý bảng lương, phê duyệt bảng lương và tạo phiếu lương cho từng nhân viên.
- Xây dựng cơ sở dữ liệu có dữ liệu mẫu đủ thực tế để kiểm thử.
- Kiểm thử chức năng, phân quyền, tính toán lương và các trường hợp ngoại lệ.
- Hoàn thiện giao diện, tích hợp frontend với backend và chuẩn bị kịch bản demo.

Mục tiêu cuối cùng là xây dựng được một website có thể sử dụng xuyên suốt từ quản lý nhân sự đến chấm công, tính lương và xem phiếu lương, thay vì chỉ có khung giao diện hoặc các API riêng lẻ.

## 2. Phân công công việc cho 5 thành viên

### THÀNH VIÊN 1 – TEAM LEADER / BACKEND LEAD / TÍCH HỢP HỆ THỐNG

**Người phụ trách:** GiHan – Team Leader.

**Vai trò:** Quản lý dự án, thống nhất nghiệp vụ, phụ trách backend nền tảng, kiểm soát chất lượng và tích hợp các phần do thành viên thực hiện.

#### A. Những công việc đã thực hiện

- Xây dựng định hướng và phạm vi dự án HRFlow.
- Chuẩn bị khung dự án và cấu trúc thư mục backend, frontend, database, docs và test.
- Thiết lập môi trường phát triển và xử lý các vấn đề chạy dự án bằng Docker.
- Cấu hình và kiểm tra các dịch vụ backend, frontend, PostgreSQL và worker.
- Xử lý lỗi health check của frontend trong Docker Compose.
- Chuẩn bị các tài liệu phân tích và thiết kế ban đầu, bao gồm SRS, quy tắc nghiệp vụ và cấu trúc API theo tiến độ đã thực hiện.
- Định hướng sử dụng Java/Spring Boot và PostgreSQL cho backend.
- Xác định phạm vi các chức năng nhân sự, chấm công, nghỉ phép, làm thêm giờ, tính lương và phiếu lương.

Lưu ý: Những công việc trên là phần nền tảng đã chuẩn bị. Các chức năng nghiệp vụ chỉ được xem là hoàn thành khi đã có code, tích hợp và kiểm thử thực tế.

#### B. Công việc tiếp tục phụ trách

1. **Quản lý và thống nhất nghiệp vụ**
   - Thống nhất mô hình công ty Marketing giả lập.
   - Xác định các quy tắc tính lương chung để các thành viên không tự xây dựng công thức khác nhau.
   - Xác định luồng xử lý từ dữ liệu nhân sự đến bảng lương và phiếu lương.
   - Chốt quy ước API, cấu trúc response, cách xử lý lỗi và phân quyền.

2. **Backend nền tảng**
   - Kiểm tra cấu hình Spring Boot, kết nối PostgreSQL và Flyway.
   - Hoàn thiện đăng nhập, xác thực JWT và phân quyền theo vai trò.
   - Thống nhất cấu trúc entity, repository, service, controller và DTO.
   - Kiểm tra tính nhất quán giữa API và database.
   - Hỗ trợ các thành viên xử lý lỗi tích hợp.

3. **Tích hợp hệ thống**
   - Tích hợp code từ các thành viên thông qua Git.
   - Review Pull Request và kiểm tra chất lượng code.
   - Xử lý xung đột khi nhiều thành viên sửa cùng một file.
   - Kiểm tra luồng nghiệp vụ xuyên suốt.
   - Kiểm tra cấu hình Docker và hướng dẫn chạy dự án.

4. **Quản lý tiến độ**
   - Theo dõi nhiệm vụ và deadline của từng thành viên.
   - Yêu cầu mỗi thành viên cung cấp code, tài liệu và kết quả kiểm thử.
   - Tổ chức kiểm thử tích hợp và chuẩn bị demo cuối kỳ.

#### C. Đầu ra cần bàn giao

- Tài liệu kiến trúc và quy ước code.
- Ma trận phân quyền.
- Backend nền tảng hoạt động ổn định.
- Danh sách API đã tích hợp.
- Các Pull Request đã review.
- Checklist nghiệm thu toàn bộ dự án.
- Kịch bản demo và hướng dẫn chạy dự án.

---

### THÀNH VIÊN 2 – NGHIỆP VỤ CÔNG TY MARKETING / QUẢN LÝ NHÂN SỰ / DATABASE

**Người phụ trách:** Lệ Hoa

**Vai trò:** Xây dựng dữ liệu doanh nghiệp giả lập có tính thực tế và hoàn thiện chức năng quản lý nhân sự.

#### A. Nhiệm vụ 1: Xây dựng công ty Marketing giả lập

Xây dựng một công ty Marketing giả lập có tên, mô hình tổ chức và quy mô nhân sự rõ ràng.

Đề xuất cơ cấu công ty gồm:

| Phòng ban | Chức năng chính | Các vị trí tiêu biểu |
|---|---|---|
| Ban Giám đốc | Điều hành và phê duyệt | CEO, Giám đốc vận hành |
| Human Resources (HR) | Quản lý nhân sự, hợp đồng và chính sách | HR Manager, HR Executive |
| Content Marketing | Sản xuất nội dung | Content Lead, Content Writer |
| Design & Creative | Thiết kế hình ảnh và video | Design Lead, Graphic Designer, Video Editor |
| Digital Marketing | Chạy quảng cáo và tối ưu chiến dịch | Digital Marketing Lead, Ads Specialist |
| Account & Client Service | Quản lý khách hàng và dự án | Account Manager, Account Executive |
| Sales & Business Development | Tìm kiếm khách hàng và doanh thu | Sales Lead, Sales Executive |
| Finance & Accounting | Theo dõi chi phí và đối soát lương | Accountant, Payroll Specialist |

Có thể điều chỉnh cơ cấu để phù hợp với quy mô công ty giả lập và thời gian thực hiện dự án.

#### B. Nhiệm vụ 2: Xây dựng dữ liệu nhân sự

Chuẩn bị dữ liệu mẫu cho từng nhân viên, bao gồm:

- Mã nhân viên, họ tên, email và số điện thoại giả lập.
- Phòng ban, chức vụ, quản lý trực tiếp.
- Ngày vào làm và trạng thái làm việc.
- Loại hợp đồng và thời hạn hợp đồng.
- Lương cơ bản, phụ cấp và thông tin áp dụng chính sách lương.
- Tài khoản đăng nhập và vai trò tương ứng.
- Ngày nghỉ phép, lịch làm việc và các thông tin cần thiết cho chấm công.

Dữ liệu cần có các tình huống khác nhau như nhân viên mới, nhân viên đang làm việc, nhân viên sắp hết hạn hợp đồng và nhân viên đã nghỉ việc.

#### C. Nhiệm vụ 3: Hoàn thiện chức năng quản lý nhân sự

- Xem danh sách và chi tiết nhân viên.
- Thêm, sửa và cập nhật trạng thái nhân viên.
- Tìm kiếm và lọc theo phòng ban, chức vụ và trạng thái.
- Quản lý phòng ban, chức vụ và quản lý trực tiếp.
- Quản lý hợp đồng lao động.
- Hiển thị lịch sử thay đổi thông tin quan trọng nếu phạm vi dự án hỗ trợ.

#### D. Nhiệm vụ 4: Phối hợp database

- Thiết kế dữ liệu cho phòng ban, chức vụ, nhân viên và hợp đồng.
- Xác định khóa chính, khóa ngoại, ràng buộc và quan hệ giữa các bảng.
- Phối hợp với Team Leader để thống nhất schema.
- Chuẩn bị dữ liệu mẫu có liên kết hợp lệ với tài khoản, hợp đồng và bảng lương.
- Không tự ý sửa migration đã được tích hợp mà chưa thống nhất với Team Leader.

#### E. Đầu ra cần bàn giao

- `docs/company_profile.md`: hồ sơ công ty Marketing giả lập.
- `docs/organization_structure.md`: sơ đồ tổ chức và cơ cấu phòng ban.
- `docs/employee_data_dictionary.md`: mô tả các trường dữ liệu nhân sự.
- Dữ liệu mẫu nhân sự và hợp đồng.
- Chức năng quản lý nhân sự hoàn chỉnh.
- Danh sách API và kết quả kiểm thử module nhân sự.

---

### THÀNH VIÊN 3 – CHẤM CÔNG / NGHỈ PHÉP / LÀM THÊM GIỜ

**Người phụ trách:** Hiếu

**Vai trò:** Hoàn thiện toàn bộ nghiệp vụ ghi nhận thời gian làm việc, đơn nghỉ phép và đăng ký làm thêm giờ.

#### A. Nhiệm vụ 1: Hoàn thiện chức năng chấm công

- Check-in và check-out.
- Ghi nhận thời gian vào làm, ra về.
- Xác định ngày công thực tế.
- Xử lý đi muộn, về sớm và thiếu lượt chấm công.
- Xem lịch sử chấm công theo ngày và theo tháng.
- Cho phép quản lý hoặc HR xem dữ liệu chấm công của nhân viên theo quyền.
- Kiểm tra dữ liệu chấm công bất thường hoặc trùng lặp.

Nếu dự án triển khai QR hoặc ảnh chụp minh chứng, cần kiểm tra tính hợp lệ của token, thời hạn QR và quyền truy cập ảnh theo phạm vi đã thống nhất.

#### B. Nhiệm vụ 2: Nghỉ phép (PTO)

- Nhân viên tạo và gửi đơn nghỉ phép.
- Kiểm tra ngày bắt đầu, ngày kết thúc và lý do.
- Quản lý trực tiếp phê duyệt hoặc từ chối.
- HR xử lý theo quy định khi nhân viên không có quản lý trực tiếp.
- Nhân viên xem trạng thái đơn.
- Xử lý rút đơn hoặc hủy đơn theo trạng thái cho phép.
- Cập nhật số ngày phép theo chính sách được thống nhất.

#### C. Nhiệm vụ 3: Làm thêm giờ (OT)

- Quản lý đăng thông tin lịch làm thêm giờ cần nhân sự.
- Nhân viên đăng ký tham gia.
- Quản lý phê duyệt hoặc từ chối.
- Ghi nhận số giờ làm thêm thực tế.
- Phân biệt giờ OT được duyệt và giờ OT thực tế đủ điều kiện tính lương.
- Chuyển dữ liệu OT hợp lệ sang module tính lương.

#### D. Nhiệm vụ 4: Tích hợp dữ liệu với tính lương

- Cung cấp API truy xuất ngày công, số giờ làm và dữ liệu nghỉ phép.
- Đảm bảo kỳ công đã khóa không bị thay đổi tùy tiện.
- Thống nhất với thành viên phụ trách Payroll về quy tắc xử lý thiếu dữ liệu và dữ liệu điều chỉnh.
- Không tự tính lại lương trong module chấm công; chỉ cung cấp dữ liệu đầu vào hợp lệ.

#### E. Đầu ra cần bàn giao

- Chức năng chấm công, nghỉ phép và OT.
- API và tài liệu nghiệp vụ của module.
- Dữ liệu mẫu có đủ trường hợp bình thường và bất thường.
- Bộ test case cho check-in/check-out, PTO và OT.
- Hướng dẫn kiểm thử các trạng thái đơn và quyền phê duyệt.

---

### THÀNH VIÊN 4 – PAYROLL ENGINEER / TÍNH LƯƠNG / QUY TẮC LƯƠNG

**Người phụ trách:** Quan

**Vai trò:** Hoàn thiện nghiệp vụ tính lương tự động, đảm bảo công thức rõ ràng và kết quả có thể đối chiếu.

#### A. Nhiệm vụ 1: Xây dựng chính sách lương công ty Marketing

Xác định chính sách áp dụng cho các nhóm nhân sự:

- Nhân viên Content.
- Nhân viên Design & Creative.
- Nhân viên Digital Marketing.
- Nhân viên Account.
- Nhân viên Sales.
- Quản lý và các vị trí hỗ trợ.

Mỗi nhóm có thể có mức lương, phụ cấp và cơ chế thưởng khác nhau, nhưng tất cả phải tuân theo một bộ quy tắc chung được công ty phê duyệt.

Ví dụ:
- Lương theo tháng.
- Phụ cấp trách nhiệm, ăn trưa hoặc gửi xe.
- Hoa hồng Sales theo doanh thu đủ điều kiện.
- Thưởng KPI theo kết quả được phê duyệt.
- Tiền OT theo số giờ đủ điều kiện.
- Khấu trừ theo quy định và dữ liệu hợp lệ.

Không tự đặt các mức thuế, bảo hiểm hoặc hệ số OT tùy ý. Các tham số này cần được thống nhất và ghi rõ nguồn hoặc giả định của dự án.

#### B. Nhiệm vụ 2: Hoàn thiện công thức tính lương

Xác định rõ các thành phần:

- Lương cơ bản theo hợp đồng.
- Lương theo ngày công thực tế hoặc công thức lương tháng đã được thống nhất.
- Phụ cấp.
- Tiền OT.
- Hoa hồng và thưởng KPI.
- Các khoản khấu trừ.
- Thuế và bảo hiểm nếu thuộc phạm vi mô phỏng.
- Tổng thu nhập trước khấu trừ.
- Thực lĩnh sau khấu trừ.

Ví dụ công thức minh họa:

`Tổng thu nhập = Lương theo công + Phụ cấp + Tiền OT + Thưởng + Hoa hồng`

`Thực lĩnh = Tổng thu nhập - Tổng các khoản khấu trừ`

Công thức chi tiết, mẫu số ngày công chuẩn, cách làm tròn và điều kiện hưởng từng khoản phải được ghi trong `payroll_rules.md`.

#### C. Nhiệm vụ 3: Hoàn thiện quy trình tính lương

- Chọn kỳ lương.
- Lấy dữ liệu nhân sự, hợp đồng, công, nghỉ phép và OT.
- Kiểm tra dữ liệu đầu vào.
- Tính lương tự động.
- Hiển thị bảng chi tiết từng khoản thu nhập và khấu trừ.
- Cho phép tính lại khi bảng lương chưa khóa và dữ liệu đầu vào thay đổi hợp lệ.
- Hỗ trợ phê duyệt bảng lương.
- Khóa kỳ lương theo quyền và trạng thái.
- Lưu lịch sử tính lương hoặc điều chỉnh theo thiết kế đã thống nhất.

#### D. Nhiệm vụ 4: Xây dựng dữ liệu kiểm chứng công thức

Chuẩn bị ít nhất các hồ sơ mẫu:

- Nhân viên đủ công, không OT.
- Nhân viên đi muộn hoặc thiếu công.
- Nhân viên có nghỉ phép.
- Nhân viên có OT được duyệt.
- Nhân viên có thưởng KPI.
- Nhân viên Sales có hoa hồng.
- Nhân viên có khoản khấu trừ.
- Nhân viên mới hoặc nghỉ việc giữa kỳ.

Với mỗi hồ sơ, ghi rõ dữ liệu đầu vào, công thức áp dụng và kết quả lương dự kiến để đối chiếu với hệ thống.

#### E. Đầu ra cần bàn giao

- `docs/payroll_rules.md`: chính sách và công thức tính lương.
- Module tính lương hoạt động với dữ liệu thật trong database.
- API tính toán, xem chi tiết và quản lý trạng thái bảng lương.
- Dữ liệu kiểm chứng và bảng kết quả lương mẫu.
- Bộ test case về công thức và các trường hợp biên.

---

### THÀNH VIÊN 5 – FRONTEND HR/ADMIN / PHIẾU LƯƠNG / DATABASE TESTING

**Người phụ trách:** Ngân

**Vai trò:** Hoàn thiện giao diện quản trị và phiếu lương, phối hợp chuẩn bị dữ liệu và thực hiện kiểm thử toàn hệ thống.

#### A. Nhiệm vụ 1: Hoàn thiện giao diện HR/Admin

- Dashboard tổng quan nhân sự và bảng lương.
- Trang danh sách và chi tiết nhân viên.
- Trang quản lý phòng ban, chức vụ và hợp đồng.
- Trang quản lý chấm công và đơn nghỉ phép.
- Trang quản lý OT.
- Trang bảng lương theo kỳ.
- Trang xem chi tiết các khoản lương.
- Hiển thị trạng thái xử lý, thông báo lỗi và trạng thái tải dữ liệu.
- Ẩn hoặc vô hiệu hóa chức năng người dùng không có quyền sử dụng.

Các trang phải gọi API backend thật, không chỉ hiển thị dữ liệu mẫu tĩnh.

#### B. Nhiệm vụ 2: Hoàn thiện trang bảng lương và phiếu lương

- Hiển thị bảng lương theo kỳ và trạng thái.
- Xem chi tiết lương của từng nhân viên.
- Hiển thị lương cơ bản, ngày công, phụ cấp, OT, thưởng, hoa hồng và khấu trừ.
- Hiển thị tổng thu nhập và thực lĩnh.
- Tạo phiếu lương riêng cho từng nhân viên.
- Cho phép xem hoặc in phiếu lương.
- Kiểm tra phiếu lương có khớp với bản ghi payroll trong database hay không.
- Đảm bảo nhân viên chỉ xem phiếu lương của chính mình; HR hoặc người được cấp quyền mới được xem dữ liệu theo phạm vi quản lý.

Trong phạm vi dự án, “phiếu lương” là tài liệu chi tiết thu nhập và khấu trừ của nhân viên, không phải hóa đơn bán hàng cho khách hàng.

#### C. Nhiệm vụ 3: Chuẩn bị database và dữ liệu test phối hợp

- Phối hợp với Team Leader và các thành viên để kiểm tra schema.
- Kiểm tra các khóa ngoại và quan hệ giữa nhân viên, hợp đồng, chấm công và bảng lương.
- Chuẩn bị dữ liệu mẫu đủ để chạy demo.
- Tạo checklist dữ liệu cần có trước khi chạy từng test case.
- Không tự sửa schema hoặc migration nếu chưa thống nhất.

#### D. Nhiệm vụ 4: Kiểm thử giao diện và tích hợp

- Kiểm tra giao diện trên các kích thước màn hình phù hợp.
- Kiểm tra form nhập liệu, validate và thông báo lỗi.
- Kiểm tra quyền xem/sửa/xóa theo vai trò.
- Kiểm tra luồng đăng nhập, đăng xuất và hết hạn phiên.
- Kiểm tra API lỗi, dữ liệu trống và trường hợp tải chậm.
- Kiểm tra phiếu lương trên màn hình và khi in.
- Ghi nhận lỗi, bước tái hiện và kết quả mong đợi.

#### E. Đầu ra cần bàn giao

- Các trang HR/Admin hoàn thiện và tích hợp API.
- Trang bảng lương, chi tiết lương và phiếu lương.
- Dữ liệu mẫu phục vụ demo.
- Checklist kiểm thử giao diện.
- Danh sách bug và kết quả kiểm thử.
- Hướng dẫn sử dụng các chức năng đã hoàn thiện.

---

## 3. Kế hoạch xây dựng công ty Marketing và dữ liệu mẫu

### 3.1. Quy mô công ty giả lập

Đề xuất mô hình khoảng 30–50 nhân sự để có đủ dữ liệu thử nghiệm nhưng vẫn phù hợp với quy mô dự án sinh viên.

Có thể phân bổ nhân sự minh họa như sau:

| Phòng ban | Số lượng dự kiến |
|---|---:|
| Ban Giám đốc | 2 |
| HR | 3 |
| Content Marketing | 8 |
| Design & Creative | 6 |
| Digital Marketing | 7 |
| Account & Client Service | 6 |
| Sales & Business Development | 5 |
| Finance & Accounting | 3 |
| **Tổng cộng** | **40** |

Đây là số lượng giả lập để xây dựng dữ liệu, không phải dữ liệu của doanh nghiệp có thật.

### 3.2. Dữ liệu cần chuẩn bị

- Hồ sơ công ty và cơ cấu phòng ban.
- Danh sách chức vụ và quan hệ quản lý.
- Danh sách nhân viên và tài khoản đăng nhập.
- Hợp đồng, lương cơ bản và phụ cấp.
- Lịch làm việc và ngày nghỉ.
- Dữ liệu chấm công theo kỳ.
- Đơn PTO và đăng ký OT.
- Kết quả KPI hoặc doanh thu Sales giả lập.
- Các kỳ lương và phiếu lương.
- Các tài khoản có vai trò Employee, Manager, HR và Admin.

Tất cả thông tin cá nhân, email, số điện thoại và dữ liệu lương dùng để demo phải là dữ liệu giả lập.

## 4. Định hướng thiết kế database

Database cần phục vụ toàn bộ luồng nghiệp vụ, không chỉ lưu thông tin nhân viên.

Các nhóm bảng đề xuất:

| Nhóm | Bảng tham khảo | Mục đích |
|---|---|---|
| Tài khoản và quyền | `users`, `roles`, `user_roles` | Đăng nhập và phân quyền |
| Cơ cấu tổ chức | `departments`, `positions` | Phòng ban và chức vụ |
| Nhân sự | `employees`, `contracts` | Hồ sơ và hợp đồng |
| Lịch làm việc | `work_schedules`, `holidays` | Lịch làm việc và ngày nghỉ |
| Chấm công | `attendance_records`, `attendance_adjustments` | Ghi nhận và điều chỉnh công |
| Nghỉ phép | `leave_types`, `leave_requests`, `leave_balances` | Loại phép, đơn phép và số dư |
| Làm thêm giờ | `overtime_schedules`, `overtime_registrations` | Lịch OT và đăng ký tham gia |
| Chính sách lương | `salary_components`, `payroll_rules` | Các thành phần và quy tắc lương |
| Bảng lương | `payroll_periods`, `payroll_runs`, `payroll_items` | Kỳ lương, lần tính và chi tiết từng nhân viên |
| Phiếu lương | `payslips` hoặc dữ liệu phiếu lương liên kết với payroll item | Xem và in phiếu lương |
| Thưởng và hoa hồng | `performance_bonuses`, `sales_commissions` | Lưu các khoản thưởng đủ điều kiện |
| Theo dõi hệ thống | `audit_logs` | Theo dõi thay đổi quan trọng |

Tên bảng trên là đề xuất thiết kế, cần đối chiếu với schema hiện tại trước khi thêm mới để tránh trùng lặp.

### Nguyên tắc thiết kế database

1. Mỗi nhân viên phải có mã định danh duy nhất.
2. Hợp đồng phải liên kết với nhân viên và có ngày hiệu lực.
3. Dữ liệu chấm công phải liên kết với nhân viên và ngày làm việc.
4. Đơn PTO/OT phải có trạng thái và thông tin người phê duyệt.
5. Mỗi kỳ lương phải xác định rõ khoảng thời gian.
6. Bảng lương phải lưu được chi tiết từng khoản thu nhập và khấu trừ.
7. Phiếu lương phải lấy dữ liệu từ kết quả tính lương đã lưu, không tự tính lại theo công thức riêng ở frontend.
8. Kỳ lương đã khóa phải được bảo vệ khỏi các thay đổi trực tiếp không hợp lệ.
9. Migration phải được quản lý bằng Flyway; không xóa hoặc sửa migration đã áp dụng ở môi trường có dữ liệu nếu chưa có kế hoạch xử lý phù hợp.
10. Không lưu mật khẩu dạng văn bản thuần; chỉ lưu mật khẩu đã băm.

## 5. Kế hoạch kiểm thử toàn hệ thống

### 5.1. Kiểm thử đăng nhập và phân quyền

- Đăng nhập bằng tài khoản hợp lệ.
- Đăng nhập sai mật khẩu.
- Tài khoản không hoạt động đăng nhập.
- Nhân viên truy cập trang quản trị.
- Nhân viên cố xem phiếu lương của người khác.
- Manager truy cập dữ liệu ngoài phạm vi được phép.
- Token hết hạn hoặc không hợp lệ.

**Kết quả mong đợi:** hệ thống từ chối truy cập trái quyền và không làm lộ dữ liệu nhạy cảm.

### 5.2. Kiểm thử quản lý nhân sự

- Thêm nhân viên hợp lệ.
- Tạo nhân viên trùng mã hoặc email.
- Thiếu trường bắt buộc.
- Chuyển nhân viên sang phòng ban khác.
- Cập nhật chức vụ và quản lý trực tiếp.
- Nhân viên hết hạn hợp đồng.
- Nhân viên nghỉ việc nhưng vẫn còn dữ liệu lịch sử lương.

**Kết quả mong đợi:** dữ liệu hợp lệ được lưu, dữ liệu sai bị từ chối và lịch sử nghiệp vụ được bảo toàn.

### 5.3. Kiểm thử chấm công

- Check-in đúng giờ.
- Check-out hợp lệ.
- Check-in hai lần trong cùng một phiên làm việc.
- Check-out khi chưa check-in.
- Đi muộn hoặc về sớm.
- Thiếu lượt chấm công.
- Chấm công vào ngày nghỉ.
- Điều chỉnh dữ liệu chấm công.
- Truy cập dữ liệu chấm công của người khác khi không có quyền.

**Kết quả mong đợi:** hệ thống xử lý đúng trạng thái và cung cấp dữ liệu chính xác cho tính lương.

### 5.4. Kiểm thử nghỉ phép và OT

- Gửi đơn nghỉ phép hợp lệ.
- Gửi đơn có ngày kết thúc trước ngày bắt đầu.
- Xin nghỉ vượt số dư phép theo chính sách.
- Quản lý duyệt hoặc từ chối đơn.
- Người không có quyền cố duyệt đơn.
- Nhân viên đăng ký OT vào lịch đã được công bố.
- Đăng ký OT trùng hoặc sau khi hết hạn.
- OT bị từ chối nhưng vẫn bị tính vào lương.
- OT được duyệt nhưng thiếu dữ liệu giờ thực tế.

**Kết quả mong đợi:** chỉ dữ liệu hợp lệ theo quy tắc mới được chuyển sang tính lương.

### 5.5. Kiểm thử tính lương

- Nhân viên đủ công và không có khoản phát sinh.
- Nhân viên thiếu công.
- Nhân viên nghỉ phép.
- Nhân viên có OT hợp lệ.
- Nhân viên có thưởng KPI.
- Nhân viên Sales có hoa hồng.
- Nhân viên có khoản khấu trừ.
- Nhân viên vào làm hoặc nghỉ việc giữa kỳ.
- Dữ liệu đầu vào thiếu hoặc không hợp lệ.
- Tính lại bảng lương trước khi khóa.
- Cố tính lại kỳ lương đã khóa.
- Hai yêu cầu tính lương được gửi đồng thời.

**Kết quả mong đợi:** kết quả khớp với bộ dữ liệu kiểm chứng, không tính trùng khoản thu nhập và không cho phép thay đổi trái phép kỳ lương đã khóa.

### 5.6. Kiểm thử phiếu lương

- Tạo phiếu lương cho nhân viên.
- Kiểm tra tổng thu nhập, các khoản khấu trừ và thực lĩnh.
- Đối chiếu phiếu lương với bảng lương.
- Nhân viên xem phiếu lương của mình.
- Nhân viên cố xem phiếu lương của người khác.
- In phiếu lương.
- Kiểm tra phiếu lương khi không có dữ liệu hoặc kỳ lương chưa được phê duyệt.

**Kết quả mong đợi:** phiếu lương khớp dữ liệu đã lưu và tuân thủ phân quyền.

### 5.7. Kiểm thử database và tích hợp

- Chạy migration trên database mới.
- Khởi động lại dịch vụ và kiểm tra dữ liệu còn nguyên.
- Tạo dữ liệu có khóa ngoại không hợp lệ.
- Kiểm tra giao dịch khi lưu bảng lương thất bại giữa chừng.
- Kiểm tra frontend khi backend trả về lỗi.
- Kiểm tra kết nối giữa backend, database và worker.
- Kiểm tra dữ liệu mẫu sau khi triển khai bằng Docker.

### 5.8. Tài liệu test cần bàn giao

Mỗi test case cần có:

- Mã test case.
- Chức năng được kiểm tra.
- Điều kiện trước khi chạy.
- Dữ liệu đầu vào.
- Các bước thực hiện.
- Kết quả mong đợi.
- Kết quả thực tế.
- Trạng thái Pass/Fail.
- Mức độ nghiêm trọng nếu phát hiện lỗi.

Đề xuất lưu tại `docs/test_plan.md` và `docs/test_cases.md`.

## 6. Kế hoạch phối hợp và nghiệm thu

### Giai đoạn 1: Thống nhất nghiệp vụ và thiết kế

- Thành viên 1: rà soát kiến trúc và phạm vi dự án.
- Thành viên 2: chốt hồ sơ công ty, phòng ban và dữ liệu nhân sự.
- Thành viên 3: chốt quy trình chấm công, PTO và OT.
- Thành viên 4: chốt công thức và quy tắc tính lương.
- Thành viên 5: rà soát giao diện và lập danh sách trang cần hoàn thiện.

**Điều kiện hoàn thành:** mọi thành viên sử dụng chung một bộ quy tắc nghiệp vụ và schema đã thống nhất.

### Giai đoạn 2: Phát triển từng module

- Thành viên 1 hoàn thiện backend nền tảng và hỗ trợ tích hợp.
- Thành viên 2 hoàn thiện nhân sự và dữ liệu tổ chức.
- Thành viên 3 hoàn thiện chấm công, PTO và OT.
- Thành viên 4 hoàn thiện Payroll Engine.
- Thành viên 5 hoàn thiện giao diện HR/Admin và phiếu lương.

**Điều kiện hoàn thành:** mỗi module có code, API hoặc giao diện cần thiết, dữ liệu thử và test case tương ứng.

### Giai đoạn 3: Tích hợp và kiểm thử

- Tích hợp các module vào nhánh phát triển chung.
- Kiểm tra dữ liệu đầu vào giữa các module.
- Chạy kiểm thử chức năng và phân quyền.
- Đối chiếu công thức tính lương bằng dữ liệu mẫu.
- Sửa lỗi và chạy lại các test bị ảnh hưởng.
- Kiểm tra dự án chạy được trên môi trường Docker.

### Giai đoạn 4: Demo và bàn giao

- Chuẩn bị tài khoản cho từng vai trò.
- Chuẩn bị dữ liệu nhân sự, chấm công và bảng lương mẫu.
- Demo quy trình từ tạo nhân viên đến in phiếu lương.
- Hoàn thiện README, hướng dẫn cài đặt và hướng dẫn sử dụng.
- Bàn giao source code, tài liệu và kết quả kiểm thử.

## 7. Quy tắc làm việc chung với Git

- Mỗi thành viên làm việc trên nhánh riêng, ví dụ `feature/employee-management`, `feature/attendance`, `feature/payroll`, `feature/payslip`.
- Không tự ý sửa cấu trúc database hoặc công thức nghiệp vụ đã thống nhất.
- Trước khi làm chức năng mới, kiểm tra code hiện tại để tránh viết lại chức năng đã có.
- Mỗi Pull Request phải mô tả thay đổi, cách kiểm thử và các file ảnh hưởng.
- Team Leader review trước khi merge vào nhánh chung.
- Không commit file `.env`, mật khẩu, JWT secret hoặc dữ liệu nhạy cảm.
- Chỉ đánh dấu hoàn thành khi chức năng chạy được và đã có kết quả kiểm thử.

## 8. Tiêu chí nghiệm thu cuối cùng

Dự án chỉ được xem là hoàn thành khi:

1. Công ty Marketing giả lập có cơ cấu phòng ban và dữ liệu nhân sự nhất quán.
2. Các vai trò người dùng đăng nhập và được phân quyền đúng.
3. Quản lý nhân sự và hợp đồng hoạt động.
4. Chấm công, PTO và OT hoạt động theo quy tắc đã thống nhất.
5. Payroll Engine tính đúng trên các bộ dữ liệu kiểm chứng.
6. Bảng lương có quy trình phê duyệt và khóa phù hợp.
7. Nhân viên xem và in được phiếu lương đúng quyền.
8. Database có migration, dữ liệu mẫu và ràng buộc hợp lệ.
9. Các test case quan trọng đã chạy và lỗi nghiêm trọng đã được xử lý.
10. Dự án khởi động được theo hướng dẫn, frontend kết nối backend và các dịch vụ cần thiết.
11. Có tài liệu cài đặt, tài liệu nghiệp vụ, test plan và kịch bản demo.

**Nguyên tắc quan trọng:** không chia việc chỉ theo tên trang giao diện. Mỗi thành viên phải chịu trách nhiệm đến khi phần việc của mình có dữ liệu, xử lý nghiệp vụ, API/giao diện và kiểm thử cơ bản; Team Leader chịu trách nhiệm kết nối các phần đó thành một hệ thống hoàn chỉnh.
