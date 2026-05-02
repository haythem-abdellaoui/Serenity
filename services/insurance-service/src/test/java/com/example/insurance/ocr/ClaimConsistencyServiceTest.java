package com.example.insurance.ocr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimConsistencyServiceTest {

    private final ClaimConsistencyService service = new ClaimConsistencyService(10.0, 2.0);

    @Test
    void shouldReturnMajorWhenAmountDifferenceIsHigh() {
        String text = "Invoice Total: 100.00 Pharmacy: Insurance 3 Date: 2025-01-05";
        OcrConsistencyResult result = service.analyze(text, 130.0, "Insurance 3");

        assertEquals(OcrMismatchSeverity.MAJOR, result.getSeverity());
        assertTrue(result.getMismatches().stream().anyMatch(m -> "AMOUNT_MISMATCH_MAJOR".equals(m.getCode())));
    }

    @Test
    void shouldReturnMinorWhenAmountDifferenceIsSmall() {
        String text = "Total amount 100.00 Pharmacy Insurance 3 2025-01-05";
        OcrConsistencyResult result = service.analyze(text, 103.0, "Insurance 3");

        assertEquals(OcrMismatchSeverity.MINOR, result.getSeverity());
        assertTrue(result.getMismatches().stream().anyMatch(m -> "AMOUNT_MISMATCH_MINOR".equals(m.getCode())));
    }

    @Test
    void shouldReturnNoneWhenDataIsConsistent() {
        String text = "Total: 100.00 Pharmacy Insurance 5 Date 2025-01-05";
        OcrConsistencyResult result = service.analyze(text, 100.0, "Insurance 5");

        assertEquals(OcrMismatchSeverity.NONE, result.getSeverity());
        assertTrue(result.getMismatches().isEmpty());
    }

    @Test
    void shouldReturnMajorWhenNoTextExtracted() {
        OcrConsistencyResult result = service.analyze("", 100.0, "Insurance 2");
        assertEquals(OcrMismatchSeverity.MAJOR, result.getSeverity());
        assertTrue(result.getMismatches().stream().anyMatch(m -> "OCR_EMPTY_TEXT".equals(m.getCode())));
    }
}
