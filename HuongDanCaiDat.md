# HRFlow — HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY DỰ ÁN TRÊN MÁY THÀNH VIÊN

**Dự án:** HRFlow — Hệ thống chấm công và tính lương tự động  
**Đối tượng:** Thành viên nhóm phát triển  
**Môi trường hướng dẫn:** Windows 10/11, PowerShell, Docker Desktop và Git  
**Cách chạy chính:** Docker Compose

---

## 1. Mục đích

Tài liệu này hướng dẫn thành viên mới từ đầu đến cuối để có thể:

1. Cài đặt các phần mềm cần thiết.
2. Kiểm tra máy tính đã sẵn sàng để chạy dự án.
3. Clone source code HRFlow từ GitHub.
4. Tạo và cấu hình file `.env`.
5. Khởi động frontend, backend, database và các service liên quan.
6. Kiểm tra hệ thống có hoạt động bình thường hay không.
7. Chạy lại dự án trong những lần làm việc tiếp theo.
8. Cập nhật code mới từ nhóm và xử lý các lỗi thường gặp.

**Lưu ý:** Các lệnh bên dưới sử dụng PowerShell trên Windows. Thành viên cần thực hiện lần lượt và kiểm tra kết quả sau từng bước, không nên chạy tất cả lệnh cùng lúc.

# PHẦN I. CÀI ĐẶT MÔI TRƯỜNG

## Bước 1. Kiểm tra cấu hình máy tính

Máy tính cần đáp ứng các điều kiện cơ bản sau:

- Windows 10 hoặc Windows 11 64-bit.
- Kết nối Internet để tải phần mềm và source code.
- Có quyền cài đặt phần mềm.
- Có đủ dung lượng trống cho source code, Docker images và database.
- Có thể sử dụng PowerShell.

Nên dành tối thiểu khoảng 10 GB dung lượng trống nếu máy chưa có các Docker images cần thiết. Dung lượng thực tế phụ thuộc vào những image và phần mềm đã cài trên máy.

## Bước 2. Cài đặt Git

Git dùng để clone source code từ GitHub và đồng bộ code giữa các thành viên.

### 2.1. Tải Git

Truy cập:

https://git-scm.com/downloads/win

Tải phiên bản Git dành cho Windows và chạy file cài đặt.

### 2.2. Cài đặt

- Chạy file cài đặt.
- Có thể giữ các lựa chọn mặc định.
- Hoàn thành quá trình cài đặt.

### 2.3. Kiểm tra Git

Mở PowerShell mới và chạy:

```powershell
git --version
```

Kết quả mong đợi có dạng:

```text
git version 2.x.x.windows.x
```

Nếu xuất hiện thông báo không nhận diện được lệnh `git`, hãy đóng PowerShell, mở lại và thử lại. Nếu vẫn lỗi, kiểm tra lại quá trình cài đặt Git.

## Bước 3. Cài đặt Docker Desktop

Docker được sử dụng để chạy các service của HRFlow trong môi trường thống nhất, giảm sự khác biệt giữa máy tính của các thành viên.

### 3.1. Tải Docker Desktop

Truy cập:

https://www.docker.com/products/docker-desktop/

Tải Docker Desktop dành cho Windows và làm theo hướng dẫn cài đặt chính thức.

### 3.2. Khởi động Docker Desktop

Sau khi cài đặt:

1. Mở Docker Desktop.
2. Nếu chương trình yêu cầu cài đặt hoặc bật WSL 2, hãy thực hiện theo hướng dẫn của Docker.
3. Chờ Docker Desktop khởi động hoàn tất.
4. Kiểm tra trạng thái Docker Engine đã sẵn sàng.

Nếu máy yêu cầu khởi động lại sau khi cài đặt, hãy khởi động lại máy trước khi tiếp tục.

### 3.3. Kiểm tra Docker

Mở PowerShell và chạy:

```powershell
docker --version
```

Tiếp tục:

```powershell
docker compose version
```

Sau đó:

```powershell
docker info
```

**Kết quả mong đợi:**

- Hai lệnh đầu hiển thị phiên bản.
- `docker info` hiển thị thông tin Docker Engine mà không báo lỗi kết nối daemon.

Nếu gặp lỗi như `Cannot connect to the Docker daemon`, hãy mở Docker Desktop và đợi Docker Engine chạy xong rồi thử lại.

**Không cần cài PostgreSQL riêng nếu nhóm sử dụng service database trong Docker Compose.** Database sẽ được khởi động bằng Docker theo cấu hình dự án.

