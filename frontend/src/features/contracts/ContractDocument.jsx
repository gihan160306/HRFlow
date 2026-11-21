import React from 'react';
import { numberToVietnameseWords } from '../../utils/numberToWords';
import { formatDate } from '../../utils/formatDate';

/**
 * Văn bản hợp đồng lao động theo bố cục thông dụng (BLLĐ 2019, TT 10/2020/TT-BLĐTBXH).
 * Đen trắng, font có chân; chỉ con dấu (đỏ) và chữ ký (mực xanh) có màu như bản giấy thật.
 * Mỗi đoạn mang class `pdf-block` để trình xuất PDF chỉ ngắt trang giữa các đoạn.
 */

const COMPANY = {
  name: 'CÔNG TY TNHH TRUYỀN THÔNG HNQ',
  brand: 'HNQ Media',
  address: 'Thành phố Hồ Chí Minh, Việt Nam',
  city: 'TP. Hồ Chí Minh',
};

const DEPARTMENTS = {
  MGMT: {
    name: 'Ban Điều hành',
    duties: [
      'Tổ chức, điều hành và giám sát hoạt động thuộc lĩnh vực được phân công; xây dựng kế hoạch, ngân sách và chỉ tiêu định kỳ.',
      'Kiểm soát chi phí, dòng tiền, đối soát doanh thu và phê duyệt các nội dung trong phạm vi thẩm quyền theo quy chế Công ty.',
      'Báo cáo Giám đốc về tình hình vận hành, tài chính và các rủi ro phát sinh.',
    ],
    special: [
      'Bảo mật tuyệt đối thông tin tài chính, chiến lược kinh doanh, giá dịch vụ và dữ liệu khách hàng của Công ty.',
      'Không lợi dụng chức vụ, quyền hạn để trục lợi; kê khai và tránh xung đột lợi ích theo quy chế quản trị của Công ty.',
    ],
  },
  HR: {
    name: 'Phòng Nhân sự',
    duties: [
      'Quản lý hồ sơ nhân sự, hợp đồng lao động, quyết định nhân sự và các thủ tục tiếp nhận, nghỉ việc.',
      'Theo dõi chấm công, nghỉ phép, làm thêm giờ; lập bảng lương, kê khai bảo hiểm xã hội và thuế thu nhập cá nhân.',
      'Triển khai tuyển dụng, đào tạo, đánh giá và các chính sách phúc lợi của Công ty.',
    ],
    special: [
      'Bảo mật dữ liệu cá nhân, tiền lương và hồ sơ của người lao động; chỉ sử dụng đúng mục đích và trong phạm vi được phân quyền.',
      'Không tiết lộ, sao chép hoặc cung cấp thông tin nhân sự cho bên ngoài khi chưa có chấp thuận bằng văn bản của Công ty.',
    ],
  },
  CONTENT: {
    name: 'Phòng Nội dung (Content)',
    duties: [
      'Lên ý tưởng, biên soạn nội dung quảng cáo, bài đăng mạng xã hội và nội dung SEO theo kế hoạch của từng dự án.',
      'Xây dựng và thực hiện lịch đăng bài; kiểm tra chính tả, tính chính xác và sự phù hợp của nội dung với thương hiệu khách hàng.',
      'Phối hợp với các bộ phận Account, Design và Digital Marketing để bảo đảm tiến độ và chất lượng sản phẩm.',
    ],
    special: [
      'Toàn bộ nội dung, ý tưởng do Bên B tạo ra trong quá trình thực hiện công việc thuộc quyền sở hữu của Công ty hoặc khách hàng của Công ty theo hợp đồng dịch vụ.',
      'Không sao chép, sử dụng tác phẩm của bên thứ ba khi chưa được phép; tuân thủ quy định về quyền tác giả và quảng cáo.',
    ],
  },
  DESIGN: {
    name: 'Phòng Thiết kế & Video',
    duties: [
      'Thiết kế ấn phẩm, banner, bộ nhận diện chiến dịch; quay, dựng và hậu kỳ video theo yêu cầu dự án.',
      'Lưu trữ đầy đủ tệp gốc, tài nguyên thiết kế trên hệ thống lưu trữ của Công ty theo đúng quy định.',
      'Chỉnh sửa sản phẩm theo phản hồi của khách hàng và bảo đảm đúng thời hạn bàn giao.',
    ],
    special: [
      'Quyền sở hữu trí tuệ đối với thiết kế, hình ảnh, video và tệp gốc thực hiện trong thời gian làm việc thuộc về Công ty; Bên B không được tự ý sử dụng cho mục đích cá nhân hoặc thương mại.',
      'Chỉ sử dụng phông chữ, hình ảnh, âm nhạc và phần mềm có giấy phép hợp lệ; bảo quản thiết bị và tài sản được Công ty giao.',
    ],
  },
  DIGITAL: {
    name: 'Phòng Digital Marketing',
    duties: [
      'Thiết lập, vận hành và tối ưu chiến dịch quảng cáo trực tuyến (Google, Meta và các nền tảng khác) theo ngân sách được duyệt.',
      'Phân tích dữ liệu, nghiên cứu từ khóa, tối ưu SEO và lập báo cáo hiệu quả chiến dịch định kỳ.',
      'Đề xuất giải pháp cải thiện hiệu quả truyền thông cho khách hàng.',
    ],
    special: [
      'Quản lý tài khoản quảng cáo, ngân sách chi hộ của khách hàng đúng mục đích, minh bạch và có chứng từ đối soát.',
      'Bảo mật thông tin đăng nhập, dữ liệu khách hàng và dữ liệu phân tích; không chuyển giao quyền truy cập khi chưa được phép.',
    ],
  },
  ACCOUNT: {
    name: 'Phòng Quản lý khách hàng (Account)',
    duties: [
      'Tiếp nhận yêu cầu của khách hàng, xác định phạm vi công việc và điều phối tiến độ giữa các bộ phận chuyên môn.',
      'Làm đầu mối liên hệ, chăm sóc khách hàng; tổ chức nghiệm thu và xác nhận kết quả theo từng giai đoạn.',
      'Theo dõi thực hiện hợp đồng dịch vụ và phối hợp với bộ phận tài chính trong việc thu hồi công nợ.',
    ],
    special: [
      'Bảo mật thông tin khách hàng, điều khoản hợp đồng dịch vụ và giá dịch vụ của Công ty.',
      'Không trực tiếp hoặc gián tiếp lôi kéo khách hàng của Công ty sang đơn vị khác trong thời gian thực hiện hợp đồng này.',
    ],
  },
  SALES: {
    name: 'Phòng Kinh doanh (Sales)',
    duties: [
      'Tìm kiếm, tiếp cận khách hàng; tư vấn và báo giá các gói dịch vụ truyền thông của Công ty.',
      'Đàm phán, chuẩn bị hồ sơ ký kết hợp đồng dịch vụ; phối hợp với Account bàn giao khách hàng sau ký kết.',
      'Thực hiện chỉ tiêu doanh số (KPI) được giao và báo cáo kết quả kinh doanh định kỳ.',
    ],
    special: [
      'Ghi nhận doanh số trung thực theo hồ sơ hợp đồng và chứng từ thanh toán đã được xác nhận; không nhận tiền, quà hoặc hoa hồng riêng từ khách hàng.',
      'Hoa hồng và thưởng KPI được tính theo Quy chế KPI – Hoa hồng Sales của Công ty, chỉ dựa trên doanh thu dịch vụ đủ điều kiện.',
    ],
  },
};

