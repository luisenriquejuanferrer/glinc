package com.glinc.glincbackend.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// Convierte excepciones en Problem Details para toda la API.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return problemDetail(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    // Body JSON ausente o mal formado: es culpa del cliente, no un 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBodyIlegible(HttpMessageNotReadableException ex) {
        return problemDetail(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "El cuerpo de la peticion es invalido o esta mal formado.");
    }

    // Red de seguridad: cualquier error no previsto sale tambien como Problem Details.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex) {
        log.error("Error no controlado", ex);
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ha ocurrido un error inesperado.");
    }

    private ResponseEntity<Map<String, Object>> problemDetail(
            HttpStatus status, String code, String detail) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("code", code);
        body.put("detail", detail);
        return ResponseEntity.status(status).body(body);
    }
}
