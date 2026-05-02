package com.example.healthcare.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.google.oauth2")
public class GoogleOAuthProperties {

    private String clientId = "";
    private String clientSecret = "";
    /** Must match an authorized redirect URI in Google Cloud Console exactly. */
    private String redirectUri = "http://localhost:4200/auth/google/callback";
}
