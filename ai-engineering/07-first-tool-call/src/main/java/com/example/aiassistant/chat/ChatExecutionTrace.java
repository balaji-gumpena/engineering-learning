package com.example.aiassistant.chat;

import com.example.aiassistant.chat.ChatDtos.ChatExecutionStep;
import com.example.aiassistant.execution.ExecutionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Request-local execution facts. Never record prompts, arguments or exception messages. */
public final class ChatExecutionTrace {
    private static final int MAX_STEPS = 40;
    private final boolean enabled;
    private final List<ChatExecutionStep> steps = new ArrayList<>();

    public ChatExecutionTrace(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public List<ChatExecutionStep> steps() {
        return List.copyOf(steps);
    }

    public void addAll(List<ExecutionEvent> events) {
        for (ExecutionEvent event : events) {
            record(
                    event.stage(),
                    event.status(),
                    event.tool(),
                    event.durationMs(),
                    event.resultCount(),
                    event.sourceIds());
        }
    }

    public void record(String stage, String status, String tool) {
        record(stage, status, tool, null, null, List.of());
    }

    public void record(
            String stage,
            String status,
            String tool,
            Long durationMs,
            Integer resultCount,
            List<String> sourceIds) {
        if (enabled && steps.size() < MAX_STEPS) {
            steps.add(
                    new ChatExecutionStep(
                            stage, status, tool, durationMs, resultCount, List.copyOf(sourceIds)));
        }
    }

    public <T> T measure(String stage, String tool, Supplier<T> operation) {
        if (!enabled) {
            return operation.get();
        }
        long started = System.nanoTime();
        try {
            T result = operation.get();
            Integer resultCount = result instanceof List<?> values ? values.size() : null;
            record(stage, "success", tool, elapsed(started), resultCount, List.of());
            return result;
        } catch (RuntimeException error) {
            record(stage, "failed", tool, elapsed(started), null, List.of());
            throw error;
        }
    }

    private long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
