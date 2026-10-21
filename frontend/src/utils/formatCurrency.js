/**
 * Formats a numeric amount to Vietnamese Dong currency representation.
 * Example: 15000000 -> "15.000.000 ₫"
 */
export function formatCurrency(amount, currencySymbol = '₫') {
  if (amount === null || amount === undefined || isNaN(Number(amount))) {
    return `0 ${currencySymbol}`;
  }
  const formatted = Math.round(Number(amount))
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${formatted} ${currencySymbol}`;
}

/**
 * Parses a currency string back to integer number.
 * Example: "15.000.000 ₫" -> 15000000
 */
export function parseCurrency(str) {
  if (!str) return 0;
  const cleaned = String(str).replace(/[^\d-]/g, '');
  return parseInt(cleaned, 10) || 0;
}

export default formatCurrency;
