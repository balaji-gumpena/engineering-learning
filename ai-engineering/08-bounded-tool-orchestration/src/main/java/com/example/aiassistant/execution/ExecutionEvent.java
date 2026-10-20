package com.example.aiassistant.execution;

import java.util.List;

/** A sanitized fact about an application operation. */
public record ExecutionEvent(
        String stage,
        String status,
        String tool,
        Long durationMs,
        Integer resultCount,
        List<String> sourceIds) {
    public ExecutionEvent {
        sourceIds = List.copyOf(sourceIds);
    }

    public static ExecutionEvent completed(
            String stage, String tool, long durationMs, Integer resultCount) {
        return new ExecutionEvent(stage, "success", tool, durationMs, resultCount, List.of());
    }

    public static ExecutionEvent failed(String stage, String tool, long durationMs) {
        return new ExecutionEvent(stage, "failed", tool, durationMs, null, List.of());
    }
}
