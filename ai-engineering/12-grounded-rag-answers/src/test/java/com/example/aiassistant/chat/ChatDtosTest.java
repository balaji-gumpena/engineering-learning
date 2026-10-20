package com.example.aiassistant.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aiassistant.chat.ChatDtos.ChatRequest;
import com.example.aiassistant.chat.ChatDtos.ChatResponse;
import com.example.aiassistant.chat.ChatDtos.ProductSummary;
import com.example.aiassistant.chat.ChatDtos.SourceCitation;
import com.example.aiassistant.chat.ChatDtos.TokenUsage;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

/** Java type names must not change the browser's JSON contract. */
class ChatDtosTest {
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void descriptiveRequestTypesPreserveMessageDeserializationAndSerialization() {
        String requestJson =
                """
                {"messages":[{"role":"user","content":"Hello"}]}
                """;
        ChatRequest request = json.readValue(requestJson, ChatRequest.class);

        request.validateConversation();

        assertEquals("user", request.messages().getFirst().role());
        assertEquals("Hello", request.messages().getFirst().content());
        assertNull(request.learningMode());
        assertEquals(json.readTree(requestJson), json.readTree(json.writeValueAsString(request)));
    }

    @Test
    void learningModeAndTraceUseAnAdditiveJsonContract() {
        ChatRequest request =
                json.readValue(
                        "{\"messages\":[{\"role\":\"user\",\"content\":\"Hello\"}],\"learning_mode\":true}",
                        ChatRequest.class);
        assertTrue(request.learningMode());
        ChatResponse response =
                new ChatResponse(
                        "Hello",
                        "test",
                        TokenUsage.unknown(),
                        List.of(),
                        null,
                        false,
                        List.of(),
                        List.of(
                                new ChatDtos.ChatExecutionStep(
                                        "initial_model", "success", null, 5L, null, List.of())));
        assertEquals(
                5,
                json.readTree(json.writeValueAsString(response))
                        .path("execution_trace")
                        .get(0)
                        .path("duration_ms")
                        .asInt());
    }

    @Test
    void descriptiveResponseTypesPreserveUsageProductsAndCitations() {
        ChatResponse response =
                new ChatResponse(
                        "A supported answer",
                        "synthetic-model",
                        new TokenUsage(10, 5, 15),
                        List.of(
                                new ProductSummary(
                                        1,
                                        "Headphones",
                                        new BigDecimal("49.00"),
                                        "EUR",
                                        "/images/headphones.svg",
                                        "Electronics")),
                        1,
                        false,
                        List.of(
                                new SourceCitation(
                                        "guide:0",
                                        "Headphones guide",
                                        "Overview",
                                        "Quiet audio",
                                        "guides/headphones.md",
                                        "headphones",
                                        true)));
        String expectedJson =
                """
                {"reply":"A supported answer","model":"synthetic-model",
                 "usage":{"input_tokens":10,"output_tokens":5,"total_tokens":15},
                 "products":[{"id":1,"name":"Headphones","price_eur":"49.00","currency":"EUR",
                              "image_path":"/images/headphones.svg","category_name":"Electronics"}],
                 "total_matches":1,"has_more":false,
                 "sources":[{"citation_id":"guide:0","title":"Headphones guide","section":"Overview",
                             "excerpt":"Quiet audio","source_path":"guides/headphones.md",
                             "product_slug":"headphones","demo":true}]}
                """;

        assertEquals(json.readTree(expectedJson), json.readTree(json.writeValueAsString(response)));
    }
}
