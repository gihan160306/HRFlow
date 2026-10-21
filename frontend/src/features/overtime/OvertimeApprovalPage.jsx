import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, X, ArrowLeft, UserCheck } from 'lucide-react';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate } from '../../utils/formatDate';

const PENDING_OT_LIST = [
  {
    id: 1,
    employeeName: 'Nguyễn Hoàng Phương Ngân',
    department: 'Khối Kỹ Thuật & Công Nghệ',
    date: '2026-10-21',
    hours: 4.0,
    rate: '1.5x (Ngày thường)',
    task: 'Deploy phiên bản tính lương v2.0',
    status: 'PENDING',
  },
  {
    id: 2,
    employeeName: 'Hồ Vinh Quan',
    department: 'Khối Kỹ Thuật & Công Nghệ',
    date: '2026-10-24',
    hours: 8.0,
    rate: '2.0x (Ngày nghỉ cuối tuần)',
    task: 'Bảo trì cơ sở dữ liệu và hạ tầng server',
    status: 'PENDING',
  },
];

export function OvertimeApprovalPage() {
  const navigate = useNavigate();
  const [requests, setRequests] = useState(PENDING_OT_LIST);

  const handleAction = (id, status) => {
    setRequests((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status } : r))
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate('/overtime')}
          icon={ArrowLeft}
        >
          Quay lại
        </Button>
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Xét duyệt Đăng ký Tăng ca (OT)
          </h1>
          <p className="text-xs text-slate-500">
            Duyệt thù lao làm thêm giờ cho nhân viên trực thuộc
          </p>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100">
          <h3 className="text-base font-bold text-slate-800 flex items-center">
            <UserCheck className="w-5 h-5 mr-2 text-amber-500" />
            Yêu cầu chờ duyệt ({requests.filter((r) => r.status === 'PENDING').length})
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3.5">Nhân viên</th>
                <th className="px-4 py-3.5">Ngày OT</th>
                <th className="px-4 py-3.5">Số giờ & Hệ số</th>
                <th className="px-4 py-3.5">Nội dung công việc</th>
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
                  <td className="px-4 py-3.5 font-semibold text-slate-800">
                    {formatDate(r.date)}
                  </td>
                  <td className="px-4 py-3.5">
                    <span className="font-bold text-slate-900">{r.hours}h</span>
                    <span className="text-xs text-amber-600 block">{r.rate}</span>
                  </td>
                  <td className="px-4 py-3.5 text-slate-600 max-w-xs truncate">
                    {r.task}
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
                      <span className="text-xs text-slate-400 italic">Đã duyệt</span>
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

export default OvertimeApprovalPage;
