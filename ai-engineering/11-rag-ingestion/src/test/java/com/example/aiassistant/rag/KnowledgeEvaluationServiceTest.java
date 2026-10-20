package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.aiassistant.TestProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.mock.env.MockEnvironment;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class KnowledgeEvaluationServiceTest {
    @TempDir Path directory;

    private final IngestionService ingestion = mock(IngestionService.class);
    private final KnowledgeIndex index = mock(KnowledgeIndex.class);
    private final EmbeddingModel embeddings = mock(EmbeddingModel.class);
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void inMemoryEvaluationPreservesFiltersThresholdTieOrderingAndLimit() throws Exception {
        MockEnvironment environment =
                evaluationEnvironment()
                        .withProperty("ai.in-memory", "true")
                        .withProperty("ai.source-type", "faq")
                        .withProperty("ai.product-slug", "headphones")
                        .withProperty("ai.min-score", "0.5")
                        .withProperty("ai.limit", "2");
        List<Document> documents =
                List.of(
                        passage("wrong-type", "policy", "headphones"),
                        passage("wrong-product", "faq", "speaker"),
                        passage("below-threshold", "faq", "headphones"),
                        passage("b", "faq", "headphones"),
                        passage("a", "faq", "headphones"),
                        passage("after-limit", "faq", "headphones"));
        when(ingestion.chunks(false)).thenReturn(documents);
        when(embeddings.embed(documents.stream().map(Document::getText).toList()))
                .thenReturn(
                        List.of(
                                new float[] {1, 0},
                                new float[] {1, 0},
                                new float[] {0, 1},
                                new float[] {4, 3},
                                new float[] {4, 3},
                                new float[] {3, 4}));
        when(embeddings.embed("Question")).thenReturn(new float[] {1, 0});

        JsonNode observed = json.valueToTree(evaluations(environment).evaluate());

        JsonNode report = observed;
        JsonNode retrieved = report.path("cases").get(0).path("retrieved");
        assertEquals(2, retrieved.size());
        assertEquals("a", retrieved.get(0).path("source_id").asText());
        assertEquals("b", retrieved.get(1).path("source_id").asText());
        assertEquals(.8, retrieved.get(0).path("score").asDouble(), .000001);
        assertEquals("in-memory", report.path("backend").asText());
        assertEquals(
                TestProperties.create().rag().fingerprint(), report.path("fingerprint").asText());
        verifyNoInteractions(index);
    }

    @Test
    void inMemoryEvaluationKeepsNegativeScoresWhenRequested() throws Exception {
        MockEnvironment environment =
                evaluationEnvironment()
                        .withProperty("ai.in-memory", "true")
                        .withProperty("ai.min-score", "-1");
        List<Document> documents = List.of(passage("opposite", "faq", "headphones"));
        when(ingestion.chunks(false)).thenReturn(documents);
        when(embeddings.embed(List.of("opposite"))).thenReturn(List.of(new float[] {-1, 0}));
        when(embeddings.embed("Question")).thenReturn(new float[] {1, 0});

        JsonNode observed = json.valueToTree(evaluations(environment).evaluate());

        JsonNode retrieved = observed.path("cases").get(0).path("retrieved");
        assertEquals(1, retrieved.size());
        assertEquals(-1, retrieved.get(0).path("score").asDouble());
        verifyNoInteractions(index);
    }

    @Test
    void databaseEvaluationForwardsOptionsWithoutPreparingAnInMemoryIndex() throws Exception {
        MockEnvironment environment =
                evaluationEnvironment()
                        .withProperty("ai.source-type", "policy")
                        .withProperty("ai.product-slug", "headphones")
                        .withProperty("ai.limit", "5")
                        .withProperty("ai.min-score", "0.25");
        when(index.search("Question", "headphones", "policy", 5, .25)).thenReturn(List.of());

        JsonNode observed = json.valueToTree(evaluations(environment).evaluate());

        assertEquals("postgresql", observed.path("backend").asText());
        verify(index).search("Question", "headphones", "policy", 5, .25);
        verifyNoInteractions(ingestion, embeddings);
    }

    private MockEnvironment evaluationEnvironment() throws Exception {
        Path cases = directory.resolve("questions.json");
        Files.writeString(
                cases, "[{\"question\":\"Question\",\"source_id\":null,\"section\":null}]");
        return new MockEnvironment().withProperty("ai.cases", cases.toString());
    }

    private KnowledgeEvaluationService evaluations(MockEnvironment environment) {
        return new KnowledgeEvaluationService(
                environment, TestProperties.create(), ingestion, index, json, embeddings);
    }

    private Document passage(String id, String sourceType, String productSlug) {
        return Document.builder()
                .id(id)
                .text(id)
                .metadata(
                        Map.of(
                                "source_id",
                                id,
                                "section",
                                "Overview",
                                "source_type",
                                sourceType,
                                "product_slug",
                                productSlug))
                .build();
    }
}
