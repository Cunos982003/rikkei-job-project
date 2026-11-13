package com.example.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jwt.secret=test-only-signing-key-not-for-production")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
