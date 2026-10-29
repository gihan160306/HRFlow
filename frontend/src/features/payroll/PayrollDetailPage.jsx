import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import payrollApi from '../../api/payrollApi';

export function PayrollDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [period, setPeriod] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [resetReason, setResetReason] = useState('');
  const [showReset, setShowReset] = useState(false);
  const load = useCallback(() => payrollApi.getPayrollPeriodById(id).then(setPeriod).catch((e) => setError(e.message)), [id]);
  useEffect(() => { load(); }, [load]);

  const act = async (fn) => {
    setBusy(true); setError('');
    try { setPeriod(await fn(id)); return true; } catch (e) { setError(e.message); return false; }
    finally { setBusy(false); }
  };

  const importInputs = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setBusy(true); setError('');
    try {
      const data = JSON.parse(await file.text());
      if (!Array.isArray(data.employees) || data.employees.length === 0) {
        throw new Error('Tệp cần có danh sách employees đã đối soát.');
      }
      setPeriod(await payrollApi.calculatePayroll(Number(id), data.employees));
    } catch (e) { setError(e.message); }
    finally { setBusy(false); event.target.value = ''; }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap justify-between gap-4">
        <div className="flex items-center gap-4">
          <Button variant="outline" size="sm" icon={ArrowLeft} onClick={() => navigate('/payroll')}>Danh sách kỳ lương</Button>
          <div>
            <h1 className="text-2xl font-extrabold text-slate-900">Kỳ lương {period?.periodCode || id}</h1>
            {period && <p className="text-xs text-slate-500 mt-1">{formatDate(period.startDate)} – {formatDate(period.endDate)}</p>}
          </div>
          {period && <StatusBadge status={period.status} />}
        </div>
        <div className="flex gap-2">
          {(period?.status === 'DRAFT' || period?.status === 'CALCULATED') &&
            <label className="cursor-pointer rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white">
              {busy ? 'Đang xử lý...' : 'Nhập bảng đối soát'}
              <input type="file" accept="application/json,.json" className="hidden" disabled={busy} onChange={importInputs} />
            </label>}
          {period?.status === 'CALCULATED' && <Button disabled={busy} onClick={() => act(payrollApi.confirmPayroll)}>Duyệt</Button>}
          {period?.status === 'CALCULATED' && <Button variant="outline" disabled={busy} onClick={() => setShowReset((v) => !v)}>Sửa nguồn</Button>}
          {period?.status === 'APPROVED' && <Button disabled={busy} onClick={() => act(payrollApi.lockPayroll)}>Khóa kỳ</Button>}
          {period?.status === 'LOCKED' && <Button disabled={busy} onClick={() => act(payrollApi.publishPayslips)}>Phát hành phiếu</Button>}
        </div>
      </div>
      {error && <p role="alert" className="text-red-600">{error}</p>}
      {showReset && period?.status === 'CALCULATED' && <div className="bg-amber-50 rounded-xl p-4 flex flex-wrap gap-3 items-center">
        <input value={resetReason} onChange={(e) => setResetReason(e.target.value)}
          placeholder="Lý do trả về bản nháp" className="border rounded-lg p-2 flex-1 min-w-48" />
        <Button variant="outline" disabled={busy || !resetReason.trim()}
          onClick={async () => { if (await act((periodId) => payrollApi.resetPayroll(periodId, resetReason))) setShowReset(false); }}>
          Trả về bản nháp
        </Button>
      </div>}
      {!period && !error && <p className="text-slate-500">Đang tải bảng lương...</p>}
      {period?.status === 'DRAFT' && <p className="text-slate-500">HR nhập bảng đầu vào đã đối soát cho toàn bộ nhân viên của kỳ này.</p>}
      {period && <div className="bg-white rounded-2xl border border-slate-200 overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500"><tr>
            <th className="px-4 py-3">Nhân viên</th><th className="px-4 py-3">Gross</th>
            <th className="px-4 py-3">Bảo hiểm</th><th className="px-4 py-3">Thuế TNCN</th>
            <th className="px-4 py-3">Khấu trừ khác</th><th className="px-4 py-3 text-right">Thực chi</th>
          </tr></thead>
          <tbody className="divide-y divide-slate-100">
            {period.records.map((r) => <tr key={r.payrollRecordId}>
              <td className="px-4 py-3"><b>{r.fullName}</b><div className="text-xs text-slate-400">{r.employeeCode}</div>
                {!r.policyVersion && <div className="text-xs text-amber-700">Dữ liệu mẫu cũ, chưa đối soát</div>}</td>
              <td className="px-4 py-3">{formatCurrency(r.grossSalary)}</td>
              <td className="px-4 py-3">{formatCurrency(Number(r.bhxhAmount) + Number(r.bhytAmount) + Number(r.bhtnAmount))}</td>
              <td className="px-4 py-3">{formatCurrency(r.pitAmount)}</td>
              <td className="px-4 py-3">{formatCurrency(r.otherDeductions)}</td>
              <td className="px-4 py-3 text-right font-bold text-blue-600">{formatCurrency(r.netSalary)}</td>
            </tr>)}
            {period.records.length === 0 && <tr><td colSpan="6" className="px-4 py-8 text-center text-slate-500">Chưa có phiếu lương.</td></tr>}
          </tbody>
        </table>
      </div>}
    </div>
  );
}

export default PayrollDetailPage;
