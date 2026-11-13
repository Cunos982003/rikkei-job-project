package com.example.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.jwt.secret=test-only-signing-key-not-for-production")
class GatewayIntegrationTest {
    private static final String SECRET = "test-only-signing-key-not-for-production";
    private static final Map<String, HttpServer> UPSTREAMS = startUpstreams();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @MockitoBean
    private StringRedisTemplate redis;

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        UPSTREAMS.forEach((service, server) -> registry.add("GATEWAY_" + service.toUpperCase() + "_URL",
                () -> "http://localhost:" + server.getAddress().getPort()));
    }

    @BeforeEach
    void allowRequests() {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString()))
                .thenReturn(1L);
    }

    @AfterAll
    static void stopUpstream() {
        UPSTREAMS.values().forEach(server -> server.stop(0));
    }

    @Test
    void routesEveryServiceAndPreservesDownstreamPathAndAuthorization() throws Exception {
        String token = token(Instant.now().plusSeconds(60));
        for (String service : List.of("auth", "profile", "evidence", "assessment", "job",
                "recruitment", "notification", "audit", "learning", "ai")) {
            HttpResponse<String> response = send("GET", "/api/services/" + service + "/api/v1/example?x=1", token);
            assertThat(response.statusCode()).as(service).isEqualTo(200);
            assertThat(response.body()).isEqualTo("/api/v1/example?x=1|Bearer " + token);
            assertThat(response.headers().firstValue("X-Test-Upstream")).contains(service);
        }
    }

    @Test
    void routesExistingPathsAndRewritesAiPrefix() throws Exception {
        String token = token(Instant.now().plusSeconds(60));
        assertThat(send("GET", "/api/v1/profiles/me", token).body())
                .isEqualTo("/api/v1/profiles/me|Bearer " + token);
        assertThat(send("POST", "/api/v1/jobs/123/recruit", token).body())
                .isEqualTo("/api/v1/jobs/123/recruit|Bearer " + token);
        assertThat(send("GET", "/recruitments/123/cv-snapshot", token).body())
                .isEqualTo("/api/v1/recruitments/123/cv-snapshot|Bearer " + token);
        assertThat(send("GET", "/ai-career/me/job-recommendations", token).body())
                .isEqualTo("/me/job-recommendations|Bearer " + token);
        assertThat(send("GET", "/me/job-recommendations", token).body())
                .isEqualTo("/me/job-recommendations|Bearer " + token);
    }

    @Test
    void permitsPublicLoginButRejectsMissingInvalidAndExpiredTokensElsewhere() throws Exception {
        assertThat(send("POST", "/api/v1/auth/login", null).statusCode()).isEqualTo(200);
        assertThat(send("POST", "/api/services/job/api/v1/auth/login", null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/profiles/me", null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/profiles/me", "invalid").statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/profiles/me", token(Instant.now().minusSeconds(60))).statusCode())
                .isEqualTo(401);
        String noExpiry = Jwts.builder().subject("user-123")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(send("GET", "/api/v1/profiles/me", noExpiry).statusCode()).isEqualTo(401);
    }

    @Test
    void enforcesIpAndAuthenticatedUserLimits() throws Exception {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString()))
                .thenReturn(1L, 0L);
        HttpResponse<String> response = send("GET", "/api/v1/profiles/me", token(Instant.now().plusSeconds(60)));
        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(response.body()).contains("\"success\":false", "\"statusCode\":429");
        ArgumentCaptor<List<String>> keys = ArgumentCaptor.captor();
        verify(redis, times(2)).execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                keys.capture(), anyString());
        assertThat(keys.getAllValues().get(0)).singleElement().asString().startsWith("gateway:rate:ip:");
        assertThat(keys.getAllValues().get(1)).singleElement().asString().startsWith("gateway:rate:user:");
    }

    @Test
    void rateLimitsInvalidTokensByIp() throws Exception {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString()))
                .thenReturn(0L);
        assertThat(send("GET", "/api/v1/profiles/me", "invalid").statusCode()).isEqualTo(429);
    }

    @Test
    void blocksUnsafeInternshipImportOnBothProfileRoutes() throws Exception {
        String token = token(Instant.now().plusSeconds(60));
        assertThat(send("GET", "/api/v1/profiles/me/internships", token).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/services/profile/api/v1/profiles/me/internships", token).statusCode())
                .isEqualTo(403);
    }

    @Test
    void returnsUnavailableWhenRedisIsDown() throws Exception {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("unavailable"));
        assertThat(send("POST", "/api/v1/auth/login", null).statusCode()).isEqualTo(503);
    }

    @Test
    void reportsUpstreamConnectionFailureAsServerError() throws Exception {
        HttpResponse<String> response = send("GET", "/api/services/audit/unavailable-test",
                token(Instant.now().plusSeconds(60)));
        assertThat(response.statusCode()).isBetween(500, 599);
    }

    @Test
    void acceptsConfiguredCorsPreflight() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/profiles/me"))
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "X-User-Id, Authorization")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("http://localhost:5173");
    }

    @Test
    void replacesClientSuppliedIdentityWithVerifiedJwtSubject() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/profiles/identity-test"))
                .header("Authorization", "Bearer " + token(Instant.now().plusSeconds(60)))
                .header("X-User-Id", "spoofed-user")
                .GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("user-123");
    }

    @Test
    void allowsCorsResponseFromGatewayAndUpstream() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/profiles/me"))
                .header("Origin", "http://localhost:5173")
                .header("Authorization", "Bearer " + token(Instant.now().plusSeconds(60)))
                .GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues("Access-Control-Allow-Origin"))
                .containsExactly("http://localhost:5173");
    }

    private HttpResponse<String> send(String method, String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        request.method(method, HttpRequest.BodyPublishers.noBody());
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String token(Instant expiry) {
        return Jwts.builder().subject("user-123").expiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private static Map<String, HttpServer> startUpstreams() {
        Map<String, HttpServer> servers = new LinkedHashMap<>();
        for (String service : List.of("auth", "profile", "evidence", "assessment", "job",
                "recruitment", "notification", "audit", "learning", "ai")) {
            servers.put(service, startUpstream(service));
        }
        return servers;
    }

    private static HttpServer startUpstream(String service) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                if (exchange.getRequestURI().getPath().equals("/unavailable-test")) {
                    exchange.close();
                    return;
                }
                exchange.getResponseHeaders().set("X-Test-Upstream", service);
                if (exchange.getRequestHeaders().getFirst("Origin") != null) {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:5173");
                }
                String body = exchange.getRequestURI().getPath().endsWith("/identity-test")
                        ? String.valueOf(exchange.getRequestHeaders().getFirst("X-User-Id"))
                        : exchange.getRequestURI() + "|" + exchange.getRequestHeaders().getFirst("Authorization");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot start test upstream", ex);
        }
    }
}
