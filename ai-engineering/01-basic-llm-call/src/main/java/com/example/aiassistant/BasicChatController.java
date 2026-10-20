package com.example.aiassistant;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BasicChatController {
    private final BasicChatService service;

    public BasicChatController(BasicChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public ChatReply chat(@Valid @RequestBody ChatRequest request) {
        return service.answer(request.messages().getFirst().content());
    }
}
