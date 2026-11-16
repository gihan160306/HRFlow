import React from 'react';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate, formatTime, formatMinutes } from '../../utils/formatDate';

export function AttendanceHistory({ records = [], showEmployee = false, onConfirm }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
      <div className="p-4 sm:p-6 border-b border-slate-100">
        <h3 className="text-base font-bold text-slate-800">Lịch sử chấm công</h3>
      </div>
      {records.length === 0 ? (
        <p className="p-6 text-sm text-slate-500">Chưa có bản ghi chấm công trong khoảng này.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3.5">Ngày</th>
                {showEmployee && <th className="px-4 py-3.5">Nhân viên</th>}
                <th className="px-4 py-3.5">Giờ vào</th>
                <th className="px-4 py-3.5">Giờ ra</th>
                <th className="px-4 py-3.5">Giờ làm</th>
                <th className="px-4 py-3.5">Muộn / Sớm</th>
                <th className="px-4 py-3.5">Trạng thái</th>
                {onConfirm && <th className="px-4 py-3.5" />}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {records.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/80">
                  <td className="px-4 py-3.5 font-semibold text-slate-900">{formatDate(r.workDate)}</td>
                  {showEmployee && <td className="px-4 py-3.5">{r.employeeName}<span className="block text-xs text-slate-400">{r.employeeCode}</span></td>}
                  <td className="px-4 py-3.5 font-mono">{formatTime(r.checkInAt)}</td>
                  <td className="px-4 py-3.5 font-mono">{formatTime(r.checkOutAt)}</td>
                  <td className="px-4 py-3.5">{formatMinutes(r.workedMinutes)}</td>
                  <td className="px-4 py-3.5 text-xs">{r.lateMinutes || 0}p / {r.earlyMinutes || 0}p</td>
                  <td className="px-4 py-3.5">
                    <StatusBadge status={r.status} />
                    {r.reviewReason && <span className="block text-xs text-amber-700 mt-1">{r.reviewReason}</span>}
                  </td>
                  {onConfirm && (
                    <td className="px-4 py-3.5 text-right">
                      {r.status === 'NEEDS_REVIEW' && onConfirm(r)}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default AttendanceHistory;
