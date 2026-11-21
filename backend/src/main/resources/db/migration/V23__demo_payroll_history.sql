-- V23: Du lieu demo LICH SU LUONG 3 ky 08, 09, 10/2026 de xem phieu luong.
-- Migration nay chi seed DU LIEU NGUON (cong, nghi, OT, thu nhap, doanh thu, bien dong nhan su).
-- Viec chot cong -> tinh luong -> doi soat -> duyet -> khoa -> tao phieu PDF -> ghi nhan chi tra do
-- DemoPayrollHistorySeeder (profile dev) chay qua chinh PayrollService/PayrollEngine, nen moi con so tren phieu
-- deu do engine tinh, khong ghi tay.
--
-- Cac tinh huong luong co trong 3 ky:
--   * Di lam du thang, di tre/ve som (tru phut), quen check-out da duoc HR dieu chinh, cong tac.
--   * Nghi phep nam (ca ngay / nua ngay), nghi viec rieng huong luong (ket hon, tang le),
--     nghi khong luong, nghi om (huong che do BHXH, cong ty khong tra), vang khong phep.
--   * OT ngay thuong 150%, cuoi tuan 200%, ngay le 2/9 300%, OT co gio dem (22:00-06:00), OT phat sinh.
--   * Hoa hong Sales 2% + thuong KPI (dat/khong dat, chi tieu theo ty le cho nguoi vao giua thang), doanh thu co hoan tien.
--   * Thuong le 2/9, thuong nong, thuong hieu qua Q3 (Giam doc len bac thue cao), khoan bo sung khi nghi viec.
--   * Khau tru co can cu, boi thuong, thu hoi tam ung, hoan chi ngoai Gross.
--   * Tang luong giua thang (tach doan luong), them nguoi phu thuoc, nhan vien moi vao giua thang,
--     thu viec/thuc tap (khau tru 10%), part-time, nghi viec giua thang 10.
--   * Truy linh sau khoa ky (payroll_adjustment GROSS/NET) do seeder tao sau khi ky 08 da khoa.

-- =====================================================================
-- 0. Quyet dinh cua HR/Finance cho cac diem mo (mo phong)
-- =====================================================================
-- OPEN-04: chot phan loai thue phu cap. An trua <= 730.000/thang khong chiu thue; gui xe, dien thoai tra tien mat chiu thue.
UPDATE allowance_type
SET tax_class = CASE WHEN code IN ('LUNCH', 'LUNCH_PT') THEN 'NON_TAXABLE' ELSE 'TAXABLE' END,
    description = description || ' – đã chốt phân loại thuế (mô phỏng)'
WHERE code IN ('LUNCH', 'PARKING', 'PHONE', 'LUNCH_PT', 'PARKING_PT') AND tax_class = 'NEEDS_REVIEW';

-- OPEN-02: xac nhan khau tru 10% cho HDLD < 3 thang/thuc tap, ap dung cho cac lan chi tra den 05/11/2026 (ky 10).
--          Ky 11 tro di van dung ban NEEDS_REVIEW de demo diem mo.
INSERT INTO payroll_parameter (code, name, category, value, unit, region, effective_from, effective_to, source_ref, status, confirmed_by, confirmed_at, param_version)
SELECT code, name, category, value, unit, region, effective_from, DATE '2026-11-05',
       source_ref || ' – HR/Finance xác nhận cho kỳ 08–10/2026', 'VERIFIED_SIM', 'HR Lead + Finance Manager (mô phỏng)', DATE '2026-08-28', param_version + 1
FROM payroll_parameter
WHERE code IN ('PIT_FLAT_10_THRESHOLD', 'PIT_FLAT_10_RATE') AND status = 'NEEDS_REVIEW' AND effective_from = DATE '2026-07-01'
ON CONFLICT DO NOTHING;

-- OPEN-03 / OPEN-02 cho part-time, thu viec, thuc tap: HR xac nhan ho so cho ky 08–10; tu 11/2026 tro ve NEEDS_REVIEW.
CREATE TEMP TABLE demo_split_ins ON COMMIT DROP AS
SELECT * FROM insurance_profile WHERE status = 'NEEDS_REVIEW' AND effective_from <= DATE '2026-10-31' AND effective_to IS NULL;
UPDATE insurance_profile SET effective_from = DATE '2026-11-01' WHERE id IN (SELECT id FROM demo_split_ins);
INSERT INTO insurance_profile (employee_id, effective_from, effective_to, bhxh, bhyt, bhtn, insurance_salary, status, verified_by, verified_at, note)
SELECT employee_id, effective_from, DATE '2026-10-31', bhxh, bhyt, bhtn, insurance_salary, 'CONFIRMED', 4, TIMESTAMPTZ '2026-08-28 10:00:00+07',
       'HR xác nhận đối tượng/mức đóng cho kỳ 08–10/2026 (demo)'
FROM demo_split_ins;

CREATE TEMP TABLE demo_split_tax ON COMMIT DROP AS
SELECT * FROM tax_profile WHERE status = 'NEEDS_REVIEW' AND effective_from <= DATE '2026-10-31' AND effective_to IS NULL;
UPDATE tax_profile SET effective_from = DATE '2026-11-06' WHERE id IN (SELECT id FROM demo_split_tax);
INSERT INTO tax_profile (employee_id, effective_from, effective_to, residency, method, tax_code, commitment_08, status, verified_by, verified_at, note)
SELECT employee_id, effective_from, DATE '2026-11-05', residency, method, tax_code, commitment_08, 'CONFIRMED', 4, TIMESTAMPTZ '2026-08-28 10:00:00+07',
       'HR xác nhận khấu trừ 10% cho các lần chi trả đến 05/11/2026 (demo)'
