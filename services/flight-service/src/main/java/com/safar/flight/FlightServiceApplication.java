package com.safar.flight;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(info = @Info(title = "Safar Flight API", version = "v1"))
@SpringBootApplication
@EnableScheduling
public class FlightServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlightServiceApplication.class, args);
    }
}
