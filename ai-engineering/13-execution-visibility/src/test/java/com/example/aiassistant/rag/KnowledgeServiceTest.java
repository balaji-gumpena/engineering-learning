package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.aiassistant.TestProperties;
import com.example.aiassistant.commerce.CommerceApi;
import com.example.aiassistant.execution.ObservedOperationResult;
import com.example.aiassistant.rag.KnowledgeService.KnowledgeRetrievalResult;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class KnowledgeServiceTest {
    KnowledgeIndex index = mock(KnowledgeIndex.class);
    ModelTokenizer tokenizer = mock(ModelTokenizer.class);
    CommerceApi commerce = mock(CommerceApi.class);
    KnowledgeService service =
            new KnowledgeService(TestProperties.create(), index, tokenizer, commerce);

    Document passage(String id, String text, String slug) {
        Map<String, Object> metadata =
                new HashMap<>(
                        Map.of(
                                "citation_id",
                                id,
                                "title",
                                "Demo",
                                "section",
                                "Window",
                                "excerpt",
                                text,
                                "source_path",
                                "demo.md"));
        if (slug != null) {
            metadata.put("product_slug", slug);
        }
        return Document.builder().id(id).text(text).metadata(metadata).build();
    }

    @Test
    void contextBudgetKeepsWholePassages() {
        when(index.searchObserved(anyString(), isNull(), anyInt(), anyDouble()))
                .thenReturn(
                        new ObservedOperationResult<>(
                                List.of(
                                        passage("source:0", "x".repeat(4000), null),
                                        passage("source:1", "y".repeat(3000), null)),
                                List.of()));
        KnowledgeRetrievalResult result = service.lookup("Question", null);
        assertEquals(1, result.sources().size());
        assertEquals(4000, result.sources().getFirst().excerpt().length());
        verifyNoInteractions(commerce);
    }

    @Test
    void catalogFailurePreservesPolicyButSuppressesUnverifiableProducts() {
        when(index.searchObserved(anyString(), isNull(), anyInt(), anyDouble()))
                .thenReturn(
                        new ObservedOperationResult<>(
                                List.of(
                                        passage("product:0", "Guide", "headphones"),
                                        passage("policy:0", "30 days", null)),
                                List.of()));
        when(commerce.publicCatalog()).thenThrow(new RuntimeException("synthetic-private-detail"));
        KnowledgeRetrievalResult result = service.lookup("Question", null);
        assertEquals("policy:0", result.sources().getFirst().citationId());
        assertEquals(1, result.sources().size());
        assertTrue(result.products().isEmpty());
        assertNull(result.error());
    }

    @Test
    void tracedCatalogFailureKeepsPolicyEvidenceAndRecordsOnlyExecutedStages() {
        when(index.searchObserved(anyString(), isNull(), anyInt(), anyDouble()))
                .thenReturn(
                        new ObservedOperationResult<>(
                                List.of(
                                        passage("product:0", "Guide", "headphones"),
                                        passage("policy:0", "30 days", null)),
                                List.of()));
        when(commerce.publicCatalog()).thenThrow(new RuntimeException("private-catalog-error"));
        KnowledgeRetrievalResult result = service.lookup("Question", null);
        assertNull(result.error());
        assertEquals("failed", result.executionEvents().getFirst().status());
        assertEquals("live_product_validation", result.executionEvents().getFirst().stage());
        assertEquals(List.of("policy:0"), result.executionEvents().getLast().sourceIds());
        assertTrue(
                result.executionEvents().stream()
                        .noneMatch(step -> step.stage().equals("query_embedding")));
        assertFalse(JsonMapper.builder().build().writeValueAsString(result).contains("execution"));
        assertTrue(result.products().isEmpty());
    }

    @Test
    void unavailableOrOversizedRetrievalIsExplicit() {
        when(index.searchObserved(anyString(), isNull(), anyInt(), anyDouble()))
                .thenThrow(new IllegalStateException("synthetic-private-detail"));
        assertEquals("knowledge_unavailable", service.lookup("Question", null).error());
        reset(index);
        when(tokenizer.count(anyString())).thenReturn(241);
        assertEquals("knowledge_unavailable", service.lookup("Long question", null).error());
        verifyNoInteractions(index);
    }
}