FROM demo_split_tax;

CREATE TEMP TABLE demo_split_al ON COMMIT DROP AS
SELECT * FROM employee_allowance WHERE status = 'NEEDS_REVIEW' AND effective_from <= DATE '2026-10-31' AND effective_to IS NULL;
UPDATE employee_allowance SET effective_from = DATE '2026-11-01' WHERE id IN (SELECT id FROM demo_split_al);
INSERT INTO employee_allowance (employee_id, allowance_type_id, amount, effective_from, effective_to, decision_ref, status, created_by)
SELECT employee_id, allowance_type_id, amount, effective_from, DATE '2026-10-31', decision_ref, 'CONFIRMED', 4
FROM demo_split_al;

-- =====================================================================
-- 1. Bien dong nhan su trong 3 thang
-- =====================================================================
-- 1a. NV009 tang luong 10tr -> 12tr tu 16/09/2026 (ky 09 tach 2 doan luong); muc dong BH doi tu 01/10.
UPDATE salary_record SET effective_to = DATE '2026-09-15'
WHERE employee_id = (SELECT id FROM employee WHERE code = 'NV009') AND effective_to IS NULL;
INSERT INTO salary_record (employee_id, contract_id, base_salary, effective_from, reason, decision_ref, created_by)
SELECT e.id, c.id, 12000000, DATE '2026-09-16', 'Tăng lương sau đánh giá giữa năm', 'QD-TL-NV009-2026-09', 4
FROM employee e JOIN contract c ON c.contract_no = 'HD-NV009-01' WHERE e.code = 'NV009';
UPDATE insurance_profile SET effective_to = DATE '2026-09-30'
WHERE employee_id = (SELECT id FROM employee WHERE code = 'NV009') AND effective_to IS NULL;
INSERT INTO insurance_profile (employee_id, effective_from, bhxh, bhyt, bhtn, insurance_salary, status, verified_by, verified_at, note)
SELECT id, DATE '2026-10-01', TRUE, TRUE, TRUE, 12000000, 'CONFIRMED', 4, TIMESTAMPTZ '2026-09-25 10:00:00+07', 'Điều chỉnh mức đóng theo lương mới'
FROM employee WHERE code = 'NV009';

-- 1b. Nguoi phu thuoc moi: NV009 co con tu 09/2026; NV035 dang ky me tu 10/2026 (giam tru gia canh tang).
INSERT INTO dependent (employee_id, full_name, relationship, valid_from, doc_ref, verified)
SELECT id, 'Nguyễn Thanh An', 'CHILD', DATE '2026-09-01', 'NPT-NV009-01', TRUE FROM employee WHERE code = 'NV009'
UNION ALL
SELECT id, 'Phạm Thị Hòa', 'PARENT', DATE '2026-10-01', 'NPT-NV035-01', TRUE FROM employee WHERE code = 'NV035';

-- 1c. NV050 nghi viec, ngay lam viec cuoi 23/10/2026 (tai khoan giu den 31/12 de xem phieu luong).
UPDATE employee SET status = 'TERMINATED', termination_date = DATE '2026-10-23',
       termination_reason = 'Nghỉ việc theo nguyện vọng cá nhân (đã báo trước 30 ngày)'
WHERE code = 'NV050';
UPDATE contract SET status = 'TERMINATED', termination_date = DATE '2026-10-23', termination_reason = 'Chấm dứt theo đơn xin nghỉ việc'
WHERE contract_no = 'HD-NV050-01';
UPDATE user_account SET disable_at = DATE '2026-12-31'
WHERE employee_id = (SELECT id FROM employee WHERE code = 'NV050');

-- =====================================================================
-- 2. Ky luong 08, 09, 10/2026 (ngay chi tra 05 thang sau, roi vao T7/CN thi tra ngay lam viec lien truoc)
-- =====================================================================
INSERT INTO payroll_period (period_year, period_month, start_date, end_date, pay_date, status, payment_status, created_by) VALUES
    (2026, 8,  DATE '2026-08-01', DATE '2026-08-31', DATE '2026-09-04', 'DRAFT', 'UNPAID', 4),
    (2026, 9,  DATE '2026-09-01', DATE '2026-09-30', DATE '2026-10-05', 'DRAFT', 'UNPAID', 4),
    (2026, 10, DATE '2026-10-01', DATE '2026-10-31', DATE '2026-11-05', 'DRAFT', 'UNPAID', 4)
ON CONFLICT (period_year, period_month) DO NOTHING;

-- Danh dau cho DemoPayrollHistorySeeder chay quy trinh tinh/duyet/khoa (mot lan).
INSERT INTO background_job (job_type, payload, dedupe_key, status, run_after)
VALUES ('DEMO_PAYROLL_HISTORY', '{"months":[8,9,10],"year":2026}', 'demo:payroll-history:2026-08-10', 'PENDING', now())
ON CONFLICT (dedupe_key) DO NOTHING;

-- =====================================================================
-- 3. Nghi phep da duyet
-- =====================================================================
CREATE TEMP TABLE demo_leave (
    code   VARCHAR(20),
    ltype  VARCHAR(30),
    unit   VARCHAR(30),
    d_from DATE,
    d_to   DATE,
    reason VARCHAR(300),
    evidence VARCHAR(200)
) ON COMMIT DROP;

