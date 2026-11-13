# TÀI LIỆU ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)

## HRFlow – Hệ thống quản lý nhân sự và tính lương tự động cho HNQ Media

| Thuộc tính | Nội dung |
|---|---|
| Tên dự án | HRFlow – Enterprise Human Resource & Automated Payroll System |
| Doanh nghiệp mô phỏng | HNQ Media – doanh nghiệp truyền thông và tiếp thị số |
| Phiên bản tài liệu | 3.3 – Đồng bộ với baseline nghiệp vụ đã chốt (bỏ `CONFLICT_HOLD`; chấm công cơ bản là MVP, QR/ảnh là mở rộng) |
| Trạng thái | ĐÃ CHỐT – đồng bộ với `company_profile.md`, `permission_matrix.md`, `payroll_rules.md`; mã FR liền mạch FR-01…FR-33; các giả định mô phỏng/ngoài MVP được ghi rõ tại Mục 11 |
| Ngày cập nhật | 13/11/2026 |
| Ngôn ngữ | Tiếng Việt; tên trạng thái, role và mã yêu cầu giữ bằng tiếng Anh khi cần |
| Nhóm thực hiện | Hồ Thị Gia Hân; Đặng Minh Hiếu; Trần Thị Lệ Hoa; Nguyễn Hoàng Phương Ngân; Hồ Vinh Quan |

> **Quy tắc đọc tài liệu:** SRS này mô tả yêu cầu và kết quả hệ thống phải đáp ứng, không thay thế tài liệu quy chế nghiệp vụ. `company_profile.md`, `payroll_rules.md` (v3.0) và `permission_matrix.md` (v1.0) là baseline nghiệp vụ đã chốt. Khi SRS hoặc tài liệu kỹ thuật có nội dung khác baseline, không tự ý chọn cách xử lý; ghi nhận mâu thuẫn và xin xác nhận trước khi thiết kế/triển khai.

---

## Lịch sử sửa đổi

| Phiên bản | Ngày | Người thực hiện | Nội dung |
|---|---|---|---|
| 1.0 | Trước 30/10/2026 | Nhóm dự án | Bản SRS ban đầu, có UR/SR, FR, BR, NFR và phân tích kỹ thuật sơ bộ |
| 2.0 | 30/10/2026 | Nhóm dự án | Chuẩn hóa theo ba tài liệu nghiệp vụ HNQ; tách role/chức danh/grant; sửa luồng payroll, PTO/OT, dữ liệu Sales |
| 3.0 | 30/10/2026 | Nhóm dự án | Chốt hệ mã FR (retire FR-18/FR-19 cũ, thêm FR-27 đến FR-33); bổ sung quỹ phép, lịch/giờ chuẩn, chốt công, Payable/tạm ứng/hoàn chi, trạng thái PAID, phân loại adjustment, quản trị tài khoản, grant, hồ sơ thuế/bảo hiểm; sửa lỗi tham chiếu; thêm NFR-09 (toàn vẹn payroll); thêm bảng PM → FR, phạm vi MVP theo ca kiểm thử P/B; thêm nhật ký quyết định DEC; viết lại ma trận truy vết |
| 3.1 | 30/10/2026 | Nhóm dự án | Chốt các DEC-01–DEC-08; tách rõ test MVP và mở rộng; quy định dữ liệu OT/Sales trong MVP; định nghĩa `CONFLICT_HOLD`; làm rõ mã lỗi, quyền grant, điều chỉnh lương, `net_salary`; phân loại vấn đề còn mở thành giả định mô phỏng hoặc tích hợp ngoài MVP |
| 3.2 | 03/11/2026 | Nhóm dự án | Gộp chấm công QR/ảnh vào FR-07 (MVP); đánh lại toàn bộ mã FR liền mạch FR-01…FR-33; cập nhật tham chiếu, phạm vi MVP, SIM-03, DEC-09, ma trận truy vết và luồng nghiệp vụ |
| 3.3 | 13/11/2026 | Nhóm dự án | Đồng bộ với baseline đã chốt: bỏ trạng thái `CONFLICT_HOLD` (kỳ có lương Director giữ `CALCULATED`, chặn `APPROVE` theo OPEN-05); FR-07 MVP là chấm công cơ bản, QR động + ảnh minh chứng về nhóm mở rộng; chỉ HR cấp QR; sửa DEC-02, DEC-09, BR-10, BR-21, SIM-03 |

## Mục lục

