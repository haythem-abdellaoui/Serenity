package com.serenity.monitoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MonitoringAiRestClientConfig {

    @Bean
    public RestClient monitoringAiRestClient(RestClient.Builder builder, MonitoringAiProperties props) {
        return builder
                .baseUrl(props.getUrl())
                .build();
    }
}
