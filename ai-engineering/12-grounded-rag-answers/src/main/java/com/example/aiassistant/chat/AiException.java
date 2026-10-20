package com.example.aiassistant.chat;

/** Only safe public messages belong here; never wrap credential-bearing exception text. */
public class AiException extends RuntimeException {
    private final int status;

    public AiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
