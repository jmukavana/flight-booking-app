package com.resilire.itinerary.infrastructure.web;

import com.resilire.itinerary.domain.itinerary.ItineraryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates domain failures into RFC 9457 problem responses. */
@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(ItineraryNotFoundException.class)
    ProblemDetail onNotFound(ItineraryNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onBadRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
