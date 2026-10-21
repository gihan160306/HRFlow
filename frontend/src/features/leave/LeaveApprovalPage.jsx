import React, { useState } from 'react';
import { Check, X, Calendar, UserCheck } from 'lucide-react';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate } from '../../utils/formatDate';

const SAMPLE_PENDING_LEAVES = [
  {
    id: 1,
    employeeName: 'Nguyễn Hoàng Phương Ngân',
    department: 'Khối Kỹ Thuật & Công Nghệ',
    leaveType: 'Nghỉ phép năm',
    startDate: '2026-10-22',
    endDate: '2026-10-23',
    daysCount: 2,
    reason: 'Giải quyết việc gia đình',
    status: 'PENDING',
  },
  {
    id: 2,
    employeeName: 'Hồ Vinh Quan',
    department: 'Khối Kỹ Thuật & Công Nghệ',
    leaveType: 'Nghỉ ốm đau',
    startDate: '2026-10-18',
    endDate: '2026-10-19',
    daysCount: 2,
    reason: 'Khám sức khỏe định kỳ có giấy bệnh viện',
    status: 'PENDING',
  },
];

export function LeaveApprovalPage() {
  const [requests, setRequests] = useState(SAMPLE_PENDING_LEAVES);

  const handleAction = (id, status) => {
    setRequests((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status } : r))
    );
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
          Xét duyệt Đơn Nghỉ phép
        </h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Dành cho Quản lý trực tiếp (Manager) & Phòng Nhân sự (HR)
        </p>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100 flex items-center justify-between">
          <h3 className="text-base font-bold text-slate-800 flex items-center">
            <UserCheck className="w-5 h-5 mr-2 text-blue-600" />
            Danh sách đơn cần duyệt ({requests.filter((r) => r.status === 'PENDING').length})
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3.5">Nhân viên</th>
                <th className="px-4 py-3.5">Loại phép & Lý do</th>
                <th className="px-4 py-3.5">Thời gian</th>
                <th className="px-4 py-3.5">Số ngày</th>
                <th className="px-4 py-3.5">Trạng thái</th>
                <th className="px-4 py-3.5 text-right">Phê duyệt</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {requests.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="px-4 py-3.5">
                    <p className="font-bold text-slate-900">{r.employeeName}</p>
                    <p className="text-xs text-slate-500">{r.department}</p>
                  </td>
                  <td className="px-4 py-3.5">
                    <p className="font-semibold text-slate-800">{r.leaveType}</p>
                    <p className="text-xs text-slate-500 italic">"{r.reason}"</p>
                  </td>
                  <td className="px-4 py-3.5 text-xs text-slate-600">
                    {formatDate(r.startDate)} - {formatDate(r.endDate)}
                  </td>
                  <td className="px-4 py-3.5 font-bold text-slate-900">
                    {r.daysCount} ngày
                  </td>
                  <td className="px-4 py-3.5">
                    <StatusBadge status={r.status} />
                  </td>
                  <td className="px-4 py-3.5 text-right">
                    {r.status === 'PENDING' ? (
                      <div className="flex items-center justify-end space-x-2">
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={() => handleAction(r.id, 'APPROVED')}
                          icon={Check}
                        >
                          Duyệt
                        </Button>
                        <Button
                          size="sm"
                          variant="danger"
                          onClick={() => handleAction(r.id, 'REJECTED')}
                          icon={X}
                        >
                          Từ chối
                        </Button>
                      </div>
                    ) : (
                      <span className="text-xs text-slate-400 italic">Đã xử lý</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default LeaveApprovalPage;
