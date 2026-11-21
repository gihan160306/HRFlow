/**
 * Chụp một phần tử DOM và lưu thành PDF khổ A4, giữ nguyên giao diện, logo và chữ có dấu.
 * Thư viện được nạp động để không làm nặng bundle ban đầu.
 *
 * - Mặc định (phiếu lương): một trang, thu nhỏ vừa khổ A4 nếu cao hơn trang.
 * - `multiPage: true` (hợp đồng): chia nhiều trang A4, chỉ ngắt tại khoảng trắng giữa các dòng
 *   để không cắt ngang chữ; có đánh số trang.
 */
export async function exportElementToPdf(element, fileName, options = {}) {
  const { multiPage = false, margin = multiPage ? 14 : 8 } = options;
  const [{ default: html2canvas }, { jsPDF }] = await Promise.all([import('html2canvas'), import('jspdf')]);
  const canvas = await html2canvas(element, {
    scale: 2,
    backgroundColor: '#ffffff',
    useCORS: true,
    windowWidth: element.scrollWidth,
  });

  const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' });
  const pageW = pdf.internal.pageSize.getWidth() - margin * 2;
  const pageH = pdf.internal.pageSize.getHeight() - margin * 2;

  if (!multiPage) {
    let w = pageW;
    let h = (canvas.height * w) / canvas.width;
    if (h > pageH) {
      h = pageH;
      w = (canvas.width * h) / canvas.height;
    }
    pdf.addImage(canvas.toDataURL('image/jpeg', 0.95), 'JPEG', margin + (pageW - w) / 2, margin, w, h);
    pdf.save(fileName);
    return;
  }

  const footer = 6; // mm dành cho số trang
  const slicePx = Math.floor((canvas.width * (pageH - footer)) / pageW);
  const slices = [];
  let y = 0;
  while (y < canvas.height - 2) {
    const limit = y + slicePx;
    if (limit >= canvas.height) {
      slices.push([y, canvas.height]);
      break;
    }
    const end = findBlankRow(canvas, Math.floor(y + slicePx * 0.6), limit) ?? limit;
    slices.push([y, end]);
    y = end;
  }

  const part = document.createElement('canvas');
  const ctx = part.getContext('2d');
  slices.forEach(([from, to], i) => {
    part.width = canvas.width;
    part.height = to - from;
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, part.width, part.height);
    ctx.drawImage(canvas, 0, from, canvas.width, part.height, 0, 0, canvas.width, part.height);
    if (i > 0) pdf.addPage();
    pdf.addImage(part.toDataURL('image/jpeg', 0.95), 'JPEG', margin, margin, pageW, (part.height * pageW) / part.width);
    pdf.setFontSize(9);
    pdf.setTextColor(110);
    pdf.text(`Trang ${i + 1}/${slices.length}`, margin + pageW, margin + pageH, { align: 'right' });
  });
  pdf.save(fileName);
}

/**
 * Tìm điểm ngắt trang thấp nhất trong [from, to] nằm giữa một dải hàng pixel trắng hoàn toàn
 * (khoảng trống giữa hai dòng chữ), để không cắt ngang chữ, bảng hay con dấu.
 * Dựa trên ảnh đã vẽ nên không bị lệch như khi đo vị trí DOM.
 */
function findBlankRow(canvas, from, to, minRun = 6) {
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  const h = to - from;
  if (h <= 0) return null;
  const { data, width } = ctx.getImageData(0, from, canvas.width, h);
  const blank = (row) => {
    for (let x = 0; x < width; x += 1) {
      const i = (row * width + x) * 4;
      if (data[i] < 245 || data[i + 1] < 245 || data[i + 2] < 245) return false;
    }
    return true;
  };
  let run = 0;
  for (let row = h - 1; row >= 0; row -= 1) {
    if (blank(row)) {
      run += 1;
    } else {
      if (run >= minRun) return from + row + 1 + Math.floor(run / 2);
      run = 0;
    }
  }
  return run >= minRun ? from + Math.floor(run / 2) : null;
}

export default exportElementToPdf;
