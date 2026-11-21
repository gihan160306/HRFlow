# Sơ đồ UML của báo cáo HRFlow

Mã PlantUML của các hình trong báo cáo cuối kỳ (`docs/BaoCao_CuoiKi_CNPM_HRFlow.docx`). Tên file `figX_Y_*` tương ứng **Hình X.Y** trong báo cáo. Ảnh PNG sinh ra nằm ở `png/`.

| Hình | File | Nội dung |
|---|---|---|
| 1.1 | `architecture/fig1_1_org_chart.puml` | Sơ đồ tổ chức HNQ Media |
| 2.1 | `architecture/fig2_1_client_server_jwt.puml` | Client–server với JWT |
| 3.1–3.4 | `usecase/fig3_*.puml` | Use Case tổng quát, chấm công–nghỉ phép, OT–thu nhập, lương–phiếu lương |
| 4.1–4.2 | `architecture/fig4_1_*`, `fig4_2_*` | Kiến trúc tổng thể, phân lớp backend |
| 4.3–4.6 | `erd/fig4_*.puml` | ERD (sinh tự động từ `docs/database_design.md` bằng `gen_erd.py`) |
| 4.7–4.9 | `class/fig4_*.puml` | Sơ đồ lớp: nhân sự, chấm công–nghỉ phép–OT, lương–phiếu lương |
| 4.10–4.14 | `sequence/fig4_*.puml` | Tuần tự: đăng nhập JWT, chấm công tại quầy (QR cá nhân + ảnh), duyệt nghỉ phép, chốt công–tính lương, duyệt–khóa–phiếu lương |
| 4.15–4.16 | `activity/fig4_*.puml` | Hoạt động: chấm công, quy trình lương tháng |
| 4.17–4.19 | `state/fig4_*.puml` | Trạng thái: kỳ lương, đơn nghỉ/OT, hợp đồng |
| 4.20 | `architecture/fig4_20_navigation.puml` | Điều hướng màn hình theo role |

Các file cũ (`usecase/uc*.puml`, `class/class_*.puml`, `state/state_*.puml`) là sơ đồ chi tiết theo phân hệ, vẫn giữ nguyên.

## Cách vẽ lại

```powershell
python docs/uml/gen_erd.py        # sinh lại 4 file ERD từ database_design.md
python docs/uml/render.py         # render mọi fig*.puml -> docs/uml/png
```

`render.py` dùng `tools/plantuml.jar` (hoặc biến môi trường `PLANTUML_JAR`) nếu có; nếu không sẽ gọi server `plantuml.com` (nội dung sơ đồ được gửi lên server đó). `_style.puml` là style dùng chung, được nhúng qua `!include`.
