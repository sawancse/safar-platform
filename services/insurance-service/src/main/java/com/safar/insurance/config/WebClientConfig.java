package com.safar.insurance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${acko.base-url:https://api.acko.com}")
    private String ackoBaseUrl;

    @Value("${icici-lombard.base-url:https://api.icicilombard.com}")
    private String iciciLombardBaseUrl;

    @Bean
    public WebClient ackoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(ackoBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public WebClient iciciLombardWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(iciciLombardBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
