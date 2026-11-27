package com.example.authservice.metrics;

public enum AuthEndpoint {
    LOGIN("login"),
    HR_LOGIN("hr_login"),
    SSO("sso");

    private final String value;

    AuthEndpoint(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AuthEndpoint fromUri(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.endsWith("/sso") || uri.contains("/sso/")) {
            return SSO;
        }
        if (uri.endsWith("/hr/login") || uri.contains("/hr/login/") || uri.endsWith("/hr-login") || uri.contains("/hr-login/")) {
            return HR_LOGIN;
        }
        if (uri.endsWith("/login") || uri.contains("/login/")) {
            return LOGIN;
        }
        return null;
    }
}
