-- V20: Du lieu demo 40 nhan su HNQ Media theo company_profile.md (Muc 2, 3, 4, 8).
-- * 34 full-time, 3 part-time, 2 thu viec, 1 thuc tap sinh.
-- * Tai khoan Admin ky thuat KHONG gan ho so nhan vien, KHONG co grant payroll.
-- * Grant payroll (DEC-03): HR -> VIEW/PREPARE, Giam doc -> APPROVE, Finance Manager -> LOCK.
-- * wage_region = REGION_I la gia dinh cho du lieu mau; HR phai xac nhan theo dia ban (OPEN-01).
-- * Mat khau demo cho moi tai khoan: password123 (BCrypt).

CREATE TEMP TABLE seed_emp (
    id          BIGINT PRIMARY KEY,
    code        VARCHAR(20),
    full_name   VARCHAR(150),
    gender      VARCHAR(10),
    dob         DATE,
    dept        VARCHAR(20),
    title       VARCHAR(30),
    emp_type    VARCHAR(30),
    manager     BIGINT,
    hire_date   DATE,
    role        VARCHAR(30)
) ON COMMIT DROP;

INSERT INTO seed_emp VALUES
    -- Management
    (1,  'NV001', 'Đặng Minh Hiếu',          'MALE',   DATE '1985-05-15', 'MGMT',    'DIRECTOR',      'FULL_TIME', NULL, DATE '2022-01-03', 'MANAGER'),
    (2,  'NV002', 'Trần Thị Lệ Hoa',         'FEMALE', DATE '1988-11-10', 'MGMT',    'OPS_MANAGER',   'FULL_TIME', 1,    DATE '2022-03-01', 'MANAGER'),
    (3,  'NV003', 'Nguyễn Hoàng Phương Ngân', 'FEMALE', DATE '1989-04-25', 'MGMT',    'FIN_MANAGER',   'FULL_TIME', 1,    DATE '2022-03-01', 'MANAGER'),
    -- Human Resources
    (4,  'NV004', 'Hồ Thị Gia Hân',          'FEMALE', DATE '1990-08-20', 'HR',      'HR_LEAD',       'FULL_TIME', 1,    DATE '2022-04-01', 'HR'),
    (5,  'NV005', 'Lê Thu Trang',            'FEMALE', DATE '1995-02-14', 'HR',      'HR_EXEC',       'FULL_TIME', 4,    DATE '2023-02-01', 'HR'),
    (6,  'NV006', 'Phạm Quốc Bảo',           'MALE',   DATE '1996-07-09', 'HR',      'HR_EXEC',       'FULL_TIME', 4,    DATE '2023-08-01', 'HR'),
    (7,  'NV007', 'Võ Ngọc Mai',             'FEMALE', DATE '1998-12-03', 'HR',      'HR_EXEC',       'FULL_TIME', 4,    DATE '2024-05-02', 'HR'),
    -- Content
    (8,  'NV008', 'Hồ Vinh Quan',            'MALE',   DATE '1991-09-12', 'CONTENT', 'CONTENT_LEAD',  'FULL_TIME', 2,    DATE '2022-06-01', 'MANAGER'),
    (9,  'NV009', 'Nguyễn Thanh Tâm',        'FEMALE', DATE '1996-03-22', 'CONTENT', 'CONTENT_EXEC',  'FULL_TIME', 8,    DATE '2023-03-01', 'EMPLOYEE'),
    (10, 'NV010', 'Trần Minh Khang',         'MALE',   DATE '1997-06-30', 'CONTENT', 'CONTENT_EXEC',  'FULL_TIME', 8,    DATE '2023-09-04', 'EMPLOYEE'),
    (11, 'NV011', 'Lý Bảo Ngọc',             'FEMALE', DATE '1998-01-17', 'CONTENT', 'CONTENT_EXEC',  'FULL_TIME', 8,    DATE '2024-02-19', 'EMPLOYEE'),
    (12, 'NV012', 'Đỗ Hải Yến',              'FEMALE', DATE '1999-10-05', 'CONTENT', 'CONTENT_EXEC',  'FULL_TIME', 8,    DATE '2025-03-03', 'EMPLOYEE'),
    (13, 'NV013', 'Bùi Gia Huy',             'MALE',   DATE '2001-04-11', 'CONTENT', 'CONTENT_EXEC',  'PART_TIME', 8,    DATE '2025-06-02', 'EMPLOYEE'),
    (14, 'NV014', 'Phan Thảo Vy',            'FEMALE', DATE '2002-08-08', 'CONTENT', 'CONTENT_EXEC',  'PART_TIME', 8,    DATE '2026-01-05', 'EMPLOYEE'),
    (15, 'NV015', 'Ngô Đức Anh',             'MALE',   DATE '2004-11-26', 'CONTENT', 'CONTENT_EXEC',  'INTERN',    8,    DATE '2026-09-07', 'EMPLOYEE'),
    -- Design & Video
    (16, 'NV016', 'Vũ Hoàng Long',           'MALE',   DATE '1990-02-28', 'DESIGN',  'DESIGN_LEAD',   'FULL_TIME', 2,    DATE '2022-07-01', 'MANAGER'),
    (17, 'NV017', 'Mai Phương Linh',         'FEMALE', DATE '1996-05-19', 'DESIGN',  'DESIGNER',      'FULL_TIME', 16,   DATE '2023-04-03', 'EMPLOYEE'),
    (18, 'NV018', 'Đinh Công Thành',         'MALE',   DATE '1995-12-01', 'DESIGN',  'DESIGNER',      'FULL_TIME', 16,   DATE '2023-10-02', 'EMPLOYEE'),
    (19, 'NV019', 'Hoàng Kim Ngân',          'FEMALE', DATE '1998-07-14', 'DESIGN',  'DESIGNER',      'FULL_TIME', 16,   DATE '2024-07-01', 'EMPLOYEE'),
    (20, 'NV020', 'Trương Nhật Minh',        'MALE',   DATE '1999-09-23', 'DESIGN',  'DESIGNER',      'FULL_TIME', 16,   DATE '2025-01-06', 'EMPLOYEE'),
    (21, 'NV021', 'Lâm Tú Anh',              'FEMALE', DATE '2001-03-03', 'DESIGN',  'DESIGNER',      'PART_TIME', 16,   DATE '2025-08-04', 'EMPLOYEE'),
    -- Digital Marketing
    (22, 'NV022', 'Cao Thành Đạt',           'MALE',   DATE '1989-06-06', 'DIGITAL', 'DIGITAL_LEAD',  'FULL_TIME', 2,    DATE '2022-08-01', 'MANAGER'),
    (23, 'NV023', 'Nguyễn Hữu Phước',        'MALE',   DATE '1994-10-10', 'DIGITAL', 'SEO_ADS',       'FULL_TIME', 22,   DATE '2023-05-02', 'EMPLOYEE'),
    (24, 'NV024', 'Trần Ngọc Hân',           'FEMALE', DATE '1997-04-04', 'DIGITAL', 'SEO_ADS',       'FULL_TIME', 22,   DATE '2023-11-01', 'EMPLOYEE'),
    (25, 'NV025', 'Lê Quang Vinh',           'MALE',   DATE '1996-08-18', 'DIGITAL', 'SEO_ADS',       'FULL_TIME', 22,   DATE '2024-03-04', 'EMPLOYEE'),
    (26, 'NV026', 'Phạm Thị Hồng Nhung',     'FEMALE', DATE '1998-02-27', 'DIGITAL', 'SEO_ADS',       'FULL_TIME', 22,   DATE '2024-09-03', 'EMPLOYEE'),
    (27, 'NV027', 'Đặng Văn Khoa',           'MALE',   DATE '1999-11-11', 'DIGITAL', 'SEO_ADS',       'FULL_TIME', 22,   DATE '2025-04-01', 'EMPLOYEE'),
    (28, 'NV028', 'Huỳnh Mỹ Duyên',          'FEMALE', DATE '2000-05-30', 'DIGITAL', 'SEO_ADS',       'PROBATION', 22,   DATE '2026-10-15', 'EMPLOYEE'),
    -- Account
    (29, 'NV029', 'Tô Thanh Hương',          'FEMALE', DATE '1991-01-21', 'ACCOUNT', 'ACCOUNT_LEAD',  'FULL_TIME', 2,    DATE '2022-09-05', 'MANAGER'),
    (30, 'NV030', 'Dương Minh Trí',          'MALE',   DATE '1995-07-07', 'ACCOUNT', 'ACCOUNT_EXEC',  'FULL_TIME', 29,   DATE '2023-06-01', 'EMPLOYEE'),
    (31, 'NV031', 'Châu Ngọc Diệp',          'FEMALE', DATE '1997-09-09', 'ACCOUNT', 'ACCOUNT_EXEC',  'FULL_TIME', 29,   DATE '2024-01-08', 'EMPLOYEE'),
    (32, 'NV032', 'Kiều Anh Tuấn',           'MALE',   DATE '1998-03-16', 'ACCOUNT', 'ACCOUNT_EXEC',  'FULL_TIME', 29,   DATE '2024-10-01', 'EMPLOYEE'),
    (33, 'NV033', 'Lương Thùy Dung',         'FEMALE', DATE '1999-12-24', 'ACCOUNT', 'ACCOUNT_EXEC',  'FULL_TIME', 29,   DATE '2025-05-05', 'EMPLOYEE'),
    -- Sales
    (34, 'NV034', 'Hà Quốc Cường',           'MALE',   DATE '1987-10-02', 'SALES',   'SALES_MANAGER', 'FULL_TIME', 1,    DATE '2022-05-03', 'MANAGER'),
    (35, 'NV035', 'Nguyễn Văn Sơn',          'MALE',   DATE '1994-04-15', 'SALES',   'SALES_EXEC',    'FULL_TIME', 34,   DATE '2023-07-03', 'EMPLOYEE'),
    (36, 'NV036', 'Trần Thị Kim Oanh',       'FEMALE', DATE '1996-06-25', 'SALES',   'SALES_EXEC',    'FULL_TIME', 34,   DATE '2023-12-04', 'EMPLOYEE'),
    (37, 'NV037', 'Phùng Gia Bảo',           'MALE',   DATE '1997-02-08', 'SALES',   'SALES_EXEC',    'FULL_TIME', 34,   DATE '2024-04-01', 'EMPLOYEE'),
    (38, 'NV038', 'Lê Hoài Thương',          'FEMALE', DATE '1998-08-29', 'SALES',   'SALES_EXEC',    'FULL_TIME', 34,   DATE '2024-11-04', 'EMPLOYEE'),
    (39, 'NV039', 'Võ Thanh Phong',          'MALE',   DATE '1999-05-13', 'SALES',   'SALES_EXEC',    'FULL_TIME', 34,   DATE '2025-07-01', 'EMPLOYEE'),
    (40, 'NV040', 'Đoàn Khánh Linh',         'FEMALE', DATE '2000-09-19', 'SALES',   'SALES_EXEC',    'PROBATION', 34,   DATE '2026-10-15', 'EMPLOYEE');

