package com.example.pharmacy.service;

import org.springframework.core.io.Resource;

public record PrescriptionInsuranceDocumentPayload(
    Resource resource,
    String contentType,
    String fileName
) {}
