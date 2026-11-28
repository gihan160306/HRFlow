import React, { useMemo, useState } from 'react';
import { Banknote, Clock, Download, Users } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import useAsync from '../../hooks/useAsync';
import { reportApi } from '../../api/recordsApi';
import Button from '../../components/common/Button';
import SelectField from '../../components/common/SelectField';
import StatusBadge from '../../components/common/StatusBadge';
import Loading from '../../components/common/Loading';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import { EMPLOYEE_TYPES } from '../../utils/constants';
import { formatCurrency } from '../../utils/formatCurrency';

const cardBox = 'bg-white rounded-2xl border border-slate-200/90 shadow-sm overflow-hidden';

const typeLabel = (v) => EMPLOYEE_TYPES.find((t) => t.value === v)?.label || v;

const EMP_TYPE_SERIES = [
  { key: 'FULL_TIME', label: 'Toàn thời gian', barClass: 'bg-blue-600', dotClass: 'bg-blue-600' },
  { key: 'PART_TIME', label: 'Bán thời gian', barClass: 'bg-cyan-500', dotClass: 'bg-cyan-500' },
  { key: 'PROBATION', label: 'Thử việc', barClass: 'bg-amber-500', dotClass: 'bg-amber-500' },
  { key: 'INTERN', label: 'Thực tập sinh', barClass: 'bg-emerald-500', dotClass: 'bg-emerald-500' },
];

const EMP_STATUS_LABELS = {
  ACTIVE: 'Đang làm việc',
  ONBOARDING: 'Đang tiếp nhận',
  ON_LEAVE: 'Đang nghỉ',
  TERMINATED: 'Đã nghỉ việc',
};

const HEADCOUNT_STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'ACTIVE', label: 'Đang làm việc' },
  { value: 'ONBOARDING', label: 'Đang tiếp nhận' },
  { value: 'ON_LEAVE', label: 'Đang nghỉ' },
  { value: 'TERMINATED', label: 'Đã nghỉ việc' },
];

const FUND_STATUS_OPTIONS = [
  { value: 'CALCULATED', label: 'Tạm tính (Đã tính)' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'LOCKED', label: 'Đã khóa' },
];

const FUND_SERIES = [
  { key: 'totalGross', label: 'Tổng Gross', barClass: 'bg-blue-600', dotClass: 'bg-blue-600' },
  { key: 'totalNet', label: 'Net (sau nghĩa vụ)', barClass: 'bg-emerald-500', dotClass: 'bg-emerald-500' },
  { key: 'totalPayable', label: 'Thực chi (Payable)', barClass: 'bg-orange-500', dotClass: 'bg-orange-500' },
];

const MONTHS_1_TO_12 = Array.from({ length: 12 }, (_, idx) => idx + 1);

function niceScale(rawMax, steps = 4) {
  if (!rawMax || rawMax <= 0) {
    return { max: steps, ticks: Array.from({ length: steps + 1 }, (_, i) => steps - i) };
  }
  const padded = rawMax * 1.15;
  const rawStep = padded / steps;
  const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
  const normalized = rawStep / magnitude;
  let niceUnit = 1;
  if (normalized > 5) niceUnit = 10;
  else if (normalized > 2.5) niceUnit = 5;
  else if (normalized > 2) niceUnit = 2.5;
  else if (normalized > 1) niceUnit = 2;
  let step = niceUnit * magnitude;
  if (rawMax <= steps) step = 1;
  const max = step * steps;
  const ticks = Array.from({ length: steps + 1 }, (_, i) => Number((max - i * step).toFixed(2)));
  return { max, ticks };
}

function formatAxisNumber(v) {
  return Number.isInteger(v) ? String(v) : v.toLocaleString('vi-VN', { maximumFractionDigits: 1 });
}

function formatCompactVnd(v) {
  const n = Number(v || 0);
  if (n === 0) return '0';
  if (Math.abs(n) >= 1_000_000_000) {
    return `${(n / 1_000_000_000).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} tỷ`;
  }
  if (Math.abs(n) >= 1_000_000) {
    return `${Math.round(n / 1_000_000).toLocaleString('vi-VN')} tr`;
  }
  return n.toLocaleString('vi-VN');
}

function formatHours(v) {
  const n = Number(v || 0);
  return Number.isInteger(n) ? String(n) : n.toLocaleString('vi-VN', { maximumFractionDigits: 2 });
}

function buildYearList(extraYears = []) {
  const cur = new Date().getFullYear();
  const years = new Set();
  for (let y = cur + 3; y >= cur - 5; y -= 1) {
    years.add(y);
  }
  extraYears.forEach((y) => {
    const n = Number(y);
    if (Number.isInteger(n) && n >= 2000 && n <= 2100) years.add(n);
  });
  return [...years].sort((a, b) => b - a).map((y) => ({ value: String(y), label: `Năm ${y}` }));
}

