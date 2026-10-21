import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, UserPlus } from 'lucide-react';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import SelectField from '../../components/common/SelectField';
import employeeApi from '../../api/employeeApi';

export function EmployeeFormPage() {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    departmentId: '1',
    position: '',
    baseSalary: '15000000',
    insuranceSalary: '15000000',
    dependentsCount: '0',
    hireDate: new Date().toISOString().split('T')[0],
    bankAccount: '',
    bankName: 'Vietcombank',
  });

  const [errors, setErrors] = useState({});

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const newErrors = {};

    if (!formData.fullName.trim()) newErrors.fullName = 'Họ tên là bắt buộc';
    if (!formData.email.trim()) newErrors.email = 'Email là bắt buộc';
    if (!formData.position.trim()) newErrors.position = 'Vị trí là bắt buộc';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    try {
      setIsLoading(true);
      await employeeApi.createEmployee(formData).catch(() => null);
      navigate('/employees');
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex items-center space-x-4">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate('/employees')}
          icon={ArrowLeft}
        >
          Quay lại
        </Button>
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Thêm nhân viên mới
          </h1>
          <p className="text-xs text-slate-500">Khởi tạo hồ sơ nhân sự và phân bổ vị trí</p>
        </div>
      </div>

      <div className="bg-white rounded-3xl border border-slate-200/80 shadow-sm p-6 sm:p-8">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <InputField
              label="Họ và tên"
              name="fullName"
              placeholder="Nguyễn Văn A"
              value={formData.fullName}
              onChange={handleChange}
              error={errors.fullName}
              required
            />

            <InputField
              label="Email công ty"
              name="email"
              type="email"
              placeholder="nguyenvana@example.com"
              value={formData.email}
              onChange={handleChange}
              error={errors.email}
              required
            />

            <InputField
              label="Số điện thoại"
              name="phone"
              placeholder="0901234567"
              value={formData.phone}
              onChange={handleChange}
            />

            <SelectField
              label="Phòng ban"
              name="departmentId"
              value={formData.departmentId}
              onChange={handleChange}
              options={[
                { value: '1', label: 'Ban Giám Đốc' },
                { value: '2', label: 'Phòng Nhân Sự' },
                { value: '3', label: 'Khối Kỹ Thuật & Công Nghệ' },
                { value: '4', label: 'Phòng Kinh Doanh' },
                { value: '5', label: 'Phòng Tài Chính - Kế Toán' },
              ]}
              required
            />

            <InputField
              label="Chức danh / Vị trí"
              name="position"
              placeholder="Software Engineer"
              value={formData.position}
              onChange={handleChange}
              error={errors.position}
              required
            />

            <InputField
              label="Ngày vào làm việc"
              name="hireDate"
              type="date"
              value={formData.hireDate}
              onChange={handleChange}
              required
            />

            <InputField
              label="Mức lương cơ bản (VND)"
              name="baseSalary"
              type="number"
              placeholder="15000000"
              value={formData.baseSalary}
              onChange={handleChange}
              required
            />

            <InputField
              label="Số người phụ thuộc"
              name="dependentsCount"
              type="number"
              placeholder="0"
              value={formData.dependentsCount}
              onChange={handleChange}
            />

            <InputField
              label="Số tài khoản ngân hàng"
              name="bankAccount"
              placeholder="1903456789012"
              value={formData.bankAccount}
              onChange={handleChange}
            />

            <InputField
              label="Tên ngân hàng"
              name="bankName"
              placeholder="Techcombank / Vietcombank"
              value={formData.bankName}
              onChange={handleChange}
            />
          </div>

          <div className="pt-4 border-t border-slate-100 flex items-center justify-end space-x-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => navigate('/employees')}
            >
              Hủy bỏ
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={isLoading}
              icon={Save}
            >
              Lưu hồ sơ nhân viên
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default EmployeeFormPage;
