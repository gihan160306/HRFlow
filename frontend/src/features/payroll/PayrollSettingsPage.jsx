import React, { useState } from 'react';
import { Plus, Check, X, Tags } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import payrollApi from '../../api/payrollApi';
import employeeApi from '../../api/employeeApi';
import { configApi } from '../../api/recordsApi';
import Button from '../../components/common/Button';
import Tabs from '../../components/common/Tabs';
import DataTable from '../../components/common/DataTable';
import FormModal from '../../components/common/FormModal';
import PromptDialog from '../../components/common/PromptDialog';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate, formatDateTime, todayIso } from '../../utils/formatDate';
import { formatCurrency } from '../../utils/formatCurrency';

const TAX_CLASSES = [
  { value: 'TAXABLE', label: 'Chịu thuế' }, { value: 'NON_TAXABLE', label: 'Không chịu thuế' },
  { value: 'EXEMPT', label: 'Miễn thuế' }, { value: 'NEEDS_REVIEW', label: 'Cần rà soát' },
];
const ADJ_TYPES = [
  { value: 'GROSS', label: 'Điều chỉnh Gross' }, { value: 'TAX', label: 'Điều chỉnh thuế' },
  { value: 'INSURANCE', label: 'Điều chỉnh bảo hiểm' }, { value: 'NET', label: 'Điều chỉnh Net' },
];
const REGIONS = [{ value: 'REGION_I', label: 'Vùng I' }, { value: 'REGION_II', label: 'Vùng II' }, { value: 'REGION_III', label: 'Vùng III' }, { value: 'REGION_IV', label: 'Vùng IV' }];
const lbl = (list, v) => list.find((x) => x.value === v)?.label || v || '-';
const txt = (v) => (v && String(v).trim() ? String(v).trim() : undefined);
const box = 'bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden';

