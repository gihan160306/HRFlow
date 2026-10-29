# Ma trận quyền nghiệp vụ HRFlow

Tài liệu này quy định **quyền nghiệp vụ cần có** của HRFlow tại HNQ Media. Đây là baseline để viết SRS, Use Case, API, thiết kế dữ liệu và test case. `company_profile.md` là nguồn cho cơ cấu/chức danh/tuyến PTO-OT; `payroll_rules.md` là nguồn cho công thức payroll và thẩm quyền chuẩn bị/duyệt/khóa. Tài liệu này không giả định rằng các quyền đã được triển khai đầy đủ trong code hoặc migration; mọi khác biệt kỹ thuật sẽ được ghi trong `database_design.md`/tài liệu đối chiếu khi bắt đầu thiết kế.

## 1. Nguyên tắc phân quyền

- Hệ thống có đúng 4 role truy cập nền tảng: `Employee`, `Manager`, `HR`, `Admin`. Chức danh Director, Operations Manager, Finance Manager, HR Lead, Sales Manager và các Lead không phải role thứ năm; chức danh, tuyến quản lý, tuyến phê duyệt và các grant nghiệp vụ phải được lưu/cấu hình riêng. Không suy quyền đặc biệt chỉ từ tên chức danh hoặc role nền tảng.
- `Employee` truy cập dữ liệu cá nhân được phép. `Manager` truy cập dữ liệu cá nhân và dữ liệu vận hành của cấp dưới trực tiếp theo nhiệm vụ; không mặc nhiên xem lương cấp dưới. `HR` truy cập dữ liệu nhân sự toàn công ty cần thiết cho nghiệp vụ; quyền xem/tính/duyệt lương vẫn phải theo phân quyền payroll. `Admin` quản trị tài khoản, role nền tảng và cấu hình kỹ thuật; không mặc nhiên xem lương, duyệt lương hoặc khóa kỳ lương.
- `employee_id` phải lấy từ token/session và quan hệ quản lý trong DB, không tin giá trị client gửi.
- Không ai được tự duyệt đơn do mình tạo. PTO/OT dùng một cấp duyệt; người dự phòng phải được cấu hình trước và quyết định phải ghi `approval_log`.
- Lương, thông tin định danh và tài khoản ngân hàng chỉ trả về đúng phạm vi quyền. Kỳ lương `LOCKED` chỉ đọc; sai sót dùng `payroll_adjustment`.
- `audit_log` và `payroll_audit` là append-only; không có API sửa/xóa và phải chặn `UPDATE/DELETE` ở DB.

Ký hiệu: `✓` = được thực hiện trong phạm vi; `own` = dữ liệu của mình; `team` = nhân viên thuộc phạm vi quản lý trực tiếp; `R` = chỉ đọc; `—` = không được phép; `grant` = chỉ được phép khi đã được cấp rõ cho đúng tài khoản và phạm vi. Bảng này là quyền tối đa theo nghiệp vụ; hệ thống vẫn phải kiểm tra chủ sở hữu bản ghi, phạm vi quản lý, trạng thái tài khoản và xung đột lợi ích.

## 2. Role hệ thống và chức danh nghiệp vụ

| Role hệ thống | Người dùng áp dụng tại HNQ | Phạm vi chính |
|---|---|---|
| `Employee` | Nhân viên Content, Design & Video, Digital Marketing, Account, Sales và nhân sự khác | Hồ sơ được phép của mình, chấm công, tạo PTO/OT, xem công và phiếu lương của mình |
| `Manager` | Các Lead, Sales Manager, Operations Manager, Finance Manager và Director/người được bổ nhiệm quản lý trực tiếp | Quyền cá nhân và quyền quản lý đúng tuyến; phê duyệt PTO/OT theo tuyến được cấu hình; không tự có quyền payroll |
| `HR` | HR Executive/HR Lead | Hồ sơ, hợp đồng, công, phép, OT, dữ liệu đầu vào và xử lý lương theo quyền được cấp |
| `Admin` | Người quản trị kỹ thuật ngoài 40 nhân sự | Tài khoản, role, cấu hình và audit kỹ thuật; không mặc nhiên có quyền xem hay duyệt lương |

