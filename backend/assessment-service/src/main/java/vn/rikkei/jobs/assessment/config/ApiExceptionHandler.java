package vn.rikkei.jobs.assessment.config;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.rikkei.jobs.assessment.form.domain.EvaluationAccessDeniedException;
import vn.rikkei.jobs.assessment.form.domain.EvaluationNotEditableException;
import vn.rikkei.jobs.assessment.form.domain.EvaluationNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EvaluationNotFoundException.class)
    ProblemDetail handleNotFound(EvaluationNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Not Found");
        detail.setType(URI.create("about:blank"));
        return detail;
    }

    @ExceptionHandler(EvaluationAccessDeniedException.class)
    ProblemDetail handleAccessDenied(EvaluationAccessDeniedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setTitle("Forbidden");
        return detail;
    }

    @ExceptionHandler(EvaluationNotEditableException.class)
    ProblemDetail handleNotEditable(EvaluationNotEditableException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Conflict");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ");
        detail.setTitle("Bad Request");
        detail.setProperty(
                "errors",
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .toList());
        return detail;
    }
}
