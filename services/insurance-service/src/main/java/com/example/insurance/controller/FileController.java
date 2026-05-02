package com.example.insurance.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping("/{folder}/{userId}/{filename:.+}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String folder,
            @PathVariable String userId,
            @PathVariable String filename
    ) {
        try {
            if (!StringUtils.hasText(folder) || !StringUtils.hasText(userId) || !StringUtils.hasText(filename)) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(uploadDir, folder, userId, filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return buildFileResponse(filePath, resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/open")
    public ResponseEntity<Resource> openByStoredPath(@RequestParam("path") String storedPath) {
        try {
            if (!StringUtils.hasText(storedPath)) {
                return ResponseEntity.badRequest().build();
            }

            // Normalize separators from DB and build robust candidate paths.
            String normalized = storedPath.replace("\\", "/");
            Path direct = Paths.get(normalized).normalize();
            Path uploadRelative = Paths.get(uploadDir).resolve(normalized).normalize();

            // Backward-compatible fallback when only "claims/..." is kept in DB.
            int claimsIdx = normalized.indexOf("claims/");
            Path claimsRelative = claimsIdx >= 0
                    ? Paths.get(uploadDir).resolve(normalized.substring(claimsIdx)).normalize()
                    : null;

            Path resolved = firstExistingReadable(direct, uploadRelative, claimsRelative);
            if (resolved == null) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(resolved.toUri());
            return buildFileResponse(resolved, resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Resource> buildFileResponse(Path path, Resource resource) {
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String contentType = Files.probeContentType(path);
            if (StringUtils.hasText(contentType)) {
                mediaType = MediaType.parseMediaType(contentType);
            }
        } catch (Exception ignored) {
            // keep octet-stream fallback
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private Path firstExistingReadable(Path... candidates) {
        if (candidates == null) {
            return null;
        }
        for (Path candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            try {
                if (Files.exists(candidate) && Files.isRegularFile(candidate) && Files.isReadable(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
                // Try next candidate
            }
        }
        return null;
    }
}

