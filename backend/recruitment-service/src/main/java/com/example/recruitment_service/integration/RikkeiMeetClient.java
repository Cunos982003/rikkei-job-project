package com.example.recruitment_service.integration;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.recruitment_service.exception.ApiException;

@Component
public class RikkeiMeetClient {
    private static final String CREATE_ROOM_PATH = "/meet/rooms";

    private final RestClient restClient;
    private final String serviceKey;

    @Autowired
    public RikkeiMeetClient(
            @Value("${app.integrations.meet-service-url:http://localhost:9000}") String baseUrl,
            @Value("${app.integrations.meet-service-key:}") String serviceKey) {
        this(baseUrl, serviceKey, RestClient.builder());
    }

    RikkeiMeetClient(String baseUrl, String serviceKey, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.serviceKey = serviceKey;
    }

    public String createRoom(String title, Instant startAt, Instant endAt) {
        Map<String, Object> body = Map.of(
                "title", title,
                "startAt", startAt.toString(),
                "endAt", endAt.toString());

        MeetRoomResponse response;
        try {
            var request = restClient.post()
                    .uri(CREATE_ROOM_PATH)
                    .contentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(serviceKey)) {
                request = request.header("X-Service-Key", serviceKey);
            }
            response = request
                    .body(body)
                    .retrieve()
                    .body(MeetRoomResponse.class);
        } catch (RestClientException ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to create Rikkei Meet room: " + ex.getMessage(),
                    "MEET_UPSTREAM_ERROR");
        }

        if (response == null || !StringUtils.hasText(response.roomUrl())) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Rikkei Meet returned an invalid roomUrl",
                    "MEET_UPSTREAM_ERROR");
        }
        return response.roomUrl();
    }

    public record MeetRoomResponse(String title, String roomUrl, String startAt, String endAt) {
    }
}
