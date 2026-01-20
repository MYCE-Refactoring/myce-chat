package com.myce.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    public RestClient restClient(@Value("${internal.core.url}") String coreUrl) {
        return RestClient.builder()
                .baseUrl(coreUrl)
                .build();
    }
}
