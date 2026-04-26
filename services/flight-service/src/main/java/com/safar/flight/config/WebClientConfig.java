package com.safar.flight.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${amadeus.base-url}")
    private String amadeusBaseUrl;

    @Value("${duffel.base-url:https://api.duffel.com}")
    private String duffelBaseUrl;

    @Value("${tbo.base-url:https://Affiliate.tektravels.com}")
    private String tboBaseUrl;

    @Bean
    public WebClient amadeusWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(amadeusBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient duffelWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(duffelBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient tboWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(tboBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
