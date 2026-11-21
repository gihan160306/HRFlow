/**
 * Đọc số tiền VND thành chữ tiếng Việt: 28010000 -> "Hai mươi tám triệu không trăm mười nghìn đồng".
 */
const DIGITS = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín'];
const UNITS = ['', ' nghìn', ' triệu', ' tỷ'];

function readTriple(n, full) {
  const h = Math.floor(n / 100);
  const t = Math.floor((n % 100) / 10);
  const u = n % 10;
  const parts = [];
  if (full || h > 0) parts.push(`${DIGITS[h]} trăm`);
  if (t === 0) {
    if (u > 0 && (full || h > 0)) parts.push('lẻ');
  } else if (t === 1) {
    parts.push('mười');
  } else {
    parts.push(`${DIGITS[t]} mươi`);
  }
  if (u > 0) {
    if (u === 1 && t > 1) parts.push('mốt');
    else if (u === 5 && t > 0) parts.push('lăm');
    else if (u === 4 && t > 1) parts.push('tư');
    else parts.push(DIGITS[u]);
  }
  return parts.join(' ');
}

export function numberToVietnameseWords(amount) {
  let n = Math.round(Math.abs(Number(amount) || 0));
  if (n === 0) return 'Không đồng';
  const groups = [];
  while (n > 0) {
    groups.push(n % 1000);
    n = Math.floor(n / 1000);
  }
  const words = [];
  for (let i = groups.length - 1; i >= 0; i -= 1) {
    const g = groups[i];
    if (g === 0) continue;
    const unit = UNITS[i] ?? '';
    words.push(readTriple(g, i < groups.length - 1) + unit);
  }
  const text = words.join(' ').replace(/\s+/g, ' ').trim();
  const sign = Number(amount) < 0 ? 'Âm ' : '';
  const out = `${sign}${text} đồng`;
  return out.charAt(0).toUpperCase() + out.slice(1);
}

export default numberToVietnameseWords;
