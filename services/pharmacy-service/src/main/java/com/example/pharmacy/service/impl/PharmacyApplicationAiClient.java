package com.example.pharmacy.service.impl;

import com.example.pharmacy.dto.CnoptVerificationResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class PharmacyApplicationAiClient {
    private static final int MIN_VERIFY_TIMEOUT_MS = 3000;
    private static final int MAX_VERIFY_TIMEOUT_MS = 900000;
    private static final int CONNECT_TIMEOUT_CAP_MS = 8000;

    @Value("${app.ai-service.base-url:http://localhost:8096}")
    private String aiServiceBaseUrl;

    @Value("${app.ai-service.internal-api-key:}")
    private String internalApiKey;

    @Value("${app.ai-service.verify-cnopt-path:/internal/pharmacy-applications/verify-cnopt}")
    private String verifyCnoptPath;

    @Value("${app.ai-service.verify-timeout-ms:180000}")
    private int verifyTimeoutMs;

    public CnoptVerificationResultDTO verifyCnoptDocument(byte[] fileBytes, String filename) {
        String url = aiServiceBaseUrl + verifyCnoptPath;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Internal-Key", internalApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(fileBytes, filename));
        body.add("filename", filename);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<CnoptVerificationResultDTO> response = buildRestTemplate().exchange(
            url,
            HttpMethod.POST,
            request,
            CnoptVerificationResultDTO.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("CNOPT verification response body is empty");
        }
        return response.getBody();
    }

    private RestTemplate buildRestTemplate() {
        int readTimeoutMs = resolveReadTimeoutMs();
        int connectTimeoutMs = readTimeoutMs == 0
            ? CONNECT_TIMEOUT_CAP_MS
            : Math.min(CONNECT_TIMEOUT_CAP_MS, readTimeoutMs);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(requestFactory);
    }

    private int resolveReadTimeoutMs() {
        if (verifyTimeoutMs <= 0) {
            return 0;
        }
        return Math.min(MAX_VERIFY_TIMEOUT_MS, Math.max(MIN_VERIFY_TIMEOUT_MS, verifyTimeoutMs));
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
