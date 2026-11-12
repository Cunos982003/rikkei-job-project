package com.example.jobservice.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void dataIntegrityViolationReturnsConflict() {
        ProblemDetail problem = new GlobalExceptionHandler()
                .handleDataIntegrityViolation(new DataIntegrityViolationException("duplicate key"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).contains("Dữ liệu đã tồn tại");
    }
}
