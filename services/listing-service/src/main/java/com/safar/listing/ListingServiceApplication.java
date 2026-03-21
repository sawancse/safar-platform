package com.safar.listing;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(info = @Info(title = "Safar Listing API", version = "v1"))
@SpringBootApplication
@EnableScheduling
public class ListingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ListingServiceApplication.class, args);
    }
}
