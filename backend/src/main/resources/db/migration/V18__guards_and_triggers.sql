-- V18: Trigger bao ve du lieu (database_design.md Muc 6) va trigger updated_at.
-- Backend map APPEND_ONLY / PERIOD_LOCKED thanh HTTP 409.

-- Bang chi ghi them: chan UPDATE/DELETE.
CREATE OR REPLACE FUNCTION fn_block_modify_append_only() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'APPEND_ONLY: % khong cho phep %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = '55006';
END;
$$;

CREATE TRIGGER trg_approval_log_append_only BEFORE UPDATE OR DELETE ON approval_log
    FOR EACH ROW EXECUTE FUNCTION fn_block_modify_append_only();
CREATE TRIGGER trg_audit_log_append_only BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION fn_block_modify_append_only();
CREATE TRIGGER trg_payroll_audit_append_only BEFORE UPDATE OR DELETE ON payroll_audit
    FOR EACH ROW EXECUTE FUNCTION fn_block_modify_append_only();
CREATE TRIGGER trg_payslip_immutable BEFORE UPDATE OR DELETE ON payslip
    FOR EACH ROW EXECUTE FUNCTION fn_block_modify_append_only();
CREATE TRIGGER trg_snapshot_immutable BEFORE UPDATE OR DELETE ON timesheet_snapshot
    FOR EACH ROW EXECUTE FUNCTION fn_block_modify_append_only();

-- Dong luong: khong them/sua/xoa khi ky APPROVED/LOCKED.
CREATE OR REPLACE FUNCTION fn_guard_payroll_line() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_status VARCHAR(30);
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        SELECT status INTO v_status FROM payroll_period WHERE id = OLD.period_id;
        IF v_status IN ('APPROVED', 'LOCKED') THEN
            RAISE EXCEPTION 'PERIOD_LOCKED: ky % dang %', OLD.period_id, v_status USING ERRCODE = '55006';
        END IF;
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT status INTO v_status FROM payroll_period WHERE id = NEW.period_id;
        IF v_status IN ('APPROVED', 'LOCKED') THEN
            RAISE EXCEPTION 'PERIOD_LOCKED: ky % dang %', NEW.period_id, v_status USING ERRCODE = '55006';
        END IF;
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_payroll_line_guard BEFORE INSERT OR UPDATE OR DELETE ON payroll_line
    FOR EACH ROW EXECUTE FUNCTION fn_guard_payroll_line();

CREATE OR REPLACE FUNCTION fn_guard_payroll_line_item() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_status VARCHAR(30);
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        SELECT pp.status INTO v_status
        FROM payroll_line pl JOIN payroll_period pp ON pp.id = pl.period_id
        WHERE pl.id = OLD.line_id;
        IF v_status IN ('APPROVED', 'LOCKED') THEN
            RAISE EXCEPTION 'PERIOD_LOCKED: dong luong % thuoc ky %', OLD.line_id, v_status USING ERRCODE = '55006';
        END IF;
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT pp.status INTO v_status
        FROM payroll_line pl JOIN payroll_period pp ON pp.id = pl.period_id
        WHERE pl.id = NEW.line_id;
        IF v_status IN ('APPROVED', 'LOCKED') THEN
            RAISE EXCEPTION 'PERIOD_LOCKED: dong luong % thuoc ky %', NEW.line_id, v_status USING ERRCODE = '55006';
        END IF;
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$;

CREATE TRIGGER trg_payroll_line_item_guard BEFORE INSERT OR UPDATE OR DELETE ON payroll_line_item
    FOR EACH ROW EXECUTE FUNCTION fn_guard_payroll_line_item();

-- Ky luong:
--  * chi xoa duoc ky DRAFT;
--  * chuyen trang thai hop le: DRAFT->CALCULATED, CALCULATED->DRAFT/APPROVED, APPROVED->LOCKED;
--  * ky APPROVED chi duoc chuyen sang LOCKED (khong mo lai/tinh lai);
--  * ky LOCKED chi cho ghi nhan chi tra va thoi diem phat hanh phieu luong.
CREATE OR REPLACE FUNCTION fn_guard_payroll_period() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    v_changed TEXT[];
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status <> 'DRAFT' THEN
            RAISE EXCEPTION 'PERIOD_LOCKED: chi xoa duoc ky DRAFT (ky % dang %)', OLD.id, OLD.status USING ERRCODE = '55006';
        END IF;
        RETURN OLD;
    END IF;

    IF NEW.status <> OLD.status AND NOT (
           (OLD.status = 'DRAFT'      AND NEW.status = 'CALCULATED')
        OR (OLD.status = 'CALCULATED' AND NEW.status IN ('DRAFT', 'APPROVED'))
        OR (OLD.status = 'APPROVED'   AND NEW.status = 'LOCKED')) THEN
        RAISE EXCEPTION 'PERIOD_LOCKED: khong chuyen % -> %', OLD.status, NEW.status USING ERRCODE = '55006';
    END IF;

    SELECT array_agg(n.key) INTO v_changed
    FROM jsonb_each(to_jsonb(NEW)) n
    JOIN jsonb_each(to_jsonb(OLD)) o ON o.key = n.key
    WHERE n.value IS DISTINCT FROM o.value
      AND n.key NOT IN ('updated_at', 'version');

    IF OLD.status = 'APPROVED' AND v_changed IS NOT NULL
       AND (NEW.status <> 'LOCKED'
            OR NOT v_changed <@ ARRAY['status', 'locked_by', 'locked_at']) THEN
        RAISE EXCEPTION 'PERIOD_LOCKED: ky APPROVED chi duoc khoa (thay doi: %)', v_changed USING ERRCODE = '55006';
    END IF;

    IF OLD.status = 'LOCKED' AND v_changed IS NOT NULL
       AND NOT v_changed <@ ARRAY['payment_status', 'paid_by', 'paid_at', 'payment_ref', 'published_at'] THEN
        RAISE EXCEPTION 'PERIOD_LOCKED: ky LOCKED chi ghi nhan chi tra (thay doi: %)', v_changed USING ERRCODE = '55006';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_payroll_period_guard BEFORE UPDATE OR DELETE ON payroll_period
    FOR EACH ROW EXECUTE FUNCTION fn_guard_payroll_period();

-- updated_at tu dong (ham trg_set_updated_at tu V1).
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'department', 'job_title', 'employee', 'user_account', 'account_request',
        'approval_route', 'contract', 'allowance_type', 'dependent',
        'work_schedule', 'holiday', 'attendance_point', 'attendance_record',
        'attendance_adjustment', 'leave_type', 'leave_request', 'leave_balance',
        'ot_schedule', 'ot_registration', 'ot_unscheduled', 'income_item',
        'sales_revenue', 'payroll_period', 'payroll_adjustment',
        'notification_preference', 'background_job'
    ]
    LOOP
        EXECUTE format('CREATE TRIGGER trg_%s_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at()', t, t);
    END LOOP;
END;
$$;