Director sử dụng role nền tảng `Manager`; chức danh Director không tự cấp quyền truy cập toàn bộ dữ liệu hoặc quyền payroll. HR Lead duyệt PTO/OT của Director theo tuyến một cấp. Finance Manager xác nhận tiền thực nhận/đối soát; Sales Manager xác nhận Sales owner/KPI. Company Profile quy định Giám đốc phê duyệt bảng lương (`APPROVE`), Finance Manager khóa kỳ (`LOCK`) sau phê duyệt; grant cấp riêng cho đúng tài khoản, không cấp chung theo role.

## 3. Ma trận quyền theo nghiệp vụ

### 3.1. Tài khoản và hồ sơ nhân sự

| Nghiệp vụ | Employee | Manager | HR | Admin | Dữ liệu nghiệp vụ liên quan |
|---|---:|---:|---:|---:|---|
| Đăng nhập, đăng xuất, đổi mật khẩu, xem tài khoản mình | own | own | own | own | Tài khoản hệ thống |
| Tạo hồ sơ onboarding, yêu cầu khởi tạo tài khoản | — | — | ✓ | — | HR tạo hồ sơ; Admin khởi tạo tài khoản theo yêu cầu hợp lệ |
| Xem danh sách nhân viên | — | team, trường cơ bản | ✓ | Định danh tối thiểu để quản lý tài khoản | Hồ sơ nhân sự, phòng ban |
| Xem hồ sơ chi tiết | own | own/team, dữ liệu team không gồm lương nhạy cảm | ✓ theo nghiệp vụ | Chỉ định danh tối thiểu | Hồ sơ nhân sự |
| Cập nhật hồ sơ cá nhân được phép | own, không trường nhạy cảm | own | own; công việc HR có audit | — | Hồ sơ nhân sự, audit |
| Cập nhật hồ sơ nhân viên | — | — | ✓, audit | — | Hồ sơ nhân sự, audit |
| Cho nghỉ việc và lưu lý do | — | — | ✓ | — | Hồ sơ nghỉ việc |
| Xem danh mục phòng ban | R | R | R | R (thông tin tối thiểu) | Phòng ban |
| Quyết định thành lập/điều chỉnh/giải thể phòng ban | — | Riêng Director theo thẩm quyền | — | — | Quyết định cơ cấu của Giám đốc theo quy chế công ty |
| Cập nhật danh mục phòng ban | — | — | ✓ theo quyết định hợp lệ của Giám đốc | Chỉ hỗ trợ cấu hình kỹ thuật | Phòng ban, ngày hiệu lực và hồ sơ nhân sự liên quan |
| Xem hợp đồng của mình | own | own | own; company theo nhiệm vụ | — | Hợp đồng |
| Tạo/sửa/gia hạn hợp đồng | — | — | ✓ theo căn cứ hợp lệ | — | Hợp đồng |
| Quản lý phụ cấp/khấu trừ | R own | R own | ✓ theo nhiệm vụ và căn cứ | — | Khoản thu nhập |
| Quản lý tài khoản, khóa/mở, role | — | — | — | ✓ | Tài khoản, role nền tảng, trạng thái |

### 3.2. Chấm công, nghỉ phép và OT