INSERT INTO demo_leave VALUES
    -- Thang 8
    ('NV010', 'ANNUAL',        'FULL_DAY',    DATE '2026-08-10', DATE '2026-08-12', 'Du lịch cùng gia đình', NULL),
    ('NV017', 'SICK',          'FULL_DAY',    DATE '2026-08-18', DATE '2026-08-19', 'Sốt xuất huyết, điều trị ngoại trú', 'Giấy ra viện BV Nhiệt Đới'),
    ('NV025', 'UNPAID',        'FULL_DAY',    DATE '2026-08-24', DATE '2026-08-28', 'Về quê lo việc gia đình', NULL),
    ('NV031', 'ANNUAL',        'HALF_DAY_PM', DATE '2026-08-14', DATE '2026-08-14', 'Đưa con đi khám', NULL),
    ('NV013', 'ANNUAL',        'FULL_DAY',    DATE '2026-08-21', DATE '2026-08-21', 'Việc cá nhân', NULL),
    ('NV062', 'ANNUAL',        'FULL_DAY',    DATE '2026-08-31', DATE '2026-08-31', 'Nghỉ nối lễ Quốc khánh', NULL),
    -- Thang 9
    ('NV005', 'ANNUAL',        'FULL_DAY',    DATE '2026-09-03', DATE '2026-09-04', 'Nghỉ nối lễ Quốc khánh', NULL),
    ('NV019', 'PERSONAL_PAID', 'FULL_DAY',    DATE '2026-09-21', DATE '2026-09-23', 'Kết hôn', 'Giấy chứng nhận kết hôn'),
    ('NV061', 'UNPAID',        'FULL_DAY',    DATE '2026-09-28', DATE '2026-09-29', 'Việc riêng (đã hết phép)', NULL),
    ('NV036', 'ANNUAL',        'HALF_DAY_AM', DATE '2026-09-17', DATE '2026-09-17', 'Làm thủ tục giấy tờ', NULL),
    ('NV045', 'SICK',          'FULL_DAY',    DATE '2026-09-08', DATE '2026-09-10', 'Viêm phổi', 'Giấy nghỉ hưởng BHXH'),
    ('NV080', 'ANNUAL',        'FULL_DAY',    DATE '2026-09-14', DATE '2026-09-14', 'Việc cá nhân', NULL),
    -- Thang 10
    ('NV012', 'ANNUAL',        'FULL_DAY',    DATE '2026-10-19', DATE '2026-10-23', 'Du lịch Đà Lạt', NULL),
    ('NV027', 'UNPAID',        'FULL_DAY',    DATE '2026-10-26', DATE '2026-10-30', 'Chăm người thân nằm viện', NULL),
    ('NV033', 'SICK',          'FULL_DAY',    DATE '2026-10-07', DATE '2026-10-07', 'Cảm sốt', 'Giấy khám bệnh'),
    ('NV009', 'ANNUAL',        'HALF_DAY_AM', DATE '2026-10-09', DATE '2026-10-09', 'Tiêm chủng cho con', NULL),
    ('NV050', 'ANNUAL',        'FULL_DAY',    DATE '2026-10-12', DATE '2026-10-13', 'Nghỉ phép trước khi nghỉ việc', NULL),
    ('NV075', 'PERSONAL_PAID', 'FULL_DAY',    DATE '2026-10-15', DATE '2026-10-16', 'Tang lễ ông nội', 'Giấy báo tử'),
    ('NV096', 'ANNUAL',        'HALF_DAY_PM', DATE '2026-10-28', DATE '2026-10-28', 'Việc cá nhân', NULL);

INSERT INTO leave_request (employee_id, leave_type_id, kind, unit, start_date, end_date, duration_minutes, duration_days, reason, evidence,
                           status, primary_approver_id, backup_approver_id, submitted_at, decided_by, decided_at, decision_reason)
SELECT e.id, lt.id, 'NEW', l.unit, l.d_from, l.d_to, 0, 0, l.reason, l.evidence, 'APPROVED',
       COALESCE(ar.primary_approver_id, e.manager_id), ar.backup_approver_id,
       (l.d_from - 5)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '9 hours',
       COALESCE(ar.primary_approver_id, e.manager_id),
       (l.d_from - 4)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '10 hours',
       'Đồng ý'
FROM demo_leave l
JOIN employee e ON e.code = l.code
JOIN leave_type lt ON lt.code = l.ltype
LEFT JOIN LATERAL (SELECT * FROM approval_route r WHERE r.requester_id = e.id ORDER BY r.effective_from DESC LIMIT 1) ar ON TRUE;

-- Tung ngay nghi: bo T7/CN va ngay le; phut theo lich (FT sang 210 / chieu 270, PT 240).
INSERT INTO leave_request_day (leave_request_id, leave_date, minutes, day_fraction)
SELECT r.id, d::date,
       CASE WHEN ws.code = 'PT_MORNING' THEN 240
            WHEN r.unit = 'HALF_DAY_AM' THEN 210
            WHEN r.unit = 'HALF_DAY_PM' THEN 270
            ELSE 480 END,
       CASE WHEN r.unit IN ('HALF_DAY_AM', 'HALF_DAY_PM') THEN 0.5 ELSE 1 END
FROM leave_request r
JOIN demo_leave l ON l.code = (SELECT code FROM employee WHERE id = r.employee_id) AND l.d_from = r.start_date
JOIN employee_schedule es ON es.employee_id = r.employee_id
JOIN work_schedule ws ON ws.id = es.schedule_id
CROSS JOIN LATERAL generate_series(r.start_date, r.end_date, INTERVAL '1 day') d
WHERE EXTRACT(ISODOW FROM d) <= 5
  AND d::date NOT IN (SELECT holiday_date FROM holiday WHERE paid);

