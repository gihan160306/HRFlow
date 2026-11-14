package com.hrflow.payroll;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Bộ ghi PDF tối giản (font Helvetica chuẩn, không phụ thuộc thư viện ngoài). Font chuẩn không có chữ có dấu tiếng Việt
 * nên văn bản được chuyển về không dấu (đ → d); số tiền và mã không bị ảnh hưởng.
 */
final class SimplePdf {

    private static final int LINES_PER_PAGE = 58;

    private SimplePdf() {
    }

    static String ascii(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        StringBuilder sb = new StringBuilder();
        for (char c : n.toCharArray()) {
            sb.append(c >= 32 && c < 127 ? c : '?');
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return ascii(s).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    static byte[] render(List<String> lines) {
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += LINES_PER_PAGE) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + LINES_PER_PAGE)));
        }
        if (pages.isEmpty()) {
            pages.add(List.of(""));
        }
        List<byte[]> objects = new ArrayList<>();
        // 1 catalog, 2 pages, 3 font, sau đó từng cặp (page, content)
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            kids.append(4 + i * 2).append(" 0 R ");
        }
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>").getBytes(StandardCharsets.ISO_8859_1));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 0; i < pages.size(); i++) {
            int contentId = 5 + i * 2;
            objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 3 0 R >> >> /Contents " + contentId + " 0 R >>")
                    .getBytes(StandardCharsets.ISO_8859_1));
            StringBuilder c = new StringBuilder("BT /F1 10 Tf 40 800 Td 13 TL\n");
            for (String line : pages.get(i)) {
                c.append('(').append(escape(line)).append(") Tj T*\n");
            }
            c.append("ET");
            byte[] stream = c.toString().getBytes(StandardCharsets.ISO_8859_1);
            objects.add(("<< /Length " + stream.length + " >>\nstream\n" + new String(stream, StandardCharsets.ISO_8859_1) + "\nendstream")
                    .getBytes(StandardCharsets.ISO_8859_1));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, (i + 1) + " 0 obj\n");
            out.writeBytes(objects.get(i));
            write(out, "\nendobj\n");
        }
        int xref = out.size();
        write(out, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) {
            write(out, String.format("%010d 00000 n \n", off));
        }
        write(out, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