-- 1. Ho so nhan vien (tao truoc, gan manager_id sau de khong phu thuoc thu tu)
INSERT INTO employee (
    id, code, full_name, gender, date_of_birth, phone, work_email, address, national_id,
    bank_name, bank_account_no, employee_type, status, hire_date, work_location, wage_region,
    department_id, job_title_id
)
SELECT s.id, s.code, s.full_name, s.gender, s.dob,
       '0901' || lpad(s.id::text, 6, '0'),
       lower(s.code) || '@hnqmedia.example',
       'TP. Hồ Chí Minh',
       '0790' || lpad(s.id::text, 8, '0'),
       (ARRAY['Vietcombank', 'Techcombank', 'ACB', 'MB Bank'])[1 + s.id % 4],
       '0071' || lpad(s.id::text, 8, '0'),
       s.emp_type, 'ACTIVE', s.hire_date,
       'Trụ sở HNQ Media, TP. Hồ Chí Minh',
       'REGION_I',
       d.id, j.id
FROM seed_emp s
JOIN department d ON d.code = s.dept
JOIN job_title j ON j.code = s.title;

UPDATE employee e SET manager_id = s.manager FROM seed_emp s WHERE s.id = e.id AND s.manager IS NOT NULL;

-- 2. Tai khoan: 40 tai khoan gan nhan vien + 1 Admin ky thuat khong gan nhan vien
INSERT INTO user_account (id, email, password_hash, role, status, employee_id)
SELECT s.id, lower(s.code) || '@hnqmedia.example',
       '$2a$10$x9xqsF4vwFM75.qCjPJmpOjKfRJpzm3p.HBjoXq/gmcoSjDJJ0Eku',
       s.role, 'ACTIVE', s.id
