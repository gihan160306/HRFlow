const pad = (n) => String(n).padStart(2, '0');

/** Chuỗi 'YYYY-MM-DD' (LocalDate của backend) được phân tích theo giờ địa phương, tránh lệch múi giờ. */
function parse(input) {
  if (!input) return null;
  if (typeof input === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(input)) {
    const [y, m, d] = input.split('-').map(Number);
    return new Date(y, m - 1, d);
  }
  const d = new Date(input);
  return Number.isNaN(d.getTime()) ? null : d;
}

/** DD/MM/YYYY */
export function formatDate(input) {
  const d = parse(input);
  if (!d) return '-';
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

/** HH:mm DD/MM/YYYY (Instant ISO-8601 từ backend -> giờ địa phương) */
export function formatDateTime(input) {
  const d = parse(input);
  if (!d) return '-';
  return `${pad(d.getHours())}:${pad(d.getMinutes())} ${formatDate(d)}`;
}

/** HH:mm từ Instant ISO hoặc LocalTime 'HH:mm[:ss]' */
export function formatTime(input) {
  if (!input) return '--:--';
  if (typeof input === 'string' && /^\d{2}:\d{2}(:\d{2})?$/.test(input)) return input.slice(0, 5);
  const d = parse(input);
  if (!d) return '--:--';
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Ngày hôm nay dạng YYYY-MM-DD (giờ địa phương) cho <input type="date"> */
export function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Phút -> "Xh Ym" */
export function formatMinutes(min) {
  const m = Number(min);
  if (!Number.isFinite(m)) return '-';
  return `${Math.floor(m / 60)}h${pad(m % 60)}`;
}

export default formatDate;