| Nghiệp vụ | Employee | Manager | HR | Admin | Dữ liệu nghiệp vụ liên quan |
|---|---:|---:|---:|---:|---|
| Check-in/check-out | own | own | own | — | Bản ghi chấm công |
| Xem bảng công | own | own/team | own/company theo nhiệm vụ | — | Bản ghi chấm công |
| Xác minh/chỉnh công | — | team, được ủy quyền | ✓ | — | Bản ghi chấm công và lịch sử điều chỉnh |
| Lưu nháp, gửi/hủy PTO khi còn `PENDING`; yêu cầu hủy/điều chỉnh đơn đã duyệt | own | own | own | — | Đơn nghỉ phép; hủy/điều chỉnh đơn đã duyệt phải được xử lý hợp lệ, không sửa lịch sử |
| Xem PTO và số dư phép | own | own/team | own/company theo nhiệm vụ | — | Đơn nghỉ phép/quỹ phép |
| Duyệt/từ chối PTO | — | Theo tuyến duyệt Mục 4 Company Profile | HR Lead/HR Executive chỉ khi thuộc tuyến hợp lệ | — | Không suy quyền duyệt chỉ từ team/role |
| Tạo và công bố lịch OT | — | team | ✓ | — | Lịch OT |
| Xem lịch OT đã công bố | đúng đối tượng | own/team đủ điều kiện | own/company theo nhiệm vụ | — | Lịch/đăng ký OT |
| Đăng ký OT | own | own | own | — | Đăng ký OT |
| Duyệt/từ chối đăng ký OT | — | Theo tuyến duyệt Mục 4 Company Profile | HR Lead/HR Executive chỉ khi thuộc tuyến hợp lệ | — | Lịch/đăng ký OT |
| Xác nhận giờ OT thực tế và phần vượt giờ đã duyệt | — | team, người quản lý có thẩm quyền xác nhận | ✓ kiểm tra, đối chiếu | — | Giờ thực tế, minh chứng và kết quả xác nhận theo Mục 7.1 Company Profile |
| Xem lịch sử phê duyệt | own | own/team và đơn được giao duyệt | own/company theo nhiệm vụ | Chỉ audit kỹ thuật đã lọc | Lịch sử phê duyệt |
| Cấp QR chấm công | — | — | ✓ | — | Chính sách QR chấm công |

Tuyến duyệt PTO/OT bắt buộc bám `company_profile.md`:

| Người gửi đơn | Người duyệt chính | Người dự phòng đã được chỉ định trước |
|---|---|---|
| Nhân viên Content/Design/Digital/Account | Lead trực tiếp | Operations Manager |
| Nhân viên Sales | Sales Manager | Director |
| Nhân viên HR | HR Lead | Director |
| Lead Content/Design/Digital/Account | Operations Manager | Director |
| Sales Manager, Operations Manager, Finance Manager | Director | Không có dự phòng mặc định; nếu Director không thể xử lý thì đơn chờ quyết định, không tự chuyển cho HR Lead |
| HR Lead | Director | Finance Manager được Director chỉ định trước |
| Director | HR Lead | HR Executive được HR Lead chỉ định trước |

Người duyệt xác định theo đúng bảng tuyến tại Company Profile; không tự chuyển mọi đơn thiếu Manager cho HR Lead. Chỉ người dự phòng nêu trong cấu hình hợp lệ mới được thay người duyệt chính; phải ghi người thực hiện, lý do thay thế và thời gian vào lịch sử phê duyệt. Nếu người duyệt chính và người dự phòng đều không thể xử lý, đơn giữ trạng thái chờ. Không tự động chọn người khác và không được tự duyệt đơn của chính mình.

### 3.3. Sales, doanh thu, KPI và hoa hồng

