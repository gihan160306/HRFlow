-- V21: Mo rong du lieu demo tu 40 len 100 nhan su HNQ Media (company_profile.md ban 100 nguoi).
-- Bo sung 60 nhan su (id 41-100) cho V20: HR +3, Content +14, Design +10, Digital +13, Account +9, Sales +11.
-- Tong sau V21: 85 full-time, 8 part-time, 5 thu viec, 2 thuc tap sinh; Admin ky thuat van ngoai 100 nhan su.
-- Khong sua V20 vi migration do co the da duoc ap dung.

CREATE TEMP TABLE seed_emp (
    id        BIGINT PRIMARY KEY,
    code      VARCHAR(20),
    full_name VARCHAR(150),
    gender    VARCHAR(10),
    dob       DATE,
    dept      VARCHAR(20),
    title     VARCHAR(30),
    emp_type  VARCHAR(30),
    manager   BIGINT,
    hire_date DATE
) ON COMMIT DROP;

-- Moi khoi: (phong ban, chuc danh, quan ly truc tiep, id dau, id cuoi, danh sach id part-time/thu viec/thuc tap).
INSERT INTO seed_emp
SELECT n.id,
       'NV' || lpad(n.id::text, 3, '0'),
       (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Phan','Vũ','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương','Lý'])[1 + (n.id * 7) % 16]
         || ' ' ||
       CASE WHEN n.id % 2 = 0
            THEN (ARRAY['Thị','Ngọc','Thu','Hoài','Khánh','Mỹ'])[1 + (n.id * 5) % 6]
            ELSE (ARRAY['Văn','Quốc','Minh','Gia','Đức','Hữu'])[1 + (n.id * 5) % 6]
       END
         || ' ' ||
       CASE WHEN n.id % 2 = 0
            THEN (ARRAY['Lan','Hương','Trang','Vy','Nhi','Thảo','Linh','Phương','Anh','Mai','Dung','Yến','Hạnh'])[1 + (n.id * 3) % 13]
            ELSE (ARRAY['Hùng','Dũng','Tuấn','Khoa','Long','Nam','Phát','Sơn','Thịnh','Bình','Đạt','Khải','Lộc'])[1 + (n.id * 3) % 13]
       END,
       CASE WHEN n.id % 2 = 0 THEN 'FEMALE' ELSE 'MALE' END,
       DATE '1988-01-01' + ((n.id * 211) % 4500),
       b.dept, b.title,
       CASE
           WHEN n.id = ANY (b.probation) THEN 'PROBATION'
           WHEN n.id = ANY (b.intern)    THEN 'INTERN'
           WHEN n.id = ANY (b.part_time) THEN 'PART_TIME'
           ELSE 'FULL_TIME'
       END,
       b.manager,
       CASE
           WHEN n.id = ANY (b.probation) OR n.id = ANY (b.intern) THEN DATE '2026-10-05' + (n.id % 10)
           WHEN n.id = ANY (b.part_time)                          THEN DATE '2025-06-02' + ((n.id * 13) % 330)
           ELSE DATE '2023-01-02' + ((n.id * 37) % 1000)
       END
FROM (VALUES
    ('HR',      'HR_EXEC',       4,  41, 43,  ARRAY[43],      ARRAY[]::INT[], ARRAY[]::INT[]),
    ('CONTENT', 'CONTENT_EXEC',  8,  44, 57,  ARRAY[]::INT[], ARRAY[57],      ARRAY[]::INT[]),
    ('DESIGN',  'DESIGNER',      16, 58, 67,  ARRAY[]::INT[], ARRAY[66],      ARRAY[67]),
    ('DIGITAL', 'SEO_ADS',       22, 68, 80,  ARRAY[79, 80],  ARRAY[]::INT[], ARRAY[]::INT[]),
    ('ACCOUNT', 'ACCOUNT_EXEC',  29, 81, 89,  ARRAY[86, 87],  ARRAY[89],      ARRAY[]::INT[]),
    ('SALES',   'SALES_EXEC',    34, 90, 100, ARRAY[]::INT[], ARRAY[]::INT[], ARRAY[]::INT[])
) AS b(dept, title, manager, id_from, id_to, part_time, probation, intern)
CROSS JOIN LATERAL generate_series(b.id_from, b.id_to) AS n(id);

-- 1. Ho so nhan vien
INSERT INTO employee (
    id, code, full_name, gender, date_of_birth, phone, work_email, address, national_id,
    bank_name, bank_account_no, employee_type, status, hire_date, work_location, wage_region,
    department_id, job_title_id, manager_id
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
       d.id, j.id, s.manager
FROM seed_emp s
JOIN department d ON d.code = s.dept
JOIN job_title j ON j.code = s.title;

-- 2. Tai khoan (id tu sinh; Admin ky thuat id 41 tu V20 khong doi)
INSERT INTO user_account (email, password_hash, role, status, employee_id)
SELECT lower(s.code) || '@hnqmedia.example',
       '$2a$10$x9xqsF4vwFM75.qCjPJmpOjKfRJpzm3p.HBjoXq/gmcoSjDJJ0Eku',
       'EMPLOYEE', 'ACTIVE', s.id
FROM seed_emp s ORDER BY s.id;

-- 3. Lich su phan cong ban dau
INSERT INTO employee_assignment (employee_id, department_id, job_title_id, manager_id, effective_from, reason, decision_ref, created_by)
SELECT e.id, e.department_id, e.job_title_id, e.manager_id, e.hire_date, 'Tiếp nhận nhân sự', 'QD-TN-' || e.code, 4
FROM employee e JOIN seed_emp s ON s.id = e.id;

-- 4. Hop dong va luong co ban theo hieu luc
INSERT INTO contract (employee_id, contract_no, contract_type, start_date, end_date, signed_date, status, note, created_by)
SELECT s.id, 'HD-' || s.code || '-01',
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
SELECT s.id, c.id,
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
       c.contract_no, 4
FROM seed_emp s
JOIN job_title j ON j.code = s.title
JOIN contract c ON c.contract_no = 'HD-' || s.code || '-01';

-- 5. Lich lam viec ca nhan
INSERT INTO employee_schedule (employee_id, schedule_id, effective_from, created_by)
SELECT s.id, ws.id, s.hire_date, 4
FROM seed_emp s
JOIN work_schedule ws ON ws.code = CASE WHEN s.emp_type = 'PART_TIME' THEN 'PT_MORNING' ELSE 'FT_STD' END;

-- 6. Phu cap co dinh (company_profile.md Muc 8.3); thuc tap sinh khong co phu cap thuong xuyen
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

-- 9. Tuyen duyet PTO/OT mot cap (company_profile.md Muc 4): nhan vien -> Lead/Sales Manager/HR Lead truc tiep.
--    Nguoi du phong: Operations Manager (Content/Design/Digital/Account) hoac Giam doc (Sales/HR).
INSERT INTO approval_route (requester_id, primary_approver_id, backup_approver_id, effective_from)
SELECT s.id, s.manager, CASE WHEN s.dept IN ('SALES', 'HR') THEN 1 ELSE 2 END,
       GREATEST(s.hire_date, DATE '2026-07-01')
FROM seed_emp s;

-- 10. Quy phep nam 2026 (thuc tap sinh khong co phep nam; vao trong nam tinh theo thang con lai)
INSERT INTO leave_balance (employee_id, balance_year, accrued_days)
SELECT s.id, 2026,
       CASE WHEN s.hire_date < DATE '2026-01-01' THEN 12
            ELSE 13 - EXTRACT(MONTH FROM s.hire_date) END
FROM seed_emp s
WHERE s.emp_type <> 'INTERN';

SELECT setval(pg_get_serial_sequence('employee', 'id'), (SELECT MAX(id) FROM employee));
SELECT setval(pg_get_serial_sequence('user_account', 'id'), (SELECT MAX(id) FROM user_account));
SELECT setval(pg_get_serial_sequence('contract', 'id'), (SELECT MAX(id) FROM contract));
