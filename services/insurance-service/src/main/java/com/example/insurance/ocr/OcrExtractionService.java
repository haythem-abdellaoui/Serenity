package com.example.insurance.ocr;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class OcrExtractionService {
    private final int maxPagesPerPdf;
    private final int maxCharsPerFile;
    private final String ocrLanguage;
    private final String tesseractDatapath;

    public OcrExtractionService(
            @Value("${app.ocr.max-pages-per-pdf:3}") int maxPagesPerPdf,
            @Value("${app.ocr.max-chars-per-file:12000}") int maxCharsPerFile,
            @Value("${app.ocr.tesseract.language:eng}") String ocrLanguage,
            @Value("${app.ocr.tesseract.datapath:}") String tesseractDatapath
    ) {
        this.maxPagesPerPdf = maxPagesPerPdf;
        this.maxCharsPerFile = maxCharsPerFile;
        this.ocrLanguage = ocrLanguage;
        this.tesseractDatapath = tesseractDatapath == null ? "" : tesseractDatapath.trim();
    }

    public OcrExtractionResult extractFromFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return OcrExtractionResult.builder()
                    .combinedText("")
                    .perFileSnippets(List.of())
                    .analyzedFileCount(0)
                    .build();
        }

        List<String> snippets = new ArrayList<>();
        StringBuilder allText = new StringBuilder();
        int analyzed = 0;
        for (String rawPath : filePaths) {
            if (rawPath == null || rawPath.isBlank()) {
                continue;
            }
            Path path = Path.of(rawPath);
            if (!Files.exists(path)) {
                continue;
            }
            String text = extractSingleFile(path);
            if (!text.isBlank()) {
                String normalized = normalize(text);
                snippets.add(truncate(normalized, maxCharsPerFile));
                allText.append("\n").append(normalized);
            }
            analyzed++;
        }

        return OcrExtractionResult.builder()
                .combinedText(truncate(allText.toString(), maxCharsPerFile * 3))
                .perFileSnippets(snippets)
                .analyzedFileCount(analyzed)
                .build();
    }

    private String extractSingleFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (fileName.endsWith(".pdf")) {
                return extractPdf(path);
            }
            if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return runTesseract(path);
            }
            return "";
        } catch (IOException | TesseractException | LinkageError ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OCR failed for file: " + path.getFileName() + ". Please upload a clearer document."
            );
        }
    }

    private String extractPdf(Path path) throws IOException, TesseractException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            int pages = Math.min(document.getNumberOfPages(), maxPagesPerPdf);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(pages);
            String extracted = stripper.getText(document);
            if (extracted != null && !extracted.trim().isEmpty()) {
                return extracted;
            }

            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 200, ImageType.RGB);
                sb.append(runTesseract(image)).append("\n");
            }
            return sb.toString();
        }
    }

    private String runTesseract(Path imagePath) throws TesseractException {
        Tesseract tesseract = buildTesseract();
        return tesseract.doOCR(imagePath.toFile());
    }

    private String runTesseract(BufferedImage image) throws TesseractException {
        Tesseract tesseract = buildTesseract();
        return tesseract.doOCR(image);
    }

    private Tesseract buildTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setLanguage(ocrLanguage);
        if (!tesseractDatapath.isBlank()) {
            tesseract.setDatapath(tesseractDatapath);
        }
        return tesseract;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace('\r', '\n').replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, max);
    }
}