| Nghiệp vụ | Sales Executive | Sales Manager | Finance Manager | HR | Admin | Dữ liệu nghiệp vụ liên quan |
|---|---:|---:|---:|---:|---:|---|
| Xem/chỉnh doanh thu do mình phụ trách | own | team | R xác nhận tiền | — | — | Bảng đối soát doanh thu |
| Nhập khoản thu/chứng từ | own, nháp | ✓ kiểm tra | ✓ xác nhận đã thu | — | — | Bảng đối soát doanh thu |
| Xác nhận Sales owner và điều kiện dịch vụ/ nghiệm thu | — | ✓ | — | — | — | Bảng đối soát doanh thu |
| Xác nhận tiền thực nhận | — | — | ✓ | — | — | Chứng từ/đối soát tài chính |
| Xác định Sales sở hữu, loại trùng/hoàn tiền | — | ✓ | ✓ phối hợp | — | — | Bảng đối soát doanh thu chưa đủ trường |
| Tính/xem KPI, hoa hồng | own kết quả | ✓ team | R đối soát | ✓ đưa khoản xác nhận vào lương | — | KPI/hoa hồng đã xác nhận |
| Xác nhận dữ liệu đủ điều kiện chuyển sang lương | — | ✓ phần Sales owner/điều kiện | ✓ phần tiền thực nhận | ✓ tiếp nhận dữ liệu đã xác nhận | — | Bảng đối soát doanh thu và đầu vào payroll |

Chỉ doanh thu dịch vụ đã nghiệm thu và thực nhận, sau chiết khấu, không gồm VAT/chi hộ quảng cáo/hoàn tiền, mới được chuyển sang KPI và hoa hồng. Không tự động chia hoa hồng cho hai Sales.

### 3.4. Payroll và phiếu lương

| Nghiệp vụ | Employee | Manager | HR | Admin | Điều kiện bắt buộc |
|---|---:|---:|---:|---:|---|
| Xem/tải phiếu lương cá nhân | own | own | own; phiếu người khác theo nhiệm vụ/grant | — mặc định | Phiếu lương cá nhân không đòi grant VIEW của người khác |
| Tạo kỳ lương | — | — | `PREPARE` | — | Kỳ mới ở trạng thái `DRAFT` |
| Chuẩn bị/kiểm tra đầu vào và tính lương | — | — | `PREPARE` | — | Nguồn công, phép, OT, phụ cấp, KPI/hoa hồng phải được xác nhận; lỗi dữ liệu không được âm thầm đổi thành 0 |
| Xem bảng lương chi tiết toàn kỳ | — | — | `VIEW` theo nhiệm vụ | — mặc định | Cần grant `VIEW`; chỉ xem trường cần thiết cho nhiệm vụ |
| Đối soát tài chính | — | Finance Manager theo nhiệm vụ | HR phối hợp theo nhiệm vụ | — | Finance Manager mang role Manager; đối soát không đồng nghĩa với `APPROVE`/`LOCK` |
| Phê duyệt bảng lương | — | Director có `APPROVE` được cấp riêng | — mặc định | — mặc định | Giám đốc phê duyệt sau đối soát; không cấp chung cho role Manager; áp dụng kiểm soát xung đột lợi ích |
| Khóa kỳ lương | — | Finance Manager có `LOCK` được cấp riêng | — mặc định | — mặc định | Finance Manager khóa sau phê duyệt; `LOCK` độc lập với `APPROVE` |
| Tạo đề xuất adjustment sau khóa | — | — | `PREPARE` | — | Có kỳ gốc, lý do, chứng từ, nguồn và liên kết đến kỳ chi trả; không sửa kỳ đã khóa |
| Duyệt adjustment | — | `APPROVE` đúng ủy quyền | — mặc định | — mặc định | Người tạo không tự duyệt |
| Quản lý tham số nghiệp vụ payroll | — | — | Đề xuất/xác nhận nghiệp vụ | Chỉ cấu hình kỹ thuật | Giá trị phải có nguồn, ngày hiệu lực, người xác nhận và audit; Admin không tự quyết định chính sách lương/thuế/BH |

**Quy tắc cấp quyền payroll:** `VIEW`, `PREPARE`, `APPROVE` và `LOCK` độc lập; role hoặc chức danh không tự cấp grant. HR chuẩn bị/tính/rà soát, Finance Manager đối soát tài chính. Giám đốc được cấp `APPROVE` để phê duyệt; Finance Manager được cấp `LOCK` để khóa sau phê duyệt. Mức lương của Giám đốc và trường hợp xung đột cần quyết định riêng của cấp có thẩm quyền; phương án xử lý kỳ lương có lương Giám đốc vẫn phải được HNQ xác nhận, không tự duyệt phần lương của mình hay tự lập tuyến thay thế. Người lập không tự duyệt bảng lương.

