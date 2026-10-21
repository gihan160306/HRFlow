import React, { useState } from 'react';
import { X, Save } from 'lucide-react';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';

export function ContractForm({
  isOpen,
  onClose,
  onSubmit,
  isLoading = false,
}) {
  const [formData, setFormData] = useState({
    contractNumber: `HDLD-${new Date().getFullYear()}-${Math.floor(100 + Math.random() * 900)}`,
    employeeName: '',
    contractType: 'INDEFINITE',
    startDate: new Date().toISOString().split('T')[0],
    endDate: '',
    baseSalary: '20000000',
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
    if (!formData.employeeName.trim()) newErrors.employeeName = 'Vui lòng nhập tên nhân viên';
    if (!formData.contractNumber.trim()) newErrors.contractNumber = 'Số hợp đồng là bắt buộc';

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
          Tạo hợp đồng lao động
        </h3>

        <form onSubmit={handleSubmit} className="space-y-4">
          <InputField
            label="Số hợp đồng"
            name="contractNumber"
            value={formData.contractNumber}
            onChange={handleChange}
            error={errors.contractNumber}
            required
          />

          <InputField
            label="Họ tên nhân viên"
            name="employeeName"
            placeholder="Nguyễn Văn A"
            value={formData.employeeName}
            onChange={handleChange}
            error={errors.employeeName}
            required
          />

          <SelectField
            label="Loại hợp đồng"
            name="contractType"
            value={formData.contractType}
            onChange={handleChange}
            options={[
              { value: 'PROBATION', label: 'Thử việc (2 tháng)' },
              { value: 'DEFINITE_1YR', label: 'Xác định thời hạn (12 tháng)' },
              { value: 'DEFINITE_3YR', label: 'Xác định thời hạn (36 tháng)' },
              { value: 'INDEFINITE', label: 'Không xác định thời hạn' },
            ]}
          />

          <div className="grid grid-cols-2 gap-4">
            <InputField
              label="Ngày hiệu lực"
              name="startDate"
              type="date"
              value={formData.startDate}
              onChange={handleChange}
              required
            />

            <InputField
              label="Ngày hết hạn (nếu có)"
              name="endDate"
              type="date"
              value={formData.endDate}
              onChange={handleChange}
            />
          </div>

          <InputField
            label="Mức lương thỏa thuận (VND)"
            name="baseSalary"
            type="number"
            value={formData.baseSalary}
            onChange={handleChange}
            required
          />

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
              Ký kết hợp đồng
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ContractForm;
