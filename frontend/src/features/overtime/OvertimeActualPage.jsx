import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Check, X, Plus, ClipboardCheck, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import overtimeApi from '../../api/overtimeApi';
import employeeApi from '../../api/employeeApi';
import Button from '../../components/common/Button';
import Tabs from '../../components/common/Tabs';
import DataTable from '../../components/common/DataTable';
import FormModal from '../../components/common/FormModal';
import PromptDialog from '../../components/common/PromptDialog';
import StatusBadge from '../../components/common/StatusBadge';
import ErrorMessage from '../../components/common/ErrorMessage';
import { formatDate, formatDateTime, formatMinutes } from '../../utils/formatDate';

const ACTUAL = { NOT_RECORDED: 'Chưa ghi nhận', CONFIRMED: 'Đã xác nhận', NOT_PERFORMED: 'Không thực hiện' };
const box = 'bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden';
const iso = (local) => new Date(local).toISOString();
const txt = (v) => (v && String(v).trim() ? String(v).trim() : undefined);

export function OvertimeActualPage() {
  const { isHR, isManager } = useAuth();
  const canVerify = isHR || isManager;
  const [tab, setTab] = useState(canVerify ? 'actual' : 'unscheduled');
  const [error, setError] = useState('');

  const regs = useAsync(() => overtimeApi.registrations(), [], { enabled: canVerify });
  const uns = useAsync(() => overtimeApi.unscheduled(), []);
  const people = useAsync(() => employeeApi.options(), [], { enabled: canVerify });

  const [actualFor, setActualFor] = useState(null);
  const [unsOpen, setUnsOpen] = useState(false);
  const [prompt, setPrompt] = useState(null);

  const approved = (regs.data || []).filter((r) => r.status === 'APPROVED');

  const hrCheck = async (r) => {
    setError('');
    try { await overtimeApi.hrCheck(r.id); regs.reload(); } catch (err) { setError(err.message); }
  };

  const regColumns = [
    { key: 'employeeName', header: 'Nhân viên', render: (r) => (<><p className="font-semibold text-slate-900">{r.employeeName}</p><p className="text-xs text-slate-500">{r.employeeCode}</p></>) },
    { key: 'scheduleTitle', header: 'Lịch OT', render: (r) => (<>{r.scheduleTitle}<p className="text-xs text-slate-500">{formatDate(r.workDate)}</p></>) },
    { key: 'approvedMinutes', header: 'Được duyệt', render: (r) => formatMinutes(r.approvedMinutes) },
    { key: 'actualStatus', header: 'Thực tế', render: (r) => (
      <>
        <p>{ACTUAL[r.actualStatus] || r.actualStatus}</p>
        {r.actualStatus === 'CONFIRMED' && <p className="text-xs text-slate-500">{formatDateTime(r.actualStart)} → {formatDateTime(r.actualEnd)}</p>}
        {r.actualConfirmedByName && <p className="text-xs text-slate-400">Xác nhận: {r.actualConfirmedByName}</p>}
      </>
    ) },
    { key: 'payableMinutes', header: 'Tính lương', render: (r) => <b className="text-slate-900">{formatMinutes(r.payableMinutes)}</b> },
    { key: 'hrChecked', header: 'HR rà soát', render: (r) => (r.hrChecked ? 'Đã rà soát' : '-') },
    { key: 'actions', header: '', align: 'right', render: (r) => (
      <div className="flex justify-end gap-1.5">
        {r.canConfirmActual && <Button size="sm" icon={ClipboardCheck} onClick={() => setActualFor(r)}>Xác nhận giờ</Button>}
        {isHR && r.actualStatus === 'CONFIRMED' && !r.hrChecked && <Button size="sm" variant="outline" icon={ShieldCheck} onClick={() => hrCheck(r)}>HR rà soát</Button>}
      </div>
    ) },
  ];

  const unsColumns = [
    { key: 'employeeName', header: 'Nhân viên', render: (o) => (<><p className="font-semibold text-slate-900">{o.employeeName}</p><p className="text-xs text-slate-500">{o.employeeCode}</p></>) },
    { key: 'time', header: 'Thời gian', render: (o) => (<>{formatDate(o.workDate)}<p className="text-xs text-slate-500">{formatDateTime(o.startAt)} → {formatDateTime(o.endAt)} ({formatMinutes(o.minutes)})</p></>) },
    { key: 'description', header: 'Nội dung / minh chứng', render: (o) => (<>{o.description}{o.evidence && <p className="text-xs text-slate-500">{o.evidence}</p>}</>) },
    { key: 'createdByName', header: 'Người tạo' },
    { key: 'status', header: 'Trạng thái', render: (o) => (<><StatusBadge status={o.status} />{o.verifiedByName && <p className="text-xs text-slate-400 mt-1">{o.verifiedByName}</p>}{o.verifyNote && <p className="text-xs text-slate-500">{o.verifyNote}</p>}</>) },
    { key: 'actions', header: '', align: 'right', render: (o) => o.canVerify && (
      <div className="flex justify-end gap-1.5">
        <Button size="sm" icon={Check} onClick={() => setPrompt({ title: 'Xác nhận OT ngoài lịch', label: 'Ghi chú', required: false, confirmLabel: 'Xác nhận', run: (n) => overtimeApi.confirmUnscheduled(o.id, n || undefined).then(uns.reload) })}>Xác nhận</Button>
        <Button size="sm" variant="danger" icon={X} onClick={() => setPrompt({ title: 'Từ chối OT ngoài lịch', label: 'Lý do', required: true, variant: 'danger', confirmLabel: 'Từ chối', run: (n) => overtimeApi.rejectUnscheduled(o.id, n).then(uns.reload) })}>Từ chối</Button>
      </div>
    ) },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Link to="/overtime"><Button variant="outline" size="sm" icon={ArrowLeft}>Lịch OT</Button></Link>
          <div>
            <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Giờ OT thực tế</h1>
            <p className="text-sm text-slate-500 mt-0.5">Chỉ giờ OT thực tế đã được xác nhận (không vượt số giờ đã duyệt) mới được tính lương.</p>
          </div>
        </div>
        {tab === 'unscheduled' && <Button icon={Plus} onClick={() => setUnsOpen(true)}>Ghi nhận OT ngoài lịch</Button>}
      </div>

      {error && <ErrorMessage message={error} />}

      <Tabs
        tabs={[
          ...(canVerify ? [{ key: 'actual', label: 'Xác nhận giờ thực tế', count: approved.length }] : []),
          { key: 'unscheduled', label: 'OT ngoài lịch', count: uns.data?.length },
        ]}
        active={tab}
        onChange={setTab}
      />

      <div className={box}>
        {tab === 'actual'
          ? <DataTable columns={regColumns} rows={approved} loading={regs.loading} error={regs.error} onRetry={regs.reload} emptyTitle="Chưa có đăng ký OT nào được duyệt" />
          : <DataTable columns={unsColumns} rows={uns.data} loading={uns.loading} error={uns.error} onRetry={uns.reload} emptyTitle="Chưa có OT ngoài lịch" />}
      </div>

      <FormModal
        isOpen={Boolean(actualFor)}
        title={`Xác nhận giờ OT – ${actualFor?.employeeName || ''}`}
        description={actualFor && `${actualFor.scheduleTitle} • được duyệt ${formatMinutes(actualFor.approvedMinutes)}`}
        fields={[
          { name: 'status', label: 'Kết quả', type: 'select', options: [{ value: 'CONFIRMED', label: 'Đã làm OT' }, { value: 'NOT_PERFORMED', label: 'Không thực hiện' }], required: true },
          { name: 'actualStart', label: 'Bắt đầu thực tế', type: 'datetime-local', half: true },
          { name: 'actualEnd', label: 'Kết thúc thực tế', type: 'datetime-local', half: true },
          { name: 'extraConfirmed', label: 'Xác nhận phần giờ vượt mức đã duyệt (nếu có)', type: 'checkbox' },
          { name: 'evidence', label: 'Minh chứng', required: true },
        ]}
        initial={{ status: 'CONFIRMED', extraConfirmed: false }}
        onClose={() => setActualFor(null)}
        onSubmit={async (v) => {
          if (v.status === 'CONFIRMED' && (!v.actualStart || !v.actualEnd)) throw new Error('Cần nhập giờ bắt đầu và kết thúc thực tế.');
          await overtimeApi.confirmActual(actualFor.id, {
            status: v.status, actualStart: v.actualStart ? iso(v.actualStart) : undefined, actualEnd: v.actualEnd ? iso(v.actualEnd) : undefined,
            extraConfirmed: Boolean(v.extraConfirmed), evidence: v.evidence.trim(),
          });
          regs.reload();
        }}
      />

      <FormModal
        isOpen={unsOpen}
        title="Ghi nhận OT ngoài lịch"
        description="Dùng khi phát sinh OT đột xuất chưa có lịch. Người xác minh phải khác người tạo và người làm OT."
        fields={[
          ...(canVerify ? [{ name: 'employeeId', label: 'Nhân viên', type: 'select', options: (people.data || []).map((p) => ({ value: p.id, label: `${p.code} – ${p.fullName}` })), placeholder: 'Chính tôi' }] : []),
          { name: 'startAt', label: 'Bắt đầu', type: 'datetime-local', required: true, half: true },
          { name: 'endAt', label: 'Kết thúc', type: 'datetime-local', required: true, half: true },
          { name: 'description', label: 'Nội dung công việc', required: true },
          { name: 'evidence', label: 'Minh chứng' },
        ]}
        onClose={() => setUnsOpen(false)}
        onSubmit={async (v) => {
          await overtimeApi.createUnscheduled({
            employeeId: v.employeeId ? Number(v.employeeId) : undefined, startAt: iso(v.startAt), endAt: iso(v.endAt),
            description: v.description.trim(), evidence: txt(v.evidence),
          });
          uns.reload();
        }}
      />

      <PromptDialog
        isOpen={Boolean(prompt)}
        title={prompt?.title}
        label={prompt?.label}
        required={prompt?.required}
        confirmLabel={prompt?.confirmLabel}
        confirmVariant={prompt?.variant}
        onClose={() => setPrompt(null)}
        onSubmit={(t) => prompt.run(t)}
      />
    </div>
  );
}

export default OvertimeActualPage;