### 3.5. Thông báo, báo cáo và audit

| Nghiệp vụ | Employee | Manager | HR | Admin | Dữ liệu nghiệp vụ liên quan |
|---|---:|---:|---:|---:|---|
| Xem/đánh dấu thông báo | own | own | own | own | Thông báo |
| Cấu hình nhận email | own | own | own | own | Cấu hình nhận thông báo |
| Dashboard nhân sự | — | theo phạm vi team | ✓ | ✓ kỹ thuật, không gồm lương nhạy cảm mặc định | Chỉ hiển thị trường theo quyền |
| Dashboard/báo cáo lương | — | `VIEW` đúng ủy quyền | `VIEW` theo nhiệm vụ | — mặc định | Cần grant payroll `VIEW`; không lộ lương qua tổng hợp/dashboard |
| Xuất báo cáo nhân sự/vận hành | — | team nếu bật | ✓ | ✓ theo quyền kỹ thuật | Lọc trường theo role và scope |
| Xuất báo cáo lương | — | `VIEW` đúng ủy quyền | `VIEW` theo nhiệm vụ | — mặc định | Grant payroll `VIEW`, audit lượt xuất |
| Xem audit log | — | — | R | R | Audit log |
| Sửa/xóa audit log hoặc payroll audit | — | — | — | — | Audit log append-only |

## 4. Quy tắc kiểm soát và ca kiểm thử bắt buộc

1. Thiếu/hết token trả `401`; đúng role nhưng ngoài phạm vi dữ liệu trả `403` hoặc `404` theo một quy ước thống nhất, không làm lộ bản ghi.
2. Không tin `employee_id`, `manager_id`, role hoặc quyền gửi từ body/query/path. Server xác định người dùng từ phiên đăng nhập và kiểm tra quan hệ trong dữ liệu đã lưu.
3. Người gửi không được approve/reject đơn do mình tạo, kể cả có role HR/Manager/Admin hoặc được cấu hình là người dự phòng.
4. Manager không mặc nhiên xem lương team nếu thiếu grant `VIEW` phù hợp; Admin không mặc định xem phiếu lương, bảng lương, tài khoản ngân hàng hoặc thông tin thuế.
5. Thay đổi hồ sơ, hợp đồng, công đã chốt, khoản thu nhập, tham số payroll, quyền và phê duyệt phải ghi người thực hiện, thời gian, bản ghi, hành động, lý do và giá trị trước/sau khi phù hợp.
6. Kỳ lương chỉ chuyển `DRAFT → CALCULATED → APPROVED → LOCKED`; chỉ đúng grant mới được chuyển trạng thái. `LOCKED` không update/delete; sai sót đi qua adjustment ở kỳ sau.
7. Công, phép, OT, doanh thu Sales, phụ cấp, thưởng và khấu trừ phải có nguồn hợp lệ, trạng thái xác nhận và mã nguồn duy nhất trước khi vào lương.
8. Khi người duyệt có xung đột lợi ích, phải dùng người thay thế được chỉ định trước. Không có người thay thế hợp lệ thì giữ trạng thái chờ.
9. Mọi quyền đặc biệt và ủy quyền phải có người cấp, người nhận, phạm vi, ngày bắt đầu/kết thúc, lý do và trạng thái hiệu lực; thu hồi quyền phải có hiệu lực ngay theo cấu hình.
10. Tài khoản bị vô hiệu hóa/nhân viên đã nghỉ việc không được tiếp tục truy cập hệ thống, nhưng lịch sử nghiệp vụ và audit phải được bảo toàn.