UPDATE leave_request r SET duration_minutes = x.m, duration_days = x.f
FROM (SELECT leave_request_id, SUM(minutes) AS m, SUM(day_fraction) AS f FROM leave_request_day GROUP BY 1) x
WHERE x.leave_request_id = r.id AND r.duration_days = 0
  AND r.start_date BETWEEN DATE '2026-08-01' AND DATE '2026-10-31';

INSERT INTO approval_log (entity_type, entity_id, action, actor_account_id, actor_name, from_status, to_status, reason)
SELECT 'LEAVE_REQUEST', r.id, 'APPROVE', a.id, ap.full_name, 'PENDING', 'APPROVED', 'Đồng ý'
FROM leave_request r
JOIN demo_leave l ON l.code = (SELECT code FROM employee WHERE id = r.employee_id) AND l.d_from = r.start_date
JOIN employee ap ON ap.id = r.decided_by
JOIN user_account a ON a.employee_id = ap.id;

-- Tru quy phep nam cho don ANNUAL
INSERT INTO leave_balance_txn (balance_id, txn_type, delta_days, leave_request_id, reason, created_by)
SELECT b.id, 'USED', -r.duration_days, r.id, 'Duyệt đơn nghỉ', 4
FROM leave_request r
JOIN demo_leave l ON l.code = (SELECT code FROM employee WHERE id = r.employee_id) AND l.d_from = r.start_date AND l.ltype = 'ANNUAL'
JOIN leave_balance b ON b.employee_id = r.employee_id AND b.balance_year = 2026;
UPDATE leave_balance b SET used_days = used_days + x.days
FROM (SELECT r.employee_id, SUM(r.duration_days) AS days
      FROM leave_request r JOIN demo_leave l ON l.code = (SELECT code FROM employee WHERE id = r.employee_id) AND l.d_from = r.start_date
      WHERE l.ltype = 'ANNUAL' GROUP BY 1) x
WHERE b.employee_id = x.employee_id AND b.balance_year = 2026;

-- =====================================================================
-- 4. Cham cong tai quay 08–10/2026
-- =====================================================================
-- Ngoai le: vang khong phep, cong tac, quen check-out (HR xac nhan thu cong).
CREATE TEMP TABLE demo_att_special (
    code   VARCHAR(20),
    d      DATE,
    kind   VARCHAR(20),
    note   VARCHAR(300)
) ON COMMIT DROP;
INSERT INTO demo_att_special VALUES
    ('NV072', DATE '2026-09-18', 'ABSENCE',  'Vắng không phép, không liên lạc được - đã lập biên bản'),
    ('NV083', DATE '2026-10-09', 'ABSENCE',  'Vắng không phép'),
    ('NV030', DATE '2026-08-26', 'TRIP',     'Công tác Bình Dương - nghiệm thu chiến dịch khách hàng'),
    ('NV030', DATE '2026-08-27', 'TRIP',     'Công tác Bình Dương - nghiệm thu chiến dịch khách hàng'),
    ('NV032', DATE '2026-10-21', 'TRIP',     'Công tác Hà Nội - pitching khách hàng mới'),
    ('NV032', DATE '2026-10-22', 'TRIP',     'Công tác Hà Nội - pitching khách hàng mới'),
    ('NV011', DATE '2026-08-13', 'MANUAL',   'Quên check-out, Lead xác nhận làm đến 17:40'),
    ('NV068', DATE '2026-09-24', 'MANUAL',   'Máy quầy lỗi khi check-in, HR điều chỉnh theo camera'),
    ('NV094', DATE '2026-10-14', 'MANUAL',   'Quên check-out, Sales Manager xác nhận');

WITH days AS (
    SELECT d::date AS work_date FROM generate_series(DATE '2026-08-01', DATE '2026-10-31', INTERVAL '1 day') d
    WHERE EXTRACT(ISODOW FROM d) <= 5 AND d::date NOT IN (SELECT holiday_date FROM holiday WHERE paid)
),
emp AS (
    SELECT e.id, e.code, e.hire_date, COALESCE(e.termination_date, DATE '9999-12-31') AS term, ws.code = 'PT_MORNING' AS pt
    FROM employee e
    JOIN employee_schedule es ON es.employee_id = e.id
    JOIN work_schedule ws ON ws.id = es.schedule_id
),
lv AS (
    SELECT r.employee_id, ld.leave_date, r.unit
    FROM leave_request r JOIN leave_request_day ld ON ld.leave_request_id = r.id
    WHERE r.status = 'APPROVED' AND r.kind = 'NEW'
),
base AS (
    SELECT e.id, e.code, e.pt, d.work_date, l.unit AS leave_unit, s.kind, s.note,
           abs(hashtext(e.code || d.work_date::text)) % 1000 AS r1,
           abs(hashtext(d.work_date::text || e.code)) % 1000 AS r2
    FROM emp e
    JOIN days d ON d.work_date BETWEEN e.hire_date AND e.term
    LEFT JOIN lv l ON l.employee_id = e.id AND l.leave_date = d.work_date
    LEFT JOIN demo_att_special s ON s.code = e.code AND s.d = d.work_date
    WHERE l.unit IS NULL OR l.unit IN ('HALF_DAY_AM', 'HALF_DAY_PM')
),
-- Khoang lam viec hieu luc (phut tu 00:00): sang ms-me, chieu as_-ae; nghi nua ngay thi bo khoang do.
iv AS (
    SELECT b.*,
           CASE WHEN b.pt THEN 510 WHEN b.leave_unit = 'HALF_DAY_AM' THEN 720 ELSE 510 END AS ms,
           CASE WHEN b.pt THEN 750 ELSE 720 END AS me,
           CASE WHEN b.pt THEN 750 WHEN b.leave_unit = 'HALF_DAY_PM' THEN 1050 ELSE 780 END AS as_,
           CASE WHEN b.pt THEN 750 ELSE 1050 END AS ae,
           CASE WHEN b.pt THEN 240 ELSE 480 END AS sched
    FROM base b
),
tm AS (
    SELECT iv.*,
           -- gio vao: ~5% di tre 5–39 phut, con lai 08:05–08:29 (hoac 12:45–12:59 neu nghi sang)
           CASE WHEN iv.kind IN ('ABSENCE', 'TRIP') THEN NULL
                WHEN iv.leave_unit = 'HALF_DAY_AM' THEN 765 + iv.r1 % 15
                WHEN iv.r1 < 50 THEN 515 + iv.r1 % 35
                ELSE 485 + iv.r1 % 25 END AS in_m,
           -- gio ra: ~3% ve som, con lai sau gio ket thuc 1–45 phut
           CASE WHEN iv.kind IN ('ABSENCE', 'TRIP') THEN NULL
                WHEN iv.kind = 'MANUAL' THEN (CASE WHEN iv.pt THEN 755 ELSE 1060 END)
                WHEN iv.pt THEN (CASE WHEN iv.r2 < 30 THEN 730 + iv.r2 % 15 ELSE 751 + iv.r2 % 20 END)
                WHEN iv.leave_unit = 'HALF_DAY_PM' THEN 720 + iv.r2 % 20
                WHEN iv.r2 < 30 THEN 1020 + iv.r2 % 25
                ELSE 1051 + iv.r2 % 45 END AS out_m
    FROM iv
)
INSERT INTO attendance_record (employee_id, work_date, check_in_at, check_out_at, check_in_point_id, check_out_point_id,
                               check_in_kiosk_id, check_out_kiosk_id, method, scheduled_minutes, worked_minutes, late_minutes,
                               early_minutes, status, review_reason, confirmed_by, confirmed_at, note)
