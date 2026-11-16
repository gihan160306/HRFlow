import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Send, Lock, XCircle, ClipboardList } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import overtimeApi from '../../api/overtimeApi';
import employeeApi from '../../api/employeeApi';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import InputField from '../../components/common/InputField';
import StatusBadge from '../../components/common/StatusBadge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import { formatDate, formatTime, todayIso } from '../../utils/formatDate';

const DAY_TYPES = { WORKDAY: 'Ngày làm việc', WEEKLY_OFF: 'Ngày nghỉ tuần', HOLIDAY: 'Ngày lễ' };

export function OvertimeSchedulePage() {
  const { isHR, isManager } = useAuth();
  const canPlan = isHR || isManager;

  const { data, loading, error, reload } = useAsync(() => overtimeApi.schedules(), []);
  const people = useAsync(() => employeeApi.options(), [], { enabled: canPlan });

  const [actionError, setActionError] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [registerFor, setRegisterFor] = useState(null);

  const act = async (fn) => {
    setActionError('');
    try {
      await fn();
      reload();
    } catch (err) {
      setActionError(err.message);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Lịch tăng ca (OT)</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            {canPlan ? 'Lập, công bố và đóng lịch OT; nhân viên đăng ký tự nguyện trong lịch được mời.' : 'Lịch OT đã công bố dành cho bạn.'}
          </p>
        </div>
        <div className="flex gap-3">
          <Link to="/overtime/registration"><Button variant="outline" icon={ClipboardList}>Đăng ký của tôi</Button></Link>
          <Link to="/overtime/actual"><Button variant="outline">Giờ thực tế</Button></Link>
          {canPlan && <Link to="/overtime/approval"><Button variant="outline">Duyệt đăng ký</Button></Link>}
          {canPlan && <Button icon={Plus} onClick={() => setCreateOpen(true)}>Lập lịch OT</Button>}
        </div>
      </div>

      {actionError && <ErrorMessage message={actionError} />}

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? (
          <Loading />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : !data?.length ? (
          <EmptyState title="Chưa có lịch OT" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Lịch OT</th>
                  <th className="px-4 py-3.5">Ngày & giờ</th>
                  <th className="px-4 py-3.5">Phòng ban</th>
                  <th className="px-4 py-3.5">Đăng ký</th>
                  <th className="px-4 py-3.5">Trạng thái</th>
                  <th className="px-4 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5">
                      <p className="font-bold text-slate-900">{s.title}</p>
                      {s.description && <p className="text-xs text-slate-500">{s.description}</p>}
                      <p className="text-xs text-slate-400">Lập bởi {s.createdByName}</p>
                    </td>
                    <td className="px-4 py-3.5">
                      {formatDate(s.workDate)}
                      <span className="block text-xs">{formatTime(s.startTime)} – {formatTime(s.endTime)} • {DAY_TYPES[s.dayType] || s.dayType}</span>
                    </td>
                    <td className="px-4 py-3.5">{s.departmentName || '-'}</td>
                    <td className="px-4 py-3.5">{s.registrations}{s.targetNames && ` / ${s.targetNames.length} được mời`}</td>
                    <td className="px-4 py-3.5"><StatusBadge status={s.status} /></td>
                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      {s.status === 'PUBLISHED' && !s.registeredByMe && (
                        <Button size="sm" onClick={() => setRegisterFor(s)}>Đăng ký OT</Button>
                      )}
                      {s.registeredByMe && <span className="text-xs text-emerald-700 font-semibold mr-2">Đã đăng ký</span>}
                      {canPlan && s.status === 'DRAFT' && (
                        <Button size="sm" icon={Send} onClick={() => act(() => overtimeApi.publishSchedule(s.id))}>Công bố</Button>
                      )}
                      {canPlan && s.status === 'PUBLISHED' && (
                        <Button size="sm" variant="outline" icon={Lock} className="ml-2" onClick={() => act(() => overtimeApi.closeSchedule(s.id))}>Đóng</Button>
                      )}
                      {canPlan && ['DRAFT', 'PUBLISHED'].includes(s.status) && (
                        <Button size="sm" variant="ghost" icon={XCircle} className="ml-2 text-red-600" onClick={() => act(() => overtimeApi.cancelSchedule(s.id))}>Hủy</Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <CreateScheduleModal
        isOpen={createOpen}
        people={people.data || []}
        onClose={() => setCreateOpen(false)}
        onSaved={() => { setCreateOpen(false); reload(); }}
      />
      <RegisterModal schedule={registerFor} onClose={() => setRegisterFor(null)} onSaved={() => { setRegisterFor(null); reload(); }} />
    </div>
  );
}

function CreateScheduleModal({ isOpen, people, onClose, onSaved }) {
  const empty = { title: '', workDate: todayIso(), startTime: '18:00', endTime: '20:00', description: '' };
  const [form, setForm] = useState(empty);
  const [targets, setTargets] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  React.useEffect(() => {
    if (isOpen) { setForm({ ...empty, workDate: todayIso() }); setTargets([]); setError(''); }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  const toggle = (id) => setTargets((t) => (t.includes(id) ? t.filter((x) => x !== id) : [...t, id]));

  const submit = async (e) => {
    e.preventDefault();
    if (targets.length === 0) return setError('Phải chọn ít nhất một nhân viên được mời.');
    setBusy(true);
    setError('');
    try {
      await overtimeApi.createSchedule({
        title: form.title.trim(), workDate: form.workDate, startTime: form.startTime, endTime: form.endTime,
        description: form.description.trim() || undefined, targetEmployeeIds: targets,
      });
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal isOpen={isOpen} title="Lập lịch OT" onClose={onClose} maxWidth="max-w-xl">
      <form onSubmit={submit} className="space-y-4">
        {error && <ErrorMessage message={error} />}
        <InputField label="Tiêu đề" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
        <div className="grid grid-cols-3 gap-4">
          <InputField label="Ngày" type="date" min={todayIso()} value={form.workDate} onChange={(e) => setForm({ ...form, workDate: e.target.value })} required />
          <InputField label="Từ" type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} required />
          <InputField label="Đến" type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} required />
        </div>
        <InputField label="Mô tả công việc" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <div>
          <p className="text-sm font-medium text-slate-700 mb-1">Nhân viên được mời ({targets.length})</p>
          <div className="max-h-48 overflow-y-auto border border-slate-200 rounded-lg divide-y divide-slate-100">
            {people.map((p) => (
              <label key={p.id} className="flex items-center px-3 py-2 text-sm hover:bg-slate-50 cursor-pointer">
                <input type="checkbox" checked={targets.includes(p.id)} onChange={() => toggle(p.id)} className="rounded border-slate-300 text-blue-600 mr-3" />
                {p.code} – {p.fullName}
              </label>
            ))}
          </div>
        </div>
        <div className="flex justify-end gap-3">
          <Button variant="outline" type="button" onClick={onClose}>Hủy</Button>
          <Button type="submit" isLoading={busy}>Lưu bản nháp</Button>
        </div>
      </form>
    </Modal>
  );
}

function RegisterModal({ schedule, onClose, onSaved }) {
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  React.useEffect(() => { setConsent(false); setError(''); }, [schedule]);

  const submit = async () => {
    setBusy(true);
    setError('');
    try {
      await overtimeApi.register({ scheduleId: schedule.id, consent });
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal isOpen={Boolean(schedule)} title="Đăng ký OT" onClose={onClose}>
      {schedule && (
        <div className="space-y-4">
          {error && <ErrorMessage message={error} />}
          <p className="text-sm text-slate-700">
            <b>{schedule.title}</b> – {formatDate(schedule.workDate)}, {formatTime(schedule.startTime)} – {formatTime(schedule.endTime)}
          </p>
          <label className="flex items-start text-sm text-slate-700">
            <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} className="rounded border-slate-300 text-blue-600 mr-2 mt-0.5" />
            Tôi tự nguyện đăng ký làm thêm giờ theo lịch trên và đồng ý với giới hạn giờ OT theo quy định.
          </label>
          <div className="flex justify-end gap-3">
            <Button variant="outline" onClick={onClose}>Hủy</Button>
            <Button onClick={submit} disabled={!consent} isLoading={busy}>Đăng ký</Button>
          </div>
        </div>
      )}
    </Modal>
  );
}

export default OvertimeSchedulePage;
