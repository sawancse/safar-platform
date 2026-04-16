package com.safar.flight.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures WebClient for Amadeus Self-Service APIs.
 * Auth: OAuth2 Client Credentials (client_id + client_secret → Bearer token).
 * Docs: https://developers.amadeus.com/self-service
 */
@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${amadeus.base-url}")
    private String amadeusBaseUrl;

    @Bean
    public WebClient amadeusWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(amadeusBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
