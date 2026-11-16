import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Check, X } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import overtimeApi from '../../api/overtimeApi';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import InputField from '../../components/common/InputField';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import PromptDialog from '../../components/common/PromptDialog';
import { formatDate, formatMinutes } from '../../utils/formatDate';

export function OvertimeApprovalPage() {
  const { data, loading, error, reload } = useAsync(() => overtimeApi.inbox(), []);
  const [approving, setApproving] = useState(null);
  const [minutes, setMinutes] = useState('');
  const [note, setNote] = useState('');
  const [modalError, setModalError] = useState('');
  const [busy, setBusy] = useState(false);
  const [rejecting, setRejecting] = useState(null);

  const openApprove = (r) => {
    setApproving(r);
    setMinutes(String(r.registeredMinutes));
    setNote('');
    setModalError('');
  };

  const approve = async (e) => {
    e.preventDefault();
    setBusy(true);
    setModalError('');
    try {
      await overtimeApi.approve(approving.id, { approvedMinutes: Number(minutes), reason: note.trim() || undefined });
      setApproving(null);
      reload();
    } catch (err) {
      setModalError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/overtime"><Button variant="outline" size="sm" icon={ArrowLeft}>Lịch OT</Button></Link>
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Duyệt đăng ký OT</h1>
          <p className="text-sm text-slate-500 mt-0.5">Chỉ hiển thị đăng ký thuộc tuyến duyệt của bạn; không tự duyệt đăng ký của mình.</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? (
          <Loading />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : !data?.length ? (
          <EmptyState title="Không có đăng ký chờ duyệt" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Nhân viên</th>
                  <th className="px-4 py-3.5">Lịch OT</th>
                  <th className="px-4 py-3.5">Đăng ký</th>
                  <th className="px-4 py-3.5 text-right">Phê duyệt</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5 font-bold text-slate-900">{r.employeeName}<span className="block text-xs font-normal text-slate-400">{r.employeeCode}</span></td>
                    <td className="px-4 py-3.5">{r.scheduleTitle}<span className="block text-xs">{formatDate(r.workDate)}</span></td>
                    <td className="px-4 py-3.5 font-semibold">{formatMinutes(r.registeredMinutes)}</td>
                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      <Button size="sm" icon={Check} onClick={() => openApprove(r)}>Duyệt</Button>
                      <Button size="sm" variant="danger" icon={X} className="ml-2" onClick={() => setRejecting(r)}>Từ chối</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Modal isOpen={Boolean(approving)} title="Duyệt đăng ký OT" onClose={() => setApproving(null)}>
        {approving && (
          <form onSubmit={approve} className="space-y-4">
            {modalError && <ErrorMessage message={modalError} />}
            <p className="text-sm text-slate-700">{approving.employeeName} – {approving.scheduleTitle} ({formatDate(approving.workDate)})</p>
            <InputField label="Số phút OT được duyệt" type="number" min="1" max={approving.registeredMinutes} value={minutes}
              onChange={(e) => setMinutes(e.target.value)} required helperText={`Tối đa ${approving.registeredMinutes} phút (đăng ký).`} />
            <InputField label="Ghi chú" value={note} onChange={(e) => setNote(e.target.value)} />
            <div className="flex justify-end gap-3">
              <Button variant="outline" type="button" onClick={() => setApproving(null)}>Hủy</Button>
              <Button type="submit" isLoading={busy}>Duyệt</Button>
            </div>
          </form>
        )}
      </Modal>

      <PromptDialog
        isOpen={Boolean(rejecting)}
        title="Từ chối đăng ký OT"
        description={rejecting && `${rejecting.employeeName} – ${rejecting.scheduleTitle}`}
        label="Lý do từ chối"
        confirmLabel="Từ chối"
        confirmVariant="danger"
        onClose={() => setRejecting(null)}
        onSubmit={async (reason) => {
          await overtimeApi.reject(rejecting.id, { reason });
          reload();
        }}
      />
    </div>
  );
}

export default OvertimeApprovalPage;
