# Tái kiểm thử các ca Người 2 còn “Đang test” — 28/11/2026

Môi trường: nhánh `dev` sau khi kéo commit `496824b`, Docker Compose (PostgreSQL, backend, frontend); dữ liệu demo local. Đã chạy web và API. Trong 8 ca còn “Đang test”: **5 đạt, 3 bị chặn bởi máy thử không có webcam**. Trạng thái đã cập nhật trong tab Checklist của file phân công kiểm thử.

| Ca | Kết quả | Minh chứng / giới hạn |
|---|---|---|
| KIO-01 | Bị chặn | Đăng nhập tài khoản quầy, `/kiosk` hiện toàn màn hình và đúng điểm Quầy lễ tân. Máy thử không có thiết bị camera; trình duyệt báo `NotReadableError`. Cần kiểm tra lại luồng camera trên máy có webcam. Ảnh: [KIO-01](evidence/KIO-01-no-camera.png). |
| KIO-02 | Đạt | NV009 mở QR trên tab web; đồng hồ bắt đầu 15 giây và tự cấp QR mới khi hết hạn. Ảnh: [KIO-02](evidence/KIO-02-qr-autorefresh.png). Chưa thử trên điện thoại thật, nhưng ca chấp nhận tab khác. |
| KIO-03 | Bị chặn | Bộ API KIOSK đạt 35/35: QR hợp lệ, ảnh JPEG và check-in ghi bản công/lịch sử. Chưa thể quét bằng camera thật vì máy không có webcam. |
| KIO-09 | Bị chặn | API từ chối không có ảnh (`422 PHOTO_REQUIRED`), ảnh quá 200 KB (`422 PHOTO_TOO_LARGE`), QR hết hạn/đã dùng. Chưa thể thử thao tác từ chối quyền camera trên webcam thật. |
| ATT-01 | Đạt | NV009 xem bảng công tháng 11 và tháng 10; tháng 10 có 22 dòng, giờ làm, phút đi muộn và trạng thái. Ảnh giao diện: [ATT-01](evidence/ATT-01-november-history.png). |
| LEA-02 | Đạt | 13:00–17:00 ngày 08/12 được tính 240 phút = 0,5 ngày; ca sáng theo lịch 210 phút = 0,4375 ngày, đúng quy tắc quy đổi theo phút trong `payroll_rules.md`. Giờ kết thúc trước bắt đầu bị `422`. Ảnh: [LEA-02](evidence/LEA-02-four-hours-half-day.png). |
| LEA-12 | Đạt | Đơn nghỉ không lương 23/12 và nghỉ ốm 24/12 của NV009 đã duyệt, giữ đúng loại và không trừ quỹ phép năm. Phần đối chiếu tiền trên kỳ lương thuộc ca của Người 3. Ảnh: [LEA-12](evidence/LEA-12-approved-types.png). |
| OT-06 | Đạt | Ba dòng OT ngoài lịch của NV009 vẫn `PENDING` ở giao diện; không tự xác nhận. `SnapshotBuilder` chỉ đưa OT ngoài lịch `CONFIRMED` vào snapshot lương. Phần tính tiền sau xác nhận thuộc ca của Người 3. Ảnh: [OT-06](evidence/OT-06-pending-unscheduled.png). |

Không phát hiện lỗi mới trong các ca đã chạy trọn vẹn. Máy thử không liệt kê thiết bị Camera trong Windows. Cần dùng một máy có webcam để hoàn tất KIO-01, KIO-03 và KIO-09; giữ trạng thái **Bị chặn** cho đến khi thử trên thiết bị đó.

Lần chạy `test/kiosk_attendance_api_test.py` đạt **35/35**. Bộ test đã tạo một bản công demo cho NV009 ngày 28/11 và một tài khoản KIOSK demo được khóa sau kiểm thử; đây chỉ là dữ liệu local.
