import React, { useState } from 'react';
import { Printer, Download, Receipt, Building2, User } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import Button from '../../components/common/Button';
import { formatCurrency } from '../../utils/formatCurrency';
import logoSvg from '../../assets/logo.svg';

export function PayslipPage() {
  const { user } = useAuth();
  const [selectedMonth, setSelectedMonth] = useState('10/2026');

  const payslip = {
    period: 'Tháng 10/2026 (01/10/2026 - 31/10/2026)',
    employeeCode: 'EMP-004',
    fullName: user?.fullName || 'Nguyễn Hoàng Phương Ngân',
    department: 'Khối Kỹ Thuật & Công Nghệ',
    position: 'Senior Software Engineer',
    baseSalary: 28000000,
    standardDays: 22,
    actualDays: 22,
    overtimeHours: 4,
    overtimePay: 1200000,
    allowances: [
      { name: 'Phụ cấp ăn trưa (miễn thuế)', amount: 1000000 },
      { name: 'Phụ cấp điện thoại & đi lại', amount: 800000 },
      { name: 'Phụ cấp trách nhiệm kỹ thuật', amount: 1200000 },
    ],
    totalAllowances: 3000000,
    grossSalary: 32200000,
    insurances: {
      social: 2240000, // 8%
      health: 420000,  // 1.5%
      unemployment: 280000, // 1%
      total: 2940000,
    },
    tax: {
      taxableIncome: 28260000,
      personalRelief: 15500000, // Mức giảm trừ mới 15.5tr
      dependentRelief: 0,
      assessableIncome: 12760000,
      pitAmount: 1250000,
    },
    netSalary: 28010000,
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 no-print">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Phiếu Lương Cá Nhân
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Chi tiết các khoản thu nhập, khấu trừ bảo hiểm và thuế TNCN
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <select
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(e.target.value)}
            className="rounded-lg border border-slate-300 py-2 px-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
          >
            <option value="10/2026">Kỳ lương Tháng 10/2026</option>
            <option value="09/2026">Kỳ lương Tháng 09/2026</option>
          </select>
          <Button onClick={handlePrint} icon={Printer}>
            In phiếu lương
          </Button>
        </div>
      </div>

      {/* Printable Payslip Card */}
      <div
        id="printable-payslip"
        className="bg-white rounded-3xl border border-slate-200/80 shadow-md p-6 sm:p-10 text-slate-800"
      >
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-slate-900 pb-6">
          <div className="flex items-center space-x-3">
            <img src={logoSvg} alt="HRFlow" className="w-10 h-10" />
            <div>
              <h2 className="text-lg font-black tracking-tight text-blue-600">
                HRFLOW ENTERPRISE
              </h2>
              <p className="text-xs text-slate-500">
                Hệ thống Quản lý Nhân sự & Tiền lương
              </p>
            </div>
          </div>

          <div className="text-right">
            <h3 className="text-xl font-extrabold uppercase tracking-tight text-slate-900">
              PHIẾU BÁO LƯƠNG
            </h3>
            <p className="text-xs font-semibold text-slate-500">{payslip.period}</p>
          </div>
        </div>

        {/* Employee Summary */}
        <div className="my-6 grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 bg-slate-50 rounded-2xl text-xs">
          <div>
            <span className="text-slate-400 block">Họ và tên:</span>
            <span className="font-bold text-slate-900 text-sm">{payslip.fullName}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Mã nhân viên:</span>
            <span className="font-semibold text-slate-800">{payslip.employeeCode}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Phòng ban:</span>
            <span className="font-semibold text-slate-800">{payslip.department}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Chức vụ:</span>
            <span className="font-semibold text-slate-800">{payslip.position}</span>
          </div>
        </div>

        {/* Breakdown Tables */}
        <div className="space-y-6">
          {/* I. Thu nhập */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-blue-600 mb-2">
              I. Các khoản thu nhập (Gross)
            </h4>
            <div className="border border-slate-200 rounded-xl overflow-hidden text-xs">
              <div className="flex justify-between p-2.5 bg-slate-50 border-b border-slate-200 font-semibold">
                <span>Khoản mục</span>
                <span>Số tiền (VND)</span>
              </div>
              <div className="divide-y divide-slate-100">
                <div className="flex justify-between p-2.5">
                  <span>Lương hợp đồng ({payslip.actualDays}/{payslip.standardDays} công)</span>
                  <span className="font-semibold">{formatCurrency(payslip.baseSalary)}</span>
                </div>
                <div className="flex justify-between p-2.5">
                  <span>Lương làm thêm giờ OT ({payslip.overtimeHours} giờ)</span>
                  <span className="font-semibold">{formatCurrency(payslip.overtimePay)}</span>
                </div>
                {payslip.allowances.map((a, idx) => (
                  <div key={idx} className="flex justify-between p-2.5">
                    <span>{a.name}</span>
                    <span className="font-semibold">{formatCurrency(a.amount)}</span>
                  </div>
                ))}
                <div className="flex justify-between p-2.5 bg-blue-50/50 font-bold text-blue-900">
                  <span>Tổng thu nhập Gross</span>
                  <span>{formatCurrency(payslip.grossSalary)}</span>
                </div>
              </div>
            </div>
          </div>

          {/* II. Khấu trừ */}
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-red-600 mb-2">
              II. Các khoản giảm trừ theo luật định (Deductions)
            </h4>
            <div className="border border-slate-200 rounded-xl overflow-hidden text-xs">
              <div className="flex justify-between p-2.5 bg-slate-50 border-b border-slate-200 font-semibold">
                <span>Khoản mục giảm trừ</span>
                <span>Số tiền (VND)</span>
              </div>
              <div className="divide-y divide-slate-100">
                <div className="flex justify-between p-2.5">
                  <span>Bảo hiểm Xã hội (BHXH 8%)</span>
                  <span className="font-semibold text-red-600">-{formatCurrency(payslip.insurances.social)}</span>
                </div>
                <div className="flex justify-between p-2.5">
                  <span>Bảo hiểm Y tế (BHYT 1.5%)</span>
                  <span className="font-semibold text-red-600">-{formatCurrency(payslip.insurances.health)}</span>
                </div>
                <div className="flex justify-between p-2.5">
                  <span>Bảo hiểm Thất nghiệp (BHTN 1.0%)</span>
                  <span className="font-semibold text-red-600">-{formatCurrency(payslip.insurances.unemployment)}</span>
                </div>
                <div className="flex justify-between p-2.5">
                  <span>Thuế thu nhập cá nhân (TNCN)</span>
                  <span className="font-semibold text-red-600">-{formatCurrency(payslip.tax.pitAmount)}</span>
                </div>
                <div className="flex justify-between p-2.5 bg-red-50/40 font-bold text-red-900">
                  <span>Tổng các khoản giảm trừ</span>
                  <span>
                    -{formatCurrency(payslip.insurances.total + payslip.tax.pitAmount)}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* III. Thực lĩnh */}
          <div className="p-4 rounded-2xl bg-gradient-to-r from-blue-600 to-indigo-600 text-white flex items-center justify-between">
            <div>
              <p className="text-xs uppercase tracking-wider font-semibold opacity-90">
                Lương thực nhận (Net Take-home)
              </p>
              <p className="text-xs opacity-75 mt-0.5">
                Đã chuyển khoản qua ngân hàng Vietcombank
              </p>
            </div>
            <div className="text-2xl sm:text-3xl font-black font-mono">
              {formatCurrency(payslip.netSalary)}
            </div>
          </div>
        </div>

        {/* Footer sign off */}
        <div className="mt-10 pt-6 border-t border-slate-200 grid grid-cols-2 text-center text-xs">
          <div>
            <p className="font-bold text-slate-800">Người lập phiếu</p>
            <p className="text-slate-400 mt-0.5">Phòng Nhân sự & Tiền lương</p>
            <p className="mt-12 font-medium text-slate-700">Hồ Thị Gia Hân</p>
          </div>
          <div>
            <p className="font-bold text-slate-800">Người nhận lương</p>
            <p className="text-slate-400 mt-0.5">Ký và ghi rõ họ tên</p>
            <p className="mt-12 font-medium text-slate-700">{payslip.fullName}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default PayslipPage;
