package com.example.aiassistant.rag;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.execution.ExecutionEvent;
import com.example.aiassistant.execution.ExecutionObservation;
import com.example.aiassistant.execution.ObservedOperationException;
import com.example.aiassistant.execution.ObservedOperationResult;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KnowledgeIndex {
    private final VectorStore store;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final AiProperties properties;
    private final EmbeddingModel embeddings;
    private final JsonMapper json;
    private final TransactionTemplate snapshot;

    public KnowledgeIndex(
            @Lazy VectorStore store,
            JdbcTemplate jdbc,
            PlatformTransactionManager manager,
            AiProperties properties,
            @Lazy EmbeddingModel embeddings,
            JsonMapper json) {
        this.store = store;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
        this.properties = properties;
        this.embeddings = embeddings;
        this.json = json;
        snapshot = new TransactionTemplate(manager);
        snapshot.setReadOnly(true);
        snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    public void initialize() {
        store.getNativeClient();
        jdbc.execute(
                "CREATE TABLE IF NOT EXISTS rag_java.index_state (singleton boolean PRIMARY KEY CHECK (singleton), fingerprint text NOT NULL)");
    }

    public boolean isCompatible() {
        try {
            List<String> state =
                    jdbc.queryForList(
                            "SELECT fingerprint FROM rag_java.index_state WHERE singleton",
                            String.class);
            return !state.isEmpty() && properties.rag().fingerprint().equals(state.getFirst());
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    public void replace(List<Document> documents, boolean allowEmpty) {
        if (documents.isEmpty() && !allowEmpty) {
            throw new IllegalArgumentException("Refusing an empty knowledge replacement");
        }
        if (documents.stream().map(Document::getId).distinct().count() != documents.size()) {
            throw new IllegalArgumentException("Duplicate chunk IDs");
        }
        initialize();
        transaction.executeWithoutResult(
                status -> {
                    jdbc.execute("SELECT pg_advisory_xact_lock(724014)");
                    // Only the AI service's derived index is replaced. Never touch commerce tables.
                    jdbc.update("DELETE FROM rag_java.knowledge_chunks");
                    for (int start = 0; start < documents.size(); start += 32) {
                        store.add(documents.subList(start, Math.min(start + 32, documents.size())));
                    }
                    jdbc.update(
                            "INSERT INTO rag_java.index_state(singleton, fingerprint) VALUES (true, ?) "
                                    + "ON CONFLICT(singleton) DO UPDATE SET fingerprint = EXCLUDED.fingerprint",
                            properties.rag().fingerprint());
                });
    }

    public List<Document> search(String question, String productSlug, int limit, double minScore) {
        return search(question, productSlug, null, limit, minScore);
    }

    public List<Document> search(
            String question, String productSlug, String sourceType, int limit, double minScore) {
        try {
            return searchObserved(question, productSlug, sourceType, limit, minScore).value();
        } catch (ObservedOperationException error) {
            throw error.operationCause();
        }
    }

    public ObservedOperationResult<List<Document>> searchObserved(
            String question, String productSlug, int limit, double minScore) {
        return searchObserved(question, productSlug, null, limit, minScore);
    }

    private ObservedOperationResult<List<Document>> searchObserved(
            String question, String productSlug, String sourceType, int limit, double minScore) {
        validateQuery(question, sourceType, limit, minScore);
        List<ExecutionEvent> events = new ArrayList<>();
        ObservedOperationResult<float[]> embeddingResult;
        try {
            embeddingResult =
                    ExecutionObservation.measure(
                            "query_embedding",
                            "search_knowledge",
                            () -> {
                                float[] embedded = embeddings.embed(question);
                                ValidatingEmbeddingModel.validate(
                                        embedded, properties.rag().dimensions());
                                return embedded;
                            });
        } catch (ObservedOperationException error) {
            throw error.withPreviousEvents(events);
        }
        events.addAll(embeddingResult.executionEvents());
        // Exact cosine SQL preserves negative thresholds and deterministic tie ordering.
        String encoded = Arrays.toString(embeddingResult.value());
        initialize();
        ObservedOperationResult<List<Document>> retrievalResult;
        try {
            retrievalResult =
                    ExecutionObservation.measure(
                            "passage_retrieval",
                            "search_knowledge",
                            () ->
                                    snapshot.execute(
                                            status -> {
                                                List<String> state =
                                                        jdbc.queryForList(
                                                                "SELECT fingerprint FROM rag_java.index_state WHERE singleton",
                                                                String.class);
                                                if (state.isEmpty()
                                                        || !properties
                                                                .rag()
                                                                .fingerprint()
                                                                .equals(state.getFirst())) {
                                                    throw new IllegalStateException(
                                                            "Knowledge index is missing or incompatible; re-ingest before searching");
                                                }
                                                return jdbc.query(
                                                        "SELECT id, content, metadata, 1 - (embedding <=> ?::vector) AS score "
                                                                + "FROM rag_java.knowledge_chunks WHERE metadata->>'fingerprint' = ? "
                                                                + "AND (?::text IS NULL OR metadata->>'product_slug' = ?) "
                                                                + "AND (?::text IS NULL OR metadata->>'source_type' = ?) "
                                                                + "AND 1 - (embedding <=> ?::vector) >= ? ORDER BY embedding <=> ?::vector, id LIMIT ?",
                                                        (rs, row) ->
                                                                Document.builder()
                                                                        .id(rs.getString("id"))
                                                                        .text(
                                                                                rs.getString(
                                                                                        "content"))
                                                                        .metadata(
                                                                                json.readValue(
                                                                                        rs
                                                                                                .getString(
                                                                                                        "metadata"),
                                                                                        new TypeReference<
                                                                                                Map<
                                                                                                        String,
                                                                                                        Object>>() {}))
                                                                        .score(
                                                                                rs.getDouble(
                                                                                        "score"))
                                                                        .build(),
                                                        encoded,
                                                        properties.rag().fingerprint(),
                                                        productSlug,
                                                        productSlug,
                                                        sourceType,
                                                        sourceType,
                                                        encoded,
                                                        minScore,
                                                        encoded,
                                                        limit);
                                            }));
        } catch (ObservedOperationException error) {
            throw error.withPreviousEvents(events);
        }
        events.addAll(retrievalResult.executionEvents());
        return new ObservedOperationResult<>(retrievalResult.value(), events);
    }

    public static void validateQuery(
            String question, String sourceType, int limit, double minScore) {
        if (question == null
                || question.isBlank()
                || limit < 1
                || limit > 20
                || !Double.isFinite(minScore)
                || minScore < -1
                || minScore > 1
                || sourceType != null
                        && !Set.of("policy", "faq", "product_guide").contains(sourceType)) {
            throw new IllegalArgumentException("Invalid knowledge query");
        }
    }
}