function TooltipBubble({ align = 'center', children }) {
  const posClass =
    align === 'left'
      ? 'left-0'
      : align === 'right'
        ? 'right-0'
        : 'left-1/2 -translate-x-1/2';
  return (
    <div
      role="tooltip"
      className={`pointer-events-none absolute bottom-full mb-2 z-20 w-max max-w-[260px] rounded-xl bg-slate-900 px-3 py-2.5 text-xs text-white shadow-xl ring-1 ring-white/10 ${posClass}`}
    >
      {children}
    </div>
  );
}

function YAxisTicks({ ticks, formatTick, widthClass = 'w-10', heightClass = 'h-60' }) {
  const last = ticks.length - 1;
  return (
    <div className={`relative ${widthClass} shrink-0 pr-2 ${heightClass} text-[11px] text-slate-400 tabular-nums text-right select-none`}>
      {ticks.map((t, i) => (
        <span
          key={i}
          className="absolute right-2 -translate-y-1/2 leading-none"
          style={{ top: `${last > 0 ? (i / last) * 100 : 0}%` }}
        >
          {formatTick(t)}
        </span>
      ))}
    </div>
  );
}

function ChartGrid({ ticks, formatTick }) {
  const last = ticks.length - 1;
  return (
    <div className="pointer-events-none absolute inset-0">
      {ticks.map((t, idx) => (
        <div
          key={idx}
          className={`absolute inset-x-0 ${idx === last ? '' : 'border-t border-dashed border-slate-200/90'}`}
          style={{ top: `${last > 0 ? (idx / last) * 100 : 0}%` }}
          aria-label={formatTick(t)}
        />
      ))}
    </div>
  );
}

