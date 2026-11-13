-- V19: Du lieu tham chieu (database_design.md Muc 8).
-- Gia tri la du lieu MO PHONG co ghi nguon; tham so chua doi chieu co status NEEDS_REVIEW.

-- 1. Phong ban (company_profile.md Muc 2)
INSERT INTO department (id, code, name, description) VALUES
    (1, 'MGMT',    'Management',        'Điều hành, vận hành, ngân sách và kiểm soát tài chính'),
    (2, 'HR',      'Human Resources',   'Hồ sơ, hợp đồng lao động, chấm công, lương và chế độ'),
    (3, 'CONTENT', 'Content',           'Nội dung quảng cáo, bài đăng, SEO content'),
    (4, 'DESIGN',  'Design & Video',    'Thiết kế đồ họa, video và ấn phẩm'),
    (5, 'DIGITAL', 'Digital Marketing', 'Quảng cáo, phân tích dữ liệu, SEO'),
    (6, 'ACCOUNT', 'Account',           'Chăm sóc khách, hợp đồng dịch vụ, nghiệm thu'),
    (7, 'SALES',   'Sales',             'Tìm khách hàng, doanh số và hợp đồng mới');

-- 2. Chuc danh (company_profile.md Muc 8.2)
INSERT INTO job_title (id, code, name, department_id, managerial, sales_owner_eligible, default_base_salary) VALUES
    (1,  'DIRECTOR',      'Giám đốc',                1, TRUE,  FALSE, 30000000),
    (2,  'OPS_MANAGER',   'Operations Manager',      1, TRUE,  FALSE, 20000000),
    (3,  'FIN_MANAGER',   'Finance Manager',         1, TRUE,  FALSE, 20000000),
    (4,  'HR_LEAD',       'HR Lead',                 2, TRUE,  FALSE, 16000000),
    (5,  'HR_EXEC',       'HR Executive',            2, FALSE, FALSE, 11000000),
    (6,  'CONTENT_LEAD',  'Content Lead',            3, TRUE,  FALSE, 16000000),
    (7,  'CONTENT_EXEC',  'Content Executive',       3, FALSE, FALSE, 10000000),
    (8,  'DESIGN_LEAD',   'Design Lead',             4, TRUE,  FALSE, 16000000),
    (9,  'DESIGNER',      'Designer / Video Editor', 4, FALSE, FALSE, 11000000),
    (10, 'DIGITAL_LEAD',  'Digital Lead',            5, TRUE,  FALSE, 16000000),
    (11, 'SEO_ADS',       'SEO / Ads Specialist',    5, FALSE, FALSE, 12000000),
    (12, 'ACCOUNT_LEAD',  'Account Lead',            6, TRUE,  FALSE, 16000000),
    (13, 'ACCOUNT_EXEC',  'Account Executive',       6, FALSE, FALSE, 11000000),
    (14, 'SALES_MANAGER', 'Sales Manager',           7, TRUE,  FALSE, 18000000),
    (15, 'SALES_EXEC',    'Sales Executive',         7, FALSE, TRUE,  9000000);

-- 3. Lich lam viec (company_profile.md Muc 5.1); day_of_week 1 = thu Hai ... 7 = Chu Nhat
INSERT INTO work_schedule (id, code, name, schedule_type, description) VALUES
    (1, 'FT_STD',     'Hành chính full-time',   'FULL_TIME', 'T2–T6, 08:30–12:00 và 13:00–17:30 (8 giờ/ngày, 40 giờ/tuần)'),
    (2, 'PT_MORNING', 'Part-time buổi sáng',    'PART_TIME', 'T2–T6, 08:30–12:30 (4 giờ/ngày, 20 giờ/tuần)');

INSERT INTO work_schedule_day (schedule_id, day_of_week, morning_start, morning_end, afternoon_start, afternoon_end)
SELECT 1, d, TIME '08:30', TIME '12:00', TIME '13:00', TIME '17:30' FROM generate_series(1, 5) AS d
UNION ALL
SELECT 2, d, TIME '08:30', TIME '12:30', NULL, NULL FROM generate_series(1, 5) AS d;

