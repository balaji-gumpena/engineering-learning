package com.example.aiassistant.chat;

import com.example.aiassistant.chat.ChatDtos.ChatExecutionStep;

import java.util.List;

/** Keeps safe execution facts alongside a failure without exposing its raw message. */
public final class ChatExecutionException extends RuntimeException {
    private final List<ChatExecutionStep> executionTrace;

    public ChatExecutionException(RuntimeException cause, List<ChatExecutionStep> executionTrace) {
        super("Chat execution failed", cause);
        this.executionTrace = List.copyOf(executionTrace);
    }

    public List<ChatExecutionStep> executionTrace() {
        return executionTrace;
    }
}
