import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calculator, Calendar, ArrowRight, CheckCircle2 } from 'lucide-react';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';

const SAMPLE_PERIODS = [
  {
    id: 1,
    name: 'Kỳ lương Tháng 10/2026',
    startDate: '2026-10-01',
    endDate: '2026-10-31',
    standardWorkingDays: 22,
    employeeCount: 48,
    totalGross: 1250000000,
    totalNet: 1045000000,
    status: 'CALCULATED',
  },
  {
    id: 2,
    name: 'Kỳ lương Tháng 09/2026',
    startDate: '2026-09-01',
    endDate: '2026-09-30',
    standardWorkingDays: 22,
    employeeCount: 47,
    totalGross: 1210000000,
    totalNet: 1012000000,
    status: 'PAID',
  },
  {
    id: 3,
    name: 'Kỳ lương Tháng 08/2026',
    startDate: '2026-08-01',
    endDate: '2026-08-31',
    standardWorkingDays: 21,
    employeeCount: 45,
    totalGross: 1180000000,
    totalNet: 986000000,
    status: 'PAID',
  },
];

export function PayrollListPage() {
  const navigate = useNavigate();
  const [periods] = useState(SAMPLE_PERIODS);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
            Quản lý Kỳ tính lương
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Bảng lương tự động theo công thức Luật Lao Động & Luật Thuế TNCN 2026
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4">
        {periods.map((p) => (
          <div
            key={p.id}
            className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow flex flex-col md:flex-row md:items-center justify-between gap-6"
          >
            <div className="space-y-2">
              <div className="flex items-center space-x-3">
                <div className="p-3 bg-blue-50 text-blue-600 rounded-xl">
                  <Calculator className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-900">{p.name}</h3>
                  <p className="text-xs text-slate-500 flex items-center mt-0.5">
                    <Calendar className="w-3.5 h-3.5 mr-1" />
                    {formatDate(p.startDate)} - {formatDate(p.endDate)} • {p.standardWorkingDays} công chuẩn
                  </p>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-6 text-sm">
              <div>
                <span className="text-xs text-slate-400 block">Số nhân viên</span>
                <span className="font-bold text-slate-800">{p.employeeCount} nhân sự</span>
              </div>
              <div>
                <span className="text-xs text-slate-400 block">Tổng quỹ Gross</span>
                <span className="font-bold text-slate-900">
                  {formatCurrency(p.totalGross)}
                </span>
              </div>
              <div>
                <span className="text-xs text-slate-400 block">Tổng thực lĩnh (Net)</span>
                <span className="font-extrabold text-blue-600 text-base">
                  {formatCurrency(p.totalNet)}
                </span>
              </div>
              <div>
                <StatusBadge status={p.status} />
              </div>
              <div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => navigate(`/payroll/${p.id}`)}
                  icon={ArrowRight}
                >
                  Xem chi tiết
                </Button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default PayrollListPage;
