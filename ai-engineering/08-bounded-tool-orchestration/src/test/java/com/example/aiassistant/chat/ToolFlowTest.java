package com.example.aiassistant.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.aiassistant.commerce.CommerceApi;
import com.example.aiassistant.llm.LlmGateway;
import com.example.aiassistant.llm.LlmGateway.LlmResponse;
import com.example.aiassistant.tools.ToolService;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

class ToolFlowTest {
    private final LlmGateway model = mock(LlmGateway.class);
    private final CommerceApi commerce = mock(CommerceApi.class);
    private final ChatOrchestrator chat =
            new ChatOrchestrator(
                    model,
                    new ToolService(commerce, JsonMapper.builder().build()),
                    JsonMapper.builder().build());

    private LlmResponse calls(ToolCall... calls) {
        return new LlmResponse(
                AssistantMessage.builder().content("").toolCalls(List.of(calls)).build(),
                "fixture",
                ChatDtos.TokenUsage.unknown());
    }

    private LlmResponse answer(String text) {
        return new LlmResponse(
                new AssistantMessage(text), "fixture", ChatDtos.TokenUsage.unknown());
    }

    private String ask() {
        return chat.chat(
                List.of(new ChatDtos.ChatMessage("user", "Store lookup")), "synthetic-bearer");
    }

    @Test
    void invalidArgumentsPreventAllHttpCalls() {
        when(model.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(
                                new ToolCall(
                                        "a",
                                        "function",
                                        "search_products",
                                        "{\"max_price_eur_exclusive\":\"500\"}")));
        assertTrue(ask().contains("couldn't interpret"));
        verifyNoInteractions(commerce);
    }

    @Test
    void directAnswerDoesNotExecuteCommerce() {
        when(model.complete(anyList(), anyList(), anyBoolean())).thenReturn(answer("Hello"));
        assertEquals("Hello", ask());
        verifyNoInteractions(commerce);
    }

    @Test
    void searchUsesOneBatchThenDisablesTools() {
        when(commerce.search(any(), eq("synthetic-bearer")))
                .thenReturn(new CommerceApi.ProductSearchResult(List.of(), 0, false));
        when(model.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(
                                new ToolCall(
                                        "a",
                                        "function",
                                        "search_products",
                                        "{\"max_price_eur_exclusive\":500}")),
                        answer("No products"));
        assertEquals("No products", ask());
        verify(commerce).search(new java.math.BigDecimal("500"), "synthetic-bearer");
        verify(model).complete(anyList(), eq(List.of()), eq(false));
        verify(model, times(2)).complete(anyList(), anyList(), eq(false));
    }

    @Test
    void invalidSecondCallPreventsWholeBatch() {
        when(model.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(
                                new ToolCall("a", "function", "get_product", "{\"product_id\":1}"),
                                new ToolCall("b", "function", "get_cart", "{\"customer_id\":9}")));
        assertTrue(ask().contains("couldn't interpret"));
        verifyNoInteractions(commerce);
    }

    @Test
    void comparisonRetainsCallIdsAndRejectsAnotherToolRound() {
        when(commerce.product(anyLong(), anyString())).thenReturn(Map.of("name", "Demo"));
        LlmResponse batch =
                calls(
                        new ToolCall("a", "function", "get_product", "{\"product_id\":1}"),
                        new ToolCall("b", "function", "get_product", "{\"product_id\":2}"));
        when(model.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(batch, answer("Compared"));
        assertEquals("Compared", ask());
        verify(commerce).product(1, "synthetic-bearer");
        verify(commerce).product(2, "synthetic-bearer");
        reset(model);
        when(model.complete(anyList(), anyList(), anyBoolean())).thenReturn(batch, batch);
        assertThrows(AiException.class, () -> ask());
    }
}
