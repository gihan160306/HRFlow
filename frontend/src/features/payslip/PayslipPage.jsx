import React, { useEffect, useRef, useState } from 'react';
import { Download, Eye, Printer, Receipt, X } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import payslipApi from '../../api/payslipApi';
import Button from '../../components/common/Button';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDateTime } from '../../utils/formatDate';
import { exportElementToPdf } from '../../utils/exportPdf';
import PayslipDocument from './PayslipDocument';

const periodLabel = (s) => `Tháng ${String(s.month).padStart(2, '0')}/${s.year}`;
const pdfName = (s) => s.fileName || `phieu-luong-${s.year}-${String(s.month).padStart(2, '0')}-${s.employeeCode}.pdf`;

export function PayslipPage() {
  const { data, loading, error, reload } = useAsync(() => payslipApi.mine(), []);
  const [viewing, setViewing] = useState(null); // chi tiết phiếu đang mở xem
  const [exporting, setExporting] = useState(null); // chi tiết phiếu đang dựng ẩn để xuất PDF
  const [busy, setBusy] = useState(null);
  const [actionError, setActionError] = useState('');
  const exportRef = useRef(null);

  // Đóng xem phiếu bằng phím Esc
  useEffect(() => {
    if (!viewing) return undefined;
    const onKey = (e) => e.key === 'Escape' && setViewing(null);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [viewing]);

  // Phiếu được dựng ẩn ngoài màn hình rồi mới chụp thành PDF
  useEffect(() => {
    if (!exporting) return;
    let cancelled = false;
    (async () => {
      try {
        await waitForImages(exportRef.current);
        if (!cancelled) await exportElementToPdf(exportRef.current, pdfName(exporting));
      } catch (err) {
        setActionError(`Không tạo được PDF: ${err.message}`);
      } finally {
        if (!cancelled) {
          setExporting(null);
          setBusy(null);
        }
      }
    })();
    return () => { cancelled = true; };
  }, [exporting]);

  const loadDetail = async (key, s) => {
    setBusy(key);
    setActionError('');
    try {
      return await payslipApi.get(s.id);
    } catch (err) {
      setActionError(err.message);
      setBusy(null);
      return null;
    }
  };

  const view = async (s) => {
    const d = await loadDetail(`v${s.id}`, s);
    setBusy(null);
    if (d) setViewing(d);
  };

  const download = async (s) => {
    if (viewing?.id === s.id) {
      setBusy(`d${s.id}`);
      setExporting(viewing);
      return;
    }
    const d = await loadDetail(`d${s.id}`, s);
    if (d) setExporting(d); // busy được xóa khi xuất xong
  };

  return (
    <div className="space-y-6">
      <div className="no-print">
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Phiếu lương cá nhân</h1>
        <p className="text-sm text-slate-500 mt-0.5">Phiếu lương được phát hành sau khi kỳ lương được khóa. Bấm “Xem” để mở phiếu.</p>
      </div>

      {actionError && <div className="no-print"><ErrorMessage message={actionError} /></div>}

      <div className="no-print bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
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
                  <th className="px-4 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.map((s) => (
                  <tr key={s.id} className="hover:bg-slate-50/80">
                    <td className="px-4 py-3.5 font-bold text-slate-900">{periodLabel(s)}</td>
                    <td className="px-4 py-3.5 text-right font-mono">{formatCurrency(s.gross)}</td>
                    <td className="px-4 py-3.5 text-right font-mono">{formatCurrency(s.netEntitlement)}</td>
                    <td className="px-4 py-3.5 text-right font-mono font-semibold text-slate-900">{formatCurrency(s.payable)}</td>
                    <td className="px-4 py-3.5 text-xs">{formatDateTime(s.publishedAt)}</td>
                    <td className="px-4 py-3.5 text-right whitespace-nowrap">
                      <Button size="sm" variant="outline" icon={Eye} isLoading={busy === `v${s.id}`} onClick={() => view(s)}>Xem</Button>
                      <Button size="sm" variant="outline" icon={Download} className="ml-2" isLoading={busy === `d${s.id}`}
                        disabled={busy?.startsWith('d')} onClick={() => download(s)}>Tải về</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Cửa sổ xem phiếu lương */}
      {viewing && (
        <div className="payslip-viewer fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm p-4 sm:p-8 print:static print:bg-transparent print:p-0 print:backdrop-blur-none"
          onClick={(e) => e.target === e.currentTarget && setViewing(null)}>
          <div className="mx-auto max-w-[820px] print:static">
            <div className="no-print mb-3 flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-bold text-white">Phiếu lương {periodLabel(viewing)}</p>
              <div className="flex items-center gap-2">
                <Button size="sm" icon={Printer} onClick={() => window.print()}>In</Button>
                <Button size="sm" variant="secondary" icon={Download} isLoading={busy === `d${viewing.id}`} onClick={() => download(viewing)}>Tải PDF</Button>
                <button type="button" aria-label="Đóng" onClick={() => setViewing(null)}
                  className="rounded-lg p-1.5 text-white/80 hover:bg-white/10 hover:text-white">
                  <X className="h-5 w-5" />
                </button>
              </div>
            </div>
            <PayslipDocument slip={viewing} />
          </div>
        </div>
      )}

      {/* Bản dựng ẩn để xuất PDF khi tải từ danh sách */}
      {exporting && (
        <div aria-hidden="true" className="no-print fixed top-0 -left-[10000px] w-[820px]">
          <PayslipDocument ref={exportRef} slip={exporting} id="payslip-export" />
        </div>
      )}
    </div>
  );
}

function waitForImages(el) {
  const imgs = Array.from(el?.querySelectorAll('img') || []);
  return Promise.all(imgs.map((img) => (img.complete ? null : new Promise((r) => { img.onload = r; img.onerror = r; }))));
}

export default PayslipPage;
