import React, { useState } from 'react';
import { Plus, Check, X, Tags } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import incomeApi from '../../api/incomeApi';
import employeeApi from '../../api/employeeApi';
import Button from '../../components/common/Button';
import Tabs from '../../components/common/Tabs';
import DataTable from '../../components/common/DataTable';
import FormModal from '../../components/common/FormModal';
import PromptDialog from '../../components/common/PromptDialog';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate, formatDateTime } from '../../utils/formatDate';
import { formatCurrency } from '../../utils/formatCurrency';

const ITEM_TYPES = [
  { value: 'BONUS', label: 'Thưởng' }, { value: 'OTHER_ADDITION', label: 'Khoản cộng khác' },
  { value: 'REIMBURSEMENT', label: 'Hoàn ứng / hoàn chi' }, { value: 'DEDUCTION', label: 'Khấu trừ' },
  { value: 'COMPENSATION', label: 'Bồi thường' }, { value: 'ADVANCE_RECOVERY', label: 'Thu hồi tạm ứng' },
];
const NEEDS_BASIS = ['DEDUCTION', 'COMPENSATION', 'ADVANCE_RECOVERY'];
const TAX_CLASSES = [
  { value: 'TAXABLE', label: 'Chịu thuế' }, { value: 'NON_TAXABLE', label: 'Không chịu thuế' },
  { value: 'EXEMPT', label: 'Miễn thuế' }, { value: 'NEEDS_REVIEW', label: 'Cần rà soát' },
];
const SALE_STATUS = { DRAFT: 'Chờ chủ hợp đồng xác nhận', OWNER_CONFIRMED: 'Chờ kế toán xác nhận thu tiền', CONFIRMED: 'Đã xác nhận', REJECTED: 'Từ chối' };
const lbl = (list, v) => list.find((x) => x.value === v)?.label || v || '-';
const box = 'bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden';
const txt = (v) => (v && String(v).trim() ? String(v).trim() : undefined);

const now = new Date();

