import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Mail, Phone, Building2, Briefcase, CreditCard, Shield, Calendar } from 'lucide-react';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import { formatCurrency } from '../../utils/formatCurrency';

export function EmployeeDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  // Sample or loaded data
  const employee = {
    id: id || 1,
    employeeCode: `EMP-00${id || 1}`,
    fullName: 'Đặng Minh Hiếu',
    email: 'admin@example.com',
    phone: '0901234567',
    departmentName: 'Ban Giám Đốc',
    position: 'Giám Đốc Vận Hành / Admin',
    hireDate: '2021-01-10',
    contractType: 'Không xác định thời hạn (Indefinite)',
    baseSalary: 45000000,
    insuranceSalary: 45000000,
    dependentsCount: 1,
    taxCode: '8023456789',
    idCardNumber: '079095001234',
    bankAccount: '1903456789012',
    bankName: 'Techcombank',
    status: 'ACTIVE',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-4">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate('/employees')}
          icon={ArrowLeft}
        >
          Quay lại danh sách
        </Button>
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Hồ sơ Nhân viên: {employee.fullName}
          </h1>
          <p className="text-xs text-slate-500">Mã nhân sự: {employee.employeeCode}</p>
        </div>
      </div>

      {/* Main Dossier Card */}
      <div className="bg-white rounded-3xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-6 sm:p-8 bg-gradient-to-r from-blue-50 to-indigo-50/50 border-b border-slate-100 flex flex-col sm:flex-row items-center sm:items-start gap-6">
          <img
            src={employee.avatar}
            alt={employee.fullName}
            className="w-24 h-24 rounded-2xl object-cover ring-4 ring-white shadow-md"
          />
          <div className="flex-1 text-center sm:text-left">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2">
              <h2 className="text-2xl font-bold text-slate-900">{employee.fullName}</h2>
              <StatusBadge status={employee.status} />
            </div>
            <p className="text-sm font-medium text-blue-600 mt-1 flex items-center justify-center sm:justify-start">
              <Briefcase className="w-4 h-4 mr-1.5" />
              {employee.position} • {employee.departmentName}
            </p>

            <div className="mt-4 flex flex-wrap gap-4 text-xs text-slate-500 justify-center sm:justify-start">
              <span className="flex items-center">
                <Mail className="w-3.5 h-3.5 mr-1 text-slate-400" />
                {employee.email}
              </span>
              <span className="flex items-center">
                <Phone className="w-3.5 h-3.5 mr-1 text-slate-400" />
                {employee.phone}
              </span>
              <span className="flex items-center">
                <Calendar className="w-3.5 h-3.5 mr-1 text-slate-400" />
                Vào làm: {employee.hireDate}
              </span>
            </div>
          </div>
        </div>

        {/* Details Grid */}
        <div className="p-6 sm:p-8 grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Section 1: Hợp đồng & Tiền lương */}
          <div className="space-y-4">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 flex items-center">
              <CreditCard className="w-4 h-4 mr-2 text-blue-600" />
              Lương & Hợp đồng
            </h3>
            <div className="bg-slate-50 rounded-2xl p-4 space-y-3 text-sm">
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Loại hợp đồng</span>
                <span className="font-semibold text-slate-800">{employee.contractType}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Lương cơ bản</span>
                <span className="font-bold text-blue-600 text-base">
                  {formatCurrency(employee.baseSalary)}
                </span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Lương đóng BHXH</span>
                <span className="font-semibold text-slate-800">
                  {formatCurrency(employee.insuranceSalary)}
                </span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Số người phụ thuộc (giảm trừ)</span>
                <span className="font-semibold text-slate-800">{employee.dependentsCount} người</span>
              </div>
            </div>
          </div>

          {/* Section 2: Tài khoản & Thuế */}
          <div className="space-y-4">
            <h3 className="text-sm font-bold uppercase tracking-wider text-slate-400 flex items-center">
              <Shield className="w-4 h-4 mr-2 text-indigo-600" />
              Định danh & Ngân hàng
            </h3>
            <div className="bg-slate-50 rounded-2xl p-4 space-y-3 text-sm">
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Số CCCD / CMND</span>
                <span className="font-semibold text-slate-800">{employee.idCardNumber}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Mã số thuế TNCN</span>
                <span className="font-semibold text-slate-800">{employee.taxCode}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Số tài khoản</span>
                <span className="font-semibold text-slate-800">{employee.bankAccount}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">Ngân hàng</span>
                <span className="font-semibold text-slate-800">{employee.bankName}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default EmployeeDetailPage;
