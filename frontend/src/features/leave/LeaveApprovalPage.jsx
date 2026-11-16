import React, { useState } from 'react';
import { Check, X, UserCheck } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import leaveApi from '../../api/leaveApi';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import PromptDialog from '../../components/common/PromptDialog';
import { formatDate, formatDateTime } from '../../utils/formatDate';

export function LeaveApprovalPage() {
  const { data, loading, error, reload } = useAsync(() => leaveApi.inbox(), []);
  const [decision, setDecision] = useState(null); // { req, approve }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Duyệt đơn nghỉ phép</h1>
        <p className="text-sm text-slate-500 mt-0.5">
          Chỉ hiển thị đơn thuộc tuyến duyệt của bạn (người duyệt chính hoặc người dự phòng đã chỉ định). Không thể tự duyệt đơn của mình.
        </p>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100">
          <h3 className="text-base font-bold text-slate-800 flex items-center">
            <UserCheck className="w-5 h-5 mr-2 text-blue-600" />
            Đơn chờ duyệt ({data?.length ?? 0})
          </h3>
        </div>

        {loading ? (
          <Loading />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : !data?.length ? (
          <EmptyState title="Không có đơn chờ duyệt" description="Mọi đơn thuộc tuyến duyệt của bạn đã được xử lý." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Nhân viên</th>
                  <th className="px-4 py-3.5">Loại phép & lý do</th>
                  <th className="px-4 py-3.5">Thời gian</th>
                  <th className="px-4 py-3.5">Số ngày</th>
                  <th className="px-4 py-3.5 text-right">Phê duyệt</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5">
                      <p className="font-bold text-slate-900">{r.employeeName}</p>
                      <p className="text-xs text-slate-500">{r.employeeCode} • gửi {formatDateTime(r.submittedAt)}</p>
                    </td>
                    <td className="px-4 py-3.5">
                      <p className="font-semibold text-slate-800">
                        {r.leaveTypeName}{r.kind === 'CANCEL' && <span className="ml-2 text-xs text-amber-700">(yêu cầu hủy đơn #{r.parentRequestId})</span>}
                      </p>
                      {r.reason && <p className="text-xs text-slate-500 italic">“{r.reason}”</p>}
                      {r.evidence && <p className="text-xs text-slate-400">Minh chứng: {r.evidence}</p>}
                    </td>
                    <td className="px-4 py-3.5 text-xs">{formatDate(r.startDate)}{r.endDate !== r.startDate && ` – ${formatDate(r.endDate)}`}</td>
                    <td className="px-4 py-3.5 font-bold text-slate-900">{r.durationDays} ngày</td>
                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      <Button size="sm" icon={Check} onClick={() => setDecision({ req: r, approve: true })}>Duyệt</Button>
                      <Button size="sm" variant="danger" icon={X} className="ml-2" onClick={() => setDecision({ req: r, approve: false })}>Từ chối</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <PromptDialog
        isOpen={Boolean(decision)}
        title={decision?.approve ? 'Duyệt đơn nghỉ phép' : 'Từ chối đơn nghỉ phép'}
        description={decision && `${decision.req.employeeName} – ${decision.req.leaveTypeName}`}
        label={decision?.approve ? 'Ghi chú (không bắt buộc)' : 'Lý do từ chối'}
        required={!decision?.approve}
        confirmLabel={decision?.approve ? 'Duyệt' : 'Từ chối'}
        confirmVariant={decision?.approve ? 'primary' : 'danger'}
        onClose={() => setDecision(null)}
        onSubmit={async (reason) => {
          if (decision.approve) await leaveApi.approve(decision.req.id, reason || undefined);
          else await leaveApi.reject(decision.req.id, reason);
          reload();
        }}
      />
    </div>
  );
}

export default LeaveApprovalPage;
