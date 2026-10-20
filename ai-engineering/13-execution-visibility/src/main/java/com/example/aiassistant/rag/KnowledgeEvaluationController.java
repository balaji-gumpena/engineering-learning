package com.example.aiassistant.rag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class KnowledgeEvaluationController {
    private final KnowledgeEvaluationService service;

    public KnowledgeEvaluationController(KnowledgeEvaluationService service) {
        this.service = service;
    }

    @PostMapping("/lesson/evaluation")
    public EvaluationReply evaluate() throws IOException {
        Map<String, Object> report = service.evaluate();
        return new EvaluationReply(
                "Retrieval evaluation complete. These scores measure retrieved passages, not generated answer quality.",
                report);
    }
}
