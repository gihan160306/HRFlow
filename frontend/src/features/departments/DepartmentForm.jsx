import React, { useState } from 'react';
import { X, Save } from 'lucide-react';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';

export function DepartmentForm({
  isOpen,
  initialData,
  onClose,
  onSubmit,
  isLoading = false,
}) {
  const [formData, setFormData] = useState({
    name: initialData?.name || '',
    code: initialData?.code || '',
    description: initialData?.description || '',
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
    if (!formData.name.trim()) newErrors.name = 'Tên phòng ban là bắt buộc';
    if (!formData.code.trim()) newErrors.code = 'Mã phòng ban là bắt buộc';

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
          {initialData ? 'Chỉnh sửa phòng ban' : 'Tạo phòng ban mới'}
        </h3>

        <form onSubmit={handleSubmit} className="space-y-4">
          <InputField
            label="Tên phòng ban"
            name="name"
            placeholder="Khối Kỹ Thuật & Công Nghệ"
            value={formData.name}
            onChange={handleChange}
            error={errors.name}
            required
          />

          <InputField
            label="Mã viết tắt"
            name="code"
            placeholder="TECH"
            value={formData.code}
            onChange={handleChange}
            error={errors.code}
            required
          />

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">
              Mô tả nhiệm vụ
            </label>
            <textarea
              name="description"
              rows={3}
              value={formData.description}
              onChange={handleChange}
              placeholder="Mô tả chức năng nhiệm vụ của phòng ban..."
              className="block w-full rounded-lg border border-slate-300 p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="pt-3 border-t border-slate-100 flex items-center justify-end space-x-3">
            <Button variant="outline" type="button" onClick={onClose}>
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={isLoading}
              icon={Save}
            >
              Lưu phòng ban
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default DepartmentForm;
