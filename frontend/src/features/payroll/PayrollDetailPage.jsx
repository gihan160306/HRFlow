import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, CheckCircle2 } from 'lucide-react';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import PayrollActions from './PayrollActions';
import { formatCurrency } from '../../utils/formatCurrency';

const SAMPLE_PAYROLL_ITEMS = [
  {
    id: 1,
    employeeCode: 'EMP-001',
    fullName: 'Đặng Minh Hiếu',
    baseSalary: 45000000,
    actualDays: 22,
    overtimePay: 0,
    allowance: 6200000,
    grossSalary: 51200000,
    insuranceDeduction: 4725000,
    personalIncomeTax: 4250000,
    netSalary: 42225000,
  },
  {
    id: 2,
    employeeCode: 'EMP-002',
    fullName: 'Hồ Thị Gia Hân',
    baseSalary: 32000000,
    actualDays: 22,
    overtimePay: 0,
    allowance: 4500000,
    grossSalary: 36500000,
    insuranceDeduction: 3360000,
    personalIncomeTax: 2125000,
    netSalary: 31015000,
  },
  {
    id: 3,
    employeeCode: 'EMP-003',
    fullName: 'Trần Thị Lệ Hoa',
    baseSalary: 38000000,
    actualDays: 22,
    overtimePay: 0,
    allowance: 5000000,
    grossSalary: 43000000,
    insuranceDeduction: 3990000,
    personalIncomeTax: 3180000,
    netSalary: 35830000,
  },
  {
    id: 4,
    employeeCode: 'EMP-004',
    fullName: 'Nguyễn Hoàng Phương Ngân',
    baseSalary: 28000000,
    actualDays: 21,
    overtimePay: 1200000,
    allowance: 3000000,
    grossSalary: 30927000,
    insuranceDeduction: 2940000,
    personalIncomeTax: 1250000,
    netSalary: 26737000,
  },
  {
    id: 5,
    employeeCode: 'EMP-005',
    fullName: 'Hồ Vinh Quan',
    baseSalary: 22000000,
    actualDays: 22,
    overtimePay: 850000,
    allowance: 2500000,
    grossSalary: 25350000,
    insuranceDeduction: 2310000,
    personalIncomeTax: 450000,
    netSalary: 22590000,
  },
];

export function PayrollDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [periodStatus, setPeriodStatus] = useState('CALCULATED');
  const [items, setItems] = useState(SAMPLE_PAYROLL_ITEMS);
  const [isCalculating, setIsCalculating] = useState(false);

  const handleCalculate = () => {
    setIsCalculating(true);
    setTimeout(() => {
      setIsCalculating(false);
      setPeriodStatus('CALCULATED');
    }, 800);
  };

  const handleApprove = () => {
    setPeriodStatus('APPROVED');
  };

  const handleSendPayslips = () => {
    setPeriodStatus('PAID');
    alert('Đã gửi thông báo phiếu lương qua email cho tất cả nhân viên!');
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div className="flex items-center space-x-4">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate('/payroll')}
            icon={ArrowLeft}
          >
            Danh sách kỳ lương
          </Button>
          <div>
            <div className="flex items-center space-x-3">
              <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
                Chi tiết Bảng lương Tháng 10/2026
              </h1>
              <StatusBadge status={periodStatus} />
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Thời gian: 01/10/2026 - 31/10/2026 • Chuẩn: 22 ngày công
            </p>
          </div>
        </div>

        <PayrollActions
          periodStatus={periodStatus}
          onCalculate={handleCalculate}
          onApprove={handleApprove}
          onSendPayslips={handleSendPayslips}
          isLoading={isCalculating}
        />
      </div>

      {/* Detailed payroll breakdown table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600">
            <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
              <tr>
                <th className="px-4 py-3.5">Mã & Họ tên</th>
                <th className="px-4 py-3.5">Lương cơ bản</th>
                <th className="px-4 py-3.5">Ngày công</th>
                <th className="px-4 py-3.5">Tiền OT</th>
                <th className="px-4 py-3.5">Phụ cấp</th>
                <th className="px-4 py-3.5">Thu nhập Gross</th>
                <th className="px-4 py-3.5">BHXH (10.5%)</th>
                <th className="px-4 py-3.5">Thuế TNCN</th>
                <th className="px-4 py-3.5 text-right">Thực lĩnh (Net)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {items.map((it) => (
                <tr key={it.id} className="hover:bg-slate-50/80 transition-colors">
                  <td className="px-4 py-3.5">
                    <p className="font-bold text-slate-900">{it.fullName}</p>
                    <p className="text-xs text-slate-400">{it.employeeCode}</p>
                  </td>
                  <td className="px-4 py-3.5 text-slate-700">
                    {formatCurrency(it.baseSalary)}
                  </td>
                  <td className="px-4 py-3.5 font-semibold text-slate-800">
                    {it.actualDays}
                  </td>
                  <td className="px-4 py-3.5 text-amber-600 font-medium">
                    {it.overtimePay > 0 ? `+${formatCurrency(it.overtimePay)}` : '-'}
                  </td>
                  <td className="px-4 py-3.5 text-slate-600">
                    {formatCurrency(it.allowance)}
                  </td>
                  <td className="px-4 py-3.5 font-semibold text-slate-900">
                    {formatCurrency(it.grossSalary)}
                  </td>
                  <td className="px-4 py-3.5 text-red-600">
                    -{formatCurrency(it.insuranceDeduction)}
                  </td>
                  <td className="px-4 py-3.5 text-red-600">
                    -{formatCurrency(it.personalIncomeTax)}
                  </td>
                  <td className="px-4 py-3.5 text-right font-extrabold text-blue-600 text-base">
                    {formatCurrency(it.netSalary)}
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

export default PayrollDetailPage;