FROM seed_emp s;

INSERT INTO user_account (id, email, password_hash, role, status, employee_id) VALUES
    (41, 'admin@hnqmedia.example', '$2a$10$x9xqsF4vwFM75.qCjPJmpOjKfRJpzm3p.HBjoXq/gmcoSjDJJ0Eku', 'ADMIN', 'ACTIVE', NULL);

-- 3. Lich su phan cong ban dau
INSERT INTO employee_assignment (employee_id, department_id, job_title_id, manager_id, effective_from, reason, decision_ref, created_by)
SELECT e.id, e.department_id, e.job_title_id, e.manager_id, e.hire_date, 'Tiếp nhận nhân sự', 'QD-TN-' || e.code, 4
FROM employee e;

-- 4. Hop dong va luong co ban theo hieu luc
INSERT INTO contract (id, employee_id, contract_no, contract_type, start_date, end_date, signed_date, status, note, created_by)
SELECT s.id, s.id, 'HD-' || s.code || '-01',
       CASE
           WHEN s.emp_type = 'PART_TIME' THEN 'PART_TIME'
           WHEN s.emp_type = 'PROBATION' THEN 'PROBATION'
           WHEN s.emp_type = 'INTERN'    THEN 'INTERNSHIP'
           WHEN s.hire_date < DATE '2025-01-01' THEN 'INDEFINITE'
           ELSE 'FIXED_TERM'
       END,
       s.hire_date,
       CASE
           WHEN s.emp_type = 'PROBATION' THEN s.hire_date + 59
           WHEN s.emp_type = 'INTERN'    THEN DATE '2026-12-31'
           WHEN s.emp_type = 'FULL_TIME' AND s.hire_date >= DATE '2025-01-01'
               THEN (s.hire_date + INTERVAL '2 years')::date - 1
       END,
       s.hire_date, 'ACTIVE', 'Dữ liệu mẫu', 4
