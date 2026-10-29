import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, ArrowRight } from 'lucide-react';
import Button from '../../components/common/Button';
import StatusBadge from '../../components/common/StatusBadge';
import { formatCurrency } from '../../utils/formatCurrency';
import { formatDate } from '../../utils/formatDate';
import payrollApi from '../../api/payrollApi';

export function PayrollListPage() {
  const navigate = useNavigate();
  const [periods, setPeriods] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    payrollApi.getPayrollPeriods()
      .then((data) => setPeriods(Array.isArray(data) ? data : []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900">Quản lý kỳ tính lương</h1>
        <p className="text-sm text-slate-500 mt-1">Dữ liệu kỳ lương và phiếu đã lưu trong hệ thống</p>
      </div>
      {loading && <p className="text-slate-500">Đang tải kỳ lương...</p>}
      {error && <p role="alert" className="text-red-600">{error}</p>}
      {!loading && !error && periods.length === 0 && <p className="text-slate-500">Chưa có kỳ lương.</p>}
      <div className="grid grid-cols-1 gap-4">
        {periods.map((p) => {
          const gross = p.records.reduce((sum, r) => sum + Number(r.grossSalary || 0), 0);
          const net = p.records.reduce((sum, r) => sum + Number(r.netSalary || 0), 0);
          const hasLegacySample = p.records.some((r) => !r.policyVersion);
          return (
            <div key={p.periodId} className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-6">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Kỳ lương {p.periodCode}</h2>
                {hasLegacySample && <p className="text-xs text-amber-700">Có dữ liệu mẫu cũ, chưa đối soát</p>}
                <p className="text-xs text-slate-500 flex items-center mt-1">
                  <Calendar className="w-3.5 h-3.5 mr-1" />{formatDate(p.startDate)} – {formatDate(p.endDate)}
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-6 text-sm">
                <div><span className="text-xs text-slate-400 block">Nhân viên</span><b>{p.employeeCount}</b></div>
                <div><span className="text-xs text-slate-400 block">Tổng Gross</span><b>{formatCurrency(gross)}</b></div>
                <div><span className="text-xs text-slate-400 block">Tổng thực chi</span><b>{formatCurrency(net)}</b></div>
                <StatusBadge status={p.status} />
                <Button size="sm" variant="outline" icon={ArrowRight} onClick={() => navigate(`/payroll/${p.periodId}`)}>Xem chi tiết</Button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default PayrollListPage;
