package com.myce.api.config;

import com.myce.api.auth.filter.InternalHeaderKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    public RestClient restClient(@Value("${internal.core.url}") String coreUrl,
            @Value("${internal.core.value}") String internalHeader) {
        return RestClient.builder()
                .baseUrl(coreUrl)
                .defaultHeader(InternalHeaderKey.INTERNAL_AUTH, internalHeader)
                .build();
    }
}
