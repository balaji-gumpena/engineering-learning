package com.example.aiassistant.rag;

import com.example.aiassistant.AiProperties;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class KnowledgeEvaluationService {
    private final Environment environment;
    private final AiProperties properties;
    private final IngestionService ingestion;
    private final KnowledgeIndex index;
    private final JsonMapper json;
    private final EmbeddingModel embeddings;

    public KnowledgeEvaluationService(
            Environment environment,
            AiProperties properties,
            IngestionService ingestion,
            KnowledgeIndex index,
            JsonMapper json,
            @Lazy EmbeddingModel embeddings) {
        this.environment = environment;
        this.properties = properties;
        this.ingestion = ingestion;
        this.index = index;
        this.json = json;
        this.embeddings = embeddings;
    }

    public Map<String, Object> evaluate() throws IOException {
        JsonNode cases =
                json.readTree(
                        Files.readString(
                                Path.of(
                                        environment.getProperty(
                                                "ai.cases", "evaluation/questions.json"))));
        int limit = environment.getProperty("ai.limit", Integer.class, 3);
        double minimumScore = environment.getProperty("ai.min-score", Double.class, -1.0);
        String sourceType = environment.getProperty("ai.source-type");
        String productSlug = environment.getProperty("ai.product-slug");
        KnowledgeIndex.validateQuery("evaluation", sourceType, limit, minimumScore);
        boolean inMemory = environment.getProperty("ai.in-memory", Boolean.class, false);
        Function<String, List<Document>> search;
        if (inMemory) {
            search = inMemorySearch(productSlug, sourceType, limit, minimumScore);
        } else {
            search =
                    question ->
                            index.search(question, productSlug, sourceType, limit, minimumScore);
        }
        Map<String, Object> report = RetrievalEvaluation.evaluate(cases, search, limit);
        report.put("backend", inMemory ? "in-memory" : "postgresql");
        report.put("fingerprint", properties.rag().fingerprint());
        return report;
    }

    private Function<String, List<Document>> inMemorySearch(
            String productSlug, String sourceType, int limit, double minimumScore)
            throws IOException {
        List<Document> documents =
                ingestion.chunks(
                        environment.getProperty("ai.include-catalog", Boolean.class, false));
        List<float[]> documentVectors =
                embeddings.embed(documents.stream().map(Document::getText).toList());
        return question -> {
            float[] queryVector = embeddings.embed(question);
            List<Document> hits = new ArrayList<>();
            for (int documentIndex = 0; documentIndex < documents.size(); documentIndex++) {
                Document document = documents.get(documentIndex);
                if (!matchesFilters(document, productSlug, sourceType)) {
                    continue;
                }
                double score = cosine(queryVector, documentVectors.get(documentIndex));
                if (score >= minimumScore) {
                    hits.add(
                            Document.builder()
                                    .id(document.getId())
                                    .text(document.getText())
                                    .metadata(document.getMetadata())
                                    .score(score)
                                    .build());
                }
            }
            return hits.stream()
                    .sorted(
                            Comparator.comparingDouble((Document document) -> document.getScore())
                                    .reversed()
                                    .thenComparing(Document::getId))
                    .limit(limit)
                    .toList();
        };
    }

    private boolean matchesFilters(Document document, String productSlug, String sourceType) {
        boolean matchesSource =
                sourceType == null || sourceType.equals(document.getMetadata().get("source_type"));
        boolean matchesProduct =
                productSlug == null
                        || productSlug.equals(document.getMetadata().get("product_slug"));
        return matchesSource && matchesProduct;
    }

    private static double cosine(float[] queryVector, float[] documentVector) {
        double dotProduct = 0;
        double querySquaredNorm = 0;
        double documentSquaredNorm = 0;
        for (int dimension = 0; dimension < queryVector.length; dimension++) {
            dotProduct += (double) queryVector[dimension] * documentVector[dimension];
            querySquaredNorm += (double) queryVector[dimension] * queryVector[dimension];
            documentSquaredNorm += (double) documentVector[dimension] * documentVector[dimension];
        }
        return Math.max(
                -1, Math.min(1, dotProduct / Math.sqrt(querySquaredNorm * documentSquaredNorm)));
    }
}