export function PayrollSettingsPage() {
  const { isHR, isAdmin, hasGrant } = useAuth();
  const canSeeAdj = hasGrant('VIEW') || hasGrant('PREPARE') || hasGrant('APPROVE');
  const [tab, setTab] = useState(canSeeAdj ? 'adjustments' : 'parameters');

  const adjustments = useAsync(() => payrollApi.adjustments(), [], { enabled: canSeeAdj });
  const periods = useAsync(() => payrollApi.periods(), [], { enabled: canSeeAdj });
  const params = useAsync(() => configApi.parameters(), []);
  const allowanceTypes = useAsync(() => configApi.allowanceTypes(), []);
  const people = useAsync(() => employeeApi.options(), [], { enabled: hasGrant('PREPARE') });

  const [adjOpen, setAdjOpen] = useState(false);
  const [paramOpen, setParamOpen] = useState(false);
  const [classifyFor, setClassifyFor] = useState(null);
  const [prompt, setPrompt] = useState(null);

  const lockedPeriods = (periods.data || []).filter((p) => p.status === 'LOCKED');

  const adjColumns = [
    { key: 'employeeName', header: 'Nhân viên', render: (a) => (<><p className="font-semibold text-slate-900">{a.employeeName}</p><p className="text-xs text-slate-500">{a.employeeCode}</p></>) },
    { key: 'originPeriod', header: 'Kỳ gốc', render: (a) => (<>{a.originPeriod}{a.appliedPeriod && <p className="text-xs text-slate-500">Áp dụng vào kỳ {a.appliedPeriod}</p>}</>) },
    { key: 'adjType', header: 'Loại', render: (a) => lbl(ADJ_TYPES, a.adjType) },
    { key: 'amount', header: 'Số tiền', render: (a) => <b className={Number(a.amount) < 0 ? 'text-red-600' : 'text-slate-900'}>{formatCurrency(a.amount)}</b> },
    { key: 'reason', header: 'Lý do / minh chứng', render: (a) => (<><p>{a.reason}</p><p className="text-xs text-slate-500">{a.evidence} • {a.sourceRef}</p></>) },
    { key: 'status', header: 'Trạng thái', render: (a) => (<><StatusBadge status={a.status === 'APPLIED' ? 'LOCKED' : a.status} label={a.status === 'APPLIED' ? 'Đã áp dụng' : undefined} /><p className="text-xs text-slate-400 mt-1">{a.createdByName}{a.approvedByName && ` → ${a.approvedByName}`}</p>{a.decisionNote && <p className="text-xs text-slate-500">{a.decisionNote}</p>}</>) },
    { key: 'actions', header: '', align: 'right', render: (a) => a.canApprove && (
      <div className="flex justify-end gap-1.5">
        <Button size="sm" icon={Check} onClick={() => setPrompt({ title: 'Duyệt điều chỉnh', label: 'Ghi chú', required: false, confirmLabel: 'Duyệt', run: (n) => payrollApi.approveAdjustment(a.id, n || undefined).then(adjustments.reload) })}>Duyệt</Button>
        <Button size="sm" variant="danger" icon={X} onClick={() => setPrompt({ title: 'Từ chối điều chỉnh', label: 'Lý do', required: true, variant: 'danger', confirmLabel: 'Từ chối', run: (n) => payrollApi.rejectAdjustment(a.id, n).then(adjustments.reload) })}>Từ chối</Button>
      </div>
    ) },
  ];

  const paramColumns = [
    { key: 'code', header: 'Tham số', render: (p) => (<><p className="font-semibold text-slate-900">{p.name}</p><p className="text-xs font-mono text-slate-500">{p.code}{p.region ? ` • ${lbl(REGIONS, p.region)}` : ''}</p></>) },
    { key: 'category', header: 'Nhóm', render: (p) => (p.category === 'LEGAL' ? 'Pháp lý' : 'Công ty') },
    { key: 'value', header: 'Giá trị', render: (p) => <b className="text-slate-900">{Number(p.value).toLocaleString('vi-VN')} <span className="font-normal text-slate-500">{p.unit}</span></b> },
    { key: 'range', header: 'Hiệu lực', render: (p) => `${formatDate(p.effectiveFrom)} – ${p.effectiveTo ? formatDate(p.effectiveTo) : 'nay'}` },
    { key: 'version', header: 'Phiên bản', render: (p) => `v${p.version}` },
    { key: 'status', header: 'Trạng thái', render: (p) => (<><span className={`px-2 py-0.5 rounded-full text-xs font-semibold border ${p.status === 'VERIFIED_SIM' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-amber-50 text-amber-700 border-amber-200'}`}>{p.status === 'VERIFIED_SIM' ? 'Đã xác nhận' : 'Cần rà soát'}</span>{p.confirmedBy && <p className="text-xs text-slate-400 mt-1">{p.confirmedBy}</p>}</>) },
    { key: 'sourceRef', header: 'Nguồn', render: (p) => p.sourceRef || '-' },
  ];

  const allowanceColumns = [
    { key: 'name', header: 'Loại phụ cấp', render: (t) => (<><p className="font-semibold text-slate-900">{t.name}</p><p className="text-xs font-mono text-slate-500">{t.code}</p></>) },
    { key: 'taxClass', header: 'Thuế TNCN', render: (t) => lbl(TAX_CLASSES, t.taxClass) },
    { key: 'insuranceIncluded', header: 'Tính vào lương đóng BH', render: (t) => (t.insuranceIncluded ? 'Có' : 'Không') },
    { key: 'includedInOtBase', header: 'Tính vào đơn giá OT', render: (t) => (t.includedInOtBase ? 'Có' : 'Không') },
    { key: 'description', header: 'Mô tả', render: (t) => t.description || '-' },
    ...(isHR ? [{ key: 'actions', header: '', align: 'right', render: (t) => <Button size="sm" variant="outline" icon={Tags} onClick={() => setClassifyFor(t)}>Phân loại</Button> }] : []),
  ];

  const tabs = [
    ...(canSeeAdj ? [{ key: 'adjustments', label: 'Điều chỉnh sau khóa', count: adjustments.data?.length }] : []),
    { key: 'parameters', label: 'Tham số payroll', count: params.data?.length },
    { key: 'allowances', label: 'Loại phụ cấp' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Cấu hình & điều chỉnh lương</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Kỳ lương đã khóa không sửa trực tiếp: sai sót được điều chỉnh bằng khoản adjustment có phê duyệt, áp dụng vào kỳ sau.{' '}
            <Link to="/payroll" className="text-blue-600 font-semibold">← Về danh sách kỳ lương</Link>
          </p>
        </div>
        {tab === 'adjustments' && hasGrant('PREPARE') && <Button icon={Plus} onClick={() => setAdjOpen(true)}>Tạo điều chỉnh</Button>}
        {tab === 'parameters' && isHR && <Button icon={Plus} onClick={() => setParamOpen(true)}>Thêm phiên bản tham số</Button>}
      </div>

      <Tabs tabs={tabs} active={tab} onChange={setTab} />

      <div className={box}>
        {tab === 'adjustments' && <DataTable columns={adjColumns} rows={adjustments.data} loading={adjustments.loading} error={adjustments.error} onRetry={adjustments.reload} emptyTitle="Chưa có điều chỉnh nào" />}
        {tab === 'parameters' && <DataTable columns={paramColumns} rows={params.data} loading={params.loading} error={params.error} onRetry={params.reload} emptyTitle="Chưa có tham số" />}
        {tab === 'allowances' && <DataTable columns={allowanceColumns} rows={allowanceTypes.data} loading={allowanceTypes.loading} error={allowanceTypes.error} onRetry={allowanceTypes.reload} emptyTitle="Chưa có loại phụ cấp" />}
      </div>
      {isAdmin && <p className="text-xs text-slate-400">Admin chỉ xem danh mục tham số, không có quyền xử lý dữ liệu lương.</p>}

      <FormModal
        isOpen={adjOpen}
        title="Tạo điều chỉnh sau khóa"
        description="Chỉ áp dụng cho kỳ đã khóa và nhân viên có dòng lương trong kỳ đó. Người tạo không được tự duyệt."
        fields={[
          { name: 'originPeriodId', label: 'Kỳ lương gốc (đã khóa)', type: 'select', options: lockedPeriods.map((p) => ({ value: p.id, label: `Tháng ${p.month}/${p.year}` })), required: true, half: true },
          { name: 'employeeId', label: 'Nhân viên', type: 'select', options: (people.data || []).map((p) => ({ value: p.id, label: `${p.code} – ${p.fullName}` })), required: true, half: true },
          { name: 'adjType', label: 'Loại điều chỉnh', type: 'select', options: ADJ_TYPES, required: true, half: true },
          { name: 'amount', label: 'Số tiền (âm = giảm)', type: 'number', step: 1, required: true, half: true },
          { name: 'taxClass', label: 'Phân loại thuế (cho Gross)', type: 'select', options: TAX_CLASSES, half: true },
          { name: 'sourceRef', label: 'Mã nguồn (duy nhất)', required: true, half: true },
          { name: 'reason', label: 'Lý do', required: true, half: true },
          { name: 'evidence', label: 'Minh chứng', required: true, half: true },
        ]}
        onClose={() => setAdjOpen(false)}
        onSubmit={async (v) => {
          await payrollApi.createAdjustment({
            originPeriodId: Number(v.originPeriodId), employeeId: Number(v.employeeId), adjType: v.adjType, amount: Number(v.amount),
            taxClass: txt(v.taxClass), reason: v.reason.trim(), evidence: v.evidence.trim(), sourceRef: v.sourceRef.trim(),
          });
          adjustments.reload();
        }}
      />

      <FormModal
        isOpen={paramOpen}
        title="Thêm phiên bản tham số payroll"
        description="Phiên bản mới đóng phiên bản trước; không áp dụng cho kỳ đã chốt công hoặc đã khóa."
        fields={[
          { name: 'code', label: 'Mã tham số', required: true, half: true },
          { name: 'name', label: 'Tên tham số', required: true, half: true },
          { name: 'category', label: 'Nhóm', type: 'select', options: [{ value: 'COMPANY', label: 'Công ty' }, { value: 'LEGAL', label: 'Pháp lý' }], required: true, half: true },
          { name: 'unit', label: 'Đơn vị', required: true, half: true },
          { name: 'value', label: 'Giá trị', type: 'number', step: 'any', required: true, half: true },
          { name: 'region', label: 'Vùng (nếu có)', type: 'select', options: REGIONS, half: true },
          { name: 'effectiveFrom', label: 'Hiệu lực từ', type: 'date', required: true, half: true },
          { name: 'sourceRef', label: 'Nguồn / căn cứ', required: true, half: true },
          { name: 'status', label: 'Trạng thái', type: 'select', options: [{ value: 'VERIFIED_SIM', label: 'Đã xác nhận' }, { value: 'NEEDS_REVIEW', label: 'Cần rà soát' }], required: true, half: true },
          { name: 'confirmedBy', label: 'Người xác nhận', half: true, helperText: 'Bắt buộc khi trạng thái là Đã xác nhận.' },
        ]}
        initial={{ category: 'COMPANY', status: 'VERIFIED_SIM', effectiveFrom: todayIso() }}
        onClose={() => setParamOpen(false)}
        onSubmit={async (v) => {
          await configApi.createParameter({
            code: v.code.trim(), name: v.name.trim(), category: v.category, value: Number(v.value), unit: v.unit.trim(),
            region: txt(v.region), effectiveFrom: v.effectiveFrom, sourceRef: v.sourceRef.trim(), status: v.status, confirmedBy: txt(v.confirmedBy),
          });
          params.reload();
        }}
      />

      <FormModal
        isOpen={Boolean(classifyFor)}
        title={`Phân loại phụ cấp – ${classifyFor?.name || ''}`}
        fields={[
          { name: 'taxClass', label: 'Thuế TNCN', type: 'select', options: TAX_CLASSES, required: true },
          { name: 'insuranceIncluded', label: 'Tính vào lương đóng bảo hiểm', type: 'checkbox', half: true },
          { name: 'includedInOtBase', label: 'Tính vào đơn giá OT', type: 'checkbox', half: true },
          { name: 'decisionRef', label: 'Căn cứ (số quyết định)', required: true },
        ]}
        initial={{ taxClass: classifyFor?.taxClass, insuranceIncluded: classifyFor?.insuranceIncluded, includedInOtBase: classifyFor?.includedInOtBase }}
        onClose={() => setClassifyFor(null)}
        onSubmit={async (v) => {
          await configApi.classifyAllowanceType(classifyFor.id, { taxClass: v.taxClass, insuranceIncluded: Boolean(v.insuranceIncluded), includedInOtBase: Boolean(v.includedInOtBase), decisionRef: v.decisionRef.trim() });
          allowanceTypes.reload();
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

export default PayrollSettingsPage;
