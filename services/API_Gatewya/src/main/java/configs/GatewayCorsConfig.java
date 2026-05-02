package configs;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayCorsConfig {

    // Intentionally left blank:
    // CORS is handled by downstream services (user-service, appointment-service)
    // to avoid duplicate Access-Control-Allow-Origin headers being added by both gateway and services.
}