| Mã kiểm thử | Tình huống | Kết quả mong đợi |
|---|---|---|
| PM-01 | API không có token | `401` |
| PM-02 | Employee xem danh sách hoặc lương người khác | `403`/`404`, không lộ dữ liệu |
| PM-03 | Manager xem ngoài team hoặc duyệt ngoài tuyến | `403`/`404` |
| PM-04 | Người gửi tự approve/reject PTO/OT | `403`; trạng thái đơn không đổi |
| PM-05 | Manager/Admin gọi API payroll không có grant | `403` |
| PM-06 | HR thử approve/lock chỉ với role HR, không có grant | `403` |
| PM-07 | Người có grant APPROVE nhưng không có LOCK khóa kỳ | `403` |
| PM-08 | Người lập bảng lương tự duyệt bảng đó | `403`; ghi audit/security event |
| PM-09 | Giám đốc phê duyệt kỳ lương có phần lương của chính mình khi chưa có biện pháp xử lý xung đột được HNQ xác nhận | Chặn phê duyệt kỳ đang có xung đột, yêu cầu quyết định hợp lệ; không tự duyệt phần lương của mình, không tự chuyển duyệt từng phiếu |
| PM-10 | Sửa kỳ/phiếu lương `LOCKED` | `409`, DB không đổi; yêu cầu adjustment |
| PM-11 | HR sửa hồ sơ đúng quyền | Thành công và có audit |
| PM-12 | Update/delete audit log | Bị chặn |
| PM-13 | Sales ghi doanh thu chưa nghiệm thu/chưa thực nhận | Không vào KPI/hoa hồng |
| PM-14 | Hai Sales nhận cùng khoản thu | Chỉ một owner sau xác nhận; không tính trùng |
| PM-15 | Gửi `employee_id` người khác khi tạo PTO/OT | Server bỏ qua giá trị client hoặc trả `422` |
| PM-16 | Người duyệt chính vắng mặt | Chỉ người dự phòng đã chỉ định trước được duyệt; lưu căn cứ thay thế |
| PM-17 | Người duyệt chính và dự phòng đều không thể xử lý | Đơn chờ; không tự chọn người khác |
| PM-18 | Tài khoản terminated đăng nhập | `401`/`403`; lịch sử vẫn còn |
| PM-19 | Admin mở dashboard/report lương | Không có dữ liệu lương nếu chưa được cấp `VIEW` |
| PM-20 | Quyền/grant hết hiệu lực hoặc đã thu hồi | Từ chối thao tác ngay theo thời điểm hiệu lực |

## 5. Yêu cầu bàn giao sang tài liệu kỹ thuật

- SRS/Use Case phải mô tả từng hành động và điều kiện quyền, đặc biệt scope `own/team/company`, tuyến duyệt PTO/OT, grant payroll, tự duyệt và xung đột lợi ích.
- `database_design.md` phải lưu đủ: role nền tảng; chức danh; quan hệ quản lý; tuyến duyệt chính/dự phòng; grant payroll theo tài khoản, phạm vi, người cấp và thời hạn; audit/approval log; trạng thái kỳ lương.
- OpenAPI phải nêu lỗi `401/403/404/409/422`, quyền yêu cầu và trường dữ liệu trả về; không cho client tự truyền quyền hoặc người thực hiện quyết định.
- Migration/seed chỉ là cách triển khai. Nếu seed hiện có quyền mẫu trái với baseline này, phải sửa seed/migration hoặc cấu hình sau khi được phê duyệt; không coi dữ liệu demo là quyết định nghiệp vụ.
- Test plan cần bao phủ toàn bộ PM-01 đến PM-20, gồm kiểm tra quyền trực tiếp qua API/service và kiểm tra dữ liệu nhạy cảm trong response/export.

## 6. Trạng thái tài liệu

Ma trận này là baseline nghiệp vụ phiên bản 1.0 cho việc viết SRS và thiết kế kỹ thuật. Các bảng dữ liệu, tên migration, endpoint và mã lỗi cụ thể sẽ được chốt trong tài liệu kỹ thuật tiếp theo; chúng không được dùng để âm thầm thay đổi quy tắc nghiệp vụ tại đây.
