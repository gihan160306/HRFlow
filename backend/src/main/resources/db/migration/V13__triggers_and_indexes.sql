-- V13: Trigger updated_at va cac index bo sung.
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'department', 'employee', 'app_user', 'contract', 'payroll_parameter',
        'system_parameter', 'allowance_deduction_policy', 'payroll_period',
        'attendance', 'leave_request', 'leave_balance', 'overtime_schedule',
        'overtime_registration', 'revenue_record', 'kpi_result',
        'payroll_adjustment', 'payslip'
    ]
    LOOP
        EXECUTE format('CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at()', t, t);
    END LOOP;
END;
$$;
