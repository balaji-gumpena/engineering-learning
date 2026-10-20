package com.example.aiassistant.execution;

import java.util.List;
import java.util.function.Supplier;

/** Measures one operation without relying on thread-bound context. */
public final class ExecutionObservation {
    private ExecutionObservation() {}

    public static <T> ObservedOperationResult<T> measure(
            String stage, String tool, Supplier<T> operation) {
        long started = System.nanoTime();
        try {
            T value = operation.get();
            Integer resultCount = value instanceof List<?> values ? values.size() : null;
            ExecutionEvent event =
                    ExecutionEvent.completed(stage, tool, elapsed(started), resultCount);
            return new ObservedOperationResult<>(value, List.of(event));
        } catch (RuntimeException error) {
            ExecutionEvent event = ExecutionEvent.failed(stage, tool, elapsed(started));
            throw new ObservedOperationException(error, List.of(event));
        }
    }

    private static long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
