import React, { useState } from 'react';
import { CheckCircle2, Clock, AlertTriangle, CalendarX, Plus, Check, X } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import attendanceApi from '../../api/attendanceApi';
import CheckInOutPanel from './CheckInOutPanel';
import AttendanceHistory from './AttendanceHistory';
import AttendancePointsPanel from './AttendancePointsPanel';
import StatCard from '../../components/dashboard/StatCard';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import ErrorMessage from '../../components/common/ErrorMessage';
import StatusBadge from '../../components/common/StatusBadge';
import PromptDialog from '../../components/common/PromptDialog';
import { formatDate, formatTime, formatMinutes, todayIso } from '../../utils/formatDate';

const pad = (n) => String(n).padStart(2, '0');
const monthOf = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}`;
// Giờ nhập theo múi giờ Việt Nam (UTC+7) -> Instant ISO gửi backend.
const toInstant = (date, time) => (date && time ? new Date(`${date}T${time}:00+07:00`).toISOString() : undefined);

const METHODS = [
  { value: 'MANUAL', label: 'Bổ sung giờ vào/ra' },
  { value: 'BUSINESS_TRIP', label: 'Công tác' },
];

export function AttendancePage() {
  const { user, isHR, isManager } = useAuth();
  const canDecide = isHR || isManager;
  const [month, setMonth] = useState(monthOf(new Date()));
  const [adjOpen, setAdjOpen] = useState(false);
  const [adjForm, setAdjForm] = useState({ workDate: todayIso(), method: 'MANUAL', inTime: '08:30', outTime: '17:30', reason: '', evidence: '' });
  const [adjError, setAdjError] = useState('');
  const [adjBusy, setAdjBusy] = useState(false);
  const [decide, setDecide] = useState(null); // { adj, approve }
  const [confirmRec, setConfirmRec] = useState(null);

  const [year, mon] = month.split('-').map(Number);
  const from = `${month}-01`;
  const to = `${month}-${pad(new Date(year, mon, 0).getDate())}`;

  const today = useAsync(() => attendanceApi.today(), []);
  const records = useAsync(() => attendanceApi.list({ employeeId: user.employeeId, from, to }), [month]);
  const sheet = useAsync(() => attendanceApi.timesheet({ year, month: mon }), [month]);
  const adjustments = useAsync(() => attendanceApi.adjustments({}), []);
  const teamReview = useAsync(() => attendanceApi.list({ status: 'NEEDS_REVIEW', from, to }), [month], { enabled: canDecide });

  const refresh = () => {
    today.reload();
    records.reload();
    sheet.reload();
    adjustments.reload();
    if (canDecide) teamReview.reload();
  };

  const submitAdjustment = async (e) => {
    e.preventDefault();
    setAdjError('');
    setAdjBusy(true);
    try {
      await attendanceApi.createAdjustment({
        workDate: adjForm.workDate,
        method: adjForm.method,
        proposedCheckIn: toInstant(adjForm.workDate, adjForm.inTime),
        proposedCheckOut: toInstant(adjForm.workDate, adjForm.outTime),
        reason: adjForm.reason.trim(),
        evidence: adjForm.evidence.trim() || undefined,
      });
      setAdjOpen(false);
      refresh();
    } catch (err) {
      setAdjError(err.message);
    } finally {
      setAdjBusy(false);
    }
  };

  const pending = (adjustments.data || []).filter((a) => a.status === 'PENDING');
  const myAdjustments = (adjustments.data || []).filter((a) => a.employeeId === user.employeeId);
  const decidable = pending.filter((a) => a.employeeId !== user.employeeId);
  const s = sheet.data;
  const lateCount = (records.data || []).filter((r) => r.lateMinutes > 0).length;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Chấm công</h1>
          <p className="text-sm text-slate-500 mt-0.5">Chấm công tại máy quầy bằng QR cá nhân và theo dõi bảng công</p>
        </div>
        <div className="flex items-end gap-3">
          <InputField label="Tháng" type="month" value={month} onChange={(e) => e.target.value && setMonth(e.target.value)} />
          <Button icon={Plus} variant="outline" onClick={() => { setAdjError(''); setAdjOpen(true); }}>Đề nghị điều chỉnh công</Button>
        </div>
      </div>

      <CheckInOutPanel record={today.data} onChanged={refresh} />
      {today.error && <ErrorMessage message={today.error.message} onRetry={today.reload} />}

      {sheet.error && <ErrorMessage message={sheet.error.message} onRetry={sheet.reload} />}
      {s && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard title="Giờ công chuẩn / hưởng lương" value={`${formatMinutes(s.paidMinutes)} / ${formatMinutes(s.stdMinutes)}`} icon={CheckCircle2} color="blue" />
          <StatCard title="Giờ làm thực tế" value={formatMinutes(s.workedMinutes)} icon={Clock} color="emerald" />
          <StatCard title="Đi muộn / về sớm" value={`${s.lateMinutes}p / ${s.earlyMinutes}p`} subtitle={`${lateCount} ngày muộn`} icon={AlertTriangle} color="amber" />
          <StatCard title="Nghỉ không lương" value={formatMinutes(s.unpaidLeaveMinutes)} subtitle={s.blockingIssues > 0 ? `${s.blockingIssues} vấn đề cần xử lý` : undefined} icon={CalendarX} color="purple" />
        </div>
      )}

      {records.error ? (
        <ErrorMessage message={records.error.message} onRetry={records.reload} />
      ) : (
        <AttendanceHistory records={records.data || []} />
      )}

      {myAdjustments.length > 0 && (
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
          <div className="p-4 sm:p-6 border-b border-slate-100"><h3 className="text-base font-bold text-slate-800">Đề nghị điều chỉnh công của tôi</h3></div>
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500"><tr>
              <th className="px-4 py-3">Ngày</th><th className="px-4 py-3">Đề xuất</th><th className="px-4 py-3">Lý do</th><th className="px-4 py-3">Trạng thái</th></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {myAdjustments.map((a) => (
                <tr key={a.id}>
                  <td className="px-4 py-3">{formatDate(a.workDate)}</td>
                  <td className="px-4 py-3 font-mono text-xs">{formatTime(a.proposedCheckIn)} – {formatTime(a.proposedCheckOut)}</td>
                  <td className="px-4 py-3">{a.reason}{a.decisionNote && <span className="block text-xs text-slate-400">Ghi chú duyệt: {a.decisionNote}</span>}</td>
                  <td className="px-4 py-3"><StatusBadge status={a.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {canDecide && decidable.length > 0 && (
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
          <div className="p-4 sm:p-6 border-b border-slate-100"><h3 className="text-base font-bold text-slate-800">Điều chỉnh công chờ duyệt ({decidable.length})</h3></div>
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500"><tr>
              <th className="px-4 py-3">Nhân viên</th><th className="px-4 py-3">Ngày</th><th className="px-4 py-3">Đề xuất</th><th className="px-4 py-3">Lý do</th><th className="px-4 py-3" /></tr></thead>
            <tbody className="divide-y divide-slate-100">
              {decidable.map((a) => (
                <tr key={a.id}>
                  <td className="px-4 py-3 font-semibold text-slate-900">{a.employeeName}</td>
                  <td className="px-4 py-3">{formatDate(a.workDate)}</td>
                  <td className="px-4 py-3 font-mono text-xs">{a.method}: {formatTime(a.proposedCheckIn)} – {formatTime(a.proposedCheckOut)}</td>
                  <td className="px-4 py-3">{a.reason}</td>
                  <td className="px-4 py-3 text-right whitespace-nowrap">
                    <Button size="sm" icon={Check} onClick={() => setDecide({ adj: a, approve: true })}>Duyệt</Button>
                    <Button size="sm" variant="danger" icon={X} className="ml-2" onClick={() => setDecide({ adj: a, approve: false })}>Từ chối</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {canDecide && (teamReview.data || []).filter((r) => r.employeeId !== user.employeeId).length > 0 && (
        <AttendanceHistory
          showEmployee
          records={(teamReview.data || []).filter((r) => r.employeeId !== user.employeeId)}
          onConfirm={(r) => (
            <Button size="sm" variant="outline" onClick={() => setConfirmRec(r)}>Xác nhận</Button>
          )}
        />
      )}

      {isHR && <AttendancePointsPanel />}

      <Modal isOpen={adjOpen} title="Đề nghị điều chỉnh công" onClose={() => setAdjOpen(false)}>
        <form onSubmit={submitAdjustment} className="space-y-4">
          {adjError && <ErrorMessage message={adjError} />}
          <InputField label="Ngày công" type="date" max={todayIso()} value={adjForm.workDate} onChange={(e) => setAdjForm({ ...adjForm, workDate: e.target.value })} required />
          <SelectField label="Loại điều chỉnh" value={adjForm.method} onChange={(e) => setAdjForm({ ...adjForm, method: e.target.value })} options={METHODS} placeholder={null} />
          <div className="grid grid-cols-2 gap-4">
            <InputField label="Giờ vào" type="time" value={adjForm.inTime} onChange={(e) => setAdjForm({ ...adjForm, inTime: e.target.value })} required />
            <InputField label="Giờ ra" type="time" value={adjForm.outTime} onChange={(e) => setAdjForm({ ...adjForm, outTime: e.target.value })} required />
          </div>
          <InputField label="Lý do" value={adjForm.reason} onChange={(e) => setAdjForm({ ...adjForm, reason: e.target.value })} required />
          <InputField label="Minh chứng (mô tả/tham chiếu)" value={adjForm.evidence} onChange={(e) => setAdjForm({ ...adjForm, evidence: e.target.value })} />
          <div className="flex justify-end gap-3">
            <Button variant="outline" type="button" onClick={() => setAdjOpen(false)}>Hủy</Button>
            <Button type="submit" isLoading={adjBusy}>Gửi đề nghị</Button>
          </div>
        </form>
      </Modal>

      <PromptDialog
        isOpen={Boolean(decide)}
        title={decide?.approve ? 'Duyệt điều chỉnh công' : 'Từ chối điều chỉnh công'}
        label="Ghi chú"
        required={!decide?.approve}
        confirmLabel={decide?.approve ? 'Duyệt' : 'Từ chối'}
        confirmVariant={decide?.approve ? 'primary' : 'danger'}
        onClose={() => setDecide(null)}
        onSubmit={async (note) => {
          if (decide.approve) await attendanceApi.approveAdjustment(decide.adj.id, note || undefined);
          else await attendanceApi.rejectAdjustment(decide.adj.id, note);
          refresh();
        }}
      />

      <PromptDialog
        isOpen={Boolean(confirmRec)}
        title={`Xác nhận công ${confirmRec ? formatDate(confirmRec.workDate) : ''}`}
        description={confirmRec && `${confirmRec.employeeName}: ${confirmRec.reviewReason || 'cần rà soát'}`}
        label="Ghi chú xác nhận"
        required={false}
        confirmLabel="Xác nhận công"
        onClose={() => setConfirmRec(null)}
        onSubmit={async (note) => {
          await attendanceApi.confirm(confirmRec.id, note || undefined);
          refresh();
        }}
      />
    </div>
  );
}

export default AttendancePage;
