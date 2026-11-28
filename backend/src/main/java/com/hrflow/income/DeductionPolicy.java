package com.hrflow.income;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hrflow.common.exception.BusinessException;

/**
 * Quy tắc kiểm soát khoản khấu trừ bị cấm theo FR-11, BR-14, B16 và payroll_rules.md (Mục 1, 6, 9):
 * không cho phép phạt tiền đi trễ/về sớm, cắt lương mang tính kỷ luật hoặc phạt tiền do không đạt KPI.
 *
 * <p>Chỉ áp dụng cho các loại khoản làm giảm thu nhập/thực chi ({@code DEDUCTION}, {@code COMPENSATION},
 * {@code ADVANCE_RECOVERY}); không áp dụng cho {@code BONUS}, {@code OTHER_ADDITION}, {@code REIMBURSEMENT}.
 * Nhận diện theo tổ hợp ngữ cảnh (không chặn đơn lẻ các từ như "kỷ luật", "KPI", "đi trễ") và bỏ qua
 * các mệnh đề phủ định rõ ràng (ví dụ "không áp dụng phạt tiền đi trễ").
 */
public final class DeductionPolicy {

    private static final Set<String> DEDUCTION_TYPES = Set.of("DEDUCTION", "COMPENSATION", "ADVANCE_RECOVERY");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern CLAUSE_SPLIT = Pattern.compile("[.,;:\\n|]+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private static final Pattern NEGATION_PREFIX = Pattern.compile(
            "(?:^|\\s)(?:khong ap dung|khong phai|khong bao gom|khong tinh|khong thu|khong dung|khong co|khong|"
                    + "loai tru|ngoai tru|mien|no|not|without|excluding|never)\\s*(?:khoan\\s+|hinh thuc\\s+|muc\\s+)?$");

    private static final List<Pattern> PROHIBITED_PATTERNS = List.of(
            // 1. Phạt đi trễ / đi muộn / về sớm / vi phạm giờ chấm công (VN & EN)
            Pattern.compile("\\b(?:phat|phat tien|tien phat|thu phat|tru luong|cat luong|khau tru)\\b(?:\\s+\\w+){0,4}\\s+\\b(?:di tre|di muon|tre gio|muon gio|ve som|cham cong)\\b"),
            Pattern.compile("\\b(?:di tre|di muon|tre gio|muon gio|ve som)\\b(?:\\s+\\w+){0,3}\\s+\\b(?:phat|phat tien|tien phat|tru luong|cat luong|khau tru)\\b"),
            Pattern.compile("\\b(?:late|lateness|tardy|tardiness|early leave|attendance)\\b(?:\\s+\\w+){0,2}\\s+\\b(?:fine|fines|penalty|penalties|deduction)\\b"),
            Pattern.compile("\\b(?:fine|fines|penalty|penalties|deduction)\\b\\s+(?:for|on)\\s+(?:being\\s+)?\\b(?:late|lateness|tardy|tardiness|early leave)\\b"),

            // 2. Phạt tiền / cắt lương mang tính kỷ luật lao động hoặc vi phạm nội quy (VN & EN)
            Pattern.compile("\\b(?:phat tien|tien phat|thu phat|cat luong|tru luong|khau tru phat)\\b(?:\\s+\\w+){0,4}\\s+\\b(?:ky luat|noi quy|vi pham)\\b"),
            Pattern.compile("\\b(?:phat)\\s+(?:ky luat|noi quy|vi pham noi quy|vi pham ky luat)\\b"),
            Pattern.compile("\\b(?:ky luat|noi quy)\\b(?:\\s+\\w+){0,3}\\s+\\b(?:phat tien|tien phat|cat luong|tru luong)\\b"),
            Pattern.compile("\\b(?:disciplinary)\\b(?:\\s+\\w+){0,2}\\s+\\b(?:fine|fines|penalty|penalties|salary cut|pay cut|deduction)\\b"),
            Pattern.compile("\\b(?:salary cut|pay cut)\\b\\s+(?:for|as)\\s+\\b(?:discipline|disciplinary|punishment)\\b"),

            // 3. Phạt / trừ lương do không đạt KPI hoặc đánh giá kém (VN & EN)
            Pattern.compile("\\b(?:phat|phat tien|tien phat|thu phat|tru luong|cat luong|khau tru)\\b(?:\\s+\\w+){0,4}\\s+\\b(?:kpi|khong dat chi tieu|khong dat kpi|danh gia kem|hieu suat kem)\\b"),
            Pattern.compile("\\b(?:kpi|khong dat chi tieu|danh gia kem|hieu suat kem)\\b(?:\\s+\\w+){0,3}\\s+\\b(?:phat|phat tien|tien phat|tru luong|cat luong)\\b"),
            Pattern.compile("\\b(?:kpi|poor performance|low kpi|underperformance)\\b(?:\\s+\\w+){0,2}\\s+\\b(?:fine|fines|penalty|penalties|salary cut|pay cut|deduction)\\b")
    );

    private DeductionPolicy() {
    }

    /** Kiểm tra khoản thu nhập/khấu trừ; ném 422 PROHIBITED_DEDUCTION nếu là khoản phạt bị cấm. */
    public static void assertAllowed(String itemType, String name, String evidence, String decisionRef, String note) {
        if (isProhibited(itemType, name, evidence, decisionRef, note)) {
            throw BusinessException.unprocessable("PROHIBITED_DEDUCTION",
                    "Không được phép tạo hoặc xác nhận khoản phạt tiền đi trễ/về sớm, phạt KPI hoặc cắt lương mang tính kỷ luật theo quy chế (FR-11, BR-14)");
        }
    }

    public static boolean isProhibited(String itemType, String name, String evidence, String decisionRef, String note) {
        if (itemType == null || !DEDUCTION_TYPES.contains(itemType)) {
            return false;
        }
        return containsProhibitedPhrase(name)
                || containsProhibitedPhrase(evidence)
                || containsProhibitedPhrase(decisionRef)
                || containsProhibitedPhrase(note);
    }

    static boolean containsProhibitedPhrase(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        for (String rawClause : CLAUSE_SPLIT.split(raw)) {
            String clause = normalizeClause(rawClause);
            if (clause.isEmpty()) {
                continue;
            }
            for (Pattern p : PROHIBITED_PATTERNS) {
                Matcher m = p.matcher(clause);
                while (m.find()) {
                    String prefix = clause.substring(0, m.start());
                    if (!NEGATION_PREFIX.matcher(prefix).find()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static String normalizeClause(String raw) {
        String replaced = raw.replace('đ', 'd').replace('Đ', 'D');
        String nfd = Normalizer.normalize(replaced, Normalizer.Form.NFD);
        String ascii = COMBINING_MARKS.matcher(nfd).replaceAll("").toLowerCase(Locale.ROOT);
        return NON_ALNUM.matcher(ascii).replaceAll(" ").trim();
    }
}