function HeadcountColumnChart({ data }) {
  const [hoveredDept, setHoveredDept] = useState(null);

  const presentTypes = useMemo(() => {
    const knownKeys = new Set(EMP_TYPE_SERIES.map((s) => s.key));
    const extras = [];
    data.forEach((d) => {
      d.typeEntries.forEach(([k]) => {
        if (!knownKeys.has(k) && !extras.some((x) => x.key === k)) {
          extras.push({ key: k, label: typeLabel(k), barClass: 'bg-slate-400', dotClass: 'bg-slate-400' });
        }
      });
    });
    return [...EMP_TYPE_SERIES, ...extras];
  }, [data]);

  const maxTypeValue = useMemo(() => {
    let m = 0;
    data.forEach((d) => {
      presentTypes.forEach((t) => {
        const v = d.typeMap.get(t.key) || 0;
        if (v > m) m = v;
      });
    });
    return m;
  }, [data, presentTypes]);

  const scale = useMemo(() => niceScale(maxTypeValue, 4), [maxTypeValue]);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-slate-600">
          {presentTypes.map((s) => (
            <span key={s.key} className="inline-flex items-center gap-1.5 font-medium">
              <span className={`w-2.5 h-2.5 rounded-sm ${s.dotClass}`} />
              {s.label}
            </span>
          ))}
        </div>
        <span className="text-xs text-slate-400">4 cột kề nhau theo từng loại nhân sự • rê chuột để xem chi tiết</span>
      </div>

      <div className="flex items-stretch pt-6">
        <YAxisTicks ticks={scale.ticks} formatTick={formatAxisNumber} widthClass="w-10" heightClass="h-60" />

        <div className="flex-1 min-w-0 overflow-x-auto pb-1">
          <div style={{ minWidth: `${Math.max(data.length * 130, 560)}px` }}>
            <div className="relative h-60 border-l border-b border-slate-300 bg-slate-50/40 rounded-tr-xl">
              <ChartGrid ticks={scale.ticks} formatTick={formatAxisNumber} />
              <div className="relative z-10 h-full flex items-end justify-around gap-3 px-3">
                {data.map((d, idx) => {
                  const align = idx === 0 ? 'left' : idx === data.length - 1 ? 'right' : 'center';
                  const isHovered = hoveredDept === d.department;
                  return (
                    <div
                      key={d.department}
                      tabIndex={0}
                      onMouseEnter={() => setHoveredDept(d.department)}
                      onMouseLeave={() => setHoveredDept(null)}
                      onFocus={() => setHoveredDept(d.department)}
                      onBlur={() => setHoveredDept(null)}
                      className={`relative flex-1 max-w-[140px] h-full flex items-end justify-center gap-1 sm:gap-1.5 px-1.5 rounded-t-lg outline-none cursor-default transition-colors ${
                        isHovered ? 'bg-white/90' : ''
                      }`}
                    >
                      {isHovered && (
                        <TooltipBubble align={align}>
                          <p className="font-bold text-white mb-1">{d.department}: {d.total} người</p>
                          <div className="space-y-0.5 text-slate-200 border-t border-white/15 pt-1">
                            {d.typeEntries.map(([k, v]) => (
                              <div key={k} className="flex justify-between gap-4">
                                <span>{typeLabel(k)}</span>
                                <span className="font-semibold tabular-nums">{v}</span>
                              </div>
                            ))}
                          </div>
                          {d.statusEntries.length > 0 && (
                            <div className="space-y-0.5 text-slate-300 border-t border-white/15 mt-1 pt-1">
                              {d.statusEntries.map(([k, v]) => (
                                <div key={k} className="flex justify-between gap-4">
                                  <span>{EMP_STATUS_LABELS[k] || k}</span>
                                  <span className="font-semibold tabular-nums">{v}</span>
                                </div>
                              ))}
                            </div>
                          )}
                        </TooltipBubble>
                      )}

                      {presentTypes.map((series) => {
                        const cnt = d.typeMap.get(series.key) || 0;
                        const pct = scale.max > 0 ? Math.min(100, (cnt / scale.max) * 100) : 0;
                        return (
                          <div key={series.key} className="flex flex-col items-center justify-end h-full w-3.5 sm:w-4">
                            <span
                              className={`mb-1 text-[10px] font-bold tabular-nums leading-none ${
                                cnt > 0 ? 'text-slate-700' : 'text-slate-300'
                              }`}
                            >
                              {cnt}
                            </span>
                            <div
                              className={`w-full rounded-t transition-all ${
                                cnt > 0 ? `${series.barClass} ${isHovered ? 'brightness-110' : ''}` : 'bg-slate-200'
                              }`}
                              style={{ height: cnt > 0 ? `${Math.max(pct, 3)}%` : '2px' }}
                            />
                          </div>
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="flex justify-around gap-3 px-3 pt-2.5">
              {data.map((d) => (
                <div
                  key={d.department}
                  className="flex-1 max-w-[140px] text-center leading-snug break-words"
                  title={d.department}
                >
                  <div className="text-xs font-semibold text-slate-700">{d.department}</div>
                  <div className="text-[11px] font-bold text-blue-600 tabular-nums mt-0.5">Tổng: {d.total}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <div className="pt-4 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-2.5">
        {data.map((d) => (
          <div
            key={d.department}
            className="rounded-xl border border-slate-200/80 bg-slate-50/70 px-3 py-2 text-xs"
          >
            <p className="font-bold text-slate-800 truncate" title={d.department}>{d.department}</p>
            <p className="text-blue-700 font-extrabold tabular-nums mt-0.5">{d.total} nhân sự</p>
            {d.statuses.length > 0 && (
              <p className="text-[11px] text-slate-500 truncate mt-0.5" title={d.statuses.join(' • ')}>
                {d.statuses.join(' • ')}
              </p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

function OtColumnChart({ rows, year }) {
  const [hoveredMonth, setHoveredMonth] = useState(null);
  const byMonth = useMemo(() => {
    const map = new Map();
    rows.forEach((r) => map.set(Number(r.month), Number(r.hours || 0)));
    return MONTHS_1_TO_12.map((m) => ({ month: m, hours: map.get(m) ?? 0 }));
  }, [rows]);

  const maxHours = Math.max(0, ...byMonth.map((m) => m.hours));
  const totalHours = useMemo(() => byMonth.reduce((s, m) => s + m.hours, 0), [byMonth]);
  const peakMonth = useMemo(
    () => byMonth.reduce((a, b) => (b.hours > a.hours ? b : a), byMonth[0]),
    [byMonth]
  );
  const avgHours = totalHours / 12;
  const scale = useMemo(() => niceScale(maxHours, 4), [maxHours]);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2.5 text-xs">
        <span className="rounded-lg bg-amber-50 border border-amber-100 px-3 py-1.5 font-semibold text-amber-800 tabular-nums">
          Cao điểm: Tháng {peakMonth.month} ({formatHours(peakMonth.hours)} giờ)
        </span>
        <span className="rounded-lg bg-slate-100 border border-slate-200/80 px-3 py-1.5 font-semibold text-slate-700 tabular-nums">
          Trung bình: {formatHours(avgHours)} giờ / tháng
        </span>
      </div>

      <div className="flex items-stretch pt-6">
        <YAxisTicks ticks={scale.ticks} formatTick={(v) => `${formatAxisNumber(v)}g`} widthClass="w-11" heightClass="h-56" />

        <div className="flex-1 min-w-0 overflow-x-auto pb-1">
          <div style={{ minWidth: '520px' }}>
            <div className="relative h-56 border-l border-b border-slate-300 bg-slate-50/40 rounded-tr-xl">
              <ChartGrid ticks={scale.ticks} formatTick={(v) => `${formatAxisNumber(v)} giờ`} />
              <div className="relative z-10 h-full flex items-end justify-around gap-1.5 px-2">
                {byMonth.map((m, idx) => {
                  const pct = scale.max > 0 ? Math.min(100, (m.hours / scale.max) * 100) : 0;
                  const align = idx <= 1 ? 'left' : idx >= 10 ? 'right' : 'center';
                  const isHovered = hoveredMonth === m.month;
                  return (
                    <div
                      key={m.month}
                      tabIndex={0}
                      onMouseEnter={() => setHoveredMonth(m.month)}
                      onMouseLeave={() => setHoveredMonth(null)}
                      onFocus={() => setHoveredMonth(m.month)}
                      onBlur={() => setHoveredMonth(null)}
                      className="relative flex-1 max-w-[44px] h-full flex flex-col items-center justify-end outline-none cursor-default"
                    >
                      {isHovered && (
                        <TooltipBubble align={align}>
                          <p className="font-bold text-white">Tháng {m.month}/{year}</p>
                          <p className="text-amber-300 font-semibold mt-0.5 tabular-nums">{formatHours(m.hours)} giờ OT đã xác nhận</p>
                        </TooltipBubble>
                      )}

                      <span className={`mb-1 text-[11px] font-bold tabular-nums leading-none ${m.hours > 0 ? 'text-slate-800' : 'text-slate-300'}`}>
                        {formatHours(m.hours)}
                      </span>
                      <div
                        className={`w-6 sm:w-7 rounded-t-md transition-all ${
                          m.hours > 0
                            ? isHovered
                              ? 'bg-amber-600 ring-2 ring-amber-400/40'
                              : 'bg-amber-500'
                            : 'bg-slate-200'
                        }`}
                        style={{ height: m.hours > 0 ? `${Math.max(pct, 3)}%` : '2px' }}
                      />
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="flex justify-around gap-1.5 px-2 pt-2">
              {byMonth.map((m) => (
                <div key={m.month} className="flex-1 max-w-[44px] text-center text-xs font-semibold text-slate-600 tabular-nums">
                  T{m.month}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function PayrollFundGroupedChart({ rows }) {
  const [hoveredPeriodId, setHoveredPeriodId] = useState(null);
  // Sắp xếp tăng dần theo thời gian trên biểu đồ để theo dõi xu hướng quỹ lương từ trái sang phải.
  const chartRows = useMemo(
    () => [...rows].sort((a, b) => (Number(a.year) - Number(b.year)) || (Number(a.month) - Number(b.month))),
    [rows]
  );

  const maxAmount = useMemo(
    () => Math.max(0, ...chartRows.map((r) => Math.max(Number(r.totalGross || 0), Number(r.totalNet || 0), Number(r.totalPayable || 0)))),
    [chartRows]
  );
  const scale = useMemo(() => niceScale(maxAmount, 4), [maxAmount]);

  return (
    <div className="rounded-xl border border-slate-200/80 bg-slate-50/50 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-2">
        <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 text-xs text-slate-600">
          {FUND_SERIES.map((s) => (
            <span key={s.key} className="inline-flex items-center gap-1.5 font-medium">
              <span className={`w-2.5 h-2.5 rounded-sm ${s.dotClass}`} />
              {s.label}
            </span>
          ))}
        </div>
        <span className="text-xs text-slate-400">So sánh theo kỳ (rê chuột để xem số tiền chi tiết)</span>
      </div>

      <div className="flex items-stretch pt-4">
        <YAxisTicks ticks={scale.ticks} formatTick={formatCompactVnd} widthClass="w-16" heightClass="h-60" />

        <div className="flex-1 min-w-0 overflow-x-auto pb-1">
          <div style={{ minWidth: `${Math.max(chartRows.length * 76, 420)}px` }}>
            <div className="relative h-60 border-l border-b border-slate-300">
              <ChartGrid ticks={scale.ticks} formatTick={formatCompactVnd} />
              <div className="relative z-10 h-full flex items-end justify-around gap-2 px-2">
                {chartRows.map((r, idx) => {
                  const label = `${String(r.month).padStart(2, '0')}/${r.year}`;
                  const align = idx === 0 ? 'left' : idx === chartRows.length - 1 ? 'right' : 'center';
                  const isHovered = hoveredPeriodId === r.periodId;
                  return (
                    <div
                      key={r.periodId}
                      tabIndex={0}
                      onMouseEnter={() => setHoveredPeriodId(r.periodId)}
                      onMouseLeave={() => setHoveredPeriodId(null)}
                      onFocus={() => setHoveredPeriodId(r.periodId)}
                      onBlur={() => setHoveredPeriodId(null)}
                      className={`relative flex-1 max-w-[92px] h-full flex items-end justify-center gap-1 px-1 rounded-t-lg outline-none cursor-default transition-colors ${
                        isHovered ? 'bg-blue-50/70' : ''
                      }`}
                    >
                      {isHovered && (
                        <TooltipBubble align={align}>
                          <p className="font-bold text-white mb-1">
                            Kỳ {label} ({r.employeeCount} NV)
                          </p>
                          <div className="space-y-0.5 text-slate-200 border-t border-white/15 pt-1 tabular-nums">
                            <div className="flex justify-between gap-4">
                              <span className="text-blue-300">Tổng Gross:</span>
                              <span className="font-semibold">{formatCurrency(r.totalGross)}</span>
                            </div>
                            <div className="flex justify-between gap-4">
                              <span>BH NLĐ:</span>
                              <span>{formatCurrency(r.totalInsurance)}</span>
                            </div>
                            <div className="flex justify-between gap-4">
                              <span>Thuế TNCN:</span>
                              <span>{formatCurrency(r.totalPit)}</span>
                            </div>
                            <div className="flex justify-between gap-4 border-t border-white/10 pt-0.5">
                              <span className="text-emerald-300">Net (sau nghĩa vụ):</span>
                              <span className="font-semibold">{formatCurrency(r.totalNet)}</span>
                            </div>
                            <div className="flex justify-between gap-4">
                              <span className="text-orange-300">Thực chi (Payable):</span>
                              <span className="font-bold text-white">{formatCurrency(r.totalPayable)}</span>
                            </div>
                          </div>
                        </TooltipBubble>
                      )}

                      {FUND_SERIES.map((s) => {
                        const val = Number(r[s.key] || 0);
                        const pct = scale.max > 0 ? Math.min(100, (val / scale.max) * 100) : 0;
                        return (
                          <div
                            key={s.key}
                            className={`w-3.5 sm:w-4 rounded-t transition-all ${s.barClass} ${isHovered ? 'brightness-110' : ''}`}
                            style={{ height: `${Math.max(pct, val > 0 ? 2 : 0)}%` }}
                          />
                        );
                      })}
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="flex justify-around gap-2 px-2 pt-2">
              {chartRows.map((r) => (
                <div key={r.periodId} className="flex-1 max-w-[92px] text-center text-xs font-semibold text-slate-700 tabular-nums">
                  {String(r.month).padStart(2, '0')}/{r.year}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export function ReportsPage() {
  const { isHR, isAdmin, isManager, hasGrant } = useAuth();
  const canSeeHeadcount = isHR || isAdmin || isManager;
  const canSeeOt = isHR || isManager;
  const canSeePayrollFund = hasGrant('VIEW');

  const currentYear = String(new Date().getFullYear());
  const [activeTab, setActiveTab] = useState('ALL');
  const [fundView, setFundView] = useState('BOTH');
  const [empStatus, setEmpStatus] = useState('');
  const [year, setYear] = useState(currentYear);
  const [fundYear, setFundYear] = useState('');
  const [fundStatus, setFundStatus] = useState('');
  const [exportErr, setExportErr] = useState({ headcount: '', ot: '', fund: '' });
  const [exporting, setExporting] = useState({ headcount: false, ot: false, fund: false });

  const headcountParams = empStatus ? { status: empStatus } : undefined;
  const headcount = useAsync(() => reportApi.headcount(headcountParams), [empStatus], { enabled: canSeeHeadcount });
  const ot = useAsync(() => reportApi.otHours(year), [year], { enabled: canSeeOt });
  const fundParams = useMemo(() => {
    const p = {};
    if (fundYear && String(fundYear).trim()) p.year = String(fundYear).trim();
    if (fundStatus) p.status = fundStatus;
    return Object.keys(p).length ? p : undefined;
  }, [fundYear, fundStatus]);
  const payrollFund = useAsync(() => reportApi.payrollFund(fundParams), [fundParams], { enabled: canSeePayrollFund });

  const runExport = async (key, fn) => {
    setExportErr((s) => ({ ...s, [key]: '' }));
    setExporting((s) => ({ ...s, [key]: true }));
    try {
      await fn();
    } catch (err) {
      setExportErr((s) => ({ ...s, [key]: err?.message || 'Không thể xuất báo cáo' }));
    } finally {
      setExporting((s) => ({ ...s, [key]: false }));
    }
  };

  const byDept = useMemo(() => {
    const map = new Map();
    (headcount.data || []).forEach((r) => {
      const cur = map.get(r.department) || { department: r.department, total: 0, typeMap: new Map(), statusMap: new Map() };
      const cnt = Number(r.headcount);
      cur.total += cnt;
      cur.typeMap.set(r.employeeType, (cur.typeMap.get(r.employeeType) || 0) + cnt);
      if (r.status) {
        cur.statusMap.set(r.status, (cur.statusMap.get(r.status) || 0) + cnt);
      }
      map.set(r.department, cur);
    });
    return [...map.values()]
      .map((d) => ({
        department: d.department,
        total: d.total,
        typeMap: d.typeMap,
        typeEntries: [...d.typeMap.entries()],
        statusEntries: [...d.statusMap.entries()],
        statuses: [...d.statusMap.entries()].map(([k, v]) => `${EMP_STATUS_LABELS[k] || k}: ${v}`),
      }))
      .sort((a, b) => b.total - a.total);
  }, [headcount.data]);

  const totalHeadcount = useMemo(() => byDept.reduce((s, d) => s + d.total, 0), [byDept]);
  const otRows = ot.data || [];
  const totalOt = useMemo(() => otRows.reduce((s, r) => s + Number(r.hours || 0), 0), [otRows]);

  const fundRows = payrollFund.data || [];
  const fundTotals = useMemo(() => fundRows.reduce((acc, r) => ({
    gross: acc.gross + Number(r.totalGross || 0),
    insurance: acc.insurance + Number(r.totalInsurance || 0),
    pit: acc.pit + Number(r.totalPit || 0),
    net: acc.net + Number(r.totalNet || 0),
    payable: acc.payable + Number(r.totalPayable || 0),
  }), { gross: 0, insurance: 0, pit: 0, net: 0, payable: 0 }), [fundRows]);

  const otYearOptions = useMemo(() => buildYearList([year]), [year]);
  const fundYearOptions = useMemo(
    () => buildYearList([fundYear, ...fundRows.map((r) => r.year)]),
    [fundYear, fundRows]
  );

  const reportTabs = useMemo(() => (
    [
      { id: 'ALL', label: 'Tất cả báo cáo', show: true },
      { id: 'HEADCOUNT', label: 'Nhân sự', show: canSeeHeadcount },
      { id: 'OT', label: 'Giờ OT', show: canSeeOt },
      { id: 'FUND', label: 'Quỹ lương', show: canSeePayrollFund },
    ].filter((t) => t.show)
  ), [canSeeHeadcount, canSeeOt, canSeePayrollFund]);

  const effectiveTab = reportTabs.some((t) => t.id === activeTab) ? activeTab : 'ALL';

  const kpiCards = [
    canSeeHeadcount && {
      label: 'Tổng nhân sự phạm vi',
      value: headcount.loading ? '—' : `${totalHeadcount} người`,
      sub: headcount.loading ? 'Đang tải...' : `${byDept.length} phòng ban`,
      accent: 'border-l-blue-600',
    },
    canSeeOt && {
      label: `Tổng giờ OT năm ${year}`,
      value: ot.loading ? '—' : `${formatHours(totalOt)} giờ`,
      sub: 'Chỉ tính OT đã xác nhận',
      accent: 'border-l-amber-500',
    },
    canSeePayrollFund && {
      label: `Tổng Thực chi (${fundRows.length} kỳ)`,
      value: payrollFund.loading ? '—' : formatCompactVnd(fundTotals.payable),
      sub: payrollFund.loading ? 'Đang tải...' : formatCurrency(fundTotals.payable),
      accent: 'border-l-orange-500',
    },
  ].filter(Boolean);

  return (
    <div className="space-y-6">
      {/* Page Header + Segmented Report Tabs + Executive KPI Strip */}
      <section className="rounded-2xl border border-slate-200/90 bg-white p-5 sm:p-6 shadow-sm">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div>
            <p className="text-xs font-semibold text-blue-600 mb-1">Phân tích &amp; Báo cáo</p>
            <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Báo cáo tổng hợp</h1>
            <p className="text-sm text-slate-500 mt-1">
              {canSeePayrollFund
                ? 'Báo cáo nhân sự, OT theo phạm vi và quỹ lương tổng hợp theo quyền grant VIEW.'
                : 'Báo cáo tổng hợp nhân sự và vận hành, không bao gồm số liệu lương.'}
            </p>
          </div>

          {reportTabs.length > 1 && (
            <div
              role="tablist"
              aria-label="Lọc nhóm báo cáo"
              className="inline-flex flex-wrap items-center gap-1 rounded-xl bg-slate-100 p-1 border border-slate-200/70 self-start lg:self-auto"
            >
              {reportTabs.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  role="tab"
                  aria-selected={effectiveTab === t.id}
                  onClick={() => setActiveTab(t.id)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                    effectiveTab === t.id
                      ? 'bg-white text-blue-700 shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>
          )}
        </div>

        {kpiCards.length > 0 && (
          <div className="mt-5 pt-5 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
            {kpiCards.map((k) => (
              <div
                key={k.label}
                className={`rounded-xl border border-slate-200/80 border-l-4 ${k.accent} bg-slate-50/60 px-4 py-3`}
              >
                <p className="text-xs font-semibold text-slate-500">{k.label}</p>
                <p className="mt-1 text-xl font-extrabold text-slate-900 tabular-nums">{k.value}</p>
                <p className="mt-0.5 text-[11px] text-slate-500 tabular-nums">{k.sub}</p>
              </div>
            ))}
          </div>
        )}
      </section>

      {canSeeHeadcount && (effectiveTab === 'ALL' || effectiveTab === 'HEADCOUNT') && (
        <section className={cardBox}>
          <div className="p-5 sm:p-6 border-b border-slate-100 flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="p-2.5 rounded-xl bg-blue-50 text-blue-600 shrink-0">
                <Users className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-900">Nhân sự theo phòng ban &amp; trạng thái</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  {isHR || isAdmin ? 'Toàn công ty' : 'Nhân viên thuộc phạm vi quản lý trực tiếp'}
                  {!empStatus ? ' • mặc định chưa tính nhân viên đã nghỉ việc.' : '.'}
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-end gap-2.5">
              <div className="w-56">
                <SelectField
                  id="rp-emp-status"
                  label="Trạng thái nhân viên"
                  placeholder="Đang công tác (trừ nghỉ việc)"
                  options={HEADCOUNT_STATUS_OPTIONS}
                  value={empStatus}
                  onChange={(e) => setEmpStatus(e.target.value)}
                />
              </div>
              <Button
                variant="outline"
                icon={Download}
                isLoading={exporting.headcount}
                className="h-[42px] bg-white shrink-0 whitespace-nowrap"
                onClick={() => runExport('headcount', () => reportApi.exportHeadcount(headcountParams))}
              >
                Xuất CSV
              </Button>
            </div>
          </div>

          <div className="p-5 sm:p-6">
            {exportErr.headcount && <div className="mb-4"><ErrorMessage message={exportErr.headcount} /></div>}
            {headcount.loading ? <Loading /> : headcount.error ? <ErrorMessage message={headcount.error.message} onRetry={headcount.reload} />
              : !byDept.length ? <EmptyState title="Chưa có dữ liệu nhân sự phù hợp" /> : (
                <HeadcountColumnChart data={byDept} />
              )}
          </div>
        </section>
      )}

      {canSeeOt && (effectiveTab === 'ALL' || effectiveTab === 'OT') && (
        <section className={cardBox}>
          <div className="p-5 sm:p-6 border-b border-slate-100 flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="p-2.5 rounded-xl bg-amber-50 text-amber-600 shrink-0">
                <Clock className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-900">Giờ OT thực tế theo tháng</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  {isHR ? 'Toàn công ty' : 'Nhân viên thuộc team của bạn'} • hiển thị 12 tháng, chỉ tính giờ OT đã xác nhận.
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-end gap-2.5">
              <div className="w-40">
                <SelectField
                  id="rp-year"
                  label="Năm báo cáo"
                  placeholder=""
                  options={otYearOptions}
                  value={year}
                  onChange={(e) => setYear(e.target.value)}
                />
              </div>
              <Button
                variant="outline"
                icon={Download}
                isLoading={exporting.ot}
                className="h-[42px] bg-white shrink-0 whitespace-nowrap"
                onClick={() => runExport('ot', () => reportApi.exportOtHours(year))}
              >
                Xuất CSV
              </Button>
            </div>
          </div>

          <div className="p-5 sm:p-6">
            {exportErr.ot && <div className="mb-4"><ErrorMessage message={exportErr.ot} /></div>}
            {ot.loading ? <Loading /> : ot.error ? <ErrorMessage message={ot.error.message} onRetry={ot.reload} />
              : (
                <>
                  <OtColumnChart rows={otRows} year={year} />
                  {!otRows.length && (
                    <p className="mt-3 text-xs text-slate-500 text-center">
                      Chưa phát sinh giờ OT đã xác nhận nào trong năm {year}.
                    </p>
                  )}
                </>
              )}
          </div>
        </section>
      )}

      {canSeePayrollFund && (effectiveTab === 'ALL' || effectiveTab === 'FUND') && (
        <section className={cardBox}>
          <div className="p-5 sm:p-6 border-b border-slate-100 flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600 shrink-0">
                <Banknote className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-slate-900">Báo cáo quỹ lương theo kỳ (Gross / Net / Thực chi)</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Tổng hợp từ kết quả lương đã lưu (CALCULATED, APPROVED, LOCKED). Thực chi = Net sau nghĩa vụ − Thu hồi tạm ứng + Hoàn chi.
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-end gap-2.5">
              <div className="w-40">
                <SelectField
                  id="rp-fund-year"
                  label="Năm"
                  placeholder="Tất cả các năm"
                  options={fundYearOptions}
                  value={fundYear}
                  onChange={(e) => setFundYear(e.target.value)}
                />
              </div>
              <div className="w-48">
                <SelectField
                  id="rp-fund-status"
                  label="Trạng thái kỳ"
                  placeholder="Tất cả kỳ đã tính"
                  options={FUND_STATUS_OPTIONS}
                  value={fundStatus}
                  onChange={(e) => setFundStatus(e.target.value)}
                />
              </div>
              <div className="inline-flex h-[42px] items-center rounded-xl bg-slate-100 p-1 border border-slate-200/80">
                {[
                  { id: 'BOTH', label: 'Cả hai' },
                  { id: 'CHART', label: 'Biểu đồ' },
                  { id: 'TABLE', label: 'Bảng' },
                ].map((v) => (
                  <button
                    key={v.id}
                    type="button"
                    onClick={() => setFundView(v.id)}
                    className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                      fundView === v.id ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
                    }`}
                  >
                    {v.label}
                  </button>
                ))}
              </div>
              <Button
                variant="outline"
                icon={Download}
                isLoading={exporting.fund}
                className="h-[42px] bg-white shrink-0 whitespace-nowrap"
                onClick={() => runExport('fund', () => reportApi.exportPayrollFund(fundParams))}
              >
                Xuất CSV
              </Button>
            </div>
          </div>

          <div className="p-5 sm:p-6 space-y-5">
            {exportErr.fund && <ErrorMessage message={exportErr.fund} />}
            {payrollFund.loading ? <Loading /> : payrollFund.error ? <ErrorMessage message={payrollFund.error.message} onRetry={payrollFund.reload} />
              : !fundRows.length ? <EmptyState title="Không có dữ liệu quỹ lương phù hợp" /> : (
                <>
                  {/* 3 màu phân biệt rõ ràng: Blue / Emerald / Orange */}
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="rounded-xl border border-blue-200/80 bg-blue-50/50 px-4 py-3.5">
                      <p className="text-xs font-semibold text-blue-700">Tổng Gross ({fundRows.length} kỳ)</p>
                      <p className="mt-1 text-lg font-extrabold text-blue-900 tabular-nums">{formatCurrency(fundTotals.gross)}</p>
                    </div>
                    <div className="rounded-xl border border-emerald-200/80 bg-emerald-50/50 px-4 py-3.5">
                      <p className="text-xs font-semibold text-emerald-700">Tổng Net (sau nghĩa vụ)</p>
                      <p className="mt-1 text-lg font-extrabold text-emerald-800 tabular-nums">{formatCurrency(fundTotals.net)}</p>
                    </div>
                    <div className="rounded-xl border border-orange-200/80 bg-orange-50/60 px-4 py-3.5">
                      <p className="text-xs font-semibold text-orange-700">Tổng Thực chi (Payable)</p>
                      <p className="mt-1 text-lg font-extrabold text-orange-800 tabular-nums">{formatCurrency(fundTotals.payable)}</p>
                    </div>
                  </div>

                  {fundView !== 'TABLE' && <PayrollFundGroupedChart rows={fundRows} />}

                  {fundView !== 'CHART' && (
                    <div className="overflow-x-auto rounded-xl border border-slate-200/80">
                      <table className="w-full text-left text-sm text-slate-600">
                        <thead className="bg-slate-50 text-xs uppercase font-bold text-slate-500 border-b border-slate-200">
                          <tr>
                            <th className="px-3 py-3">Kỳ</th>
                            <th className="px-3 py-3">Trạng thái</th>
                            <th className="px-3 py-3 text-right">Nhân sự</th>
                            <th className="px-3 py-3 text-right">Tổng Gross</th>
                            <th className="px-3 py-3 text-right">BH NLĐ</th>
                            <th className="px-3 py-3 text-right">Thuế TNCN</th>
                            <th className="px-3 py-3 text-right">Net (sau nghĩa vụ)</th>
                            <th className="px-3 py-3 text-right">Thực chi (Payable)</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                          {fundRows.map((r) => (
                            <tr key={r.periodId} className="hover:bg-slate-50/70">
                              <td className="px-3 py-3 font-bold text-slate-900 whitespace-nowrap">{String(r.month).padStart(2, '0')}/{r.year}</td>
                              <td className="px-3 py-3 whitespace-nowrap">
                                <StatusBadge status={r.status} label={r.status === 'CALCULATED' ? 'Tạm tính' : undefined} />
                                {r.status === 'LOCKED' && <StatusBadge status={r.paymentStatus} className="ml-1.5" />}
                              </td>
                              <td className="px-3 py-3 text-right tabular-nums">{r.employeeCount}</td>
                              <td className="px-3 py-3 text-right tabular-nums">{formatCurrency(r.totalGross)}</td>
                              <td className="px-3 py-3 text-right tabular-nums">{formatCurrency(r.totalInsurance)}</td>
                              <td className="px-3 py-3 text-right tabular-nums">{formatCurrency(r.totalPit)}</td>
                              <td className="px-3 py-3 text-right font-semibold text-emerald-700 tabular-nums">{formatCurrency(r.totalNet)}</td>
                              <td className="px-3 py-3 text-right font-bold text-orange-700 tabular-nums">{formatCurrency(r.totalPayable)}</td>
                            </tr>
                          ))}
                        </tbody>
                        <tfoot className="bg-slate-50 border-t border-slate-200 font-bold text-slate-900">
                          <tr>
                            <td className="px-3 py-3" colSpan={3}>Tổng ({fundRows.length} kỳ)</td>
                            <td className="px-3 py-3 text-right tabular-nums">{formatCurrency(fundTotals.gross)}</td>
                            <td className="px-3 py-3 text-right tabular-nums">{formatCurrency(fundTotals.insurance)}</td>
                            <td className="px-3 py-3 text-right tabular-nums">{formatCurrency(fundTotals.pit)}</td>
                            <td className="px-3 py-3 text-right text-emerald-700 tabular-nums">{formatCurrency(fundTotals.net)}</td>
                            <td className="px-3 py-3 text-right text-orange-700 tabular-nums">{formatCurrency(fundTotals.payable)}</td>
                          </tr>
                        </tfoot>
                      </table>
                    </div>
                  )}
                </>
              )}
          </div>
        </section>
      )}
    </div>
  );
}

export default ReportsPage;
