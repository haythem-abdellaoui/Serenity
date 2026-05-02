package com.example.pharmacy.service;

import org.springframework.core.io.Resource;

public record PharmacyApplicationDocumentPayload(
    Resource resource,
    String contentType,
    String fileName
) {}
