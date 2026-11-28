# Kết quả tái kiểm thử Người 2 — 26/11/2026

Nguồn ca kiểm thử: `HRFlow – Phân công kiểm thử.pdf` do nhóm cung cấp. Kiểm tra trên máy local, nhánh `dev` tại commit `f76ba9a`, Docker Compose gồm PostgreSQL, backend và frontend. Đã đối chiếu đủ **38 mã ca**. Đây là dữ liệu demo local; các lệnh kiểm thử tạo thêm bản ghi chấm công, nghỉ phép, OT và một điểm chấm công QA đã vô hiệu hóa. Ngày lễ QA, đơn phép tạm và dấu vắng mặt của Lead đã được dọn/hoàn sau khi kiểm tra. Ca KIO-04 dùng mốc check-in mô phỏng lùi 31 phút trên một bản ghi demo.

**Tổng quan:** 29 ca đạt; 8 ca chỉ đạt một phần hoặc còn chờ điều kiện; 1 ca không đạt theo kỳ vọng tài liệu (OT-05). “API đạt” chỉ xác nhận server và dữ liệu trả về. Webcam thật và điện thoại thật vẫn cần kiểm tra trên thiết bị có camera.

## Chấm công tại quầy (KIO-01–13)

| Mã | Kết quả | Ghi chú |
|---|---|---|
| KIO-01 | Một phần | Đăng nhập KIOSK, tên điểm và màn hình quầy hiện đúng. Trình duyệt thử nghiệm báo `NotReadableError` khi mở camera; chưa xác nhận webcam thật. |
| KIO-02 | Một phần | QR và đồng hồ đếm ngược hiện trên web; API cấp mã 15 giây. Đã sửa số giây hiển thị sai thoáng qua khi mới mở, xác nhận lại bắt đầu ở 15 giây. Chưa thử điện thoại thật. |
| KIO-03 | API đạt; webcam chưa kiểm tra | Chạy lại trên NV012: quét QR qua API, gửi ảnh JPEG và ghi check-in thành công. |
| KIO-04 | Đạt với mốc thời gian mô phỏng | Lùi mốc check-in của NV012 31 phút trong DB demo rồi quét QR: `CHECK_OUT`, có ảnh vào/ra, 31 phút làm; quét tiếp bị `409 ALREADY_CHECKED_OUT`. |
| KIO-05 | API đạt | Quét lại ngay sau check-in trả `409 TOO_SOON_TO_CHECK_OUT`. |
| KIO-06 | API đạt | QR quá 15 giây trả `422 QR_EXPIRED`; không ghi công. |
| KIO-07 | API đạt | Mã đã dùng trả `422 QR_USED`. |
| KIO-08 | API đạt | QR sai định dạng/không do server cấp trả `422 QR_INVALID`; audit endpoint truy cập được. |
| KIO-09 | Một phần | Thiếu ảnh và ảnh quá 200 KB bị chặn. Chưa thử từ chối quyền camera trên máy thật. |
| KIO-10 | Đạt | KIOSK bị chặn khỏi API nhân viên; vào `/attendance` trên trình duyệt bị chuyển lại `/kiosk`. |
| KIO-11 | API đạt | EMP và HR gọi `/kiosk/me` đều bị `403`. |
| KIO-12 | API đạt | Ảnh của NV009: chính nhân viên, Lead và HR xem được; nhân viên khác, Admin và KIOSK bị chặn. |
| KIO-13 | Đạt | Chạy lại `test/kiosk_attendance_api_test.py` với NV012–NV014 chưa chấm công hôm nay: **35/35 PASS**. |

## Bảng công (ATT-01–07)

| Mã | Kết quả | Ghi chú |
|---|---|---|
| ATT-01 | API đạt | NV009 xem được tháng 11 và tháng 10 (22 dòng tháng 10); có phút trễ/sớm, không có khoản phạt tiền. Cần đối chiếu trình bày trên web. |
| ATT-02 | API đạt | NV019, NV047, NV092 thiếu check-out tháng 11 đều là `NEEDS_REVIEW`, không tự điền giờ ra. |
| ATT-03 | Đạt | NV047 bổ sung giờ ra, HR duyệt: `CONFIRMED`, 480 phút. NV092 bị từ chối có lý do: công giữ `NEEDS_REVIEW`. Thiếu minh chứng/lý do bị `422`. Tạo đơn QA của NV009, Lead thấy trong danh sách và từ chối trên web; API lưu đúng người quyết định và lý do. |
| ATT-04 | API đạt | Tạo điểm QA, mã trùng bị `409`, sửa tên và vô hiệu hóa thành công. Điểm QA còn trong DB ở trạng thái không hoạt động. |
| ATT-05 | API đạt | EMP chỉ thấy công của mình; Lead thấy team; HR thấy nhiều phòng ban. |
| ATT-06 | Đạt | Chạy lại: ngày lễ tạm 02/12 làm giờ chuẩn tháng giảm 480 phút, ngày đó là `HOLIDAY` và 0 phút phải làm. Đã xóa ngày lễ và xác nhận giờ chuẩn phục hồi. |
| ATT-07 | Đạt | Kỳ tháng 10 đã `LOCKED`; gửi điều chỉnh công hợp lệ cho 20/10 trả `409 PERIOD_FROZEN`, không tạo đơn. |

