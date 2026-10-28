# HRFlow - Frontend Giao Diện Quản Trị Nhân Sự & Tính Lương Tự Động

Phân hệ giao diện Web hiện đại dành cho hệ thống **Enterprise HR & Automated Payroll System** (Topic 9, Công nghệ phần mềm).

---

## Công Nghệ Sử Dụng

- **Framework:** React 18, TypeScript, Vite
- **Styling:** Tailwind CSS (Enterprise Design System, Inter Font, responsive design)
- **Icons:** Lucide React
- **Routing:** React Router v6
- **Architecture:** Role-Based Access Control (RBAC), Contract-First (OpenAPI 3.1), Clean State Storage & Persistence

---

## Các Phân Hệ & Chức Năng Đã Hoàn Thiện

1. **Xác thực & Phân quyền (Auth & RBAC):**
   - Đăng nhập bảo mật với hỗ trợ **1-Click Quick Login** bằng 4 tài khoản mẫu:
     - **Admin:** `admin@example.com` (Đặng Minh Hiếu)
     - **HR:** `hr@example.com` (Hồ Thị Gia Hân)
     - **Manager:** `manager@example.com` (Trần Thị Lệ Hoa)
     - **Employee:** `employee1@example.com` (Nguyễn Hoàng Phương Ngân)
   - **Quick Role Switcher:** Nút chuyển đổi vai trò trực tiếp trên thanh điều hướng topbar giúp kiểm thử tức thì ma trận quyền hạn theo `docs/permission_matrix.md`.
2. **Bảng điều khiển (Dashboard):**
   - Thẻ KPI: Tổng nhân sự, tỷ lệ chuyên cần hôm nay, đơn chờ duyệt, quỹ lương tháng.
   - Cảnh báo hợp đồng sắp hết hạn trong 30-45 ngày (SR-02.1).
   - Cơ cấu nhân sự theo phòng ban & nhật ký hoạt động kiểm toán gần đây.
3. **Hồ sơ nhân sự (Employees):**
   - Danh sách nhân viên, tìm kiếm theo tên/mã/email, lọc theo phòng ban & trạng thái.
   - Tiếp nhận nhân sự mới (Onboarding Wizard) với validate CCCD 12 số, mã số thuế, lương cơ bản (SR-01.1, SR-01.2).
   - Xem chi tiết hồ sơ nhân sự (lương, người phụ thuộc, hợp đồng).
   - Thủ tục thôi việc (Offboarding / Terminate) và lưu vết kiểm toán (SR-03.1, SR-03.3).
4. **Quản lý phòng ban (Departments):**
   - Danh sách phòng ban, mã viết tắt, trưởng bộ phận và định biên nhân sự.
   - Thêm phòng ban mới.
5. **Chấm công & Thời gian làm việc (Attendance):**
   - Widget chấm công thời gian thực với đồng hồ số UTC+7.
   - Check-in / Check-out, tính số phút đi trễ/về sớm sau 08:30 (SR-06.2).
   - Chụp ảnh selfie minh chứng và định vị GPS văn phòng (SR-06.3).
   - Bảng ghi nhận công (Timecard Logs).
6. **Chấm công nâng cao bằng QR Động (Dynamic QR Kiosk):**
   - Màn hình Kiosk / Lễ tân sinh mã QR động tự động làm mới mỗi 15 giây chống gian lận.
   - Giả lập máy quét di động bật camera selfie chụp ảnh xác thực (SR-06.3).
7. **Nghỉ phép (Paid Time Off - PTO):**
   - Thẻ theo dõi số dư ngày phép (Phép năm còn lại 9/12, Phép ốm đau, Nghỉ không lương).
   - Tạo đơn xin nghỉ phép, kiểm tra tự động số dư ngày phép trước khi gửi (SR-04.1, SR-04.2).
   - Phê duyệt đơn 1 cấp dành cho Quản lý & HR (SR-05.1, BR-05).
   - Bắt buộc nhập lý do khi Từ chối đơn nghỉ phép (SR-05.1).
8. **Làm thêm giờ (Overtime - OT):**
   - Lịch làm thêm giờ công bố theo hệ số luật: 150% (ngày thường), 200% (cuối tuần), 300% (ngày lễ) (BR-03, SR-10.1).
   - Công bố lịch OT mới & Đăng ký làm thêm giờ.
   - Phê duyệt / từ chối đăng ký OT.
9. **Động cơ tính lương tự động (Payroll Engine):**
   - Quản lý vòng đời kỳ lương: `Draft` ➔ `Calculated` ➔ `Approved` ➔ `Locked` (BR-01).
   - Nút **"Chạy tính lương" (Calculate)**: Tự động tính toán theo công thức chuẩn:
     - Lương theo ngày công thực tế (Proration).
     - Phụ cấp & Tiền OT (Lương giờ × Hệ số).
     - Trừ BHXH 8%, BHYT 1.5%, BHTN 1% có áp dụng mức trần 20 lần lương cơ sở.
     - Giảm trừ gia cảnh bản thân (15.5tr) & người phụ thuộc (6.2tr/người) theo Nghị quyết 110/2025/UBTVQH15.
     - Tính thuế TNCN lũy tiến theo Biểu 5 bậc mới (Luật Thuế TNCN 2025).
     - Lương thực nhận (Net Salary).
   - Phê duyệt bảng lương (Approve) & Khóa kỳ lương (Lock - dữ liệu trở nên bất biến theo BR-01).
   - Thêm bản ghi điều chỉnh lương hồi tố (Adjustment) theo SR-11.2.
10. **Bảng lương chi tiết (Payroll Spreadsheet):**
    - Bảng tổng hợp lương toàn công ty với đầy đủ các cột thu nhập, khấu trừ và thực lĩnh.
    - Xuất dữ liệu bảng lương định dạng file CSV / Excel.
11. **Phiếu lương cá nhân (Payslips / ESS):**
    - Cổng tự phục vụ dành cho nhân viên tra cứu lịch sử phiếu lương các kỳ đã khóa (UR-12, SR-12.2).
    - Mẫu phiếu lương doanh nghiệp chuẩn đẹp, chi tiết từng dòng thu nhập, khấu trừ.
    - Hỗ trợ **In phiếu lương (Print) / Xuất PDF**.
12. **Cấu hình tham số hệ thống & Pháp lý (System Config):**
    - Tra cứu và điều chỉnh biểu thuế TNCN 5 bậc mới, mức giảm trừ gia cảnh, tỷ lệ bảo hiểm.
    - Tuân thủ nguyên tắc BR-04: Đổi luật không sửa mã nguồn.
13. **Nhật ký kiểm toán (Immutable Audit Trail):**
    - Lưu vết toàn bộ các thao tác thêm, sửa, tính lương, duyệt, khóa kỳ lương (SR-20.1, NFR-03).
    - Bộ lọc theo hành động và so sánh giá trị cũ/mới (Diff Viewer).

---

## 🛠️ Hướng Dẫn Chạy Cục Bộ (Local Development)

Yêu cầu: Đã cài đặt **Node.js >= 18** và **npm**.

```bash
# 1. Di chuyển vào thư mục frontend
cd frontend

# 2. Cài đặt thư viện phụ thuộc (nếu chưa cài)
npm install

# 3. Khởi chạy máy chủ phát triển
npm run dev
```

Ứng dụng sẽ chạy tại địa chỉ: **http://localhost:3000** (hoặc cổng được hiển thị trên terminal).

---

## 🐳 Chạy Bằng Docker Compose

Từ thư mục gốc dự án:

```bash
docker compose up -d --build
```

- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **Postgres Database:** localhost:5432
