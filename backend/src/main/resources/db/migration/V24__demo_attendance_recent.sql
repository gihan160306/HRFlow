-- V24: Du lieu demo cham cong 30 ngay gan nhat (22/10 -> 20/11/2026).
-- 22–31/10 da co trong V23 (ky 10 da khoa); file nay bo sung 02–20/11/2026 cho ky 11 (DRAFT) de demo:
--   * cham cong tai quay hang ngay (dung gio / di tre / ve som), nghi phep da duyet (ca ngay, nua ngay, om, khong luong),
--     cong tac, dieu chinh thu cong da duyet, vang khong phep;
--   * cac truong hop CON CHO XU LY: quen check-out (tu chuyen "can ra soat"), ban ghi NEEDS_REVIEW,
--     yeu cau dieu chinh cong dang cho duyet, don nghi phep dang cho duyet (ngay tuong lai).
-- Ban ghi da ton tai (vd. nguoi dung da thu cham cong bang KIOSK) duoc giu nguyen (ON CONFLICT DO NOTHING).

-- =====================================================================
-- 1. Nghi phep thang 11
-- =====================================================================
CREATE TEMP TABLE demo_leave11 (
    code     VARCHAR(20),
    ltype    VARCHAR(30),
    unit     VARCHAR(30),
    d_from   DATE,
    d_to     DATE,
    status   VARCHAR(30),
    reason   VARCHAR(300),
    evidence VARCHAR(200)
) ON COMMIT DROP;
INSERT INTO demo_leave11 VALUES
    ('NV011', 'ANNUAL',        'FULL_DAY',    DATE '2026-11-09', DATE '2026-11-10', 'APPROVED', 'Về quê dự đám cưới người thân', NULL),
    ('NV026', 'SICK',          'FULL_DAY',    DATE '2026-11-12', DATE '2026-11-12', 'APPROVED', 'Viêm họng sốt cao', 'Giấy khám bệnh'),
    ('NV038', 'ANNUAL',        'HALF_DAY_PM', DATE '2026-11-13', DATE '2026-11-13', 'APPROVED', 'Đi làm thủ tục ngân hàng', NULL),
    ('NV063', 'UNPAID',        'FULL_DAY',    DATE '2026-11-16', DATE '2026-11-17', 'APPROVED', 'Việc gia đình', NULL),
    ('NV088', 'ANNUAL',        'FULL_DAY',    DATE '2026-11-20', DATE '2026-11-20', 'APPROVED', 'Nghỉ ngày 20/11 đưa con đi diễn văn nghệ', NULL),
    ('NV014', 'ANNUAL',        'FULL_DAY',    DATE '2026-11-06', DATE '2026-11-06', 'APPROVED', 'Thi cuối kỳ', NULL),
    ('NV006', 'PERSONAL_PAID', 'FULL_DAY',    DATE '2026-11-19', DATE '2026-11-20', 'APPROVED', 'Kết hôn', 'Giấy chứng nhận kết hôn'),
    ('NV098', 'ANNUAL',        'HALF_DAY_AM', DATE '2026-11-18', DATE '2026-11-18', 'APPROVED', 'Khám sức khỏe định kỳ', NULL),
    -- dang cho duyet (ngay tuong lai)
    ('NV021', 'ANNUAL',        'FULL_DAY',    DATE '2026-11-26', DATE '2026-11-27', 'PENDING',  'Du lịch cuối năm', NULL),
    ('NV055', 'ANNUAL',        'FULL_DAY',    DATE '2026-11-30', DATE '2026-11-30', 'PENDING',  'Việc cá nhân', NULL),
    ('NV037', 'PERSONAL_PAID', 'FULL_DAY',    DATE '2026-11-24', DATE '2026-11-24', 'PENDING',  'Đám cưới em ruột', 'Thiệp cưới');

