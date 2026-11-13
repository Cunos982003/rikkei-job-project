package com.example.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    private GatewayMvcProperties gatewayMvcProperties;

    @Test
    void contextLoadsAndRoutesAreConfigured() {
        assertNotNull(gatewayMvcProperties.getRoutes());
        assertFalse(gatewayMvcProperties.getRoutes().isEmpty(), "Routes must not be empty");
        assertTrue(gatewayMvcProperties.getRoutes().stream().anyMatch(r -> "learning-service".equals(r.getId())));
        assertTrue(gatewayMvcProperties.getRoutes().stream().anyMatch(r -> "learning-service-me".equals(r.getId())));
    }
}
