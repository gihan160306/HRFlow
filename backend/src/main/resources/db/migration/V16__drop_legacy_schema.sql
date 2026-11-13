-- V16: Go bo schema cu (V2-V15) de dung lai theo docs/database_design.md (ban 3.4).
-- Khong sua migration da chay; toan bo bang/ham cu duoc xoa tai day va tao lai o V17-V20.
-- Ham trg_set_updated_at() (V1) duoc giu lai de dung chung.

DROP TABLE IF EXISTS
    payroll_audit,
    payroll_permission,
    payroll_line,
    payslip,
    payroll_adjustment,
    kpi_result,
    revenue_record,
    commission_rate,
    approval_log,
    overtime_registration,
    overtime_schedule,
    leave_balance,
    leave_request,
    attendance,
    notification,
    audit_log,
    employee_policy_assignment,
    allowance_deduction_policy,
    allowance_type,
    calendar_exception,
    tax_bracket,
    tax_schedule,
    payroll_parameter,
    system_parameter,
    payroll_period,
    termination_record,
    contract,
    app_user,
    employee,
    department
CASCADE;

DROP FUNCTION IF EXISTS reject_final_payroll_edit() CASCADE;
DROP FUNCTION IF EXISTS guard_payroll_state() CASCADE;
DROP FUNCTION IF EXISTS reject_payroll_audit_edit() CASCADE;