FROM seed_emp s;

INSERT INTO salary_record (employee_id, contract_id, base_salary, effective_from, reason, decision_ref, created_by)
SELECT s.id, s.id,
       CASE s.emp_type
           WHEN 'PART_TIME' THEN 5000000
           WHEN 'PROBATION' THEN ROUND(j.default_base_salary * 0.85)
           WHEN 'INTERN'    THEN 4000000
           ELSE j.default_base_salary
       END,
       s.hire_date,
       CASE s.emp_type
           WHEN 'PART_TIME' THEN 'Lương part-time lịch 20 giờ/tuần'
           WHEN 'PROBATION' THEN 'Lương thử việc 85% mức chức danh'
           WHEN 'INTERN'    THEN 'Trợ cấp thực tập'
           ELSE 'Lương cơ bản theo chức danh'
       END,
       'HD-' || s.code || '-01', 4
FROM seed_emp s JOIN job_title j ON j.code = s.title;

-- 5. Lich lam viec ca nhan
INSERT INTO employee_schedule (employee_id, schedule_id, effective_from, created_by)
SELECT s.id, ws.id, s.hire_date, 4
FROM seed_emp s
JOIN work_schedule ws ON ws.code = CASE WHEN s.emp_type = 'PART_TIME' THEN 'PT_MORNING' ELSE 'FT_STD' END;

-- 6. Phu cap co dinh (company_profile.md Muc 8.3), hieu luc tu ngay ap dung chinh sach 01/07/2026
INSERT INTO employee_allowance (employee_id, allowance_type_id, amount, effective_from, decision_ref, status, created_by)
SELECT s.id, at.id, x.amount, GREATEST(s.hire_date, DATE '2026-07-01'), 'QC-HNQ-2026-07',
       CASE WHEN s.emp_type = 'PROBATION' THEN 'NEEDS_REVIEW' ELSE 'CONFIRMED' END, 4
FROM seed_emp s
JOIN job_title j ON j.code = s.title
CROSS JOIN LATERAL (VALUES
    ('LUNCH',      730000,  s.emp_type IN ('FULL_TIME', 'PROBATION')),
    ('PARKING',    200000,  s.emp_type IN ('FULL_TIME', 'PROBATION')),
    ('PHONE',      300000,  s.emp_type IN ('FULL_TIME', 'PROBATION')
                            AND (j.managerial OR j.code IN ('SALES_EXEC', 'ACCOUNT_EXEC'))),
    ('POSITION',   2000000, j.managerial),
    ('LUNCH_PT',   365000,  s.emp_type = 'PART_TIME'),
    ('PARKING_PT', 100000,  s.emp_type = 'PART_TIME')
) AS x(code, amount, eligible)
JOIN allowance_type at ON at.code = x.code
WHERE x.eligible;

