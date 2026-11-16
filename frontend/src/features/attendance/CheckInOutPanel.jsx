import React, { useState, useEffect } from 'react';
import { Clock, LogIn, LogOut } from 'lucide-react';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import ErrorMessage from '../../components/common/ErrorMessage';
import StatusBadge from '../../components/common/StatusBadge';
import attendanceApi from '../../api/attendanceApi';
import { formatTime } from '../../utils/formatDate';

/**
 * Chấm công theo API thật: today -> check-in -> check-out.
 * Điểm chấm công yêu cầu QR sẽ trả lỗi QR_INVALID; khi đó nhân viên nhập mã ngắn hiển thị tại điểm chấm công.
 */
export function CheckInOutPanel({ record, onChanged }) {
  const [now, setNow] = useState(new Date());
  const [shortCode, setShortCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const run = async (fn) => {
    setBusy(true);
    setError('');
    try {
      await fn({ shortCode: shortCode.trim() || undefined });
      setShortCode('');
      onChanged?.();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const checkedIn = Boolean(record?.checkInAt);
  const checkedOut = Boolean(record?.checkOutAt);

  return (
    <div className="bg-white rounded-3xl border border-slate-200/80 shadow-sm p-6 sm:p-8 space-y-4">
      <div className="flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="flex items-center space-x-5">
          <div className="p-4 bg-blue-50 text-blue-600 rounded-2xl"><Clock className="w-8 h-8" /></div>
          <div>
            <p className="text-xs uppercase font-bold text-slate-400 tracking-wider">Giờ hiện tại</p>
            <div className="text-3xl sm:text-4xl font-extrabold text-slate-900 font-mono tracking-tight">
              {now.toLocaleTimeString('vi-VN')}
            </div>
            <p className="text-xs text-slate-500 mt-1">
              {now.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}
            </p>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-4 w-full md:w-auto">
          <div className="bg-slate-50 rounded-2xl p-3 border border-slate-200/60 flex items-center space-x-4 text-xs w-full sm:w-auto justify-around">
            <div>
              <span className="text-slate-400 block">Vào ca</span>
              <span className="font-bold text-slate-800 text-sm">{formatTime(record?.checkInAt)}</span>
            </div>
            <div className="h-6 w-px bg-slate-200" />
            <div>
              <span className="text-slate-400 block">Ra ca</span>
              <span className="font-bold text-slate-800 text-sm">{formatTime(record?.checkOutAt)}</span>
            </div>
            {record?.status && <StatusBadge status={record.status} />}
          </div>

          <div className="flex items-center space-x-3 w-full sm:w-auto">
            <Button onClick={() => run(attendanceApi.checkIn)} isLoading={busy} disabled={checkedIn} icon={LogIn} className="flex-1 sm:flex-initial">
              {checkedIn ? 'Đã check-in' : 'Check-in'}
            </Button>
            <Button variant="outline" onClick={() => run(attendanceApi.checkOut)} isLoading={busy} disabled={!checkedIn || checkedOut} icon={LogOut} className="flex-1 sm:flex-initial">
              {checkedOut ? 'Đã check-out' : 'Check-out'}
            </Button>
          </div>
        </div>
      </div>

      <div className="max-w-xs">
        <InputField label="Mã chấm công ngắn (nếu điểm chấm công yêu cầu)" value={shortCode} onChange={(e) => setShortCode(e.target.value)} placeholder="Để trống nếu không dùng QR" />
      </div>
      {error && <ErrorMessage message={error} />}
    </div>
  );
}

export default CheckInOutPanel;
