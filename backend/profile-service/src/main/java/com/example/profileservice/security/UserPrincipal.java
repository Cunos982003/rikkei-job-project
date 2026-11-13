package com.example.profileservice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID userId, String username, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.authorities = authorities;
        this.username = username;
    }

    public static UserPrincipal of(UUID userId, String username, String role) {
        String normalizedRole = role == null ? null : role.trim();
        if (normalizedRole == null || normalizedRole.isBlank()) {
            throw new IllegalArgumentException("Role must not be null or blank");
        }
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }
        return new UserPrincipal(userId, username, List.of(new SimpleGrantedAuthority(normalizedRole)));
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
