package com.example.aiassistant.chat;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ChatController {
    private final ChatOrchestrator chat;

    public ChatController(ChatOrchestrator chat) {
        this.chat = chat;
    }

    @PostMapping("/chat")
    public ChatDtos.ChatResponse chat(@Valid @RequestBody ChatDtos.ChatRequest request) {
        request.validateConversation();
        return chat.chat(
                request.messages(),
                null,
                UUID.randomUUID().toString(),
                Boolean.TRUE.equals(request.learningMode()));
    }
}
