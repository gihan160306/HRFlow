import React, { useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import { configApi } from '../../api/recordsApi';
import Button from '../../components/common/Button';
import Tabs from '../../components/common/Tabs';
import DataTable from '../../components/common/DataTable';
import FormModal from '../../components/common/FormModal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import InputField from '../../components/common/InputField';
import { formatDate, formatTime, todayIso } from '../../utils/formatDate';
import { formatCurrency } from '../../utils/formatCurrency';

const box = 'bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden';
const DOW = { 1: 'Thứ 2', 2: 'Thứ 3', 3: 'Thứ 4', 4: 'Thứ 5', 5: 'Thứ 6', 6: 'Thứ 7', 7: 'Chủ nhật' };
const yn = (v) => (v ? 'Có' : 'Không');
const txt = (v) => (v && String(v).trim() ? String(v).trim() : undefined);

export function CatalogPage() {
  const { isHR } = useAuth();
  const [tab, setTab] = useState('holidays');
  const [year, setYear] = useState(String(new Date().getFullYear()));
  const [holidayOpen, setHolidayOpen] = useState(false);
  const [deleting, setDeleting] = useState(null);
  const [deleteError, setDeleteError] = useState('');

  const holidays = useAsync(() => configApi.holidays({ year }), [year]);
  const schedules = useAsync(() => configApi.workSchedules(), []);
  const leaveTypes = useAsync(() => configApi.leaveTypes(), []);
  const brackets = useAsync(() => configApi.taxBrackets(), []);

  const holidayColumns = [
    { key: 'holidayDate', header: 'Ngày', render: (h) => <b className="text-slate-900">{formatDate(h.holidayDate)}</b> },
    { key: 'name', header: 'Tên ngày lễ' },
    { key: 'paid', header: 'Hưởng lương', render: (h) => yn(h.paid) },
    { key: 'note', header: 'Ghi chú', render: (h) => h.note || '-' },
    ...(isHR ? [{ key: 'actions', header: '', align: 'right', render: (h) => <Button size="sm" variant="ghost" className="text-red-600" icon={Trash2} onClick={() => { setDeleteError(''); setDeleting(h); }}>Xóa</Button> }] : []),
  ];

  const scheduleColumns = [
    { key: 'name', header: 'Lịch làm việc', render: (s) => (<><p className="font-semibold text-slate-900">{s.name}</p><p className="text-xs text-slate-500">{s.code} • {s.description || s.scheduleType}</p></>) },
    { key: 'days', header: 'Ca làm việc', render: (s) => (
      <div className="space-y-0.5 text-xs">
        {(s.days || []).map((d) => (
          <p key={d.dayOfWeek}>
            <span className="inline-block w-16 font-semibold">{DOW[d.dayOfWeek] || d.dayOfWeek}</span>
            {formatTime(d.morningStart)}–{formatTime(d.morningEnd)}
            {d.afternoonStart && ` • ${formatTime(d.afternoonStart)}–${formatTime(d.afternoonEnd)}`}
          </p>
        ))}
      </div>
    ) },
  ];

  const leaveColumns = [
    { key: 'name', header: 'Loại nghỉ', render: (t) => (<><p className="font-semibold text-slate-900">{t.name}</p><p className="text-xs font-mono text-slate-500">{t.code}</p></>) },
    { key: 'paid', header: 'Hưởng lương', render: (t) => yn(t.paid) },
    { key: 'deductBalance', header: 'Trừ phép năm', render: (t) => yn(t.deductBalance) },
    { key: 'requiresEvidence', header: 'Cần minh chứng', render: (t) => yn(t.requiresEvidence) },
  ];

  const bracketColumns = [
    { key: 'setCode', header: 'Biểu thuế' },
    { key: 'effectiveFrom', header: 'Hiệu lực từ', render: (b) => formatDate(b.effectiveFrom) },
    { key: 'seq', header: 'Bậc' },
    { key: 'range', header: 'Thu nhập tính thuế', render: (b) => `${formatCurrency(b.lowerBound)} – ${b.upperBound == null ? 'trở lên' : formatCurrency(b.upperBound)}` },
    { key: 'rate', header: 'Thuế suất', render: (b) => `${Number(b.rate) * (Number(b.rate) <= 1 ? 100 : 1)}%` },
    { key: 'sourceRef', header: 'Căn cứ', render: (b) => b.sourceRef || '-' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Danh mục</h1>
          <p className="text-sm text-slate-500 mt-0.5">Ngày lễ, lịch làm việc, loại nghỉ phép và biểu thuế dùng để chấm công, nghỉ phép và tính lương.</p>
        </div>
        {isHR && tab === 'holidays' && <Button icon={Plus} onClick={() => setHolidayOpen(true)}>Thêm ngày lễ</Button>}
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <Tabs tabs={[{ key: 'holidays', label: 'Ngày lễ' }, { key: 'schedules', label: 'Lịch làm việc' }, { key: 'leave', label: 'Loại nghỉ phép' }, { key: 'tax', label: 'Biểu thuế TNCN' }]} active={tab} onChange={setTab} />
        {tab === 'holidays' && <InputField id="ct-year" label="Năm" type="number" className="w-28 ml-auto" value={year} onChange={(e) => setYear(e.target.value)} />}
      </div>

      <div className={box}>
        {tab === 'holidays' && <DataTable columns={holidayColumns} rows={holidays.data} loading={holidays.loading} error={holidays.error} onRetry={holidays.reload} emptyTitle={`Chưa khai báo ngày lễ năm ${year}`} />}
        {tab === 'schedules' && <DataTable columns={scheduleColumns} rows={schedules.data} loading={schedules.loading} error={schedules.error} onRetry={schedules.reload} />}
        {tab === 'leave' && <DataTable columns={leaveColumns} rows={leaveTypes.data} loading={leaveTypes.loading} error={leaveTypes.error} onRetry={leaveTypes.reload} />}
        {tab === 'tax' && <DataTable columns={bracketColumns} rows={brackets.data} loading={brackets.loading} error={brackets.error} onRetry={brackets.reload} />}
      </div>

      <FormModal
        isOpen={holidayOpen}
        title="Thêm ngày lễ"
        description="Không thêm được vào kỳ lương đã chốt công."
        fields={[
          { name: 'holidayDate', label: 'Ngày', type: 'date', required: true, half: true },
          { name: 'name', label: 'Tên ngày lễ', required: true, half: true },
          { name: 'paid', label: 'Hưởng lương', type: 'checkbox', half: true },
          { name: 'note', label: 'Ghi chú', half: true },
        ]}
        initial={{ holidayDate: todayIso(), paid: true }}
        onClose={() => setHolidayOpen(false)}
        onSubmit={async (v) => { await configApi.addHoliday({ holidayDate: v.holidayDate, name: v.name.trim(), paid: Boolean(v.paid), note: txt(v.note) }); holidays.reload(); }}
      />

      <ConfirmDialog
        isOpen={Boolean(deleting)}
        title="Xóa ngày lễ"
        message={deleting ? `Xóa "${deleting.name}" (${formatDate(deleting.holidayDate)})?${deleteError ? ` — ${deleteError}` : ''}` : ''}
        confirmLabel="Xóa"
        confirmVariant="danger"
        onCancel={() => setDeleting(null)}
        onConfirm={async () => {
          try { await configApi.deleteHoliday(deleting.id); setDeleting(null); holidays.reload(); } catch (err) { setDeleteError(err.message); }
        }}
      />
    </div>
  );
}

export default CatalogPage;