# PHẦN II. CLONE SOURCE CODE

## Bước 4. Chuẩn bị thư mục lưu dự án

Ví dụ, mỗi thành viên lưu source code trong thư mục `D:\Study`.

Mở PowerShell và chạy:

```powershell
New-Item -ItemType Directory -Force -Path "D:\Study"
Set-Location "D:\Study"
```

Có thể sử dụng thư mục khác, miễn là nhớ chính xác đường dẫn.

## Bước 5. Clone dự án từ GitHub

### 5.1. Lấy địa chỉ repository

Mở repository HRFlow trên GitHub do nhóm cung cấp.

Chọn **Code → HTTPS → Copy URL**.

Ví dụ minh họa:

```text
https://github.com/TEAM_NAME/HRFlow.git
```

Địa chỉ trên chỉ là ví dụ. Phải thay bằng URL repository thực tế của nhóm.

### 5.2. Clone

Chạy lệnh sau, thay URL bằng địa chỉ thực tế:

```powershell
git clone <REPOSITORY_URL>
```

Ví dụ:

```powershell
git clone https://github.com/TEAM_NAME/HRFlow.git
```

Nếu repository là private, thành viên phải được cấp quyền truy cập trước. Khi Git yêu cầu xác thực, hãy thực hiện theo hướng dẫn của GitHub; không chia sẻ token hoặc mật khẩu trong nhóm chat.

### 5.3. Đi vào thư mục dự án

```powershell
cd HRFlow
```

Kiểm tra vị trí hiện tại:

```powershell
Get-Location
```

Liệt kê file và thư mục:

```powershell
Get-ChildItem
```

Thành viên cần kiểm tra có file `docker-compose.yml` hoặc `compose.yaml` hay không.

Có thể kiểm tra bằng:

```powershell
Test-Path .\docker-compose.yml
Test-Path .\compose.yaml
```

Ít nhất một lệnh cần trả về `True` theo tên file Compose thực tế của repository.

**Nếu không tìm thấy file Compose, không tự tạo file mới.** Hãy kiểm tra đã vào đúng thư mục dự án chưa hoặc hỏi người phụ trách repository.

# PHẦN III. CẤU HÌNH FILE MÔI TRƯỜNG

## Bước 6. Kiểm tra file `.env`

File `.env` chứa các biến môi trường phục vụ quá trình khởi chạy, chẳng hạn mật khẩu database và khóa JWT.

File này thường không được commit lên Git vì có thể chứa thông tin nhạy cảm. Vì vậy, sau khi clone, thành viên có thể chưa có file `.env`.

Kiểm tra:

```powershell
Test-Path .\.env
Test-Path .\.env.example
```

- Nếu `.env` đã tồn tại, kiểm tra nội dung và cấu hình theo hướng dẫn của nhóm.
- Nếu `.env` chưa tồn tại nhưng `.env.example` có sẵn, có thể sao chép file mẫu.
- Nếu cả hai đều không tồn tại, hãy yêu cầu nhóm cung cấp danh sách biến môi trường chính xác. Không tự đoán tên biến.

### 6.1. Tạo `.env` từ file mẫu

Nếu repository có `.env.example`, chạy:

```powershell
Copy-Item .\.env.example .\.env
```

Mở file để chỉnh sửa:

```powershell
notepad .env
```

### 6.2. Cấu hình biến môi trường

Theo cấu hình HRFlow hiện tại, các biến cần chú ý gồm:

| Biến | Mục đích |
|---|---|
| `DB_PASSWORD` | Mật khẩu database mà cấu hình Docker yêu cầu |
| `APP_JWT_SECRET` | Khóa bí mật dùng để ký JWT theo cấu hình Spring Boot hiện tại |

Tên biến phải khớp chính xác với cách khai báo trong `docker-compose.yml` và `application.yml`.

Ví dụ minh họa:

```dotenv
DB_PASSWORD=thay_bang_mat_khau_local
APP_JWT_SECRET=thay_bang_chuoi_base64_hop_le
```

Đây chỉ là mẫu để hiểu cấu trúc, **không sử dụng nguyên văn các giá trị minh họa**.

### 6.3. Tạo khóa JWT Base64 hợp lệ

HRFlow hiện có code giải mã khóa JWT bằng Base64. Vì vậy, không nên sử dụng chuỗi thông thường có dấu gạch dưới hoặc ký tự không hợp lệ với Base64.

