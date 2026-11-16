import React, { useMemo, useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import { reportApi } from '../../api/recordsApi';
import InputField from '../../components/common/InputField';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import { EMPLOYEE_TYPES } from '../../utils/constants';

const box = 'bg-white rounded-2xl border border-slate-200/80 shadow-sm p-5';
const typeLabel = (v) => EMPLOYEE_TYPES.find((t) => t.value === v)?.label || v;

function Bar({ value, max, color = 'bg-blue-600' }) {
  const pct = max > 0 ? Math.max(2, Math.round((value / max) * 100)) : 0;
  return (
    <div className="h-2.5 rounded-full bg-slate-100 overflow-hidden">
      <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
    </div>
  );
}

export function ReportsPage() {
  const { isHR, isAdmin, isManager } = useAuth();
  const [year, setYear] = useState(String(new Date().getFullYear()));

  const headcount = useAsync(() => reportApi.headcount(), [], { enabled: isHR || isAdmin });
  const ot = useAsync(() => reportApi.otHours(year), [year], { enabled: isHR || isManager });

  const byDept = useMemo(() => {
    const map = new Map();
    (headcount.data || []).forEach((r) => {
      const cur = map.get(r.department) || { department: r.department, total: 0, types: [] };
      cur.total += Number(r.headcount);
      cur.types.push(`${typeLabel(r.employeeType)}: ${r.headcount}`);
      map.set(r.department, cur);
    });
    return [...map.values()].sort((a, b) => b.total - a.total);
  }, [headcount.data]);

  const maxDept = Math.max(0, ...byDept.map((d) => d.total));
  const otRows = ot.data || [];
  const maxOt = Math.max(0, ...otRows.map((r) => Number(r.hours)));
  const totalOt = otRows.reduce((s, r) => s + Number(r.hours), 0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Báo cáo</h1>
        <p className="text-sm text-slate-500 mt-0.5">Báo cáo tổng hợp, không bao gồm số liệu lương của từng cá nhân.</p>
      </div>

      {(isHR || isAdmin) && (
        <section className={box}>
          <h2 className="font-bold text-slate-800 mb-1">Nhân sự theo phòng ban</h2>
          <p className="text-xs text-slate-500 mb-4">Không tính nhân viên đã nghỉ việc.</p>
          {headcount.loading ? <Loading /> : headcount.error ? <ErrorMessage message={headcount.error.message} onRetry={headcount.reload} />
            : !byDept.length ? <EmptyState title="Chưa có dữ liệu" /> : (
              <div className="space-y-3">
                {byDept.map((d) => (
                  <div key={d.department}>
                    <div className="flex justify-between text-sm">
                      <span className="font-semibold text-slate-800">{d.department}</span>
                      <span className="text-slate-600"><b>{d.total}</b> người <span className="text-xs text-slate-400">({d.types.join(' • ')})</span></span>
                    </div>
                    <Bar value={d.total} max={maxDept} />
                  </div>
                ))}
                <p className="pt-2 text-sm text-slate-600 border-t border-slate-100">Tổng: <b>{byDept.reduce((s, d) => s + d.total, 0)}</b> nhân sự</p>
              </div>
            )}
        </section>
      )}

      {(isHR || isManager) && (
        <section className={box}>
          <div className="flex flex-wrap items-end justify-between gap-3 mb-4">
            <div>
              <h2 className="font-bold text-slate-800">Giờ OT thực tế theo tháng</h2>
              <p className="text-xs text-slate-500">{isHR ? 'Toàn công ty' : 'Nhân viên thuộc team của bạn'} • chỉ tính giờ OT đã được xác nhận.</p>
            </div>
            <InputField id="rp-year" label="Năm" type="number" className="w-28" value={year} onChange={(e) => setYear(e.target.value)} />
          </div>
          {ot.loading ? <Loading /> : ot.error ? <ErrorMessage message={ot.error.message} onRetry={ot.reload} />
            : !otRows.length ? <EmptyState title={`Chưa có giờ OT nào trong năm ${year}`} /> : (
              <div className="space-y-3">
                {otRows.map((r) => (
                  <div key={r.month}>
                    <div className="flex justify-between text-sm">
                      <span className="font-semibold text-slate-800">Tháng {r.month}</span>
                      <span className="text-slate-600"><b>{Number(r.hours).toLocaleString('vi-VN')}</b> giờ</span>
                    </div>
                    <Bar value={Number(r.hours)} max={maxOt} color="bg-amber-500" />
                  </div>
                ))}
                <p className="pt-2 text-sm text-slate-600 border-t border-slate-100">Tổng năm {year}: <b>{totalOt.toLocaleString('vi-VN')}</b> giờ</p>
              </div>
            )}
        </section>
      )}
    </div>
  );
}

export default ReportsPage;
