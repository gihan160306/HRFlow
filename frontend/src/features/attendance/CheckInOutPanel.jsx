import React, { useState, useEffect } from 'react';
import { Clock, QrCode } from 'lucide-react';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import MyQrModal from './MyQrModal';
import { formatTime } from '../../utils/formatDate';

/**
 * Chấm công tại quầy (SRS FR-07, DEC-10): nhân viên bấm "Chấm công" để hiện QR cá nhân,
 * đưa vào máy chấm công đặt ở quầy; máy quét mã, chụp ảnh minh chứng rồi ghi check-in/check-out.
 * Lượt chấm đầu tiên trong ngày là check-in, lượt sau là check-out.
 */
export function CheckInOutPanel({ record, onChanged }) {
  const [now, setNow] = useState(new Date());
  const [qrOpen, setQrOpen] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const checkedIn = Boolean(record?.checkInAt);
  const checkedOut = Boolean(record?.checkOutAt);
  const label = checkedOut ? 'Đã chấm đủ hôm nay' : checkedIn ? 'Chấm công ra (check-out)' : 'Chấm công vào (check-in)';

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

          <Button onClick={() => setQrOpen(true)} disabled={checkedOut} icon={QrCode} className="w-full sm:w-auto">
            {label}
          </Button>
        </div>
      </div>

      <p className="text-xs text-slate-500">
        Chấm công tại <b>máy chấm công ở quầy lễ tân</b>: bấm nút để hiện mã QR cá nhân (đổi mỗi 15 giây, chỉ dùng một lần),
        đưa vào camera của máy quầy, máy sẽ chụp ảnh minh chứng. Không chấm được tại quầy (máy hỏng, quên điện thoại, đi công tác)
        thì gửi <b>đề nghị điều chỉnh công</b> kèm minh chứng.
        {record?.checkInPoint && <> Vào ca tại: {record.checkInPoint}.</>}
      </p>

      <MyQrModal isOpen={qrOpen} onClose={() => setQrOpen(false)} record={record} onDone={onChanged} />
    </div>
  );
}

export default CheckInOutPanel;
