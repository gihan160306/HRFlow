import React from 'react';
import hqLogo from '../../assets/hq-logo.webp';
import { formatDate, formatDateTime } from '../../utils/formatDate';
import { numberToVietnameseWords } from '../../utils/numberToWords';

const COMPANY = {
  name: 'CÔNG TY TNHH TRUYỀN THÔNG HNQ',
  brand: 'HNQ Media',
  address: 'Thành phố Hồ Chí Minh, Việt Nam',
  slogan: 'Sáng tạo – Kết nối – Phát triển',
};

const INCOME_CATS = ['INCOME', 'ALLOWANCE', 'OVERTIME'];

const money = (v) => {
  if (v === null || v === undefined || v === '') return '-';
  const n = Math.round(Number(v));
  if (Number.isNaN(n)) return '-';
  return `${n < 0 ? '-' : ''}${Math.abs(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.')}`;
};
const num = (v) => Number(v || 0);
const hours = (v) => (v === null || v === undefined ? '-' : Number(v).toLocaleString('vi-VN', { maximumFractionDigits: 2 }));

/** Một dòng trong bảng khoản mục. */
function Row({ label, value, hint, negative, strong }) {
  return (
    <tr className={strong ? 'font-bold text-slate-900' : 'text-slate-700'}>
      <td className="py-2 pr-3 align-top">
        {label}
        {hint && <span className="block text-[11px] font-normal text-slate-400">{hint}</span>}
      </td>
      <td className={`py-2 text-right align-top font-mono tabular-nums whitespace-nowrap ${negative && num(value) !== 0 ? 'text-rose-600' : ''}`}>
        {negative && num(value) !== 0 ? `(${money(value)})` : money(value)}
      </td>
    </tr>
  );
}

function Section({ index, title, accent, children, total, totalLabel, negative }) {
  return (
    <div className="ps-avoid-break rounded-xl border border-slate-200 overflow-hidden">
      <div className={`flex items-center gap-2 px-4 py-2.5 ${accent}`}>
        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-white/25 text-[11px] font-black">{index}</span>
        <h4 className="text-xs font-bold uppercase tracking-wider">{title}</h4>
      </div>
      <table className="w-full text-[13px]">
        <tbody className="divide-y divide-slate-100 [&>tr>td]:px-4">{children}</tbody>
        {totalLabel && (
          <tfoot>
            <tr className="bg-slate-50 border-t border-slate-200 font-bold text-slate-900">
              <td className="px-4 py-2.5">{totalLabel}</td>
              <td className={`px-4 py-2.5 text-right font-mono tabular-nums ${negative ? 'text-rose-700' : ''}`}>
                {negative && num(total) !== 0 ? `(${money(total)})` : money(total)}
              </td>
            </tr>
          </tfoot>
        )}
      </table>
    </div>
  );
}

