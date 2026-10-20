package com.example.aiassistant.execution;

import java.util.ArrayList;
import java.util.List;

/** Preserves sanitized execution facts when an observed operation fails. */
public final class ObservedOperationException extends RuntimeException {
    private final List<ExecutionEvent> executionEvents;

    public ObservedOperationException(
            RuntimeException cause, List<ExecutionEvent> executionEvents) {
        super("Observed operation failed", cause);
        this.executionEvents = List.copyOf(executionEvents);
    }

    public List<ExecutionEvent> executionEvents() {
        return executionEvents;
    }

    public RuntimeException operationCause() {
        return (RuntimeException) getCause();
    }

    public ObservedOperationException withPreviousEvents(List<ExecutionEvent> previousEvents) {
        List<ExecutionEvent> combined = new ArrayList<>(previousEvents);
        combined.addAll(executionEvents);
        return new ObservedOperationException(operationCause(), combined);
    }
}
