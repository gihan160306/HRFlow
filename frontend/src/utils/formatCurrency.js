/**
 * Định dạng số tiền VND: 15000000 -> "15.000.000 ₫".
 * null/undefined (backend ẩn dữ liệu nhạy cảm) hiển thị "-" thay vì 0 để không gây hiểu nhầm.
 */
export function formatCurrency(amount, currencySymbol = '₫') {
  if (amount === null || amount === undefined || amount === '' || Number.isNaN(Number(amount))) {
    return '-';
  }
  const n = Math.round(Number(amount));
  const sign = n < 0 ? '-' : '';
  const formatted = Math.abs(n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${sign}${formatted} ${currencySymbol}`;
}

export default formatCurrency;
