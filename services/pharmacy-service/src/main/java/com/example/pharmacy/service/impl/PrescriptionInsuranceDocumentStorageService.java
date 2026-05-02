package com.example.pharmacy.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PrescriptionInsuranceDocumentStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String storeDocument(Long prescriptionId, MultipartFile file, String previousPath) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Prescription document file is required");
        }

        String extension = extractSafeExtension(file.getOriginalFilename());
        Path prescriptionDir = getUploadRoot()
            .resolve("pharmacy-prescriptions")
            .resolve(String.valueOf(prescriptionId));
        Path target = prescriptionDir.resolve("insurance-document-" + UUID.randomUUID() + "." + extension).normalize();

        try {
            Files.createDirectories(prescriptionDir);
            file.transferTo(target.toFile());
            deleteIfExists(previousPath);
            return toStoredRelativePath(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store insurance prescription document");
        }
    }

    public Resource loadDocument(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            throw new IllegalStateException("No insurance prescription document was uploaded");
        }

        Path resolved = resolveFromStoredPath(storedPath);
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new IllegalStateException("Insurance prescription document file not found");
        }

        try {
            return new UrlResource(resolved.toUri());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Unable to load insurance prescription document");
        }
    }

    public String resolveContentType(String storedPath) {
        try {
            String detected = Files.probeContentType(resolveFromStoredPath(storedPath));
            return StringUtils.hasText(detected) ? detected : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    public String resolveFileName(String storedPath, Long prescriptionId) {
        String extension = "jpg";
        if (StringUtils.hasText(storedPath)) {
            int dot = storedPath.lastIndexOf('.');
            if (dot >= 0 && dot < storedPath.length() - 1) {
                extension = storedPath.substring(dot + 1);
            }
        }
        return "prescription-" + prescriptionId + "-insurance." + extension;
    }

    private void deleteIfExists(String storedPath) {
        if (!StringUtils.hasText(storedPath)) {
            return;
        }

        try {
            Files.deleteIfExists(resolveFromStoredPath(storedPath));
        } catch (IOException ignored) {
            // Ignore replacement cleanup failure.
        }
    }

    private Path getUploadRoot() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private Path resolveFromStoredPath(String storedPath) {
        Path uploadRoot = getUploadRoot();
        Path resolved = uploadRoot.resolve(storedPath.replace("\\", "/")).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid insurance prescription document path");
        }
        return resolved;
    }

    private String toStoredRelativePath(Path absolutePath) {
        Path relative = getUploadRoot().relativize(absolutePath.toAbsolutePath().normalize());
        return relative.toString().replace("\\", "/");
    }

    private String extractSafeExtension(String originalName) {
        String safeName = StringUtils.hasText(originalName) ? originalName.trim() : "";
        int lastDot = safeName.lastIndexOf('.');
        String extension = lastDot >= 0 ? safeName.substring(lastDot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Only PNG, JPG, or JPEG images are allowed");
        }
        return extension;
    }
}