-- 4. Ngay le 2026 (mo phong, can doi chieu thong bao nghi le chinh thuc truoc khi van hanh)
INSERT INTO holiday (holiday_date, name, paid, note) VALUES
    (DATE '2026-01-01', 'Tết Dương lịch',               TRUE, 'Mô phỏng'),
    (DATE '2026-02-16', 'Tết Nguyên đán',               TRUE, 'Mô phỏng – 5 ngày nghỉ Tết'),
    (DATE '2026-02-17', 'Tết Nguyên đán',               TRUE, 'Mô phỏng – 5 ngày nghỉ Tết'),
    (DATE '2026-02-18', 'Tết Nguyên đán',               TRUE, 'Mô phỏng – 5 ngày nghỉ Tết'),
    (DATE '2026-02-19', 'Tết Nguyên đán',               TRUE, 'Mô phỏng – 5 ngày nghỉ Tết'),
    (DATE '2026-02-20', 'Tết Nguyên đán',               TRUE, 'Mô phỏng – 5 ngày nghỉ Tết'),
    (DATE '2026-04-27', 'Nghỉ bù Giỗ Tổ Hùng Vương',    TRUE, 'Mô phỏng – 10/3 âm lịch rơi vào Chủ Nhật 26/04'),
    (DATE '2026-04-30', 'Ngày Chiến thắng 30/4',        TRUE, 'Mô phỏng'),
    (DATE '2026-05-01', 'Ngày Quốc tế Lao động',        TRUE, 'Mô phỏng'),
    (DATE '2026-09-01', 'Quốc khánh (ngày liền kề)',    TRUE, 'Mô phỏng – cần đối chiếu ngày liền kề'),
    (DATE '2026-09-02', 'Quốc khánh',                   TRUE, 'Mô phỏng');

-- 5. Loai nghi (company_profile.md Muc 6.1)
INSERT INTO leave_type (id, code, name, paid, deduct_balance, requires_evidence) VALUES
    (1, 'ANNUAL',        'Nghỉ phép năm',              TRUE,  TRUE,  FALSE),
    (2, 'PERSONAL_PAID', 'Nghỉ việc riêng hưởng lương', TRUE,  FALSE, TRUE),
    (3, 'UNPAID',        'Nghỉ không hưởng lương',     FALSE, FALSE, FALSE),
    (4, 'SICK',          'Nghỉ ốm (chế độ BHXH)',      FALSE, FALSE, TRUE),
    (5, 'MATERNITY',     'Nghỉ thai sản (chế độ BHXH)', FALSE, FALSE, TRUE);

-- 6. Loai phu cap (company_profile.md Muc 8.3). Phan loai thue cac khoan an trua/gui xe/dien thoai
--    chua duoc xac nhan (OPEN-04) -> NEEDS_REVIEW, khong tu mien/tu chiu thue.
INSERT INTO allowance_type (id, code, name, tax_class, insurance_included, included_in_ot_base, description) VALUES
    (1, 'LUNCH',      'Phụ cấp ăn trưa',            'NEEDS_REVIEW', FALSE, FALSE, 'Full-time: 730.000/tháng (OPEN-04)'),
    (2, 'PARKING',    'Phụ cấp gửi xe',             'NEEDS_REVIEW', FALSE, FALSE, 'Full-time: 200.000/tháng (OPEN-04)'),
    (3, 'PHONE',      'Phụ cấp điện thoại',         'NEEDS_REVIEW', FALSE, FALSE, 'Sales, Account, trưởng bộ phận, cấp quản lý: 300.000/tháng (OPEN-04)'),
    (4, 'POSITION',   'Phụ cấp chức vụ',            'TAXABLE',      TRUE,  TRUE,  'Lead, Manager, Giám đốc: 2.000.000/tháng'),
    (5, 'LUNCH_PT',   'Phụ cấp ăn trưa part-time',  'NEEDS_REVIEW', FALSE, FALSE, 'Part-time 20 giờ/tuần: 365.000/tháng (OPEN-04)'),
    (6, 'PARKING_PT', 'Phụ cấp gửi xe part-time',   'NEEDS_REVIEW', FALSE, FALSE, 'Part-time 20 giờ/tuần: 100.000/tháng (OPEN-04)');

-- 7. (Mo rong) Diem cham cong QR
INSERT INTO attendance_point (id, code, name, photo_required) VALUES
    (1, 'HQ_KIOSK', 'Kiosk QR trụ sở HNQ Media', TRUE);

