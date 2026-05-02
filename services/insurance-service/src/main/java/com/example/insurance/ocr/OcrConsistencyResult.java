package com.example.insurance.ocr;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OcrConsistencyResult {
    private final OcrMismatchSeverity severity;
    private final List<OcrMismatchDetail> mismatches;
    private final Double extractedAmount;
    private final String extractedInvoiceDate;
    private final String extractedProviderName;
    private final String mismatchSummary;
}
