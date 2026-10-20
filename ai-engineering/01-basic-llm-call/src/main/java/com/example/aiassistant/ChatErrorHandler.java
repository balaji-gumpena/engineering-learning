package com.example.aiassistant;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ChatErrorHandler {
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, String>> invalid(Exception error) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("detail", "Invalid lesson message."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unavailable(Exception error) {
        return ResponseEntity.status(503)
                .body(
                        Map.of(
                                "detail",
                                "Check the local model configuration and provider availability."));
    }
}
