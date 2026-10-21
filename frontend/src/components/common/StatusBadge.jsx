import React from 'react';

const STATUS_CONFIGS = {
  // Common
  ACTIVE: { bg: 'bg-emerald-50 text-emerald-700 border-emerald-200', label: 'Hoạt động' },
  INACTIVE: { bg: 'bg-slate-100 text-slate-600 border-slate-200', label: 'Không hoạt động' },
  PENDING: { bg: 'bg-amber-50 text-amber-700 border-amber-200', label: 'Chờ duyệt' },
  APPROVED: { bg: 'bg-blue-50 text-blue-700 border-blue-200', label: 'Đã duyệt' },
  REJECTED: { bg: 'bg-red-50 text-red-700 border-red-200', label: 'Từ chối' },
  DRAFT: { bg: 'bg-gray-100 text-gray-700 border-gray-200', label: 'Bản nháp' },
  EXPIRED: { bg: 'bg-red-50 text-red-600 border-red-200', label: 'Hết hạn' },
  TERMINATED: { bg: 'bg-rose-50 text-rose-700 border-rose-200', label: 'Đã chấm dứt' },
  
  // Attendance
  PRESENT: { bg: 'bg-emerald-50 text-emerald-700 border-emerald-200', label: 'Đúng giờ' },
  LATE: { bg: 'bg-amber-50 text-amber-700 border-amber-200', label: 'Đi muộn' },
  EARLY_LEAVE: { bg: 'bg-orange-50 text-orange-700 border-orange-200', label: 'Về sớm' },
  ABSENT: { bg: 'bg-red-50 text-red-700 border-red-200', label: 'Vắng mặt' },
  ON_LEAVE: { bg: 'bg-indigo-50 text-indigo-700 border-indigo-200', label: 'Nghỉ phép' },

  // Payroll
  CALCULATED: { bg: 'bg-cyan-50 text-cyan-700 border-cyan-200', label: 'Đã tính' },
  PAID: { bg: 'bg-emerald-50 text-emerald-700 border-emerald-200', label: 'Đã thanh toán' },
};

export function StatusBadge({ status, label, className = '' }) {
  const normalizedKey = String(status || '').toUpperCase();
  const config = STATUS_CONFIGS[normalizedKey] || {
    bg: 'bg-slate-100 text-slate-700 border-slate-200',
    label: label || status || 'N/A',
  };

  const displayLabel = label || config.label;

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${config.bg} ${className}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current mr-1.5 opacity-75"></span>
      {displayLabel}
    </span>
  );
}

export default StatusBadge;
