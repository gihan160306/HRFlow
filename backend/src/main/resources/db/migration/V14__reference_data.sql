-- V14: Du lieu tham chieu va du lieu mau cho he thong HRFlow.

-- 1. Phong ban
INSERT INTO department (department_id, department_code, department_name, description, is_active) VALUES
    (1, 'TECH', 'Khối Kỹ Thuật & Công Nghệ', 'Phát triển giải pháp công nghệ và bảo trì hệ thống', TRUE),
    (2, 'HR', 'Phòng Quản Trị Nhân Sự', 'Tuyển dụng, đào tạo, C&B và quan hệ lao động', TRUE),
    (3, 'CONTENT', 'Bộ Phận Nội Dung & SEO', 'Sáng tạo nội dung, tối ưu hóa công cụ tìm kiếm', TRUE),
    (4, 'DESIGN', 'Bộ Phận Thiết Kế & Media', 'Thiết kế đồ họa, quay dựng video quảng cáo', TRUE),
    (5, 'DIGITAL', 'Bộ Phận Digital Marketing', 'Chạy chiến dịch quảng cáo Facebook, Google, TikTok', TRUE),
    (6, 'SALES', 'Bộ Phận Kinh Doanh & CSKH', 'Tư vấn giải pháp, quản lý hợp đồng đối tác', TRUE),
    (7, 'FINANCE', 'Bộ Phận Tài Chính & Kế Toán', 'Kế toán quản trị, dòng tiền và kiểm toán nội bộ', TRUE)
ON CONFLICT (department_id) DO NOTHING;

-- 2. Ho so nhan su
INSERT INTO employee (
    employee_id, employee_code, full_name, work_email, phone, date_of_birth, address,
    national_id, tax_code, bank_name, bank_account_no, department_id, job_title,
    employee_type, manager_id, hire_date, status, dependents_count, tax_commitment_08,
    ot_eligible, commission_eligible, kpi_eligible, performance_bonus_eligible, team_bonus_eligible,
    lunch_allowance_eligible, parking_allowance_eligible, phone_allowance_eligible, position_allowance_eligible
) VALUES
    (1, 'EMP-0001', 'Đặng Minh Hiếu', 'admin@example.com', '0901234567', DATE '1990-05-15', 'Quận 1, TP. Hồ Chí Minh',
     '079090000001', '8000000001', 'Vietcombank', '1012345678', 1, 'Giám Đốc Vận Hành / Admin',
     'MANAGER', NULL, DATE '2024-01-01', 'ACTIVE', 0, FALSE,
     TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),

    (2, 'EMP-0002', 'Hồ Thị Gia Hân', 'hr@example.com', '0902345678', DATE '1994-08-20', 'Quận 3, TP. Hồ Chí Minh',
     '079094000002', '8000000002', 'Techcombank', '19023456789012', 2, 'Trưởng Phòng Nhân Sự',
     'LEADER', 1, DATE '2024-02-01', 'ACTIVE', 1, FALSE,
     TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),

    (3, 'EMP-0003', 'Trần Thị Lệ Hoa', 'manager@example.com', '0903456789', DATE '1992-11-10', 'Bình Thạnh, TP. Hồ Chí Minh',
     '079092000003', '8000000003', 'MB Bank', '090345678901', 1, 'Quản Lý Dự Án Kỹ Thuật',
     'MANAGER', 1, DATE '2024-03-01', 'ACTIVE', 0, FALSE,
     TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),

    (4, 'EMP-0004', 'Nguyễn Hoàng Phương Ngân', 'employee1@example.com', '0904567890', DATE '1998-04-25', 'Phú Nhuận, TP. Hồ Chí Minh',
     '079098000004', '8000000004', 'ACB', '2468101214', 2, 'Chuyên Viên C&B & Nhân Sự',
     'STAFF', 2, DATE '2024-05-01', 'ACTIVE', 0, FALSE,
     TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE),

    (5, 'EMP-0005', 'Hồ Vinh Quan', 'hqvinh@example.com', '0905678901', DATE '1999-09-12', 'Quận 7, TP. Hồ Chí Minh',
     '079099000005', '8000000005', 'TPBank', '0357911131', 1, 'Kỹ Sư Phần Mềm Fullstack',
     'STAFF', 3, DATE '2024-06-01', 'ACTIVE', 0, FALSE,
     TRUE, FALSE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE)
