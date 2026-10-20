package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@Tag("embeddings")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LocalEmbeddingEvaluationIT {
    @Autowired KnowledgeEvaluationService evaluation;

    @Test
    void realLocalEmbeddingsProduceARetrievalReport() throws Exception {
        Map<String, Object> report = evaluation.evaluate();
        assertTrue(((Number) report.get("answerable_cases")).intValue() > 0);
    }
}
