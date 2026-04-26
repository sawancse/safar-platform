package com.safar.services;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(info = @Info(title = "Safar Services API", version = "v1"))
@SpringBootApplication
@EnableScheduling
public class ServicesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicesServiceApplication.class, args);
    }
}