Mỗi thành viên có thể tạo một khóa ngẫu nhiên riêng trong PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$jwtSecret = [Convert]::ToBase64String($bytes)
$jwtSecret
```

Sao chép kết quả được in ra và đặt làm giá trị của `APP_JWT_SECRET` trong file `.env`.

Ví dụ cấu trúc:

```dotenv
DB_PASSWORD=mat_khau_local_cua_ban
APP_JWT_SECRET=dan_chuoi_base64_vua_tao_vao_day
```

Không ghi dấu nháy nếu không cần thiết và không để khoảng trắng thừa ở đầu hoặc cuối giá trị.

Khóa JWT dùng cho môi trường local không nên được chia sẻ công khai hoặc commit lên Git. Nếu thay đổi khóa JWT giữa các lần chạy, token đăng nhập cũ có thể không còn hợp lệ.

### 6.4. Kiểm tra file `.env`

Lưu file rồi chạy:

```powershell
Test-Path .\.env
```

Kết quả mong đợi:

```text
True
```

Nếu Compose báo thiếu `DB_PASSWORD` hoặc `APP_JWT_SECRET`, kiểm tra lại tên biến và cách viết trong `.env`.

**Không gửi nội dung `.env` lên GitHub, chụp ảnh công khai hoặc gửi lên nhóm chat nếu file có chứa thông tin bí mật.**

# PHẦN IV. KHỞI ĐỘNG HỆ THỐNG

## Bước 7. Kiểm tra cấu hình Docker Compose

Trước khi chạy, đảm bảo đang ở thư mục chứa file Compose:

```powershell
Get-Location
Get-ChildItem
```

Kiểm tra Docker Desktop đang hoạt động:

```powershell
docker info
```

Kiểm tra cấu hình Compose:

```powershell
docker compose config --quiet
```

Nếu lệnh trả về lỗi, cần xử lý lỗi cấu hình trước khi khởi động. Nếu lệnh hoàn tất mà không báo lỗi, có thể tiếp tục.

Lưu ý: lệnh kiểm tra cấu hình có thể đọc các giá trị biến môi trường. Không đăng công khai toàn bộ kết quả `docker compose config` nếu trong đó có thông tin bí mật.

## Bước 8. Build và chạy lần đầu

Trong lần chạy đầu tiên, nên build các image và khởi động các service:

```powershell
docker compose up -d --build
```

Giải thích:

- `up`: tạo và khởi động các service được khai báo trong Compose.
- `-d`: chạy nền để có thể tiếp tục sử dụng PowerShell.
- `--build`: build lại image nếu cấu hình yêu cầu.

Lần đầu chạy có thể mất nhiều thời gian vì Docker cần tải image nền và các dependency.

Không đóng Docker Desktop trong lúc các container đang hoạt động.

## Bước 9. Kiểm tra trạng thái các service

Sau khi lệnh khởi động hoàn tất, chạy:

```powershell
docker compose ps
```

HRFlow hiện có các service với tên dự kiến:

- `backend`: API Spring Boot.
- `db`: PostgreSQL.
- `frontend`: giao diện web.
- `worker`: tác vụ nền nếu service này được sử dụng trong cấu hình hiện tại.

Tên service thực tế phải căn cứ vào file Compose của repository.

### Cách đọc trạng thái

| Trạng thái | Ý nghĩa |
|---|---|
| `Up` hoặc `running` | Container đang chạy |
| `healthy` | Health check của service đã thành công |
| `starting` | Service đang khởi động hoặc đang kiểm tra sức khỏe |
| `unhealthy` | Container đang chạy nhưng health check thất bại |
| `Exited` | Container đã dừng |
| `Restarting` | Container liên tục khởi động lại |

Không nên kết luận hệ thống đã hoạt động hoàn toàn chỉ vì container được tạo thành công. Cần kiểm tra database, backend, frontend và log.

## Bước 10. Xem log nếu có vấn đề

Xem log toàn bộ hệ thống:

```powershell
docker compose logs --tail=100
```

Theo dõi log liên tục:

```powershell
docker compose logs -f
```

Nhấn `Ctrl + C` để thoát chế độ xem log. Thao tác này không có nghĩa là dừng các container đang chạy nền.

Xem log riêng từng service:

```powershell
docker compose logs --tail=100 backend
docker compose logs --tail=100 db
docker compose logs --tail=100 frontend
docker compose logs --tail=100 worker
```

Nếu backend liên tục restart, hãy ưu tiên xem log backend và database trước.

# PHẦN V. KIỂM TRA HỆ THỐNG SAU KHI KHỞI ĐỘNG

## Bước 11. Kiểm tra frontend

Mở trình duyệt và truy cập:

http://localhost:3000

Nếu cấu hình frontend hiện tại sử dụng cổng 3000, trang web sẽ được mở tại địa chỉ này.

Kiểm tra:

- Trang web có hiển thị hay không.
- Có lỗi tải tài nguyên hoặc màn hình trắng không.
- Có thể mở DevTools bằng `F12`, vào tab **Console** để xem lỗi JavaScript.
- Vào tab **Network** để kiểm tra request API có thất bại không.

Nếu không truy cập được, kiểm tra trạng thái frontend:

```powershell
docker compose ps frontend
docker compose logs --tail=100 frontend
```

## Bước 12. Kiểm tra backend

Mở địa chỉ:

http://localhost:8000/actuator/health

Backend hiện được cấu hình để cung cấp health endpoint tại `/actuator/health`.

Kết quả thành công thường có dạng:

```json
{
  "status": "UP"
}
```

Nếu trình duyệt không hiển thị kết quả như mong đợi, có thể kiểm tra bằng PowerShell:

```powershell
Invoke-RestMethod http://localhost:8000/actuator/health
```

Nếu backend chưa hoạt động, kiểm tra:

```powershell
docker compose ps backend
docker compose logs --tail=100 backend
```

Lưu ý: health endpoint thành công không có nghĩa là mọi API nghiệp vụ đã hoạt động. Đây chỉ là một bước kiểm tra ban đầu.

## Bước 13. Kiểm tra API documentation

Nếu dự án đã bật Swagger UI, thử mở:

http://localhost:8000/swagger-ui/index.html

Nếu trang Swagger mở được, kiểm tra các API được hiển thị.

Nếu trả về `404`, không nhất thiết backend bị lỗi: Swagger có thể chưa được bật hoặc URL có thể khác với cấu hình hiện tại. Hãy kiểm tra tài liệu API của repository.

## Bước 14. Kiểm tra database

Kiểm tra container database:

```powershell
docker compose ps db
```

Nếu service có trạng thái `healthy`, đó là dấu hiệu tốt cho thấy health check database đã thành công.

Xem log database:

```powershell
docker compose logs --tail=100 db
```

Nếu backend không kết nối được database, hãy kiểm tra:

- Database có đang chạy không.
- Các biến `DB_NAME`, `DB_USER`, `DB_PASSWORD` có khớp với cấu hình Compose không.
- Backend có dùng hostname `db` khi kết nối từ bên trong Docker hay không.
- PostgreSQL đã sẵn sàng trước khi backend thực hiện truy vấn chưa.

**Không xóa volume database để xử lý lỗi kết nối**, vì thao tác đó có thể làm mất dữ liệu local.

## Bước 15. Kiểm tra luồng hoạt động thực tế

Sau khi các service đã hoạt động, thành viên nên thực hiện một vòng kiểm thử cơ bản theo quyền truy cập của tài khoản được nhóm cung cấp:

1. Mở frontend.
2. Thử đăng nhập bằng tài khoản demo do nhóm cung cấp.
3. Kiểm tra trang chính có tải dữ liệu không.
4. Thử truy cập một chức năng mà tài khoản được phép sử dụng.
5. Kiểm tra request API trong DevTools → Network.
6. Nếu chức năng lỗi, ghi lại thông báo, thời gian và log liên quan để báo nhóm.

Không tự tạo tài khoản hoặc tự sửa dữ liệu database nếu chưa rõ quy trình seed dữ liệu của dự án.

# PHẦN VI. CÁC LẦN LÀM VIỆC TIẾP THEO

## Bước 16. Khởi động lại dự án

Sau khi đã thiết lập thành công, ở những lần làm việc sau:

1. Mở Docker Desktop.
2. Mở PowerShell.
3. Đi đến thư mục HRFlow.
4. Chạy lệnh khởi động.

```powershell
cd D:\Study\HRFLOW_demo\HRFlow
docker compose up -d
```

Đường dẫn trên là đường dẫn máy phát triển ban đầu. Thành viên khác phải thay bằng thư mục đã clone trên máy của mình.

Kiểm tra:

```powershell
docker compose ps
```

## Bước 17. Cập nhật code mới từ nhóm

Trước khi lấy code mới, kiểm tra mình đang ở nhánh nào và có thay đổi local chưa commit hay không:

```powershell
git status
git branch --show-current
```

Nếu có thay đổi chưa commit, hãy commit hoặc lưu lại theo quy trình nhóm trước khi cập nhật để tránh mất code.

Sau đó, chuyển sang nhánh được nhóm quy định và cập nhật:

```powershell
git pull
```

Lệnh này lấy và tích hợp thay đổi từ upstream của nhánh hiện tại. Nếu nhóm sử dụng quy trình Pull Request hoặc nhiều nhánh, cần làm theo hướng dẫn nhánh của nhóm thay vì tự ý lấy code từ nhánh khác.

Sau khi cập nhật, nếu code thay đổi cần build lại image:

```powershell
docker compose up -d --build
```

Kiểm tra lại:

```powershell
docker compose ps
docker compose logs --tail=100
```

**Lưu ý:** Không phải thay đổi nào cũng bắt buộc build lại tất cả image. Tuy nhiên, khi chưa quen với quy trình build riêng từng service, lệnh trên là cách đơn giản để kiểm tra và cập nhật hệ thống theo cấu hình Compose.

## Bước 18. Dừng hệ thống

Khi kết thúc buổi làm việc:

```powershell
docker compose down
```

Lệnh này dừng và xóa các container cùng network do Compose quản lý. Thông thường, named volume vẫn được giữ lại.

Khi muốn làm việc tiếp, chạy lại:

```powershell
docker compose up -d
```

**Tuyệt đối không dùng `docker compose down -v` như một lệnh dừng thông thường**, vì tùy cấu hình, lệnh đó có thể xóa volume chứa dữ liệu database local.

# PHẦN VII. XỬ LÝ LỖI THƯỜNG GẶP

## Lỗi 1. `git` không được nhận diện

**Nguyên nhân có thể:** Git chưa cài hoặc chưa được thêm vào PATH.

**Cách xử lý:**

1. Cài Git từ trang chính thức.
2. Đóng PowerShell.
3. Mở lại PowerShell.
4. Chạy `git --version`.

## Lỗi 2. `docker` không kết nối được daemon

**Nguyên nhân có thể:** Docker Desktop chưa khởi động hoàn tất hoặc Docker Engine gặp sự cố.

**Cách xử lý:**

1. Mở Docker Desktop.
2. Chờ Docker Engine sẵn sàng.
3. Chạy `docker info`.
4. Nếu vẫn lỗi, ghi lại thông báo và kiểm tra trạng thái Docker Desktop.

## Lỗi 3. Thiếu biến môi trường

Ví dụ:

```text
variable is not set
```

Hoặc Compose yêu cầu đặt `DB_PASSWORD` hay `APP_JWT_SECRET`.

**Cách xử lý:**

1. Kiểm tra có file `.env` tại thư mục chứa file Compose không.
2. Kiểm tra chính tả tên biến.
3. Kiểm tra biến được khai báo đúng theo cấu hình Compose.
4. Tạo khóa JWT Base64 hợp lệ theo Bước 6 nếu cần.

Không đưa khóa bí mật lên GitHub để khắc phục lỗi này.

## Lỗi 4. Backend liên tục restart

Chạy:

```powershell
docker compose logs --tail=150 backend
```

Đọc phần cuối log và tìm lỗi gốc, chẳng hạn:

- Lỗi cấu hình môi trường.
- Không kết nối được database.
- Lỗi migration.
- Lỗi khóa JWT hoặc Base64.
- Lỗi khởi động Spring Boot.

Không nên sửa code theo phỏng đoán khi chưa đọc log.

## Lỗi 5. Frontend không truy cập được

Chạy:

```powershell
docker compose ps frontend
docker compose logs --tail=100 frontend
```

Kiểm tra thêm:

- Docker có ánh xạ đúng cổng host không.
- Cổng 3000 có bị ứng dụng khác chiếm không.
- Frontend đã build thành công chưa.
- Backend có đang hoạt động không.

## Lỗi 6. Trang web mở được nhưng API thất bại

Mở DevTools bằng `F12` → **Network** và kiểm tra request API.

Các nguyên nhân có thể gồm:

- Backend chưa chạy.
- URL API của frontend không đúng.
- CORS chưa cho phép origin của frontend.
- API trả về lỗi xác thực hoặc phân quyền.
- Database chưa sẵn sàng hoặc backend chưa kết nối được.

Không nên kết luận mọi lỗi API đều do CORS. Hãy kiểm tra status code và log để xác định nguyên nhân.

## Lỗi 7. Cổng đã được sử dụng

Ví dụ, Docker không thể bind cổng 3000, 8000 hoặc 5432.

Kiểm tra cổng 8000 bằng PowerShell:

```powershell
Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue
```

Thay `8000` bằng cổng đang gặp lỗi.

Nếu cổng đang được ứng dụng khác sử dụng, hãy xác định ứng dụng đó trước khi dừng hoặc đổi cổng. Nếu đổi cổng host trong Compose, cần đảm bảo URL truy cập và URL API của frontend cũng phù hợp.

## Lỗi 8. Database có dữ liệu cũ hoặc migration thất bại

Trước tiên, xem log:

```powershell
docker compose logs --tail=150 db
docker compose logs --tail=150 backend
```

Không tự ý xóa volume, xóa bảng hoặc chạy migration ngược khi chưa được người phụ trách database hướng dẫn. Nếu dữ liệu local cần được giữ lại, hãy trao đổi với nhóm trước khi thực hiện bất kỳ thao tác làm sạch nào.

# PHẦN VIII. CHECKLIST HOÀN THÀNH CÀI ĐẶT

Thành viên đánh dấu từng mục sau khi thực hiện thành công.

### A. Môi trường

- [ ] Đã cài Git.
- [ ] `git --version` hoạt động.
- [ ] Đã cài Docker Desktop.
- [ ] Docker Engine đã khởi động.
- [ ] `docker --version` hoạt động.
- [ ] `docker compose version` hoạt động.
- [ ] `docker info` không báo lỗi kết nối daemon.

### B. Source code

- [ ] Đã được cấp quyền repository.
- [ ] Đã clone HRFlow thành công.
- [ ] Đã vào đúng thư mục dự án.
- [ ] Đã tìm thấy file Docker Compose.
- [ ] Đã kiểm tra file `.env` và cấu hình đúng biến môi trường.

### C. Khởi động

- [ ] `docker compose config --quiet` không báo lỗi.
- [ ] `docker compose up -d --build` chạy hoàn tất.
- [ ] Database đang chạy và health check thành công nếu đã cấu hình.
- [ ] Backend đang chạy.
- [ ] Frontend đang chạy.
- [ ] Worker đang chạy nếu được yêu cầu trong cấu hình hiện tại.
- [ ] Không có lỗi nghiêm trọng hoặc container restart liên tục trong log.

### D. Kiểm thử

- [ ] Frontend mở được.
- [ ] Backend health endpoint phản hồi.
- [ ] Đã thử đăng nhập bằng tài khoản demo.
- [ ] Đã kiểm tra ít nhất một chức năng theo quyền của tài khoản.
- [ ] Đã biết cách xem log và khởi động lại hệ thống.

# PHẦN IX. THÔNG TIN CẦN BÁO CHO NHÓM

Nếu không thể chạy dự án sau khi thực hiện các bước trên, hãy gửi cho người phụ trách:

1. Hệ điều hành và phiên bản Docker.
2. Bước đang thực hiện khi phát sinh lỗi.
3. Nội dung lỗi đầy đủ hoặc ảnh chụp màn hình.
4. Kết quả của:

```powershell
docker compose ps
```

5. Log của service liên quan, ví dụ:

```powershell
docker compose logs --tail=100 backend
```

6. Kết quả kiểm tra:

```powershell
git status
```

Không gửi file `.env`, mật khẩu, khóa JWT, GitHub token hoặc thông tin đăng nhập cá nhân.

---

## TÓM TẮT CÁC LỆNH QUAN TRỌNG

| Công việc | Lệnh |
|---|---|
| Clone repository | `git clone <REPOSITORY_URL>` |
| Vào thư mục dự án | `cd HRFlow` |
| Kiểm tra cấu hình Compose | `docker compose config --quiet` |
| Build và khởi động lần đầu | `docker compose up -d --build` |
| Khởi động những lần sau | `docker compose up -d` |
| Kiểm tra service | `docker compose ps` |
| Xem log | `docker compose logs --tail=100` |
| Theo dõi log liên tục | `docker compose logs -f` |
| Xem log backend | `docker compose logs -f backend` |
| Cập nhật code | `git pull` |
| Build lại sau khi sửa code | `docker compose up -d --build` |
| Dừng hệ thống | `docker compose down` |

**Nguyên tắc quan trọng:** Tất cả thành viên cần dùng đúng source code, đúng file cấu hình và đúng quy trình của nhóm. Khi gặp lỗi, ưu tiên đọc log và xác định nguyên nhân trước khi xóa container, xóa dữ liệu hoặc sửa cấu hình chung.
