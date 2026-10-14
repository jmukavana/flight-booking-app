package com.resilire.booking.infrastructure.web;

import com.resilire.booking.domain.order.OfferNotFoundException;
import com.resilire.booking.domain.order.OrderNotFoundException;
import com.resilire.booking.domain.order.PaymentDeclinedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates domain/infrastructure failures into RFC 9457 problem responses. */
@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail onOrderNotFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OfferNotFoundException.class)
    ProblemDetail onOfferNotFound(OfferNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    ProblemDetail onPaymentDeclined(PaymentDeclinedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MissingRequestHeaderException.class,
            IllegalArgumentException.class})
    ProblemDetail onBadRequest(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onDuplicatePlacement(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Concurrent order placement detected, please retry");
    }
}
