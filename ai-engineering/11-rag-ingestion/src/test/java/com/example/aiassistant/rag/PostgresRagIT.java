package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.commerce.CommerceApi;
import com.example.aiassistant.rag.KnowledgeService.KnowledgeRetrievalResult;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Tag("postgres")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.datasource.url=jdbc:postgresql://127.0.0.1:55439/rag_test",
                    "spring.datasource.username=postgres",
            "spring.datasource.password=", "ai.rag.bootstrap=false"
        })
class PostgresRagIT {
    @Autowired IngestionService ingestion;
    @Autowired KnowledgeIndex index;
    @Autowired @Lazy EmbeddingModel embeddings;
    @Autowired JdbcTemplate jdbc;
    @Autowired KnowledgeService knowledge;
    @Autowired AiProperties properties;
    @Autowired @Lazy ModelTokenizer tokenizer;

    @Test
    void updatesDeletesFiltersAndFingerprintStateArePublishedTogether() throws Exception {
        ingestion.ingest(false, false);
        List<Document> policies = index.search("return postage", null, "policy", 20, -1);
        assertFalse(policies.isEmpty());
        assertTrue(
                policies.stream()
                        .allMatch(doc -> "policy".equals(doc.getMetadata().get("source_type"))));
        assertTrue(index.search("return postage", "nonexistent-product", null, 3, -1).isEmpty());
        assertEquals("30s", jdbc.queryForObject("SHOW statement_timeout", String.class));
        assertEquals("5s", jdbc.queryForObject("SHOW lock_timeout", String.class));
        Document first = policies.getFirst();
        Document changed =
                Document.builder()
                        .id(first.getId())
                        .text("Updated demo evidence")
                        .metadata(first.getMetadata())
                        .build();
        index.replace(List.of(changed), false);
        assertEquals(
                1L,
                jdbc.queryForObject("SELECT count(*) FROM rag_java.knowledge_chunks", Long.class));
        assertEquals(
                "Updated demo evidence",
                index.search("updated demo", null, 20, -1).getFirst().getText());
        jdbc.update("UPDATE rag_java.index_state SET fingerprint = 'incompatible'");
        assertFalse(index.isCompatible());
        assertThrows(IllegalStateException.class, () -> index.search("return", null, 3, -1));
        index.replace(List.of(), true);
        assertTrue(index.isCompatible());
        assertTrue(index.search("return", null, 3, -1).isEmpty());
        jdbc.update("DELETE FROM rag_java.index_state");
        assertThrows(IllegalStateException.class, () -> index.search("return", null, 3, -1));
    }

    @Test
    void combinedCatalogIngestionHydratesCurrentValuesAndSuppressesRemovedProducts()
            throws Exception {
        Builder builder = RestClient.builder().baseUrl("http://commerce.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JsonMapper json = JsonMapper.builder().build();
        CommerceApi api = new CommerceApi(builder.build(), json);
        String catalog =
                """
                {"totalElements":1,"content":[{"id":1001,"slug":"audit-headphones","name":"Audit Headphones",
                 "description":"Quiet wireless audio for a journey","brand":"Demo","categoryName":"Electronics",
                 "price":49,"currency":"EUR","stockQuantity":3,"active":true}]}
                """;
        server.expect(
                        MockRestRequestMatchers.requestTo(
                                "http://commerce.test/api/products?page=0&size=50"))
                .andExpect(MockRestRequestMatchers.headerDoesNotExist("Authorization"))
                .andRespond(
                        MockRestResponseCreators.withSuccess(catalog, MediaType.APPLICATION_JSON));
        IngestionService preparation =
                new IngestionService(
                        properties, new MarkdownDocumentLoader(), tokenizer, index, api);
        assertEquals(8L, preparation.ingest(true, false).get("sources"));
        List<Document> hits = index.search("quiet wireless audio", "audit-headphones", 4, -1);
        assertFalse(hits.isEmpty());
        assertFalse(hits.getFirst().getText().contains("49"));
        server.verify();
        server.reset();
        server.expect(MockRestRequestMatchers.anything())
                .andRespond(
                        MockRestResponseCreators.withSuccess(
                                catalog.replace("\"price\":49", "\"price\":79"),
                                MediaType.APPLICATION_JSON));
        KnowledgeService service = new KnowledgeService(properties, index, tokenizer, api);
        KnowledgeRetrievalResult result =
                service.lookup("quiet wireless audio", "audit-headphones");
        assertFalse(result.sources().isEmpty());
        assertEquals(new BigDecimal("79"), result.products().getFirst().priceEur());
        server.verify();
        server.reset();
        server.expect(MockRestRequestMatchers.anything())
                .andRespond(
                        MockRestResponseCreators.withSuccess(
                                "{\"content\":[],\"totalElements\":0}",
                                MediaType.APPLICATION_JSON));
        KnowledgeRetrievalResult removed =
                service.lookup("quiet wireless audio", "audit-headphones");
        assertTrue(removed.sources().isEmpty());
        assertTrue(removed.products().isEmpty());
        server.verify();
    }

    @Test
    void realEmbeddingsLoadAndRetrieveExpectedPassages() throws Exception {
        Map<String, Long> result = ingestion.ingest(false, false);
        assertEquals(7, result.get("sources"));
        List<Document> hits = index.search("Who pays postage when I change my mind?", null, 3, .25);
        assertTrue(
                hits.stream()
                        .anyMatch(
                                doc ->
                                        "returns-policy".equals(doc.getMetadata().get("source_id"))
                                                && doc.getMetadata()
                                                        .get("section")
                                                        .toString()
                                                        .contains("Who pays")));
        assertEquals(384, embeddings.embed("Quiet headphones").length);
        long count =
                jdbc.queryForObject("SELECT count(*) FROM rag_java.knowledge_chunks", Long.class);
        assertThrows(
                RuntimeException.class,
                () ->
                        index.replace(
                                List.of(
                                        Document.builder()
                                                .id("invalid\0id")
                                                .text("Synthetic test text")
                                                .build()),
                                false));
        assertEquals(
                count,
                jdbc.queryForObject("SELECT count(*) FROM rag_java.knowledge_chunks", Long.class));
        assertThrows(IllegalArgumentException.class, () -> index.replace(List.of(), false));
    }
}
