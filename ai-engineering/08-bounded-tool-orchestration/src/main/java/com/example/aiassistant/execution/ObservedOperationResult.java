package com.example.aiassistant.execution;

import java.util.List;

/** A value accompanied by immutable execution facts produced while computing it. */
public record ObservedOperationResult<T>(T value, List<ExecutionEvent> executionEvents) {
    public ObservedOperationResult {
        executionEvents = List.copyOf(executionEvents);
    }
}