1. [Giới thiệu](#1-giới-thiệu)
2. [Mô tả chung](#2-mô-tả-chung)
3. [Yêu cầu chức năng chi tiết](#3-yêu-cầu-chức-năng-chi-tiết)
4. [Yêu cầu giao diện ngoài](#4-yêu-cầu-giao-diện-ngoài)
5. [Yêu cầu hiệu suất](#5-yêu-cầu-hiệu-suất)
6. [Yêu cầu phi chức năng](#6-yêu-cầu-phi-chức-năng)
7. [Quy tắc nghiệp vụ](#7-quy-tắc-nghiệp-vụ)
8. [Phạm vi triển khai và ưu tiên](#8-phạm-vi-triển-khai-và-ưu-tiên)
9. [Luồng nghiệp vụ tổng thể](#9-luồng-nghiệp-vụ-tổng-thể)
10. [Ma trận truy vết yêu cầu](#10-ma-trận-truy-vết-yêu-cầu)
11. [Quyết định thiết kế, giả định và vấn đề cần xác nhận](#11-quyết-định-thiết-kế-giả-định-và-vấn-đề-cần-xác-nhận)
12. [Phụ lục](#12-phụ-lục)

---

# 1. Giới thiệu

## 1.1. Mục đích

Tài liệu này đặc tả yêu cầu phần mềm của HRFlow, ứng dụng web nội bộ hỗ trợ HNQ Media quản lý hồ sơ nhân sự, chấm công, nghỉ phép, tăng ca, dữ liệu thu nhập và quy trình tính lương. Tài liệu là cơ sở thống nhất cho Product Owner, thiết kế, lập trình viên Frontend/Backend, thiết kế cơ sở dữ liệu và QA khi xây dựng Use Case, thiết kế dữ liệu, OpenAPI, giao diện và kiểm thử.

SRS xác định: nhóm người dùng và phạm vi dữ liệu; chức năng bắt buộc; tiền điều kiện, luồng chính, ngoại lệ và kết quả; quy tắc nghiệp vụ không được thay đổi tùy tiện; yêu cầu giao diện, hiệu suất, bảo mật, audit, khôi phục, toàn vẹn dữ liệu; và liên kết giữa UR, FR, BR, tiêu chí nghiệm thu và ca kiểm thử.

## 1.2. Phạm vi dự án

HRFlow được xây dựng cho HNQ Media (TP.HCM), số hóa các quy trình nhân sự và tiền lương vốn theo dõi bằng bảng tính/giấy tờ.

Phạm vi nghiệp vụ:

1. Hồ sơ nhân viên, phòng ban, chức danh, quan hệ quản lý, trạng thái làm việc, hợp đồng; hồ sơ thuế/bảo hiểm phục vụ tính lương.
2. Lịch làm việc cá nhân, lịch lễ/Tết, giờ chuẩn.
3. Chấm công vào/ra, bảng công, xử lý sai lệch, chốt công.
4. Nghỉ phép (PTO) và quỹ phép.
5. Lịch OT, đăng ký OT, duyệt và xác nhận giờ thực tế.
6. Dữ liệu thu nhập có căn cứ: phụ cấp, thưởng, khấu trừ, KPI/hoa hồng Sales (bảng đối soát đã xác nhận).
7. Kỳ lương `DRAFT → CALCULATED → APPROVED → LOCKED`, cùng trạng thái chi trả `PAID` tách riêng.
8. Phiếu lương PDF sau khi kỳ được khóa.
9. Xác thực, phân quyền, grant, quản trị tài khoản, approval log, audit log.
10. Thông báo, báo cáo, dashboard theo phân quyền.

HRFlow **không** thay thế CRM, quản lý dự án, kế toán hoặc hệ thống ngân hàng; không quản lý chiến dịch quảng cáo, không thanh toán ngân hàng từ đầu đến cuối, không tự suy doanh thu từ ngân sách quảng cáo.

## 1.3. Đối tượng đọc

| Đối tượng | Cách sử dụng SRS |
|---|---|
| Giảng viên/khách hàng giả định | Xác nhận phạm vi, tính hợp lý, tiêu chí nghiệm thu |
| Product Owner/trưởng nhóm | Phân rã backlog, Use Case, kế hoạch Sprint |
| Frontend | Màn hình, trường nhập, trạng thái, phản hồi lỗi |
| Backend | Nghiệp vụ, kiểm tra quyền, chuyển trạng thái, API |
| Thiết kế CSDL | Thực thể, quan hệ, lịch sử hiệu lực, grant, audit |
| QA | Test case theo yêu cầu, luồng, ngoại lệ, ma trận quyền |
| DevOps | Môi trường, backup, build/test |

SRS mô tả **điều hệ thống phải làm**; tên bảng, endpoint, package, migration do tài liệu kỹ thuật chốt nhưng không được làm mất yêu cầu nghiệp vụ.

## 1.4. Quy ước, định nghĩa và thuật ngữ

| Thuật ngữ | Ý nghĩa |
|---|---|
| SRS | Software Requirements Specification |
| UR / FR / NFR / BR / AC | Yêu cầu người dùng / chức năng / phi chức năng / quy tắc nghiệp vụ / tiêu chí nghiệm thu |
| DEC | Decision – quyết định thiết kế đã chốt trong baseline (Mục 11) |
| HR | Bộ phận Nhân sự; đồng thời là một role |
| ESS | Employee Self-Service |
| PTO / OT | Nghỉ phép / làm thêm giờ |
| RBAC | Phân quyền theo vai trò nền tảng |
| Grant | Quyền nghiệp vụ cấp riêng theo tài khoản/phạm vi/thời hạn: `VIEW`, `PREPARE`, `APPROVE`, `LOCK` |
| Audit log / Approval log | Nhật ký thao tác quan trọng / lịch sử gửi-duyệt-từ chối-hủy |
| Gross / Net / Payable | Tổng thu nhập trước khấu trừ / thực nhận sau nghĩa vụ / số thực chi sau tạm ứng và hoàn chi |
| H_std | Tổng giờ chuẩn của toàn kỳ theo lịch cá nhân |
| H_paid | Tổng thời lượng được trả hợp lệ |
| H_eligible | Giờ theo lịch đủ điều kiện hưởng một khoản phụ cấp |
| R_ot | Đơn giá giờ dùng tính OT |
| Chốt công | Hành động HR khóa nguồn dữ liệu công/phép/OT của kỳ, có người, thời điểm, phiên bản snapshot |
| `DRAFT` | Kỳ đang chuẩn bị |
| `CALCULATED` | Đã tính, HR rà soát; được quay lại `DRAFT` trước khi duyệt |
| `APPROVED` | Đã được người có grant `APPROVE` duyệt; không tính lại |
| `LOCKED` | Đã được người có grant `LOCK` khóa; chỉ đọc |
| `PAID` | Trạng thái chi trả riêng sau `LOCKED`; không mở quyền sửa lương |
| `Needs Review` | Dữ liệu chưa xác minh, không được đưa âm thầm vào payroll |

### 1.4.1. Quy tắc mã hóa và hệ mã FR của bản 3.0

- `UR-xx`, `FR-xx`, `NFR-xx`, `BR-xx`, `AC-FR-xx`, `PM-xx` (ca phân quyền từ `permission_matrix.md`), `DEC-xx` (quyết định đã chốt), `SIM/EXT/QA` (giả định mô phỏng/hạng mục ngoài MVP/kiểm thử).
- Từ bản **3.2**, hệ mã FR được đánh lại **liền mạch FR-01 … FR-33** (không còn khoảng trống mã sau khi gộp chấm công và thu gọn các mã đã retire trước đó trong phạm vi đánh số hiện hành).
- Chấm công = **FR-07**: chấm công cơ bản là MVP; QR động + ảnh minh chứng là chế độ mở rộng trong cùng mã (từ bản 3.3). Cơ cấu tổ chức giữ **FR-04**.
- **FR-08** = Quản lý yêu cầu nghỉ phép (PTO) — trước đây là FR-09 ở bản ≤ 3.1.
- Tổng hợp Gross/Net/Payable = **FR-27** (trước đây FR-30 / FR-19 tùy phiên bản).
- Khi thêm yêu cầu mới, dùng mã tiếp theo sau FR-33; cập nhật SRS và ma trận truy vết cùng lúc. Bảng ánh xạ mã các phiên bản nằm ở Phụ lục 12.4.
- Các giả định mô phỏng và hạng mục ngoài MVP được ghi tại Mục 11.3; `OPEN-01..06` trong `payroll_rules.md` là mã của tài liệu nghiệp vụ riêng.

## 1.5. Tài liệu tham chiếu và thứ tự ưu tiên

1. `company_profile.md` – hồ sơ công ty và quy chế vận hành HNQ Media.
2. `payroll_rules.md` (v3.0) – quy tắc tính lương.
3. `permission_matrix.md` (v1.0) – ma trận quyền nghiệp vụ.
4. SRS các phiên bản trước và mẫu SRS tham khảo bố cục.

Thứ tự ưu tiên khi có khác biệt: (1) pháp luật bắt buộc và thỏa thuận hợp pháp có lợi hơn cho người lao động; (2) hợp đồng/quyết định lương và quy chế đã được HNQ phê duyệt; (3) `company_profile.md`; (4) `payroll_rules.md` và `permission_matrix.md` theo phạm vi từng tài liệu; (5) SRS và tài liệu kỹ thuật. Nếu chưa đủ căn cứ pháp lý hoặc xử lý xung đột lợi ích, hệ thống yêu cầu người có thẩm quyền xác minh; không tự lấy giá trị mặc định.

---

# 2. Mô tả chung

## 2.1. Toàn cảnh sản phẩm

HRFlow là ứng dụng web nội bộ. Người dùng đăng nhập bằng tài khoản được cấp; giao diện và dữ liệu phụ thuộc role nền tảng, quan hệ quản lý, grant và quyền sở hữu bản ghi. Hệ thống không mặc định có dịch vụ ngoài (email thật, cloud storage, nhận diện khuôn mặt) nếu chưa cấu hình và kiểm thử.

Chuỗi nghiệp vụ: hồ sơ + lịch → chấm công/nghỉ/OT → xác minh và **chốt công** → xác nhận thu nhập → tính và rà soát lương → đối soát tài chính → phê duyệt → khóa kỳ → phiếu lương → ghi nhận chi trả. Mỗi dữ liệu đầu vào payroll có nguồn, trạng thái xác nhận và liên kết truy xuất.

## 2.2. Bối cảnh doanh nghiệp HNQ Media

HNQ Media cung cấp dịch vụ truyền thông và tiếp thị số B2B: quản trị nội dung, thiết kế, quảng cáo trực tuyến, SEO, quản lý khách hàng/chiến dịch, tư vấn gói truyền thông.

### 2.2.1. Quy mô và cơ cấu mẫu

| Bộ phận | Số lượng | Chức danh chính |
|---|---:|---|
| Management | 3 | Director, Operations Manager, Finance Manager |
| Human Resources | 4 | HR Lead, HR Executive |
| Content | 8 | Content Lead, Content Executive |
| Design & Video | 6 | Design Lead, Designer/Video Editor |
| Digital Marketing | 7 | Digital Lead, SEO/Ads Specialist |
| Account | 5 | Account Lead, Account Executive |
| Sales | 7 | Sales Manager, Sales Executive |
| **Tổng** | **40** | 7 phòng ban |

Nhóm mẫu: 34 chính thức toàn thời gian, 3 bán thời gian, 2 thử việc, 1 thực tập sinh. Đây là dữ liệu demo; nhân viên thực tế phải có hồ sơ và điều kiện hợp đồng/lịch tương ứng. Loại nhân sự không tự quyết định nghĩa vụ thuế/bảo hiểm.

### 2.2.2. Lịch làm việc mặc định

- Full-time mẫu: thứ Hai–thứ Sáu, 08:30–12:00 và 13:00–17:30 (8 giờ/ngày, 40 giờ/tuần).
- Part-time mẫu: thứ Hai–thứ Sáu, 08:30–12:30 (4 giờ/ngày).
- Lịch cá nhân có hiệu lực là nguồn xác định giờ chuẩn; không mặc định 22/26 ngày. Tính bằng phút chính xác; không làm tròn công trước khi tính tiền.
- Múi giờ `Asia/Ho_Chi_Minh`; tiền tệ VND.

### 2.2.3. Tuyến quản lý

- Content/Design/Digital/Account Executive → Lead → Operations Manager → Director.
- Sales Executive → Sales Manager → Director.
- HR Executive → HR Lead → Director.
- Finance Manager, Operations Manager → Director.

Tuyến quản lý xác định phạm vi team và tuyến duyệt PTO/OT; **không tự cấp quyền xem lương** hay quyền `APPROVE`/`LOCK`.

## 2.3. Tổng quan chức năng

| Nhóm | Chức năng chính | Đầu ra |
|---|---|---|
| Tài khoản và truy cập | Đăng nhập/đăng xuất, quản trị tài khoản (FR-31), role/grant (FR-02, FR-32) | Phiên hợp lệ, quyền đúng phạm vi |
| Hồ sơ nhân sự | Hồ sơ, cơ cấu, hợp đồng, onboarding/offboarding, hồ sơ thuế/BH (FR-03–FR-06, FR-33) | Hồ sơ có lịch sử hiệu lực |
| Lịch và chấm công | Lịch/lễ/giờ chuẩn (FR-29), chấm công cơ bản (QR động + ảnh minh chứng là mở rộng), xác minh, chốt công (FR-07, 11, 33) | Bản ghi công đã xác nhận, snapshot nguồn |
| PTO | Tạo, duyệt, hủy/điều chỉnh, quỹ phép (FR-08, FR-28) | Trạng thái đơn, số dư, approval log |
| OT | Lịch, đăng ký, duyệt, xác nhận thực tế, OT ngoài lịch (FR-09) | Dữ liệu OT đủ căn cứ |
| Thu nhập và Sales | Phụ cấp, thưởng, khấu trừ, KPI/hoa hồng (FR-11, FR-12) | Dòng thu nhập có nguồn xác nhận |
| Payroll | Kỳ lương, tính, tổng hợp, duyệt, khóa, chi trả (FR-13–FR-16, FR-17, FR-27) | Bảng lương có snapshot và tham số |
| Phiếu lương | PDF sau khóa (FR-18, FR-19) | Phiếu theo nhân viên/kỳ |
| Báo cáo/thông báo/audit | FR-20–FR-26 | Dữ liệu tổng hợp không vượt quyền |

## 2.4. Đặc điểm người dùng và role hệ thống

HRFlow chỉ có **4 role nền tảng**: `Employee`, `Manager`, `HR`, `Admin`. Director, Operations Manager, Finance Manager, HR Lead, Sales Manager và các Lead là chức danh (Director dùng role `Manager`), không phải role thứ năm.

| Role | Người dùng | Quyền tổng quát | Giới hạn |
|---|---|---|---|
| `Employee` | Nhân viên các phòng ban | Dữ liệu của mình; chấm công; tạo PTO; đăng ký OT đã công bố; xem công/phiếu lương của mình | Không xem hồ sơ/công/lương người khác |
| `Manager` | Lead, Sales Manager, Operations Manager, Finance Manager, Director | Quyền cá nhân và dữ liệu vận hành team trực tiếp; duyệt PTO/OT đúng tuyến | Không mặc nhiên xem lương team; không tự có quyền payroll |
| `HR` | HR Executive/HR Lead | Hồ sơ, hợp đồng, công, PTO/OT; chuẩn bị/tính/rà soát lương theo grant | Không mặc nhiên `APPROVE`/`LOCK` |
| `Admin` | Quản trị kỹ thuật ngoài 40 nhân sự | Tài khoản, role nền tảng, cấu hình kỹ thuật, giám sát audit | Không mặc nhiên xem lương, duyệt, khóa |

### 2.4.1. Quyền payroll dạng grant

`VIEW`, `PREPARE`, `APPROVE`, `LOCK` là bốn quyền độc lập; không suy ra từ role hoặc chức danh.

| Nhiệm vụ | Người chịu trách nhiệm | Grant |
|---|---|---|
| Thu thập, tính, rà soát bảng lương | HR | `PREPARE` (kèm `VIEW` theo nhiệm vụ) |
| Đối soát tài chính, tiền thực nhận Sales | Finance Manager | Không đồng nghĩa `APPROVE`/`LOCK` |
| Xác nhận Sales owner/điều kiện KPI | Sales Manager | — |
| Phê duyệt bảng lương, duyệt adjustment | Director | `APPROVE` cấp riêng |
| Khóa kỳ sau phê duyệt | Finance Manager | `LOCK` cấp riêng |

Người lập/tính bảng lương không tự duyệt. Admin không mặc nhiên có `VIEW`/`APPROVE`/`LOCK`. Xung đột lợi ích khi kỳ có lương Director xử lý theo DEC-02 và BR-21 (chặn duyệt, không thêm trạng thái).

## 2.5. Môi trường vận hành dự kiến

- Ứng dụng web trên trình duyệt hiện đại, HTTPS khi truy cập qua mạng.
- Backend dự kiến Java/Spring Boot; CSDL PostgreSQL; API đặc tả bằng OpenAPI trước khi tích hợp.
- Công nghệ cụ thể, Docker, lưu trữ PDF, nhà cung cấp email ghi trong tài liệu kiến trúc/README. Không xem dịch vụ ngoài là sẵn có nếu chưa cấu hình.
- Môi trường demo có dữ liệu seed nhất quán với 4 role, chức danh, tuyến quản lý, grant và bộ ví dụ P/B (Mục 8.1).

## 2.6. Ràng buộc thiết kế và triển khai

1. Giữ đúng bốn role và tách role khỏi chức danh/tuyến duyệt/grant.
2. Dữ liệu cá nhân, lương, thuế, bảo hiểm, ngân hàng chỉ trả trong phạm vi cần thiết.
3. Dữ liệu lịch sử (hồ sơ, lương, hợp đồng, công, quyền, phê duyệt) không bị xóa khi nghỉ việc.
4. Kỳ `APPROVED`/`LOCKED` không tính lại; `LOCKED` không update/delete trực tiếp.
5. Tham số lương/pháp lý có hiệu lực theo thời gian, nguồn, người xác nhận; không hard-code.
6. Dữ liệu demo hoặc tài liệu kỹ thuật cũ không phải nguồn quyết định chính sách.
7. Gửi thông báo/email xử lý nền, không chặn request chính.
8. Tính năng mở rộng không được đánh dấu hoàn thành nếu chưa có kiểm thử chứng minh.
9. Mọi phép tính tiền dùng số thập phân chính xác (BigDecimal/decimal), không float/double.

## 2.7. Giả định và phụ thuộc

- HNQ là doanh nghiệp mô phỏng; dữ liệu mẫu không phải dữ liệu thật.
- Tài khoản do Admin khởi tạo theo yêu cầu hợp lệ của HR; không có đăng ký công khai.
- HR nhập hồ sơ và dữ liệu nguồn có căn cứ; Manager/Sales Manager/Finance Manager xác nhận phần thuộc trách nhiệm.
- Dữ liệu Sales nhập từ bảng đối soát đã xác nhận, không yêu cầu CRM.
- Email và lưu trữ PDF chỉ hoạt động khi có cấu hình; nếu chưa có, demo phải thể hiện rõ là giả lập/in-app. MVP dùng chấm công cơ bản (FR-07); QR động + ảnh minh chứng là chức năng mở rộng. Khi demo chế độ QR/ảnh mà chưa có camera/lưu trữ ảnh thật thì phải mô phỏng rõ ràng (in-app/giả lập) và ghi rõ trong tài liệu demo, không tuyên bố đã tích hợp phần cứng.
- Tham số thuế, bảo hiểm, lương tối thiểu, giới hạn OT là bộ mô phỏng, ghi nguồn/trạng thái xác minh; phải xác minh văn bản pháp lý trước khi vận hành thật.

---

# 3. Yêu cầu chức năng chi tiết

## 3.1. Nguyên tắc đặc tả

Mỗi FR có mã, mô tả, tác nhân, ưu tiên, tiền điều kiện, luồng, ngoại lệ, tiêu chí nghiệm thu. Tên API chỉ là định hướng; endpoint cuối cùng chốt trong OpenAPI nhưng phải bảo toàn hành vi, quyền và dữ liệu nêu ở đây.

Ưu tiên: **MVP** (cần cho quy trình cốt lõi, Mục 8.1); **Mở rộng** (sau MVP, không đổi nguyên tắc nghiệp vụ); **Tích hợp/định hướng** (phụ thuộc dịch vụ ngoài).

## 3.2. FR-01 – Xác thực và quản lý phiên đăng nhập

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | Tất cả người dùng có tài khoản |
| Ưu tiên | MVP |
| Tiền điều kiện | Tài khoản đã khởi tạo, trạng thái cho phép đăng nhập |

**Mô tả:** đăng nhập, đăng xuất, đổi mật khẩu theo chính sách (NFR-01). Tài khoản vô hiệu hóa hoặc nghỉ việc theo ngày hiệu lực không truy cập được.

**Luồng chính:** (1) nhập thông tin xác thực; (2) kiểm tra thông tin, trạng thái và hiệu lực tài khoản; (3) tạo phiên/token, xác định role và grant còn hiệu lực; (4) giao diện hiển thị chức năng theo quyền; (5) đăng xuất kết thúc phiên.

**Ngoại lệ:** sai thông tin → lỗi chung, không tiết lộ tài khoản có tồn tại; tài khoản bị khóa/hết hiệu lực → từ chối; token thiếu/hết hạn → `401`; vượt số lần đăng nhập sai → khóa tạm theo NFR-01.

**AC-FR-01:** đăng nhập sai không tạo phiên; tài khoản vô hiệu hóa không gọi được API bảo vệ (PM-18); API không token trả `401` (PM-01); đăng xuất vô hiệu phiên; mật khẩu không lưu dạng rõ; khóa tạm sau số lần sai cấu hình.

## 3.3. FR-02 – Role, phạm vi dữ liệu và kiểm soát truy cập

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | Tất cả người dùng đã xác thực |
| Ưu tiên | MVP |

**Yêu cầu:**
1. Mỗi tài khoản có đúng một role nền tảng đang hiệu lực.
2. Phạm vi `own` / `team` / `company` xác định từ dữ liệu lưu trữ và nhiệm vụ được cấp.
3. `employee_id`, `manager_id`, role, grant từ client không được tin cậy làm căn cứ phân quyền (PM-15).
4. Manager không mặc nhiên xem lương cấp dưới; Admin không mặc nhiên xem payroll; HR không mặc nhiên `APPROVE`/`LOCK`.
5. Quyền kiểm tra tại server, theo thứ tự: xác thực → trạng thái tài khoản → role → grant còn hiệu lực → phạm vi bản ghi → trạng thái nghiệp vụ.
6. Grant hết hạn/thu hồi mất hiệu lực ngay (PM-20); quản lý grant xem FR-32.

**Quy ước mã lỗi (DEC-01):** thiếu/hết hạn token → `401`; thiếu quyền ở mức **chức năng/endpoint** (không có role/grant) → `403`; truy cập **bản ghi cụ thể** ngoài phạm vi (own/team) → `404` để không lộ sự tồn tại; trạng thái nghiệp vụ không cho phép → `409`; dữ liệu sai quy tắc → `422`.

**AC-FR-02:** bao phủ PM-01 đến PM-08, PM-15, PM-19, PM-20 theo bảng Phụ lục 12.2; kiểm tra quyền phía server, không chỉ ẩn nút.

## 3.4. FR-03 – Quản lý hồ sơ nhân sự

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | HR; nhân viên cập nhật trường cá nhân được phép; Manager xem trường cơ bản team |
| Ưu tiên | MVP |

**Dữ liệu tối thiểu:** mã nhân viên duy nhất; họ tên; liên hệ cần thiết; phòng ban; chức danh; quản lý trực tiếp; loại nhân sự; trạng thái làm việc; ngày vào/nghỉ; thông tin hợp đồng/lương theo quyền; **địa bàn nơi làm việc áp dụng** (xác định lương tối thiểu vùng, trần BHTN – không suy từ tên "TP.HCM"). Dữ liệu thuế/bảo hiểm/ngân hàng xem FR-33; trường định danh, thuế, bảo hiểm, ngân hàng là dữ liệu nhạy cảm, chỉ thu thập khi cần.

**Luồng tạo:** HR tạo hồ sơ → nhập trường bắt buộc và thông tin tổ chức/hợp đồng → kiểm tra bắt buộc/định dạng/duy nhất/ngày hiệu lực → lưu trạng thái `Onboarding` nếu chưa đủ điều kiện → yêu cầu khởi tạo tài khoản qua FR-31 → ghi audit.

**Cập nhật:** HR cập nhật theo nhiệm vụ; nhân viên chỉ trường cá nhân được phép; Manager không sửa hồ sơ. Thay đổi phòng ban/chức danh/lương có ngày hiệu lực và lưu lịch sử.

**Trạng thái hồ sơ:** `Onboarding`, `Active`, `On Leave`, `Terminated` – tách khỏi trạng thái tài khoản đăng nhập.

**Ngoại lệ:** trùng mã, thiếu trường bắt buộc, ngày hiệu lực sai, thao tác ngoài quyền → từ chối kèm thông báo dễ hiểu.

**AC-FR-03:** không có hai hồ sơ cùng mã; Manager chỉ thấy trường cơ bản team (PM-03); thay đổi có ngày hiệu lực, lịch sử cũ còn; cập nhật nhạy cảm có audit (PM-11).

## 3.5. FR-04 – Cơ cấu tổ chức và tuyến quản lý

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | HR cập nhật theo quyết định hợp lệ; Director quyết định thẩm quyền; Admin chỉ hỗ trợ kỹ thuật |
| Ưu tiên | MVP (danh mục cơ bản, lịch sử hiệu lực bắt buộc) |

1. Danh mục phòng ban và chức danh; một nhân viên thuộc một phòng ban tại một thời điểm.
2. Quan hệ quản lý trực tiếp cấu hình theo thời gian hiệu lực.
3. Chuyển phòng ban/chức danh/người quản lý lưu ngày hiệu lực và lịch sử.
4. HR cập nhật theo quyết định của Director; Admin không quyết định cơ cấu.
5. Tuyến quản lý xác định phạm vi team và tuyến PTO/OT, không cấp quyền payroll.
6. Tuyến duyệt chính/dự phòng của PTO/OT được lưu thành dữ liệu cấu hình riêng, có hiệu lực theo thời gian (xem FR-32).

**AC-FR-04:** quan hệ mới áp dụng theo ngày hiệu lực, lịch sử truy xuất được; không cho vòng quản lý.

## 3.6. FR-05 – Hợp đồng, lịch làm việc cá nhân và mức lương có hiệu lực

| Thuộc tính | Đặc tả |
|---|---|
| Tác nhân | HR |
| Ưu tiên | MVP (dữ liệu cho payroll); cảnh báo hết hạn: Mở rộng |

1. Lưu loại hợp đồng/thỏa thuận, ngày bắt đầu/kết thúc, trạng thái, mức lương, lịch làm việc có hiệu lực (liên kết FR-29).
2. Cập nhật/gia hạn hợp đồng theo căn cứ hợp lệ; gia hạn không tạo hồ sơ mới.
3. Thay đổi lương, lịch, chức danh, loại hợp đồng giữa tháng lưu thành các khoảng hiệu lực không chồng lấn.
4. Cảnh báo hợp đồng sắp hết hạn trước 30 ngày (cấu hình, FR-25).
5. Không tự gán lương mẫu khi thiếu hợp đồng/quyết định lương; thiếu dữ liệu → `Needs Review`.
6. Mức lương thử việc (mô phỏng 85%), part-time (5.000.000), trợ cấp thực tập (4.000.000) chỉ áp dụng khi phù hợp hợp đồng/thỏa thuận; không so máy móc sàn lương.

**AC-FR-05:** mỗi kỳ dùng đúng lương/lịch theo hiệu lực (P08); hồ sơ thiếu dữ liệu bị đánh dấu, không tự thay bằng 0.

## 3.7. FR-06 – Onboarding và offboarding

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR; Admin thao tác tài khoản theo yêu cầu hợp lệ |
| Ưu tiên | MVP |

**Onboarding:** HR tạo hồ sơ, gán phòng ban/chức danh/quản lý, nhập hợp đồng, lịch, lương, hồ sơ thuế/BH; tài khoản khởi tạo qua FR-31; hồ sơ chuyển `Active` khi đủ điều kiện.

**Offboarding:** HR ghi ngày nghỉ, lý do; hệ thống xác định thời điểm vô hiệu hóa tài khoản theo ngày hiệu lực; giữ hồ sơ, công, PTO/OT, phiếu lương, audit. Khoản còn phải trả (lương kỳ cuối, phép chưa nghỉ, trợ cấp khi đủ điều kiện) xử lý trong kỳ phù hợp, có căn cứ và phân loại thuế riêng; trạng thái `Terminated` không loại nhân viên khỏi quyết toán (B20).

**AC-FR-06:** sau thời điểm vô hiệu hóa không đăng nhập/gọi API được (PM-18); dữ liệu lịch sử vẫn truy xuất được; nhân viên nghỉ việc vẫn có dòng quyết toán nếu còn khoản phải trả.

## 3.8. FR-07 – Chấm công (cơ bản; QR động + ảnh minh chứng là mở rộng)

| Thuộc tính | Nội dung |
|---|---|
| Mã | FR-07 (chấm công cơ bản; chế độ QR/ảnh của bản ≤ 3.1 giữ trong cùng mã) |
| Tác nhân | Employee, Manager, HR tự chấm công của mình; Manager xác minh team; HR cấu hình/cấp QR (khi bật chế độ mở rộng), kiểm tra/chốt. Admin không chấm công và không cấp QR |
| Ưu tiên | MVP: chấm công cơ bản (check-in/check-out trên web). Mở rộng: QR động + ảnh minh chứng (`company_profile.md` Mục 14.2) |

**Mô tả:** MVP cung cấp **chấm công cơ bản**: nhân viên check-in khi bắt đầu ca và check-out khi kết thúc ca trên ứng dụng; hệ thống lưu thời điểm server và tính thời lượng công theo lịch cá nhân. **QR động + ảnh minh chứng** là chế độ mở rộng, bật theo cấu hình điểm chấm công; khi bật, việc check-in/out phải kèm QR hợp lệ (và ảnh nếu cấu hình).

**Luồng chính (MVP – chấm công cơ bản):**
1. Nhân viên mở chức năng chấm công → bấm check-in khi bắt đầu ca.
2. Hệ thống lấy nhân viên từ phiên (không tin `employee_id` từ client), lịch hiệu lực, lưu thời điểm server.
3. Check-out cuối ca theo cùng quy trình.
4. Hệ thống tính phút làm việc, trễ/sớm theo lịch cá nhân, loại khoảng nghỉ và chồng lặp.
5. Nhân viên xem công của mình; Manager xem team; HR xử lý bản ghi `Needs Review` và chốt công (FR-30).

**Luồng mở rộng – QR động + ảnh minh chứng (khi bật):**
1. HR cấu hình và cấp QR động (nonce, thời hạn ngắn) tại điểm chấm công hoặc theo ca (`permission_matrix.md` Mục 3.2: chỉ HR cấp QR).
2. Nhân viên quét QR động còn hiệu lực khi check-in/check-out.
3. Backend xác thực QR (nonce, hạn dùng, điểm/ca hợp lệ); không chỉ kiểm tra phía trình duyệt.
4. QR hợp lệ → (nếu bật) yêu cầu chụp ảnh minh chứng; hệ thống lưu ảnh theo giới hạn dung lượng và quyền truy cập.
5. Hệ thống lưu thêm kết quả xác thực QR và tham chiếu ảnh.

**Quy tắc QR và ảnh (chỉ áp dụng khi bật chế độ mở rộng):**
- QR động có nonce và thời hạn ngắn (mặc định 10–15 giây, cấu hình); **không dùng mã tĩnh dài hạn**.
- QR hết hạn/không hợp lệ → không tạo bản ghi chấm công, giao diện báo lỗi rõ ràng, không tiếp tục chụp ảnh.
- Ảnh minh chứng: giới hạn dung lượng cấu hình (mục tiêu ≤ 200 KB), chỉ lưu dữ liệu cần thiết, kiểm soát người xem theo quyền; thuộc dữ liệu nhạy cảm (NFR-01).
- Lưu thời điểm, người thực hiện, kết quả xác thực QR, tham chiếu ảnh.
- Không khẳng định sinh trắc học/nhận diện khuôn mặt; QR + ảnh chỉ là bằng chứng hỗ trợ xác minh.
- Nếu môi trường demo chưa có camera/lưu trữ ảnh thật: phải mô phỏng rõ (in-app/giả lập) và ghi rõ trong tài liệu demo; không tuyên bố đã tích hợp phần cứng.

**Quy tắc tính công:**
- Ghi phút trễ/sớm từ phút đầu; không dung sai 15 phút tự động; **không phạt tiền**.
- Thiếu check-in/out (hoặc QR/ảnh không hợp lệ khi bật chế độ mở rộng) → `Needs Review`, không tự 0 công.
- Làm bù trong ngày không tự thành OT; đến sớm/về muộn không tự là OT.
- Ngày lễ hưởng lương không giảm lương tháng, không cộng trùng công.
- **Đi công tác/làm việc bên ngoài:** có lịch và minh chứng được Manager duyệt; giờ đủ điều kiện được tính như công thường.
- Lịch đổi giữa kỳ: dùng lịch hiệu lực tại ngày đó, không áp ngược.

**Ngoại lệ:** check-in trùng, check-out không có check-in, thời gian bất hợp lý (và QR hết hạn/giả mạo khi bật chế độ mở rộng) → từ chối hoặc `Needs Review`; không xóa bản ghi gốc.

**AC-FR-07 (MVP):**
- A không tạo/chỉnh công cho B bằng `employee_id` khác (PM-15).
- Bảng công hiển thị giờ gốc, thời lượng, phút trễ/sớm, trạng thái.
- Thiếu dữ liệu không thành 0 công (B04); trễ 10 phút không phạt, chỉ chênh công đã đối soát (B05).
- Điều chỉnh có người, lý do, thời điểm, trước/sau.

**AC-FR-07 (mở rộng – QR/ảnh):**
- QR hết hạn/không hợp lệ bị từ chối, không tạo bản ghi công.
- Chỉ HR cấp được QR; Admin gọi chức năng cấp QR bị từ chối (`403`).
- Ảnh minh chứng không truy cập được bởi người không có quyền.
- Demo chưa tích hợp camera/lưu trữ phải ghi rõ trạng thái mô phỏng.

> **Lưu ý mã (bản 3.3):** Mã FR-07 giữ nguyên. Từ bản 3.3, MVP chỉ yêu cầu chấm công cơ bản; QR động + ảnh minh chứng trở lại nhóm mở rộng để thống nhất với `company_profile.md` Mục 14. Hệ mã FR liền mạch của bản 3.2 không thay đổi. Xem Phụ lục 12.4.

## 3.9. FR-08 – Quản lý yêu cầu nghỉ phép (PTO)

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Employee tạo đơn; người duyệt đúng tuyến; HR theo dõi/xử lý theo quyền |
| Ưu tiên | MVP |

**Dữ liệu đơn:** người gửi lấy từ phiên; loại nghỉ (phép năm, việc riêng hưởng lương, không hưởng lương, ốm/thai sản, khác); thời điểm bắt đầu/kết thúc; đơn vị giờ/ngày; **thời lượng thực tế theo lịch cá nhân** (nghỉ 0,5 ngày = 4 giờ full-time/2 giờ part-time theo ca được duyệt; không giả định mỗi buổi 4 giờ); lý do; minh chứng nếu chính sách yêu cầu; trạng thái; người duyệt; lịch sử.

**Trạng thái:** `Draft → Pending → Approved / Rejected / Cancelled`.

**Luồng chính:**
1. Nhân viên tạo bản nháp, nhập loại nghỉ, khoảng thời gian, thời lượng, lý do.
2. Hệ thống kiểm tra dữ liệu, lịch làm việc, trùng đơn, số dư phép (loại trừ quỹ phép – FR-28). Nghỉ vào ngày không phải ngày làm việc theo lịch không bị trừ phép.
3. Nhân viên gửi đơn → `Pending`; hệ thống xác định người duyệt chính theo tuyến cấu hình (FR-32).
4. Người duyệt `APPROVE` hoặc `REJECT`; từ chối bắt buộc lý do.
5. Hệ thống cập nhật trạng thái, ghi approval log, thông báo cho người gửi.
6. Quỹ phép chỉ bị trừ khi đơn được duyệt; đơn `Pending` không tự trừ quỹ phép.

**Tuyến duyệt một cấp:**

| Người gửi đơn | Người duyệt chính | Người dự phòng đã chỉ định trước |
|---|---|---|
| Nhân viên Content/Design/Digital/Account | Lead trực tiếp | Operations Manager |
| Nhân viên Sales | Sales Manager | Director |
| Nhân viên HR | HR Lead | Director |
| Lead Content/Design/Digital/Account | Operations Manager | Director |
| Sales Manager, Operations Manager, Finance Manager | Director | Không có dự phòng mặc định; Director không xử lý được thì đơn giữ chờ (HR Lead có thể tiếp nhận hồ sơ nhưng không duyệt thay) |
| HR Lead | Director | Finance Manager nếu Director chỉ định trước |
| Director | HR Lead | HR Executive nếu HR Lead chỉ định trước |

**Quy tắc ngoại lệ:**
- Không tự duyệt đơn của mình, kể cả khi là HR/Manager hoặc người dự phòng (PM-04).
- Chỉ người dự phòng chỉ định trước mới thay người chính; ghi lý do thay thế (PM-16).
- Người chính và dự phòng đều không xử lý được → giữ `Pending`, không tự chọn người khác (PM-17).
- Một quyết định `Approve/Reject` duy nhất; không chuyển tiếp nhiều cấp.

**Hủy và điều chỉnh:**
- Nhân viên được hủy đơn khi còn `Pending` (→ `Cancelled`).
- Với đơn `Approved`, nhân viên gửi **yêu cầu hủy/điều chỉnh** gắn với đơn gốc; yêu cầu này đi cùng tuyến duyệt của đơn gốc và có approval log riêng. Đơn gốc không bị sửa âm thầm; khi yêu cầu được duyệt, hệ thống hoàn trả quỹ phép tương ứng và điều chỉnh dữ liệu công qua cơ chế có audit.
- Đơn kỳ đã chốt công: yêu cầu hủy/điều chỉnh ảnh hưởng kỳ đã chốt chỉ được xử lý bằng điều chỉnh kỳ sau (FR-30, FR-24).

**Đơn chờ trước chốt lương:** HR được nhắc/giục xử lý đơn `Pending` trước chốt công; đơn chưa được duyệt **không** tự tính là nghỉ không lương và không tự là nghỉ hưởng lương (xem FR-30). Đơn bị từ chối không tự ghi nhận nghỉ hưởng lương.

**AC-FR-08:** kiểm thử đủ bảng tuyến (kể cả không dự phòng); người không đúng tuyến/người gửi không duyệt được (PM-03, PM-04, PM-16, PM-17); từ chối bắt buộc lý do; mọi thao tác có approval log; nghỉ 0,5 ngày tính theo thời lượng ca thực tế; hủy đơn đã duyệt hoàn trả quỹ phép; B03 (4 giờ làm + 4 giờ PTO) không đếm trùng.

## 3.10. FR-09 – Lịch OT, đăng ký OT và xác nhận giờ thực tế

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR/Manager công bố lịch; Employee đăng ký; người duyệt theo tuyến; Manager xác nhận giờ thực tế; HR đối chiếu |
| Ưu tiên | Mở rộng ưu tiên cao (ảnh hưởng trực tiếp payroll) |

**Mô tả:** OT thông thường chỉ đăng ký trên lịch OT đã công bố cho đối tượng đủ điều kiện; không có mô hình tự tạo OT bất kỳ lúc nào.

**Luồng chính:**
1. HR hoặc Manager (team) tạo lịch OT: ngày, giờ, công việc, đối tượng đủ điều kiện, điều kiện cần thiết; công bố.
2. Nhân viên đủ điều kiện xác nhận đồng ý và đăng ký.
3. Hệ thống xác định người duyệt đúng tuyến PTO/OT (cùng bảng FR-08), duyệt một cấp, lưu approval log.
4. Sau OT, **Manager có thẩm quyền xác nhận giờ thực tế** (kèm minh chứng); **HR kiểm tra, đối chiếu** lịch – đăng ký – quyết định – giờ thực tế.
5. Chỉ giờ thực tế đủ căn cứ chuyển sang payroll.

**Ba thông tin tách riêng:** giờ đăng ký, giờ được duyệt, giờ thực tế xác nhận.

**Quy tắc:**
- Duyệt 3 giờ, thực tế 2 giờ → trả 2 giờ (B07). Có lịch mà không thực hiện → không trả theo lịch danh nghĩa.
- **Giờ vượt giờ được duyệt:** Manager/HR xác minh thời gian, công việc, minh chứng, điều kiện trả; kết quả lưu thành phần OT bổ sung được xác nhận hoặc phần chưa đủ căn cứ. Thiếu phê duyệt bổ sung tại thời điểm phát sinh không tự làm mất quyền được thanh toán giờ làm thực tế thuộc diện phải trả; phần không xác minh được không tự vào lương.
- **Làm quá giờ không có lịch OT (OT ngoài lịch):** HR/Manager xác minh có làm việc thực tế và nghĩa vụ trả công; hệ thống tạo bản ghi xác minh riêng; không mặc định xóa giờ, cũng không tự tính OT. Bản ghi được xác nhận thì dùng như OT thực tế đủ căn cứ.
- Không mặc định loại Manager/Lead khỏi OT theo chức danh.
- Giới hạn: ≤ 4 giờ OT/ngày với ca 8 giờ, ≤ 40 giờ/tháng, mặc định 200 giờ/năm (300 chỉ khi có căn cứ thuộc trường hợp được phép); đều là tham số có hiệu lực và phải đối chiếu pháp luật/thỏa thuận. Kiểm tra cả lịch, giờ duyệt, giờ thực tế. Vượt giới hạn chưa có căn cứ → chặn lập lịch mới; vi phạm đã phát sinh → cảnh báo/xử lý trách nhiệm, **không xóa tiền công**.
- Đăng ký OT: `employee_id` lấy từ phiên (PM-15).

**AC-FR-09:** không đăng ký được lịch chưa công bố/không dành cho người dùng; giờ thực tế lưu riêng giờ đăng ký; payroll dùng giờ thực tế xác nhận (B07); OT ngoài lịch có bản ghi xác minh; vượt giới hạn có cảnh báo, không xóa công; quyết định lưu người, thời gian, lý do.

## 3.11. FR-10 – Xác minh và điều chỉnh dữ liệu chấm công

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Manager (team, được giao); HR |
| Ưu tiên | MVP |

1. Danh sách bản ghi `Needs Review` kèm lý do: thiếu check-in/out, QR hết hạn/không hợp lệ hoặc thiếu ảnh minh chứng (khi bật chế độ mở rộng), giờ bất thường, trùng thời gian, sai lệch.
2. Nhân viên gửi giải trình/minh chứng; Manager xác nhận dữ liệu thực tế; HR rà soát và chốt (chốt công theo FR-30).
3. Dữ liệu gốc và lịch sử thay đổi được bảo toàn.
4. Chỉ dữ liệu đã xác nhận được chuyển sang payroll.
5. Dữ liệu chưa rõ không tự thành 0 công, nghỉ không lương hoặc OT.
6. Công kỳ đã khóa không sửa trực tiếp; sai sót đi theo điều chỉnh kỳ sau (FR-24).

**AC-FR-10:** bản ghi chưa xác minh được đánh dấu và không đi vào payroll; mọi điều chỉnh có audit và lý do.

## 3.12. FR-11 – Phụ cấp, thưởng và khấu trừ

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR theo nhiệm vụ; người phê duyệt theo quy chế |
| Ưu tiên | MVP với khoản bắt buộc tính lương; danh mục nâng cao: Mở rộng |

**Yêu cầu:**
1. Mỗi khoản có mã loại, số tiền/công thức, điều kiện, hiệu lực, nguồn/chứng từ, người tạo, trạng thái xác nhận, mã nguồn duy nhất.
2. Nhóm phụ cấp theo chính sách HNQ: ăn trưa (730.000; part-time 365.000), gửi xe (200.000; part-time 100.000), điện thoại (300.000), chức vụ (2.000.000). Thử việc hưởng theo thỏa thuận; thực tập đào tạo không có phụ cấp thường xuyên.
3. **Công thức phân bổ phụ cấp:** `Allowance_i = ROUND_VND(Σ_s A_i_s × H_eligible_i_s / H_std_s)`; `H_eligible` là giờ **theo lịch** trong thời gian đồng thời có hợp đồng và đủ điều kiện, trừ nghỉ không lương. Đi trễ/sớm hoặc nghỉ phép hưởng lương không làm giảm phụ cấp. Vào/nghỉ/bổ nhiệm giữa kỳ chia đoạn; cộng đoạn rồi làm tròn một lần cho từng khoản (B06: 298.636 + 81.818 = 380.454).
4. Mỗi khoản phân loại riêng: Gross, căn cứ bảo hiểm, căn cứ thuế (chịu thuế/không tính thuế/miễn), **cờ `included_in_ot_base`** (có đưa vào căn cứ R_ot không) và nguồn xác định. Không áp một quy tắc cho tất cả. Mức chi nội bộ (ví dụ 730.000) không phải hạn mức miễn thuế pháp luật; khoản chưa có phân loại thuế → `NEEDS_REVIEW`, không tự miễn hay tự chịu thuế.
5. **Thưởng dự án:** không phát sinh tự động. Người quản lý lập đề xuất (dự án, lý do, nhân viên, số tiền, người phê duyệt, kỳ chi trả); sau khi được phê duyệt, HR nhập thành khoản "thưởng được duyệt". Không trừ lương cơ bản khi không đạt KPI phi doanh thu.
6. Khấu trừ chỉ vào payroll khi có căn cứ, chứng từ, nguồn duy nhất, phê duyệt hợp lệ. **Không dùng phạt tiền đi trễ hoặc cắt lương như hình thức kỷ luật.** Khấu trừ bồi thường tuân giới hạn pháp luật (ví dụ 30% lương thực trả sau BH/thuế theo Điều 102 BLLĐ cho trường hợp tương ứng), tách số thu kỳ này và số còn lại; không tự ép thu toàn bộ.
7. Hoàn chi công tác có chứng từ nằm ngoài Gross, theo bản chất chi phí công vụ.
8. Thay đổi chính sách có ngày hiệu lực và lịch sử; không áp ngược tự động cho kỳ cũ.

**AC-FR-11:** khoản chưa xác nhận không tự cộng/trừ; khoản trùng nguồn không tính hai lần (B16, B24); phụ cấp phân bổ khớp B06 và P03/P04; khoản thiếu phân loại thuế bị chặn trước approve; thay đổi có audit và hiệu lực.

## 3.13. FR-12 – Doanh thu, KPI và hoa hồng Sales

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Sales Executive nhập nháp; Sales Manager xác nhận Sales owner/điều kiện nghiệm thu; Finance Manager xác nhận tiền thực nhận; HR tiếp nhận vào payroll |
| Ưu tiên | Mở rộng (cấu trúc dữ liệu tối thiểu chuẩn bị sớm để không thiết kế lại payroll) |

HRFlow không phải CRM/kế toán; lưu bảng kết quả đối soát đã xác nhận kèm chứng từ tham chiếu.

**Doanh thu đủ điều kiện:** thuộc hợp đồng dịch vụ HNQ có mã hợp đồng và Sales owner; phần dịch vụ **đã nghiệm thu**; công ty **đã thực nhận tiền**; giá trị dịch vụ chưa VAT sau chiết khấu; loại chi hộ ngân sách quảng cáo và hoàn tiền; không tính trùng giữa kỳ/hợp đồng/nhân viên. Thanh toán một phần chỉ ghi nhận phần đã nghiệm thu và thực nhận. HR không tự suy doanh thu từ tổng tiền khách chuyển.

**Dữ liệu mỗi khoản:** kỳ, mã nhân viên, mã hợp đồng, mã khoản thu duy nhất, số tiền thanh toán, doanh thu dịch vụ đủ điều kiện, ngày thanh toán, ngày nghiệm thu, điều chỉnh/hoàn, Sales owner, người xác nhận, trạng thái.

**Công thức (theo payroll_rules mục 6):**
```text
Revenue_eligible = Σ doanh thu dịch vụ đủ điều kiện chưa ghi nhận ở kỳ khác
Commission = ROUND_VND(Revenue_eligible × 2%)
Target = 100.000.000 × ngày theo lịch giữ vị trí Sales / ngày chuẩn cả tháng
KPI_bonus = 1.500.000 nếu Target > 0 và Revenue_eligible ≥ Target; ngược lại 0
```
So sánh chính xác, không làm tròn tỷ lệ trước khi xét đạt; dưới KPI vẫn có hoa hồng; vượt KPI không tăng thưởng; thưởng KPI không chia theo chỉ tiêu nửa tháng. Chỉ Sales Executive được giao sở hữu doanh thu hưởng chính sách; Sales Manager không nhận 2% doanh thu nhóm. Các tỷ lệ/chỉ tiêu lấy từ tham số hiệu lực (FR-23), không hard-code.

**Quy tắc:** một khoản thu chỉ một Sales owner sau xác nhận (PM-14); hoàn tiền/sai sót sau kỳ khóa dùng adjustment (FR-24); khách chậm trả → khoản chuyển sang kỳ đủ điều kiện sau.

**AC-FR-12:** bao phủ PM-13, PM-14; khoản chưa nghiệm thu/chưa thực nhận không vào KPI/hoa hồng; B11 (80/100/150 triệu → hoa hồng 1,6/2/3 triệu; KPI 0/1,5/1,5 triệu); B12 (nửa tháng, 60 triệu → chỉ tiêu 50 triệu, hoa hồng 1,2 triệu, KPI 1,5 triệu); dữ liệu vào payroll có trạng thái xác nhận và nguồn.

## 3.14. FR-13 – Tạo và quản lý kỳ lương

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR có grant `PREPARE` |
| Ưu tiên | MVP |

1. HR tạo kỳ theo tháng dương lịch; lưu thời gian kỳ, ngày chi trả dự kiến, trạng thái, người tạo, phiên bản tham số. Không cho hai kỳ chồng nhau.
2. Nhân viên đủ điều kiện được đưa vào kỳ theo hồ sơ, hợp đồng, hiệu lực (kể cả nhân viên nghỉ việc còn khoản phải trả).
3. Trước khi tính, kiểm tra bắt buộc: hồ sơ/lương/lịch, công đã chốt, PTO, OT, phụ cấp/thưởng, Sales (nếu áp dụng), hồ sơ thuế/BH (FR-33), khấu trừ, tham số hiệu lực (FR-23), `H_std`, quyền của người thao tác.
4. Thiếu dữ liệu: báo rõ nhân viên/khoản mục và `Needs Review`; không gán 0, bỏ qua hoặc dùng lương mẫu.
5. Kết quả tính lưu snapshot đầu vào, `H_std`, ngày chi trả, phiên bản tham số.
6. Chỉ tài khoản có grant `VIEW` mới xem chi tiết bảng lương toàn kỳ.

**Trạng thái chuẩn:** `DRAFT → CALCULATED → APPROVED → LOCKED`. `CALCULATED → DRAFT` được phép trước duyệt (có lý do, audit, vô hiệu kết quả cũ, mở chốt công theo FR-30). Không có đường mở lại `APPROVED`/`LOCKED`. `PAID` là trạng thái chi trả riêng (FR-17). Không có trạng thái kỳ lương nào khác ngoài bốn trạng thái trên; kỳ có dòng lương Director vẫn ở `CALCULATED` và bị chặn `APPROVE` theo DEC-02.

**AC-FR-13:** không tính khi còn thiếu dữ liệu bắt buộc chưa xử lý; kết quả gắn kỳ, người thực hiện, snapshot; chỉ `PREPARE` được chuẩn bị/tính; kỳ chồng bị chặn.

## 3.15. FR-14 – Tính lương theo công và thay đổi giữa kỳ

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR `PREPARE`; hệ thống tính |
| Ưu tiên | MVP |

**Đầu vào:** lương hiệu lực, lịch cá nhân, `H_std` toàn kỳ, `H_paid`, hợp đồng, các khoảng đổi lương/lịch/chức danh, lễ/nghỉ, công đã chốt.

```text
R_hour = L_month / H_std
Salary_attendance = ROUND_VND(L_month × H_paid / H_std),  0 ≤ H_paid ≤ H_std
Salary_attendance = ROUND_VND(Σ_s L_month_s × H_paid_s / H_std_s)   (nhiều đoạn hiệu lực)
```

`H_std` là tổng giờ phải làm theo lịch cá nhân của **toàn tháng**, đã loại lễ nghỉ hưởng lương; vào/nghỉ giữa tháng chỉ làm tử số thuộc thời gian hợp đồng, **không** rút mẫu số. `H_paid` hợp các khoảng làm thường xác nhận, công tác được xác nhận, nghỉ hưởng lương hợp lệ; loại chồng lặp; tính bằng phút chính xác (`H = phút/60`); OT và giờ làm lễ không cộng vào `H_paid`. Cộng các đoạn chưa làm tròn, làm tròn một lần ở dòng lương theo công.

**Quy tắc:** đủ công nhận đủ lương tháng, kể cả tháng có lễ (B01, B02); ngày lễ không trừ lương, không cộng trùng; `H_std = 0` → không chia cho 0 hay tự trả 0, HR nhập kết quả có căn cứ; dữ liệu thiếu → `Needs Review`; công tương đương hiển thị (full-time `H_paid/8`, part-time `H_paid/4`) không dùng để tính tiền.

**AC-FR-14:** khớp P01–P04, P08, B01–B06; không mặc định 22 ngày; không cộng trùng lễ; không làm tròn công trước khi tính tiền; thử việc/part-time/thực tập dùng điều kiện hợp đồng.

## 3.16. FR-15 – Tính tiền OT và làm đêm

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR `PREPARE`; dữ liệu OT do Manager xác nhận, HR đối chiếu |
| Ưu tiên | Mở rộng ưu tiên cao |

1. Chỉ dùng giờ OT thực tế đủ căn cứ đã xác nhận.
2. Tách đoạn tại 00:00, 06:00, 22:00, khi đổi loại ngày/lương/lịch. Ưu tiên lễ > nghỉ tuần > ngày thường; không cộng chồng hệ số 200% và 300%.
3. `R_ot` = tiền lương thực trả của công việc đang làm chia tổng giờ làm thường tương ứng (đã loại OT và thời gian nghỉ hưởng lương), theo Điều 55 NĐ 145/2020; chỉ khoản có `included_in_ot_base = true` vào căn cứ; loại tiền OT, phần trả thêm đêm, thưởng, ăn ca, điện thoại, đi lại.
4. Công thức:
```text
OT_day_segment = R_ot × H_ot_day × k            (k = 1,5 / 2 / 3)
Night_regular_extra = R_ot × H_night_regular × 0,3
OT_night_segment = R_ot × H_ot_night × (k + 0,3 + 0,2 × k_day)
```
Hệ số OT đêm tối thiểu: ngày thường chưa OT ban ngày 2,0; đã OT ban ngày 2,1; nghỉ tuần 2,7; lễ 3,9. Giờ đêm thường chỉ cộng 30%; OT đêm dùng công thức đầy đủ, không cộng 30% lần nữa. OT lễ cộng riêng khỏi lương tháng.
5. Không làm tròn giờ thành nửa giờ, không loại thời gian dưới 30 phút. Cộng đoạn chưa làm tròn theo từng dòng (thường/nghỉ tuần/lễ/đêm), rồi `ROUND_VND` từng dòng.
6. Không áp ca đêm giả định cho lịch ban ngày HNQ.
7. Tham số hệ số, đơn giá, giới hạn lấy từ FR-23.

**AC-FR-15:** test B07–B10 (B08: R_ot 100.000, 2 giờ → 300.000/400.000/600.000; B09 → 400.000/420.000/540.000/780.000); qua 22:00/00:00/đổi loại ngày; vượt giờ duyệt; thiếu xác nhận; P06.

## 3.17. FR-16 – Tính bảo hiểm và thuế thu nhập cá nhân

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR `PREPARE`; người xác nhận tham số theo quy chế |
| Ưu tiên | MVP trong phạm vi mô phỏng |

**Bảo hiểm (payroll_rules mục 7):**
```text
B_bhxh = min(mức đóng BHXH hợp lệ, 20 × mức tham chiếu)
B_bhyt = min(mức đóng BHYT hợp lệ, 20 × mức tham chiếu)
B_bhtn = min(mức đóng BHTN hợp lệ, 20 × lương tối thiểu vùng)
BHXH = ROUND_VND(B_bhxh × 8%); BHYT = ROUND_VND(B_bhyt × 1,5%); BHTN = ROUND_VND(B_bhtn × 1%)  (nếu tham gia quỹ tương ứng)
```
- `insurance_salary` do HR xác nhận theo tháng; không mặc định bằng `base_salary`, không dùng lương theo công đã giảm; đổi mức giữa tháng dùng mức tháng HR xác nhận, không tự bình quân.
- Tham gia từng quỹ xác định riêng (không một cờ miễn cả ba quỹ). Nghỉ không lương dưới 14 ngày làm việc không tự chia tỷ lệ; từ 14 ngày, ốm/thai sản, tháng quay lại: theo hồ sơ từng quỹ (B15). Phần doanh nghiệp đóng lưu riêng, không trừ Net.
- Thiếu hồ sơ → `NEEDS_REVIEW` nêu rõ trước approve.

**Thuế TNCN:**
- Phương pháp theo đối tượng: cư trú lũy tiến 5 bậc; cư trú không HĐLĐ/HĐLĐ dưới 3 tháng khấu trừ 10% (từ 5.000.000/lần, có điều kiện); không cư trú phương pháp riêng (thông thường 20%). Không suy phương pháp từ Net/role; không chia nhỏ lần trả; tổng hợp lần trả cùng tháng.
- Công thức bộ 2026:
```text
Taxable_income = Gross − Exempt_income − Non_taxable_income
Family_relief = 15.500.000 + N_dependents_valid × 6.200.000
T = max(0, Taxable_income − Insurance_deductible − Family_relief − Other_legal_relief)
PIT = ROUND_VND(Σ_b max(0, min(T, upper_b) − lower_b) × rate_b)
```
Bậc: ≤10 triệu 5%; ≤30 triệu 10%; ≤60 triệu 20%; ≤100 triệu 30%; >100 triệu 35% (T/tháng). Giảm trừ không chia theo công. Thuế tra theo thời điểm chi trả (lương tháng 12 trả tháng 1 không mặc định dùng bộ tháng 12). Phần OT/làm đêm đủ điều kiện miễn theo bộ 2026; phần vượt/không đủ điều kiện phân loại riêng. Kỳ lịch sử trước 2026 dùng bộ riêng.
- Nhánh phương pháp engine chưa hỗ trợ phải nhận kết quả HR xác nhận có chứng từ, không âm thầm dùng lũy tiến.

**Tham số:** mỗi tham số có mã, giá trị/đơn vị, phạm vi, địa bàn, hiệu lực, nguồn, người xác nhận, phiên bản (FR-23). Không tìm thấy bộ phù hợp hoặc hồ sơ chưa xác minh → báo lỗi cần rà soát, không lấy bộ mới nhất, không dùng tỷ lệ hard-code. Các tỷ lệ/mức trần trong `payroll_rules.md` chỉ là bộ tham số mô phỏng đã ghi nhận.

**AC-FR-16:** MVP phải khớp P01–P04, P07, P08 và B13–B15 trong phạm vi tham số mô phỏng được chốt; P05–P06 và B21 là bộ kiểm thử mở rộng. Phải kiểm tra tham số theo hiệu lực, thiếu tham số, ngày chi khác kỳ và khả năng truy về phiên bản tham số. Các con số ví dụ trong bộ mô phỏng là dữ liệu kiểm thử, không phải khẳng định pháp lý.

## 3.18. FR-17 – Rà soát, đối soát, phê duyệt, khóa kỳ và ghi nhận chi trả

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR `PREPARE`; Finance Manager đối soát; Director `APPROVE`; Finance Manager `LOCK`; Finance ghi nhận chi trả |
| Ưu tiên | MVP |

**Luồng bắt buộc:**
1. HR tập hợp, kiểm tra nguồn, chốt công (FR-30), chạy tính, rà soát (`DRAFT → CALCULATED`).
2. Finance Manager đối soát căn cứ tài chính và khoản thu Sales; không đồng nghĩa `APPROVE`/`LOCK`.
3. Director dùng grant `APPROVE` duyệt (`CALCULATED → APPROVED`).
4. Finance Manager dùng grant `LOCK` khóa (`APPROVED → LOCKED`).
5. Sau `LOCKED`: kết quả và phiếu chỉ đọc; hệ thống tạo phiếu PDF (FR-18).
6. **Chi trả:** sau khóa, Finance ghi nhận chi trả → trạng thái `PAID` (kèm ngày/chứng từ). `PAID` không mở quyền sửa lương và không thay `LOCKED`; ngày phát hành phiếu lương lưu riêng.
7. Sai sót sau khóa: adjustment (FR-24) vào kỳ mở tiếp theo.

**Kiểm tra trước approve/lock:** tổng dòng khớp, đúng phiên bản, đúng thẩm quyền, grant còn hiệu lực, không còn ngoại lệ trọng yếu chưa phân loại (`NEEDS_REVIEW`), người duyệt không phải người lập.

**Tách biệt nhiệm vụ:** người lập/tính không duyệt (PM-08, ghi security event); `APPROVE` và `LOCK` độc lập (PM-07); HR không approve/lock chỉ nhờ role (PM-06); Admin không có quyền payroll (PM-05, PM-19); thiếu người có grant → giữ trạng thái chờ, không tự nâng quyền.

**Xung đột lợi ích Director (DEC-02, PM-09):** Khi kỳ có dòng lương của Director, kỳ vẫn ở `CALCULATED` và hệ thống chặn `APPROVE` bằng `409 CONFLICT_OF_INTEREST` cho tới khi có bản ghi quyết định xử lý xung đột đã được cấp có thẩm quyền HNQ xác nhận (căn cứ/số quyết định, người xác nhận, ngày xác nhận). Nội dung biện pháp xử lý là vấn đề mở `OPEN-05` của `payroll_rules.md`; SRS không tự xác lập biện pháp. Hệ thống không thêm trạng thái kỳ lương, không tự chọn người duyệt thay, không tự tạo grant và không tách duyệt từng phiếu; Director không duyệt phần lương của chính mình. Nếu quyết định của HNQ dẫn tới thay đổi người giữ `APPROVE`, việc cấp grant đi theo quy trình grant thông thường (DEC-03, FR-32) và có audit. Với MVP, đây là ca kiểm soát bảo mật; demo có thể dùng dữ liệu kiểm thử riêng, không cần kỳ lương Director để chứng minh luồng payroll thông thường.

**Giao dịch, idempotent, đồng thời:** xem NFR-09.

**Lịch HNQ:** xem Mục 7.2 (kể cả dời mốc khi ngày trả sớm hơn ngày 05).

**AC-FR-17:** chỉ chuyển trạng thái theo luồng cho phép; `APPROVED` không tính lại, `LOCKED` không cập nhật/xóa, sửa kỳ khóa trả `409` (PM-10, B17); APPROVE không khóa được (PM-07); HR không approve/lock bằng role (PM-06); người lập không tự duyệt (PM-08); kỳ có lương Director bị chặn tới khi có biện pháp xác nhận (PM-09); `PAID` ghi nhận không thay đổi dữ liệu kỳ; sai sót sau khóa chỉ qua adjustment.

## 3.19. FR-18 – Tạo và xem phiếu lương PDF

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Hệ thống tạo PDF sau khóa; Employee xem/tải phiếu của mình; HR xem theo grant/nhiệm vụ |
| Ưu tiên | MVP |

**Nội dung tối thiểu:** thông tin công ty; kỳ lương; mã, họ tên, chức danh; lương cơ bản và khoảng hiệu lực; giờ chuẩn và giờ được trả; lương theo công; OT; từng phụ cấp; hoa hồng/thưởng; Gross; từng khoản bảo hiểm; thuế TNCN; khấu trừ/adjustment; **Net và Payable** (phân biệt Net sau nghĩa vụ, thu hồi tạm ứng, hoàn chi, thực chi).

**Yêu cầu:**
1. Phiếu chính thức chỉ phát hành sau khi kỳ `LOCKED`; thời điểm phát hành lưu riêng, theo lịch chi.
2. PDF phản ánh snapshot đã khóa, không đổi khi hồ sơ hiện tại thay đổi; không sửa PDF/snapshot kỳ gốc.
3. Employee chỉ xem/tải phiếu của mình; không tin `employee_id` từ client (PM-02).
4. HR xem phiếu người khác theo grant/nhiệm vụ; Manager không mặc nhiên xem; Admin không mặc nhiên xem (PM-05, PM-19).
5. Tạo/tải/xuất ghi audit.
6. Tạo PDF là tác vụ chống trùng sau commit khóa; lỗi PDF/email retry có log, không đảo trạng thái kỳ, không gửi trước commit (NFR-09).

**AC-FR-18:** không tạo phiếu chính thức từ kỳ chưa `LOCKED`; A không tải phiếu của B; Gross/Net/Payable trong PDF khớp bảng lương đã khóa; tạo lại không sinh phiếu trùng.

## 3.20. FR-19 – Cổng tự phục vụ nhân viên (ESS)

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Employee; Manager/HR theo quyền tương ứng |
| Ưu tiên | Mở rộng |

ESS cho nhân viên xem thông tin cá nhân được phép, hợp đồng của mình, bảng công, **số dư phép (đầu kỳ, phát sinh, đã dùng, còn lại, lịch sử điều chỉnh – FR-28)**, đơn PTO/OT, phiếu lương của mình. Dashboard chỉ lấy dữ liệu theo người đăng nhập. Trường nhạy cảm (lương, thuế, bảo hiểm, ngân hàng) theo chính sách hiển thị riêng.

**AC-FR-19:** API không trả dữ liệu người khác; hiển thị khớp quyền backend; tài khoản bị vô hiệu hóa không truy cập ESS.

## 3.21. FR-20 – Thông báo

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Hệ thống; người dùng nhận thông báo của mình |
| Ưu tiên | Mở rộng; thông báo trạng thái quan trọng có thể dùng cơ chế in-app đơn giản trong MVP |

**Sự kiện:** PTO/OT gửi, duyệt, từ chối, cần rà soát; đơn `Pending` sắp chốt công; hợp đồng sắp hết hạn; kỳ lương/phiếu lương sẵn sàng; dữ liệu cần bổ sung; kỳ bị chặn duyệt do xung đột lợi ích.

**Yêu cầu:** mỗi thông báo có người nhận, thời điểm, loại sự kiện, nội dung tối thiểu, trạng thái đã đọc; người dùng chỉ xem thông báo của mình; email (nếu tích hợp) xử lý bất đồng bộ, lỗi có log và retry; nếu chưa cấu hình email, ghi rõ là in-app/demo, không giả nhận đã gửi email. Cấu hình nhận email là của từng người dùng.

**AC-FR-20:** thông báo tạo đúng sự kiện và người nhận; không lộ thông tin nhạy cảm cho người không có quyền; không tuyên bố "đã gửi email" khi chưa tích hợp.

## 3.22. FR-21 – Dashboard và báo cáo

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR; Manager trong phạm vi team; Admin chỉ thông tin kỹ thuật; người có grant `VIEW` cho báo cáo lương |
| Ưu tiên | Mở rộng |

**Báo cáo:** nhân viên theo phòng ban/trạng thái; PTO/OT chờ xử lý; hợp đồng sắp hết hạn; tổng giờ OT; quỹ lương theo kỳ; bảng công, bảng lương theo phạm vi.

1. Dữ liệu theo quyền và phạm vi người xem; không lộ lương qua tổng hợp.
2. Báo cáo lương và xuất file cần grant `VIEW`; Admin không mặc nhiên (PM-19).
3. Xuất file áp dụng cùng kiểm tra quyền như màn hình.
4. Ghi audit khi xuất dữ liệu nhạy cảm.
5. Không mô tả là "thời gian thực" nếu hệ thống không đảm bảo.

**AC-FR-21:** người không có quyền không xem/xuất được dữ liệu payroll; số liệu khớp nguồn theo bộ lọc; phạm vi team/company kiểm tra phía server.

## 3.23. FR-22 – Audit log và approval log

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Hệ thống tự ghi; HR/Admin xem trong phạm vi được cấp |
| Ưu tiên | Audit cốt lõi: MVP; báo cáo audit nâng cao: Mở rộng |

**Audit log** ghi tối thiểu: người thực hiện, thời điểm (múi giờ nhất quán), hành động, loại/bản ghi, kết quả, lý do khi cần, giá trị trước/sau khi lưu an toàn. Thao tác cần ghi: cập nhật hồ sơ/hợp đồng, điều chỉnh công, chốt/mở chốt công, thay đổi khoản thu nhập/khấu trừ, thay đổi tham số payroll, cấp/thu hồi grant, thay đổi tuyến duyệt, quản trị tài khoản, đổi trạng thái kỳ lương, ghi nhận chi trả `PAID`, ghi nhận quyết định xử lý xung đột lợi ích, duyệt/từ chối PTO/OT, tự duyệt bị chặn (security event), xuất dữ liệu nhạy cảm.

**Approval log:** người gửi, người duyệt, hành động, thời gian, lý do, trạng thái trước/sau, căn cứ dùng người dự phòng.

**Bất biến:** `audit_log` và `payroll_audit` append-only; không có API sửa/xóa; chặn `UPDATE/DELETE` ở tầng lưu trữ (PM-12). Không lưu mật khẩu, token, ảnh định danh hoặc dữ liệu nhạy cảm vượt nhu cầu kiểm toán. Xem audit log giới hạn theo quyền (HR/Admin: chỉ đọc).

**AC-FR-22:** thao tác nhạy cảm tạo log; không sửa/xóa log qua API; cập nhật/xóa trực tiếp bị chặn; log cho biết ai làm gì, khi nào, kết quả.

## 3.24. FR-23 – Tham số nghiệp vụ theo hiệu lực

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR đề xuất/xác nhận theo thẩm quyền; Admin chỉ cấu hình kỹ thuật |
| Ưu tiên | MVP đối với tham số dùng tính lương |

1. Tham số có mã, tên, giá trị/đơn vị, phạm vi, địa bàn (nếu có), `effective_from/to`, nguồn/văn bản/điều khoản, người và ngày xác nhận, trạng thái xác minh, phiên bản.
2. Tách **tham số công ty** (ví dụ 730.000 ăn trưa, 2% hoa hồng, 100 triệu chỉ tiêu, 1,5 triệu KPI) và **tham số pháp luật** (thuế, bảo hiểm, lương tối thiểu, giới hạn OT).
3. Không chồng lấn hiệu lực cho cùng phạm vi.
4. Khi tính, chọn bộ đúng ngày/kỳ nghiệp vụ: lương/lịch/phụ cấp/OT tra theo ngày phát sinh; bảo hiểm theo tháng tham gia; thuế theo thời điểm chi trả; không tự lấy bộ mới nhất khi thiếu.
5. Thay đổi tham số không viết lại kết quả kỳ đã khóa.
6. Admin không quyết định chính sách lương/thuế/BH/OT bằng quyền kỹ thuật.
7. Tham số chưa xác minh có trạng thái `NEEDS_REVIEW`; trạng thái này hiển thị khi tính và trong báo cáo kỳ.

**AC-FR-23:** truy xuất được phiên bản tham số của mỗi kỳ; thiếu/hết hiệu lực gây lỗi cần rà soát, không fallback; thay đổi có audit.

## 3.25. FR-24 – Adjustment sau khóa kỳ

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR (`PREPARE`) lập; Director (`APPROVE`, đúng ủy quyền) duyệt |
| Ưu tiên | MVP |

1. Không sửa trực tiếp kỳ `LOCKED`.
2. Adjustment gồm: kỳ gốc, kỳ ghi nhận/chi trả, nhân viên, **loại điều chỉnh `GROSS` / `TAX` / `INSURANCE` / `NET`**, số tiền có dấu, lý do, chứng từ, nguồn duy nhất, người tạo, người duyệt.
3. `GROSS` đi qua thuế/bảo hiểm tương ứng; chỉ `NET` thuần túy cộng/trừ ở cuối; không vừa cộng Gross vừa cộng Net cùng nguồn.
4. Người tạo không tự duyệt.
5. Adjustment đã duyệt vào kỳ mở tiếp theo; kỳ gốc, PDF và snapshot giữ nguyên.
6. Hoàn tiền Sales sau khóa, sai thu nhập, sai bảo hiểm/thuế kỳ cũ đều qua adjustment; sai BH/thuế còn cần hồ sơ nghĩa vụ tương ứng (không thay tờ khai điều chỉnh).
7. Không để số âm bị ép về 0 rồi mất dấu: lưu nghĩa vụ, phải thu, hoãn thu, quyết định phân bổ.

**AC-FR-24:** liên kết kỳ gốc và kỳ chi trả; kỳ gốc không đổi; người tạo không tự duyệt; thiếu chứng từ/lý do bị chặn; B16, B24 (không trùng nguồn; Gross/Net adjustment phân loại đúng).

## 3.26. FR-25 – Cảnh báo hợp đồng và tác vụ định kỳ

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Hệ thống; HR nhận thông báo |
| Ưu tiên | Mở rộng |

Tác vụ định kỳ rà soát: hợp đồng sắp hết hạn (mặc định trước 30 ngày), tài khoản đến ngày vô hiệu hóa, đơn PTO `Pending` gần chốt công, kỳ cần chuẩn bị theo lịch. Có log thực thi; không tạo lặp thông báo khi điều kiện không đổi; ngày hiệu lực theo `Asia/Ho_Chi_Minh`.

**AC-FR-25:** chạy lặp không tạo dữ liệu trùng; đúng múi giờ; lỗi tác vụ có log.

## 3.27. FR-26 – Xử lý lỗi và phản hồi nghiệp vụ

Phản hồi lỗi có cấu trúc thống nhất (mã lỗi nghiệp vụ, thông điệp, trường liên quan):

| Mã | Ý nghĩa | Ví dụ |
|---|---|---|
| `400` | Request sai cấu trúc | Thiếu trường bắt buộc về cấu trúc |
| `401` | Chưa xác thực/token sai/hết hạn | PM-01, PM-18 |
| `403` | Có xác thực nhưng thiếu quyền chức năng/grant | PM-05, PM-06, PM-07, PM-08 |
| `404` | Bản ghi không tồn tại hoặc ngoài phạm vi (không lộ sự tồn tại) | PM-02, PM-03 |
| `409` | Xung đột trạng thái nghiệp vụ | Sửa kỳ `LOCKED`; duyệt kỳ có lương Director khi chưa có quyết định xử lý xung đột (`CONFLICT_OF_INTEREST`) |
| `422` | Vi phạm quy tắc nghiệp vụ/validation | Khoảng ngày PTO sai; `employee_id` giả mạo (PM-15) |
| `500` | Lỗi hệ thống | Không xử lý được ở backend |

Quy ước cuối cùng chốt trong OpenAPI theo DEC-01. Response không trả stack trace, mật khẩu, token, thông tin ngân hàng hoặc chi tiết nhạy cảm không cần thiết.

**AC-FR-26:** lỗi phổ biến có mã và cấu trúc nhất quán; không lộ thông tin nhạy cảm; giao dịch thất bại không để lại trạng thái dở dang.

## 3.28. FR-27 – Tổng hợp Gross, Net, Payable, khấu trừ và làm tròn

*(Chức năng tổng hợp Gross/Net/Payable; mã hiện tại FR-27 theo hệ liền mạch bản 3.2.)*

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR có grant `PREPARE` |
| Ưu tiên | MVP |

**Công thức:**
```text
Gross = Salary_attendance + OT_pay (gồm Night_extra) + Allowances
      + Eligible_Sales_Commission + Approved_Bonuses + Other_Eligible_Additions

Net_entitlement = Gross − Insurance_employee − PIT − Legal_deductions
                + Net_adjustment_credit − Net_adjustment_debit

Payable = Net_entitlement − Advance_recovery + Reimbursements
```
(Adjustment loại `GROSS` đã nằm trong Gross, `TAX`/`INSURANCE` trong khoản tương ứng; chỉ `NET` vào hai vế trên.)

**Yêu cầu:**
1. Mỗi khoản tính đúng một lần; không cộng lại phụ cấp khi tính Net.
2. Hệ thống phân biệt Net_entitlement và Payable. Nếu lưu một trường `net_salary`, chốt trường đó là **Payable** và lưu thành phần đối soát; không thu hồi tạm ứng hai lần (ở `other_deductions` và `Advance_recovery`).
3. **Tạm ứng và hoàn chi** có chứng từ, nguồn duy nhất, phê duyệt; hoàn chi không cộng Gross rồi trừ như khấu trừ.
4. `ROUND_VND(x) = x.setScale(0, HALF_UP)` kể cả số âm (B22); tính trung gian bằng BigDecimal/decimal, chia tối thiểu 12 chữ số thập phân, không làm tròn đơn giá; làm tròn từng dòng cuối (lương sau cộng đoạn, từng phụ cấp, từng nhóm OT, từng quỹ BH, hoa hồng, từng quyết định thưởng, thuế sau cộng bậc); Gross/Net/Payable cộng các dòng đã làm tròn; không phân bổ chênh lệch tổng toàn công ty.
5. Mỗi dòng lưu loại khoản, giá trị, nguồn, quy tắc/phiên bản tham số, số trước/sau làm tròn.
6. Không trả âm: xử lý theo FR-24 mục 7.

**AC-FR-27:** Gross/Net/Payable khớp phép cộng các dòng; MVP khớp P01–P04, P07, P08; P05–P06 là test mở rộng. Không cộng lặp phụ cấp; khấu trừ thiếu căn cứ bị chặn; kiểm tra B22, B24.

## 3.29. FR-28 – Quỹ phép

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR quản lý; Employee xem số dư của mình; Manager xem team |
| Ưu tiên | MVP (phép năm); các loại khác Mở rộng |

1. Quỹ phép năm: 12 ngày/năm khi đủ 12 tháng trong điều kiện bình thường; chưa đủ năm tính tỷ lệ theo thời gian và quy định áp dụng. Lưu theo ngày hoặc quy đổi giờ theo lịch của từng người; hỗ trợ 0,5 ngày, 1 ngày, nhiều ngày.
2. Số dư gồm: đầu kỳ, phát sinh, đã dùng, còn lại, lịch sử điều chỉnh (có người, lý do).
3. Kiểm tra số dư và đơn đã duyệt để ngăn dùng trùng; không cho số dư âm theo chính sách thông thường.
4. Chỉ trừ quỹ khi đơn phép năm được duyệt; nghỉ việc riêng hưởng lương, lễ/Tết không trừ phép năm; nghỉ ốm/thai sản phân loại theo chế độ, không mặc định là phép năm hay không lương; nghỉ vào ngày không phải ngày làm việc theo lịch không trừ.
5. Khi chấm dứt hợp đồng, HR rà soát ngày phép chưa nghỉ để thanh toán nếu thuộc trường hợp được áp dụng; khoản thanh toán là dòng riêng, có căn cứ và phân loại thuế riêng (không dùng tùy tiện đơn giá tháng hiện tại).
6. Hủy/điều chỉnh đơn đã duyệt hoàn trả quỹ theo FR-08.

**AC-FR-28:** không cho nghỉ vượt số dư; đơn `Pending` không trừ quỹ; hủy đơn đã duyệt hoàn quỹ; phép không trừ vào ngày không phải ngày làm việc; số dư khớp lịch sử.

## 3.30. FR-29 – Lịch làm việc, lịch lễ/Tết và giờ chuẩn

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR |
| Ưu tiên | MVP |

1. Lịch làm việc cá nhân có hiệu lực theo thời gian (ngày trong tuần, ca, nghỉ trưa); mẫu full-time và part-time (Mục 2.2.2) là bản mẫu, không phải mặc định áp cho mọi người.
2. Danh mục ngày lễ/Tết/nghỉ bù theo lịch công bố, có năm và hiệu lực; ngày lễ rơi vào ngày làm việc: hưởng lương theo chế độ, không trừ phép, không cộng công trùng.
3. Hệ thống tính **`H_std` toàn kỳ** từ lịch cá nhân hiệu lực trừ lễ nghỉ hưởng lương, theo từng đoạn hiệu lực, và lưu snapshot cùng kỳ lương. Không dùng 22/26 làm quy tắc; 22 chỉ là dữ liệu ví dụ.
4. Đổi lịch giữa kỳ áp dụng từ ngày hiệu lực, không áp ngược.
5. Nếu `H_std = 0`, theo FR-14.
6. Tính bằng phút chính xác; ngày/giờ chuẩn phục vụ cả FR-11 (mẫu số phụ cấp) và FR-14.

**AC-FR-29:** tháng 20/22/23 ngày đều ra `H_std` đúng (B01); lễ không trừ lương (B02); lịch đổi giữa kỳ chia đoạn đúng; snapshot `H_std` lưu theo kỳ.

## 3.31. FR-30 – Chốt công và snapshot nguồn kỳ lương

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR (`PREPARE`) |
| Ưu tiên | MVP |

1. **Chốt công** là hành động HR khóa nguồn dữ liệu công/phép/OT/thu nhập của kỳ trước khi tính; lưu người, thời điểm, **phiên bản snapshot** riêng.
2. Điều kiện chốt: không còn bản ghi `Needs Review` chưa xử lý trọng yếu; đơn PTO `Pending` đã được xử lý hoặc có quyết định phần được trả (không tự coi là 0 hay nghỉ không lương); dữ liệu OT/Sales liên quan đã được xác nhận hoặc người có trách nhiệm xác nhận rõ “không phát sinh”. Trong MVP, OT/Sales chỉ là dữ liệu đã xác nhận nhập thủ công/seed, không triển khai quy trình OT/Sales tự động; hồ sơ thuế/BH đã đủ theo bộ mô phỏng.
3. Nếu còn phần chờ xác minh: HR lập hồ sơ chi phần hợp lệ và điều chỉnh riêng cho phần chờ; hệ thống không tự giữ toàn bộ lương, không coi `Pending` là 0.
4. Sau chốt, nguồn của kỳ chỉ đọc; thay đổi nguồn (ví dụ duyệt PTO muộn) không tác động snapshot đã chốt mà đi vào điều chỉnh kỳ sau.
5. `CALCULATED → DRAFT` để sửa nguồn phải **mở chốt có lý do**, ghi audit, vô hiệu kết quả tính cũ; chốt lại tạo phiên bản snapshot mới.
6. Sau `APPROVED`/`LOCKED`, không mở chốt.
7. Chốt công chạm đồng thời với duyệt PTO/OT được kiểm soát bằng khóa/phiên bản (NFR-09, B19).

**AC-FR-30:** snapshot có người/thời điểm/phiên bản; mở chốt cần lý do và có audit; không mở chốt sau `APPROVED`; duyệt PTO đồng thời với chốt cho một phiên bản nhất quán (B19); đơn `Pending` không tự thành 0 hay nghỉ không lương (B04).

## 3.32. FR-31 – Quản trị tài khoản

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Admin; HR gửi yêu cầu theo hồ sơ |
| Ưu tiên | MVP |

1. Admin khởi tạo tài khoản theo yêu cầu hợp lệ của HR (DEC-04); mật khẩu khởi tạo buộc đổi lần đầu.
2. Admin khóa/mở tài khoản, đặt lại mật khẩu, gán/đổi **một role nền tảng** trong bốn role; mọi thay đổi có audit, lý do, người yêu cầu.
3. Vô hiệu hóa tài khoản theo ngày hiệu lực nghỉ việc do HR ghi nhận (FR-06); không xóa tài khoản/lịch sử.
4. Admin xem định danh tối thiểu để quản trị; không xem lương, hợp đồng nhạy cảm, ngân hàng, thuế.
5. Admin không tự cấp cho mình grant payroll hay thao tác duyệt/khóa.

**AC-FR-31:** tạo/khóa/mở/đổi role có audit; Admin không đọc được dữ liệu lương (PM-19); tài khoản bị khóa không truy cập được (PM-18); Admin không tự cấp grant payroll.

## 3.33. FR-32 – Grant, ủy quyền và cấu hình tuyến duyệt

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | Người có thẩm quyền cấp grant (DEC-03); Admin thực thi kỹ thuật theo quyết định; Director/HR Lead chỉ định người dự phòng trong trường hợp được nêu |
| Ưu tiên | MVP |

1. Grant payroll `VIEW`/`PREPARE`/`APPROVE`/`LOCK`: người cấp, người nhận, quyền, phạm vi, thời điểm bắt đầu/kết thúc, lý do, tham chiếu quyết định, trạng thái hiệu lực.
2. Grant chỉ cấp đúng tài khoản theo Company Profile (HR → `PREPARE`; Director → `APPROVE`; Finance Manager → `LOCK`); không cấp chung theo role `Manager`/`HR`.
3. Hết hạn/thu hồi có hiệu lực ngay (PM-20); không cho tài khoản tự cấp/gia hạn grant cho mình.
4. Không cho một tài khoản đồng thời giữ grant mâu thuẫn với kiểm soát tách biệt (người lập không duyệt bảng của mình; kiểm tra khi duyệt, không chỉ khi cấp).
5. **Tuyến duyệt PTO/OT:** lưu thành dữ liệu riêng gồm người gửi/nhóm, người duyệt chính, người dự phòng đã chỉ định trước, hiệu lực; việc chỉ định dự phòng (Director chỉ định Finance Manager cho HR Lead; HR Lead chỉ định HR Executive cho Director) phải có bản ghi trước khi dùng. Không dự phòng → đơn chờ.
6. Mọi thay đổi grant/tuyến có audit.

**AC-FR-32:** cấp/thu hồi có người cấp, phạm vi, hiệu lực, audit; grant hết hạn bị từ chối (PM-20); không cấp APPROVE/LOCK theo role; dự phòng chưa chỉ định không được dùng (PM-16, PM-17).

## 3.34. FR-33 – Hồ sơ thuế và bảo hiểm của nhân viên

| Thuộc tính | Nội dung |
|---|---|
| Tác nhân | HR; nhân viên cung cấp/xác nhận thông tin cá nhân |
| Ưu tiên | MVP (phạm vi mô phỏng) |

**Dữ liệu theo nhân viên và theo tháng hiệu lực:**
- Thuế: trạng thái cư trú; phương pháp khấu trừ áp dụng; danh sách người phụ thuộc hợp lệ (hồ sơ, hiệu lực); hồ sơ cam kết (cờ `tax_commitment_08` chỉ có giá trị khi có hồ sơ cam kết kèm); mã số thuế khi có.
- Bảo hiểm: tham gia từng quỹ BHXH/BHYT/BHTN (cờ riêng từng quỹ), `insurance_salary` do HR xác nhận theo tháng, báo tăng/giảm, ngoại lệ (nghỉ không lương ≥ 14 ngày, ốm/thai sản).
- Địa bàn làm việc áp dụng (liên kết FR-03) để tra lương tối thiểu vùng và trần BHTN.
- Tài khoản ngân hàng nếu dùng cho chi trả: dữ liệu nhạy cảm, chỉ HR/Finance theo nhiệm vụ.

**Quy tắc:** không suy nghĩa vụ thuế/BH từ `employee_type` hoặc role; thiếu hồ sơ → `NEEDS_REVIEW`; lưu người/ngày xác nhận; thay đổi có hiệu lực theo tháng và audit; không áp ngược tự động cho kỳ cũ.

**AC-FR-33:** thiếu hồ sơ thuế/BH chặn tính của nhân viên đó với thông báo cụ thể; cờ cam kết không có hiệu lực nếu thiếu hồ sơ; thay đổi áp dụng đúng tháng hiệu lực; dữ liệu nhạy cảm không lộ cho Manager/Admin.

---

# 4. Yêu cầu giao diện ngoài

## 4.1. Giao diện người dùng

Giao diện web dễ hiểu, dùng tiếng Việt nhất quán, trình bày rõ trạng thái và lỗi. Mỗi màn hình ẩn/hiện hành động theo quyền nhưng kiểm tra quyền bắt buộc vẫn ở backend.

Màn hình dự kiến:
1. Đăng nhập, đổi mật khẩu.
2. Trang chủ/dashboard theo role.
3. Hồ sơ nhân sự (danh sách, chi tiết); hồ sơ thuế/bảo hiểm.
4. Phòng ban/chức danh, hợp đồng, lịch làm việc và lịch lễ.
5. Chấm công cơ bản (màn hình QR/ảnh khi bật chế độ mở rộng), bảng công, danh sách `Needs Review`, chốt công.
6. PTO (danh sách, tạo, chi tiết, hộp thư duyệt, hủy/điều chỉnh); số dư phép.
7. Lịch OT, đăng ký OT, xác nhận giờ thực tế, OT ngoài lịch.
8. Phụ cấp/thưởng/khấu trừ/tạm ứng; dữ liệu Sales đã xác nhận.
9. Kỳ lương: danh sách lỗi đầu vào, bảng lương dự kiến, đối soát, duyệt, khóa, ghi nhận chi trả, cảnh báo chặn duyệt do xung đột lợi ích.
10. Adjustment.
11. Phiếu lương PDF/ESS.
12. Thông báo, báo cáo, audit, quản trị tài khoản, grant/tuyến duyệt, tham số.

### 4.1.1. Quy tắc giao diện
- Nhãn trạng thái thống nhất: `DRAFT`, `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `CALCULATED`, `LOCKED`, `PAID`, `Needs Review` tùy phân hệ.
- Thao tác không hoàn tác hoặc tác động tiền lương (duyệt, khóa, mở chốt, terminate, tạo adjustment, ghi nhận chi trả) cần xác nhận rõ và hiển thị phạm vi tác động.
- Trường bắt buộc có nhãn; lỗi validation gần trường tương ứng; không xóa dữ liệu đã nhập khi có thể tránh.
- Bảng chứa dữ liệu nhạy cảm chỉ hiển thị cột được phép.
- Thiếu dữ liệu payroll: nêu cụ thể nhân viên/khoản mục cần rà soát.
- Không hiển thị "đã gửi email" nếu chỉ tạo in-app notification.

## 4.2. Giao diện phần mềm và API

- Frontend giao tiếp Backend qua HTTP API theo OpenAPI; API đặc tả trước khi triển khai song song; thay đổi contract cần review.
- Backend chịu trách nhiệm xác thực, phân quyền, validation, nghiệp vụ.
- Response/error thống nhất theo FR-26 và DEC-01; endpoint dữ liệu nhạy cảm nêu quyền cần thiết và phạm vi dữ liệu.
- Thao tác chuyển trạng thái (approve, reject, lock, terminate, adjustment, chốt/mở chốt công, ghi nhận chi trả) kiểm tra trạng thái hiện tại, **phiên bản** (kiểm soát đồng thời) và quyền ở server.
- API không cho client tự gửi `user_id`, `employee_id`, role, grant để giả mạo chủ thể; trường định danh chỉ để tham chiếu đối tượng và phải được kiểm tra phạm vi.
- Tác vụ ghi lặp phải idempotent (NFR-09).

## 4.3. Giao diện cơ sở dữ liệu và lưu trữ

SRS không khóa tên bảng/kiểu dữ liệu. Thiết kế dữ liệu phải đáp ứng tối thiểu các nhóm: tài khoản/role/grant/tuyến duyệt; hồ sơ/phòng ban/chức danh/quan hệ quản lý; hợp đồng/lịch/lễ/lương hiệu lực; hồ sơ thuế/bảo hiểm; công và lịch sử điều chỉnh; chốt công và snapshot; PTO, quỹ phép, approval log; lịch/đăng ký/xác nhận OT, OT ngoài lịch; phụ cấp/thưởng/khấu trừ/tạm ứng/hoàn chi; Sales đã đối soát; tham số có phiên bản; kỳ lương, dòng lương, phiếu lương, chi trả, adjustment; thông báo; audit; nhật ký tác vụ định kỳ.

Dữ liệu cần lịch sử có thời điểm hiệu lực hoặc phiên bản. Kỳ khóa và audit log được bảo vệ khỏi `UPDATE/DELETE` ở cả tầng ứng dụng và tầng lưu trữ.

## 4.4. Giao diện với hệ thống ngoài

| Tích hợp | Mục đích | Điều kiện |
|---|---|---|
| Email | Thông báo, gửi phiếu lương | Chỉ coi là tích hợp thật khi cấu hình SMTP/provider và kiểm thử thành công |
| PDF renderer | Sinh phiếu lương | PDF khớp snapshot kỳ khóa |
| QR/camera | Chế độ chấm công mở rộng (FR-07) | Mở rộng; nếu demo chưa có camera/lưu trữ thật thì mô phỏng rõ và ghi rõ trong tài liệu demo |
| Bảng đối soát Sales | Nhận doanh thu/KPI đã xác nhận | Có thể nhập thủ công; không bắt buộc CRM |
| Git/CI | Kiểm thử tự động, kiểm soát thay đổi | Theo cấu hình repository |

**Quy tắc xử lý nền (dùng bởi UR-23):** gửi email/thông báo và tạo PDF chạy nền, có hàng đợi/retry/log, chống trùng, không chặn request nghiệp vụ chính.

---

# 5. Yêu cầu hiệu suất

Các ngưỡng là mục tiêu kiểm thử của đồ án, không phải SLA thương mại. Phải ghi môi trường và dữ liệu thử nghiệm khi báo cáo; chỉ số chưa đo ghi **"chưa kiểm chứng"**.

| Mã | Yêu cầu | Tiêu chí đo |
|---|---|---|
| PERF-01 | CRUD thông thường | p95 ≤ 2 giây trên môi trường demo, dữ liệu ghi rõ |
| PERF-02a | Tính lương theo lô – quy mô mẫu | 40 nhân sự ≤ 30 giây (ngưỡng nghiệm thu MVP) |
| PERF-02b | Tính lương theo lô – quy mô mở rộng (B23) | 500 nhân viên, 50 người dùng đồng thời, batch ≤ 5 phút; không bỏ/trùng bản ghi; có báo cáo đo (mục tiêu mở rộng, bắt buộc có báo cáo trước khi tuyên bố đạt) |
| PERF-03 | Truy xuất bảng công/PTO | p95 ≤ 2 giây, có phân trang |
| PERF-04 | Tạo phiếu PDF | ≤ 5 giây/phiếu; ghi rõ nếu theo lô |
| PERF-05 | Email/notification | Không chặn request chính; lỗi có log và retry |
| PERF-06 | Danh sách lớn | Phân trang/lọc; không trả toàn bộ dữ liệu nhạy cảm cho một trang |

---

# 6. Yêu cầu phi chức năng

## 6.1. NFR-01 – Bảo mật và xác thực
1. Mật khẩu băm bằng BCrypt hoặc tương đương; không lưu dạng rõ; không log.
2. **Chính sách mật khẩu:** độ dài tối thiểu 10 ký tự, có chữ và số; không trùng 3 mật khẩu gần nhất; buộc đổi mật khẩu khởi tạo lần đầu; (các ngưỡng là cấu hình).
3. **Khóa tạm** tài khoản sau 5 lần đăng nhập sai liên tiếp trong khoảng cấu hình; mở theo thời gian hoặc do Admin.
4. **Token/phiên:** access token hạn ngắn (mặc định ≤ 60 phút), cơ chế làm mới/thu hồi khi đăng xuất, khóa tài khoản hoặc đổi mật khẩu; token kiểm tra hạn và trạng thái tài khoản mỗi request.
5. **Dữ liệu nhạy cảm** (định danh, mã số thuế, tài khoản ngân hàng, ảnh minh chứng): mã hóa hoặc bảo vệ ở lớp lưu trữ theo thiết kế; che một phần khi hiển thị nếu không cần đủ; chỉ trả trong phạm vi quyền. Tuân thủ nguyên tắc tối thiểu hóa dữ liệu; nếu dùng ngoài đồ án phải rà soát quy định bảo vệ dữ liệu cá nhân hiện hành.
6. HTTPS/TLS khi truy cập qua mạng; không tuyên bố phiên bản TLS khi chưa cấu hình và xác minh.
7. Thông báo lỗi không lộ mật khẩu, token, stack trace, dữ liệu ngoài phạm vi.
8. Không log thông tin xác thực hoặc dữ liệu nhạy cảm không cần thiết.

**Kiểm thử:** đăng nhập sai, khóa tạm, token hết hạn/thu hồi, tài khoản bị khóa, API không token, kiểm tra response/log.

## 6.2. NFR-02 – Phân quyền và bảo vệ dữ liệu
1. Mọi endpoint nhạy cảm kiểm tra quyền ở backend, theo thứ tự nêu ở FR-02.
2. Phân quyền = role + phạm vi bản ghi + grant + trạng thái tài khoản + trạng thái bản ghi.
3. Employee chỉ xem dữ liệu cá nhân; Manager dữ liệu vận hành team; payroll theo grant `VIEW` và phạm vi.
4. `APPROVE`/`LOCK` độc lập, không suy từ role/chức danh.
5. Từ chối tự duyệt PTO/OT/payroll/adjustment và sửa kỳ khóa.
6. Test plan bao phủ PM-01 đến PM-20 (Phụ lục 12.2, ánh xạ FR ở Mục 10.3).

## 6.3. NFR-03 – Toàn vẹn và chính xác dữ liệu
1. Dữ liệu đầu vào payroll có nguồn, trạng thái xác nhận, mã nguồn duy nhất khi phù hợp.
2. Một phút công/OT không trả hai lần.
3. Không tính trùng ngày lễ, phụ cấp, khoản Sales, adjustment.
4. Payroll lưu snapshot đầu vào, `H_std`, ngày chi trả, phiên bản tham số.
5. Giao dịch thất bại không để lại trạng thái dở dang.
6. Kết quả đối chiếu được với toàn bộ P01–P08 và B01–B24 khi xét đầy đủ phạm vi sản phẩm; nghiệm thu MVP chỉ bắt buộc các ca được liệt kê ở Mục 8.1, các ca mở rộng được kiểm thử khi triển khai chức năng tương ứng.

## 6.4. NFR-04 – Audit và truy vết
1. Thay đổi nhạy cảm, grant, duyệt/từ chối, chuyển trạng thái payroll, chốt/mở chốt, xuất báo cáo nhạy cảm đều có log.
2. Log có người, thời gian, hành động, đối tượng, kết quả, lý do khi cần, trước/sau khi phù hợp.
3. `audit_log` và `payroll_audit` append-only; API không sửa/xóa.
4. Múi giờ log nhất quán, đủ chính xác để đối soát.
5. Xem audit giới hạn theo quyền.

## 6.5. NFR-05 – Khả dụng và khôi phục
- Availability tham khảo 99,5%/tháng chỉ khi có dữ liệu đo; demo cục bộ không tuyên bố SLA.
- Backup CSDL tối thiểu mỗi ngày với môi trường có dữ liệu cần bảo toàn.
- RPO ≤ 24 giờ, RTO ≤ 4 giờ chỉ xác nhận sau khi diễn tập backup/restore và đo thời gian.
- Tài liệu triển khai nêu cách tạo backup, nơi lưu, cách khôi phục, người được phép.

## 6.6. NFR-06 – Bảo trì và kiểm thử
1. Backend tách theo miền nghiệp vụ, xử lý lỗi thống nhất.
2. Quy tắc lương, thuế, bảo hiểm, OT và trạng thái kỳ có unit test; nghiệm thu MVP dùng bộ test tại Mục 8.1, còn P/B ngoài MVP là test mở rộng khi chức năng tương ứng được triển khai.
3. Coverage unit test mục tiêu ≥ 60% nếu có công cụ đo; báo cáo nêu công cụ, phạm vi, kết quả thật.
4. Thay đổi API qua pull request; CI chạy build/test trước merge.
5. OpenAPI, Use Case, ERD, traceability cập nhật khi yêu cầu đổi.

## 6.7. NFR-07 – Dễ sử dụng và tiếp cận
1. Thuật ngữ thống nhất với quy chế HNQ.
2. Trạng thái/lỗi giải thích bằng ngôn ngữ dễ hiểu.
3. Biểu mẫu có nhãn, chỉ rõ trường bắt buộc, không chỉ dựa màu sắc.
4. Thao tác quan trọng có xác nhận.
5. Bảng có lọc/phân trang; không hiển thị trường không có quyền.

## 6.8. NFR-08 – Tương thích và triển khai
1. Chạy được theo README trên môi trường nhóm công bố.
2. Mật khẩu DB, JWT secret, thông tin email không commit dạng bí mật thật.
3. Migration có thứ tự, chạy được trên DB sạch, không phá dữ liệu ngoài dự kiến.
4. Seed demo nhất quán với baseline: 4 role, cơ cấu HNQ, tuyến duyệt (kể cả dự phòng đã chỉ định), grant payroll, lịch/lễ, hồ sơ thuế/BH và bộ ví dụ P/B. Nếu seed trái baseline phải sửa seed.
5. Chức năng phụ thuộc dịch vụ ngoài chưa cấu hình phải ghi rõ giới hạn trong tài liệu demo.

## 6.9. NFR-09 – Toàn vẹn thực thi payroll
1. **Giao dịch:** ghi toàn bộ kết quả của một lần tính trong một transaction; lỗi giữa batch → rollback cả batch, không "nửa kỳ mới nửa kỳ cũ" (B18).
2. **Idempotent:** chạy lại cùng nhân viên/kỳ/phiên bản không tạo trùng dòng lương, hoa hồng, adjustment, phiếu.
3. **Đồng thời:** approve/lock/chốt công dùng kiểm soát phiên bản và khóa giao dịch để chống sửa nguồn hoặc duyệt PTO/OT đồng thời; sau chốt/khóa không đổi nguồn (B19).
4. **Trạng thái:** trái trạng thái trả `409`, trái quyền `403`; bảo vệ cả backend và DB, không chỉ ẩn nút.
5. **Sau commit khóa:** việc tạo PDF/thông báo chống trùng, retry có log, không đảo trạng thái kỳ, không gửi trước commit.
6. **Kiểu số:** BigDecimal/decimal cho mọi phép tính tiền và giờ, không float/double; chia tối thiểu 12 chữ số thập phân.
7. **Quy mô:** theo PERF-02b khi kiểm chứng.
8. **Kiểm thử:** B17, B18, B19, B22, B23 là test bắt buộc cho NFR này.

---

# 7. Quy tắc nghiệp vụ

Các BR là ràng buộc bắt buộc cho thiết kế và code. Công thức, hệ số, ví dụ chi tiết ở `payroll_rules.md`; không sao chép thành chính sách riêng trong code.

| Mã | Tên | Quy tắc |
|---|---|---|
| BR-01 | Role nền tảng | Chỉ `Employee`, `Manager`, `HR`, `Admin`; chức danh, tuyến duyệt, grant là thuộc tính riêng |
| BR-02 | Phạm vi dữ liệu | `employee_id` người đăng nhập lấy từ phiên/token và quan hệ đã lưu, không tin client |
| BR-03 | Quyền payroll | `VIEW`, `PREPARE`, `APPROVE`, `LOCK` độc lập; role/chức danh không tự cấp grant |
| BR-04 | Tách biệt nhiệm vụ | Người lập/tính không tự duyệt; Director cần `APPROVE`; Finance Manager cần `LOCK` |
| BR-05 | Trạng thái kỳ lương | `DRAFT → CALCULATED → APPROVED → LOCKED`; `CALCULATED → DRAFT` trước duyệt (có lý do, mở chốt, vô hiệu kết quả cũ); không tính lại `APPROVED`/`LOCKED` |
| BR-06 | Kỳ khóa chỉ đọc | Không update/delete kỳ `LOCKED`; sai sót qua adjustment ở kỳ mở tiếp theo |
| BR-07 | Đầu vào có căn cứ | Công, PTO, OT, phụ cấp, thưởng, Sales, BH, thuế, khấu trừ có nguồn/trạng thái xác nhận |
| BR-08 | Thiếu dữ liệu | Không thay bằng 0, lương mẫu hoặc tham số mới nhất; báo lỗi, `Needs Review` |
| BR-09 | Lịch làm việc | Lịch cá nhân hiệu lực; không mặc định 22/26 công; phút chính xác; `H_std` toàn tháng |
| BR-10 | Chấm công | MVP: check-in/check-out cơ bản (FR-07); QR động + ảnh minh chứng là mở rộng; trễ/sớm ghi từ phút đầu; không dung sai tự động; không phạt tiền; thiếu công (hoặc QR/ảnh không hợp lệ khi bật) = `Needs Review` |
| BR-11 | PTO một cấp | Tuyến chính/dự phòng theo Company Profile; không tự duyệt; không tự chuyển người nếu thiếu dự phòng hợp lệ |
| BR-12 | OT theo lịch và thực tế | Đăng ký trên lịch công bố; trả theo giờ thực tế xác nhận; giờ vượt/OT ngoài lịch phải xác minh, không tự xóa cũng không tự trả |
| BR-13 | OT chồng lặp | Không trả trùng một phút; chia đoạn theo loại ngày, giờ đêm, thay đổi lương/lịch |
| BR-14 | Phụ cấp và khấu trừ | Mỗi khoản một lần, có hiệu lực/căn cứ; không phạt đi trễ hoặc cắt lương kỷ luật |
| BR-15 | Sales đủ điều kiện | Chỉ doanh thu dịch vụ đã nghiệm thu và thực nhận sau chiết khấu, loại VAT/chi hộ/hoàn tiền |
| BR-16 | Không trùng hoa hồng | Một khoản thu chỉ một Sales owner sau xác nhận |
| BR-17 | Tham số hiệu lực | Có phiên bản, nguồn, người xác nhận, khoảng hiệu lực; không tự fallback |
| BR-18 | Làm tròn | `ROUND_VND` HALF_UP tại từng dòng cuối; tổng từ các dòng đã làm tròn; BigDecimal |
| BR-19 | Hồ sơ nghỉ việc | Vô hiệu hóa tài khoản theo ngày hiệu lực; giữ hồ sơ/lịch sử; khoản còn phải trả vẫn quyết toán |
| BR-20 | Audit append-only | Không sửa/xóa `audit_log`/`payroll_audit` qua ứng dụng; chặn ở tầng lưu trữ |
| BR-21 | Xung đột lợi ích | Dùng người dự phòng chỉ định trước; kỳ có lương Director bị chặn `APPROVE` (`409`) tới khi có quyết định xử lý được HNQ xác nhận (OPEN-05); không thêm trạng thái, không tự lập tuyến thay thế |
| BR-22 | Bảo toàn lịch sử | Thay đổi lương, lịch, chức danh, phòng ban, quyền, tham số lưu lịch sử/hiệu lực |
| BR-23 | Chốt công | Nguồn kỳ được chốt có người/thời điểm/phiên bản; mở chốt chỉ khi `CALCULATED → DRAFT`, có lý do; không mở sau `APPROVED` |
| BR-24 | Net và Payable | Phân biệt Net_entitlement và Payable; tạm ứng/hoàn chi có chứng từ, không tính hai lần |
| BR-25 | Phân loại adjustment | `GROSS`/`TAX`/`INSURANCE`/`NET`; Gross adjustment qua thuế/BH; không vừa cộng Gross vừa Net cùng nguồn |
| BR-26 | Quỹ phép | Chỉ trừ khi đơn được duyệt; không số dư âm; nghỉ ngày không làm việc không trừ; hủy đơn hoàn quỹ |
| BR-27 | Hồ sơ thuế/BH | Tham gia từng quỹ xác định riêng; không suy từ `employee_type`; thiếu hồ sơ = `NEEDS_REVIEW` |
| BR-28 | PAID | `PAID` là trạng thái chi trả sau `LOCKED`, không mở quyền sửa lương |
| BR-29 | Phụ cấp phân bổ | `A × H_eligible / H_std` theo giờ theo lịch; trễ/sớm/phép có lương không giảm phụ cấp; nghỉ không lương có giảm |
| BR-30 | Quy ước mã lỗi | `401` chưa xác thực; `403` thiếu quyền chức năng; `404` bản ghi ngoài phạm vi; `409` trạng thái; `422` validation (DEC-01) |

## 7.1. Công thức tổng quát payroll

```text
Gross = Salary_attendance + OT_pay + Allowances
      + Eligible_Sales_Commission + Approved_Bonuses + Other_Eligible_Additions

Net_entitlement = Gross − Employee_Insurance − Personal_Income_Tax − Legal_Deductions
                + Net_adjustment_credit − Net_adjustment_debit

Payable = Net_entitlement − Advance_recovery + Reimbursements
```

Đây là biểu thức giải thích cấu phần; cách phân loại, căn cứ BH/thuế, làm tròn và adjustment theo `payroll_rules.md` phiên bản hiệu lực. Không cộng phụ cấp lại vào Net.

## 7.2. Quy trình payroll theo lịch HNQ

| Thời điểm | Công việc | Trách nhiệm |
|---|---|---|
| Ngày cuối tháng | Kết thúc kỳ chấm công, tổng hợp ngày/giờ công | Employee, Manager, HR |
| Ngày 01–02 tháng sau | Xử lý PTO/OT, xác minh công thiếu, đối soát Sales và phụ cấp; chốt công | Manager, Sales Manager, Finance Manager, HR |
| Ngày 03 | Tính bảng lương dự kiến, rà soát Gross, BH, thuế, khấu trừ | HR |
| Ngày 04 | Đối soát tài chính, phê duyệt, khóa kỳ | Finance Manager đối soát; Director `APPROVE`; Finance Manager `LOCK` |
| Ngày 05 | Chi trả, phát hành phiếu lương PDF, ghi nhận `PAID` | Finance, HR, HRFlow |

Nếu ngày 05 trùng ngày nghỉ, thanh toán vào ngày làm việc liền trước và **dời các mốc chuẩn bị (chốt công, tính, duyệt, khóa) sớm tương ứng** để hoàn thành trước ngày chi. Phiếu lương phát hành theo lịch chi, thời điểm phát hành lưu riêng. Dữ liệu còn chờ xác minh xử lý theo FR-30 (chi phần hợp lệ, điều chỉnh phần chờ); phần mềm không tự giữ toàn bộ lương vì một khoản chờ.

---

# 8. Phạm vi triển khai và ưu tiên

## 8.1. MVP bắt buộc và tiêu chí hoàn thành theo ca kiểm thử

MVP đạt khi hoàn thành các chức năng sau **và** qua các ca kiểm thử nêu bên dưới.

**Chức năng MVP:** FR-01–FR-08, FR-10, FR-11 (khoản bắt buộc: phụ cấp, khấu trừ có căn cứ), FR-13, FR-14, FR-16 (tính cho cư trú lũy tiến; các nhánh khác `NEEDS_REVIEW`), FR-17, FR-18, FR-22, FR-23, FR-24, FR-26–FR-33. (FR-07 trong MVP = chấm công cơ bản; QR động + ảnh minh chứng là mở rộng.)

**Ca kiểm thử nghiệp vụ bắt buộc cho MVP (không áp dụng các ca được ghi rõ là mở rộng):**

| Nhóm | Ca bắt buộc |
|---|---|
| Ví dụ payroll | P01, P02, P03, P04, P07, P08 |
| Ca biên | B01, B02, B03, B04, B05, B06, B13, B14, B15, B16, B17, B18, B19, B20, B22, B24 |
| Phân quyền | PM-01 đến PM-12, PM-15 đến PM-20 |
| An toàn | NFR-01, NFR-02, NFR-04, NFR-09 (mục 1–6, 8) |
| Chấm công cơ bản | Check-in/out theo phiên; thiếu dữ liệu = `Needs Review`; không dung sai 15 phút; không phạt tiền |

**Ngoài MVP (Mở rộng, có test riêng khi làm):** FR-09, FR-12, FR-15, FR-19–FR-21, FR-25; P05 (Sales), P06 (OT + miễn thuế OT); B07–B12 (OT, Sales); B21 (chi qua năm, không cư trú, short-term); B23 (500 nhân viên, PERF-02b); PM-13, PM-14.

**Quy tắc dữ liệu OT/Sales trong MVP:** không xây dựng quy trình đăng ký/duyệt OT hoặc tự động tính KPI/hoa hồng. Nếu demo không có phát sinh, HR phải xác nhận rõ “không phát sinh”; nếu có dữ liệu mô phỏng, chỉ nhận dòng đã được xác nhận, có nguồn và kỳ hiệu lực. Thiếu hoặc chưa xác minh dữ liệu không được tự đổi thành 0; phải đánh dấu `NEEDS_REVIEW` và không đưa khoản chưa xác minh vào kết quả.

**Lưu ý:** các kiểm soát bảo mật và bất biến payroll (không tự duyệt, kỳ khóa chỉ đọc, `APPROVE`≠`LOCK`, audit append-only, idempotent/rollback) không phải tùy chọn dù một số chức năng khác để mở rộng.

## 8.2. Chức năng mở rộng ưu tiên
1. OT theo lịch công bố: FR-09, FR-15.
2. Phụ cấp/thưởng/KPI/hoa hồng Sales tự động hóa: FR-11 (nâng cao), FR-12.
3. ESS mở rộng, thông báo/email, dashboard, báo cáo.
4. Cảnh báo hợp đồng và tác vụ định kỳ.
5. Báo cáo audit nâng cao; PERF-02b.

**Thứ tự làm đề xuất:** OT và Sales cùng làm trước vì P05/P06 phụ thuộc cả hai.

> **Ghi chú bản 3.3:** QR động và ảnh minh chứng trở lại nhóm mở rộng (theo `company_profile.md` Mục 14.2); MVP của FR-07 là chấm công cơ bản. Hệ mã FR liền mạch FR-01…FR-33 của bản 3.2 giữ nguyên.

## 8.3. Ngoài phạm vi
- CRM đầy đủ, pipeline bán hàng, chăm sóc khách hàng.
- Quản lý tiến độ dự án marketing, lịch đăng nội dung, thiết kế, chiến dịch quảng cáo.
- Kế toán tổng hợp, hóa đơn khách hàng, thanh toán ngân hàng tự động.
- Tích hợp trực tiếp mọi nhà cung cấp thuế/bảo hiểm hoặc cơ quan nhà nước; tờ khai thuế/BH.
- Nhận diện khuôn mặt/sinh trắc học.
- SLA thương mại khi chỉ chạy demo.

---

# 9. Luồng nghiệp vụ tổng thể

## 9.1. Hồ sơ nhân sự
1. HR tạo hồ sơ, hợp đồng/lịch/lương hiệu lực, hồ sơ thuế/BH.
2. Hệ thống kiểm tra và lưu `Onboarding`.
3. HR yêu cầu; Admin khởi tạo tài khoản và gán role (FR-31).
4. Đủ điều kiện → `Active`.
5. Thay đổi hiệu lực lưu lịch sử.
6. Nghỉ việc: HR ghi ngày hiệu lực; tài khoản vô hiệu hóa đúng thời điểm; dữ liệu giữ; khoản còn phải trả quyết toán.

## 9.2. Chấm công và PTO
1. Nhân viên chấm công (check-in/out) theo lịch cá nhân (FR-07); khi bật chế độ mở rộng thì kèm QR động + ảnh minh chứng.
2. Hệ thống lưu thời điểm server (và kết quả xác thực QR, tham chiếu ảnh khi bật), tính thời lượng, đánh `Needs Review` khi thiếu/sai lệch.
3. Nhân viên tạo PTO; hệ thống kiểm tra số dư, xác định người duyệt theo tuyến.
4. Người duyệt đúng tuyến duyệt/từ chối; từ chối cần lý do; approval log.
5. Duyệt → trừ quỹ phép; hủy/điều chỉnh đơn đã duyệt có tuyến riêng.
6. Manager xác nhận, HR rà soát và chốt công.

## 9.3. OT
1. HR/Manager công bố lịch; nhân viên đăng ký.
2. Duyệt đúng tuyến, ghi quyết định.
3. Sau OT, Manager xác nhận giờ thực tế; HR đối chiếu.
4. OT ngoài lịch/vượt giờ duyệt: xác minh riêng.
5. Chỉ giờ thực tế đủ căn cứ vào payroll; vi phạm giới hạn cảnh báo, không xóa công.

## 9.4. Payroll
1. HR mở kỳ `DRAFT`.
2. Hệ thống kiểm tra dữ liệu, tham số, hồ sơ; thiếu sót vào danh sách `Needs Review`.
3. HR chốt công, tính, chuyển `CALCULATED`, rà soát (có thể quay `DRAFT` có lý do).
4. Finance Manager đối soát.
5. Director dùng `APPROVE` (kỳ có lương Director bị chặn duyệt tới khi có quyết định xử lý xung đột được HNQ xác nhận – DEC-02).
6. Finance Manager dùng `LOCK`.
7. HRFlow tạo PDF theo snapshot; phát hành theo lịch chi.
8. Finance ghi nhận chi trả → `PAID`.
9. Nhân viên xem/tải phiếu của mình; xuất dữ liệu nhạy cảm có audit.
10. Sai sót sau khóa: adjustment ở kỳ mở tiếp theo.

## 9.5. Xử lý quyền truy cập
1. Request kèm token/phiên.
2. Backend xác thực danh tính và trạng thái tài khoản.
3. Lấy role/grant/quan hệ quản lý từ dữ liệu đã lưu.
4. Kiểm tra quyền chức năng, phạm vi bản ghi, trạng thái nghiệp vụ.
5. Hợp lệ → xử lý và audit khi cần; không → lỗi thống nhất theo DEC-01, không lộ dữ liệu.

---

# 10. Ma trận truy vết yêu cầu

## 10.1. UR → FR → BR → tiêu chí

| Mã UR | Nhu cầu người dùng | FR liên quan | BR | Tiêu chí/test |
|---|---|---|---|---|
| UR-01 | HR quản lý hồ sơ tập trung | FR-03, FR-04, FR-05, FR-33 | BR-22, BR-27 | AC-FR-03/04/05/33 |
| UR-02 | Theo dõi hợp đồng sắp hết hạn | FR-05, FR-25 | BR-17, BR-22 | AC-FR-05, AC-FR-19 |
| UR-03 | Offboarding và bảo toàn lịch sử | FR-06, FR-31 | BR-19, BR-20 | AC-FR-06, AC-FR-25, B20 |
| UR-04 | Nhân viên gửi PTO | FR-08, FR-28 | BR-11, BR-26 | AC-FR-08/28, PM-04, PM-16, PM-17 |
| UR-05 | Duyệt/từ chối PTO/OT đúng tuyến | FR-08, FR-09, FR-32 | BR-11, BR-21 | AC-FR-08/09/32, PM-03/04/16/17 |
| UR-06 | Ghi nhận công (cơ bản; QR/ảnh là mở rộng), xử lý sai lệch, chốt công | FR-07, FR-10, FR-29, FR-30 | BR-09, BR-10, BR-23 | AC-FR-07/10/29/30, B04, B05 |
| UR-07 | Đăng ký OT theo lịch công bố | FR-09, FR-15 | BR-12, BR-13 | AC-FR-09/15, B07–B10 |
| UR-08 | Tính lương tháng tự động | FR-13, FR-14, FR-17, FR-27, FR-29, FR-30 | BR-05–BR-09, BR-18, BR-23, BR-24 | AC-FR-13/14/17/27, P01–P04, P08, B01–B03 |
| UR-09 | Tính BH, thuế theo tham số | FR-16, FR-23, FR-33 | BR-17, BR-27 | AC-FR-16/23/33, P07, B13–B15, B21 |
| UR-10 | Tính OT, phụ cấp, thưởng | FR-11, FR-15, FR-27 | BR-12–BR-18, BR-29 | AC-FR-11/15/27, B06, P05, P06 |
| UR-11 | Thay đổi giữa kỳ, sai sót sau khóa | FR-05, FR-14, FR-24, FR-30 | BR-06, BR-17, BR-22, BR-25 | AC-FR-05/14/24/30, PM-10, P03, P08 |
| UR-12 | Xem/tải phiếu lương cá nhân | FR-18, FR-19 | BR-02, BR-06 | AC-FR-18/19, PM-02 |
| UR-13 | ESS xem hồ sơ/công/phép/đơn | FR-19, FR-28 | BR-02 | AC-FR-19 |
| UR-14 | Một cấp duyệt PTO/OT | FR-08, FR-09, FR-32 | BR-11, BR-21 | AC-FR-08/09/32, PM-04/16/17 |
| UR-15 | Tra cứu lịch sử phê duyệt | FR-08, FR-09, FR-22 | BR-20, BR-21 | AC-FR-08/09/22 |
| UR-16 | Nhận thông báo | FR-20, FR-25 | BR-19 | AC-FR-17/22 |
| UR-17 | Dashboard nhân sự/quỹ lương | FR-21 | BR-02, BR-03 | AC-FR-18, PM-19 |
| UR-18 | Xuất báo cáo theo quyền | FR-21, FR-22 | BR-02, BR-03, BR-20 | AC-FR-18/19 |
| UR-19 | Kiểm soát quyền truy cập | FR-01, FR-02, FR-32 | BR-01–BR-03, BR-30 | AC-FR-01/02/32, PM-01–PM-08, PM-15, PM-19, PM-20 |
| UR-20 | Truy vết thay đổi dữ liệu nhạy cảm | FR-22 | BR-20, BR-22 | AC-FR-19, PM-11, PM-12 |
| UR-21 | Bảo vệ dữ liệu cá nhân/lương | FR-01, FR-02, FR-18, FR-22, FR-33 | BR-02, BR-03, BR-20 | NFR-01/02/04, PM-02, PM-05, PM-19 |
| UR-22 | API nhất quán cho FE/BE | FR-26 | BR-30 | AC-FR-20; Mục 4.2; kiểm tra OpenAPI/contract test |
| UR-23 | Tác vụ nền không chặn request | FR-18, FR-20, FR-25 | — | PERF-05, NFR-09 (mục 5), Mục 4.4 |
| UR-24 | Kiểm thử đủ cho chức năng rủi ro cao | Tất cả FR trọng yếu | Tất cả BR liên quan | NFR-06; P/B/PM (Mục 8.1) |
| UR-25 | Khả dụng và khôi phục | — (phi chức năng) | — | NFR-05; bài diễn tập restore |
| UR-26 | Truy vết đóng góp, kiểm soát thay đổi | — (quy trình phát triển) | — | NFR-06/08; PR, CI, README |
| UR-27 | Quản lý quỹ phép | FR-28, FR-08 | BR-26 | AC-FR-22 |
| UR-28 | Lịch làm việc, lịch lễ, giờ chuẩn | FR-29, FR-05 | BR-09 | AC-FR-23, B01, B02 |
| UR-29 | Chốt công và snapshot nguồn | FR-30 | BR-23 | AC-FR-24, B04, B19 |
| UR-30 | Quản trị tài khoản, grant, tuyến duyệt | FR-31, FR-32 | BR-01, BR-03, BR-21 | AC-FR-22/32, PM-18, PM-20 |
| UR-31 | Hồ sơ thuế/bảo hiểm của nhân viên | FR-33, FR-16 | BR-27 | AC-FR-27, B14, B15 |
| UR-32 | Ghi nhận chi trả và phát hành phiếu | FR-17, FR-18 | BR-28 | AC-FR-16/18 |

## 10.2. FR → tài liệu tiếp theo

| Nhóm FR | Use Case cần đặc tả | Thiết kế dữ liệu | Kiểm thử bắt buộc |
|---|---|---|---|
| FR-01, 02, 34, 35 | Đăng nhập; quản trị tài khoản; cấp/thu hồi grant; cấu hình tuyến duyệt | Account, role, grant, approver_route, audit | 401/403/404, khóa tạm, grant hết hạn/thu hồi |
| FR-03–06, 36 | Hồ sơ; cơ cấu; hợp đồng; onboarding/offboarding; hồ sơ thuế/BH | Employee, department, title, reporting_line, contract, tax/insurance profile, history | Mã trùng, hiệu lực, tài khoản nghỉ việc, thiếu hồ sơ thuế/BH |
| FR-07, 11, 32, 33 | Chấm công cơ bản (QR/ảnh mở rộng); xác minh; lịch/lễ; chốt công | Attendance, schedule, holiday, adjustment, work_snapshot, QR token, proof image ref | Thiếu công, trùng giờ, sai người, QR hết hạn/không hợp lệ, ảnh ngoài quyền, `H_std`, mở chốt |
| FR-08, 31 | PTO; hủy/điều chỉnh; quỹ phép | PTO request, leave_balance, approval_log | Tuyến, tự duyệt, từ chối thiếu lý do, thiếu dự phòng, số dư |
| FR-09, 16 | Lịch OT; đăng ký/duyệt; xác nhận; OT ngoài lịch; tính OT | OT schedule, registration, approval, actual segment, verification | Thực tế ≠ đăng ký, qua nửa đêm, loại ngày, giới hạn |
| FR-11, 13 | Khoản thu nhập; thưởng dự án; đối soát Sales | Allowance/bonus/deduction/advance/reimbursement, sales_reconciliation | Chưa duyệt, trùng nguồn, chưa thu/nghiệm thu, hoàn tiền |
| FR-13, 15, 17, 20, 26, 27, 30 | Tạo kỳ; tính; rà soát; đối soát; duyệt; khóa; chi trả; adjustment | Payroll period, payroll line, snapshot, parameter_version, grant, adjustment, payment | Công thức P/B, thiếu nguồn, self-approval, APPROVE≠LOCK, khóa kỳ, chặn duyệt do xung đột lợi ích, idempotent |
| FR-18, 22 | Tạo/xem phiếu; ESS | Payslip metadata, employee scope | Không xem phiếu người khác, PDF khớp snapshot |
| FR-20, 24 | Thông báo; dashboard; xuất báo cáo | Notification, report scope, export audit | Đúng người nhận, không lộ qua export |
| FR-22, 28, 29 | Audit; tác vụ định kỳ; xử lý lỗi | Audit log, job log, error contract | Append-only, chạy lặp, response lỗi nhất quán |

## 10.3. Ca kiểm thử phân quyền PM → FR

| Mã | Tình huống (rút gọn) | FR chịu trách nhiệm | AC |
|---|---|---|---|
| PM-01 | API không token | FR-01, FR-26 | AC-FR-01 |
| PM-02 | Employee truy cập bản ghi/dữ liệu lương của người khác | FR-02, FR-18, FR-19 | Thiếu quyền endpoint: `403`; bản ghi ngoài phạm vi: `404`; không trả dữ liệu |
| PM-03 | Manager ngoài team/ngoài tuyến | FR-02, FR-08, FR-09 | AC-FR-02, AC-FR-08 |
| PM-04 | Tự approve/reject PTO/OT | FR-08, FR-09 | AC-FR-08, AC-FR-08 |
| PM-05 | Manager/Admin gọi payroll không grant | FR-02, FR-13, FR-17 | AC-FR-02, AC-FR-15 |
| PM-06 | HR approve/lock chỉ bằng role | FR-17, FR-32 | AC-FR-15, AC-FR-26 |
| PM-07 | APPROVE nhưng khóa kỳ | FR-17 | AC-FR-15 |
| PM-08 | Người lập tự duyệt | FR-17, FR-22 | AC-FR-15, AC-FR-19 |
| PM-09 | Director duyệt kỳ có lương mình | FR-17 | AC-FR-15 (DEC-02) |
| PM-10 | Sửa kỳ/phiếu `LOCKED` | FR-17, FR-18, FR-24 | AC-FR-15, AC-FR-18 |
| PM-11 | HR sửa hồ sơ đúng quyền | FR-03, FR-22 | AC-FR-03, AC-FR-19 |
| PM-12 | Update/delete audit log | FR-22 | AC-FR-19 |
| PM-13 | Sales chưa nghiệm thu/thu tiền | FR-12 | AC-FR-10 |
| PM-14 | Hai Sales cùng khoản thu | FR-12 | AC-FR-10 |
| PM-15 | Gửi `employee_id` người khác | FR-02, FR-07, FR-08, FR-09 | AC-FR-02, AC-FR-07 |
| PM-16 | Người duyệt chính vắng | FR-08, FR-09, FR-32 | AC-FR-08, AC-FR-26 |
| PM-17 | Cả chính và dự phòng không xử lý | FR-08, FR-09, FR-32 | AC-FR-08, AC-FR-26 |
| PM-18 | Tài khoản terminated đăng nhập | FR-01, FR-06, FR-31 | AC-FR-01, AC-FR-06 |
| PM-19 | Admin xem báo cáo lương | FR-21, FR-31 | AC-FR-18, AC-FR-25 |
| PM-20 | Grant hết hạn/thu hồi | FR-02, FR-32 | AC-FR-02, AC-FR-26 |

---

# 11. Quyết định thiết kế, giả định và vấn đề cần xác nhận

## 11.1. Nhật ký quyết định (DEC)

Các quyết định dưới đây là quyết định đã chốt cho baseline SRS 3.1. Mọi thay đổi sau khi đóng băng phải được ghi nhận như change request và đánh giá tác động tới FR/BR, Use Case, ERD, OpenAPI và test.

| Mã | Quyết định | Ảnh hưởng | Trạng thái |
|---|---|---|---|
| DEC-01 | Quy ước lỗi: `401` chưa xác thực/tài khoản không được phép đăng nhập; `403` thiếu quyền chức năng/endpoint hoặc grant; `404` bản ghi ngoài phạm vi own/team để không lộ sự tồn tại; `409` xung đột trạng thái nghiệp vụ; `422` dữ liệu không qua validation | FR-01, FR-02, FR-26, OpenAPI, test | ĐÃ CHỐT |
| DEC-02 | Kỳ có dòng lương Director giữ trạng thái `CALCULATED`; `APPROVE` bị chặn (`409 CONFLICT_OF_INTEREST`) tới khi có bản ghi quyết định xử lý xung đột được cấp có thẩm quyền HNQ xác nhận (căn cứ, người xác nhận, ngày) và có audit. Nội dung biện pháp là `OPEN-05` của `payroll_rules.md`, SRS không tự xác lập; hệ thống không thêm trạng thái kỳ, không tự chọn người duyệt thay, không tự tạo grant, không duyệt từng phiếu | FR-17, BR-21, PM-09 | ĐÃ CHỐT (sửa ở bản 3.3) |
| DEC-03 | Grant payroll seed theo hồ sơ công ty: HR `PREPARE`; Director `APPROVE`; Finance Manager `LOCK`. Chủ sở hữu/đại diện chủ sở hữu HNQ hoặc người được ủy quyền bằng văn bản quyết định thay đổi; Admin chỉ thực thi, lưu tham chiếu quyết định và audit; không ai tự cấp/gia hạn quyền cho mình | FR-32, BR-03 | ĐÃ CHỐT |
| DEC-04 | HR xác nhận hồ sơ và gửi yêu cầu; Admin khởi tạo/quản trị tài khoản, không tự quyết định hồ sơ nhân sự hoặc tự cấp grant payroll | FR-06, FR-31 | ĐÃ CHỐT |
| DEC-05 | Adjustment do Director duyệt hoặc người được cấp có thẩm quyền HNQ ủy quyền bằng văn bản; người tạo adjustment không được tự duyệt; mọi ủy quyền phải có phạm vi, hiệu lực và audit | FR-24, FR-32 | ĐÃ CHỐT |
| DEC-06 | Hệ mã FR (bản 3.0): retire mã cũ; bổ sung FR quỹ phép/lịch/chốt công/tài khoản/grant/hồ sơ thuế–BH; bản 3.2 đánh lại liền mạch thành FR-01…FR-33 | Mục 1.4.1, 12.4 | ĐÃ CHỐT |
| DEC-07 | MVP theo đúng danh sách chức năng và ca kiểm thử tại Mục 8.1; P05/P06, B07–B12, PM-13/PM-14 là mở rộng, không chặn nghiệm thu MVP | Mục 8 | ĐÃ CHỐT |
| DEC-08 | Nếu database/API cần trường `net_salary`, trường này mang nghĩa `Payable`; `Net_entitlement`, `Advance_recovery` và `Reimbursements` phải lưu/tính riêng để đối soát, tránh thu hồi tạm ứng hoặc cộng hoàn chi hai lần | FR-27 | ĐÃ CHỐT |
| DEC-09 | Chấm công cơ bản và chế độ QR/ảnh cùng thuộc FR-07; MVP chỉ yêu cầu chấm công cơ bản, QR/ảnh là mở rộng (sửa ở bản 3.3 theo `company_profile.md` Mục 14); đánh lại mã FR liền mạch FR-01…FR-33 (PTO = FR-08 trở đi); nếu demo QR/ảnh chưa có camera/lưu trữ thật thì mô phỏng rõ | FR-07…FR-33, Mục 8, SIM-03, UR-06, ma trận truy vết | ĐÃ CHỐT |

## 11.2. Giả định và phụ thuộc
1. HNQ Media là doanh nghiệp mô phỏng; các bộ thuế/BH/lương tối thiểu và giới hạn OT dùng trong demo là **tham số mô phỏng**, có nguồn, ngày hiệu lực, phiên bản và trạng thái xác minh. Không trình bày kết quả này như tư vấn pháp lý hoặc bảng lương tuân thủ pháp luật hiện hành nếu chưa được đối chiếu văn bản gốc.
2. Địa bàn làm việc và hồ sơ thuế/BH của 40 nhân sự mẫu phải được seed/xác nhận trước khi chạy demo. Hồ sơ thiếu được gắn `NEEDS_REVIEW`, không tự suy theo chức danh hoặc loại nhân viên.
3. MVP dùng chấm công cơ bản (FR-07); QR động + ảnh minh chứng là mở rộng. Khi demo chế độ QR/ảnh mà môi trường chưa có camera/lưu trữ ảnh thật thì phải mô phỏng rõ (in-app/giả lập) và ghi rõ trong tài liệu demo; không tuyên bố đã tích hợp phần cứng khi chưa kiểm thử end-to-end. Email thật vẫn là tích hợp tùy chọn.
4. MVP chỉ cần dữ liệu OT/Sales đã xác nhận hoặc xác nhận không phát sinh; luồng đăng ký/duyệt OT và tự động hóa KPI/hoa hồng là mở rộng.

## 11.3. Giả định mô phỏng và hạng mục ngoài MVP

Các mục dưới đây **không phải quyết định chặn đóng băng SRS**. Đây là giới hạn đã thống nhất của đồ án hoặc công việc cấu hình/kiểm chứng trước demo. Nếu cần đổi phạm vi sau khi bắt đầu thiết kế, tạo change request và đánh giá tác động thay vì sửa ngầm baseline.

| Mã | Hạng mục | Quyết định baseline | Việc cần làm trước demo |
|---|---|---|---|
| SIM-01 | Tham số thuế/BH/lương tối thiểu/OT chưa được xác minh pháp lý đầy đủ | Dùng bộ tham số mô phỏng có nguồn, hiệu lực, phiên bản, người xác nhận và cờ xác minh; chưa xác minh thì không tuyên bố tuân thủ pháp luật | Seed và kiểm thử bộ tham số; ghi rõ đây là dữ liệu mô phỏng |
| SIM-02 | Địa bàn làm việc ảnh hưởng tham số vùng | Lưu địa bàn riêng trong hồ sơ; không suy từ tên công ty/thành phố | Seed địa bàn cho từng nhân viên mẫu |
| SIM-03 | Camera/lưu trữ ảnh thật và email thật | Chế độ QR + ảnh là mở rộng (FR-07); phần cứng camera/lưu trữ thật và email SMTP có thể mô phỏng nếu chưa tích hợp end-to-end | Ghi rõ trong demo/báo cáo phần nào là mô phỏng; không tuyên bố đã tích hợp phần cứng khi chưa kiểm thử |
| EXT-01 | Tự động hóa OT và Sales/KPI/hoa hồng | Ngoài MVP; MVP nhận dữ liệu đã xác nhận hoặc xác nhận không phát sinh | Kiểm thử dữ liệu đầu vào, nguồn, kỳ hiệu lực và `NEEDS_REVIEW` |
| SIM-04 | Phân loại thuế/BH cho từng loại phụ cấp/thưởng/hoàn chi và căn cứ tham gia từng quỹ | Mỗi khoản có mã phân loại; chưa phân loại hoặc hồ sơ thiếu thì `NEEDS_REVIEW`; không suy theo `employee_type` | Seed dữ liệu đã phân loại cho bộ demo |
| QA-01 | Đo performance/availability trong môi trường demo | Dùng ngưỡng NFR làm tiêu chí kiểm thử, ghi môi trường và kết quả thực đo | Chạy test và lưu bằng chứng |

---

# 12. Phụ lục

## 12.1. Checklist xác minh baseline khi bắt đầu thiết kế

- [ ] Thông tin công ty, quy mô, phòng ban thống nhất với `company_profile.md`.
- [ ] Chỉ có 4 role; chức danh lưu riêng, không là role thứ năm.
- [ ] Tuyến PTO/OT chính và dự phòng khớp bảng Company Profile và permission_matrix.
- [ ] `VIEW`/`PREPARE`/`APPROVE`/`LOCK` độc lập; không cấp theo role.
- [ ] Director `APPROVE`; Finance Manager `LOCK` sau phê duyệt; đối soát ≠ approve/lock.
- [ ] Người lập không tự duyệt; kỳ có lương Director bị chặn `APPROVE` tới khi có quyết định xử lý xung đột được HNQ xác nhận.
- [ ] Công thức dùng lịch cá nhân và `H_std` toàn tháng, không mặc định 22/26.
- [ ] Gross/Net/Payable không cộng trùng; tạm ứng/hoàn chi có chứng từ.
- [ ] OT theo giờ thực tế đủ căn cứ; OT ngoài lịch/vượt giờ có bản ghi xác minh.
- [ ] Sales chỉ doanh thu đã nghiệm thu và thực nhận, loại VAT/chi hộ/hoàn tiền.
- [ ] Kỳ lương `DRAFT → CALCULATED → APPROVED → LOCKED`, `PAID` tách riêng; kỳ khóa không sửa; adjustment phân loại GROSS/TAX/INSURANCE/NET.
- [ ] Chốt công và snapshot có người/thời điểm/phiên bản.
- [ ] Quỹ phép, lịch/lễ, hồ sơ thuế/BH có FR riêng.
- [ ] Audit/approval log append-only và có test.
- [ ] Tham số pháp lý chưa xác minh được gắn trạng thái, không hard-code.
- [ ] Mỗi FR có AC; ma trận truy vết đủ UR, PM; không mã FR trùng hoặc dùng lại mã retire.
- [x] DEC-01 đến DEC-08 đã được chốt trong baseline SRS 3.1; DEC-09 (gộp chấm công + đánh lại mã FR liền mạch) đã chốt ở bản 3.2; DEC-02 và DEC-09 được sửa ở bản 3.3 để khớp baseline nghiệp vụ.
- [x] Phạm vi MVP và bộ P/B/PM bắt buộc được xác định tại Mục 8.1; các ca ngoài MVP không chặn nghiệm thu MVP.
- [x] Chấm công cơ bản là MVP (FR-07); QR động + ảnh minh chứng là mở rộng; hệ mã FR liền mạch FR-01…FR-33.

## 12.2. Ca kiểm thử phân quyền bắt buộc (từ permission_matrix.md)

| Mã | Tình huống | Kết quả mong đợi |
|---|---|---|
| PM-01 | API không có token | `401` |
| PM-02 | Employee gọi chức năng không được cấp quyền | `403`; nếu gọi được endpoint nhưng bản ghi ngoài phạm vi own/team thì `404`; không trả dữ liệu |
| PM-03 | Manager xem ngoài team hoặc duyệt ngoài tuyến | `403`/`404` |
| PM-04 | Người gửi tự approve/reject PTO/OT | `403`, trạng thái đơn không đổi |
| PM-05 | Manager/Admin gọi API payroll không có grant | `403` |
| PM-06 | HR approve/lock chỉ với role HR, không grant | `403` |
| PM-07 | Có `APPROVE` nhưng không có `LOCK` cố khóa kỳ | `403` |
| PM-08 | Người lập bảng lương tự duyệt | `403`, ghi audit/security event |
| PM-09 | Director phê duyệt kỳ có lương mình khi chưa có biện pháp xác nhận | Chặn `APPROVE` (`409`), kỳ giữ `CALCULATED`, yêu cầu quyết định hợp lệ; không tự chuyển duyệt từng phiếu |
| PM-10 | Sửa kỳ/phiếu `LOCKED` | `409`, dữ liệu không đổi; yêu cầu adjustment |
| PM-11 | HR sửa hồ sơ đúng quyền | Thành công và có audit |
| PM-12 | Update/delete audit log | Bị chặn |
| PM-13 | Sales ghi doanh thu chưa nghiệm thu/thực nhận | Không vào KPI/hoa hồng |
| PM-14 | Hai Sales nhận cùng khoản thu | Chỉ một owner sau xác nhận |
| PM-15 | Gửi `employee_id` người khác khi tạo PTO/OT | Server bỏ qua giá trị client hoặc `422` |
| PM-16 | Người duyệt chính vắng mặt | Chỉ người dự phòng chỉ định trước được duyệt; lưu căn cứ |
| PM-17 | Cả chính và dự phòng không xử lý | Đơn giữ chờ; không tự chọn người khác |
| PM-18 | Tài khoản terminated đăng nhập | `401`/`403`; lịch sử còn |
| PM-19 | Admin mở dashboard/report lương | Không có dữ liệu lương nếu chưa có `VIEW` |
| PM-20 | Grant hết hiệu lực/thu hồi | Từ chối theo thời điểm hiệu lực |

## 12.3. Quy định cập nhật tài liệu

1. Ghi lý do và người đề xuất trong decision log/backlog.
2. Xác định tác động tới `company_profile.md`, `payroll_rules.md`, `permission_matrix.md`.
3. Nếu đổi chính sách nghiệp vụ, cập nhật baseline trước hoặc đồng thời SRS.
4. Cập nhật UR/FR/NFR/BR và traceability.
5. Cập nhật Use Case, ERD, OpenAPI, test case bị ảnh hưởng.
6. Review trước khi merge; không sửa riêng code bỏ qua tài liệu.

## 12.4. Ánh xạ mã FR theo phiên bản (liền mạch bản 3.2)

Từ bản **3.2**, mã FR được đánh **liền mạch FR-01 … FR-33**. Bảng dưới đây giúp đối chiếu với bản ≤ 3.1.

| Mã bản ≤ 3.1 | Mã bản 3.2 | Nội dung |
|---|---|---|
| FR-01 … FR-06 | FR-01 … FR-06 | Không đổi |
| FR-07 (chấm công cơ bản) + FR-08 (QR/ảnh) | **FR-07** | Gộp thành một mã chấm công: cơ bản là MVP, QR động + ảnh minh chứng là mở rộng (từ bản 3.3) |
| FR-09 PTO | **FR-08** | Quản lý yêu cầu nghỉ phép |
| FR-10 OT | **FR-09** | Lịch OT, đăng ký, xác nhận giờ thực tế |
| FR-11 Xác minh công | **FR-10** | Xác minh và điều chỉnh dữ liệu chấm công |
| FR-12 Phụ cấp/thưởng/khấu trừ | **FR-11** | Phụ cấp, thưởng và khấu trừ |
| FR-13 Sales | **FR-12** | Doanh thu, KPI và hoa hồng Sales |
| FR-14 Kỳ lương | **FR-13** | Tạo và quản lý kỳ lương |
| FR-15 Tính lương theo công | **FR-14** | Tính lương theo công và thay đổi giữa kỳ |
| FR-16 Tính OT | **FR-15** | Tính tiền OT và làm đêm |
| FR-17 BH & thuế | **FR-16** | Tính bảo hiểm và thuế TNCN |
| FR-20 Duyệt/khóa/chi trả | **FR-17** | Rà soát, đối soát, phê duyệt, khóa kỳ và ghi nhận chi trả |
| FR-21 Phiếu lương PDF | **FR-18** | Tạo và xem phiếu lương PDF |
| FR-22 ESS | **FR-19** | Cổng tự phục vụ nhân viên |
| FR-23 Thông báo | **FR-20** | Thông báo |
| FR-24 Dashboard/báo cáo | **FR-21** | Dashboard và báo cáo |
| FR-25 Audit log | **FR-22** | Audit log và approval log |
| FR-26 Tham số | **FR-23** | Tham số nghiệp vụ theo hiệu lực |
| FR-27 Adjustment | **FR-24** | Adjustment sau khóa kỳ |
| FR-28 Cảnh báo HĐ | **FR-25** | Cảnh báo hợp đồng và tác vụ định kỳ |
| FR-29 Xử lý lỗi | **FR-26** | Xử lý lỗi và phản hồi nghiệp vụ |
| FR-30 Gross/Net/Payable | **FR-27** | Tổng hợp Gross, Net, Payable |
| FR-31 Quỹ phép | **FR-28** | Quỹ phép |
| FR-32 Lịch/lễ/H_std | **FR-29** | Lịch làm việc, lịch lễ/Tết và giờ chuẩn |
| FR-33 Chốt công | **FR-30** | Chốt công và snapshot nguồn kỳ lương |
| FR-34 Quản trị tài khoản | **FR-31** | Quản trị tài khoản |
| FR-35 Grant/tuyến duyệt | **FR-32** | Grant, ủy quyền và cấu hình tuyến duyệt |
| FR-36 Hồ sơ thuế/BH | **FR-33** | Hồ sơ thuế và bảo hiểm của nhân viên |

**Ghi chú lịch sử mã đã retire trước bản 3.2:** FR-18/FR-19 của các bản rất cũ từng bị retire; nội dung Gross/Net đã chuyển thành FR-30 (bản 3.0–3.1) rồi **FR-27** (bản 3.2). Không dùng lại khoảng mã ngoài dải FR-01…FR-33 khi chưa có change request.

| Nhóm khác | Ghi chú |
|---|---|
| SRS-OPEN-01..09 (bản 3.0) | → SIM-01..04 / EXT-01 / QA-01 (bản 3.1+) |
| UR-01..UR-32 | Giữ nguyên mã UR |
| BR-01..BR-30 | Giữ nguyên mã BR |
| AC-FR-xx | Theo mã FR tương ứng bản 3.2 |
---

**Kết thúc tài liệu SRS HRFlow phiên bản 3.3 – Baseline đã chốt, đồng bộ với ba tài liệu nghiệp vụ (mã FR liền mạch FR-01…FR-33).**
