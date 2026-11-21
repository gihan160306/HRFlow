# HRFlow - Frontend

Giao diện web của **Enterprise HR & Automated Payroll System**. Frontend chỉ là lớp hiển thị: mọi nghiệp vụ,
phân quyền và tính lương do backend (Spring Boot) quyết định; frontend không tự tính lương/thuế/bảo hiểm và
không giữ dữ liệu giả.

## Công nghệ

React 18 · Vite 6 · React Router 6 · Tailwind CSS · lucide-react (JavaScript/JSX).

## Chạy cục bộ

Yêu cầu Node.js >= 18 và backend đang chạy ở `http://localhost:8080` (xem `HuongDanCaiDat.md`).

```bash
cd frontend
npm install
npm run dev      # http://localhost:3000, Vite proxy /api -> http://localhost:8080
npm run build    # kiểm tra bản build production
```

`VITE_API_BASE_URL` mặc định là `/api/v1` (cùng origin; Vite proxy khi dev, nginx proxy khi chạy Docker).
Chỉ đặt URL tuyệt đối khi backend ở origin khác, và khi đó phải thêm origin vào `HRFLOW_CORS_ALLOWED_ORIGINS` của backend.

## Chạy bằng Docker Compose

Từ thư mục gốc: `docker compose up -d --build` (cần `.env` có `DB_PASSWORD` và `APP_JWT_SECRET` dạng Base64, xem `.env.example`).

- Frontend: http://localhost:3000 (nginx, proxy `/api` sang backend)
- Backend API: http://localhost:8000/api/v1 (cổng 8080 trong container)

## Tài khoản demo

Mật khẩu `password123`. Email nhân viên = `<mã nhân viên viết thường>@hnqmedia.example`.

| Vai trò | Email | Ghi chú |
|---|---|---|
| Admin | `admin@hnqmedia.example` | Không có hồ sơ nhân viên, không có quyền payroll |
| HR | `nv004@hnqmedia.example` | Grant payroll `VIEW`, `PREPARE` |
| Manager (Giám đốc) | `nv001@hnqmedia.example` | Grant payroll `APPROVE` |
| Manager (Finance) | `nv003@hnqmedia.example` | Grant payroll `LOCK` |
| Manager (Lead) | `nv008@hnqmedia.example` | Duyệt PTO/OT của team |
| Employee | `nv009@hnqmedia.example` | |
| Máy chấm công tại quầy | `kiosk.letan@hnqmedia.example` | Tài khoản thiết bị `KIOSK`, gắn điểm "Quầy lễ tân", chỉ vào được `/kiosk` |

## Demo máy chấm công tại quầy (SRS FR-07, DEC-10)

1. Trên laptop làm máy quầy: chạy backend + `npm run dev`, mở `http://localhost:3000` và đăng nhập `kiosk.letan@hnqmedia.example`
   → trang `/kiosk` mở camera. Trình duyệt chỉ cho dùng camera trên `localhost` hoặc HTTPS, nên máy quầy phải mở bằng `localhost`.
2. Trên điện thoại cùng Wi-Fi: mở `http://<IP của laptop>:3000` (Vite đã bật `host: true`; mở cổng 3000 trên Windows Firewall nếu cần),
   đăng nhập nhân viên → **Chấm công** → đưa mã QR vào camera của máy quầy. Điện thoại chỉ hiển thị QR, không cần camera.
3. Máy quầy chào tên nhân viên, đếm 3 giây, chụp ảnh rồi ghi check-in/check-out; điện thoại tự báo kết quả.
   Lượt đầu trong ngày là check-in; quét lại trong 30 phút sau check-in không tạo check-out.

Proxy (Vite khi dev, nginx khi Docker) bỏ header `Origin` của request `/api`, nên truy cập bằng IP LAN không cần sửa CORS của backend.
Muốn trỏ dev server tới backend ở cổng khác: `VITE_PROXY_TARGET=http://localhost:18080 npm run dev`.

## Chức năng đã nối API thật

