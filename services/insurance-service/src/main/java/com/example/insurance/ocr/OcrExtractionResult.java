package com.example.insurance.ocr;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OcrExtractionResult {
    private final String combinedText;
    private final List<String> perFileSnippets;
    private final int analyzedFileCount;
}
