package com.example.insurance.ocr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClaimConsistencyService {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?i)(?:total|amount|montant|sum)[^0-9]{0,12}([0-9]+(?:[.,][0-9]{1,2})?)");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{2}-\\d{2}-\\d{4})\\b");
    private static final Pattern PROVIDER_PATTERN = Pattern.compile("(?i)(?:pharmacy|pharmacie|provider|clinic|hospital)\\s*[:\\-]?\\s*([A-Za-z0-9 .,'-]{3,60})");

    private final double majorAmountDiffThreshold;
    private final double minorAmountDiffThreshold;

    public ClaimConsistencyService(
            @Value("${app.ocr.amount-major-diff-threshold:10.0}") double majorAmountDiffThreshold,
            @Value("${app.ocr.amount-minor-diff-threshold:2.0}") double minorAmountDiffThreshold
    ) {
        this.majorAmountDiffThreshold = majorAmountDiffThreshold;
        this.minorAmountDiffThreshold = minorAmountDiffThreshold;
    }

    public OcrConsistencyResult analyze(String extractedText, Double submittedAmount, String submittedInsuranceCompany) {
        String safeText = extractedText == null ? "" : extractedText.trim();
        List<OcrMismatchDetail> mismatches = new ArrayList<>();

        Double extractedAmount = parseAmount(safeText);
        String extractedDate = parseDate(safeText);
        String extractedProvider = parseProvider(safeText);

        if (safeText.isBlank()) {
            mismatches.add(mismatch(
                    "OCR_EMPTY_TEXT",
                    "documents",
                    "Readable text",
                    "No readable text",
                    OcrMismatchSeverity.MAJOR,
                    "No readable text was found in uploaded documents."
            ));
        }

        if (submittedAmount != null) {
            if (extractedAmount == null) {
                mismatches.add(mismatch(
                        "MISSING_AMOUNT",
                        "amount",
                        submittedAmount.toString(),
                        null,
                        OcrMismatchSeverity.MINOR,
                        "Could not extract invoice amount from uploaded documents."
                ));
            } else {
                double diff = Math.abs(submittedAmount - extractedAmount);
                if (diff > majorAmountDiffThreshold) {
                    mismatches.add(mismatch(
                            "AMOUNT_MISMATCH_MAJOR",
                            "amount",
                            submittedAmount.toString(),
                            extractedAmount.toString(),
                            OcrMismatchSeverity.MAJOR,
                            "Invoice amount differs significantly from submitted amount."
                    ));
                } else if (diff > minorAmountDiffThreshold) {
                    mismatches.add(mismatch(
                            "AMOUNT_MISMATCH_MINOR",
                            "amount",
                            submittedAmount.toString(),
                            extractedAmount.toString(),
                            OcrMismatchSeverity.MINOR,
                            "Invoice amount slightly differs from submitted amount."
                    ));
                }
            }
        }

        if (submittedInsuranceCompany != null && !submittedInsuranceCompany.isBlank()) {
            String normalizedCompany = submittedInsuranceCompany.trim().toLowerCase(Locale.ROOT);
            if (!safeText.toLowerCase(Locale.ROOT).contains(normalizedCompany)) {
                mismatches.add(mismatch(
                        "INSURANCE_COMPANY_NOT_FOUND",
                        "insuranceCompany",
                        submittedInsuranceCompany,
                        extractedProvider,
                        OcrMismatchSeverity.MINOR,
                        "Insurance company was not clearly found in the uploaded document text."
                ));
            }
        }

        if (extractedDate != null) {
            LocalDate parsed = parseDateValue(extractedDate);
            if (parsed != null) {
                if (parsed.isAfter(LocalDate.now())) {
                    mismatches.add(mismatch(
                            "INVOICE_DATE_IN_FUTURE",
                            "invoiceDate",
                            "Not in future",
                            extractedDate,
                            OcrMismatchSeverity.MAJOR,
                            "Invoice date appears to be in the future."
                    ));
                } else if (parsed.isBefore(LocalDate.now().minusYears(2))) {
                    mismatches.add(mismatch(
                            "INVOICE_DATE_OLD",
                            "invoiceDate",
                            "Within the last 2 years",
                            extractedDate,
                            OcrMismatchSeverity.MINOR,
                            "Invoice date is old and requires manual confirmation."
                    ));
                }
            }
        } else {
            mismatches.add(mismatch(
                    "INVOICE_DATE_NOT_FOUND",
                    "invoiceDate",
                    "Invoice date",
                    null,
                    OcrMismatchSeverity.MINOR,
                    "Could not extract an invoice date from uploaded documents."
            ));
        }

        OcrMismatchSeverity finalSeverity = mismatches.stream().anyMatch(m -> m.getSeverity() == OcrMismatchSeverity.MAJOR)
                ? OcrMismatchSeverity.MAJOR
                : (mismatches.isEmpty() ? OcrMismatchSeverity.NONE : OcrMismatchSeverity.MINOR);

        String summary = mismatches.isEmpty()
                ? "OCR consistency checks passed."
                : mismatches.stream()
                .map(OcrMismatchDetail::getMessage)
                .limit(3)
                .reduce((a, b) -> a + " " + b)
                .orElse("OCR consistency checks found mismatches.");

        return OcrConsistencyResult.builder()
                .severity(finalSeverity)
                .mismatches(mismatches)
                .extractedAmount(extractedAmount)
                .extractedInvoiceDate(extractedDate)
                .extractedProviderName(extractedProvider)
                .mismatchSummary(summary)
                .build();
    }

    private OcrMismatchDetail mismatch(
            String code,
            String field,
            String expected,
            String extracted,
            OcrMismatchSeverity severity,
            String message
    ) {
        return OcrMismatchDetail.builder()
                .code(code)
                .field(field)
                .expectedValue(expected)
                .extractedValue(extracted)
                .severity(severity)
                .message(message)
                .build();
    }

    private Double parseAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String parseDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String parseProvider(String text) {
        Matcher matcher = PROVIDER_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private LocalDate parseDateValue(String dateRaw) {
        if (dateRaw == null || dateRaw.isBlank()) {
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateRaw, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format.
            }
        }
        return null;
    }
}
