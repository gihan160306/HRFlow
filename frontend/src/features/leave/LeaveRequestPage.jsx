import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CalendarDays, Plus, Clock, CheckCircle2, FileText } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import StatCard from '../../components/dashboard/StatCard';
import LeaveRequestForm from './LeaveRequestForm';
import { formatDate } from '../../utils/formatDate';

const INITIAL_REQUESTS = [
  {
    id: 1,
    leaveType: 'Nghỉ phép năm',
    startDate: '2026-10-05',
    endDate: '2026-10-05',
    daysCount: 1,
    reason: 'Việc cá nhân',
    status: 'APPROVED',
  },
  {
    id: 2,
    leaveType: 'Nghỉ phép năm',
    startDate: '2026-10-22',
    endDate: '2026-10-23',
    daysCount: 2,
    reason: 'Giải quyết việc gia đình',
    status: 'PENDING',
  },
];

export function LeaveRequestPage() {
  const navigate = useNavigate();
  const { isManager, isHR, isAdmin } = useAuth();
  const canApprove = isManager || isHR || isAdmin;

  const [requests, setRequests] = useState(INITIAL_REQUESTS);
  const [isFormOpen, setIsFormOpen] = useState(false);

  const handleCreateLeave = (data) => {
    const newReq = {
      id: Date.now(),
      leaveType: data.leaveType === 'ANNUAL' ? 'Nghỉ phép năm' : 'Nghỉ ốm đau',
      startDate: data.startDate,
      endDate: data.endDate,
      daysCount: 1,
      reason: data.reason,
      status: 'PENDING',
    };
    setRequests((prev) => [newReq, ...prev]);
    setIsFormOpen(false);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Quản lý Nghỉ phép (PTO)
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Theo dõi quỹ phép năm cá nhân và nộp đơn xin nghỉ
          </p>
        </div>

        <div className="flex items-center space-x-3">
          {canApprove && (
            <Button
              variant="outline"
              onClick={() => navigate('/leave/approval')}
              icon={FileText}
            >
              Duyệt đơn nghỉ
            </Button>
          )}
          <Button onClick={() => setIsFormOpen(true)} icon={Plus}>
            Tạo đơn nghỉ phép
          </Button>
        </div>
      </div>

      {/* Quota balance cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          title="Tổng quỹ phép năm"
          value="12 ngày"
          subtitle="Tiêu chuẩn năm 2026"
          icon={CalendarDays}
          color="blue"
        />
        <StatCard
          title="Phép năm đã sử dụng"
          value="3 ngày"
          subtitle="Bao gồm các đơn đã duyệt"
          icon={Clock}
          color="amber"
        />
        <StatCard
          title="Phép năm còn lại"
          value="9 ngày"
          subtitle="Khả dụng đến hết 31/12/2026"
          icon={CheckCircle2}
          color="emerald"
        />
      </div>

      {/* History Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100">
          <h3 className="text-base font-bold text-slate-800">
            Lịch sử đơn xin nghỉ phép của tôi
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3.5">Loại nghỉ phép</th>
                <th className="px-4 py-3.5">Khoảng thời gian</th>
                <th className="px-4 py-3.5">Số ngày</th>
                <th className="px-4 py-3.5">Lý do</th>
                <th className="px-4 py-3.5">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {requests.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="px-4 py-3.5 font-bold text-slate-900">
                    {r.leaveType}
                  </td>
                  <td className="px-4 py-3.5 text-xs text-slate-600">
                    {formatDate(r.startDate)} - {formatDate(r.endDate)}
                  </td>
                  <td className="px-4 py-3.5 font-semibold text-slate-800">
                    {r.daysCount} ngày
                  </td>
                  <td className="px-4 py-3.5 text-slate-600">{r.reason}</td>
                  <td className="px-4 py-3.5">
                    <StatusBadge status={r.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <LeaveRequestForm
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSubmit={handleCreateLeave}
      />
    </div>
  );
}

export default LeaveRequestPage;