-- 7. Ho so thue (OPEN-02: thu viec/thuc tap chua xac nhan phuong phap)
INSERT INTO tax_profile (employee_id, effective_from, residency, method, tax_code, status, verified_by, verified_at, note)
SELECT s.id, GREATEST(s.hire_date, DATE '2026-07-01'), 'RESIDENT',
       CASE WHEN s.emp_type IN ('PROBATION', 'INTERN') THEN 'FLAT_10' ELSE 'PROGRESSIVE' END,
       '80' || lpad(s.id::text, 8, '0'),
       CASE WHEN s.emp_type IN ('PROBATION', 'INTERN') THEN 'NEEDS_REVIEW' ELSE 'CONFIRMED' END,
       CASE WHEN s.emp_type IN ('PROBATION', 'INTERN') THEN NULL ELSE 4 END,
       CASE WHEN s.emp_type IN ('PROBATION', 'INTERN') THEN NULL ELSE TIMESTAMPTZ '2026-07-01 09:00:00+07' END,
       CASE WHEN s.emp_type IN ('PROBATION', 'INTERN') THEN 'OPEN-02: chờ HR/Finance xác nhận phương pháp khấu trừ' END
FROM seed_emp s;

-- 8. Ho so bao hiem (OPEN-03: part-time/thu viec/thuc tap chua xac nhan doi tuong tham gia)
INSERT INTO insurance_profile (employee_id, effective_from, bhxh, bhyt, bhtn, insurance_salary, status, verified_by, verified_at, note)
SELECT s.id, GREATEST(s.hire_date, DATE '2026-07-01'),
       s.emp_type IN ('FULL_TIME', 'PART_TIME'),
       s.emp_type IN ('FULL_TIME', 'PART_TIME'),
       s.emp_type IN ('FULL_TIME', 'PART_TIME'),
       CASE s.emp_type
           WHEN 'FULL_TIME' THEN j.default_base_salary + CASE WHEN j.managerial THEN 2000000 ELSE 0 END
           WHEN 'PART_TIME' THEN 5000000
           ELSE 0
       END,
       CASE WHEN s.emp_type = 'FULL_TIME' THEN 'CONFIRMED' ELSE 'NEEDS_REVIEW' END,
       CASE WHEN s.emp_type = 'FULL_TIME' THEN 4 END,
       CASE WHEN s.emp_type = 'FULL_TIME' THEN TIMESTAMPTZ '2026-07-01 09:00:00+07' END,
       CASE WHEN s.emp_type <> 'FULL_TIME' THEN 'OPEN-03: chờ HR xác nhận đối tượng và căn cứ đóng' END
FROM seed_emp s JOIN job_title j ON j.code = s.title;

-- 9. Nguoi phu thuoc
INSERT INTO dependent (employee_id, full_name, relationship, valid_from, doc_ref, verified) VALUES
    (1,  'Đặng Minh Khôi',  'CHILD',  DATE '2026-01-01', 'NPT-NV001-01', TRUE),
    (1,  'Đặng Minh Anh',   'CHILD',  DATE '2026-01-01', 'NPT-NV001-02', TRUE),
    (2,  'Lê Gia Bảo',      'CHILD',  DATE '2026-01-01', 'NPT-NV002-01', TRUE),
    (4,  'Nguyễn Gia Huy',  'CHILD',  DATE '2026-01-01', 'NPT-NV004-01', TRUE),
    (23, 'Nguyễn Thị Lan',  'PARENT', DATE '2026-01-01', 'NPT-NV023-01', TRUE),
    (34, 'Hà Minh Châu',    'CHILD',  DATE '2026-01-01', 'NPT-NV034-01', TRUE);