export const PayslipDocument = React.forwardRef(function PayslipDocument({ slip, id = 'printable-payslip' }, ref) {
  const l = slip.line || {};
  const items = slip.items || [];
  const period = `${String(slip.month).padStart(2, '0')}/${slip.year}`;
  const slipNo = `PL-${slip.year}${String(slip.month).padStart(2, '0')}-${slip.employeeCode}`;

  // Thu nhập: ưu tiên chi tiết khoản mục đã chốt; nếu không có thì dựng từ tổng của dòng lương.
  let incomeRows = items
    .filter((i) => INCOME_CATS.includes(i.category) || (i.category === 'ADJUSTMENT' && i.code === 'ADJ_GROSS'))
    .map((i) => ({ label: i.name, value: i.amount }));
  if (!incomeRows.length) {
    incomeRows = [
      { label: 'Lương theo ngày công', value: l.salaryAttendance },
      { label: 'Lương làm thêm giờ', value: l.otPay },
      { label: 'Phụ cấp', value: l.allowances },
      { label: 'Hoa hồng', value: l.commission },
      { label: 'Thưởng KPI', value: l.kpiBonus },
      { label: 'Thưởng khác', value: l.bonuses },
      { label: 'Khoản cộng khác', value: l.otherAdditions },
      { label: 'Điều chỉnh Gross', value: l.grossAdjustment },
    ].filter((r) => num(r.value) !== 0);
  }

  const totalDeduction = num(l.insuranceTotal) + num(l.pit) + num(l.legalDeductions) - num(l.netAdjustment);

  return (
    <article ref={ref} id={id} className="payslip-doc relative mx-auto w-full max-w-[820px] overflow-hidden rounded-2xl bg-white text-slate-800 ring-1 ring-slate-200">

      {/* Logo chìm */}
      <img src={hqLogo} alt="" aria-hidden="true"
        className="pointer-events-none select-none absolute left-1/2 top-[52%] w-[520px] -translate-x-1/2 -translate-y-1/2 opacity-[0.035]" />

      <div className="relative px-6 py-7 sm:px-10 sm:py-9">
        {/* Header */}
        <header className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex items-center gap-4">
            {/* Ảnh logo vuông nền trắng nhiều lề: cắt phần chữ HNQ bằng khung overflow (html2canvas không hỗ trợ object-fit) */}
            <div className="relative h-14 w-20 shrink-0 overflow-hidden">
              <img src={hqLogo} alt="HNQ Media" className="absolute left-0 top-1/2 h-20 w-20 max-w-none -translate-y-1/2" />
            </div>
            <div>
              <p className="text-[15px] font-black leading-tight tracking-tight text-slate-900">{COMPANY.name}</p>
              <p className="mt-0.5 text-xs font-semibold text-blue-600">{COMPANY.brand} · {COMPANY.slogan}</p>
              <p className="text-xs text-slate-500">{COMPANY.address}</p>
            </div>
          </div>
          <div className="sm:text-right">
            <h2 className="text-2xl font-black uppercase tracking-tight text-slate-900">Phiếu lương</h2>
            <p className="mt-0.5 text-sm font-bold text-blue-700">Kỳ lương tháng {period}</p>
            <p className="text-xs text-slate-500">
              {l.startDate && l.endDate ? `Từ ${formatDate(l.startDate)} đến ${formatDate(l.endDate)}` : null}
            </p>
            <p className="mt-1 font-mono text-[11px] text-slate-400">Số: {slipNo}</p>
          </div>
        </header>

        {/* Thông tin nhân viên */}
        <section className="mt-7 grid grid-cols-2 gap-x-6 gap-y-3 rounded-xl border border-slate-200 bg-slate-50/80 px-5 py-4 text-[13px] sm:grid-cols-3">
          <Info label="Họ và tên" value={slip.employeeName} strong />
          <Info label="Mã nhân viên" value={slip.employeeCode} mono />
          <Info label="Phòng ban" value={l.departmentName} />
          <Info label="Chức danh" value={l.jobTitleName} />
          <Info label="Giờ công chuẩn" value={`${hours(l.hStd)} giờ`} />
          <Info label="Giờ công hưởng lương" value={`${hours(l.hPaid)} giờ`} />
        </section>

        {/* Bảng khoản mục */}
        <div className="mt-6 grid gap-5 md:grid-cols-2 print:grid-cols-2">
          <Section index="I" title="Thu nhập" accent="bg-blue-600 text-white" totalLabel="Tổng thu nhập (Gross)" total={l.gross ?? slip.gross}>
            <Row label="Lương cơ bản theo hợp đồng" hint="Tham chiếu, không cộng vào tổng" value={l.baseSalary} />
            {incomeRows.map((r, i) => <Row key={i} label={r.label} value={r.value} />)}
          </Section>

          <Section index="II" title="Khấu trừ" accent="bg-rose-600 text-white" totalLabel="Tổng khấu trừ" total={totalDeduction} negative>
            <Row label="Bảo hiểm xã hội (BHXH)" value={l.bhxh} negative />
            <Row label="Bảo hiểm y tế (BHYT)" value={l.bhyt} negative />
            <Row label="Bảo hiểm thất nghiệp (BHTN)" value={l.bhtn} negative />
            <Row label="Thuế thu nhập cá nhân" value={l.pit} negative />
            {num(l.legalDeductions) !== 0 && <Row label="Khấu trừ khác có căn cứ" value={l.legalDeductions} negative />}
            {num(l.netAdjustment) !== 0 && <Row label="Điều chỉnh sau thuế" value={l.netAdjustment} />}
          </Section>
        </div>

        {/* Thuế TNCN */}
        <section className="ps-avoid-break mt-5 rounded-xl border border-dashed border-slate-300 px-5 py-4">
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">III. Căn cứ tính thuế TNCN</h4>
          <dl className="mt-3 grid grid-cols-2 gap-x-6 gap-y-2 text-[13px] sm:grid-cols-4">
            <Stat label="Thu nhập không chịu thuế" value={num(l.nonTaxableIncome) + num(l.exemptIncome)} />
            <Stat label="Thu nhập chịu thuế" value={l.taxableIncome} />
            <Stat label="Giảm trừ gia cảnh" value={l.familyRelief} />
            <Stat label="Thu nhập tính thuế" value={l.assessableIncome} />
          </dl>
        </section>

        {/* Thực nhận */}
        <section className="ps-avoid-break mt-5 overflow-hidden rounded-xl border border-slate-200">
          <div className="grid divide-y divide-slate-200 text-[13px] sm:grid-cols-3 sm:divide-x sm:divide-y-0">
            <Stat boxed label="Thực nhận sau nghĩa vụ (Net)" value={l.netEntitlement ?? slip.netEntitlement} />
            <Stat boxed label="Thu hồi tạm ứng" value={l.advanceRecovery} negative />
            <Stat boxed label="Hoàn chi" value={l.reimbursements} />
          </div>
          <div className="flex flex-col gap-2 bg-gradient-to-r from-blue-700 via-blue-600 to-violet-600 px-5 py-4 text-white sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-xs font-bold uppercase tracking-wider text-white/85">Số tiền thực chi</p>
              <p className="mt-0.5 text-xs italic text-white/80">Bằng chữ: {numberToVietnameseWords(l.payable ?? slip.payable)}</p>
            </div>
            <p className="whitespace-nowrap font-mono text-3xl font-black tabular-nums">{money(l.payable ?? slip.payable)} <span className="text-base font-bold">₫</span></p>
          </div>
        </section>

        {/* Ký tên */}
        <section className="ps-avoid-break mt-8 grid grid-cols-2 gap-6 text-center text-[13px]">
          <div>
            <p className="font-bold text-slate-800">Người lập phiếu</p>
            <p className="text-xs italic text-slate-400">Phòng Nhân sự – Tiền lương</p>
            <div className="mx-auto mt-4 flex h-20 w-36 items-center justify-center rounded-full border-2 border-double border-rose-400/70 text-[10px] font-bold uppercase leading-tight tracking-wide text-rose-500/80 rotate-[-8deg]">
              HNQ Media<br />Đã phát hành<br />{formatDate(slip.publishedAt)}
            </div>
          </div>
          <div>
            <p className="font-bold text-slate-800">Người lao động</p>
            <p className="text-xs italic text-slate-400">(Ký, ghi rõ họ tên)</p>
            <p className="mt-20 font-semibold text-slate-700">{slip.employeeName}</p>
          </div>
        </section>

        <footer className="mt-8 border-t border-slate-200 pt-4 text-[11px] leading-relaxed text-slate-400">
          <p>Phiếu lương được tạo tự động bởi HRFlow sau khi kỳ lương đã khóa{l.lockedAt ? ` (${formatDateTime(l.lockedAt)})` : ''} và không chỉnh sửa sau khi phát hành.</p>
          <p>Thông tin lương là bảo mật. Mọi thắc mắc vui lòng liên hệ Phòng Nhân sự trong vòng 05 ngày làm việc kể từ ngày phát hành. Đơn vị tính: VND.</p>
        </footer>
      </div>
    </article>
  );
});

function Info({ label, value, strong, mono }) {
  return (
    <div>
      <p className="text-[11px] font-medium uppercase tracking-wide text-slate-400">{label}</p>
      <p className={`${strong ? 'font-bold text-slate-900' : 'font-semibold text-slate-700'} ${mono ? 'font-mono' : ''}`}>{value || '-'}</p>
    </div>
  );
}

function Stat({ label, value, negative, boxed }) {
  return (
    <div className={boxed ? 'px-5 py-3' : ''}>
      <dt className="text-[11px] text-slate-500">{label}</dt>
      <dd className={`font-mono font-bold tabular-nums ${negative && num(value) !== 0 ? 'text-rose-600' : 'text-slate-900'}`}>
        {negative && num(value) !== 0 ? `(${money(value)})` : money(value)}
      </dd>
    </div>
  );
}

export default PayslipDocument;
