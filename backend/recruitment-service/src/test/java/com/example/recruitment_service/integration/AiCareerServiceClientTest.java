package com.example.recruitment_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.example.recruitment_service.exception.ApiException;

class AiCareerServiceClientTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID JOB_ID = UUID.randomUUID();
    private static final String BASE_URL = "http://ai-career.test";
    private static final String INTERNAL_TOKEN = "private-service-token";

    @Test
    void forwardsInternalTokenAndReturnsMatchScore() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/internal/v1/recruitments/" + USER_ID + "/jobs/" + JOB_ID + "/match-score"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", INTERNAL_TOKEN))
                .andRespond(withSuccess(
                        "{\"userId\":\"" + USER_ID + "\",\"jobId\":\"" + JOB_ID + "\",\"score\":84.5}",
                        MediaType.APPLICATION_JSON));
        AiCareerServiceClient client = new AiCareerServiceClient(BASE_URL, INTERNAL_TOKEN, builder);

        var response = client.getMatchScore(USER_ID, JOB_ID);

        assertEquals(USER_ID, response.userId());
        assertEquals(JOB_ID, response.jobId());
        assertEquals(84.5, response.score());
        server.verify();
    }

    @Test
    void convertsUpstreamNotFoundToExplicitNotFound() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/internal/v1/recruitments/" + USER_ID + "/jobs/" + JOB_ID + "/match-score"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        AiCareerServiceClient client = new AiCareerServiceClient(BASE_URL, INTERNAL_TOKEN, builder);

        ApiException exception = assertThrows(ApiException.class, () -> client.getMatchScore(USER_ID, JOB_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("NOT_FOUND", exception.getCode());
        server.verify();
    }

    @Test
    void convertsUpstreamServerErrorToBadGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/internal/v1/recruitments/" + USER_ID + "/jobs/" + JOB_ID + "/match-score"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        AiCareerServiceClient client = new AiCareerServiceClient(BASE_URL, INTERNAL_TOKEN, builder);

        ApiException exception = assertThrows(ApiException.class, () -> client.getMatchScore(USER_ID, JOB_ID));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("UPSTREAM_ERROR", exception.getCode());
        server.verify();
    }

    @Test
    void convertsTransportErrorToBadGateway() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/internal/v1/recruitments/" + USER_ID + "/jobs/" + JOB_ID + "/match-score"))
                .andRespond(request -> {
                    throw new ResourceAccessException("connection failed");
                });
        AiCareerServiceClient client = new AiCareerServiceClient(BASE_URL, INTERNAL_TOKEN, builder);

        ApiException exception = assertThrows(ApiException.class, () -> client.getMatchScore(USER_ID, JOB_ID));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("UPSTREAM_ERROR", exception.getCode());
        server.verify();
    }

    @Test
    void doesNotFabricateMissingScore() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/internal/v1/recruitments/" + USER_ID + "/jobs/" + JOB_ID + "/match-score"))
                .andRespond(withSuccess(
                        "{\"userId\":\"" + USER_ID + "\",\"jobId\":\"" + JOB_ID + "\"}",
                        MediaType.APPLICATION_JSON));
        AiCareerServiceClient client = new AiCareerServiceClient(BASE_URL, INTERNAL_TOKEN, builder);

        ApiException exception = assertThrows(ApiException.class, () -> client.getMatchScore(USER_ID, JOB_ID));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("UPSTREAM_ERROR", exception.getCode());
        server.verify();
    }
}
