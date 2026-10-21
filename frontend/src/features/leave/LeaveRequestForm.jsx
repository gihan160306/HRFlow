import React, { useState } from 'react';
import { X, Send } from 'lucide-react';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';

export function LeaveRequestForm({
  isOpen,
  onClose,
  onSubmit,
  isLoading = false,
}) {
  const [formData, setFormData] = useState({
    leaveType: 'ANNUAL',
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0],
    reason: '',
  });

  const [errors, setErrors] = useState({});

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const newErrors = {};
    if (!formData.reason.trim()) newErrors.reason = 'Lý do nghỉ phép là bắt buộc';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    onSubmit(formData);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl relative animate-in fade-in zoom-in-95 duration-150">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 p-1 rounded-lg"
        >
          <X className="w-5 h-5" />
        </button>

        <h3 className="text-lg font-bold text-slate-900 mb-4">
          Tạo đơn xin nghỉ phép (PTO)
        </h3>

        <form onSubmit={handleSubmit} className="space-y-4">
          <SelectField
            label="Loại nghỉ phép"
            name="leaveType"
            value={formData.leaveType}
            onChange={handleChange}
            options={[
              { value: 'ANNUAL', label: 'Nghỉ phép năm (hưởng 100% lương)' },
              { value: 'SICK', label: 'Nghỉ ốm đau / thai sản (BHXH chi trả)' },
              { value: 'UNPAID', label: 'Nghỉ không hưởng lương' },
              { value: 'WEDDING', label: 'Nghỉ kết hôn (3 ngày có lương)' },
            ]}
          />

          <div className="grid grid-cols-2 gap-4">
            <InputField
              label="Từ ngày"
              name="startDate"
              type="date"
              value={formData.startDate}
              onChange={handleChange}
              required
            />

            <InputField
              label="Đến hết ngày"
              name="endDate"
              type="date"
              value={formData.endDate}
              onChange={handleChange}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Lý do chi tiết <span className="text-red-500">*</span>
            </label>
            <textarea
              name="reason"
              rows={3}
              value={formData.reason}
              onChange={handleChange}
              placeholder="Ghi rõ lý do xin nghỉ phép..."
              className="block w-full rounded-lg border border-slate-300 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.reason && (
              <p className="mt-1 text-xs text-red-600 font-medium">
                {errors.reason}
              </p>
            )}
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-end space-x-3">
            <Button variant="outline" type="button" onClick={onClose}>
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={isLoading}
              icon={Send}
            >
              Gửi đơn xin nghỉ
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default LeaveRequestForm;
