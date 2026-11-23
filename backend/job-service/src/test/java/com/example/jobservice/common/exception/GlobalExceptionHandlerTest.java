package com.example.jobservice.common.exception;

import com.example.jobservice.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void dataIntegrityViolationReturnsConflict() {
        ResponseEntity<ApiResponse<Void>> response = new GlobalExceptionHandler()
                .handleDataIntegrityViolation(new DataIntegrityViolationException("duplicate key"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().statusCode()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("Dữ liệu đã tồn tại");
    }
}
