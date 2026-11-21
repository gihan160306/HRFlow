-- V25: Cap grant payroll VIEW cho Finance Manager (NV003) de doi soat tai chinh.
-- Baseline: company_profile.md Muc lich payroll (ngay 04) va permission_matrix.md quy tac cap quyen payroll:
-- "HR chuan bi/tinh/ra soat, Finance Manager doi soat tai chinh". Doi soat (POST /payroll/periods/{id}/reconcile)
-- yeu cau grant VIEW; V20 chi cap LOCK cho NV003 nen Finance Manager khong doi soat duoc.
-- VIEW va LOCK van doc lap (DEC-03): grant moi khong cho them quyen PREPARE/APPROVE.

INSERT INTO payroll_grant (account_id, permission, valid_from, reason, decision_ref, granted_by)
SELECT a.id, 'VIEW', TIMESTAMPTZ '2026-07-01 00:00:00+07', 'Finance Manager đối soát tài chính bảng lương', 'DEC-03', admin.id
FROM user_account a
JOIN employee e ON e.id = a.employee_id
CROSS JOIN (SELECT id FROM user_account WHERE email = 'admin@hnqmedia.example') admin
WHERE e.code = 'NV003'
  AND NOT EXISTS (
      SELECT 1 FROM payroll_grant g
      WHERE g.account_id = a.id AND g.permission = 'VIEW' AND g.revoked_at IS NULL AND g.period_id IS NULL
  );
