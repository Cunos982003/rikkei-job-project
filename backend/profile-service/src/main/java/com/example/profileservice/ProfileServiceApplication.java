package com.example.profileservice;

import com.example.profileservice.security.ProductionJwtSecretApplicationContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ProfileServiceApplication.class);
        springApplication.addInitializers(new ProductionJwtSecretApplicationContextInitializer());
        springApplication.run(args);
    }

}
