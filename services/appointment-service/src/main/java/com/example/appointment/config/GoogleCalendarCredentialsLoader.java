package com.example.appointment.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fills {@link GoogleCalendarProperties#clientId} / {@code clientSecret} from a downloaded
 * client_secret_*.json when explicit properties are blank.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarCredentialsLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GoogleCalendarProperties properties;

    @PostConstruct
    void loadFromJsonIfNeeded() {
        if (StringUtils.hasText(properties.getClientId()) && StringUtils.hasText(properties.getClientSecret())) {
            return;
        }
        if (!StringUtils.hasText(properties.getCredentialsJsonPath())) {
            return;
        }
        try {
            Path p = Path.of(properties.getCredentialsJsonPath());
            if (!Files.isRegularFile(p)) {
                log.warn("Google Calendar credentials file not found: {}", p);
                return;
            }
            JsonNode root = MAPPER.readTree(Files.newInputStream(p));
            JsonNode web = root.get("web");
            if (web == null || web.isNull()) {
                log.warn("Google Calendar credentials JSON missing 'web' section");
                return;
            }
            if (!StringUtils.hasText(properties.getClientId()) && web.hasNonNull("client_id")) {
                properties.setClientId(web.get("client_id").asText());
            }
            if (!StringUtils.hasText(properties.getClientSecret()) && web.hasNonNull("client_secret")) {
                properties.setClientSecret(web.get("client_secret").asText());
            }
            log.info("Loaded Google Calendar OAuth client id from JSON file");
        } catch (Exception e) {
            log.warn("Could not read Google Calendar credentials JSON: {}", e.getMessage());
        }
    }
}
