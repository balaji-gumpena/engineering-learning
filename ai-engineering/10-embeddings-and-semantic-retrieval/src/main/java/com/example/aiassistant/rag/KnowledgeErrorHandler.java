package com.example.aiassistant.rag;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class KnowledgeErrorHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> failure(Exception error) {
        int status =
                error instanceof IllegalArgumentException
                                || error instanceof MethodArgumentNotValidException
                                || error instanceof HttpMessageNotReadableException
                        ? 422
                        : 503;
        return ResponseEntity.status(status)
                .body(
                        Map.of(
                                "detail",
                                "Check the question, embedding resources and disposable index availability."));
    }
}
