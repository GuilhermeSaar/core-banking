package com.corebaking.shared.handler;

import com.corebaking.shared.exception.*;
import com.corebaking.shared.exception.dto.FieldErrors;
import com.corebaking.shared.exception.dto.ResponseError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ResponseEntity<ResponseError> validationException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.BAD_REQUEST;

        List<FieldErrors> errors = new ArrayList<>();

        for (FieldError err : ex.getBindingResult().getFieldErrors()) {

            String field = err.getField();
            String error =  err.getDefaultMessage();

            errors.add(new FieldErrors(field, error));
        }
        var response = new ResponseError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {

        var status = HttpStatus.BAD_REQUEST;

        var response = new ResponseError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                List.of(new FieldErrors("body", ex.getMessage()))
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ResponseError> handleAccountBlocked(AccountBlockedException ex, HttpServletRequest request) {
        var status = HttpStatus.FORBIDDEN;
        var response = new ResponseError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                List.of(new FieldErrors("", ex.getMessage()))
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(FinancialRuleException.class)
    public ResponseEntity<ResponseError> financialRuleException(FinancialRuleException ex, HttpServletRequest request){

        var status = HttpStatus.BAD_REQUEST;

        var response = new ResponseError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                List.of(new FieldErrors("", ex.getMessage()))
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ResponseError> idempotencyKeyException(InvalidIdempotencyKeyException ex, HttpServletRequest request){

        var status = HttpStatus.BAD_REQUEST;

        var response = new ResponseError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                List.of(new FieldErrors("", ex.getMessage()))
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ResponseError> idempotencyException(IdempotencyConflictException ex, HttpServletRequest request){

        var status = HttpStatus.CONFLICT;

        var response = new ResponseError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                List.of(new FieldErrors("", ex.getMessage()))
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(DuplicateAccountException.class)
    public ResponseEntity<ResponseError> duplicateAccountException(DuplicateAccountException ex, HttpServletRequest request){

        var status = HttpStatus.CONFLICT;

        var response = new ResponseError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                List.of(new FieldErrors("", ex.getMessage()))
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseError> financialRuleException(ResourceNotFoundException ex, HttpServletRequest request){

        var status = HttpStatus.NOT_FOUND;

        var response = new ResponseError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                request.getRequestURI(),
                List.of(new FieldErrors("", ex.getMessage()))
        );

        return ResponseEntity.status(status).body(response);
    }
}
