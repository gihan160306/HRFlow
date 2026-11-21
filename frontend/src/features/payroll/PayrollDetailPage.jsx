import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Download, ShieldAlert } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import payrollApi from '../../api/payrollApi';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import InputField from '../../components/common/InputField';
import StatusBadge from '../../components/common/StatusBadge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import PromptDialog from '../../components/common/PromptDialog';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate, formatDateTime, todayIso } from '../../utils/formatDate';

// Nhãn nút cho allowedActions do backend trả về (backend là nguồn sự thật về hành động được phép).
const ACTION_LABELS = {
  PRECHECK: 'Kiểm tra đầu vào',
  CLOSE_TIMESHEET: 'Chốt công',
  REOPEN_TIMESHEET: 'Mở lại chốt công',
  CALCULATE: 'Tính lương',
  REOPEN: 'Mở lại kỳ (về nháp)',
  RECONCILE: 'Đối soát tài chính',
  APPROVE: 'Phê duyệt bảng lương',
  LOCK: 'Khóa kỳ lương',
  MARK_PAID: 'Xác nhận đã chi trả',
};
// Các hành động có phản hồi cần xác nhận/nhập thêm.
const NEEDS_REASON = ['REOPEN_TIMESHEET', 'REOPEN'];

const LEVEL_STYLE = { BLOCKING: 'text-red-700 bg-red-50 border-red-200', WARNING: 'text-amber-700 bg-amber-50 border-amber-200' };

const money = (v) => <span className="font-mono">{formatCurrency(v)}</span>;

