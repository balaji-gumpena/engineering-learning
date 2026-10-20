package com.example.aiassistant.chat;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChatController {
    private final ChatOrchestrator chat;

    public ChatController(ChatOrchestrator chat) {
        this.chat = chat;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(
            @Valid @RequestBody ChatDtos.ChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        request.validateConversation();
        String bearer =
                authorization != null && authorization.startsWith("Bearer ")
                        ? authorization.substring(7)
                        : null;
        return Map.of("reply", chat.chat(request.messages(), bearer));
    }
}
