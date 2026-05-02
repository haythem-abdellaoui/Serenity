package com.example.insurance.exception;

import com.example.insurance.ocr.OcrMismatchDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class OcrMajorMismatchException extends RuntimeException {
    private final List<OcrMismatchDetail> mismatches;

    public OcrMajorMismatchException(String message, List<OcrMismatchDetail> mismatches) {
        super(message);
        this.mismatches = mismatches;
    }
}
