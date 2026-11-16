import React, { useState } from 'react';
import { Download, Eye, Receipt } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import payslipApi from '../../api/payslipApi';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDateTime } from '../../utils/formatDate';

export function PayslipPage() {
  const { data, loading, error, reload } = useAsync(() => payslipApi.mine(), []);
  const [busy, setBusy] = useState(null);
  const [actionError, setActionError] = useState('');

  const run = async (key, fn) => {
    setBusy(key);
    setActionError('');
    try {
      await fn();
    } catch (err) {
      setActionError(err.message);
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Phiếu lương cá nhân</h1>
        <p className="text-sm text-slate-500 mt-0.5">Phiếu lương được phát hành sau khi kỳ lương được khóa.</p>
      </div>

      {actionError && <ErrorMessage message={actionError} />}

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        {loading ? (
          <Loading />
        ) : error ? (
          <div className="p-4"><ErrorMessage message={error.message} onRetry={reload} /></div>
        ) : !data?.length ? (
          <EmptyState icon={Receipt} title="Chưa có phiếu lương" description="Phiếu lương sẽ xuất hiện khi kỳ lương của bạn được khóa." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3.5">Kỳ lương</th>
                  <th className="px-4 py-3.5 text-right">Tổng thu nhập (Gross)</th>
                  <th className="px-4 py-3.5 text-right">Thực nhận (Net)</th>
                  <th className="px-4 py-3.5 text-right">Thực chi</th>
                  <th className="px-4 py-3.5">Phát hành</th>
                  <th className="px-4 py-3.5 text-right">Tệp PDF</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5 font-bold text-slate-900">Tháng {String(s.month).padStart(2, '0')}/{s.year}</td>
                    <td className="px-4 py-3.5 text-right font-mono">{formatCurrency(s.gross)}</td>
                    <td className="px-4 py-3.5 text-right font-mono">{formatCurrency(s.netEntitlement)}</td>
                    <td className="px-4 py-3.5 text-right font-mono font-semibold text-slate-900">{formatCurrency(s.payable)}</td>
                    <td className="px-4 py-3.5 text-xs">{formatDateTime(s.publishedAt)}</td>
                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      <Button size="sm" variant="outline" icon={Eye} isLoading={busy === `v${s.id}`} onClick={() => run(`v${s.id}`, () => payslipApi.viewPdf(s.id))}>Xem</Button>
                      <Button size="sm" variant="outline" icon={Download} className="ml-2" isLoading={busy === `d${s.id}`}
                        onClick={() => run(`d${s.id}`, () => payslipApi.downloadPdf(s.id, s.fileName))}>Tải về</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default PayslipPage;
