package com.gateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", status.value(),
            "error", status.getReasonPhrase(),
            "message", message));
    }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> auth(AuthenticationException e) {
        return error(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, "Dados de requisição inválidos.");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor.");
    }
}
