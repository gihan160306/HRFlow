# Enterprise Human Resource & Automated Payroll System
**Hệ thống Quản lý Nhân sự Doanh nghiệp & Tính lương Tự động**

---

### Thành viên nhóm:
- **52400074** – Hồ Thị Gia Hân
- **52400266** – Đặng Minh Hiếu
- **52400267** – Trần Thị Lệ Hoa
- **52400292** – Nguyễn Hoàng Phương Ngân
- **52200021** – Hồ Vinh Quan

---

## MỤC LỤC
- [Enterprise Human Resource \& Automated Payroll System](#enterprise-human-resource--automated-payroll-system)
    - [Thành viên nhóm:](#thành-viên-nhóm)
  - [MỤC LỤC](#mục-lục)
  - [1. GIỚI THIỆU](#1-giới-thiệu)
    - [1.1 Mục đích tài liệu](#11-mục-đích-tài-liệu)
    - [1.2 Phạm vi hệ thống](#12-phạm-vi-hệ-thống)
    - [1.3 Đối tượng đọc tài liệu](#13-đối-tượng-đọc-tài-liệu)
    - [1.4 Định nghĩa, từ viết tắt và thuật ngữ](#14-định-nghĩa-từ-viết-tắt-và-thuật-ngữ)
  - [2. MÔ TẢ TỔNG QUAN HỆ THỐNG](#2-mô-tả-tổng-quan-hệ-thống)
    - [2.1 Bối cảnh \& mục tiêu sản phẩm](#21-bối-cảnh--mục-tiêu-sản-phẩm)
    - [2.2 Bốn phân hệ chức năng chính (Core Subsystems)](#22-bốn-phân-hệ-chức-năng-chính-core-subsystems)
    - [2.3 Đối tượng người dùng hệ thống](#23-đối-tượng-người-dùng-hệ-thống)
    - [2.4 Giả định \& ràng buộc chung](#24-giả-định--ràng-buộc-chung)
  - [3. YÊU CẦU NGƯỜI DÙNG (USER REQUIREMENTS)](#3-yêu-cầu-người-dùng-user-requirements)
    - [3.1 Hồ sơ nhân sự \& Onboarding/Offboarding](#31-hồ-sơ-nhân-sự--onboardingoffboarding)
    - [3.2 Chấm công \& Nghỉ phép (Timecard \& PTO)](#32-chấm-công--nghỉ-phép-timecard--pto)
    - [3.3 Động cơ Tính lương (Payroll Engine)](#33-động-cơ-tính-lương-payroll-engine)
    - [3.4 Phiếu lương \& Cổng Tự phục vụ (ESS)](#34-phiếu-lương--cổng-tự-phục-vụ-ess)
    - [3.5 Quy trình Duyệt \& Thông báo (Workflow \& Notification)](#35-quy-trình-duyệt--thông-báo-workflow--notification)
    - [3.6 Bảng điều khiển \& Báo cáo (Dashboard \& Reports)](#36-bảng-điều-khiển--báo-cáo-dashboard--reports)
    - [3.7 Phân quyền \& Bảo mật (RBAC, Audit, Encryption)](#37-phân-quyền--bảo-mật-rbac-audit-encryption)
    - [3.8 Khóa Kỹ thuật, Kiến trúc \& Vận hành (Infrastructure \& DevOps)](#38-khóa-kỹ-thuật-kiến-trúc--vận-hành-infrastructure--devops)
  - [4. YÊU CẦU CHỨC NĂNG (FUNCTIONAL REQUIREMENTS)](#4-yêu-cầu-chức-năng-functional-requirements)
  - [5. QUY TẮC NGHIỆP VỤ CỐT LÕI (BUSINESS RULES)](#5-quy-tắc-nghiệp-vụ-cốt-lõi-business-rules)
  - [6. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)](#6-yêu-cầu-phi-chức-năng-non-functional-requirements)
  - [7. PHÂN TÍCH \& LUẬN CHỨNG CHỈ SỐ AVAILABILITY (TÍNH SẴN SÀNG HỆ THỐNG)](#7-phân-tích--luận-chứng-chỉ-số-availability-tính-sẵn-sàng-hệ-thống)
    - [7.1 Mức cam kết mục tiêu](#71-mức-cam-kết-mục-tiêu)
    - [7.2 Cơ sở lựa chọn \& luận chứng](#72-cơ-sở-lựa-chọn--luận-chứng)
    - [7.3 Mô hình toán học mô phỏng kỹ thuật (MTBF \& MTTR)](#73-mô-hình-toán-học-mô-phỏng-kỹ-thuật-mtbf--mttr)
      - [\* ĐỊNH LƯỢNG CHI TIẾT 150 PHÚT MTTR (QUY TRÌNH XỬ LÝ SỰ CỐ)](#-định-lượng-chi-tiết-150-phút-mttr-quy-trình-xử-lý-sự-cố)
  - [8. KIẾN TRÚC KỸ THUẬT \& MỨC ƯU TIÊN TRIỂN KHAI](#8-kiến-trúc-kỹ-thuật--mức-ưu-tiên-triển-khai)
      - [\* YÊU CẦU GIAO DIỆN NGOÀI (EXTERNAL INTERFACE REQUIREMENTS)](#-yêu-cầu-giao-diện-ngoài-external-interface-requirements)
  - [9. LUỒNG NGHIỆP VỤ TỔNG THỂ (END-TO-END WORKFLOW)](#9-luồng-nghiệp-vụ-tổng-thể-end-to-end-workflow)
  - [10. MA TRẬN TRUY VẾT YÊU CẦU (REQUIREMENTS TRACEABILITY MATRIX)](#10-ma-trận-truy-vết-yêu-cầu-requirements-traceability-matrix)

---

## 1. GIỚI THIỆU

### 1.1 Mục đích tài liệu
Tài liệu này đặc tả đầy đủ yêu cầu phần mềm (Software Requirements Specification – SRS) cho hệ thống **"Enterprise Human Resource & Automated Payroll System"** (Topic 9), phục vụ mục đích thống nhất hiểu biết giữa các bên liên quan — giảng viên/khách hàng giả định, nhóm phát triển (Product Owner, Frontend, Backend, QA/DevOps) — về phạm vi, chức năng, ràng buộc kỹ thuật và tiêu chí nghiệm thu của hệ thống trước khi bước vào giai đoạn thiết kế và triển khai.

Tài liệu tổng hợp và chuẩn hóa từ ba nguồn đầu vào:
1. Đề bài gốc và phân tích 4 phân hệ lõi cùng các hướng mở rộng.
2. Bảng Yêu cầu Chức năng (FR) và Phi chức năng (NFR) chi tiết.
3. Bảng ánh xạ Yêu cầu Người dùng (User Requirements) sang Yêu cầu Hệ thống (System Requirements) ở mức kỹ thuật, đặc tả cụ thể qua các API endpoint.

### 1.2 Phạm vi hệ thống
Hệ thống số hóa và tự động hóa toàn bộ quy trình quản lý nhân sự – tính lương cho doanh nghiệp, thay thế cho việc quản lý thủ công bằng file Excel vốn dễ sai sót, thiếu kiểm soát và khó kiểm toán. Hệ thống thuộc lĩnh vực ERP với các tính năng cốt lõi bắt buộc theo đề bài: quản lý hồ sơ nhân viên (onboarding), chấm công (timecard logging), quy trình phê duyệt nghỉ phép (PTO approval workflow), tính lương – thuế – bảo hiểm (tax/benefit payroll calculation) và xuất phiếu lương PDF (payslip PDF generation).

Dự án được triển khai theo mô hình Agile/Scrum trong 8 tuần, áp dụng thiết kế API theo hướng Contract-First (chuẩn OpenAPI 3.1), quy trình CI/CD, lưu vết đóng góp qua Git, và đạt tối thiểu 60% Unit Test Coverage.

❖ **Phân loại phạm vi triển khai:**  
Để cân bằng giữa tính chuyên nghiệp của một hệ thống doanh nghiệp và khả năng hoàn thành trong quy mô đồ án (nhóm 4–5 sinh viên, 8 tuần), phạm vi yêu cầu được phân chia minh bạch thành 2 cấp độ:

| Cấp độ | Nội dung |
| :--- | :--- |
| **MVP bắt buộc** *(Core Minimum Viable Product)* | Hồ sơ nhân sự · Chấm công cơ bản & Nghỉ phép · Engine tính lương cơ bản · Xuất phiếu lương PDF · Xác thực & phân quyền RBAC. |
| **Chức năng mở rộng ưu tiên** *(Extended Enterprise Features)* | Quản lý tăng ca (OT) · Quản lý hợp đồng & phụ cấp/khấu trừ · Quy trình phê duyệt cơ bản · Cổng tự phục vụ (ESS) · Dashboard/Báo cáo · Audit Trail · Chấm công nâng cao (QR động + ảnh minh chứng). |

### 1.3 Đối tượng đọc tài liệu
- **Giảng viên / khách hàng giả định:** Đánh giá mức độ đầy đủ, tính khả thi và sự chuyên nghiệp của đặc tả.
- **Product Owner / Trưởng nhóm:** Làm cơ sở lập kế hoạch Sprint, phân chia backlog.
- **Kiến trúc sư hệ thống & lập trình viên Backend/Frontend:** Làm căn cứ thiết kế API, cơ sở dữ liệu và giao diện.
- **Kỹ sư QA/DevOps:** Làm cơ sở xây dựng kịch bản kiểm thử, tiêu chí nghiệm thu và pipeline CI/CD.

### 1.4 Định nghĩa, từ viết tắt và thuật ngữ

| Thuật ngữ | Giải thích |
| :--- | :--- |
| **SRS** | Software Requirements Specification – Đặc tả yêu cầu phần mềm |
| **FR** | Functional Requirement – Yêu cầu chức năng |
| **NFR** | Non-Functional Requirement – Yêu cầu phi chức năng |
| **BR** | Business Rule – Quy tắc nghiệp vụ |
| **UR** | User Requirement – Yêu cầu người dùng |
| **SR** | System Requirement – Yêu cầu hệ thống (chi tiết kỹ thuật) |
| **MVP** | Minimum Viable Product – Sản phẩm khả dụng tối thiểu, bắt buộc hoàn thành |
| **ESS** | Employee Self-Service – Cổng tự phục vụ dành cho nhân viên |
| **OT** | Overtime – Tăng ca |
| **PTO** | Paid Time Off – Nghỉ phép có lương |
| **RBAC** | Role-Based Access Control – Kiểm soát truy cập theo vai trò |
| **BHXH / BHYT / BHTN** | Bảo hiểm Xã hội / Bảo hiểm Y tế / Bảo hiểm Thất nghiệp |
| **TNCN** | Thu nhập cá nhân (thuế TNCN – thuế thu nhập cá nhân) |
| **MTBF** | Mean Time Between Failures – Thời gian trung bình giữa hai lần lỗi |
| **MTTR** | Mean Time To Repair – Thời gian trung bình khắc phục sự cố |
| **RPO / RTO** | Recovery Point/Time Objective – Mục tiêu điểm khôi phục / thời gian khôi phục dữ liệu |
| **CI/CD** | Continuous Integration / Continuous Deployment – Tích hợp & triển khai liên tục |
| **API** | Application Programming Interface – Giao diện lập trình ứng dụng |

---

## 2. MÔ TẢ TỔNG QUAN HỆ THỐNG

### 2.1 Bối cảnh & mục tiêu sản phẩm
Hệ thống phục vụ nhu cầu nội bộ của một doanh nghiệp trong việc quản trị vòng đời nhân sự từ tuyển dụng, chấm công, nghỉ phép, tăng ca cho đến tính lương và phát hành phiếu lương trên một nền tảng tập trung, có kiểm soát phân quyền chặt chẽ và có khả năng kiểm toán (audit) đầy đủ. Mục tiêu là loại bỏ hoàn toàn sự phụ thuộc vào quy trình thủ công (Excel, giấy tờ), giảm sai sót trong tính lương vốn ảnh hưởng trực tiếp đến quyền lợi tài chính của người lao động và cung cấp một nền tảng có thể mở rộng cho quy mô doanh nghiệp lớn hơn trong tương lai.

### 2.2 Bốn phân hệ chức năng chính (Core Subsystems)

| # | Phân hệ | Mô tả & trách nhiệm chính |
| :-: | :--- | :--- |
| **1** | **Hồ sơ Nhân sự** *(Employee Records)* | Quản lý thông tin hợp đồng, phòng ban, mức lương cơ bản, thuế, bảo hiểm; hỗ trợ onboarding nhân viên mới. |
| **2** | **Chấm công & Nghỉ phép** *(Timecard & PTO)* | Nhân viên tạo đơn xin nghỉ (PTO), quản lý duyệt/từ chối, ghi nhận ngày công thực tế, đi trễ/về sớm. |
| **3** | **Động cơ Tính lương** *(Payroll Engine)* | Tự động tính Gross dựa trên ngày công, trừ BHXH/BHYT/BHTN, thuế TNCN $\rightarrow$ Net Salary. Áp dụng quy tắc thuế phức tạp. |
| **4** | **Xuất Phiếu lương** *(Payslip Generation)* | Tự động sinh file PDF phiếu lương chi tiết và gửi email thông báo đến nhân viên. |

### 2.3 Đối tượng người dùng hệ thống
Hệ thống phục vụ bốn nhóm vai trò (role) chính, được kiểm soát nghiêm ngặt bằng cơ chế RBAC (Role-Based Access Control):
- **Employee (Nhân viên):** Tự chấm công, gửi đơn xin nghỉ phép/OT, xem hồ sơ, hợp đồng và phiếu lương của chính mình qua cổng ESS.
- **Manager (Quản lý trực tiếp):** Phê duyệt/từ chối đơn PTO, OT của nhân viên thuộc quyền quản lý; xem báo cáo công của team.
- **HR (Nhân sự):** Quản lý toàn bộ hồ sơ nhân viên, hợp đồng, cấu hình lương/thuế/bảo hiểm, thực hiện tính lương và phát hành phiếu lương.
- **Admin (Quản trị hệ thống):** Cấu hình tham số hệ thống (hệ số OT, biểu thuế, bảo hiểm), quản lý tài khoản, phân quyền vai trò và giám sát nhật ký kiểm toán (audit log).

### 2.4 Giả định & ràng buộc chung
- Hệ thống là ứng dụng nội bộ doanh nghiệp (internal HRM & payroll), không phục vụ mô hình truy cập công khai 24/7 như hệ thống ngân hàng hay thương mại điện tử.
- Các tham số pháp luật (thuế TNCN, tỷ lệ bảo hiểm, hệ số OT) có thể thay đổi theo thời gian và phải được cấu hình động, không hard-code trong mã nguồn.
- Nhóm phát triển triển khai theo mô hình Agile/Scrum trong khung thời gian 8 tuần; kiến trúc hạ tầng được lựa chọn phải khả thi trong phạm vi đồ án (xem Mục 7 — phân tích Availability).
- Toàn bộ API phải được đặc tả trước theo phương pháp Contract-First bằng OpenAPI 3.1 trước khi tiến hành lập trình song song Frontend/Backend.

---

## 3. YÊU CẦU NGƯỜI DÙNG (USER REQUIREMENTS)

Yêu cầu người dùng (User Requirement – UR) là các phát biểu ở mức cao, bằng ngôn ngữ tự nhiên, mô tả “hệ thống cần làm gì” từ góc nhìn của khách hàng/HR/quản lý — những người không nhất thiết am hiểu kỹ thuật. Đây là điểm khởi đầu của quá trình đặc tả, làm cơ sở để suy ra (derive) các Yêu cầu Hệ thống (System Requirements) chi tiết hơn ở Mục 4.

Các UR được nhóm theo 8 phân hệ nghiệp vụ, đánh mã từ UR-01 đến UR-26, trình bày song song với các Yêu cầu Hệ thống (SR) tương ứng để đảm bảo tính truy vết (traceability) đầy đủ, không mơ hồ.

### 3.1 Hồ sơ nhân sự & Onboarding/Offboarding

| Mã UR | Yêu cầu người dùng (User Requirement) | Mã SR | Yêu cầu hệ thống (System Requirement) |
| :--- | :--- | :--- | :--- |
| **UR-01** | HR muốn tạo và quản lý hồ sơ nhân viên (hợp đồng, phòng ban, lương, thuế, bảo hiểm) tập trung, thay vì rải rác trên Excel. | **SR-01.1**<br>**SR-01.2**<br>**SR-01.3** | Hệ thống cho phép HR/Admin tạo hồ sơ nhân viên mới, bao gồm các thông tin cơ bản và khởi tạo tài khoản đăng nhập, liên kết tài khoản với `employee_id` tương ứng.<br>Hệ thống phải kiểm tra tính hợp lệ và tính đầy đủ của dữ liệu khi tạo hồ sơ nhân viên và tài khoản đăng nhập.<br>Hệ thống cho phép HR/Admin cập nhật thông tin nhân viên; các thay đổi trên dữ liệu nhạy cảm phải được ghi nhận vào audit log. |
| **UR-02** | HR muốn hệ thống tự động cảnh báo khi hợp đồng nhân viên sắp/đã hết hạn để chủ động gia hạn. | **SR-02.1**<br>**SR-02.2**<br>**SR-02.3** | Chạy scheduled job hàng ngày kiểm tra `contract_end_date`, phân loại: Active / Expiring Soon ($\le 30$ ngày) / Expired.<br>Gửi notification tới HR khi phát hiện hợp đồng ở trạng thái Expiring Soon.<br>Cho phép HR gia hạn hợp đồng qua endpoint `POST /contracts/{id}/renew` mà không cần tạo hồ sơ nhân viên mới. |
| **UR-03** | HR muốn xử lý nhân viên nghỉ việc gọn gàng, đảm bảo chốt đủ nghĩa vụ lương/bảo hiểm. | **SR-03.1**<br>**SR-03.2**<br>**SR-03.3** | Endpoint `POST /employees/{id}/terminate` ghi nhận ngày nghỉ việc, lý do, trạng thái chốt sổ BHXH.<br>Tự động tính lương/trợ cấp thôi việc còn lại (nếu có) tại kỳ lương cuối cùng.<br>Sau khi terminate, tài khoản ESS tự động deactivate, không xóa dữ liệu lịch sử. |

### 3.2 Chấm công & Nghỉ phép (Timecard & PTO)

| Mã UR | Yêu cầu người dùng (User Requirement) | Mã SR | Yêu cầu hệ thống (System Requirement) |
| :--- | :--- | :--- | :--- |
| **UR-04** | Nhân viên muốn tự tạo đơn xin nghỉ phép mà không cần giấy tờ thủ công. | **SR-04.1**<br>**SR-04.2** | `POST /pto-requests` với các trường: ngày bắt đầu, ngày kết thúc, loại phép, lý do.<br>Tự động kiểm tra số ngày phép còn lại trước khi cho phép gửi đơn. |
| **UR-05** | Quản lý muốn duyệt từ chối đơn nghỉ phép và tăng ca nhanh chóng, có lý do rõ ràng. | **SR-05.1**<br>**SR-05.2** | `PATCH /pto-requests/{id}` với action `approve`/`reject`; trường `reason` bắt buộc khi `reject`.<br>Cập nhật trạng thái đơn theo thời gian thực và gửi notification cho người gửi đơn. |
| **UR-06** | HR muốn hệ thống tự động ghi nhận ngày công thực tế, đi trễ/về sớm và hỗ trợ chấm công nâng cao bằng QR động + ảnh xác thực để tránh gian lận. | **SR-06.1**<br>**SR-06.2**<br>**SR-06.3** | Ghi nhận `check_in_time`, `check_out_time` mỗi ngày qua API chấm công.<br>Tự động tính số phút đi trễ/về sớm dựa trên giờ hành chính cấu hình sẵn.<br>Hệ thống tạo mã QR động (reset mỗi 10–15s). Nhân viên quét mã thành công thì ứng dụng di động mới bật camera selfie chụp ảnh minh chứng. Ứng dụng nén ảnh ($\le 200\text{KB}$) gửi về API kèm thời gian quét. Nếu QR hết hạn/không hợp lệ, camera không mở và báo lỗi. |
| **UR-07** | Nhân viên muốn đăng ký làm thêm giờ (OT) theo lịch OT do cấp trên phân công và biết được đơn đăng ký của mình có được xác nhận hay không. | **SR-07.1**<br>**SR-07.2**<br>**SR-07.3** | Hệ thống cho phép HR/Quản lý tạo và công bố lịch OT theo ngày, thời gian/số giờ và công việc cần thực hiện. Nhân viên chỉ được đăng ký OT đối với các lịch OT đã được công bố và phù hợp với đối tượng/điều kiện được phân công.<br>Quản lý trực tiếp phê duyệt/từ chối đăng ký OT của nhân viên đối với lịch OT đã được công bố.<br>Thông báo cho nhân viên trạng thái đơn đăng ký OT (chờ duyệt/đã duyệt/từ chối). |

### 3.3 Động cơ Tính lương (Payroll Engine)

| Mã UR | Yêu cầu người dùng (User Requirement) | Mã SR | Yêu cầu hệ thống (System Requirement) |
| :--- | :--- | :--- | :--- |
| **UR-08** | HR muốn hệ thống tự động tính lương hàng tháng để giảm sai sót thủ công và tiết kiệm thời gian. | **SR-08.1**<br>**SR-08.2**<br>**SR-08.3** | Tính $\text{Gross Salary} = (\text{Lương cơ bản} / \text{ngày công chuẩn}) \times \text{ngày công thực tế} + \text{phụ cấp} + \text{tiền OT}$.<br>Chạy batch job tính lương cho toàn bộ nhân viên (500 nhân viên) trong $\le 5$ phút.<br>Cho phép HR xem trước (preview) bảng lương trước khi chốt (finalize) để kiểm tra sai sót. |
| **UR-09** | HR/Nhân viên muốn số tiền BHXH/BHYT/BHTN/thuế TNCN được trừ đúng theo quy định pháp luật hiện hành. | **SR-09.1**<br>**SR-09.2**<br>**SR-09.3** | Trừ BHXH 8%, BHYT 1.5%, BHTN 1% trên mức lương đóng bảo hiểm, có áp dụng mức trần (20 lần lương cơ sở).<br>Tính thuế TNCN theo biểu lũy tiến từng phần, áp dụng giảm trừ gia cảnh (bản thân + người phụ thuộc khai báo).<br>Lưu cấu hình các mức thuế bảo hiểm dạng tham số có thể cập nhật khi luật thay đổi (không hard-code). |
| **UR-10** | HR muốn tiền tăng ca và phụ cấp được cộng tự động, đúng hệ số luật lao động. | **SR-10.1**<br>**SR-10.2** | Áp dụng hệ số 150% (ngày thường), 200% (cuối tuần), 300% (ngày lễ) khi tính tiền OT.<br>Cho phép cấu hình danh mục phụ cấp (ăn trưa, xăng xe, điện thoại, chức vụ) theo từng nhân viên/phòng ban. |
| **UR-11** | HR muốn xử lý được trường hợp nhân viên vào/nghỉ giữa tháng hoặc cần điều chỉnh lương hồi tố. | **SR-11.1**<br>**SR-11.2** | Tính lương theo tỷ lệ ngày công thực tế (proration) khi ngày vào/nghỉ không trùng đầu/cuối tháng.<br>Cho phép tạo bản ghi điều chỉnh lương (adjustment) tham chiếu kỳ lương trước, không sửa trực tiếp dữ liệu đã chốt. |

### 3.4 Phiếu lương & Cổng Tự phục vụ (ESS)

| Mã UR | Yêu cầu người dùng (User Requirement) | Mã SR | Yêu cầu hệ thống (System Requirement) |
| :--- | :--- | :--- | :--- |
| **UR-12** | Nhân viên muốn xem/tải phiếu lương của mình bất cứ lúc nào mà không cần hỏi HR. | **SR-12.1**<br>**SR-12.2**<br>**SR-12.3** | Tự động sinh file PDF phiếu lương ngay sau khi HR chốt bảng lương.<br>Cung cấp `GET /employees/{id}/payslips`; chỉ chủ sở hữu token JWT tương ứng ID đó mới truy cập được.<br>Gửi email tự động kèm thông báo khi phiếu lương mới được tạo. |
| **UR-13** | Nhân viên muốn tự tra cứu thông tin cá nhân, hợp đồng, bảng công, số ngày phép còn lại mà không cần liên hệ HR trực tiếp. | **SR-13.1**<br>**SR-13.2** | Dashboard ESS hiển thị các mục: hồ sơ, hợp đồng, bảng công, phép còn lại, đơn đã gửi.<br>Mọi dữ liệu hiển thị trên ESS phải được lọc theo `employee_id` của người đăng nhập (không cho truy cập chéo). |

### 3.5 Quy trình Duyệt & Thông báo (Workflow & Notification)

| Mã UR | Yêu cầu người dùng (User Requirement) | Mã SR | Yêu cầu hệ thống (System Requirement) |
| :--- | :--- | :--- | :--- |
| **UR-14** | Doanh nghiệp muốn đơn PTO, OT được duyệt một cấp bởi Quản lý trực tiếp; nếu nhân viên không có Quản lý trực tiếp thì HR thực hiện phê duyệt. | **SR-14.1**<br>**SR-14.2** | Hệ thống hỗ trợ workflow trạng thái `Pending` $\rightarrow$ `Approved`/`Rejected` cho yêu cầu PTO/OT, với một cấp phê duyệt bởi Quản lý trực tiếp; nếu nhân viên không có Quản lý trực tiếp thì HR thực hiện phê duyệt.<br>Chỉ cần 1 cấp duyệt, không bắt buộc đa cấp. |
| **UR-15** | HR/Quản lý muốn tra soát lại lịch sử ai đã gửi, ai đã duyệt, khi nào, vì sao từ chối. | **SR-15.1**<br>**SR-15.2** | Lưu bảng `approval_log` gồm: `request_id`, `approver_id`, `timestamp`, `action`, `reason`.<br>Cho phép truy vấn lịch sử duyệt theo nhân viên hoặc theo khoảng thời gian. |
| **UR-16** | Người dùng muốn được thông báo ngay khi có sự kiện liên quan đến mình (đơn được duyệt, phiếu lương mới, cảnh báo hợp đồng). | **SR-16.1**<br>**SR-16.2** | Đẩy in-app notification qua cơ chế polling với chu kỳ 30–60 giây.<br>Gửi kèm qua kênh Email cơ bản theo cấu hình người dùng. |

### 3.6 Bảng điều khiển & Báo cáo (Dashboard & Reports)

| Mã UR | Yêu cầu người dùng (User Requirement) | Mã SR | Yêu cầu hệ thống (System Requirement) |
| :--- | :--- | :--- | :--- |
| **UR-17** | Ban lãnh đạo/HR muốn nhìn tổng quan tình hình nhân sự và quỹ lương mà không cần cộng tay từ Excel. | **SR-17.1**<br>**SR-17.2** | Hiển thị dashboard với các chỉ số: tổng nhân viên theo phòng ban, số đơn chờ duyệt, tổng quỹ lương tháng, tổng giờ OT, hợp đồng sắp hết hạn.<br>Cập nhật số liệu dashboard theo thời gian thực hoặc tối thiểu 1 lần mỗi ngày. |
| **UR-18** | HR muốn xuất báo cáo theo phòng ban/khoảng thời gian để phục vụ kiểm toán hoặc báo cáo cấp trên. | **SR-18.1**<br>**SR-18.2** | `GET /reports/export?type=payroll&from=...&to=...&department=...` trả về file Excel/PDF.<br>Báo cáo bao gồm các cột: nhân viên, ngày công, OT, phụ cấp, các khoản trừ, Net Salary. |

### 3.7 Phân quyền & Bảo mật (RBAC, Audit, Encryption)

| Mã UR | Yêu cầu người dùng (User Requirement) | Mã SR | Yêu cầu hệ thống (System Requirement) |
| :--- | :--- | :--- | :--- |
| **UR-19** | Doanh nghiệp muốn đảm bảo mỗi vai trò chỉ thấy đúng phạm vi dữ liệu được phép, tránh rò rỉ thông tin lương. | **SR-19.1**<br>**SR-19.2** | Định nghĩa 4 role: Employee, Manager, HR, Admin với ma trận quyền (permission matrix) rõ ràng cho từng API endpoint.<br>Middleware xác thực JWT phải kiểm tra role trước khi cho phép truy cập bất kỳ endpoint nhạy cảm nào (VD: `/payroll/*`). |
| **UR-20** | Doanh nghiệp muốn có thể truy vết lại ai đã sửa dữ liệu lương/nhạy cảm, khi nào, giá trị cũ/mới là gì. | **SR-20.1**<br>**SR-20.2** | Ghi audit log tự động (trigger hoặc middleware) mỗi khi có UPDATE/DELETE trên bảng lương, hợp đồng, thông tin cá nhân.<br>Audit log lưu tối thiểu: `user_id`, `action`, `table`, `old_value`, `new_value`, `timestamp` — không cho phép sửa/xóa log này. |
| **UR-21** | Doanh nghiệp muốn dữ liệu nhạy cảm (lương, CCCD, tài khoản ngân hàng) được bảo vệ ngay cả khi database bị truy cập trái phép. | **SR-21.1**<br>**SR-21.2** | Mã hóa mật khẩu người dùng bằng BCrypt. Bảo vệ dữ liệu truyền tải bằng HTTPS/TLS 1.3.<br>Áp dụng kiểm soát truy cập cấp dòng (Row-Level Access Control): Nhân viên chỉ xem/sửa dữ liệu của chính mình; Quản lý chỉ xem dữ liệu nhân viên thuộc nhóm quản lý. Tránh field-level encryption toàn bộ để không ảnh hưởng performance khi query. |

### 3.8 Khóa Kỹ thuật, Kiến trúc & Vận hành (Infrastructure & DevOps)

| Mã UR | Yêu cầu người dùng (User Requirement) | Mã SR | Yêu cầu hệ thống (System Requirement) |
| :--- | :--- | :--- | :--- |
| **UR-22** | Nhóm phát triển muốn API được thiết kế nhất quán, dễ tích hợp giữa frontend và backend, tránh sai lệch khi làm song song. | **SR-22.1**<br>**SR-22.2** | Toàn bộ API phải được đặc tả trước bằng OpenAPI 3.1 YAML (Contract-First) trước khi code.<br>Mọi thay đổi API contract phải qua Pull Request review, không sửa trực tiếp trên branch chính. |
| **UR-23** | Doanh nghiệp muốn các tác vụ nặng (gửi email, gửi thông báo) không làm chậm hệ thống chính khi nhiều người dùng cùng thao tác. | **SR-23.1** | Xử lý gửi email/notification bất đồng bộ bằng background worker đơn giản (không dùng Redis/Message Queue phức tạp), không block request chính. |
| **UR-24** | Doanh nghiệp muốn hệ thống được kiểm thử kỹ để tránh sai sót khi tính lương thật (ảnh hưởng tài chính nhân viên). | **SR-24.1**<br>**SR-24.2** | Unit test coverage $\ge 60\%$, ưu tiên các module tính thuế/bảo hiểm/OT.<br>Integration test (Playwright) bắt buộc cho luồng: tính lương $\rightarrow$ duyệt đơn $\rightarrow$ phân quyền. |
| **UR-25** | Doanh nghiệp muốn hệ thống luôn sẵn sàng sử dụng, ít gián đoạn, đặc biệt vào thời điểm chốt lương cuối tháng. | **SR-25.1**<br>**SR-25.2** | Đạt Availability tối thiểu 99,5% (downtime $\le 3,6$ giờ/tháng).<br>Có cơ chế backup dữ liệu hàng ngày, với $\text{RPO} \le 24$ giờ, $\text{RTO} \le 4$ giờ. |
| **UR-26** | Doanh nghiệp muốn dễ dàng biết được ai đã đóng góp gì trong quá trình phát triển (phục vụ đánh giá nội bộ nhóm). | **SR-26.1**<br>**SR-26.2** | Hệ thống Git áp dụng branching strategy rõ ràng (feature branch $\rightarrow$ PR $\rightarrow$ merge).<br>CI pipeline (GitHub Actions) tự động chạy test khi có PR; không merge nếu test fail ("No Git Evidence = Zero Score"). |

---

## 4. YÊU CẦU CHỨC NĂNG (FUNCTIONAL REQUIREMENTS)

Bảng dưới đây tổng hợp các yêu cầu chức năng (FR-01 đến FR-17) của hệ thống được suy ra và chi tiết hóa từ các Yêu cầu Người dùng ở Mục 3. Mỗi FR được gắn Mức độ ưu tiên triển khai (MVP / Mở rộng) tương ứng với phân loại phạm vi tại Mục 1.2.

| Mã FR | Chức năng | Đối tượng (Actor) | Mức độ | Yêu cầu / kết quả mong đợi |
| :--- | :--- | :--- | :-: | :--- |
| **FR-01** | Hồ sơ nhân sự | HR / Admin | **MVP** | Cung cấp giao diện và API (`POST`/`PUT`/`GET` `/employees`) cho phép tạo mới, tra cứu và cập nhật thông tin hồ sơ nhân viên (SR-01.1). Hệ thống tự động kiểm tra tính hợp lệ dữ liệu đầu vào (CCCD 12 số, mã số thuế) trước khi lưu (SR-01.2), đồng thời tự động ghi nhật ký kiểm toán (Audit Log) cho mọi thao tác chỉnh sửa hồ sơ (SR-01.3). |
| **FR-02** | Onboarding | HR | **MVP** | Thiết lập hồ sơ nhân viên mới, gán phòng ban/chức danh, cấu hình mức lương khởi điểm và khởi tạo tài khoản đăng nhập. |
| **FR-03** | Hợp đồng lao động | HR | **Mở rộng** | Lưu trữ loại hợp đồng, thời hạn, trạng thái (CRUD cơ bản); tự động cảnh báo hợp đồng sắp hết hạn trước 30 ngày (SR-02.1, SR-02.2, SR-02.3). Xử lý thủ tục nghỉ việc, chốt sổ BHXH, trợ cấp thôi việc và deactivate tài khoản ESS (SR-03.1, SR-03.2, SR-03.3). |
| **FR-04.1** | Chấm công thời gian cơ bản | Employee / HR | **MVP** | Ghi nhận giờ check-in/check-out, tổng ngày công thực tế, số lượt đi trễ/về sớm và trạng thái công từng ngày (SR-06.1, SR-06.2). |
| **FR-04.2** | Chấm công nâng cao (QR động + Ảnh) | Employee / HR | **Mở rộng** | Nhân viên quét mã QR động (tự động reset sau 10–15s). Khi quét mã hợp lệ, ứng dụng tự động mở camera selfie để chụp ảnh minh chứng và lưu dữ liệu để kiểm toán (SR-06.3). |
| **FR-05** | Quản lý nghỉ phép (PTO) | Employee / Manager / HR | **MVP** | Nhân viên tạo yêu cầu nghỉ phép, hệ thống kiểm tra số dư phép và gửi yêu cầu đến Quản lý trực tiếp để phê duyệt. Nếu nhân viên không có Quản lý trực tiếp thì HR thực hiện phê duyệt. Hệ thống cập nhật trạng thái yêu cầu và gửi thông báo kết quả cho nhân viên. |
| **FR-06** | Quản lý tăng ca (OT) | Employee / Manager / HR | **Mở rộng** | Đăng ký tăng ca theo ngày, số giờ và lý do; Quản lý duyệt/từ chối trước khi chốt vào kỳ tính lương (không phức tạp hóa). |
| **FR-07** | Engine tính lương cốt lõi | HR / Admin | **MVP** | Tính toán tự động Gross Salary dựa trên ngày công thực tế, lương cơ bản, ngày nghỉ không lương và phụ cấp. Hỗ trợ tính lương tỷ lệ (proration) và tạo bản ghi điều chỉnh lương hồi tố (Adjustment) cho kỳ trước mà không sửa dữ liệu đã khóa (SR-11.1, SR-11.2). |
| **FR-08** | Tính thuế TNCN & bảo hiểm | HR / Admin | **MVP** | Tự động trích BHXH/BHYT/BHTN và thuế TNCN lũy tiến theo các tham số pháp luật được cấu hình. |
| **FR-09** | Tính tiền tăng ca (OT) | HR / Admin | **Mở rộng** | Tính tiền OT bằng cách nhân hệ số cấu hình động (ngày thường/nghỉ/lễ) với đơn giá lương giờ. |
| **FR-10** | Phụ cấp & khấu trừ | HR / Admin | **Mở rộng** | Gán các khoản phụ cấp (ăn trưa, đi lại...) hoặc khấu trừ tùy chỉnh cho từng nhân viên/phòng ban. |
| **FR-11** | Xuất phiếu lương (PDF) | HR / System | **MVP** | Tự động sinh phiếu lương dạng PDF chuẩn định dạng, thể hiện chi tiết Gross, Net, thuế, bảo hiểm, OT, phụ cấp. |
| **FR-12** | Cổng tự phục vụ (ESS) | Employee | **Mở rộng** | Giao diện cá nhân để nhân viên tự tra cứu hồ sơ, hợp đồng, bảng công, số ngày phép, đơn từ và phiếu lương. |
| **FR-13** | Quy trình phê duyệt (Workflow) | Manager / HR | **Mở rộng** | Luồng phê duyệt 1 cấp cho PTO, OT: Gửi đơn $\rightarrow$ Chờ duyệt $\rightarrow$ Duyệt/Từ chối $\rightarrow$ Ghi nhận. Tự động lưu vết lịch sử phê duyệt `approval_log` (SR-15.1) và cho phép truy vấn lịch sử (SR-15.2). |
| **FR-14** | Xử lý tác vụ nền (Background worker) | System | **MVP** | Xử lý gửi email/notification bất đồng bộ bằng background worker đơn giản (không dùng Redis/Message Queue phức tạp), không block request chính. |
| **FR-15** | Dashboard & báo cáo HR | HR / Admin | **Mở rộng** | Hiển thị các KPI cần thiết: tổng nhân viên, đơn chờ duyệt, quỹ lương kỳ, tổng giờ OT, hợp đồng sắp hết hạn; hỗ trợ xuất Excel/PDF cơ bản. |
| **FR-16** | Xác thực & phân quyền (RBAC) | All Users | **MVP** | Phân quyền vai trò nghiêm ngặt (Employee, Manager, HR, Admin); ngăn chặn truy cập dữ liệu ngoài phạm vi. |
| **FR-17** | Nhật ký kiểm toán (Audit Trail) | HR / Admin | **Mở rộng** | Ghi vết tự động các thao tác nhạy cảm (Payroll, Attendance, Employee): người thực hiện, thời gian, giá trị cũ/mới. |

> **Ghi chú:** Cột “Mức độ” thể hiện độ ưu tiên triển khai: **MVP** là bắt buộc hoàn thành để hệ thống vận hành được; **Mở rộng** là các chức năng doanh nghiệp nâng cao nên có.

---

## 5. QUY TẮC NGHIỆP VỤ CỐT LÕI (BUSINESS RULES)

Các quy tắc nghiệp vụ (Business Rules – BR) dưới đây điều hướng toàn bộ logic xử lý dữ liệu ở tầng Backend và phải được tuân thủ nghiêm ngặt trong toàn bộ quá trình thiết kế và cài đặt hệ thống, không được phép có ngoại lệ ngầm định.

| Mã BR | Tên quy tắc | Nội dung mô tả & điều kiện ràng buộc |
| :--- | :--- | :--- |
| **BR-01** | **Quản lý kỳ lương** | Mỗi kỳ lương xác định thời gian bắt đầu/kết thúc và có các trạng thái chuyển tiếp: `Draft` $\rightarrow$ `Calculated` $\rightarrow$ `Approved` $\rightarrow$ `Locked`. Không được phép chỉnh sửa bảng công hay dữ liệu lương của kỳ đã khóa (`Locked`). |
| **BR-02** | **Công thức lương thực nhận** | $\text{Net Salary} = \text{Gross Salary} - (\text{BHXH} + \text{BHYT} + \text{BHTN}) - \text{Thuế TNCN} - \text{Các khoản khấu trừ khác} + \text{Phụ cấp}$.<br>Gross Salary được tính từ lương cơ bản, ngày công thực tế và tiền OT. |
| **BR-03** | **Tham số OT cấu hình động** | Hệ số OT (150%/200%/300%) không được hard-code mà phải lưu dạng tham số cấu hình theo luật hiện hành. |
| **BR-04** | **Chính sách thuế & bảo hiểm** | Biểu thuế TNCN lũy tiến, mức giảm trừ gia cảnh và tỷ lệ đóng bảo hiểm phải được cấu hình theo hiệu lực thời gian (effective date-based) để cập nhật khi luật thay đổi mà không sửa mã nguồn. |
| **BR-05** | **Quy trình phê duyệt đơn** | Mọi đơn PTO, đăng ký OT phải có trạng thái rõ ràng: `Draft`, `Pending`, `Approved`, `Rejected`, `Cancelled`. Chỉ 1 cấp duyệt duy nhất bởi Quản lý trực tiếp (hoặc HR nếu nhân viên không có Manager). |
| **BR-06** | **Bảo mật & quyền truy cập** | Dữ liệu nhân sự và lương thưởng tuân thủ nguyên tắc "Need-to-know". Nhân viên thường tuyệt đối không được xem dữ liệu bảng công, hồ sơ hoặc lương của nhân viên khác. |

---

## 6. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS)

Yêu cầu phi chức năng quy định các tiêu chuẩn chất lượng mà hệ thống phải đạt được, độc lập với các chức năng nghiệp vụ cụ thể. Mỗi NFR được gắn tiêu chí đo lường và kiểm thử cụ thể (đo lường được), tránh mọi phát biểu mơ hồ kiểu “hệ thống phải nhanh” hay “hệ thống phải an toàn”.

| Mã NFR | Nhóm yêu cầu | Tiêu chuẩn đo lường & tiêu chí kiểm thử cụ thể |
| :--- | :--- | :--- |
| **NFR-01** | **Bảo mật (Security)** | Xác thực API bằng JWT/OAuth2; mã hóa mật khẩu bằng BCrypt; bảo vệ dữ liệu truyền tải bằng TLS 1.3; áp dụng Row-Level Access Control; phòng chống OWASP Top 10 (SQL Injection, XSS...). |
| **NFR-02** | **Cách ly dữ liệu (Data Isolation)** | Kiểm tra phân quyền cấp dữ liệu (row-level security theo RBAC): Employee chỉ đọc/ghi dữ liệu của chính mình; Manager chỉ truy cập nhân viên thuộc team quản lý. |
| **NFR-03** | **Khả năng kiểm toán (Auditability)** | Tự động ghi Audit Log cho mọi thao tác sửa đổi dữ liệu lương và ngày công; bảng log dạng append-only, cấm xóa/sửa kể cả với tài khoản Admin. |
| **NFR-04** | **Kiến trúc & chuẩn API** | Backend theo mô hình RESTful, thiết kế Contract-First tuân thủ OpenAPI 3.1 YAML (lưu tại `docs/openapi-spec.yaml`). |
| **NFR-05** | **Hiệu năng & thời gian phản hồi** | Hệ thống phải hỗ trợ tối thiểu 50 users đồng thời. Khi phát sinh tác vụ gửi email/notification, tác vụ được đưa vào background worker để xử lý bất đồng bộ, đảm bảo request chính không bị block và thời gian phản hồi không vượt quá 3 giây trong điều kiện tải thông thường. |
| **NFR-06** | **Khả năng mở rộng (Scalability)** | Các module Engine tính lương/thuế được thiết kế độc lập; hệ thống chịu tải tốt khi mở rộng đến 2.000 nhân viên mà không đổi kiến trúc cốt lõi. |
| **NFR-07** | **Kiểm thử tự động (Unit Test)** | Unit Test Coverage $\ge 60\%$ tổng mã nguồn Backend, ưu tiên Payroll Engine, Tax/Benefit Rules, Approval Workflow. |
| **NFR-08** | **Kiểm thử tích hợp (E2E Test)** | Kịch bản kiểm thử E2E tự động (Playwright/Jest) cho luồng: Đăng nhập $\rightarrow$ Chấm công/Tạo đơn PTO $\rightarrow$ Duyệt đơn $\rightarrow$ Tính lương $\rightarrow$ Sinh phiếu lương PDF. |
| **NFR-09** | **Đóng gói & triển khai (Deployment)** | Đóng gói toàn bộ bằng Docker, khởi chạy qua `docker-compose.yml`; có README hướng dẫn chạy môi trường Local/Staging. |
| **NFR-10** | **Tự động hóa CI/CD** | Pipeline GitHub Actions tự động Build & chạy Unit Test khi tạo Pull Request; yêu cầu ít nhất 1 reviewer phê duyệt trước khi merge vào nhánh chính. |
| **NFR-11** | **Độ dễ sử dụng (Usability)** | Giao diện Web Responsive đa màn hình; form có validation rõ ràng, thông báo lỗi thân thiện; HR thực hiện thao tác chính trong $\le 3$ bước. |
| **NFR-12** | **Độ tin cậy & toàn vẹn (Reliability)** | Áp dụng Transaction Rollback cho thao tác tính lương và khóa kỳ lương; khi sự cố giữa chừng, dữ liệu phải quay về điểm an toàn, không mất/sai lệch dữ liệu tài chính. |
| **NFR-13** | **Tính bảo trì (Maintainability)** | Mã nguồn tổ chức theo Clean/Layered Architecture, tuân thủ clean code và Git Flow thống nhất; tự động cập nhật tài liệu API. |
| **NFR-14** | **Tuân thủ pháp lý (Compliance)** | Công thức lương, OT, bảo hiểm, thuế TNCN tuân thủ Luật Lao động Việt Nam hiện hành và Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân. |

---

## 7. PHÂN TÍCH & LUẬN CHỨNG CHỈ SỐ AVAILABILITY (TÍNH SẴN SÀNG HỆ THỐNG)

### 7.1 Mức cam kết mục tiêu
Hệ thống được thiết kế hướng tới đạt chỉ số **Availability = 99,5%** (tương ứng tổng thời gian gián đoạn dịch vụ/downtime cho phép tối đa $\le 3,6$ giờ/tháng, hay $\le 3$ giờ 36 phút/tháng).

### 7.2 Cơ sở lựa chọn & luận chứng
- **Đặc thù nghiệp vụ hệ thống nội bộ:** Khác với hệ thống ngân hàng trực tuyến hay sàn thương mại điện tử vốn đòi hỏi khả năng đáp ứng 24/7/365, hệ thống Topic 9 phục vụ đối tượng người dùng nội bộ doanh nghiệp với traffic mang tính chu kỳ rõ rệt, tập trung vào hai khung giờ:
  - *Khung giờ hành chính (08h00 – 17h30):* Nhân viên truy cập check-in/check-out, gửi đơn PTO, đăng ký OT.
  - *Khung giờ chốt lương cuối tháng (ngày 25 – 30 hằng tháng):* Bộ phận HR tập trung tính lương hàng loạt, rà soát bảng lương và phát hành phiếu lương.
- **Cân bằng chi phí hạ tầng & tính khả thi cho đồ án:** Để tăng Availability từ 99,5% lên 99,9% hay 99,99% (“ba hoặc bốn số 9”), hệ thống bắt buộc phải trang bị hạ tầng phức tạp: Load Balancer phần cứng, cụm cơ sở dữ liệu đa vùng (multi-region active-active), cơ chế auto-failover... Điều này tốn kém chi phí lớn và không khả thi đối với mô hình đồ án phát triển phần mềm trong 8 tuần. Mức 99,5% cho phép hệ thống thực hiện bảo trì, vá lỗi ngoài giờ hành chính (off-peak hours) một cách an toàn mà không ảnh hưởng đến vận hành doanh nghiệp.

### 7.3 Mô hình toán học mô phỏng kỹ thuật (MTBF & MTTR)
Độ sẵn sàng hệ thống được chứng minh dựa trên hai chỉ số kỹ thuật tiêu chuẩn ISO:
- **MTBF (Mean Time Between Failures):** Thời gian hoạt động ổn định trung bình giữa 2 lần phát sinh sự cố. Định hướng thiết kế hệ thống đạt $\text{MTBF} = 500$ giờ ($\approx 20,8$ ngày vận hành liên tục không lỗi).
- **MTTR (Mean Time To Repair):** Thời gian trung bình để phát hiện, sửa lỗi, deploy bản vá và khôi phục hệ thống chạy bình thường. Cam kết $\text{MTTR} \le 2,5$ giờ (150 phút).

$$\text{Availability} = \left[ \frac{\text{MTBF}}{\text{MTBF} + \text{MTTR}} \right] \times 100\% = \left[ \frac{500}{500 + 2,5} \right] \times 100\% \approx 99,502\% \quad (\approx 99,5\%)$$

#### * ĐỊNH LƯỢNG CHI TIẾT 150 PHÚT MTTR (QUY TRÌNH XỬ LÝ SỰ CỐ)

| Bước | Giai đoạn xử lý | Hành động kỹ thuật cụ thể | Thời gian |
| :-: | :--- | :--- | :-: |
| **1** | **Phát hiện sự cố** *(Detection)* | Hệ thống Monitoring phát hiện lỗi (VD: batch job tính lương treo, API trả mã lỗi 500) hoặc người dùng HR báo sự cố qua kênh hỗ trợ. | 15 - 30 phút |
| **2** | **Chẩn đoán nguyên nhân** *(Diagnosis)* | Tra cứu Audit Log và Server Log để định vị nguyên nhân (VD: sai công thức thuế TNCN, tràn bộ nhớ, nghẽn truy vấn DB). | 30 - 45 phút |
| **3** | **Khắc phục & kiểm thử** *(Fix & Test)* | Đội Developer sửa lỗi trên nhánh hotfix, chạy tự động toàn bộ Unit Test để đảm bảo không phát sinh lỗi kéo theo (regression). | 45 - 60 phút |
| **4** | **Triển khai & khôi phục** *(Deployment)* | GitHub Actions tự động deploy bản vá lên server qua Docker; khôi phục và transaction rollback dữ liệu về trạng thái an toàn gần nhất. | 15 - 30 phút |

$\rightarrow$ **Tổng thời gian khôi phục dịch vụ trung bình (MTTR):** $\sim 150$ phút (2,5 giờ).

---

## 8. KIẾN TRÚC KỸ THUẬT & MỨC ƯU TIÊN TRIỂN KHAI

Bảng dưới đây quy định các hạng mục kỹ thuật bắt buộc, khuyến nghị và mở rộng, cùng phương án triển khai phù hợp với quy mô một nhóm sinh viên thực hiện đồ án.

| Hạng mục kỹ thuật | Mức độ ưu tiên | Phương án triển khai phù hợp |
| :--- | :-: | :--- |
| **REST API + OpenAPI 3.1** | Bắt buộc | Thiết kế API theo phong cách Contract-First, duy trì file YAML chuẩn hóa làm giao ước kết nối giữa Frontend và Backend. |
| **Database + RBAC** | Bắt buộc | Thiết kế lược đồ CSDL quan hệ chuẩn hóa; middleware kiểm tra phân quyền chặt chẽ ở phía Backend. |
| **Docker + docker-compose** | Bắt buộc | Đóng gói hoàn chỉnh Web App, Backend Service và Database; hướng dẫn khởi chạy tự động. |
| **Unit Test Coverage $\ge 60\%$** | Bắt buộc | Tập trung test case cho toàn bộ logic nghiệp vụ tính lương, tính thuế, công thức OT và quy trình duyệt đơn. |
| **Git + PR Review + CI Pipeline** | Bắt buộc | Branching strategy (feature branch); mọi PR được review và pass CI (GitHub Actions) trước khi merge. |
| **Xử lý bất đồng bộ (Async Worker)** | Khuyến nghị | Background Worker cho gửi email và xuất PDF phiếu lương để không làm treo luồng request chính. |
| **Background Worker đơn giản** | Khuyến nghị | Hệ thống phải xử lý gửi email/notification bất đồng bộ bằng background worker đơn giản. |

#### * YÊU CẦU GIAO DIỆN NGOÀI (EXTERNAL INTERFACE REQUIREMENTS)
- **Giao diện API:** RESTful API, đặc tả Contract-First theo chuẩn OpenAPI 3.1 YAML, lưu trữ tại `docs/openapi-spec.yaml`; mọi thay đổi hợp đồng API phải qua Pull Request review (SR-22.1, SR-22.2).
- **Giao diện người dùng:** Web Responsive, tương thích đa kích thước màn hình; các form đều có validation dữ liệu đầu vào rõ ràng (NFR-11).
- **Giao diện thông báo:** Kênh in-app (polling), Email cơ bản (SR-16.1, SR-16.2).
- **Giao diện triển khai:** Đóng gói bằng Docker/docker-compose, có tài liệu README hướng dẫn khởi chạy môi trường Local/Staging (NFR-09).

---

## 9. LUỒNG NGHIỆP VỤ TỔNG THỂ (END-TO-END WORKFLOW)

Quy trình vận hành chuẩn từ đầu đến cuối của hệ thống HRM & Payroll được số hóa qua 8 bước tuyến tính, đảm bảo tính chặt chẽ và phân định rõ trách nhiệm giữa các bên:

1. **Bước 1: Tiếp nhận & Onboarding hồ sơ nhân viên** *(HR thực hiện)*  
   HR khởi tạo hồ sơ nhân viên mới, gán phòng ban, chức danh, cấu hình mức lương khởi điểm, các thông số đóng bảo hiểm/thuế và cấp tài khoản truy cập hệ thống.
2. **Bước 2: Ghi nhận công & Phát sinh đơn từ** *(Nhân viên thực hiện)*  
   Hằng ngày, nhân viên thực hiện check-in/check-out. Khi có nhu cầu, nhân viên chủ động gửi các đơn xin nghỉ phép (PTO) hoặc đăng ký làm thêm giờ (OT) qua cổng tự phục vụ ESS.
3. **Bước 3: Phê duyệt đơn từ** *(Manager / HR thực hiện)*  
   Đơn PTO/OT được gửi đến Quản lý trực tiếp để xem xét và phê duyệt/từ chối. Nếu nhân viên không có Quản lý trực tiếp thì HR thực hiện phê duyệt. Hệ thống ghi nhận trạng thái và lịch sử phê duyệt.
4. **Bước 4: Khóa bảng công kỳ lương** *(HR thực hiện)*  
   Vào cuối kỳ công, HR tiến hành rà soát và bấm chốt (khóa) bảng công của toàn bộ nhân viên. Lúc này dữ liệu ngày công, giờ OT, lượt đi trễ/về sớm được cố định.
5. **Bước 5: Engine tự động tính lương** *(Hệ thống thực hiện)*  
   Hệ thống tự động chạy batch job tính toán Gross Salary dựa trên ngày công thực tế, tính tiền OT, trích đóng BHXH/BHYT/BHTN, trừ thuế TNCN lũy tiến để ra Net Salary.
6. **Bước 6: Phê duyệt bảng lương & Khóa kỳ lương** *(HR / Admin thực hiện)*  
   HR rà soát lại toàn bộ bảng tính lương. Sau khi xác nhận số liệu chính xác, HR/Admin phê duyệt và chuyển trạng thái kỳ lương sang `Locked` (Khóa hoàn toàn).
7. **Bước 7: Tự động phát hành phiếu lương** *(Hệ thống thực hiện)*  
   Hệ thống tự động sinh file PDF phiếu lương chi tiết cho từng nhân viên, đồng thời gửi email thông báo và đẩy thông báo in-app.
8. **Bước 8: Tra cứu phiếu lương** *(Nhân viên thực hiện)*  
   Nhân viên đăng nhập vào Cổng tự phục vụ ESS để xem chi tiết bảng tính lương hoặc tải file PDF phiếu lương về máy.

> **LƯU Ý QUAN TRỌNG VỀ RÀNG BUỘC KỲ LƯƠNG (BR-01 & SR-11.2):**
> - **Tính không thể đảo ngược:** Sau khi kỳ lương chuyển sang trạng thái `Locked` ở Bước 6, hệ thống khóa toàn bộ dữ liệu công và lương của kỳ đó, tuyệt đối không cho phép chỉnh sửa hay xóa để đảm bảo tính kiểm toán.
> - **Cơ chế xử lý điều chỉnh (Adjustment):** Trường hợp phát hiện sai sót hoặc thiếu công sau khi đã khóa kỳ lương, HR không mở lại kỳ cũ mà sẽ tạo bản ghi điều chỉnh (Adjustment). Số tiền điều chỉnh này sẽ tự động được cộng hoặc trừ vào kỳ tính lương của tháng tiếp theo.

---

## 10. MA TRẬN TRUY VẾT YÊU CẦU (REQUIREMENTS TRACEABILITY MATRIX)

Ma trận dưới đây thể hiện mối liên hệ hai chiều giữa Yêu cầu Chức năng (FR), Yêu cầu Người dùng gốc (UR), Quy tắc nghiệp vụ (BR) và Yêu cầu Phi chức năng (NFR) liên quan, đảm bảo mọi yêu cầu đều có thể truy vết nguồn gốc và không có yêu cầu nào bị “mồ côi” (orphan requirement) trong quá trình phát triển và kiểm thử.

| Mã FR | UR liên quan | SR liên quan | BR liên quan | NFR liên quan |
| :--- | :--- | :--- | :--- | :--- |
| **FR-01** | UR-01 | SR-01.1, SR-01.2, SR-01.3 | BR-06 | NFR-02, NFR-13 |
| **FR-02** | UR-01 | SR-01.1, SR-01.2 | BR-06 | NFR-01 |
| **FR-03** | UR-02, UR-03 | SR-02.1, SR-02.2, SR-02.3, SR-03.1, SR-03.2, SR-03.3 | — | NFR-13 |
| **FR-04.1** | UR-06 | SR-06.1, SR-06.2 | BR-06 | NFR-05, NFR-11 |
| **FR-04.2** | UR-06 | SR-06.3 | BR-06 | NFR-01 |
| **FR-05** | UR-04, UR-05 | SR-04.1, SR-04.2, SR-05.1, SR-05.2 | BR-05 | NFR-05, NFR-11 |
| **FR-06** | UR-07 | SR-07.1, SR-07.2, SR-07.3 | BR-03, BR-05 | NFR-05 |
| **FR-07** | UR-08, UR-11 | SR-08.1, SR-08.2, SR-08.3, SR-11.1, SR-11.2 | BR-01, BR-02 | NFR-05, NFR-06, NFR-12 |
| **FR-08** | UR-09 | SR-09.1, SR-09.2, SR-09.3 | BR-02, BR-04 | NFR-14 |
| **FR-09** | UR-10 | SR-10.1 | BR-03 | NFR-14 |
| **FR-10** | UR-10 | SR-10.2 | BR-02 | NFR-13 |
| **FR-11** | UR-12 | SR-12.1, SR-12.2, SR-12.3 | BR-01, BR-02 | NFR-09 |
| **FR-12** | UR-13 | SR-13.1, SR-13.2 | BR-06 | NFR-02, NFR-11 |
| **FR-13** | UR-14, UR-15 | SR-14.1, SR-14.2, SR-15.1, SR-15.2 | BR-05 | NFR-03, NFR-08 |
| **FR-14** | UR-16, UR-23 | SR-16.1, SR-16.2, SR-23.1 | — | NFR-05, NFR-09 |
| **FR-15** | UR-17, UR-18 | SR-17.1, SR-17.2, SR-18.1, SR-18.2 | — | NFR-05, NFR-06 |
| **FR-16** | UR-19 | SR-19.1, SR-19.2 | BR-06 | NFR-01, NFR-02 |
| **FR-17** | UR-20 | SR-20.1, SR-20.2 | BR-06 | NFR-03 |