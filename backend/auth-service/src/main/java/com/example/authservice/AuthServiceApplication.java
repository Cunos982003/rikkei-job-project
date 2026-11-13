package com.example.authservice;

import com.example.authservice.security.ProductionJwtSecretApplicationContextInitializer;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "Auth Service API",
        version = "1.0",
        description = "Authentication and SSO endpoints for Rikkei Jobs"
    )
)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(AuthServiceApplication.class);
        springApplication.addInitializers(new ProductionJwtSecretApplicationContextInitializer());
        springApplication.run(args);
    }
}