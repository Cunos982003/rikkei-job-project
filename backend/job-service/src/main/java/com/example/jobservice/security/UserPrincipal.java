
package com.example.jobservice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String role;
    private final UUID companyId;
    private final String password;

    public UserPrincipal(
            UUID id,
            String email,
            String fullName,
            String role,
            UUID companyId,
            String password) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.companyId = companyId;
        this.password = password;
    }

    public static UserPrincipal of(
            UUID id,
            String email,
            String fullName,
            String role,
            UUID companyId) {
        return new UserPrincipal(
                id, email, fullName, role, companyId, "");
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;

        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
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
