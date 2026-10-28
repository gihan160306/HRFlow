import React, { useEffect, useState } from 'react';
import { Printer, Receipt } from 'lucide-react';
import Button from '../../components/common/Button';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import payslipApi from '../../api/payslipApi';

const INCOME = new Set(['INCOME', 'ALLOWANCE', 'OVERTIME', 'NIGHT_EXTRA']);
const DEDUCTION = new Set(['INSURANCE', 'TAX', 'DEDUCTION', 'ADVANCE']);

export function PayslipPage() {
  const [payslips, setPayslips] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    payslipApi.getMyPayslips()
      .then((rows) => {
        const data = Array.isArray(rows) ? rows : [];
        setPayslips(data);
        setSelectedId(data[0]?.payslipId ?? null);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const payslip = payslips.find((p) => p.payslipId === selectedId);
  const incomeLines = payslip?.lines.filter((line) => INCOME.has(line.category)) || [];
  const deductionLines = payslip?.lines.filter((line) => DEDUCTION.has(line.category)) || [];
  const adjustmentLines = payslip?.lines.filter((line) => !INCOME.has(line.category) && !DEDUCTION.has(line.category)) || [];

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex flex-wrap items-center justify-between gap-4 no-print">
        <div><h1 className="text-2xl font-extrabold text-slate-900">Phiếu lương cá nhân</h1>
          <p className="text-sm text-slate-500 mt-1">Chỉ hiển thị phiếu đã phát hành của kỳ đã khóa</p></div>
        {payslip && <div className="flex items-center gap-3">
          <select value={selectedId ?? ''} onChange={(e) => setSelectedId(Number(e.target.value))}
            className="rounded-lg border border-slate-300 py-2 px-3 text-sm bg-white">
            {payslips.map((p) => <option key={p.payslipId} value={p.payslipId}>{p.periodCode}</option>)}
          </select>
          <Button onClick={() => window.print()} icon={Printer}>In phiếu</Button>
        </div>}
      </div>
      {loading && <p className="text-slate-500">Đang tải phiếu lương...</p>}
      {error && <p role="alert" className="text-red-600">{error}</p>}
      {!loading && !error && !payslip && <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center text-slate-500">
        <Receipt className="mx-auto mb-3" />Chưa có phiếu lương được phát hành.
      </div>}
      {payslip && <article id="printable-payslip" className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-10 text-slate-800">
        <header className="border-b-2 border-slate-900 pb-5 flex justify-between gap-4">
          <div><h2 className="text-lg font-black text-blue-600">HRFLOW</h2>
            <p className="text-sm">Phiếu lương kỳ {payslip.periodCode}</p></div>
          <div className="text-right"><h3 className="text-lg font-extrabold uppercase">Phiếu báo lương</h3>
            <p className="text-xs text-slate-500">{formatDate(payslip.startDate)} – {formatDate(payslip.endDate)}</p></div>
        </header>
        <div className="grid grid-cols-2 gap-3 my-6 rounded-xl bg-slate-50 p-4 text-sm">
          <div><span className="text-slate-500">Nhân viên</span><div className="font-bold">{payslip.fullName}</div></div>
          <div><span className="text-slate-500">Mã nhân viên</span><div className="font-bold">{payslip.employeeCode}</div></div>
        </div>
        <section className="mb-6">
          <h4 className="font-bold text-blue-700 mb-2">Thu nhập</h4>
          <div className="divide-y divide-slate-100 border rounded-xl">
            {incomeLines.map((line) => <div key={line.lineId} className="flex justify-between gap-4 p-3 text-sm">
              <span>{line.label}</span><b>{formatCurrency(line.amount)}</b></div>)}
            <div className="flex justify-between gap-4 p-3 bg-blue-50 font-bold"><span>Tổng Gross</span><span>{formatCurrency(payslip.grossSalary)}</span></div>
          </div>
        </section>
        <section className="mb-6">
          <h4 className="font-bold text-red-700 mb-2">Bảo hiểm, thuế và khấu trừ</h4>
          <div className="divide-y divide-slate-100 border rounded-xl">
            {deductionLines.map((line) => <div key={line.lineId} className="flex justify-between gap-4 p-3 text-sm">
              <span>{line.label}</span><b className="text-red-600">−{formatCurrency(line.amount)}</b></div>)}
          </div>
        </section>
        {adjustmentLines.length > 0 && <section className="mb-6">
          <h4 className="font-bold text-slate-700 mb-2">Điều chỉnh và hoàn chi</h4>
          <div className="divide-y divide-slate-100 border rounded-xl">
            {adjustmentLines.map((line) => <div key={line.lineId} className="flex justify-between gap-4 p-3 text-sm">
              <span>{line.label}</span><b>{formatCurrency(line.amount)}</b></div>)}
          </div>
        </section>}
        <div className="rounded-xl bg-blue-600 p-5 text-white flex justify-between items-center gap-4">
          <span className="font-semibold">Thực chi</span><strong className="text-2xl">{formatCurrency(payslip.netSalary)}</strong>
        </div>
        <p className="text-xs text-slate-500 mt-4">Số thực chi đã trừ tạm ứng và cộng hoàn chi nếu có. Phiếu này không xác nhận giao dịch chuyển khoản.</p>
      </article>}
    </div>
  );
}

export default PayslipPage;