-- 10. Tuyen duyet PTO/OT mot cap (company_profile.md Muc 4)
INSERT INTO approval_route (requester_id, primary_approver_id, backup_approver_id, backup_designated_by, backup_designation_ref, effective_from, note)
SELECT s.id,
       CASE
           WHEN s.title = 'DIRECTOR' THEN 4                                       -- Giam doc -> HR Lead
           WHEN s.title IN ('OPS_MANAGER', 'FIN_MANAGER', 'SALES_MANAGER', 'HR_LEAD') THEN 1
           WHEN s.title IN ('CONTENT_LEAD', 'DESIGN_LEAD', 'DIGITAL_LEAD', 'ACCOUNT_LEAD') THEN 2
           ELSE s.manager                                                         -- nhan vien -> Lead/Manager truc tiep
       END,
       CASE
           WHEN s.title = 'DIRECTOR' THEN 5                                       -- HR Executive do HR Lead chi dinh truoc
           WHEN s.title = 'HR_LEAD' THEN 3                                        -- Finance Manager do Giam doc chi dinh truoc
           WHEN s.title IN ('OPS_MANAGER', 'FIN_MANAGER', 'SALES_MANAGER') THEN NULL
           WHEN s.title IN ('CONTENT_LEAD', 'DESIGN_LEAD', 'DIGITAL_LEAD', 'ACCOUNT_LEAD') THEN 1
           WHEN s.dept IN ('SALES', 'HR') THEN 1
           ELSE 2                                                                 -- Content/Design/Digital/Account -> Operations Manager
       END,
       CASE WHEN s.title = 'DIRECTOR' THEN 4 WHEN s.title = 'HR_LEAD' THEN 1 END,
       CASE WHEN s.title = 'DIRECTOR' THEN 'CD-DP-2026-01 (HR Lead chỉ định)'
            WHEN s.title = 'HR_LEAD' THEN 'CD-DP-2026-02 (Giám đốc chỉ định)' END,
       GREATEST(s.hire_date, DATE '2026-07-01'),
       CASE WHEN s.title IN ('OPS_MANAGER', 'FIN_MANAGER', 'SALES_MANAGER')
            THEN 'Không có người dự phòng mặc định; Giám đốc vắng thì đơn giữ PENDING' END
FROM seed_emp s;

-- 11. Quy phep nam 2026 (thuc tap sinh khong co phep nam; vao trong nam tinh theo thang con lai)
INSERT INTO leave_balance (employee_id, balance_year, accrued_days)
SELECT s.id, 2026,
       CASE WHEN s.hire_date < DATE '2026-01-01' THEN 12
            ELSE 13 - EXTRACT(MONTH FROM s.hire_date) END
FROM seed_emp s
WHERE s.emp_type <> 'INTERN';

-- 12. Ky luong 11/2026 o trang thai DRAFT (ngay 05/12/2026 la thu Bay -> tra ngay lam viec lien truoc)
INSERT INTO payroll_period (id, period_year, period_month, start_date, end_date, pay_date, status, payment_status, created_by)
VALUES (1, 2026, 11, DATE '2026-11-01', DATE '2026-11-30', DATE '2026-12-04', 'DRAFT', 'UNPAID', 4);

-- 13. Grant payroll theo DEC-03 (Admin thuc hien cau hinh ky thuat theo quyet dinh, ban than khong co grant)
INSERT INTO payroll_grant (account_id, permission, valid_from, reason, decision_ref, granted_by) VALUES
    (4, 'VIEW',    TIMESTAMPTZ '2026-07-01 00:00:00+07', 'HR Lead lập và rà soát bảng lương',        'DEC-03', 41),
    (4, 'PREPARE', TIMESTAMPTZ '2026-07-01 00:00:00+07', 'HR Lead lập và rà soát bảng lương',        'DEC-03', 41),
    (5, 'VIEW',    TIMESTAMPTZ '2026-07-01 00:00:00+07', 'HR Executive phụ trách C&B',                'DEC-03', 41),
    (5, 'PREPARE', TIMESTAMPTZ '2026-07-01 00:00:00+07', 'HR Executive phụ trách C&B',                'DEC-03', 41),
    (1, 'APPROVE', TIMESTAMPTZ '2026-07-01 00:00:00+07', 'Giám đốc phê duyệt bảng lương',            'DEC-03', 41),
    (3, 'LOCK',    TIMESTAMPTZ '2026-07-01 00:00:00+07', 'Finance Manager khóa kỳ sau phê duyệt',     'DEC-03', 41);

SELECT setval(pg_get_serial_sequence('employee', 'id'), (SELECT MAX(id) FROM employee));
SELECT setval(pg_get_serial_sequence('user_account', 'id'), (SELECT MAX(id) FROM user_account));
SELECT setval(pg_get_serial_sequence('contract', 'id'), (SELECT MAX(id) FROM contract));
SELECT setval(pg_get_serial_sequence('payroll_period', 'id'), (SELECT MAX(id) FROM payroll_period));