INSERT INTO leave_request (employee_id, leave_type_id, kind, unit, start_date, end_date, duration_minutes, duration_days, reason, evidence,
                           status, primary_approver_id, backup_approver_id, submitted_at, decided_by, decided_at, decision_reason)
SELECT e.id, lt.id, 'NEW', l.unit, l.d_from, l.d_to, 0, 0, l.reason, l.evidence, l.status,
       COALESCE(ar.primary_approver_id, e.manager_id), ar.backup_approver_id,
       LEAST(l.d_from - 4, DATE '2026-11-19')::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '9 hours',
       CASE WHEN l.status = 'APPROVED' THEN COALESCE(ar.primary_approver_id, e.manager_id) END,
       CASE WHEN l.status = 'APPROVED' THEN (l.d_from - 3)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '10 hours' END,
       CASE WHEN l.status = 'APPROVED' THEN 'Đồng ý' END
FROM demo_leave11 l
JOIN employee e ON e.code = l.code
JOIN leave_type lt ON lt.code = l.ltype
LEFT JOIN LATERAL (SELECT * FROM approval_route r WHERE r.requester_id = e.id ORDER BY r.effective_from DESC LIMIT 1) ar ON TRUE;

INSERT INTO leave_request_day (leave_request_id, leave_date, minutes, day_fraction)
SELECT r.id, d::date,
       CASE WHEN ws.code = 'PT_MORNING' THEN 240
            WHEN r.unit = 'HALF_DAY_AM' THEN 210
            WHEN r.unit = 'HALF_DAY_PM' THEN 270
            ELSE 480 END,
       CASE WHEN r.unit IN ('HALF_DAY_AM', 'HALF_DAY_PM') THEN 0.5 ELSE 1 END
FROM leave_request r
JOIN demo_leave11 l ON l.code = (SELECT code FROM employee WHERE id = r.employee_id) AND l.d_from = r.start_date
JOIN employee_schedule es ON es.employee_id = r.employee_id
JOIN work_schedule ws ON ws.id = es.schedule_id
CROSS JOIN LATERAL generate_series(r.start_date, r.end_date, INTERVAL '1 day') d
WHERE EXTRACT(ISODOW FROM d) <= 5
  AND d::date NOT IN (SELECT holiday_date FROM holiday WHERE paid);

UPDATE leave_request r SET duration_minutes = x.m, duration_days = x.f
FROM (SELECT leave_request_id, SUM(minutes) AS m, SUM(day_fraction) AS f FROM leave_request_day GROUP BY 1) x
WHERE x.leave_request_id = r.id AND r.duration_days = 0 AND r.start_date >= DATE '2026-11-01';

INSERT INTO approval_log (entity_type, entity_id, action, actor_account_id, actor_name, from_status, to_status, reason)
SELECT 'LEAVE_REQUEST', r.id, 'APPROVE', a.id, ap.full_name, 'PENDING', 'APPROVED', 'Đồng ý'
FROM leave_request r
JOIN demo_leave11 l ON l.code = (SELECT code FROM employee WHERE id = r.employee_id) AND l.d_from = r.start_date AND l.status = 'APPROVED'
JOIN employee ap ON ap.id = r.decided_by
JOIN user_account a ON a.employee_id = ap.id;

INSERT INTO leave_balance_txn (balance_id, txn_type, delta_days, leave_request_id, reason, created_by)
SELECT b.id, 'USED', -r.duration_days, r.id, 'Duyệt đơn nghỉ', 4
FROM leave_request r
JOIN demo_leave11 l ON l.code = (SELECT code FROM employee WHERE id = r.employee_id) AND l.d_from = r.start_date
                   AND l.ltype = 'ANNUAL' AND l.status = 'APPROVED'
JOIN leave_balance b ON b.employee_id = r.employee_id AND b.balance_year = 2026;
UPDATE leave_balance b SET used_days = used_days + x.days
FROM (SELECT r.employee_id, SUM(r.duration_days) AS days
      FROM leave_request r JOIN demo_leave11 l ON l.code = (SELECT code FROM employee WHERE id = r.employee_id) AND l.d_from = r.start_date
      WHERE l.ltype = 'ANNUAL' AND l.status = 'APPROVED' GROUP BY 1) x
