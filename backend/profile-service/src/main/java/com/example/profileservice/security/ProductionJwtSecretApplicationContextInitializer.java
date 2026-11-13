package com.example.profileservice.security;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class ProductionJwtSecretApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ProductionJwtSecretValidator.requireValidSecret(applicationContext.getEnvironment());
    }
}