SELECT t.id, t.work_date,
       CASE WHEN t.in_m IS NOT NULL THEN (t.work_date::timestamp + t.in_m * INTERVAL '1 minute') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
       CASE WHEN t.out_m IS NOT NULL THEN (t.work_date::timestamp + t.out_m * INTERVAL '1 minute') AT TIME ZONE 'Asia/Ho_Chi_Minh' END,
       CASE WHEN t.kind IS NULL THEN 1 END,
       CASE WHEN t.kind IS NULL THEN 1 END,
       CASE WHEN t.kind IS NULL THEN (SELECT id FROM user_account WHERE role = 'KIOSK' ORDER BY id LIMIT 1) END,
       CASE WHEN t.kind IS NULL THEN (SELECT id FROM user_account WHERE role = 'KIOSK' ORDER BY id LIMIT 1) END,
       CASE t.kind WHEN 'ABSENCE' THEN 'ABSENCE' WHEN 'TRIP' THEN 'BUSINESS_TRIP' WHEN 'MANUAL' THEN 'MANUAL' ELSE 'QR' END,
       t.sched,
       CASE WHEN t.kind = 'ABSENCE' THEN 0
            WHEN t.kind = 'TRIP' THEN (t.me - t.ms) + (t.ae - t.as_)
            ELSE GREATEST(0, LEAST(t.out_m, t.me) - GREATEST(t.in_m, t.ms)) + GREATEST(0, LEAST(t.out_m, t.ae) - GREATEST(t.in_m, t.as_)) END,
       CASE WHEN t.in_m IS NULL THEN 0
            ELSE GREATEST(0, LEAST(t.in_m, t.me) - t.ms) + GREATEST(0, LEAST(t.in_m, t.ae) - t.as_) END,
       CASE WHEN t.out_m IS NULL THEN 0
            ELSE GREATEST(0, t.me - GREATEST(t.out_m, t.ms)) + GREATEST(0, t.ae - GREATEST(t.out_m, t.as_)) END,
       CASE t.kind WHEN 'ABSENCE' THEN 'ABSENT_CONFIRMED' WHEN 'TRIP' THEN 'CONFIRMED' WHEN 'MANUAL' THEN 'CONFIRMED' ELSE 'VALID' END,
       NULL,
       CASE WHEN t.kind IS NOT NULL THEN 4 END,
       CASE WHEN t.kind IS NOT NULL THEN (t.work_date + 1)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '9 hours' END,
       t.note
FROM tm t
ON CONFLICT (employee_id, work_date) DO NOTHING;

-- Yeu cau dieu chinh cong da duyet cho cac ngay HR xac nhan thu cong
INSERT INTO attendance_adjustment (attendance_id, employee_id, work_date, proposed_check_in, proposed_check_out, method, reason, evidence,
                                   status, requested_by, decided_by, decided_at, decision_note)
SELECT ar.id, ar.employee_id, ar.work_date, ar.check_in_at, ar.check_out_at, ar.method, s.note,
       CASE s.kind WHEN 'TRIP' THEN 'Quyết định cử đi công tác' ELSE 'Xác nhận của quản lý trực tiếp' END,
       'APPROVED', ua.id, 4, ar.confirmed_at, 'Đã đối chiếu'
FROM demo_att_special s
JOIN employee e ON e.code = s.code
JOIN attendance_record ar ON ar.employee_id = e.id AND ar.work_date = s.d
JOIN user_account ua ON ua.employee_id = e.id
WHERE s.kind IN ('TRIP', 'MANUAL');

