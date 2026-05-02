package configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
public class GatewayConfig {
    private final JwtGatewayFilter jwtGatewayFilter;

    public GatewayConfig(JwtGatewayFilter jwtGatewayFilter) {
        this.jwtGatewayFilter = jwtGatewayFilter;
    }

    @Bean
    public WebFilter jwtFilter() {
        return jwtGatewayFilter;
    }
}
