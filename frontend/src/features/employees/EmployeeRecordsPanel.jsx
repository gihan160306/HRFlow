import React, { useState } from 'react';
import { Plus, CalendarX2, Pencil } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import recordsApi, { configApi } from '../../api/recordsApi';
import departmentApi, { catalogApi } from '../../api/departmentApi';
import employeeApi from '../../api/employeeApi';
import Button from '../../components/common/Button';
import Tabs from '../../components/common/Tabs';
import DataTable from '../../components/common/DataTable';
import FormModal from '../../components/common/FormModal';
import { formatDate, todayIso } from '../../utils/formatDate';
import { formatCurrency } from '../../utils/formatCurrency';

const STATUS = [{ value: 'CONFIRMED', label: 'Đã xác nhận' }, { value: 'NEEDS_REVIEW', label: 'Cần rà soát' }];
const RESIDENCY = [{ value: 'RESIDENT', label: 'Cư trú' }, { value: 'NON_RESIDENT', label: 'Không cư trú' }];
const TAX_METHOD = [
  { value: 'PROGRESSIVE', label: 'Biểu thuế lũy tiến' }, { value: 'FLAT_10', label: 'Khấu trừ 10%' },
  { value: 'NON_RESIDENT_20', label: 'Không cư trú 20%' }, { value: 'MANUAL', label: 'Thủ công' },
];
const RELATIONSHIPS = ['Con', 'Vợ/Chồng', 'Cha', 'Mẹ', 'Anh/Chị/Em', 'Khác'].map((v) => ({ value: v, label: v }));
const lbl = (list, v) => list.find((x) => x.value === v)?.label || v || '-';
const range = (a, b) => `${formatDate(a)} – ${b ? formatDate(b) : 'nay'}`;
const yesNo = (v) => (v ? 'Có' : 'Không');
const num = (v) => (v === '' || v === undefined ? undefined : Number(v));
const txt = (v) => (v && String(v).trim() ? String(v).trim() : undefined);

/**
 * Hồ sơ có hiệu lực của nhân viên (chỉ HR ghi). Mỗi bản ghi mới đóng bản ghi hiệu lực trước đó,
 * không sửa đè dữ liệu cũ; backend từ chối thay đổi lùi vào kỳ lương đã chốt.
 */