ON CONFLICT (employee_id) DO NOTHING;

-- 3. Tai khoan dang nhap (BCrypt password: password123)
INSERT INTO app_user (id, employee_id, email, password_hash, role, is_active) VALUES
    (1, 1, 'admin@example.com', '$2a$10$x9xqsF4vwFM75.qCjPJmpOjKfRJpzm3p.HBjoXq/gmcoSjDJJ0Eku', 'ADMIN', TRUE),
    (2, 2, 'hr@example.com', '$2a$10$x9xqsF4vwFM75.qCjPJmpOjKfRJpzm3p.HBjoXq/gmcoSjDJJ0Eku', 'HR', TRUE),
    (3, 3, 'manager@example.com', '$2a$10$x9xqsF4vwFM75.qCjPJmpOjKfRJpzm3p.HBjoXq/gmcoSjDJJ0Eku', 'MANAGER', TRUE),
    (4, 4, 'employee1@example.com', '$2a$10$x9xqsF4vwFM75.qCjPJmpOjKfRJpzm3p.HBjoXq/gmcoSjDJJ0Eku', 'EMPLOYEE', TRUE),
    (5, 5, 'hqvinh@example.com', '$2a$10$x9xqsF4vwFM75.qCjPJmpOjKfRJpzm3p.HBjoXq/gmcoSjDJJ0Eku', 'EMPLOYEE', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 4. Hop dong lao dong
INSERT INTO contract (
    contract_id, employee_id, contract_no, contract_type, start_date, end_date,
    base_salary, monthly_stipend, probation_rate, insurance_salary, probation_insured,
    status, note, created_by
) VALUES
    (1, 1, 'HD-2024-001', 'INDEFINITE', DATE '2024-01-01', NULL,
     35000000, 0, 1.0, 35000000, TRUE, 'ACTIVE', 'Hợp đồng không xác định thời hạn', 1),
    (2, 2, 'HD-2024-002', 'INDEFINITE', DATE '2024-02-01', NULL,
     25000000, 0, 1.0, 25000000, TRUE, 'ACTIVE', 'Hợp đồng không xác định thời hạn', 1),
    (3, 3, 'HD-2024-003', 'INDEFINITE', DATE '2024-03-01', NULL,
     28000000, 0, 1.0, 28000000, TRUE, 'ACTIVE', 'Hợp đồng không xác định thời hạn', 1),
    (4, 4, 'HD-2024-004', 'FIXED_TERM', DATE '2024-05-01', DATE '2026-12-31',
     18000000, 0, 1.0, 18000000, TRUE, 'ACTIVE', 'Hợp đồng lao động 2 năm', 2),
    (5, 5, 'HD-2024-005', 'FIXED_TERM', DATE '2024-06-01', DATE '2026-12-31',
     16000000, 0, 1.0, 16000000, TRUE, 'ACTIVE', 'Hợp đồng lao động 2 năm', 2)
ON CONFLICT (contract_id) DO NOTHING;

-- 5. Quy phep nam va don nghi phep
INSERT INTO leave_balance (id, employee_id, leave_year, accrued_days, used_days) VALUES
    (1, 1, 2026, 12.0, 0.0),
    (2, 2, 2026, 12.0, 1.0),
    (3, 3, 2026, 12.0, 2.0),
    (4, 4, 2026, 12.0, 3.0),
    (5, 5, 2026, 12.0, 1.5)
ON CONFLICT (id) DO NOTHING;

INSERT INTO leave_request (
    leave_request_id, employee_id, leave_type, start_date, end_date,
    duration_days, reason, status, submitted_at, decided_by, decided_at, decision_reason
) VALUES
    (1, 4, 'ANNUAL', DATE '2026-10-05', DATE '2026-10-05',
     1.0, 'Nghỉ giải quyết việc gia đình', 'APPROVED', NOW() - INTERVAL '15 days', 2, NOW() - INTERVAL '14 days', 'Đồng ý duyệt'),
    (2, 4, 'ANNUAL', CURRENT_DATE + INTERVAL '2 days', CURRENT_DATE + INTERVAL '3 days',
     2.0, 'Khám sức khỏe định kỳ', 'PENDING', NOW() - INTERVAL '1 hour', NULL, NULL, NULL)
ON CONFLICT (leave_request_id) DO NOTHING;

-- 6. Lich OT va dang ky OT
INSERT INTO overtime_schedule (
    schedule_id, title, description, start_at, end_at, required_employees, status, created_by
) VALUES
    (1, 'Deploy phiên bản tính lương v2.0', 'Triển khai release backend và kiểm tra tính toán thuế TNCN',
     NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 4 hours', 4, 'PUBLISHED', 1),
    (2, 'Tăng ca bảo trì hệ thống cuối tuần', 'Bảo trì định kỳ hạ tầng cơ sở dữ liệu PostgreSQL',
     NOW() + INTERVAL '4 days', NOW() + INTERVAL '4 days 8 hours', 2, 'PUBLISHED', 3)
ON CONFLICT (schedule_id) DO NOTHING;

INSERT INTO overtime_registration (
    registration_id, schedule_id, employee_id, status, approved_by, decision_reason
) VALUES
    (1, 1, 4, 'PENDING', NULL, NULL),
    (2, 1, 5, 'APPROVED', 3, 'Đã xác nhận phân công phụ trách backend')
ON CONFLICT (registration_id) DO NOTHING;

-- 7. Ky tinh luong va phieu luong
INSERT INTO payroll_period (
    id, period_code, period_name, start_date, end_date, payment_date, status, standard_working_days
) VALUES
    (1, '2026-09', 'Kỳ lương Tháng 09/2026', DATE '2026-09-01', DATE '2026-09-30', DATE '2026-10-05', 'LOCKED', 22),
    (2, '2026-10', 'Kỳ lương Tháng 10/2026', DATE '2026-10-01', DATE '2026-10-31', DATE '2026-11-05', 'LOCKED', 22),
    (3, '2026-11', 'Kỳ lương Tháng 11/2026', DATE '2026-11-01', DATE '2026-11-30', DATE '2026-12-05', 'DRAFT', 22)
ON CONFLICT (id) DO NOTHING;

INSERT INTO payslip (
    payslip_id, payroll_period_id, employee_id, status, base_salary,
    standard_working_days, actual_worked_days, paid_leave_days, unpaid_leave_days,
    gross_salary, bhxh_amount, bhyt_amount, bhtn_amount, total_employee_insurance,
    personal_deduction, taxable_income, pit_amount, other_deductions, period_adjustments,
    net_salary, carried_debt
) VALUES
    (1, 2, 4, 'ISSUED', 18000000,
     22, 21.0, 1.0, 0.0,
     18000000, 1440000, 270000, 180000, 1890000,
     11000000, 5110000, 255500, 0, 0,
     15854500, 0),
    (2, 2, 2, 'ISSUED', 25000000,
     22, 22.0, 0.0, 0.0,
     25000000, 2000000, 375000, 250000, 2625000,
     15400000, 6975000, 447500, 0, 0,
     21927500, 0)
ON CONFLICT (payslip_id) DO NOTHING;

INSERT INTO payroll_line (payroll_line_id, payslip_id, category, label, amount, calculation_note, sort_order) VALUES
    (1, 1, 'INCOME', 'Lương cơ bản theo ngày công', 18000000, '22 ngày công chuẩn', 1),
    (2, 1, 'INSURANCE', 'Bảo hiểm xã hội (8%)', 1440000, '8% lương đóng BH', 2),
    (3, 1, 'INSURANCE', 'Bảo hiểm y tế (1.5%)', 270000, '1.5% lương đóng BH', 3),
    (4, 1, 'INSURANCE', 'Bảo hiểm thất nghiệp (1%)', 180000, '1% lương đóng BH', 4),
    (5, 1, 'TAX', 'Thuế thu nhập cá nhân (TNCN)', 255500, 'Biểu lũy tiến từng phần', 5)
ON CONFLICT (payroll_line_id) DO NOTHING;

-- 8. Tham so he thong
INSERT INTO system_parameter (param_key, param_value, value_type, description, effective_from) VALUES
    ('STANDARD_MONTHLY_WORK_DAYS', '22', 'INTEGER', 'Số ngày làm việc tiêu chuẩn trong tháng', DATE '2026-01-01'),
    ('OVERTIME_WEEKDAY_MULTIPLIER', '1.5', 'DECIMAL', 'Hệ số tăng ca ngày làm việc bình thường', DATE '2026-01-01'),
    ('OVERTIME_WEEKLY_REST_MULTIPLIER', '2.0', 'DECIMAL', 'Hệ số tăng ca ngày nghỉ hàng tuần', DATE '2026-01-01'),
    ('OVERTIME_HOLIDAY_MULTIPLIER', '3.0', 'DECIMAL', 'Hệ số tăng ca ngày lễ Tết', DATE '2026-01-01')
ON CONFLICT (param_key, effective_from) DO NOTHING;

-- 9. Bang cham cong mau
INSERT INTO attendance (
    attendance_id, employee_id, work_date, check_in_at, check_out_at,
    late_minutes, early_minutes, worked_minutes, day_credit, status, source
) VALUES
    (1, 4, CURRENT_DATE - INTERVAL '1 day',
     (CURRENT_DATE - INTERVAL '1 day') + TIME '08:25:00',
     (CURRENT_DATE - INTERVAL '1 day') + TIME '17:35:00',
     0, 0, 480, 1.0, 'PRESENT', 'WEB'),
    (2, 2, CURRENT_DATE - INTERVAL '1 day',
     (CURRENT_DATE - INTERVAL '1 day') + TIME '08:28:00',
     (CURRENT_DATE - INTERVAL '1 day') + TIME '17:32:00',
     0, 0, 480, 1.0, 'PRESENT', 'WEB')
ON CONFLICT (attendance_id) DO NOTHING;

-- Cap nhat sequence cho tat ca cac bang
SELECT setval(pg_get_serial_sequence('department', 'department_id'), COALESCE(MAX(department_id), 1)) FROM department;
SELECT setval(pg_get_serial_sequence('employee', 'employee_id'), COALESCE(MAX(employee_id), 1)) FROM employee;
SELECT setval(pg_get_serial_sequence('app_user', 'id'), COALESCE(MAX(id), 1)) FROM app_user;
SELECT setval(pg_get_serial_sequence('contract', 'contract_id'), COALESCE(MAX(contract_id), 1)) FROM contract;
SELECT setval(pg_get_serial_sequence('leave_balance', 'id'), COALESCE(MAX(id), 1)) FROM leave_balance;
SELECT setval(pg_get_serial_sequence('leave_request', 'leave_request_id'), COALESCE(MAX(leave_request_id), 1)) FROM leave_request;
SELECT setval(pg_get_serial_sequence('overtime_schedule', 'schedule_id'), COALESCE(MAX(schedule_id), 1)) FROM overtime_schedule;
SELECT setval(pg_get_serial_sequence('overtime_registration', 'registration_id'), COALESCE(MAX(registration_id), 1)) FROM overtime_registration;
SELECT setval(pg_get_serial_sequence('payroll_period', 'id'), COALESCE(MAX(id), 1)) FROM payroll_period;
SELECT setval(pg_get_serial_sequence('payslip', 'payslip_id'), COALESCE(MAX(payslip_id), 1)) FROM payslip;
SELECT setval(pg_get_serial_sequence('payroll_line', 'payroll_line_id'), COALESCE(MAX(payroll_line_id), 1)) FROM payroll_line;
SELECT setval(pg_get_serial_sequence('attendance', 'attendance_id'), COALESCE(MAX(attendance_id), 1)) FROM attendance;
