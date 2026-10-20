package com.example.aiassistant;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProviderChatController {
    private final ProviderChatService service;

    public ProviderChatController(ProviderChatService service) {
        this.service = service;
    }

    @GetMapping("/lesson/providers")
    public List<String> providers() {
        return service.aliases();
    }

    @PostMapping("/chat")
    public ProviderChatReply chat(@Valid @RequestBody ProviderChatRequest request) {
        return service.answer(request);
    }
}