export function IncomePage() {
  const { isHR } = useAuth();
  const [tab, setTab] = useState('items');
  const [year, setYear] = useState(String(now.getFullYear()));
  const [month, setMonth] = useState('');

  const query = { year: year || undefined, month: month || undefined };
  const items = useAsync(() => incomeApi.items(query), [year, month]);
  const sales = useAsync(() => incomeApi.sales(query), [year, month]);
  const people = useAsync(() => employeeApi.options(), []);

  const [itemOpen, setItemOpen] = useState(false);
  const [saleOpen, setSaleOpen] = useState(false);
  const [classifyFor, setClassifyFor] = useState(null);
  const [prompt, setPrompt] = useState(null); // { title, label, confirmLabel, variant, required, run }

  const peopleOptions = (people.data || []).map((p) => ({ value: p.id, label: `${p.code} – ${p.fullName}` }));

  const itemColumns = [
    { key: 'employeeName', header: 'Nhân viên', render: (i) => (<><p className="font-semibold text-slate-900">{i.employeeName}</p><p className="text-xs text-slate-500">{i.employeeCode}</p></>) },
    { key: 'period', header: 'Kỳ', render: (i) => `${String(i.periodMonth).padStart(2, '0')}/${i.periodYear}` },
    { key: 'name', header: 'Khoản', render: (i) => (<><p className="font-semibold">{i.name}</p><p className="text-xs text-slate-500">{lbl(ITEM_TYPES, i.itemType)} • {i.sourceRef}</p></>) },
    { key: 'amount', header: 'Số tiền', render: (i) => <b className="text-slate-900">{formatCurrency(i.amount)}</b> },
    { key: 'taxClass', header: 'Thuế', render: (i) => lbl(TAX_CLASSES, i.taxClass) },
    { key: 'status', header: 'Trạng thái', render: (i) => (<><StatusBadge status={i.status} />{i.confirmedByName && <p className="text-xs text-slate-400 mt-1">{i.confirmedByName}</p>}</>) },
    { key: 'actions', header: '', align: 'right', render: (i) => i.status === 'DRAFT' && (
      <div className="flex justify-end gap-1.5">
        {isHR && <Button size="sm" variant="outline" icon={Tags} onClick={() => setClassifyFor(i)}>Phân loại</Button>}
        {i.canConfirm && <Button size="sm" icon={Check} onClick={() => setPrompt({ title: 'Xác nhận khoản thu nhập', label: 'Ghi chú', required: false, confirmLabel: 'Xác nhận', run: (n) => incomeApi.confirmItem(i.id, n || undefined).then(items.reload) })}>Xác nhận</Button>}
        {i.canConfirm && <Button size="sm" variant="danger" icon={X} onClick={() => setPrompt({ title: 'Từ chối khoản thu nhập', label: 'Lý do', required: true, variant: 'danger', confirmLabel: 'Từ chối', run: (n) => incomeApi.rejectItem(i.id, n).then(items.reload) })}>Từ chối</Button>}
      </div>
    ) },
  ];

  const saleColumns = [
    { key: 'ownerName', header: 'Chủ hợp đồng', render: (s) => (<><p className="font-semibold text-slate-900">{s.ownerName}</p><p className="text-xs text-slate-500">{s.ownerCode}</p></>) },
    { key: 'period', header: 'Kỳ', render: (s) => `${String(s.periodMonth).padStart(2, '0')}/${s.periodYear}` },
    { key: 'contractCode', header: 'Hợp đồng / phiếu thu', render: (s) => (<><p>{s.contractCode}</p><p className="text-xs text-slate-500">{s.receiptCode}</p></>) },
    { key: 'paymentAmount', header: 'Đã thu', render: (s) => formatCurrency(s.paymentAmount) },
    { key: 'eligibleRevenue', header: 'Doanh thu đủ điều kiện', render: (s) => <b className="text-slate-900">{formatCurrency(s.eligibleRevenue)}</b> },
    { key: 'dates', header: 'Ngày thu / nghiệm thu', render: (s) => `${s.paymentDate ? formatDate(s.paymentDate) : '-'} / ${s.acceptanceDate ? formatDate(s.acceptanceDate) : '-'}` },
    { key: 'status', header: 'Trạng thái', render: (s) => (<><StatusBadge status={s.status === 'OWNER_CONFIRMED' ? 'PENDING' : s.status} label={SALE_STATUS[s.status]} />{s.financeConfirmedAt && <p className="text-xs text-slate-400 mt-1">{formatDateTime(s.financeConfirmedAt)}</p>}</>) },
    { key: 'actions', header: '', align: 'right', render: (s) => (
      <div className="flex justify-end gap-1.5">
        {s.canConfirmOwner && <Button size="sm" icon={Check} onClick={() => setPrompt({ title: 'Xác nhận doanh thu (Sales Manager)', label: 'Ghi chú', required: false, confirmLabel: 'Xác nhận', run: (n) => incomeApi.confirmOwner(s.id, n || undefined).then(sales.reload) })}>Xác nhận chủ HĐ</Button>}
        {s.canConfirmPayment && <Button size="sm" icon={Check} onClick={() => setPrompt({ title: 'Xác nhận thu tiền (Kế toán)', label: 'Ghi chú', required: false, confirmLabel: 'Xác nhận', run: (n) => incomeApi.confirmPayment(s.id, n || undefined).then(sales.reload) })}>Xác nhận thu tiền</Button>}
        {(s.canConfirmOwner || s.canConfirmPayment) && <Button size="sm" variant="danger" icon={X} onClick={() => setPrompt({ title: 'Từ chối doanh thu', label: 'Lý do', required: true, variant: 'danger', confirmLabel: 'Từ chối', run: (n) => incomeApi.rejectSale(s.id, n).then(sales.reload) })}>Từ chối</Button>}
      </div>
    ) },
  ];

  const itemFields = (v) => [
    { name: 'employeeId', label: 'Nhân viên', type: 'select', options: peopleOptions, required: true },
    { name: 'periodYear', label: 'Năm', type: 'number', min: 2020, required: true, half: true },
    { name: 'periodMonth', label: 'Tháng', type: 'number', min: 1, max: 12, required: true, half: true },
    { name: 'itemType', label: 'Loại khoản', type: 'select', options: ITEM_TYPES, required: true, half: true },
    { name: 'amount', label: 'Số tiền (VND)', type: 'number', min: 1, required: true, half: true },
    { name: 'name', label: 'Tên khoản', required: true, half: true },
    { name: 'sourceRef', label: 'Mã nguồn (duy nhất)', required: true, half: true, helperText: 'Dùng để chống nhập trùng.' },
    { name: 'taxClass', label: 'Phân loại thuế', type: 'select', options: TAX_CLASSES, half: true, placeholder: 'Cần rà soát (HR phân loại sau)' },
    { name: 'decisionRef', label: 'Căn cứ (số quyết định)', half: true, helperText: 'Bắt buộc với khấu trừ / bồi thường / thu hồi.' },
    { name: 'evidence', label: 'Chứng từ', helperText: 'Bắt buộc với khấu trừ / bồi thường / thu hồi.' },
    { name: 'note', label: 'Ghi chú' },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Thu nhập khác & doanh thu</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Thưởng, khấu trừ, hoàn ứng và doanh thu Sales dùng để tính lương. Người tạo/người hưởng không được tự xác nhận.
          </p>
        </div>
        <div className="flex gap-2">
          {isHR && tab === 'items' && <Button icon={Plus} onClick={() => setItemOpen(true)}>Thêm khoản</Button>}
          {tab === 'sales' && <Button icon={Plus} onClick={() => setSaleOpen(true)}>Ghi nhận doanh thu</Button>}
        </div>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <Tabs tabs={[{ key: 'items', label: 'Khoản thu nhập / khấu trừ', count: items.data?.length }, { key: 'sales', label: 'Doanh thu Sales', count: sales.data?.length }]} active={tab} onChange={setTab} />
        <div className="ml-auto flex gap-2">
          <InputField id="inc-year" label="Năm" type="number" className="w-28" value={year} onChange={(e) => setYear(e.target.value)} />
          <SelectField id="inc-month" label="Tháng" className="w-32" placeholder="Tất cả" options={Array.from({ length: 12 }, (_, i) => ({ value: i + 1, label: `Tháng ${i + 1}` }))} value={month} onChange={(e) => setMonth(e.target.value)} />
        </div>
      </div>

      <div className={box}>
        {tab === 'items'
          ? <DataTable columns={itemColumns} rows={items.data} loading={items.loading} error={items.error} onRetry={items.reload} emptyTitle="Không có khoản nào" />
          : <DataTable columns={saleColumns} rows={sales.data} loading={sales.loading} error={sales.error} onRetry={sales.reload} emptyTitle="Không có doanh thu nào" />}
      </div>

      <FormModal
        isOpen={itemOpen}
        title="Thêm khoản thu nhập / khấu trừ"
        fields={itemFields()}
        initial={{ periodYear: now.getFullYear(), periodMonth: now.getMonth() + 1 }}
        onClose={() => setItemOpen(false)}
        onSubmit={async (v) => {
          if (NEEDS_BASIS.includes(v.itemType) && (!txt(v.evidence) || !txt(v.decisionRef))) {
            throw new Error('Khấu trừ / bồi thường / thu hồi phải có chứng từ và căn cứ (số quyết định).');
          }
          await incomeApi.createItem({
            employeeId: Number(v.employeeId), periodYear: Number(v.periodYear), periodMonth: Number(v.periodMonth),
            itemType: v.itemType, name: v.name.trim(), amount: Number(v.amount), taxClass: txt(v.taxClass),
            sourceRef: v.sourceRef.trim(), evidence: txt(v.evidence), decisionRef: txt(v.decisionRef), note: txt(v.note),
          });
          items.reload();
        }}
      />

      <FormModal
        isOpen={Boolean(classifyFor)}
        title={`Phân loại thuế – ${classifyFor?.name || ''}`}
        fields={[
          { name: 'taxClass', label: 'Phân loại thuế', type: 'select', options: TAX_CLASSES, required: true },
          { name: 'note', label: 'Ghi chú' },
        ]}
        initial={{ taxClass: classifyFor?.taxClass }}
        onClose={() => setClassifyFor(null)}
        onSubmit={async (v) => { await incomeApi.classify(classifyFor.id, { taxClass: v.taxClass, note: txt(v.note) }); items.reload(); }}
      />

      <FormModal
        isOpen={saleOpen}
        title="Ghi nhận doanh thu Sales"
        description="Sales tự ghi nhận doanh thu của mình; Sales Manager xác nhận chủ hợp đồng, Kế toán xác nhận thu tiền."
        fields={[
          { name: 'periodYear', label: 'Năm', type: 'number', required: true, half: true },
          { name: 'periodMonth', label: 'Tháng', type: 'number', min: 1, max: 12, required: true, half: true },
          { name: 'contractCode', label: 'Mã hợp đồng', required: true, half: true },
          { name: 'receiptCode', label: 'Mã phiếu thu', required: true, half: true },
          { name: 'paymentAmount', label: 'Số tiền đã thu (VND)', type: 'number', min: 0, required: true, half: true },
          { name: 'eligibleRevenue', label: 'Doanh thu đủ điều kiện (VND)', type: 'number', min: 0, required: true, half: true },
          { name: 'paymentDate', label: 'Ngày thu tiền', type: 'date', half: true },
          { name: 'acceptanceDate', label: 'Ngày nghiệm thu', type: 'date', half: true },
          { name: 'refundAmount', label: 'Hoàn tiền (nếu có)', type: 'number', min: 0, half: true },
          { name: 'note', label: 'Ghi chú', half: true },
        ]}
        initial={{ periodYear: now.getFullYear(), periodMonth: now.getMonth() + 1 }}
        onClose={() => setSaleOpen(false)}
        onSubmit={async (v) => {
          await incomeApi.createSale({
            periodYear: Number(v.periodYear), periodMonth: Number(v.periodMonth), contractCode: v.contractCode.trim(),
            receiptCode: v.receiptCode.trim(), paymentAmount: Number(v.paymentAmount), eligibleRevenue: Number(v.eligibleRevenue),
            paymentDate: txt(v.paymentDate), acceptanceDate: txt(v.acceptanceDate),
            refundAmount: v.refundAmount === '' ? undefined : Number(v.refundAmount), note: txt(v.note),
          });
          sales.reload();
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

export default IncomePage;
