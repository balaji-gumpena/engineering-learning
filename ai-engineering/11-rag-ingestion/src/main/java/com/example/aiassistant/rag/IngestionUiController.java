package com.example.aiassistant.rag;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class IngestionUiController {
    private final IngestionUiService service;

    public IngestionUiController(IngestionUiService service) {
        this.service = service;
    }

    @PostMapping("/lesson/ingestion")
    public KnowledgeReply prepare(@Valid @RequestBody IngestionRequest request) throws IOException {
        return service.prepare(request.action());
    }
}
