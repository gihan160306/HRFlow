
``` mermaid
erDiagram

    PHONG_BAN {
        bigint ma_phong_ban PK
        varchar ten_phong_ban
        varchar mo_ta
    }

    NHAN_VIEN {
        bigint ma_nhan_vien PK
        bigint ma_phong_ban FK
        bigint ma_quan_ly_truc_tiep FK
        varchar ho_ten
        date ngay_sinh
        varchar gioi_tinh
        varchar email
        varchar so_dien_thoai
        varchar dia_chi
        date ngay_vao_lam
        date ngay_nghi_viec
        varchar ty_le_nghi_viec
        varchar trang_thai
    }

    HOP_DONG {
        bigint ma_hop_dong PK
        bigint ma_nhan_vien FK
        varchar loai_hop_dong
        date ngay_bat_dau
        date ngay_ket_thuc
        decimal luong_co_ban
        varchar trang_thai
    }

    NGUOI_DUNG {
        bigint ma_nguoi_dung PK
        bigint ma_nhan_vien FK
        varchar ten_dang_nhap
        varchar email
        varchar mat_khau_ma_hoa
        varchar trang_thai
        date ngay_tao
        date ngay_cap_nhat
    }

    VAI_TRO {
        bigint ma_vai_tro PK
        varchar ten_vai_tro
        varchar mo_ta
    }

    NGUOI_DUNG_VAI_TRO {
        bigint ma_nguoi_dung PK, FK
        bigint ma_vai_tro PK, FK
    }

    NHAT_KY_HE_THONG {
        bigint ma_nhat_ky PK
        bigint ma_nguoi_dung FK
        varchar hanh_dong
        varchar ten_bang
        bigint ma_thuc_the
        text gia_tri_cu
        text gia_tri_moi
        varchar dia_chi_ip
        datetime thoi_gian
    }

    THONG_BAO {
        bigint ma_thong_bao PK
        bigint ma_nguoi_dung FK
        varchar tieu_de
        text noi_dung
        varchar loai_thong_bao
        varchar kenh_thong_bao
        boolean da_doc
        datetime ngay_tao
    }

    BANG_CONG {
        bigint ma_bang_cong PK
        bigint ma_nhan_vien FK
        date ngay_lam_viec
        datetime thoi_gian_vao
        datetime thoi_gian_ra
        int so_phut_di_tre
        int so_phut_ve_som
        varchar trang_thai
        varchar anh_selfie
        varchar ma_xac_thuc_qr
    }

    DON_NGHI_PHEP {
        bigint ma_don PK
        bigint ma_nhan_vien FK
        date ngay_bat_dau
        date ngay_ket_thuc
        varchar loai_nghi
        decimal so_ngay_nghi
        varchar ly_do
        varchar trang_thai
        datetime ngay_tao
    }

    LICH_OT {
        bigint ma_lich_ot PK
        date ngay_ot
        time gio_bat_dau
        time gio_ket_thuc
        decimal so_gio_toi_da
        varchar mo_ta_cong_viec
        varchar loai_ngay
        bigint ma_phong_ban FK
        boolean da_cong_bo
        bigint nguoi_tao FK
    }

    DON_DANG_KY_OT {
        bigint ma_don PK
        bigint ma_nhan_vien FK
        bigint ma_lich_ot FK
        date ngay_ot
        decimal so_gio_ot
        varchar loai_ngay
        varchar ly_do
        varchar trang_thai
        datetime ngay_tao
    }

    NHAT_KY_PHE_DUYET {
        bigint ma_nhat_ky PK
        varchar loai_don
        bigint ma_don
        bigint nguoi_phe_duyet FK
        varchar hanh_dong
        varchar ly_do
        datetime thoi_gian
    }

    KY_LUONG {
        bigint ma_ky_luong PK
        varchar ten_ky_luong
        date ngay_bat_dau
        date ngay_ket_thuc
        varchar trang_thai
        datetime thoi_gian_phe_duyet
        datetime thoi_gian_khoa
    }

    DONG_LUONG {
        bigint ma_dong_luong PK
        bigint ma_ky_luong FK
        bigint ma_nhan_vien FK
        decimal luong_co_ban
        decimal ngay_cong
        decimal tien_ot
        decimal tong_phu_cap
        decimal luong_gross
        decimal bao_hiem_xa_hoi
        decimal bao_hiem_y_te
        decimal bao_hiem_that_nghiep
        decimal thue_tncn
        decimal tong_khau_tru
        decimal luong_net
    }

    PHIEU_LUONG {
        bigint ma_phieu_luong PK
        bigint ma_nhan_vien FK
        bigint ma_ky_luong FK
        decimal luong_gross
        decimal tien_ot
        decimal tien_phu_cap
        decimal tien_bao_hiem
        decimal tien_thue
        decimal tong_khau_tru
        decimal luong_net
        varchar trang_thai
        varchar duong_dan_pdf
        datetime ngay_tao
    }

    DIEU_CHINH_HOI_TO {
        bigint ma_dieu_chinh PK
        bigint ma_nhan_vien FK
        bigint ma_ky_luong_truoc FK
        bigint ma_ky_luong_hien_tai FK
        varchar loai_dieu_chinh
        decimal so_tien_chenh_lech
        varchar ly_do
        varchar trang_thai
        date ngay_dieu_chinh
    }

    PHU_CAP {
        bigint ma_phu_cap PK
        varchar ten_phu_cap
        decimal so_tien
        varchar trang_thai
        bigint ma_nhan_vien FK
        bigint ma_phong_ban FK
        varchar loai_phu_cap
        boolean dang_ap_dung
    }

    KHAU_TRU {
        bigint ma_khau_tru PK
        varchar ten_khau_tru
        decimal so_tien
        varchar trang_thai
        bigint ma_nhan_vien FK
        varchar loai_khau_tru
        boolean dang_ap_dung
    }

    CAU_HINH_THUE {
        bigint ma_cau_hinh_thue PK
        date ngay_hieu_luc
        int bac_thue
        decimal muc_thue
        decimal muc_giam_tru_ban_than
        decimal muc_giam_tru_nguoi_phu_thuoc
        decimal giam_tru
        varchar trang_thai
    }

    CAU_HINH_BAO_HIEM {
        bigint ma_cau_hinh_bao_hiem PK
        date ngay_hieu_luc
        decimal ty_le_bhxh
        decimal ty_le_bhyt
        decimal ty_le_bhtn
        decimal muc_luong_toi_da
    }


    PHONG_BAN ||--o{ NHAN_VIEN : "quan_ly"

    NHAN_VIEN ||--o{ HOP_DONG : "co"

    NHAN_VIEN ||--o| NGUOI_DUNG : "su_dung_tai_khoan"

    NHAN_VIEN ||--o{ NHAN_VIEN : "quan_ly_truc_tiep"


    NGUOI_DUNG ||--o{ NGUOI_DUNG_VAI_TRO : "duoc_gan"

    VAI_TRO ||--o{ NGUOI_DUNG_VAI_TRO : "duoc_gan_cho"


    NGUOI_DUNG ||--o{ NHAT_KY_HE_THONG : "thuc_hien"

    NGUOI_DUNG ||--o{ THONG_BAO : "nhan"


    NHAN_VIEN ||--o{ BANG_CONG : "co"

    NHAN_VIEN ||--o{ DON_NGHI_PHEP : "tao_don"


    PHONG_BAN ||--o{ LICH_OT : "ap_dung"

    NGUOI_DUNG ||--o{ LICH_OT : "tao"

    NHAN_VIEN ||--o{ DON_DANG_KY_OT : "dang_ky"

    LICH_OT ||--o{ DON_DANG_KY_OT : "duoc_dang_ky"


    DON_NGHI_PHEP ||--o{ NHAT_KY_PHE_DUYET : "co_lich_su"

    DON_DANG_KY_OT ||--o{ NHAT_KY_PHE_DUYET : "co_lich_su"

    NGUOI_DUNG ||--o{ NHAT_KY_PHE_DUYET : "phe_duyet"


    KY_LUONG ||--o{ DONG_LUONG : "bao_gom"

    NHAN_VIEN ||--o{ DONG_LUONG : "co"

    KY_LUONG ||--o{ PHIEU_LUONG : "phat_hanh"

    NHAN_VIEN ||--o{ PHIEU_LUONG : "nhan"


    NHAN_VIEN ||--o{ DIEU_CHINH_HOI_TO : "co_dieu_chinh"

    KY_LUONG ||--o{ DIEU_CHINH_HOI_TO : "ky_luong_truoc"

    KY_LUONG ||--o{ DIEU_CHINH_HOI_TO : "ky_luong_ap_dung"


    NHAN_VIEN ||--o{ PHU_CAP : "duoc_ap_dung"

    PHONG_BAN ||--o{ PHU_CAP : "ap_dung_theo"

    NHAN_VIEN ||--o{ KHAU_TRU : "co_khau_tru"
```