| Trang | Endpoint chính |
|---|---|
| Đăng nhập, đổi mật khẩu (bắt buộc khi `mustChangePassword`), đăng xuất | `/auth/login`, `/auth/me`, `/auth/change-password`, `/auth/logout` |
| Tổng quan, thông báo | `/dashboard/summary`, `/notifications` |
| Nhân viên (danh sách phân trang, chi tiết, tạo/sửa, onboarding, nghỉ việc, hồ sơ của tôi) | `/employees`, `/employees/options`, `/me/profile` |
| Phòng ban | `/departments`, `/job-titles` |
| Hợp đồng (tạo, gia hạn, chấm dứt) | `/contracts` |
| Chấm công tại quầy: hiện QR cá nhân (đổi mỗi 15 giây) để máy quầy quét, bảng công, ảnh minh chứng, điều chỉnh công, xác nhận công, điểm chấm công (HR) | `/attendance/my-qr`, `/attendance/*`, `/attendance-points` |
| Máy chấm công tại quầy (tài khoản `KIOSK`, trang `/kiosk` toàn màn hình): camera quét QR → chụp ảnh → ghi công | `/kiosk/me`, `/kiosk/scan`, `/kiosk/punch` |
| Nghỉ phép (xem trước, nháp/gửi, hủy, yêu cầu hủy, duyệt/từ chối) | `/leave-requests/*`, `/leave-balances/me` |
| Tăng ca (lập/công bố/đóng lịch, đăng ký, duyệt) | `/ot-schedules`, `/ot-registrations` |
| Kỳ lương (tạo, kiểm tra đầu vào, chốt công, tính, đối soát, duyệt, khóa, chi trả, CSV, nhật ký) | `/payroll/periods/*` |
| Phiếu lương cá nhân (xem/tải PDF) | `/payslips/me`, `/payslips/{id}/pdf` |
| Tài khoản & phân quyền: tạo/khóa/mở khóa/đặt lại mật khẩu/đổi vai trò, đề nghị cấp tài khoản (HR gửi, Admin xử lý), cấp/thu hồi quyền payroll | `/accounts`, `/account-requests`, `/grants` |
| Tuyến duyệt, khai báo người duyệt vắng mặt | `/approval-routes`, `/approver-unavailability` |
| Hồ sơ có hiệu lực của nhân viên (HR): lương, phụ cấp, điều chuyển, người phụ thuộc, bảo hiểm, thuế TNCN, lịch làm việc | `/employees/{id}/salaries` … `/schedules` |
| Thu nhập khác (thưởng, khấu trừ, hoàn ứng) và doanh thu Sales | `/income-items`, `/sales-revenues` |
| OT: xác nhận giờ thực tế, HR rà soát, OT ngoài lịch | `/ot-registrations/{id}/actual`, `/ot-unscheduled` |
| Cấu hình & điều chỉnh lương: adjustment sau khóa, tham số payroll, phân loại thuế loại phụ cấp | `/payroll/adjustments`, `/parameters`, `/allowance-types` |
| Danh mục (ngày lễ, lịch làm việc, loại nghỉ, biểu thuế), báo cáo, thông báo, nhật ký kiểm toán | `/holidays`, `/reports/*`, `/notifications`, `/audit-logs`, `/job-runs` |

## Phân quyền ở frontend

- Role lấy từ backend (`ADMIN`, `HR`, `MANAGER`, `EMPLOYEE`; tài khoản thiết bị `KIOSK` của máy chấm công chỉ vào được `/kiosk`); quyền payroll (`VIEW`/`PREPARE`/`APPROVE`/`LOCK`) lấy từ `grants` của `/auth/me`
  và quyết định hiển thị menu/nút của Kỳ lương. Nút thao tác trên kỳ lương lấy từ `allowedActions` do backend trả về.
- Ẩn/hiện trang chỉ là lớp trải nghiệm; backend vẫn kiểm tra quyền ở mọi API (401/403/404).
- Token JWT lưu trong `localStorage`; khi backend trả 401 (hết hạn/thu hồi) frontend tự đăng xuất.

## Chưa có giao diện (backend đã có API)

Số dư phép và điều chỉnh số dư (`/leave-balances/{id}/adjust`), quyết định xử lý xung đột lợi ích của kỳ lương
(`payrollApi.resolveConflict` đã có, chưa có biểu mẫu), refresh token (`/auth/refresh`).