export function EmployeeRecordsPanel({ employeeId, onChanged }) {
  const [tab, setTab] = useState('salary');
  const [modal, setModal] = useState(null); // { kind, row? }

  const salaries = useAsync(() => recordsApi.salaries(employeeId), [employeeId]);
  const allowances = useAsync(() => recordsApi.allowances(employeeId), [employeeId]);
  const assignments = useAsync(() => recordsApi.assignments(employeeId), [employeeId]);
  const dependents = useAsync(() => recordsApi.dependents(employeeId), [employeeId]);
  const insurance = useAsync(() => recordsApi.insurance(employeeId), [employeeId]);
  const taxes = useAsync(() => recordsApi.taxProfiles(employeeId), [employeeId]);
  const schedules = useAsync(() => recordsApi.schedules(employeeId), [employeeId]);

  const types = useAsync(() => configApi.allowanceTypes(), []);
  const depts = useAsync(() => departmentApi.list(), []);
  const titles = useAsync(() => catalogApi.jobTitles(), []);
  const workSchedules = useAsync(() => catalogApi.workSchedules(), []);
  const people = useAsync(() => employeeApi.options(), []);

  const close = () => setModal(null);
  const after = (...loaders) => { loaders.forEach((l) => l.reload()); onChanged?.(); };

  const sections = {
    salary: {
      label: 'Lương cơ bản', data: salaries, addLabel: 'Thêm mức lương',
      columns: [
        { key: 'baseSalary', header: 'Lương cơ bản', render: (r) => <b className="text-slate-900">{formatCurrency(r.baseSalary)}</b> },
        { key: 'range', header: 'Hiệu lực', render: (r) => range(r.effectiveFrom, r.effectiveTo) },
        { key: 'reason', header: 'Lý do' },
        { key: 'decisionRef', header: 'Quyết định', render: (r) => r.decisionRef || '-' },
      ],
      fields: [
        { name: 'baseSalary', label: 'Lương cơ bản (VND/tháng)', type: 'number', min: 1, required: true, half: true },
        { name: 'effectiveFrom', label: 'Hiệu lực từ', type: 'date', required: true, half: true },
        { name: 'decisionRef', label: 'Số quyết định', half: true },
        { name: 'reason', label: 'Lý do', required: true, half: true },
      ],
      initial: () => ({ effectiveFrom: todayIso() }),
      submit: (v) => recordsApi.addSalary(employeeId, { baseSalary: Number(v.baseSalary), effectiveFrom: v.effectiveFrom, reason: v.reason.trim(), decisionRef: txt(v.decisionRef) }),
      reload: () => after(salaries),
    },
    allowance: {
      label: 'Phụ cấp', data: allowances, addLabel: 'Thêm phụ cấp',
      columns: [
        { key: 'allowanceName', header: 'Loại phụ cấp', render: (r) => (<><p className="font-semibold text-slate-900">{r.allowanceName}</p><p className="text-xs text-slate-500">{r.allowanceCode}</p></>) },
        { key: 'amount', header: 'Số tiền', render: (r) => formatCurrency(r.amount) },
        { key: 'range', header: 'Hiệu lực', render: (r) => range(r.effectiveFrom, r.effectiveTo) },
        { key: 'decisionRef', header: 'Quyết định' },
        { key: 'status', header: 'Phân loại thuế', render: (r) => lbl(STATUS, r.status) },
        { key: 'actions', header: '', align: 'right', render: (r) => !r.effectiveTo && (
          <Button size="sm" variant="outline" icon={CalendarX2} onClick={() => setModal({ kind: 'allowanceEnd', row: r })}>Kết thúc</Button>
        ) },
      ],
      fields: [
        { name: 'allowanceTypeId', label: 'Loại phụ cấp', type: 'select', options: (types.data || []).map((t) => ({ value: t.id, label: t.name })), required: true },
        { name: 'amount', label: 'Số tiền (VND/tháng)', type: 'number', min: 1, required: true, half: true },
        { name: 'decisionRef', label: 'Số quyết định', required: true, half: true },
        { name: 'effectiveFrom', label: 'Hiệu lực từ', type: 'date', required: true, half: true },
        { name: 'effectiveTo', label: 'Đến ngày (nếu có)', type: 'date', half: true },
      ],
      initial: () => ({ effectiveFrom: todayIso() }),
      submit: (v) => recordsApi.addAllowance(employeeId, {
        allowanceTypeId: Number(v.allowanceTypeId), amount: Number(v.amount), effectiveFrom: v.effectiveFrom,
        effectiveTo: txt(v.effectiveTo), decisionRef: v.decisionRef.trim(),
      }),
      reload: () => after(allowances),
    },
    assignment: {
      label: 'Điều chuyển', data: assignments, addLabel: 'Điều chuyển / bổ nhiệm',
      columns: [
        { key: 'departmentName', header: 'Phòng ban' },
        { key: 'jobTitleName', header: 'Chức danh' },
        { key: 'managerName', header: 'Quản lý trực tiếp', render: (r) => r.managerName || '-' },
        { key: 'range', header: 'Hiệu lực', render: (r) => range(r.effectiveFrom, r.effectiveTo) },
        { key: 'reason', header: 'Lý do', render: (r) => (<>{r.reason}{r.decisionRef && <span className="block text-xs text-slate-400">QĐ: {r.decisionRef}</span>}</>) },
      ],
      fields: [
        { name: 'departmentId', label: 'Phòng ban', type: 'select', options: (depts.data || []).filter((d) => d.active).map((d) => ({ value: d.id, label: d.name })), required: true, half: true },
        { name: 'jobTitleId', label: 'Chức danh', type: 'select', options: (titles.data || []).map((t) => ({ value: t.id, label: t.name })), required: true, half: true },
        { name: 'managerId', label: 'Quản lý trực tiếp', type: 'select', options: (people.data || []).filter((p) => p.id !== employeeId).map((p) => ({ value: p.id, label: `${p.code} – ${p.fullName}` })) },
        { name: 'effectiveFrom', label: 'Hiệu lực từ', type: 'date', required: true, half: true },
        { name: 'decisionRef', label: 'Số quyết định', half: true },
        { name: 'reason', label: 'Lý do', required: true },
      ],
      initial: () => ({ effectiveFrom: todayIso() }),
      submit: (v) => recordsApi.addAssignment(employeeId, {
        departmentId: Number(v.departmentId), jobTitleId: Number(v.jobTitleId), managerId: num(v.managerId),
        effectiveFrom: v.effectiveFrom, reason: v.reason.trim(), decisionRef: txt(v.decisionRef),
      }),
      reload: () => after(assignments),
    },
    dependent: {
      label: 'Người phụ thuộc', data: dependents, addLabel: 'Thêm người phụ thuộc',
      columns: [
        { key: 'fullName', header: 'Họ tên', render: (r) => <b className="text-slate-900">{r.fullName}</b> },
        { key: 'relationship', header: 'Quan hệ' },
        { key: 'range', header: 'Giảm trừ từ – đến', render: (r) => range(r.validFrom, r.validTo) },
        { key: 'docRef', header: 'Hồ sơ chứng minh', render: (r) => r.docRef || '-' },
        { key: 'verified', header: 'Đã xác minh', render: (r) => yesNo(r.verified) },
        { key: 'actions', header: '', align: 'right', render: (r) => (
          <Button size="sm" variant="outline" icon={Pencil} onClick={() => setModal({ kind: 'dependent', row: r })}>Sửa</Button>
        ) },
      ],
      fields: [
        { name: 'fullName', label: 'Họ tên', required: true, half: true },
        { name: 'relationship', label: 'Quan hệ', type: 'select', options: RELATIONSHIPS, required: true, half: true },
        { name: 'validFrom', label: 'Giảm trừ từ', type: 'date', required: true, half: true },
        { name: 'validTo', label: 'Đến ngày (nếu có)', type: 'date', half: true },
        { name: 'docRef', label: 'Hồ sơ chứng minh (số CCCD/giấy khai sinh...)', helperText: 'Bắt buộc nếu đánh dấu đã xác minh.' },
        { name: 'verified', label: 'HR đã xác minh hồ sơ', type: 'checkbox' },
      ],
      initial: (row) => (row ? { fullName: row.fullName, relationship: row.relationship, validFrom: row.validFrom, validTo: row.validTo || '', docRef: row.docRef || '', verified: row.verified } : { validFrom: todayIso() }),
      submit: (v, row) => {
        const body = { fullName: v.fullName.trim(), relationship: v.relationship, validFrom: v.validFrom, validTo: txt(v.validTo), docRef: txt(v.docRef), verified: Boolean(v.verified) };
        return row ? recordsApi.updateDependent(row.id, body) : recordsApi.addDependent(employeeId, body);
      },
      reload: () => after(dependents),
    },
    insurance: {
      label: 'Bảo hiểm', data: insurance, addLabel: 'Thêm hồ sơ bảo hiểm',
      columns: [
        { key: 'range', header: 'Hiệu lực', render: (r) => range(r.effectiveFrom, r.effectiveTo) },
        { key: 'bhxh', header: 'BHXH', render: (r) => yesNo(r.bhxh) },
        { key: 'bhyt', header: 'BHYT', render: (r) => yesNo(r.bhyt) },
        { key: 'bhtn', header: 'BHTN', render: (r) => yesNo(r.bhtn) },
        { key: 'insuranceSalary', header: 'Lương đóng BH', render: (r) => formatCurrency(r.insuranceSalary) },
        { key: 'status', header: 'Trạng thái', render: (r) => lbl(STATUS, r.status) },
        { key: 'note', header: 'Ghi chú', render: (r) => r.note || '-' },
      ],
      fields: [
        { name: 'effectiveFrom', label: 'Hiệu lực từ', type: 'date', required: true, half: true },
        { name: 'insuranceSalary', label: 'Lương đóng bảo hiểm (VND)', type: 'number', min: 0, required: true, half: true },
        { name: 'bhxh', label: 'Tham gia BHXH', type: 'checkbox', half: true },
        { name: 'bhyt', label: 'Tham gia BHYT', type: 'checkbox', half: true },
        { name: 'bhtn', label: 'Tham gia BHTN', type: 'checkbox', half: true },
        { name: 'status', label: 'Trạng thái', type: 'select', options: STATUS, required: true, half: true },
        { name: 'note', label: 'Ghi chú' },
      ],
      initial: () => ({ effectiveFrom: todayIso(), bhxh: true, bhyt: true, bhtn: true, status: 'CONFIRMED' }),
      submit: (v) => recordsApi.addInsurance(employeeId, {
        effectiveFrom: v.effectiveFrom, bhxh: Boolean(v.bhxh), bhyt: Boolean(v.bhyt), bhtn: Boolean(v.bhtn),
        insuranceSalary: Number(v.insuranceSalary), status: v.status, note: txt(v.note),
      }),
      reload: () => after(insurance),
    },
    tax: {
      label: 'Thuế TNCN', data: taxes, addLabel: 'Thêm hồ sơ thuế',
      columns: [
        { key: 'range', header: 'Hiệu lực', render: (r) => range(r.effectiveFrom, r.effectiveTo) },
        { key: 'residency', header: 'Cư trú', render: (r) => lbl(RESIDENCY, r.residency) },
        { key: 'method', header: 'Phương pháp', render: (r) => lbl(TAX_METHOD, r.method) },
        { key: 'taxCode', header: 'Mã số thuế', render: (r) => r.taxCode || '-' },
        { key: 'commitment08', header: 'Cam kết 08', render: (r) => yesNo(r.commitment08) },
        { key: 'status', header: 'Trạng thái', render: (r) => lbl(STATUS, r.status) },
      ],
      fields: [
        { name: 'effectiveFrom', label: 'Hiệu lực từ', type: 'date', required: true, half: true },
        { name: 'taxCode', label: 'Mã số thuế', half: true },
        { name: 'residency', label: 'Cư trú', type: 'select', options: RESIDENCY, required: true, half: true },
        { name: 'method', label: 'Phương pháp khấu trừ', type: 'select', options: TAX_METHOD, required: true, half: true },
        { name: 'commitment08', label: 'Có cam kết 08 (khấu trừ 10%)', type: 'checkbox', half: true },
        { name: 'commitmentDocRef', label: 'Số hồ sơ cam kết', half: true },
        { name: 'status', label: 'Trạng thái', type: 'select', options: STATUS, required: true, half: true },
        { name: 'note', label: 'Ghi chú', half: true },
      ],
      initial: () => ({ effectiveFrom: todayIso(), residency: 'RESIDENT', method: 'PROGRESSIVE', status: 'CONFIRMED' }),
      submit: (v) => recordsApi.addTaxProfile(employeeId, {
        effectiveFrom: v.effectiveFrom, residency: v.residency, method: v.method, taxCode: txt(v.taxCode),
        commitment08: Boolean(v.commitment08), commitmentDocRef: txt(v.commitmentDocRef), status: v.status, note: txt(v.note),
      }),
      reload: () => after(taxes),
    },
    schedule: {
      label: 'Lịch làm việc', data: schedules, addLabel: 'Đổi lịch làm việc',
      columns: [
        { key: 'scheduleName', header: 'Lịch làm việc', render: (r) => <b className="text-slate-900">{r.scheduleName}</b> },
        { key: 'range', header: 'Hiệu lực', render: (r) => range(r.effectiveFrom, r.effectiveTo) },
      ],
      fields: [
        { name: 'scheduleId', label: 'Lịch làm việc', type: 'select', options: (workSchedules.data || []).map((s) => ({ value: s.id, label: s.name })), required: true },
        { name: 'effectiveFrom', label: 'Hiệu lực từ', type: 'date', required: true, half: true },
        { name: 'reason', label: 'Lý do', required: true, half: true },
      ],
      initial: () => ({ effectiveFrom: todayIso() }),
      submit: (v) => recordsApi.addSchedule(employeeId, { scheduleId: Number(v.scheduleId), effectiveFrom: v.effectiveFrom, reason: v.reason.trim() }),
      reload: () => after(schedules),
    },
  };

  const current = sections[tab];
  // Biểu mẫu đang mở: kind trùng khóa section, riêng 'dependent' dùng chung cho thêm/sửa.
  const formSection = modal && sections[modal.kind === 'allowanceEnd' ? 'allowance' : modal.kind];

  return (
    <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
      <div className="px-5 pt-4 flex flex-wrap items-center justify-between gap-3">
        <h3 className="text-base font-bold text-slate-800">Hồ sơ có hiệu lực</h3>
        <Button size="sm" icon={Plus} onClick={() => setModal({ kind: tab })}>{current.addLabel}</Button>
      </div>
      <div className="px-5">
        <Tabs tabs={Object.entries(sections).map(([key, s]) => ({ key, label: s.label, count: s.data.data?.length }))} active={tab} onChange={setTab} />
      </div>
      <DataTable columns={current.columns} rows={current.data.data} loading={current.data.loading} error={current.data.error}
        onRetry={current.data.reload} emptyTitle="Chưa có bản ghi" />

      {formSection && modal.kind !== 'allowanceEnd' && (
        <FormModal
          isOpen
          title={modal.row ? `Sửa – ${formSection.label}` : formSection.addLabel}
          fields={formSection.fields}
          initial={formSection.initial(modal.row)}
          onClose={close}
          onSubmit={async (v) => { await formSection.submit(v, modal.row); formSection.reload(); }}
        />
      )}

      <FormModal
        isOpen={modal?.kind === 'allowanceEnd'}
        title={`Kết thúc phụ cấp – ${modal?.row?.allowanceName || ''}`}
        fields={[
          { name: 'effectiveTo', label: 'Kết thúc ngày', type: 'date', required: true, half: true },
          { name: 'reason', label: 'Lý do', required: true, half: true },
        ]}
        initial={{ effectiveTo: todayIso() }}
        submitLabel="Kết thúc phụ cấp"
        onClose={close}
        onSubmit={async (v) => { await recordsApi.endAllowance(modal.row.id, { effectiveTo: v.effectiveTo, reason: v.reason.trim() }); sections.allowance.reload(); }}
      />
    </section>
  );
}

export default EmployeeRecordsPanel;
