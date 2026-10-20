package com.example.aiassistant.chat;

import com.anthropic.errors.AnthropicServiceException;
import com.openai.errors.OpenAIServiceException;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ChatExecutionException.class)
    ResponseEntity<?> executionFailure(ChatExecutionException error) {
        Throwable cause = error.getCause();
        int status = cause instanceof AiException known ? known.status() : upstreamStatus(cause);
        String detail =
                cause instanceof AiException known
                        ? known.getMessage()
                        : "AI service could not complete the request. Please try again.";
        return ResponseEntity.status(status)
                .body(Map.of("detail", detail, "execution_trace", error.executionTrace()));
    }

    @ExceptionHandler(AiException.class)
    ResponseEntity<?> known(AiException error) {
        return ResponseEntity.status(error.status()).body(Map.of("detail", error.getMessage()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<?> validation(Exception error) {
        return ResponseEntity.unprocessableEntity().body(Map.of("detail", "Invalid chat request."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception error) {
        // Exception messages may contain credentials or provider request bodies.
        LoggerFactory.getLogger(ApiExceptionHandler.class)
                .warn("AI request failed error_type={}", error.getClass().getSimpleName());
        int status = upstreamStatus(error);
        return ResponseEntity.status(status)
                .body(
                        Map.of(
                                "detail",
                                "AI service could not complete the request. Please try again."));
    }

    static int upstreamStatus(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof InterruptedIOException
                    || cause instanceof TimeoutException) {
                return 504;
            }
            int status =
                    cause instanceof OpenAIServiceException openai
                            ? openai.statusCode()
                            : cause instanceof AnthropicServiceException anthropic
                                    ? anthropic.statusCode()
                                    : 0;
            if (status == 429) {
                return 429;
            }
            if (status == 401 || status == 403) {
                return 503;
            }
            if (status == 408 || status == 504) {
                return 504;
            }
        }
        return 502;
    }
}
