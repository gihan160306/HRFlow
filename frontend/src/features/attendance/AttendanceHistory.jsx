import React, { useEffect, useState } from 'react';
import { Image } from 'lucide-react';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import ErrorMessage from '../../components/common/ErrorMessage';
import attendanceApi from '../../api/attendanceApi';
import { formatDate, formatTime, formatMinutes } from '../../utils/formatDate';

/** Ảnh minh chứng do máy chấm công tại quầy chụp; chỉ trả theo phạm vi own/team/company (backend kiểm tra). */
function PhotoModal({ target, onClose }) {
  const [urls, setUrls] = useState({});
  const [error, setError] = useState('');

  useEffect(() => {
    if (!target) return undefined;
    let alive = true;
    const created = [];
    setUrls({});
    setError('');
    const kinds = [target.hasCheckInPhoto && 'in', target.hasCheckOutPhoto && 'out'].filter(Boolean);
    Promise.all(kinds.map((k) => attendanceApi.photo(target.id, k).then((blob) => [k, URL.createObjectURL(blob)])))
      .then((pairs) => {
        pairs.forEach(([, u]) => created.push(u));
        if (alive) setUrls(Object.fromEntries(pairs));
      })
      .catch((e) => alive && setError(e.message));
    return () => {
      alive = false;
      created.forEach((u) => URL.revokeObjectURL(u));
    };
  }, [target]);

  return (
    <Modal isOpen={Boolean(target)} title={target ? `Ảnh minh chứng ${formatDate(target.workDate)}${target.employeeName ? ` – ${target.employeeName}` : ''}` : ''} onClose={onClose} maxWidth="max-w-2xl">
      {error && <ErrorMessage message={error} />}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {[['in', 'Check-in', target?.checkInAt, target?.checkInPoint], ['out', 'Check-out', target?.checkOutAt, target?.checkOutPoint]].map(([k, label, at, point]) => (
          <figure key={k} className="space-y-1.5">
            <div className="aspect-[4/3] bg-slate-100 rounded-xl overflow-hidden flex items-center justify-center">
              {urls[k] ? <img src={urls[k]} alt={`Ảnh ${label}`} className="w-full h-full object-cover" /> : <span className="text-xs text-slate-400">Không có ảnh</span>}
            </div>
            <figcaption className="text-xs text-slate-500">{label} {formatTime(at)}{point ? ` • ${point}` : ''}</figcaption>
          </figure>
        ))}
      </div>
      <p className="text-xs text-slate-400 mt-3">Ảnh chỉ dùng để đối chiếu, không nhận diện khuôn mặt. Mỗi lượt xem được ghi nhật ký.</p>
    </Modal>
  );
}

export function AttendanceHistory({ records = [], showEmployee = false, onConfirm }) {
  const [photoOf, setPhotoOf] = useState(null);
  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
      <div className="p-4 sm:p-6 border-b border-slate-100">
        <h3 className="text-base font-bold text-slate-800">{showEmployee ? 'Bản ghi công cần rà soát' : 'Lịch sử chấm công'}</h3>
      </div>
      {records.length === 0 ? (
        <p className="p-6 text-sm text-slate-500">Chưa có bản ghi chấm công trong khoảng này.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3.5">Ngày</th>
                {showEmployee && <th className="px-4 py-3.5">Nhân viên</th>}
                <th className="px-4 py-3.5">Giờ vào</th>
                <th className="px-4 py-3.5">Giờ ra</th>
                <th className="px-4 py-3.5">Giờ làm</th>
                <th className="px-4 py-3.5">Muộn / Sớm</th>
                <th className="px-4 py-3.5">Trạng thái</th>
                <th className="px-4 py-3.5">Minh chứng</th>
                {onConfirm && <th className="px-4 py-3.5" />}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {records.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/80">
                  <td className="px-4 py-3.5 font-semibold text-slate-900">{formatDate(r.workDate)}</td>
                  {showEmployee && <td className="px-4 py-3.5">{r.employeeName}<span className="block text-xs text-slate-400">{r.employeeCode}</span></td>}
                  <td className="px-4 py-3.5 font-mono">{formatTime(r.checkInAt)}</td>
                  <td className="px-4 py-3.5 font-mono">{formatTime(r.checkOutAt)}</td>
                  <td className="px-4 py-3.5">{formatMinutes(r.workedMinutes)}</td>
                  <td className="px-4 py-3.5 text-xs">{r.lateMinutes || 0}p / {r.earlyMinutes || 0}p</td>
                  <td className="px-4 py-3.5">
                    <StatusBadge status={r.status} />
                    {r.reviewReason && <span className="block text-xs text-amber-700 mt-1">{r.reviewReason}</span>}
                  </td>
                  <td className="px-4 py-3.5 text-xs">
                    {r.hasCheckInPhoto || r.hasCheckOutPhoto ? (
                      <button type="button" onClick={() => setPhotoOf(r)} className="inline-flex items-center gap-1 text-blue-600 hover:underline font-semibold">
                        <Image className="w-4 h-4" /> Xem ảnh
                      </button>
                    ) : (
                      <span className="text-slate-400">{r.method === 'QR' ? '-' : 'Điều chỉnh công'}</span>
                    )}
                    {r.checkInPoint && <span className="block text-slate-400 mt-0.5">{r.checkInPoint}</span>}
                  </td>
                  {onConfirm && (
                    <td className="px-4 py-3.5 text-right">
                      {r.status === 'NEEDS_REVIEW' && onConfirm(r)}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <PhotoModal target={photoOf} onClose={() => setPhotoOf(null)} />
    </div>
  );
}

export default AttendanceHistory;
