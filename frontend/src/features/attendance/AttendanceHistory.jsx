import React from 'react';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate } from '../../utils/formatDate';

export function AttendanceHistory({ records = [] }) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
      <div className="p-4 sm:p-6 border-b border-slate-100 flex items-center justify-between">
        <div>
          <h3 className="text-base font-bold text-slate-800">
            Lịch sử chấm công gần đây
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Dữ liệu ghi nhận từ máy chấm công & GPS mobile
          </p>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-slate-600">
          <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
            <tr>
              <th className="px-4 py-3.5">Ngày làm việc</th>
              <th className="px-4 py-3.5">Giờ vào</th>
              <th className="px-4 py-3.5">Giờ ra</th>
              <th className="px-4 py-3.5">Tổng giờ công</th>
              <th className="px-4 py-3.5">Trạng thái</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {records.map((rec) => (
              <tr key={rec.id} className="hover:bg-slate-50/80 transition-colors">
                <td className="px-4 py-3.5 font-semibold text-slate-900">
                  {formatDate(rec.date)}
                </td>
                <td className="px-4 py-3.5 font-mono text-slate-700">
                  {rec.checkIn || '--:--'}
                </td>
                <td className="px-4 py-3.5 font-mono text-slate-700">
                  {rec.checkOut || '--:--'}
                </td>
                <td className="px-4 py-3.5 font-medium text-slate-900">
                  {rec.workHours ? `${rec.workHours} giờ` : '-'}
                </td>
                <td className="px-4 py-3.5">
                  <StatusBadge status={rec.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default AttendanceHistory;
