package com.example.aiassistant.rag;

import org.springframework.ai.document.Document;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class KnowledgeEvaluationService {
    private final SemanticSearch search;
    private final Environment environment;

    public KnowledgeEvaluationService(SemanticSearch search, Environment environment) {
        this.search = search;
        this.environment = environment;
    }

    public Map<String, Object> evaluate() throws java.io.IOException {
        int limit = environment.getProperty("ai.limit", Integer.class, 3);
        double floor = environment.getProperty("ai.min-score", Double.class, -1.0);
        String sourceType = environment.getProperty("ai.source-type");
        RetrievalQueryValidator.validate("evaluation", sourceType, limit, floor);
        Function<String, List<Document>> query =
                search.inMemorySearch(
                        environment.getProperty("ai.product-slug"), sourceType, limit, floor);
        JsonMapper json = JsonMapper.builder().build();
        Path cases = Path.of(environment.getProperty("ai.cases", "evaluation/questions.json"));
        return RetrievalEvaluation.evaluate(json.readTree(Files.readString(cases)), query, limit);
    }
}
