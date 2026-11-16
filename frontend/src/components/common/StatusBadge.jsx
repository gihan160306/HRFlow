import React from 'react';

const GREEN = 'bg-emerald-50 text-emerald-700 border-emerald-200';
const AMBER = 'bg-amber-50 text-amber-700 border-amber-200';
const BLUE = 'bg-blue-50 text-blue-700 border-blue-200';
const RED = 'bg-red-50 text-red-700 border-red-200';
const GRAY = 'bg-slate-100 text-slate-600 border-slate-200';
const CYAN = 'bg-cyan-50 text-cyan-700 border-cyan-200';

// Trạng thái khớp CHECK constraint trong V17__core_schema.sql.
const STATUS_CONFIGS = {
  // employee
  ONBOARDING: { bg: AMBER, label: 'Đang tiếp nhận' },
  ACTIVE: { bg: GREEN, label: 'Đang làm việc' },
  ON_LEAVE: { bg: BLUE, label: 'Đang nghỉ' },
  TERMINATED: { bg: RED, label: 'Đã nghỉ việc' },
  // contract
  DRAFT: { bg: GRAY, label: 'Bản nháp' },
  EXPIRED: { bg: RED, label: 'Hết hạn' },
  // approval flows (leave / OT / adjustments)
  PENDING: { bg: AMBER, label: 'Chờ duyệt' },
  APPROVED: { bg: BLUE, label: 'Đã duyệt' },
  REJECTED: { bg: RED, label: 'Từ chối' },
  CANCELLED: { bg: GRAY, label: 'Đã hủy' },
  CONFIRMED: { bg: GREEN, label: 'Đã xác nhận' },
  // OT schedule
  PUBLISHED: { bg: GREEN, label: 'Đã công bố' },
  CLOSED: { bg: GRAY, label: 'Đã đóng' },
  // attendance
  VALID: { bg: GREEN, label: 'Hợp lệ' },
  NEEDS_REVIEW: { bg: AMBER, label: 'Cần rà soát' },
  ABSENT_CONFIRMED: { bg: RED, label: 'Vắng (đã xác nhận)' },
  // payroll
  CALCULATED: { bg: CYAN, label: 'Đã tính' },
  LOCKED: { bg: GREEN, label: 'Đã khóa' },
  OK: { bg: GREEN, label: 'Hợp lệ' },
  PAID: { bg: GREEN, label: 'Đã chi trả' },
  UNPAID: { bg: AMBER, label: 'Chưa chi trả' },
};

export function StatusBadge({ status, label, className = '' }) {
  const key = String(status || '').toUpperCase();
  const config = STATUS_CONFIGS[key] || { bg: GRAY, label: status || 'N/A' };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${config.bg} ${className}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current mr-1.5 opacity-75" />
      {label || config.label}
    </span>
  );
}

export default StatusBadge;
