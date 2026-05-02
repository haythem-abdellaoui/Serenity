package com.example.pharmacy.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PharmacyApplicationDocumentStorageServiceTest {

    @Test
    void storeDocument_rejectsWrongExtension(@TempDir Path tmp) {
        PharmacyApplicationDocumentStorageService service = new PharmacyApplicationDocumentStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tmp.toString());

        MockMultipartFile file = new MockMultipartFile("f", "a.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> service.storeDocument(1L, "cin", file, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF");
    }

    @Test
    void storeDocument_writesFileAndReturnsRelativePath(@TempDir Path tmp) throws IOException {
        PharmacyApplicationDocumentStorageService service = new PharmacyApplicationDocumentStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tmp.toString());

        MockMultipartFile file = new MockMultipartFile("f", "a.pdf", "application/pdf", new byte[]{1, 2, 3});

        String stored = service.storeDocument(1L, "cin", file, null);
        assertThat(stored).contains("pharmacy-applications/1/cin-");

        Path abs = tmp.resolve(stored);
        assertThat(Files.exists(abs)).isTrue();

        assertThat(service.resolveContentType(stored)).isNotBlank();
        assertThat(service.loadDocument(stored)).isNotNull();
    }
}

