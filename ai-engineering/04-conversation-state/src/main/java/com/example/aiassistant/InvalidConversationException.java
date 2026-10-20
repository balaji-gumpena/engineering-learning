package com.example.aiassistant;

public final class InvalidConversationException extends IllegalArgumentException {
    public InvalidConversationException(String message) {
        super(message);
    }
}