const FALLBACK_DEPT = {
  name: null,
  duties: ['Thực hiện các công việc theo mô tả chức danh và sự phân công của người quản lý trực tiếp.'],
  special: ['Bảo mật thông tin, tài liệu và dữ liệu của Công ty và khách hàng.'],
};

const TITLES = {
  INDEFINITE: 'HỢP ĐỒNG LAO ĐỘNG',
  FIXED_TERM: 'HỢP ĐỒNG LAO ĐỘNG',
  PART_TIME: 'HỢP ĐỒNG LAO ĐỘNG',
  PROBATION: 'HỢP ĐỒNG THỬ VIỆC',
  INTERNSHIP: 'HỢP ĐỒNG THỰC TẬP',
};

const money = (v) => (v === null || v === undefined ? '…………' : Math.round(Number(v)).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.'));

function parseDate(s) {
  if (!s) return null;
  const [y, m, d] = String(s).slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}

/** Số tháng tròn của khoảng [a, b] (tính cả ngày cuối); null nếu không tròn tháng. */
function wholeMonths(a, b) {
  const s = parseDate(a);
  const e = parseDate(b);
  if (!s || !e) return null;
  const end = new Date(e.getFullYear(), e.getMonth(), e.getDate() + 1);
  const months = (end.getFullYear() - s.getFullYear()) * 12 + end.getMonth() - s.getMonth();
  const check = new Date(s.getFullYear(), s.getMonth() + months, s.getDate());
  return months > 0 && check.getTime() === end.getTime() ? months : null;
}

/** Số ngày của khoảng [a, b], tính cả ngày đầu và ngày cuối. */
function daysBetween(a, b) {
  const s = parseDate(a);
  const e = parseDate(b);
  if (!s || !e) return null;
  return Math.round((e - s) / 86400000) + 1;
}

function longDate(s) {
  const d = parseDate(s);
  if (!d) return 'ngày …… tháng …… năm ……';
  return `ngày ${String(d.getDate()).padStart(2, '0')} tháng ${String(d.getMonth() + 1).padStart(2, '0')} năm ${d.getFullYear()}`;
}

const honorific = (gender) => (gender === 'FEMALE' ? 'Bà' : gender === 'MALE' ? 'Ông' : 'Ông/Bà');

const SEAL_RED = '#c8102e';

/** Nét vẽ con dấu tròn trong hệ tọa độ 160×160 (dùng chung cho con dấu rời và khối ký tên). */
function SealGraphic({ idPrefix = 'seal' }) {
  return (
    <g>
      <defs>
        <path id={`${idPrefix}-top`} d="M 27.4 99.2 A 56 56 0 1 1 132.6 99.2" />
        <path id={`${idPrefix}-bottom`} d="M 27.6 116.7 A 64 64 0 0 0 132.4 116.7" />
      </defs>
      <g fill="none" stroke={SEAL_RED}>
        <circle cx="80" cy="80" r="76" strokeWidth="3.2" />
        <circle cx="80" cy="80" r="71" strokeWidth="1" />
        <circle cx="80" cy="80" r="45" strokeWidth="1.3" />
      </g>
      <g fill={SEAL_RED} fontFamily="'Times New Roman', Times, serif" fontWeight="700">
        <text fontSize="11">
          <textPath href={`#${idPrefix}-top`} startOffset="3" textLength="208" lengthAdjust="spacing">CÔNG TY TNHH TRUYỀN THÔNG HNQ</textPath>
        </text>
        <text fontSize="10.5" letterSpacing="1">
          <textPath href={`#${idPrefix}-bottom`} startOffset="50%" textAnchor="middle">TP. HỒ CHÍ MINH</textPath>
        </text>
        <text x="21" y="114" fontSize="9">★</text>
        <text x="131" y="114" fontSize="9">★</text>
        <text x="80" y="68" fontSize="11" textAnchor="middle">★</text>
        <text x="80" y="89" fontSize="19" textAnchor="middle" letterSpacing="1">HNQ</text>
        <text x="80" y="101" fontSize="8.5" textAnchor="middle" letterSpacing="1.6">MEDIA</text>
      </g>
      <line x1="56" y1="73" x2="104" y2="73" stroke={SEAL_RED} strokeWidth="0.8" />
    </g>
  );
}

/** Con dấu tròn của công ty. */
export function CompanySeal({ size = 150, className = '' }) {
  return (
    <svg viewBox="0 0 160 160" width={size} height={size} className={className} aria-label="Con dấu công ty">
      <SealGraphic idPrefix="seal-solo" />
    </svg>
  );
}

/**
 * Chữ ký tay cách điệu (mực xanh) và con dấu đóng trùm 1/3 bên trái chữ ký, gộp trong MỘT SVG:
 * xoay/độ mờ đặt bằng thuộc tính SVG vì html2canvas xử lý sai CSS transform/opacity khi xuất PDF.
 */
function SignatureWithSeal() {
  return (
    <svg viewBox="0 0 240 124" width="240" height="124" aria-label="Chữ ký và con dấu công ty" style={{ display: 'block' }}>
      <g transform="translate(92 26) scale(0.86)" fill="none" stroke="#1f3a93" strokeLinecap="round" strokeLinejoin="round">
        <path strokeWidth="2.4" d="M12 52 C 22 20, 38 14, 40 34 C 42 52, 30 62, 34 46 C 40 26, 58 22, 62 40 C 65 54, 70 50, 78 36 C 84 26, 92 30, 90 44 C 88 58, 100 52, 110 34 C 118 20, 128 24, 124 42 C 121 56, 136 54, 150 38 C 162 26, 176 30, 170 44 C 166 54, 182 50, 206 30" />
        <path strokeWidth="1.8" d="M30 64 C 80 58, 140 60, 196 52" />
      </g>
      <g opacity="0.88" transform="translate(2 0) scale(0.775) rotate(-10 80 80)">
        <SealGraphic idPrefix="seal-sign" />
      </g>
    </svg>
  );
}

function Article({ no, title, children }) {
  return (
    <>
      <p className="pdf-block mt-3 font-bold">Điều {no}. {title}</p>
      {children}
    </>
  );
}

function P({ children, indent = true, className = '' }) {
  return <p className={`pdf-block text-justify ${indent ? 'pl-5' : ''} ${className}`}>{children}</p>;
}

function Field({ label, children }) {
  return (
    <tr className="pdf-block align-top">
      <td className="w-[190px] py-[1px] pl-5 pr-2">{label}</td>
      <td className="py-[1px]">{children || '……………………………'}</td>
    </tr>
  );
}

export const ContractDocument = React.forwardRef(function ContractDocument({ doc, id }, ref) {
  const e = doc.employee || {};
  const dept = DEPARTMENTS[e.departmentCode] || { ...FALLBACK_DEPT, name: e.departmentName };
  const type = doc.contractType;
  const isProbation = type === 'PROBATION';
  const isIntern = type === 'INTERNSHIP';
  const isPartTime = type === 'PART_TIME' || e.employeeType === 'PART_TIME';
  const signed = doc.status !== 'DRAFT';
  const rep = doc.representative || { fullName: '……………………', title: 'Giám đốc' };
  const party = isIntern ? 'Bên nhận thực tập' : 'Người sử dụng lao động';
  const worker = isIntern ? 'Người thực tập' : 'Người lao động';
  const months = wholeMonths(doc.startDate, doc.endDate);
  const days = daysBetween(doc.startDate, doc.endDate);
  const allowances = doc.allowances || [];
  const salaryLabel = isIntern ? 'Mức trợ cấp thực tập' : isProbation ? 'Mức lương thử việc' : 'Mức lương cơ bản';

  let n = 0;
  const next = () => { n += 1; return n; };

  const termText = (() => {
    if (type === 'INDEFINITE' || !doc.endDate) return `Hợp đồng lao động không xác định thời hạn, kể từ ngày ${formatDate(doc.startDate)}.`;
    // Thử việc tính theo ngày (Điều 25 BLLĐ); loại khác chỉ ghi số tháng khi tròn tháng.
    const span = isProbation && days ? ` ${days} ngày,` : months ? ` ${months} tháng,` : '';
    const label = isProbation ? 'Thời gian thử việc' : isIntern ? 'Thời gian thực tập' : 'Hợp đồng lao động xác định thời hạn';
    return `${label}${span} từ ngày ${formatDate(doc.startDate)} đến hết ngày ${formatDate(doc.endDate)}.`;
  })();

  const noticeText = (() => {
    if (isProbation) return 'Trong thời gian thử việc, mỗi bên có quyền hủy bỏ hợp đồng này mà không cần báo trước và không phải bồi thường (Điều 27 Bộ luật Lao động).';
    if (isIntern) return 'Mỗi bên có quyền chấm dứt hợp đồng thực tập trước thời hạn nhưng phải thông báo cho bên kia ít nhất 03 ngày làm việc.';
    if (type === 'INDEFINITE' || !doc.endDate) return 'Bên nào đơn phương chấm dứt hợp đồng phải báo trước cho bên kia ít nhất 45 ngày, trừ các trường hợp không cần báo trước theo Điều 35, Điều 36 Bộ luật Lao động.';
    if (days && days < 365) return 'Bên nào đơn phương chấm dứt hợp đồng phải báo trước cho bên kia ít nhất 03 ngày làm việc, trừ các trường hợp không cần báo trước theo Điều 35, Điều 36 Bộ luật Lao động.';
    return 'Bên nào đơn phương chấm dứt hợp đồng phải báo trước cho bên kia ít nhất 30 ngày, trừ các trường hợp không cần báo trước theo Điều 35, Điều 36 Bộ luật Lao động.';
  })();

  return (
    <article
      ref={ref}
      id={id}
      className="contract-doc relative mx-auto w-[794px] bg-white px-[64px] py-[56px] text-[15px] leading-[1.6] text-black"
      style={{ fontFamily: "'Times New Roman', Times, 'Noto Serif', serif" }}
    >
      {!signed && (
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 flex items-start justify-center pt-[380px]">
          <span className="rotate-[-30deg] border-4 border-black/10 px-6 py-2 text-[64px] font-bold tracking-[0.2em] text-black/10">BẢN NHÁP</span>
        </div>
      )}

      {/* Quốc hiệu */}
      <header className="pdf-block grid grid-cols-[1fr_1.25fr] gap-4 text-center">
        <div>
          <p className="text-[13.5px] font-bold uppercase leading-snug">{COMPANY.name}</p>
          <div className="mx-auto mt-1 h-px w-24 bg-black" />
          <p className="mt-2 text-[14px]">Số: {doc.contractNo}</p>
        </div>
        <div>
          <p className="text-[13.5px] font-bold">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</p>
          <p className="text-[14.5px] font-bold">Độc lập – Tự do – Hạnh phúc</p>
          <div className="mx-auto mt-1 h-px w-44 bg-black" />
          <p className="mt-2 text-[14px] italic">{COMPANY.city}, {longDate(doc.signedDate || doc.startDate)}</p>
        </div>
      </header>

      <div className="pdf-block mt-7 text-center">
        <h1 className="text-[20px] font-bold tracking-wide">{TITLES[type] || 'HỢP ĐỒNG LAO ĐỘNG'}</h1>
        {isPartTime && !isIntern && !isProbation && <p className="italic">(Làm việc không trọn thời gian)</p>}
      </div>

      <div className="mt-4 italic">
        <P indent={false}>– Căn cứ Bộ luật Lao động số 45/2019/QH14 ngày 20 tháng 11 năm 2019;</P>
        <P indent={false}>– Căn cứ Nghị định số 145/2020/NĐ-CP ngày 14 tháng 12 năm 2020 của Chính phủ quy định chi tiết và hướng dẫn thi hành một số điều của Bộ luật Lao động về điều kiện lao động và quan hệ lao động;</P>
        <P indent={false}>– Căn cứ Nội quy lao động, Quy chế tiền lương và Quy chế hoạt động của {COMPANY.name.replace('CÔNG TY', 'Công ty').replace('TNHH TRUYỀN THÔNG', 'TNHH Truyền thông')};</P>
        {doc.previousContractId && <P indent={false}>– Căn cứ kết quả thực hiện hợp đồng trước đó và nhu cầu sử dụng lao động của Công ty;</P>}
        <P indent={false}>– Căn cứ nhu cầu và năng lực của hai bên.</P>
      </div>

      <P indent={false} className="mt-2">Hôm nay, {longDate(doc.signedDate || doc.startDate)}, tại trụ sở {COMPANY.name.replace('CÔNG TY', 'Công ty').replace('TNHH TRUYỀN THÔNG', 'TNHH Truyền thông')}, chúng tôi gồm:</P>

      <p className="pdf-block mt-3 font-bold uppercase">Bên A: {party}</p>
      <table className="w-full"><tbody>
        <Field label="Tên đơn vị:"><b>{COMPANY.name}</b> ({COMPANY.brand})</Field>
        <Field label="Địa chỉ trụ sở:">{COMPANY.address}</Field>
        <Field label="Đại diện bởi:"><b>{honorific(rep.gender)} {rep.fullName}</b></Field>
        <Field label="Chức vụ:">{rep.title}</Field>
      </tbody></table>

      <p className="pdf-block mt-3 font-bold uppercase">Bên B: {worker}</p>
      <table className="w-full"><tbody>
        <Field label="Họ và tên:"><b>{honorific(e.gender)} {e.fullName}</b></Field>
        <Field label="Mã nhân viên:">{e.code}</Field>
        <Field label="Ngày sinh:">{e.dateOfBirth ? formatDate(e.dateOfBirth) : null}</Field>
        <Field label="Số CCCD:">{e.nationalId}</Field>
        <Field label="Địa chỉ thường trú:">{e.address}</Field>
        <Field label="Điện thoại:">{e.phone}</Field>
        <Field label="Thư điện tử:">{e.personalEmail || e.workEmail}</Field>
      </tbody></table>

      <P indent={false} className="mt-2">Hai bên thỏa thuận ký kết {(TITLES[type] || 'hợp đồng lao động').toLowerCase()} và cam kết thực hiện đúng các điều khoản sau đây:</P>

      <Article no={next()} title="Công việc và địa điểm làm việc">
        <P>1. Chức danh: <b>{e.jobTitleName}</b>{e.managerial ? ' (cấp quản lý)' : ''}.</P>
        <P>2. Bộ phận công tác: <b>{dept.name || e.departmentName}</b>{e.managerName ? `; chịu sự quản lý trực tiếp của ${e.managerTitle ? `${e.managerTitle} – ` : ''}${e.managerName}` : ''}.</P>
        <P>3. Công việc phải làm:</P>
        {dept.duties.map((d) => <P key={d} className="pl-9">– {d}</P>)}
        {e.managerial && <P className="pl-9">– Quản lý, phân công, hướng dẫn, đánh giá nhân sự thuộc bộ phận; duyệt chấm công, nghỉ phép và làm thêm giờ theo phân quyền.</P>}
        <P className="pl-9">– Thực hiện các công việc khác phù hợp với chuyên môn theo sự phân công của người quản lý.</P>
        <P>4. Địa điểm làm việc: {e.workLocation || COMPANY.address}; Bên B có thể được cử đi công tác tại địa điểm của khách hàng khi có yêu cầu công việc.</P>
      </Article>

      <Article no={next()} title={isProbation ? 'Thời gian thử việc' : isIntern ? 'Thời hạn thực tập' : 'Thời hạn hợp đồng'}>
        <P>1. {termText}</P>
        {isProbation && <P>2. Kết thúc thời gian thử việc, Bên A thông báo kết quả cho Bên B. Trường hợp đạt yêu cầu, hai bên tiến hành giao kết hợp đồng lao động; trường hợp không đạt, hợp đồng này chấm dứt.</P>}
        {isIntern && <P>2. Kết thúc thời gian thực tập, Bên A đánh giá và xác nhận kết quả thực tập cho Bên B; việc tuyển dụng chính thức (nếu có) được thực hiện bằng hợp đồng lao động riêng.</P>}
      </Article>

      <Article no={next()} title="Thời giờ làm việc, thời giờ nghỉ ngơi">
        {isPartTime ? (
          <P>1. Thời giờ làm việc: không trọn thời gian, 04 giờ/ngày, 20 giờ/tuần, từ thứ Hai đến thứ Sáu, sáng từ 08h30 đến 12h30.</P>
        ) : (
          <P>1. Thời giờ làm việc: 08 giờ/ngày, 40 giờ/tuần, từ thứ Hai đến thứ Sáu; sáng từ 08h30 đến 12h00, chiều từ 13h00 đến 17h30.</P>
        )}
        <P>2. Bên B chấm công tại quầy bằng mã QR cá nhân theo quy định chấm công của Công ty. Làm thêm giờ chỉ được thực hiện khi có kế hoạch hoặc yêu cầu được phê duyệt và trong giới hạn pháp luật cho phép.</P>
        <P>3. Nghỉ lễ, Tết và nghỉ việc riêng có hưởng lương theo Điều 112, Điều 115 Bộ luật Lao động.{!isProbation && !isIntern ? ' Nghỉ hằng năm 12 ngày làm việc/năm đối với người làm đủ 12 tháng; trường hợp chưa đủ 12 tháng được tính theo tỷ lệ thời gian làm việc.' : ''}</P>
      </Article>

      <Article no={next()} title={isIntern ? 'Trợ cấp và chế độ' : 'Tiền lương, phụ cấp và hình thức trả lương'}>
        <P>1. {salaryLabel}: <b>{money(doc.baseSalary)} đồng/tháng</b>{doc.baseSalary !== null && doc.baseSalary !== undefined ? <i> (Bằng chữ: {numberToVietnameseWords(doc.baseSalary)})</i> : null}.{isProbation ? ' Mức lương này không thấp hơn 85% mức lương của công việc tương ứng (Điều 26 Bộ luật Lao động).' : ''}</P>
        {!isIntern && (
          allowances.length ? (
            <>
              <P>2. Phụ cấp (theo quy chế phụ cấp và điều kiện hưởng thực tế):</P>
              <div className="pdf-block pl-9">
                {/* Lưới div thay cho <table>: html2canvas tính sai vị trí viền của bảng HTML */}
                <div role="table" className="w-[520px] border-l border-t border-black text-[14px]">
                  {[{ code: '_h', head: true }, ...allowances].map((a, i) => (
                    <div role="row" key={a.code} className={`grid grid-cols-[56px_1fr_170px] ${a.head ? 'font-bold' : ''}`}>
                      <div className="border-b border-r border-black px-2 py-[3px] text-center">{a.head ? 'STT' : i}</div>
                      <div className="border-b border-r border-black px-2 py-[3px]">{a.head ? 'Khoản phụ cấp' : a.name}</div>
                      <div className="border-b border-r border-black px-2 py-[3px] text-right">{a.head ? 'Mức (đồng/tháng)' : money(a.amount)}</div>
                    </div>
                  ))}
                </div>
              </div>
            </>
          ) : (
            <P>2. Phụ cấp: theo Quy chế tiền lương, phụ cấp của Công ty và điều kiện hưởng thực tế của Bên B.</P>
          )
        )}
        {e.departmentCode === 'SALES' && !isIntern && <P>3. Thu nhập bổ sung: hoa hồng doanh số và thưởng KPI theo Quy chế KPI – Hoa hồng Sales được Công ty ban hành theo từng thời kỳ.</P>}
        {!isIntern && <P>{e.departmentCode === 'SALES' ? '4' : '3'}. Tiền lương làm thêm giờ: ít nhất bằng 150% vào ngày thường, 200% vào ngày nghỉ hằng tuần, 300% vào ngày nghỉ lễ, Tết, ngày nghỉ có hưởng lương (Điều 98 Bộ luật Lao động).</P>}
        <P>{isIntern ? '2' : e.departmentCode === 'SALES' ? '5' : '4'}. Hình thức trả: chuyển khoản vào tài khoản ngân hàng do Bên B đăng ký, mỗi tháng một lần, chậm nhất vào ngày 05 của tháng kế tiếp. Bên B được nhận phiếu lương chi tiết trên hệ thống HRFlow.</P>
        {!isProbation && !isIntern && <P>{e.departmentCode === 'SALES' ? '6' : '5'}. Chế độ nâng lương, thưởng: xem xét theo kết quả đánh giá công việc định kỳ và Quy chế tiền lương của Công ty.</P>}
      </Article>

      <Article no={next()} title="Bảo hiểm và phúc lợi">
        {isProbation || isIntern ? (
          <P>1. Trong thời gian {isProbation ? 'thử việc' : 'thực tập'}, Bên B chưa thuộc đối tượng tham gia bảo hiểm xã hội bắt buộc theo hợp đồng này; Bên A bảo đảm điều kiện an toàn, vệ sinh lao động cho Bên B.</P>
        ) : (
          <P>1. Hai bên tham gia bảo hiểm xã hội, bảo hiểm y tế, bảo hiểm thất nghiệp bắt buộc theo quy định. Bên B đóng 8% BHXH, 1,5% BHYT, 1% BHTN trên tiền lương tháng đóng bảo hiểm; Bên A đóng phần thuộc trách nhiệm của người sử dụng lao động.</P>
        )}
        <P>2. Bên B được hưởng các phúc lợi chung của Công ty và được đào tạo, bồi dưỡng chuyên môn phù hợp với yêu cầu công việc.</P>
      </Article>

      <Article no={next()} title={`Quyền và nghĩa vụ của ${worker.toLowerCase()} (Bên B)`}>
        <P>1. Quyền: được bố trí công việc, trả lương, hưởng các chế độ theo hợp đồng này, nội quy lao động và quy định của pháp luật; được đơn phương chấm dứt hợp đồng theo quy định.</P>
        <P>2. Nghĩa vụ chung: hoàn thành công việc được giao; chấp hành nội quy lao động, kỷ luật lao động, quy định về an toàn – vệ sinh lao động và sự điều hành hợp pháp của Bên A; bồi thường thiệt hại khi vi phạm theo quy định.</P>
        <P>3. Nghĩa vụ riêng theo vị trí công việc tại {dept.name || 'bộ phận'}:</P>
        {dept.special.map((s) => <P key={s} className="pl-9">– {s}</P>)}
        <P>4. Bảo mật thông tin: không tiết lộ bí mật kinh doanh, bí mật công nghệ, dữ liệu của Công ty và khách hàng trong thời gian làm việc và sau khi chấm dứt hợp đồng; hoàn trả tài liệu, thiết bị và quyền truy cập khi nghỉ việc.</P>
      </Article>

      <Article no={next()} title={`Quyền và nghĩa vụ của ${party.toLowerCase()} (Bên A)`}>
        <P>1. Quyền: điều hành, phân công, kiểm tra Bên B hoàn thành công việc; xử lý kỷ luật lao động, tạm đình chỉ công việc theo quy định của pháp luật và nội quy lao động.</P>
        <P>2. Nghĩa vụ: bảo đảm việc làm, điều kiện làm việc theo hợp đồng; thanh toán đầy đủ, đúng hạn tiền lương và các chế độ cho Bên B; bảo vệ dữ liệu cá nhân của Bên B theo quy định.</P>
      </Article>

      <Article no={next()} title="Chấm dứt hợp đồng">
        <P>1. Hợp đồng chấm dứt trong các trường hợp quy định tại Điều 34 Bộ luật Lao động.</P>
        <P>2. {noticeText}</P>
        <P>3. Trong thời hạn 14 ngày làm việc kể từ ngày chấm dứt hợp đồng, hai bên có trách nhiệm thanh toán đầy đủ các khoản liên quan đến quyền lợi của mỗi bên.</P>
      </Article>

      <Article no={next()} title="Điều khoản thi hành">
        <P>1. Những vấn đề không ghi trong hợp đồng này được áp dụng theo nội quy lao động, quy chế của Công ty và quy định của pháp luật lao động.</P>
        <P>2. Hợp đồng có hiệu lực từ ngày {formatDate(doc.startDate)}, được lập thành 02 bản có giá trị pháp lý như nhau, mỗi bên giữ 01 bản. Mọi sửa đổi, bổ sung phải được hai bên thỏa thuận bằng văn bản (phụ lục hợp đồng).</P>
      </Article>

      {/* Ký tên */}
      <section className="pdf-block mt-8 grid grid-cols-2 text-center">
        <div>
          <p className="font-bold uppercase">{worker}</p>
          <p className="text-[14px] italic">(Ký, ghi rõ họ tên)</p>
          <div className="h-[124px]" />
          <p className="font-bold">{e.fullName}</p>
        </div>
        <div>
          <p className="font-bold uppercase">{party}</p>
          <p className="font-bold uppercase">{rep.title}</p>
          <div className="mx-auto h-[124px] w-[240px]">
            {signed && <SignatureWithSeal />}
          </div>
          <p className="font-bold">{rep.fullName}</p>
        </div>
      </section>

      {(doc.status === 'TERMINATED' || doc.status === 'EXPIRED') && (
        <div className="pdf-block mt-8 border border-black px-4 py-2 text-[14px]">
          <b>Ghi chú tình trạng:</b>{' '}
          {doc.status === 'TERMINATED'
            ? `Hợp đồng đã chấm dứt từ ngày ${formatDate(doc.terminationDate)}${doc.terminationReason ? `. Lý do: ${doc.terminationReason}` : ''}.`
            : `Hợp đồng đã hết hạn ngày ${formatDate(doc.endDate)}.`}
        </div>
      )}

      <p className="pdf-block mt-8 border-t border-black/40 pt-2 text-[12px] italic text-black/60">
        Bản sao điện tử trích xuất từ hệ thống HRFlow – {COMPANY.brand}. Bản gốc hợp đồng do mỗi bên lưu giữ.
      </p>
    </article>
  );
});

export default ContractDocument;
