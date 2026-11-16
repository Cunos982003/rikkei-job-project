package com.example.recruitment_service.security;

import java.util.UUID;

public record UserPrincipal(UUID id, String role, UUID companyId) {
}