-- =====================================================================
-- 5. Lam them gio
-- =====================================================================
CREATE TEMP TABLE demo_ot (
    key        VARCHAR(20),
    title      VARCHAR(200),
    work_date  DATE,
    t_start    TIME,
    t_end      TIME,
    dept       VARCHAR(20),
    codes      TEXT[],
    short_code VARCHAR(20),  -- nhan vien lam it hon so gio dang ky
    short_min  INTEGER
) ON COMMIT DROP;
INSERT INTO demo_ot VALUES
    ('OT08A', 'Dựng video TVC chiến dịch Back-to-school', DATE '2026-08-20', TIME '18:00', TIME '21:00', 'DESIGN',
     ARRAY['NV016','NV017','NV018','NV058','NV059'], 'NV059', 120),
    ('OT08B', 'Chạy quảng cáo flash sale 8.8 (cuối tuần)', DATE '2026-08-08', TIME '09:00', TIME '13:00', 'DIGITAL',
     ARRAY['NV023','NV024','NV068'], NULL, NULL),
    ('OT09A', 'Trực livestream sự kiện lễ Quốc khánh', DATE '2026-09-02', TIME '09:00', TIME '13:00', 'DIGITAL',
     ARRAY['NV022','NV023','NV024','NV025','NV026'], NULL, NULL),
    ('OT09B', 'Hoàn thiện bộ bài đăng ra mắt sản phẩm (thứ Bảy)', DATE '2026-09-12', TIME '08:30', TIME '12:30', 'CONTENT',
     ARRAY['NV008','NV009','NV010','NV044','NV045'], 'NV044', 180),
    ('OT10A', 'Sự kiện roadshow khách hàng (thứ Bảy)', DATE '2026-10-03', TIME '08:00', TIME '12:00', 'SALES',
     ARRAY['NV035','NV036','NV037','NV090','NV091'], NULL, NULL),
    ('OT10B', 'Go-live chiến dịch 20/10 - trực xuyên đêm', DATE '2026-10-19', TIME '19:00', TIME '23:30', 'DIGITAL',
     ARRAY['NV023','NV024','NV017','NV018'], NULL, NULL),
    ('OT10C', 'Chốt hồ sơ thầu khách hàng FMCG', DATE '2026-10-27', TIME '18:00', TIME '20:30', 'ACCOUNT',
     ARRAY['NV029','NV030','NV031','NV081'], NULL, NULL);

INSERT INTO ot_schedule (title, work_date, start_time, end_time, description, department_id, status, created_by, published_at)
SELECT o.title, o.work_date, o.t_start, o.t_end, '[' || o.key || '] Kế hoạch OT đã thông báo trước', d.id, 'CLOSED',
       COALESCE((SELECT ua.id FROM user_account ua JOIN employee m ON m.id = ua.employee_id
                 WHERE m.department_id = d.id AND m.manager_id IN (1, 2) ORDER BY m.id LIMIT 1), 4),
       (o.work_date - 3)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '9 hours'
FROM demo_ot o JOIN department d ON d.code = o.dept;

INSERT INTO ot_schedule_target (schedule_id, employee_id)
SELECT s.id, e.id
FROM demo_ot o
JOIN ot_schedule s ON s.description LIKE '[' || o.key || ']%'
CROSS JOIN LATERAL unnest(o.codes) c(code)
JOIN employee e ON e.code = c.code;

INSERT INTO ot_registration (schedule_id, employee_id, consent, registered_start, registered_end, registered_minutes, status, approved_minutes,
                             primary_approver_id, backup_approver_id, decided_by, decided_at, decision_reason,
                             actual_status, actual_start, actual_end, actual_minutes, actual_confirmed_by, actual_confirmed_at, actual_evidence,
                             hr_checked, hr_checked_by, hr_checked_at)
SELECT s.id, e.id, TRUE,
       (o.work_date + o.t_start) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       (o.work_date + o.t_end) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       x.reg, 'APPROVED', x.reg,
       COALESCE(ar.primary_approver_id, e.manager_id), ar.backup_approver_id, COALESCE(ar.primary_approver_id, e.manager_id),
       (o.work_date - 2)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '15 hours', 'Đồng ý',
       'CONFIRMED',
       (o.work_date + o.t_start) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       (o.work_date + o.t_start) AT TIME ZONE 'Asia/Ho_Chi_Minh' + x.act * INTERVAL '1 minute',
       x.act, COALESCE(ar.primary_approver_id, e.manager_id),
       (o.work_date + 1)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '10 hours',
       'Đối chiếu chấm công tại quầy + báo cáo công việc',
       TRUE, 4, (o.work_date + 2)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '10 hours'
FROM demo_ot o
JOIN ot_schedule s ON s.description LIKE '[' || o.key || ']%'
CROSS JOIN LATERAL unnest(o.codes) c(code)
JOIN employee e ON e.code = c.code
LEFT JOIN LATERAL (SELECT * FROM approval_route r WHERE r.requester_id = e.id ORDER BY r.effective_from DESC LIMIT 1) ar ON TRUE
CROSS JOIN LATERAL (SELECT (EXTRACT(EPOCH FROM (o.t_end - o.t_start)) / 60)::int AS reg,
                           CASE WHEN c.code = o.short_code THEN o.short_min ELSE (EXTRACT(EPOCH FROM (o.t_end - o.t_start)) / 60)::int END AS act) x;

-- OT phat sinh (khong co ke hoach truoc), HR xac minh
INSERT INTO ot_unscheduled (employee_id, work_date, start_at, end_at, minutes, description, evidence, status, created_by, verified_by, verified_at, verify_note)
SELECT e.id, v.d, (v.d + v.s) AT TIME ZONE 'Asia/Ho_Chi_Minh', (v.d + v.t) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       (EXTRACT(EPOCH FROM (v.t - v.s)) / 60)::int, v.descr, v.evd, 'CONFIRMED', 4, 4,
       (v.d + 1)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '11 hours', 'Đã xác minh với quản lý'