-- 8. Tham so payroll (payroll_rules.md Muc 2.2, 12.1)
INSERT INTO payroll_parameter (code, name, category, value, unit, region, effective_from, source_ref, status, confirmed_by, confirmed_at) VALUES
    -- Chinh sach cong ty
    ('OT_RATE_WEEKDAY',       'Hệ số OT ngày thường',                  'COMPANY', 1.5,       'MULTIPLIER', NULL, DATE '2026-07-01', 'company_profile.md 7.2; payroll_rules.md 2.2', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('OT_RATE_WEEKEND',       'Hệ số OT ngày nghỉ hằng tuần',          'COMPANY', 2,         'MULTIPLIER', NULL, DATE '2026-07-01', 'company_profile.md 7.2; payroll_rules.md 2.2', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('OT_RATE_HOLIDAY',       'Hệ số OT ngày lễ/Tết',                  'COMPANY', 3,         'MULTIPLIER', NULL, DATE '2026-07-01', 'company_profile.md 7.2; payroll_rules.md 2.2', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('NIGHT_PREMIUM_RATE',    'Phần trả thêm làm đêm 22:00–06:00',     'COMPANY', 0.3,       'RATE',       NULL, DATE '2026-07-01', 'payroll_rules.md 2.2; L5', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('OT_MAX_HOURS_DAY',      'Giới hạn OT/ngày (ca 8 giờ)',           'COMPANY', 4,         'HOURS',      NULL, DATE '2026-07-01', 'company_profile.md 7.2; SRS OT; OPEN-06', 'NEEDS_REVIEW', NULL, NULL),
    ('OT_MAX_HOURS_MONTH',    'Giới hạn OT/tháng',                     'COMPANY', 40,        'HOURS',      NULL, DATE '2026-07-01', 'company_profile.md 7.2; SRS OT; OPEN-06', 'NEEDS_REVIEW', NULL, NULL),
    ('OT_MAX_HOURS_YEAR',     'Giới hạn OT/năm (mặc định)',            'COMPANY', 200,       'HOURS',      NULL, DATE '2026-07-01', 'SRS OT; OPEN-06', 'NEEDS_REVIEW', NULL, NULL),
    ('SALES_COMMISSION_RATE', 'Tỷ lệ hoa hồng Sales Executive',        'COMPANY', 0.02,      'RATE',       NULL, DATE '2026-07-01', 'company_profile.md 9.3', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('SALES_TARGET_MONTHLY',  'Chỉ tiêu doanh thu Sales đủ tháng',     'COMPANY', 100000000, 'VND',        NULL, DATE '2026-07-01', 'company_profile.md 9.3', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('SALES_KPI_BONUS',       'Thưởng KPI khi đạt ≥ 100% chỉ tiêu',   'COMPANY', 1500000,   'VND',        NULL, DATE '2026-07-01', 'company_profile.md 9.3', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('ANNUAL_LEAVE_DAYS',     'Số ngày phép năm đủ 12 tháng',          'COMPANY', 12,        'DAYS',       NULL, DATE '2026-07-01', 'company_profile.md 6.2', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    -- Tham so phap ly
    ('BHXH_EMPLOYEE_RATE',    'Tỷ lệ BHXH người lao động',             'LEGAL',   0.08,      'RATE',       NULL, DATE '2026-01-01', 'payroll_rules.md 2.2; L6', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('BHYT_EMPLOYEE_RATE',    'Tỷ lệ BHYT người lao động',             'LEGAL',   0.015,     'RATE',       NULL, DATE '2026-01-01', 'payroll_rules.md 2.2; L6', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('BHTN_EMPLOYEE_RATE',    'Tỷ lệ BHTN người lao động',             'LEGAL',   0.01,      'RATE',       NULL, DATE '2026-01-01', 'payroll_rules.md 2.2; L6', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('INSURANCE_REFERENCE_SALARY', 'Mức tham chiếu BHXH/BHYT',         'LEGAL',   2530000,   'VND',        NULL, DATE '2026-07-01', 'payroll_rules.md 2.2; L3 NĐ 161/2026/NĐ-CP', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('BHXH_BHYT_CAP_MULTIPLIER',   'Trần căn cứ BHXH/BHYT (× mức tham chiếu)', 'LEGAL', 20,  'MULTIPLIER', NULL, DATE '2026-07-01', 'payroll_rules.md 2.2; L3', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('BHTN_CAP_MULTIPLIER',   'Trần căn cứ BHTN (× lương tối thiểu vùng)', 'LEGAL', 20,      'MULTIPLIER', NULL, DATE '2026-01-01', 'payroll_rules.md 2.2; L6', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('MIN_WAGE_MONTHLY',      'Lương tối thiểu vùng theo tháng',       'LEGAL',   5310000,   'VND',        'REGION_I',   DATE '2026-01-01', 'payroll_rules.md 2.2; L4 NĐ 293/2025/NĐ-CP', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('MIN_WAGE_MONTHLY',      'Lương tối thiểu vùng theo tháng',       'LEGAL',   4730000,   'VND',        'REGION_II',  DATE '2026-01-01', 'L4 NĐ 293/2025/NĐ-CP – chưa đối chiếu', 'NEEDS_REVIEW', NULL, NULL),
    ('MIN_WAGE_MONTHLY',      'Lương tối thiểu vùng theo tháng',       'LEGAL',   4140000,   'VND',        'REGION_III', DATE '2026-01-01', 'L4 NĐ 293/2025/NĐ-CP – chưa đối chiếu', 'NEEDS_REVIEW', NULL, NULL),
    ('MIN_WAGE_MONTHLY',      'Lương tối thiểu vùng theo tháng',       'LEGAL',   3700000,   'VND',        'REGION_IV',  DATE '2026-01-01', 'L4 NĐ 293/2025/NĐ-CP – chưa đối chiếu', 'NEEDS_REVIEW', NULL, NULL),
    ('MIN_WAGE_HOURLY',       'Lương tối thiểu vùng theo giờ',         'LEGAL',   25500,     'VND_PER_HOUR', 'REGION_I', DATE '2026-01-01', 'payroll_rules.md 2.2; L4 NĐ 293/2025/NĐ-CP', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('PERSONAL_DEDUCTION',    'Giảm trừ bản thân',                     'LEGAL',   15500000,  'VND',        NULL, DATE '2026-01-01', 'payroll_rules.md 2.2; L1 NQ 110/2025/UBTVQH15', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('DEPENDENT_DEDUCTION',   'Giảm trừ mỗi người phụ thuộc',          'LEGAL',   6200000,   'VND',        NULL, DATE '2026-01-01', 'payroll_rules.md 2.2; L1 NQ 110/2025/UBTVQH15', 'VERIFIED_SIM', 'Nhóm dự án HRFlow (mô phỏng)', DATE '2026-10-29'),
    ('PIT_FLAT_10_THRESHOLD', 'Ngưỡng khấu trừ 10% (không HĐLĐ/HĐLĐ < 3 tháng)', 'LEGAL', 5000000, 'VND', NULL, DATE '2026-07-01', 'payroll_rules.md 8; L2 - OPEN-02', 'NEEDS_REVIEW', NULL, NULL),
    ('PIT_FLAT_10_RATE',      'Thuế suất khấu trừ 10%',                'LEGAL',   0.10,      'RATE',       NULL, DATE '2026-07-01', 'payroll_rules.md 8; L2 - OPEN-02', 'NEEDS_REVIEW', NULL, NULL),
    ('PIT_NON_RESIDENT_RATE', 'Thuế suất cá nhân không cư trú',        'LEGAL',   0.20,      'RATE',       NULL, DATE '2026-01-01', 'payroll_rules.md 8; L2 - OPEN-02', 'NEEDS_REVIEW', NULL, NULL);

-- 9. Bieu thue luy tien 5 bac (payroll_rules.md Muc 8)
INSERT INTO tax_bracket (set_code, effective_from, seq, lower_bound, upper_bound, rate, source_ref) VALUES
    ('PIT_RESIDENT_2026', DATE '2026-01-01', 1, 0,         10000000,  0.05, 'payroll_rules.md 8; L1 Luật 109/2025/QH15'),
    ('PIT_RESIDENT_2026', DATE '2026-01-01', 2, 10000000,  30000000,  0.10, 'payroll_rules.md 8; L1 Luật 109/2025/QH15'),
    ('PIT_RESIDENT_2026', DATE '2026-01-01', 3, 30000000,  60000000,  0.20, 'payroll_rules.md 8; L1 Luật 109/2025/QH15'),
    ('PIT_RESIDENT_2026', DATE '2026-01-01', 4, 60000000,  100000000, 0.30, 'payroll_rules.md 8; L1 Luật 109/2025/QH15'),
    ('PIT_RESIDENT_2026', DATE '2026-01-01', 5, 100000000, NULL,      0.35, 'payroll_rules.md 8; L1 Luật 109/2025/QH15');

SELECT setval(pg_get_serial_sequence('department', 'id'), (SELECT MAX(id) FROM department));
SELECT setval(pg_get_serial_sequence('job_title', 'id'), (SELECT MAX(id) FROM job_title));
SELECT setval(pg_get_serial_sequence('work_schedule', 'id'), (SELECT MAX(id) FROM work_schedule));
SELECT setval(pg_get_serial_sequence('leave_type', 'id'), (SELECT MAX(id) FROM leave_type));
SELECT setval(pg_get_serial_sequence('allowance_type', 'id'), (SELECT MAX(id) FROM allowance_type));
SELECT setval(pg_get_serial_sequence('attendance_point', 'id'), (SELECT MAX(id) FROM attendance_point));
