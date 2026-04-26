package com.safar.insurance;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(title = "Safar Insurance API", version = "v1"))
@SpringBootApplication
public class InsuranceServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsuranceServiceApplication.class, args);
    }
}