WHERE b.employee_id = x.employee_id AND b.balance_year = 2026;

-- =====================================================================
-- 2. Cham cong 02–20/11/2026
-- =====================================================================
-- kind: ABSENCE (vang khong phep da xac nhan), TRIP (cong tac), MANUAL (HR da dieu chinh),
--       NO_OUT (quen check-out -> he thong tu danh dau can ra soat), REVIEW (ban ghi NEEDS_REVIEW), MISSING (khong co ban ghi)
CREATE TEMP TABLE demo_att11 (
    code VARCHAR(20),
    d    DATE,
    kind VARCHAR(20),
    note VARCHAR(300)
) ON COMMIT DROP;
INSERT INTO demo_att11 VALUES
    ('NV085', DATE '2026-11-11', 'ABSENCE', 'Vắng không phép - đã lập biên bản'),
    ('NV033', DATE '2026-11-05', 'TRIP',    'Công tác Cần Thơ - khảo sát điểm bán khách hàng'),
    ('NV033', DATE '2026-11-06', 'TRIP',    'Công tác Cần Thơ - khảo sát điểm bán khách hàng'),
    ('NV042', DATE '2026-11-04', 'MANUAL',  'Quên check-out, HR Lead xác nhận làm đến 17:40'),
    ('NV071', DATE '2026-11-10', 'MANUAL',  'Máy quầy mất kết nối lúc check-in, HR điều chỉnh theo camera'),
    ('NV019', DATE '2026-11-18', 'NO_OUT',  NULL),
    ('NV047', DATE '2026-11-19', 'NO_OUT',  NULL),
    ('NV092', DATE '2026-11-20', 'NO_OUT',  NULL),
    ('NV077', DATE '2026-11-17', 'REVIEW',  'Ảnh check-in không rõ mặt, cần đối chiếu'),
    ('NV059', DATE '2026-11-13', 'MISSING', NULL);

WITH days AS (
    SELECT d::date AS work_date FROM generate_series(DATE '2026-11-01', DATE '2026-11-20', INTERVAL '1 day') d
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
    LEFT JOIN demo_att11 s ON s.code = e.code AND s.d = d.work_date
    WHERE (l.unit IS NULL OR l.unit IN ('HALF_DAY_AM', 'HALF_DAY_PM'))
      AND s.kind IS DISTINCT FROM 'MISSING'
),
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
           CASE WHEN iv.kind IN ('ABSENCE', 'TRIP') THEN NULL
                WHEN iv.leave_unit = 'HALF_DAY_AM' THEN 765 + iv.r1 % 15
                WHEN iv.r1 < 60 THEN 515 + iv.r1 % 35
                ELSE 485 + iv.r1 % 25 END AS in_m,
           CASE WHEN iv.kind IN ('ABSENCE', 'TRIP', 'NO_OUT') THEN NULL
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
       CASE WHEN t.in_m IS NOT NULL AND t.kind IS DISTINCT FROM 'MANUAL' THEN 1 END,
       CASE WHEN t.out_m IS NOT NULL AND t.kind IS NULL THEN 1 END,
       CASE WHEN t.in_m IS NOT NULL AND t.kind IS DISTINCT FROM 'MANUAL' THEN (SELECT id FROM user_account WHERE role = 'KIOSK' ORDER BY id LIMIT 1) END,
       CASE WHEN t.out_m IS NOT NULL AND t.kind IS NULL THEN (SELECT id FROM user_account WHERE role = 'KIOSK' ORDER BY id LIMIT 1) END,
       CASE t.kind WHEN 'ABSENCE' THEN 'ABSENCE' WHEN 'TRIP' THEN 'BUSINESS_TRIP' WHEN 'MANUAL' THEN 'MANUAL' ELSE 'QR' END,
       t.sched,
       CASE WHEN t.kind IN ('ABSENCE', 'NO_OUT') THEN 0
            WHEN t.kind = 'TRIP' THEN (t.me - t.ms) + (t.ae - t.as_)
            ELSE GREATEST(0, LEAST(t.out_m, t.me) - GREATEST(t.in_m, t.ms)) + GREATEST(0, LEAST(t.out_m, t.ae) - GREATEST(t.in_m, t.as_)) END,
       CASE WHEN t.in_m IS NULL THEN 0
            ELSE GREATEST(0, LEAST(t.in_m, t.me) - t.ms) + GREATEST(0, LEAST(t.in_m, t.ae) - t.as_) END,
       CASE WHEN t.out_m IS NULL THEN 0
            ELSE GREATEST(0, t.me - GREATEST(t.out_m, t.ms)) + GREATEST(0, t.ae - GREATEST(t.out_m, t.as_)) END,
       CASE t.kind WHEN 'ABSENCE' THEN 'ABSENT_CONFIRMED' WHEN 'TRIP' THEN 'CONFIRMED' WHEN 'MANUAL' THEN 'CONFIRMED'
                   WHEN 'REVIEW' THEN 'NEEDS_REVIEW' ELSE 'VALID' END,
       CASE WHEN t.kind = 'REVIEW' THEN t.note END,
       CASE WHEN t.kind IN ('ABSENCE', 'TRIP', 'MANUAL') THEN 4 END,
       CASE WHEN t.kind IN ('ABSENCE', 'TRIP', 'MANUAL') THEN (t.work_date + 1)::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh' + INTERVAL '9 hours' END,
       CASE WHEN t.kind <> 'REVIEW' THEN t.note END
