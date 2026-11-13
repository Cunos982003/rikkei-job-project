package com.example.jobservice.client;

import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);
    private final RestClient restClient;

    public AuthServiceClient(
        @Value("${app.auth-service.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public boolean existsByEmail(String email) {
        try {
            Boolean exists = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v1/auth/users/check-email")
                    .queryParam("email", email)
                    .build())
                .retrieve()
                .body(Boolean.class);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.warn("Could not check email with auth-service: {}", ex.getMessage());
            return false;
        }
    }

    public User createHrAccount(String email, String fullName, UUID companyId, String tempPassword) {
        try {
            CreateHrUserPayload payload = new CreateHrUserPayload(email, fullName, companyId, tempPassword);
            AuthUserDto dto = restClient.post()
                .uri("/api/v1/auth/users/hr")
                .body(payload)
                .retrieve()
                .onStatus(status -> status.value() == 409, (req, resp) -> {
                    throw new ConflictException("Email " + email + " đã tồn tại trên auth-service");
                })
                .body(AuthUserDto.class);

            if (dto != null) {
                User u = new User();
                u.setId(dto.id());
                u.setEmail(dto.email());
                u.setFullName(dto.fullName());
                u.setRole(dto.role());
                u.setCompanyId(dto.companyId());
                u.setMustChangePassword(dto.mustChangePassword());
                return u;
            }
        } catch (ConflictException ce) {
            throw ce;
        } catch (Exception ex) {
            log.error("Failed to create HR account in auth-service: {}", ex.getMessage());
        }
        return null;
    }

    public record CreateHrUserPayload(String email, String fullName, UUID companyId, String temporaryPassword) {}
    public record AuthUserDto(UUID id, String email, String fullName, String role, UUID companyId, boolean mustChangePassword) {}
}