FROM (VALUES
    ('NV030', DATE '2026-10-14', TIME '18:00', TIME '20:00', 'Xử lý sự cố quảng cáo khách hàng bị từ chối hiển thị', 'Email khách hàng + log hệ thống'),
    ('NV020', DATE '2026-09-25', TIME '17:45', TIME '19:45', 'Sửa gấp key visual theo phản hồi khách', 'Tin nhắn duyệt của Design Lead')
) AS v(code, d, s, t, descr, evd)
JOIN employee e ON e.code = v.code;

-- =====================================================================
-- 6. Thu nhap khac / khau tru / hoan chi (da xac nhan)
-- =====================================================================
CREATE TEMP TABLE demo_income (
    code    VARCHAR(20),
    m       INTEGER,
    itype   VARCHAR(30),
    name    VARCHAR(200),
    amount  NUMERIC(15,0),
    tclass  VARCHAR(30),
    ref     VARCHAR(100),
    evd     VARCHAR(300)
) ON COMMIT DROP;
INSERT INTO demo_income VALUES
    -- Thang 8
    ('NV035', 8,  'BONUS',            'Thưởng ký hợp đồng khách hàng lớn',          3000000, 'TAXABLE',     'DEMO-2026-08-BON-NV035', 'QĐ thưởng QDT-2026-08-01'),
    ('NV030', 8,  'REIMBURSEMENT',    'Hoàn chi taxi, vé xe đi công tác',            450000,  'NON_TAXABLE', 'DEMO-2026-08-RMB-NV030', 'Hóa đơn GTGT + đề nghị thanh toán'),
    ('NV061', 8,  'ADVANCE_RECOVERY', 'Thu hồi tạm ứng lương tháng 7',               2000000, 'NON_TAXABLE', 'DEMO-2026-08-ADV-NV061', 'Phiếu chi tạm ứng PC-0712'),
    ('NV018', 8,  'BONUS',            'Thưởng nóng hoàn thành TVC trước hạn',        1000000, 'TAXABLE',     'DEMO-2026-08-BON-NV018', 'QĐ thưởng QDT-2026-08-02'),
    -- Thang 9
    ('NV072', 9,  'DEDUCTION',        'Khấu trừ phí đồng phục theo thỏa thuận',      300000,  'TAXABLE',     'DEMO-2026-09-DED-NV072', 'Biên bản thỏa thuận ký 01/09'),
    ('NV064', 9,  'COMPENSATION',     'Bồi thường làm hỏng ống kính máy ảnh',        2500000, 'TAXABLE',     'DEMO-2026-09-CMP-NV064', 'Biên bản xử lý trách nhiệm vật chất'),
    ('NV034', 9,  'REIMBURSEMENT',    'Hoàn chi tiếp khách',                         1200000, 'NON_TAXABLE', 'DEMO-2026-09-RMB-NV034', 'Hóa đơn nhà hàng'),
    ('NV024', 9,  'OTHER_ADDITION',   'Phụ cấp kiêm nhiệm đào tạo nhân viên mới',    800000,  'TAXABLE',     'DEMO-2026-09-ADD-NV024', 'QĐ kiêm nhiệm KN-2026-03'),
    ('NV083', 9,  'ADVANCE_RECOVERY', 'Thu hồi tạm ứng mua laptop (kỳ 1/2)',         1500000, 'NON_TAXABLE', 'DEMO-2026-09-ADV-NV083', 'Thỏa thuận tạm ứng TU-2026-09'),
    -- Thang 10
    ('NV001', 10, 'BONUS',            'Thưởng hiệu quả kinh doanh quý III',          20000000,'TAXABLE',     'DEMO-2026-10-Q3-NV001',  'NQ HĐTV về thưởng quý III'),
    ('NV002', 10, 'BONUS',            'Thưởng hiệu quả kinh doanh quý III',          8000000, 'TAXABLE',     'DEMO-2026-10-Q3-NV002',  'QĐ thưởng quý III'),
    ('NV003', 10, 'BONUS',            'Thưởng hiệu quả kinh doanh quý III',          8000000, 'TAXABLE',     'DEMO-2026-10-Q3-NV003',  'QĐ thưởng quý III'),
    ('NV034', 10, 'BONUS',            'Thưởng vượt doanh số quý III',                10000000,'TAXABLE',     'DEMO-2026-10-Q3-NV034',  'QĐ thưởng quý III'),
    ('NV008', 10, 'BONUS',            'Thưởng hiệu quả quý III (Lead)',              5000000, 'TAXABLE',     'DEMO-2026-10-Q3-NV008',  'QĐ thưởng quý III'),
    ('NV016', 10, 'BONUS',            'Thưởng hiệu quả quý III (Lead)',              5000000, 'TAXABLE',     'DEMO-2026-10-Q3-NV016',  'QĐ thưởng quý III'),
    ('NV022', 10, 'BONUS',            'Thưởng hiệu quả quý III (Lead)',              5000000, 'TAXABLE',     'DEMO-2026-10-Q3-NV022',  'QĐ thưởng quý III'),
    ('NV029', 10, 'BONUS',            'Thưởng hiệu quả quý III (Lead)',              5000000, 'TAXABLE',     'DEMO-2026-10-Q3-NV029',  'QĐ thưởng quý III'),
    ('NV024', 10, 'BONUS',            'Thưởng chiến dịch 20/10 xuất sắc',            2000000, 'TAXABLE',     'DEMO-2026-10-BON-NV024', 'QĐ thưởng QDT-2026-10-05'),
    ('NV083', 10, 'ADVANCE_RECOVERY', 'Thu hồi tạm ứng mua laptop (kỳ 2/2)',         1500000, 'NON_TAXABLE', 'DEMO-2026-10-ADV-NV083', 'Thỏa thuận tạm ứng TU-2026-09'),
    ('NV050', 10, 'OTHER_ADDITION',   'Thanh toán tiền phép năm chưa nghỉ (3 ngày)', 1363636, 'TAXABLE',     'DEMO-2026-10-PN-NV050',  'Biên bản thanh lý HĐLĐ'),
    ('NV050', 10, 'ADVANCE_RECOVERY', 'Thu hồi tạm ứng còn lại khi nghỉ việc',       500000,  'NON_TAXABLE', 'DEMO-2026-10-ADV-NV050', 'Biên bản thanh lý HĐLĐ'),
    ('NV032', 10, 'REIMBURSEMENT',    'Hoàn chi vé máy bay, khách sạn công tác Hà Nội', 4850000, 'NON_TAXABLE', 'DEMO-2026-10-RMB-NV032', 'Hóa đơn + quyết định cử đi công tác');

