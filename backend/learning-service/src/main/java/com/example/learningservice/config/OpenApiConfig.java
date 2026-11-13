package com.example.learningservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI learningServiceOpenAPI(
            @Value("${server.port:8089}") int serverPort,
            @Value("${app.gateway-url:http://localhost:8080}") String gatewayUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("Learning Service API - Module Đào tạo bổ sung (DT-01)")
                        .version("1.0.0")
                        .description("Tài liệu OpenAPI phục vụ Phân tích khoảng thiếu hụt năng lực theo JD và Vị trí mục tiêu. " +
                                "Hỗ trợ tích hợp qua API Gateway cổng 8080 và gọi trực tiếp dịch vụ tại cổng " + serverPort + ".")
                        .contact(new Contact()
                                .name("Rikkei Jobs Engineering Team")
                                .email("engineering@rikkeijobs.vn")))
                .servers(List.of(
                        new Server().url(gatewayUrl).description("API Gateway (Cổng chính thức: 8080)"),
                        new Server().url("http://localhost:" + serverPort).description("Learning Microservice trực tiếp (Cổng nội bộ: " + serverPort + ")")
                ));
    }
}

