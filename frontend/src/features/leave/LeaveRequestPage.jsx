import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CalendarDays, Plus, Clock, CheckCircle2, FileText, Hourglass } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import leaveApi from '../../api/leaveApi';
import { catalogApi } from '../../api/departmentApi';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import StatCard from '../../components/dashboard/StatCard';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import PromptDialog from '../../components/common/PromptDialog';
import LeaveRequestForm from './LeaveRequestForm';
import { formatDate } from '../../utils/formatDate';

export function LeaveRequestPage() {
  const navigate = useNavigate();
  const { isManager, isHR } = useAuth();
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [actionError, setActionError] = useState('');
  const [cancelReq, setCancelReq] = useState(null);

  const requests = useAsync(() => leaveApi.mine(), []);
  const balance = useAsync(() => leaveApi.myBalance(), []);
  const types = useAsync(() => catalogApi.leaveTypes(), []);

  const reload = () => {
    requests.reload();
    balance.reload();
  };

  const act = async (fn) => {
    setActionError('');
    try {
      await fn();
      reload();
    } catch (err) {
      setActionError(err.message);
    }
  };

  const b = balance.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Nghỉ phép (PTO)</h1>
          <p className="text-sm text-slate-500 mt-0.5">Theo dõi quỹ phép năm và đơn xin nghỉ của bạn</p>
        </div>
        <div className="flex items-center gap-3">
          {(isManager || isHR) && (
            <Button variant="outline" onClick={() => navigate('/leave/approval')} icon={FileText}>Duyệt đơn nghỉ</Button>
          )}
          <Button onClick={() => setIsFormOpen(true)} icon={Plus} disabled={!types.data}>Tạo đơn nghỉ phép</Button>
        </div>
      </div>

      {balance.error && <ErrorMessage message={balance.error.message} onRetry={balance.reload} />}
      {b && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard title={`Quỹ phép năm ${b.year}`} value={`${Number(b.openingDays) + Number(b.accruedDays) + Number(b.adjustedDays)} ngày`}
            subtitle={`Tồn đầu ${b.openingDays} • phát sinh ${b.accruedDays} • điều chỉnh ${b.adjustedDays}`} icon={CalendarDays} color="blue" />
          <StatCard title="Đã sử dụng" value={`${b.usedDays} ngày`} icon={Clock} color="amber" />
          <StatCard title="Đang chờ duyệt" value={`${b.pendingDays} ngày`} icon={Hourglass} color="purple" />
          <StatCard title="Còn lại" value={`${b.remainingDays} ngày`} icon={CheckCircle2} color="emerald" />
        </div>
      )}

      {actionError && <ErrorMessage message={actionError} />}

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100"><h3 className="text-base font-bold text-slate-800">Đơn nghỉ của tôi</h3></div>
        {requests.loading ? (
          <Loading />
        ) : requests.error ? (
          <div className="p-4"><ErrorMessage message={requests.error.message} onRetry={requests.reload} /></div>
        ) : !requests.data?.length ? (
          <p className="p-6 text-sm text-slate-500">Bạn chưa có đơn nghỉ nào.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Loại nghỉ</th>
                  <th className="px-4 py-3.5">Thời gian</th>
                  <th className="px-4 py-3.5">Số ngày</th>
                  <th className="px-4 py-3.5">Người duyệt</th>
                  <th className="px-4 py-3.5">Trạng thái</th>
                  <th className="px-4 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {requests.data.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5">
                      <span className="font-bold text-slate-900">{r.leaveTypeName}</span>
                      {r.kind === 'CANCEL' && <span className="block text-xs text-amber-700">Yêu cầu hủy đơn #{r.parentRequestId}</span>}
                      {r.reason && <span className="block text-xs text-slate-500 italic">{r.reason}</span>}
                    </td>
                    <td className="px-4 py-3.5 text-xs">{formatDate(r.startDate)}{r.endDate !== r.startDate && ` – ${formatDate(r.endDate)}`}</td>
                    <td className="px-4 py-3.5 font-semibold">{r.durationDays} ngày</td>
                    <td className="px-4 py-3.5 text-xs">{r.primaryApproverName || '-'}{r.decisionReason && <span className="block text-slate-400">“{r.decisionReason}”</span>}</td>
                    <td className="px-4 py-3.5"><StatusBadge status={r.status} /></td>
                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      {r.status === 'DRAFT' && (
                        <Button size="sm" onClick={() => act(() => leaveApi.submit(r.id))}>Gửi duyệt</Button>
                      )}
                      {['DRAFT', 'PENDING'].includes(r.status) && (
                        <Button size="sm" variant="ghost" className="ml-2 text-red-600" onClick={() => act(() => leaveApi.cancel(r.id))}>Hủy đơn</Button>
                      )}
                      {r.status === 'APPROVED' && r.kind === 'NEW' && (
                        <Button size="sm" variant="outline" onClick={() => setCancelReq(r)}>Yêu cầu hủy</Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <LeaveRequestForm
        isOpen={isFormOpen}
        leaveTypes={types.data || []}
        onClose={() => setIsFormOpen(false)}
        onSaved={() => { setIsFormOpen(false); reload(); }}
      />

      <PromptDialog
        isOpen={Boolean(cancelReq)}
        title="Yêu cầu hủy đơn đã duyệt"
        description="Đơn đã duyệt không bị sửa trực tiếp; yêu cầu hủy sẽ được gửi lại cho người duyệt xử lý."
        label="Lý do hủy"
        confirmLabel="Gửi yêu cầu hủy"
        onClose={() => setCancelReq(null)}
        onSubmit={async (reason) => {
          await leaveApi.requestCancel(cancelReq.id, reason);
          reload();
        }}
      />
    </div>
  );
}

export default LeaveRequestPage;
