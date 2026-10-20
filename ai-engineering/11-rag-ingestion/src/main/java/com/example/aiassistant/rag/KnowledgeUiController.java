package com.example.aiassistant.rag;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class KnowledgeUiController {
    private final KnowledgeSearchService service;

    public KnowledgeUiController(KnowledgeSearchService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public KnowledgeReply search(@Valid @RequestBody KnowledgeChatRequest request)
            throws IOException {
        return service.answer(request.messages().getFirst().content());
    }
}