-- Thuong le Quoc khanh 2/9 cho nhan su chinh thuc (full-time) dang lam viec trong thang 9
INSERT INTO demo_income
SELECT e.code, 9, 'BONUS', 'Thưởng lễ Quốc khánh 2/9', 1000000, 'TAXABLE', 'DEMO-2026-09-QK-' || e.code, 'QĐ thưởng lễ QDT-2026-09-01'
FROM employee e
WHERE e.employee_type = 'FULL_TIME' AND e.hire_date <= DATE '2026-09-01';

INSERT INTO income_item (employee_id, period_year, period_month, item_type, name, amount, tax_class, source_ref, evidence, decision_ref,
                         status, created_by, confirmed_by, confirmed_at)
SELECT e.id, 2026, i.m, i.itype, i.name, i.amount, i.tclass, i.ref, i.evd, i.evd, 'CONFIRMED', 5, 4,
       make_date(2026, i.m, 25)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '16 hours'
FROM demo_income i JOIN employee e ON e.code = i.code
ON CONFLICT (source_ref) DO NOTHING;

-- =====================================================================
-- 7. Doanh thu Sales da xac nhan (hoa hong 2%, KPI 1.500.000 khi dat >= 100tr theo lich giu vi tri)
-- =====================================================================
CREATE TEMP TABLE demo_sales (
    code    VARCHAR(20),
    m       INTEGER,
    seq     INTEGER,
    contract VARCHAR(50),
    amount  NUMERIC(15,0),
    refund  NUMERIC(15,0)
) ON COMMIT DROP;
INSERT INTO demo_sales VALUES
    ('NV035', 8,  1, 'HD-KH-ALPHA-01',   85000000, 0),
    ('NV035', 8,  2, 'HD-KH-BETA-02',    50000000, 0),
    ('NV036', 8,  1, 'HD-KH-GAMMA-01',   80000000, 0),
    ('NV037', 8,  1, 'HD-KH-DELTA-01',  102000000, 0),
    ('NV038', 8,  1, 'HD-KH-EPS-01',     45000000, 0),
    ('NV035', 9,  1, 'HD-KH-ALPHA-01',   60000000, 0),
    ('NV036', 9,  1, 'HD-KH-ZETA-01',   125000000, 0),
    ('NV037', 9,  1, 'HD-KH-ETA-01',     50000000, 10000000),
    ('NV039', 9,  1, 'HD-KH-THETA-01',   98000000, 0),
    ('NV035', 10, 1, 'HD-KH-IOTA-01',   150000000, 0),
    ('NV038', 10, 1, 'HD-KH-KAPPA-01',  110000000, 0),
    ('NV039', 10, 1, 'HD-KH-LAMBDA-01',  30000000, 0),
    ('NV040', 10, 1, 'HD-KH-MU-01',      60000000, 0);   -- thu viec tu 15/10: chi tieu theo ty le ~54,5tr -> dat KPI

-- Sales Executive con lai (NV090–NV100): doanh so 20–160tr/thang, mot so thang khong co doanh thu
INSERT INTO demo_sales
SELECT e.code, m, 1, 'HD-KH-' || e.code || '-' || m,
       ((20 + abs(hashtext(e.code || m::text)) % 141) * 1000000)::numeric, 0
FROM employee e CROSS JOIN generate_series(8, 10) m
WHERE e.code BETWEEN 'NV090' AND 'NV100' AND abs(hashtext(m::text || e.code)) % 5 <> 0;

INSERT INTO sales_revenue (period_year, period_month, owner_employee_id, contract_code, receipt_code, payment_amount, eligible_revenue,
                           payment_date, acceptance_date, refund_amount, status, owner_confirmed_by, owner_confirmed_at,
                           finance_confirmed_by, finance_confirmed_at, created_by, note)
SELECT 2026, s.m, e.id, s.contract, 'PT-2026' || lpad(s.m::text, 2, '0') || '-' || s.code || '-' || s.seq,
       s.amount, s.amount - s.refund,
       make_date(2026, s.m, 10 + s.seq * 5), make_date(2026, s.m, 8 + s.seq * 5), s.refund, 'CONFIRMED',
       34, make_date(2026, s.m, 26)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '10 hours',
       3,  make_date(2026, s.m, 27)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '10 hours',
       5, CASE WHEN s.refund > 0 THEN 'Khách hoàn một phần do giảm phạm vi dịch vụ' END
FROM demo_sales s JOIN employee e ON e.code = s.code
ON CONFLICT (receipt_code) DO NOTHING;