FROM tm t
ON CONFLICT (employee_id, work_date) DO NOTHING;

-- Dieu chinh cong: da duyet (cong tac, HR dieu chinh) va DANG CHO DUYET (NV019 xin bo sung gio check-out)
INSERT INTO attendance_adjustment (attendance_id, employee_id, work_date, proposed_check_in, proposed_check_out, method, reason, evidence,
                                   status, requested_by, decided_by, decided_at, decision_note)
SELECT ar.id, ar.employee_id, ar.work_date, ar.check_in_at,
       CASE WHEN s.kind = 'NO_OUT' THEN (s.d::timestamp + TIME '17:45') AT TIME ZONE 'Asia/Ho_Chi_Minh' ELSE ar.check_out_at END,
       CASE WHEN s.kind = 'NO_OUT' THEN 'MANUAL' ELSE ar.method END,
       CASE WHEN s.kind = 'NO_OUT' THEN 'Quên quét QR khi ra về, xin bổ sung giờ check-out 17:45' ELSE s.note END,
       CASE s.kind WHEN 'TRIP' THEN 'Quyết định cử đi công tác' WHEN 'NO_OUT' THEN 'Tin nhắn báo cáo cuối ngày gửi Lead lúc 17:50'
                   ELSE 'Xác nhận của quản lý trực tiếp' END,
       CASE WHEN s.kind = 'NO_OUT' THEN 'PENDING' ELSE 'APPROVED' END,
       ua.id,
       CASE WHEN s.kind = 'NO_OUT' THEN NULL ELSE 4 END,
       CASE WHEN s.kind = 'NO_OUT' THEN NULL ELSE ar.confirmed_at END,
       CASE WHEN s.kind = 'NO_OUT' THEN NULL ELSE 'Đã đối chiếu' END
FROM demo_att11 s
JOIN employee e ON e.code = s.code
JOIN attendance_record ar ON ar.employee_id = e.id AND ar.work_date = s.d
JOIN user_account ua ON ua.employee_id = e.id
WHERE s.kind IN ('TRIP', 'MANUAL') OR (s.kind = 'NO_OUT' AND s.code = 'NV019');
