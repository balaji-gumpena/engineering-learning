package com.example.aiassistant.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class LocalErrorHandler {
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> failure(Exception error) {
        int status =
                error instanceof AiException known
                        ? known.status()
                        : error
                                        instanceof
                                        org.springframework.web.bind.MethodArgumentNotValidException
                                ? 422
                                : 502;
        return ResponseEntity.status(status)
                .body(
                        Map.of(
                                "detail",
                                "Request failed; check local configuration and request format."));
    }
}
