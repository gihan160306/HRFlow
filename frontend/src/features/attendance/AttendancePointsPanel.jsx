import React, { useState } from 'react';
import { MapPin, Plus, Pencil } from 'lucide-react';
import useAsync from '../../hooks/useAsync';
import attendanceApi from '../../api/attendanceApi';
import Button from '../../components/common/Button';
import FormModal from '../../components/common/FormModal';
import ErrorMessage from '../../components/common/ErrorMessage';

/**
 * HR cấu hình điểm chấm công (ví dụ quầy lễ tân). Máy chấm công tại quầy là tài khoản thiết bị KIOSK
 * do Admin tạo theo yêu cầu của HR và gắn với điểm (SRS FR-07, FR-31; permission_matrix 3.1/3.2).
 */
export function AttendancePointsPanel() {
  const points = useAsync(() => attendanceApi.points(), []);
  const [edit, setEdit] = useState(null); // {} = tạo mới, point = sửa

  return (
    <section className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
      <div className="p-4 sm:p-6 border-b border-slate-100 flex items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-bold text-slate-800">Điểm chấm công</h3>
          <p className="text-xs text-slate-500 mt-0.5">Mỗi máy chấm công tại quầy gắn với một điểm. Cần thêm máy: đề nghị Admin tạo tài khoản máy chấm công.</p>
        </div>
        <Button size="sm" icon={Plus} onClick={() => setEdit({})}>Thêm điểm</Button>
      </div>
      {points.error && <div className="p-4"><ErrorMessage message={points.error.message} onRetry={points.reload} /></div>}
      <ul className="divide-y divide-slate-100">
        {(points.data || []).map((p) => (
          <li key={p.id} className="px-4 sm:px-6 py-3 flex items-center justify-between gap-3 text-sm">
            <div className="flex items-center gap-3">
              <MapPin className={`w-5 h-5 ${p.active ? 'text-emerald-600' : 'text-slate-300'}`} />
              <div>
                <p className="font-semibold text-slate-900">{p.name} <span className="text-xs font-mono text-slate-400">{p.code}</span></p>
                <p className="text-xs text-slate-500">{p.active ? 'Đang hoạt động' : 'Ngừng hoạt động'} • {p.kioskCount} máy chấm công</p>
              </div>
            </div>
            <Button size="sm" variant="outline" icon={Pencil} onClick={() => setEdit(p)}>Sửa</Button>
          </li>
        ))}
      </ul>
      <FormModal
        isOpen={Boolean(edit)}
        title={edit?.id ? `Sửa điểm chấm công – ${edit.name}` : 'Thêm điểm chấm công'}
        fields={[
          { name: 'code', label: 'Mã điểm', required: true, half: true },
          { name: 'name', label: 'Tên điểm', required: true, half: true },
          { name: 'active', label: 'Đang hoạt động (ngừng thì máy quầy gắn với điểm không quét được)', type: 'checkbox' },
        ]}
        initial={edit?.id ? edit : { active: true }}
        onClose={() => setEdit(null)}
        onSubmit={async (v) => {
          const body = { code: v.code.trim(), name: v.name.trim(), active: Boolean(v.active) };
          if (edit?.id) await attendanceApi.updatePoint(edit.id, body);
          else await attendanceApi.createPoint(body);
          points.reload();
        }}
      />
    </section>
  );
}

export default AttendancePointsPanel;
