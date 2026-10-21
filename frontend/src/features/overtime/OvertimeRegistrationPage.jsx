import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Zap, ArrowLeft, Send } from 'lucide-react';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import StatusBadge from '../../components/common/StatusBadge';
import { formatDate } from '../../utils/formatDate';

const MY_OT_RECORDS = [
  {
    id: 1,
    date: '2026-10-14',
    hours: 2.5,
    type: 'Ngày thường (150%)',
    task: 'Fix critical bug module bảo hiểm xã hội',
    status: 'APPROVED',
  },
  {
    id: 2,
    date: '2026-10-21',
    hours: 4.0,
    type: 'Ngày thường (150%)',
    task: 'Deploy phiên bản tính lương v2.0',
    status: 'PENDING',
  },
];

export function OvertimeRegistrationPage() {
  const navigate = useNavigate();
  const [registrations, setRegistrations] = useState(MY_OT_RECORDS);
  const [formData, setFormData] = useState({
    date: new Date().toISOString().split('T')[0],
    hours: '2.0',
    type: 'WEEKDAY',
    task: '',
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!formData.task.trim()) return;

    const newRec = {
      id: Date.now(),
      date: formData.date,
      hours: parseFloat(formData.hours),
      type: formData.type === 'WEEKDAY' ? 'Ngày thường (150%)' : 'Cuối tuần (200%)',
      task: formData.task,
      status: 'PENDING',
    };

    setRegistrations((prev) => [newRec, ...prev]);
    setFormData({
      date: new Date().toISOString().split('T')[0],
      hours: '2.0',
      type: 'WEEKDAY',
      task: '',
    });
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex items-center space-x-4">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate('/overtime')}
          icon={ArrowLeft}
        >
          Quay lại danh mục OT
        </Button>
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Đăng ký Tăng ca (OT)
          </h1>
          <p className="text-xs text-slate-500">Khai báo giờ làm thêm ngoài giờ tiêu chuẩn</p>
        </div>
      </div>

      {/* Registration Form */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6">
        <h3 className="text-base font-bold text-slate-900 mb-4 flex items-center">
          <Zap className="w-5 h-5 mr-2 text-amber-500" />
          Kê khai ca làm thêm giờ
        </h3>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <InputField
              label="Ngày làm OT"
              type="date"
              value={formData.date}
              onChange={(e) => setFormData({ ...formData, date: e.target.value })}
              required
            />

            <InputField
              label="Số giờ dự kiến"
              type="number"
              step="0.5"
              value={formData.hours}
              onChange={(e) => setFormData({ ...formData, hours: e.target.value })}
              required
            />

            <SelectField
              label="Phân loại ngày OT"
              value={formData.type}
              onChange={(e) => setFormData({ ...formData, type: e.target.value })}
              options={[
                { value: 'WEEKDAY', label: 'Ngày thường (hệ số 1.5x)' },
                { value: 'WEEKEND', label: 'Ngày nghỉ cuối tuần (hệ số 2.0x)' },
                { value: 'HOLIDAY', label: 'Ngày lễ, Tết (hệ số 3.0x)' },
              ]}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Nội dung công việc thực hiện <span className="text-red-500">*</span>
            </label>
            <textarea
              rows={2}
              value={formData.task}
              onChange={(e) => setFormData({ ...formData, task: e.target.value })}
              placeholder="Mô tả công việc cần xử lý thêm giờ..."
              className="block w-full rounded-lg border border-slate-300 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>

          <div className="flex justify-end pt-2">
            <Button type="submit" icon={Send}>
              Gửi đăng ký OT
            </Button>
          </div>
        </form>
      </div>

      {/* Personal list */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100">
          <h3 className="text-base font-bold text-slate-800">
            Lịch sử đăng ký tăng ca của tôi
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3.5">Ngày OT</th>
                <th className="px-4 py-3.5">Loại ca</th>
                <th className="px-4 py-3.5">Số giờ</th>
                <th className="px-4 py-3.5">Nội dung</th>
                <th className="px-4 py-3.5">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {registrations.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="px-4 py-3.5 font-bold text-slate-900">
                    {formatDate(r.date)}
                  </td>
                  <td className="px-4 py-3.5 font-medium text-slate-700">{r.type}</td>
                  <td className="px-4 py-3.5 font-bold text-slate-900">{r.hours}h</td>
                  <td className="px-4 py-3.5 text-slate-600">{r.task}</td>
                  <td className="px-4 py-3.5">
                    <StatusBadge status={r.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default OvertimeRegistrationPage;
