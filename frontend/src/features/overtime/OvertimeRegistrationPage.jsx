import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import overtimeApi from '../../api/overtimeApi';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import { formatDate, formatMinutes } from '../../utils/formatDate';

const ACTUAL = { NOT_RECORDED: 'Chưa ghi nhận', CONFIRMED: 'Đã xác nhận', NOT_PERFORMED: 'Không thực hiện' };

export function OvertimeRegistrationPage() {
  const { data, loading, error, reload } = useAsync(() => overtimeApi.myRegistrations(), []);
  const [actionError, setActionError] = useState('');

  const cancel = async (id) => {
    setActionError('');
    try {
      await overtimeApi.cancel(id);
      reload();
    } catch (err) {
      setActionError(err.message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/overtime"><Button variant="outline" size="sm" icon={ArrowLeft}>Lịch OT</Button></Link>
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Đăng ký OT của tôi</h1>
          <p className="text-sm text-slate-500 mt-0.5">Theo dõi trạng thái duyệt và số giờ OT được ghi nhận</p>
        </div>
      </div>

      {actionError && <ErrorMessage message={actionError} />}

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? (
          <Loading />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : !data?.length ? (
          <EmptyState title="Chưa có đăng ký OT" description="Đăng ký OT từ trang Lịch OT khi có lịch được công bố." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Lịch OT</th>
                  <th className="px-4 py-3.5">Đăng ký</th>
                  <th className="px-4 py-3.5">Được duyệt</th>
                  <th className="px-4 py-3.5">Thực tế</th>
                  <th className="px-4 py-3.5">Trạng thái</th>
                  <th className="px-4 py-3.5" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5 font-semibold text-slate-900">{r.scheduleTitle}<span className="block text-xs font-normal text-slate-500">{formatDate(r.workDate)}</span></td>
                    <td className="px-4 py-3.5">{formatMinutes(r.registeredMinutes)}</td>
                    <td className="px-4 py-3.5">{r.approvedMinutes != null ? formatMinutes(r.approvedMinutes) : '-'}
                      {r.decisionReason && <span className="block text-xs text-slate-400">“{r.decisionReason}”</span>}</td>
                    <td className="px-4 py-3.5">{ACTUAL[r.actualStatus] || r.actualStatus}
                      {r.actualStatus === 'CONFIRMED' && <span className="block text-xs text-slate-500">Tính lương: {formatMinutes(r.payableMinutes)}</span>}</td>
                    <td className="px-4 py-3.5"><StatusBadge status={r.status} /></td>
                    <td className="px-4 py-3.5 text-right">
                      {['PENDING', 'APPROVED'].includes(r.status) && r.actualStatus === 'NOT_RECORDED' && (
                        <Button size="sm" variant="ghost" className="text-red-600" onClick={() => cancel(r.id)}>Hủy đăng ký</Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default OvertimeRegistrationPage;
