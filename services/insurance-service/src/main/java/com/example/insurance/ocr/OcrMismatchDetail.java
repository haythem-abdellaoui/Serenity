package com.example.insurance.ocr;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OcrMismatchDetail {
    private final String code;
    private final String field;
    private final String expectedValue;
    private final String extractedValue;
    private final OcrMismatchSeverity severity;
    private final String message;
}