## Nghỉ phép (LEA-01–12)

| Mã | Kết quả | Ghi chú |
|---|---|---|
| LEA-01 | Đạt | Chạy lại với đơn QA tạm: chuyển `PENDING`, người duyệt NV008; quỹ chưa trừ khi chờ. Đơn đã hủy sau test. |
| LEA-02 | Cần thống nhất kỳ vọng nửa ngày | Ngày kết thúc trước bắt đầu và đơn trùng bị chặn. Ca sáng demo là 210/480 phút = **0,4375 ngày**, đúng cách quy đổi thời lượng thực trong `payroll_rules.md` và SRS. Nghỉ 4 giờ là **0,5 ngày**. PDF ghi chung “nửa ngày = 0,5 ngày (hoặc 4 giờ)” nên cần sửa ca test cho rõ lịch/khung giờ. |
| LEA-03 | API đạt | Đơn phép vượt 11,5 ngày còn lại bị `422 INSUFFICIENT_BALANCE`. |
| LEA-04 | API đạt | Thứ Sáu 11/12 đến thứ Hai 14/12 tính 2 ngày làm việc; duyệt trừ đúng 2 ngày. |
| LEA-05 | API đạt | Lead duyệt; quỹ, approval log và thông báo cho nhân viên cập nhật. |
| LEA-06 | API đạt | Từ chối thiếu lý do bị `422`; có lý do thì `REJECTED`, không trừ quỹ. |
| LEA-07 | API đạt | Lead tự duyệt đơn của mình bị `403`. |
| LEA-08 | API đạt | Manager ngoài tuyến duyệt đơn NV009 bị `403/404`. |
| LEA-09 | Đạt | Dùng tuyến NV009: Lead NV008, dự phòng NV002. Khi Lead khả dụng, dự phòng bị `403`; sau dấu vắng mặt tạm, thiếu lý do bị `422`, có lý do thì duyệt thay được và trừ đúng 1 ngày. Đã xóa dấu vắng mặt tạm, hủy đơn đã duyệt và hoàn quỹ. |
| LEA-10 | API đạt | Hủy đơn chờ; yêu cầu hủy đơn đã duyệt được Lead duyệt và hoàn quỹ. |
| LEA-11 | API đạt | Gửi `employeeId` giả trong request không đổi chủ đơn: đơn vẫn thuộc NV009. |
| LEA-12 | API đạt một phần | Nghỉ không lương và nghỉ ốm được duyệt, không trừ quỹ phép năm. Người 3 cần đối chiếu tác động kỳ lương. |

## Tăng ca (OT-01–06)

| Mã | Kết quả | Ghi chú |
|---|---|---|
| OT-01 | API đạt | Giờ kết thúc trước giờ bắt đầu bị `422`; HR tạo/công bố lịch OT cho đúng nhân viên. |
| OT-02 | API đạt | NV009 đăng ký chuyển `PENDING`; đăng ký trùng bị `409`, giờ ngoài lịch bị `422`. |
| OT-03 | API đạt | Lead duyệt/từ chối; từ chối thiếu lý do bị `422`; Lead tự duyệt bị `403`. |
| OT-04 | Chờ ngày OT | Lịch QA là 10/12/2026. API từ chối xác nhận giờ thực tế trước ngày OT bằng `422`; vì vậy chưa thể kiểm tra chênh lệch giờ thực tế so với giờ đăng ký/duyệt của ca này. |
| OT-05 | **Không đạt theo ca test** | Gửi OT ngoài lịch 23:30 ngày 25/11 → 00:30 ngày 26/11 trả `422 VALIDATION_ERROR` (“phải nằm trong một ngày”). Cả lịch OT cũng yêu cầu giờ kết thúc sau giờ bắt đầu trong cùng ngày. Bộ tính lương có hàm tách đoạn qua 00:00, nhưng luồng nhập hiện tại không nhận một lượt OT qua nửa đêm để kiểm tra nó. |
| OT-06 | Một phần | OT ngoài lịch 25/11 hiện `PENDING` ở tab riêng của Lead với nút xác nhận/từ chối; nhân viên cũng thấy dòng chờ. Chưa tự thành giờ trả lương; cần đối chiếu kỳ lương khi được xác nhận. |

## Lỗi đã sửa trong workspace

- `AttendanceService.createAdjustment` thiếu tham số ngày khi tìm bản ghi công, khiến mọi yêu cầu điều chỉnh trả `500`. Đã bổ sung tham số và xác minh API tạo/duyệt thành công.
- Bắt buộc minh chứng khi tạo điều chỉnh và lý do khi từ chối ở backend; đánh dấu ô minh chứng bắt buộc ở frontend.
- Thêm `frontend/.dockerignore` để Docker không sao chép `node_modules` trên máy vào image, tránh lỗi build.
- Đồng bộ đồng hồ giao diện ngay khi lấy QR mới, để số giây ban đầu không vượt 15 giây. Đã build lại frontend và xác nhận trên web hiển thị `Mã đổi sau 15 s` lúc mở.

Sau khi sửa, `docker compose up -d --build frontend` thành công; ba service khỏe, web và API hoạt động. Các thay đổi code hiện ở local, chưa commit/push.
