package com.hrflow.income;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.hrflow.common.exception.BusinessException;

class DeductionPolicyTest {

    @Test
    void blocksLatenessAndEarlyLeavePenaltiesAcrossVariants() {
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Phạt đi trễ", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Phat di tre", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "  PHẠT   ĐI   MUỘN  3 lần ", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Trừ lương về sớm", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Late arrival penalty", "Doc-01", "DEC-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Fine for being late", "Doc-01", "DEC-01", null));
        assertTrue(DeductionPolicy.isProhibited("COMPENSATION", "Bồi thường phạt đi trễ", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("ADVANCE_RECOVERY", "Thu hồi phạt đi trễ", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Khoản khấu trừ tháng 11", "Biên bản phạt đi trễ ngày 10/11", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Khoản khấu trừ khác", "BB-01", "QD-01", "Ghi chú: trừ lương đi muộn"));
    }

    @Test
    void blocksDisciplinarySalaryCutsAndKpiPenalties() {
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Cắt lương kỷ luật", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Phạt tiền vi phạm nội quy", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Disciplinary salary cut", "Doc-01", "DEC-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Phạt không đạt KPI", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "Trừ lương KPI kém tháng 11", "BB-01", "QD-01", null));
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION", "KPI penalty Q4", "Doc-01", "DEC-01", null));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> DeductionPolicy.assertAllowed("DEDUCTION", "Phạt đi trễ", "BB-01", "QD-01", null));
        assertTrue("PROHIBITED_DEDUCTION".equals(ex.getCode()));
        assertTrue(ex.getStatus().value() == 422);
    }

    @Test
    void allowsLegitimateDeductionsBonusesAndNegatedNotes() {
        // Bồi thường hư hỏng thiết bị có biên bản kỷ luật -> KHÔNG phải phạt tiền/cắt lương kỷ luật
        assertFalse(DeductionPolicy.isProhibited("COMPENSATION",
                "Bồi thường hư hỏng thiết bị", "Biên bản kỷ luật và xác nhận thiệt hại vật chất", "QD-BT-01", null));
        // Khoản phải thu hợp lệ có chứng từ
        assertFalse(DeductionPolicy.isProhibited("DEDUCTION",
                "Khấu trừ phí đồng phục theo thỏa thuận", "Biên bản thỏa thuận ký 01/09", "QD-DP-01", null));
        assertFalse(DeductionPolicy.isProhibited("DEDUCTION",
                "Khoản phải thu hợp lệ có chứng từ", "Phiếu xác nhận công nợ PT-01", "QD-PT-01", null));
        // Thu hồi tạm ứng hợp lệ
        assertFalse(DeductionPolicy.isProhibited("ADVANCE_RECOVERY",
                "Thu hồi tạm ứng lương tháng 10", "Phiếu chi tạm ứng PC-1001", "TU-2026-10", null));
        // Thưởng chuyên cần không đi trễ & thưởng KPI -> không phải loại khấu trừ
        assertFalse(DeductionPolicy.isProhibited("BONUS", "Thưởng chuyên cần không đi trễ", "QD-01", "QD-01", null));
        assertFalse(DeductionPolicy.isProhibited("BONUS", "Thưởng KPI tháng 11", "QD-02", "QD-02", null));
        assertFalse(DeductionPolicy.isProhibited("REIMBURSEMENT", "Hoàn chi công tác", "HD-01", "QD-03", null));
        // Ghi chú phủ định rõ ràng: không áp dụng phạt tiền đi trễ
        assertDoesNotThrow(() -> DeductionPolicy.assertAllowed("DEDUCTION",
                "Khấu trừ phí đồng phục theo thỏa thuận", "Biên bản thỏa thuận", "QD-DP-02",
                "Ghi chú: không áp dụng phạt tiền đi trễ, chỉ khấu trừ theo thỏa thuận"));
    }

    @Test
    void negationInOneClauseDoesNotHidePenaltyInAnotherClause() {
        assertTrue(DeductionPolicy.isProhibited("DEDUCTION",
                "Khoản khấu trừ", "BB-01", "QD-01",
                "Không áp dụng miễn trừ; phạt đi trễ 50.000đ"));
    }
}