export function PayrollDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const { hasGrant } = useAuth();
  const period = useAsync(() => payrollApi.period(id), [id]);
  const p = period.data;
  const actions = p?.allowedActions || [];

  const lines = useAsync(() => payrollApi.lines(id), [id, p?.calcVersion, p?.status], { enabled: Boolean(p) && p.lineCount > 0 });
  const audit = useAsync(() => payrollApi.audit(id), [id, p?.status, p?.calcVersion], { enabled: Boolean(p) });

  const [precheck, setPrecheck] = useState(null);
  const [actionError, setActionError] = useState('');
  const [busy, setBusy] = useState('');
  const [prompt, setPrompt] = useState(null); // { action, label, required, submit }
  const [confirm, setConfirm] = useState(null);
  const [paidOpen, setPaidOpen] = useState(false);
  const [paid, setPaid] = useState({ paymentRef: '', paidDate: todayIso() });
  const [lineDetail, setLineDetail] = useState(null);
  const EMPTY_CONFLICT = { decisionRef: '', authorityConfirmedBy: '', authorityConfirmedAt: todayIso(), note: '' };
  const [conflictOpen, setConflictOpen] = useState(false);
  const [conflict, setConflict] = useState(EMPTY_CONFLICT);
  const conflictValid = conflict.decisionRef.trim() && conflict.authorityConfirmedBy.trim() && conflict.note.trim()
    && conflict.authorityConfirmedAt && conflict.authorityConfirmedAt <= todayIso();

  const refresh = () => {
    period.reload();
    audit.reload();
  };

  const run = async (name, fn) => {
    setBusy(name);
    setActionError('');
    try {
      await fn();
      refresh();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setBusy('');
    }
  };

  const onAction = (a) => {
    switch (a) {
      case 'PRECHECK':
        return run(a, async () => setPrecheck(await payrollApi.precheck(id)));
      case 'CLOSE_TIMESHEET':
        return run(a, () => payrollApi.closeTimesheet(id));
      case 'CALCULATE':
        return run(a, () => payrollApi.calculate(id));
      case 'REOPEN_TIMESHEET':
        return setPrompt({ title: 'Mở lại chốt công', submit: (r) => payrollApi.reopenTimesheet(id, r) });
      case 'REOPEN':
        return setPrompt({ title: 'Mở lại kỳ lương về nháp', submit: (r) => payrollApi.reopen(id, r) });
      case 'RECONCILE':
        return setPrompt({ title: 'Đối soát tài chính', label: 'Ghi chú đối soát', required: false, submit: (n) => payrollApi.reconcile(id, n || undefined) });
      case 'APPROVE':
        return setConfirm({ title: 'Phê duyệt bảng lương', message: 'Sau khi phê duyệt, bảng lương không thể tính lại. Người lập bảng lương không được tự phê duyệt.', label: 'Phê duyệt', fn: () => payrollApi.approve(id) });
      case 'LOCK':
        return setConfirm({ title: 'Khóa kỳ lương', message: 'Kỳ lương bị khóa chỉ đọc; sai sót sau đó phải xử lý bằng điều chỉnh (adjustment). Phiếu lương sẽ được sinh tự động.', label: 'Khóa kỳ', fn: () => payrollApi.lock(id) });
      case 'MARK_PAID':
        return setPaidOpen(true);
      case 'CONFLICT_RESOLUTION':
        setConflict(EMPTY_CONFLICT);
        return setConflictOpen(true);
      default:
        return undefined;
    }
  };

  if (period.loading) return <Loading message="Đang tải kỳ lương..." />;
  if (period.error) {
    return (
      <div className="space-y-4">
        <Button variant="outline" icon={ArrowLeft} onClick={() => navigate('/payroll')}>Danh sách kỳ lương</Button>
        <ErrorMessage message={period.error.message} onRetry={period.reload} />
      </div>
    );
  }

  const showable = actions.filter((a) => ACTION_LABELS[a]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Button variant="outline" size="sm" icon={ArrowLeft} onClick={() => navigate('/payroll')}>Kỳ lương</Button>
          <div>
            <h1 className="text-2xl font-extrabold text-slate-900">Kỳ lương {String(p.month).padStart(2, '0')}/{p.year}</h1>
            <p className="text-sm text-slate-500">{formatDate(p.startDate)} – {formatDate(p.endDate)} • chi trả {formatDate(p.payDate)}</p>
          </div>
          <StatusBadge status={p.status} />
          {p.status === 'LOCKED' && <StatusBadge status={p.paymentStatus} />}
        </div>
        {hasGrant('VIEW') && p.lineCount > 0 && (
          <Button variant="outline" icon={Download} onClick={() => run('export', () => payrollApi.exportCsv(id))}>Xuất CSV</Button>
        )}
      </div>

      {actionError && <ErrorMessage message={actionError} />}

      <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5 space-y-4">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 text-sm">
          <div><p className="text-slate-500">Chốt công</p><p className="font-semibold">{p.timesheetClosed ? `Đã chốt${p.closedAt ? ` (${formatDateTime(p.closedAt)})` : ''}` : 'Chưa chốt'}</p></div>
          <div><p className="text-slate-500">Số dòng lương</p><p className="font-semibold">{p.lineCount}{p.needsReviewCount > 0 && <span className="text-amber-600"> ({p.needsReviewCount} cần rà soát)</span>}</p></div>
          <div><p className="text-slate-500">Tổng Gross</p><p className="font-semibold">{formatCurrency(p.totalGross)}</p></div>
          <div><p className="text-slate-500">Tổng thực chi</p><p className="font-semibold">{formatCurrency(p.totalPayable)}</p></div>
          <div><p className="text-slate-500">Tính lương</p><p className="font-semibold">{p.calculatedByName || '-'}{p.calculatedAt && ` • ${formatDateTime(p.calculatedAt)}`}</p></div>
          <div><p className="text-slate-500">Đối soát</p><p className="font-semibold">{p.reconciledByName || '-'}{p.reconciledAt && ` • ${formatDateTime(p.reconciledAt)}`}</p></div>
          <div><p className="text-slate-500">Phê duyệt</p><p className="font-semibold">{p.approvedByName || '-'}{p.approvedAt && ` • ${formatDateTime(p.approvedAt)}`}</p></div>
          <div><p className="text-slate-500">Khóa kỳ</p><p className="font-semibold">{p.lockedByName || '-'}{p.lockedAt && ` • ${formatDateTime(p.lockedAt)}`}</p></div>
        </div>

        {showable.length > 0 ? (
          <div className="flex flex-wrap gap-3 pt-2 border-t border-slate-100">
            {showable.map((a) => (
              <Button key={a} variant={['APPROVE', 'LOCK', 'CALCULATE'].includes(a) ? 'primary' : 'outline'}
                isLoading={busy === a} disabled={Boolean(busy)} onClick={() => onAction(a)}>
                {ACTION_LABELS[a]}
              </Button>
            ))}
          </div>
        ) : (
          <p className="text-xs text-slate-500 pt-2 border-t border-slate-100">
            Bạn không có thao tác nào khả dụng ở trạng thái này (tùy trạng thái kỳ và grant payroll của tài khoản).
          </p>
        )}
        {(p.conflictDecisionRef || actions.includes('CONFLICT_RESOLUTION')) && (
          <div className={`text-sm rounded-xl border px-4 py-3 flex flex-wrap items-center justify-between gap-3 ${p.conflictDecisionRef ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-amber-200 bg-amber-50 text-amber-800'}`}>
            <div className="flex items-start gap-2">
              <ShieldAlert className="w-5 h-5 shrink-0 mt-0.5" />
              {p.conflictDecisionRef ? (
                <p>
                  <b>Đã có quyết định xử lý xung đột lợi ích:</b> {p.conflictDecisionRef}
                  {p.conflictResolvedByName && ` • ghi nhận bởi ${p.conflictResolvedByName}`}
                  {p.conflictResolvedAt && ` • ${formatDateTime(p.conflictResolvedAt)}`}
                </p>
              ) : (
                <p>
                  <b>Xung đột lợi ích:</b> kỳ lương gồm cả lương của Giám đốc (người phê duyệt). Hệ thống chặn phê duyệt cho tới khi
                  ghi nhận quyết định xử lý do cấp có thẩm quyền HNQ xác nhận. Giám đốc không được tự ghi nhận.
                </p>
              )}
            </div>
            {actions.includes('CONFLICT_RESOLUTION') && (
              <Button size="sm" variant="outline" disabled={Boolean(busy)} onClick={() => onAction('CONFLICT_RESOLUTION')}>
                {p.conflictDecisionRef ? 'Cập nhật quyết định' : 'Ghi nhận quyết định xử lý'}
              </Button>
            )}
          </div>
        )}
      </section>

      {precheck && (
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5">
          <h3 className="font-bold text-slate-800 flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-amber-600" />
            Kiểm tra đầu vào: {precheck.blocking} lỗi chặn, {precheck.warnings} cảnh báo ({precheck.employees} nhân sự)
          </h3>
          {precheck.issues.length === 0 ? (
            <p className="text-sm text-emerald-700 mt-2">Không phát hiện vấn đề.</p>
          ) : (
            <ul className="mt-3 space-y-2 max-h-80 overflow-y-auto">
              {precheck.issues.map((i, idx) => (
                <li key={idx} className={`text-sm border rounded-lg px-3 py-2 ${LEVEL_STYLE[i.level] || ''}`}>
                  <b>{i.level === 'BLOCKING' ? 'Chặn' : 'Cảnh báo'}</b> [{i.category}]
                  {i.employeeName && ` ${i.employeeCode} – ${i.employeeName}`}: {i.message}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {p.lineCount > 0 && (
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
          <div className="p-5 border-b border-slate-100"><h3 className="font-bold text-slate-800">Bảng lương chi tiết</h3></div>
          {lines.loading ? <Loading /> : lines.error ? (
            <div className="p-4"><ErrorMessage message={lines.error.message} onRetry={lines.reload} /></div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-600">
                <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500">
                  <tr>
                    <th className="px-4 py-3">Nhân viên</th><th className="px-4 py-3 text-right">Gross</th>
                    <th className="px-4 py-3 text-right">Bảo hiểm</th><th className="px-4 py-3 text-right">TNCN</th>
                    <th className="px-4 py-3 text-right">Thực nhận</th><th className="px-4 py-3 text-right">Thực chi</th><th className="px-4 py-3">TT</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {(lines.data || []).map((l) => (
                    <tr key={l.id} className="hover:bg-slate-50 cursor-pointer" onClick={() => payrollApi.line(l.id).then(setLineDetail).catch((e) => setActionError(e.message))}>
                      <td className="px-4 py-3 font-semibold text-slate-900">{l.employeeName}<span className="block text-xs font-normal text-slate-400">{l.employeeCode} • {l.departmentName}</span></td>
                      <td className="px-4 py-3 text-right">{money(l.gross)}</td>
                      <td className="px-4 py-3 text-right">{money(l.insuranceTotal)}</td>
                      <td className="px-4 py-3 text-right">{money(l.pit)}</td>
                      <td className="px-4 py-3 text-right">{money(l.netEntitlement)}</td>
                      <td className="px-4 py-3 text-right font-semibold text-slate-900">{money(l.payable)}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={l.status} />
                        {l.issues?.length > 0 && <span className="block text-xs text-amber-700 mt-1" title={l.issues.join('\n')}>{l.issues.length} vấn đề</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {audit.data?.length > 0 && (
        <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5">
          <h3 className="font-bold text-slate-800 mb-3">Nhật ký kỳ lương (chỉ thêm, không sửa/xóa)</h3>
          <ul className="space-y-2 text-sm">
            {audit.data.map((a) => (
              <li key={a.id} className="flex flex-wrap gap-x-3 text-slate-600">
                <span className="text-xs text-slate-400 w-36">{formatDateTime(a.createdAt)}</span>
                <b className="text-slate-800">{a.action}</b>
                <span>{a.actorName}</span>
                {a.fromStatus && <span className="text-xs">{a.fromStatus} → {a.toStatus}</span>}
                {a.detail && <span className="text-slate-500">{a.detail}</span>}
              </li>
            ))}
          </ul>
        </section>
      )}

      <PromptDialog
        isOpen={Boolean(prompt)}
        title={prompt?.title}
        label={prompt?.label || 'Lý do'}
        required={prompt?.required !== false}
        onClose={() => setPrompt(null)}
        onSubmit={async (text) => { await prompt.submit(text); refresh(); }}
      />

      <ConfirmDialog
        isOpen={Boolean(confirm)}
        title={confirm?.title}
        message={confirm?.message}
        confirmLabel={confirm?.label}
        confirmVariant="primary"
        isLoading={Boolean(busy)}
        onCancel={() => setConfirm(null)}
        onConfirm={async () => {
          const c = confirm;
          setConfirm(null);
          await run(c.label, c.fn);
        }}
      />

      <Modal isOpen={paidOpen} title="Xác nhận đã chi trả lương" onClose={() => setPaidOpen(false)}
        footer={
          <>
            <Button variant="outline" onClick={() => setPaidOpen(false)}>Hủy</Button>
            <Button disabled={!paid.paymentRef.trim()} onClick={async () => {
              setPaidOpen(false);
              await run('MARK_PAID', () => payrollApi.markPaid(id, { paymentRef: paid.paymentRef.trim(), paidDate: paid.paidDate || undefined }));
            }}>Xác nhận</Button>
          </>
        }>
        <div className="space-y-4">
          <InputField label="Mã tham chiếu chi trả" value={paid.paymentRef} onChange={(e) => setPaid({ ...paid, paymentRef: e.target.value })} required />
          <InputField label="Ngày chi trả" type="date" value={paid.paidDate} onChange={(e) => setPaid({ ...paid, paidDate: e.target.value })} />
        </div>
      </Modal>

      <Modal isOpen={conflictOpen} title="Ghi nhận quyết định xử lý xung đột lợi ích" onClose={() => setConflictOpen(false)}
        footer={
          <>
            <Button variant="outline" onClick={() => setConflictOpen(false)}>Hủy</Button>
            <Button disabled={!conflictValid} onClick={async () => {
              setConflictOpen(false);
              await run('CONFLICT_RESOLUTION', () => payrollApi.resolveConflict(id, {
                decisionRef: conflict.decisionRef.trim(),
                authorityConfirmedBy: conflict.authorityConfirmedBy.trim(),
                authorityConfirmedAt: conflict.authorityConfirmedAt,
                note: conflict.note.trim(),
              }));
            }}>Ghi nhận</Button>
          </>
        }>
        <div className="space-y-4">
          <p className="text-sm text-slate-500">
            Nhập căn cứ do cấp có thẩm quyền HNQ (ví dụ Hội đồng thành viên) đã xác nhận. Hệ thống chỉ lưu và kiểm tra đủ thông tin,
            không tự quyết định biện pháp xử lý. Thao tác được ghi nhật ký.
          </p>
          <InputField label="Số / căn cứ quyết định" placeholder="VD: NQ-HDTV-2026-11" value={conflict.decisionRef}
            onChange={(e) => setConflict({ ...conflict, decisionRef: e.target.value })} required />
          <InputField label="Người / cơ quan xác nhận" placeholder="VD: Chủ tịch Hội đồng thành viên" value={conflict.authorityConfirmedBy}
            onChange={(e) => setConflict({ ...conflict, authorityConfirmedBy: e.target.value })} required />
          <InputField label="Ngày xác nhận" type="date" max={todayIso()} value={conflict.authorityConfirmedAt}
            onChange={(e) => setConflict({ ...conflict, authorityConfirmedAt: e.target.value })} required
            error={conflict.authorityConfirmedAt > todayIso() ? 'Ngày xác nhận không được ở tương lai' : undefined} />
          <InputField label="Nội dung xử lý" placeholder="VD: Dòng lương Giám đốc do HĐTV phê duyệt riêng" value={conflict.note}
            onChange={(e) => setConflict({ ...conflict, note: e.target.value })} required />
        </div>
      </Modal>

      <Modal isOpen={Boolean(lineDetail)} title={lineDetail ? `${lineDetail.employeeName} (${lineDetail.employeeCode})` : ''} onClose={() => setLineDetail(null)} maxWidth="max-w-2xl">
        {lineDetail && (
          <div className="space-y-3 text-sm">
            <div className="grid grid-cols-2 gap-x-6 gap-y-1">
              {[['Lương cơ bản', lineDetail.baseSalary], ['Lương theo công', lineDetail.salaryAttendance], ['Lương OT', lineDetail.otPay],
                ['Phụ cấp', lineDetail.allowances], ['Hoa hồng', lineDetail.commission], ['Thưởng KPI', lineDetail.kpiBonus],
                ['Gross', lineDetail.gross], ['BHXH', lineDetail.bhxh], ['BHYT', lineDetail.bhyt], ['BHTN', lineDetail.bhtn],
                ['Giảm trừ gia cảnh', lineDetail.familyRelief], ['Thu nhập tính thuế', lineDetail.assessableIncome], ['Thuế TNCN', lineDetail.pit],
                ['Thực nhận', lineDetail.netEntitlement], ['Thực chi', lineDetail.payable]].map(([k, v]) => (
                <div key={k} className="flex justify-between border-b border-slate-100 py-1"><span>{k}</span>{money(v)}</div>
              ))}
            </div>
            {lineDetail.issues?.length > 0 && (
              <ul className="list-disc pl-5 text-amber-700">{lineDetail.issues.map((i) => <li key={i}>{i}</li>)}</ul>
            )}
            {lineDetail.items?.length > 0 && (
              <div>
                <p className="font-semibold text-slate-800 mt-2 mb-1">Chi tiết khoản mục</p>
                <ul className="divide-y divide-slate-100 max-h-56 overflow-y-auto">
                  {lineDetail.items.map((it) => (
                    <li key={it.seq} className="flex justify-between py-1"><span>{it.name} <span className="text-xs text-slate-400">[{it.category}]</span></span>{money(it.amount)}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

export default PayrollDetailPage;
