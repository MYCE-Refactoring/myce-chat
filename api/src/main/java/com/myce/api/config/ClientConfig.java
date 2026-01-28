package com.myce.api.config;

import com.myce.api.auth.filter.InternalHeaderKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder lbRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient restClient(
            RestClient.Builder lbRestClientBuilder,
            @Value("${internal.core.url}") String coreUrl,
            @Value("${internal.core.value}") String internalHeader) {
        return lbRestClientBuilder
                .baseUrl(coreUrl)
                .defaultHeader(InternalHeaderKey.INTERNAL_AUTH, internalHeader)
                .build();
    }
}
