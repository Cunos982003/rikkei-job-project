package vn.rikkei.jobs.assessment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Temporary auth until auth-service JWKS is available. Gateway/front end forwards identity headers.
 */
@Component
public class DevHeaderAuthenticationFilter extends OncePerRequestFilter {

    private final String userIdHeader;
    private final String emailHeader;
    private final String roleHeader;

    public DevHeaderAuthenticationFilter(
            @Value("${rikkei.security.header-user-id}") String userIdHeader,
            @Value("${rikkei.security.header-user-email}") String emailHeader,
            @Value("${rikkei.security.header-user-role}") String roleHeader) {
        this.userIdHeader = userIdHeader;
        this.emailHeader = emailHeader;
        this.roleHeader = roleHeader;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }

        String userIdRaw = request.getHeader(userIdHeader);
        String email = request.getHeader(emailHeader);
        String role = request.getHeader(roleHeader);

        if (userIdRaw == null || userIdRaw.isBlank() || email == null || email.isBlank() || role == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing identity headers");
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdRaw);
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid user id");
            return;
        }

        MentorPrincipal principal = new MentorPrincipal(userId, email, role);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
