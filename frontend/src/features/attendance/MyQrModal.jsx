import React, { useCallback, useEffect, useRef, useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { CheckCircle2, Sun } from 'lucide-react';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import ErrorMessage from '../../components/common/ErrorMessage';
import attendanceApi from '../../api/attendanceApi';
import { formatTime } from '../../utils/formatDate';

const REFRESH_BEFORE_MS = 3000; // lấy mã mới khi còn ~3 giây để máy quầy không quét trúng mã vừa hết hạn
const POLL_MS = 2000;

/**
 * QR cá nhân động để đưa vào máy chấm công tại quầy (SRS FR-07, DEC-10).
 * Mã do server cấp, hạn 15 giây, dùng một lần; tự làm mới. Khi máy quầy ghi công xong,
 * hỏi lại /attendance/today và báo kết quả.
 */
export function MyQrModal({ isOpen, onClose, record, onDone }) {
  const [qr, setQr] = useState(null);
  const [error, setError] = useState('');
  const [now, setNow] = useState(Date.now());
  const [done, setDone] = useState(null); // { action, at }
  const baseline = useRef(null);
  const fetching = useRef(false);

  const loadQr = useCallback(async () => {
    if (fetching.current) return;
    fetching.current = true;
    try {
      const res = await attendanceApi.myQr();
      setQr({ ...res, expiresMs: new Date(res.expiresAt).getTime() });
      setError('');
    } catch (err) {
      setError(err.message);
    } finally {
      fetching.current = false;
    }
  }, []);

  // Mở hộp thoại: ghi nhận trạng thái hiện tại, lấy mã đầu tiên.
  useEffect(() => {
    if (!isOpen) return;
    baseline.current = { in: record?.checkInAt || null, out: record?.checkOutAt || null };
    setDone(null);
    setQr(null);
    loadQr();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  // Đồng hồ đếm ngược + tự lấy mã mới.
  useEffect(() => {
    if (!isOpen || done) return undefined;
    const t = setInterval(() => {
      const n = Date.now();
      setNow(n);
      if (qr && qr.expiresMs - n <= REFRESH_BEFORE_MS) loadQr();
    }, 250);
    return () => clearInterval(t);
  }, [isOpen, done, qr, loadQr]);

  // Hỏi lại kết quả chấm công do máy quầy ghi.
  useEffect(() => {
    if (!isOpen || done) return undefined;
    const t = setInterval(async () => {
      try {
        const today = await attendanceApi.today();
        const b = baseline.current || {};
        if (today?.checkOutAt && today.checkOutAt !== b.out) {
          setDone({ action: 'Check-out', at: today.checkOutAt, status: today.status });
        } else if (today?.checkInAt && today.checkInAt !== b.in) {
          setDone({ action: 'Check-in', at: today.checkInAt, status: today.status });
        }
      } catch {
        // bỏ qua lỗi mạng tạm thời, lần sau hỏi lại
      }
    }, POLL_MS);
    return () => clearInterval(t);
  }, [isOpen, done]);

  useEffect(() => {
    if (!done) return undefined;
    onDone?.();
    const t = setTimeout(onClose, 3500);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [done]);

  const remaining = qr ? Math.max(0, Math.ceil((qr.expiresMs - now) / 1000)) : 0;
  const pct = qr ? Math.min(100, (remaining / (qr.ttlSeconds || 15)) * 100) : 0;

  return (
    <Modal isOpen={isOpen} title={done ? 'Chấm công thành công' : 'Mã QR chấm công của bạn'} onClose={onClose}>
      {done ? (
        <div className="text-center py-6 space-y-3">
          <CheckCircle2 className="w-16 h-16 text-emerald-500 mx-auto" />
          <p className="text-xl font-extrabold text-slate-900">{done.action} lúc {formatTime(done.at)}</p>
          <p className="text-sm text-slate-500">Dữ liệu đã được ghi nhận kèm ảnh minh chứng chụp tại quầy.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {error && <ErrorMessage message={error} onRetry={loadQr} />}
          <p className="text-sm text-slate-600 text-center">
            Đưa màn hình này vào camera của <b>máy chấm công tại quầy</b>. Quét xong, máy sẽ chụp ảnh minh chứng.
          </p>
          <div className="flex justify-center">
            <div className="p-4 bg-white rounded-2xl border-2 border-slate-200 w-full max-w-[320px] aspect-square flex items-center justify-center">
              {qr ? (
                <QRCodeSVG value={qr.qrPayload} level="M" marginSize={2} style={{ width: '100%', height: '100%' }} />
              ) : (
                <span className="text-sm text-slate-400">Đang tạo mã...</span>
              )}
            </div>
          </div>
          {qr && (
            <div className="space-y-1.5">
              <div className="flex justify-between text-xs text-slate-500">
                <span className="font-semibold text-slate-700">{qr.employeeCode} – {qr.employeeName}</span>
                <span>Mã đổi sau {remaining}s</span>
              </div>
              <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full bg-blue-500 transition-all duration-200" style={{ width: `${pct}%` }} />
              </div>
            </div>
          )}
          <p className="flex items-center justify-center gap-1.5 text-xs text-amber-700">
            <Sun className="w-4 h-4" /> Tăng độ sáng màn hình để máy quét nhanh hơn. Mã chỉ dùng một lần.
          </p>
          <div className="flex justify-end">
            <Button variant="outline" onClick={onClose}>Đóng</Button>
          </div>
        </div>
      )}
    </Modal>
  );
}

export default MyQrModal;
