package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.TestProperties;
import com.example.aiassistant.execution.ObservedOperationException;
import com.example.aiassistant.execution.ObservedOperationResult;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

class KnowledgeIndexTraceTest {
    private final EmbeddingModel embeddings = mock(EmbeddingModel.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final VectorStore store = mock(VectorStore.class);
    private final AiProperties properties = TestProperties.create();
    private final KnowledgeIndex index =
            new KnowledgeIndex(
                    store,
                    jdbc,
                    mock(PlatformTransactionManager.class),
                    properties,
                    embeddings,
                    JsonMapper.builder().build());

    @Test
    @SuppressWarnings("unchecked")
    void recordsActualEmbeddingAndRetrievalWithRawPassageCount() {
        float[] vector = new float[properties.rag().dimensions()];
        vector[0] = 1;
        when(embeddings.embed("Question")).thenReturn(vector);
        when(jdbc.queryForList(anyString(), any(Class.class)))
                .thenReturn(List.of(properties.rag().fingerprint()));
        List<Document> hits = List.of(Document.builder().id("demo:0").text("Evidence").build());
        doReturn(hits).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
        ObservedOperationResult<List<Document>> result =
                index.searchObserved("Question", null, 4, 0.25);
        assertEquals(hits, result.value());
        assertEquals(
                List.of("query_embedding", "passage_retrieval"),
                result.executionEvents().stream().map(step -> step.stage()).toList());
        assertEquals(1, result.executionEvents().getLast().resultCount());
        assertTrue(result.executionEvents().stream().allMatch(step -> step.durationMs() >= 0));
    }

    @Test
    void embeddingFailureDoesNotClaimRetrievalRan() {
        when(embeddings.embed("Question"))
                .thenThrow(new IllegalStateException("private-embedding-error"));
        ObservedOperationException error =
                assertThrows(
                        ObservedOperationException.class,
                        () -> index.searchObserved("Question", null, 4, 0.25));
        assertEquals(1, error.executionEvents().size());
        assertEquals("query_embedding", error.executionEvents().getFirst().stage());
        assertEquals("failed", error.executionEvents().getFirst().status());
        assertTrue(error.operationCause() instanceof IllegalStateException);
        assertThrows(IllegalStateException.class, () -> index.search("Question", null, 4, 0.25));
        verifyNoInteractions(jdbc, store);
    }
}
