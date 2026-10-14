package com.resilire.identityconsent.infrastructure.web;

import com.resilire.identityconsent.domain.consent.ConsentNotGrantedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Translates domain/infrastructure failures into RFC 9457 problem responses. */
@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(ConsentNotGrantedException.class)
    ProblemDetail onNotGranted(ConsentNotGrantedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail onUnknownScope(MethodArgumentTypeMismatchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Unknown consent scope: " + e.getValue());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onConcurrentAppend(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Concurrent consent change detected, please retry");
    }
}
