package com.serenity.monitoring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.monitoring-ai")
public class MonitoringAiProperties {

    /**
     * When false, mood entries are saved without calling the Python service.
     */
    private boolean enabled = true;

    /**
     * Base URL of the FastAPI monitoring-ai service (default port 5150).
     */
    private String url = "http://localhost:5150";
}
