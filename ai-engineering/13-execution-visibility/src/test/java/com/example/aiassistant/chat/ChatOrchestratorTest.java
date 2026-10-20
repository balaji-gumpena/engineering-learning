package com.example.aiassistant.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.aiassistant.chat.ChatDtos.ChatResponse;
import com.example.aiassistant.chat.ChatDtos.ProductSummary;
import com.example.aiassistant.chat.ChatDtos.SourceCitation;
import com.example.aiassistant.commerce.CommerceApi;
import com.example.aiassistant.execution.ExecutionEvent;
import com.example.aiassistant.llm.LlmGateway;
import com.example.aiassistant.llm.LlmGateway.LlmResponse;
import com.example.aiassistant.rag.KnowledgeService;
import com.example.aiassistant.tools.ToolService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class ChatOrchestratorTest {
    LlmGateway llm = mock(LlmGateway.class);
    CommerceApi commerce = mock(CommerceApi.class);
    KnowledgeService knowledge = mock(KnowledgeService.class);
    JsonMapper json = JsonMapper.builder().build();
    ChatOrchestrator chat;

    @BeforeEach
    void setup() throws Exception {
        chat = new ChatOrchestrator(llm, new ToolService(commerce, knowledge, json), json);
    }

    LlmGateway.LlmResponse answer(String text) {
        return new LlmGateway.LlmResponse(
                new AssistantMessage(text), "test-model", new ChatDtos.TokenUsage(10, 5, 15));
    }

    LlmGateway.LlmResponse calls(AssistantMessage.ToolCall... calls) {
        return new LlmGateway.LlmResponse(
                AssistantMessage.builder().content("").toolCalls(List.of(calls)).build(),
                "test-model",
                new ChatDtos.TokenUsage(10, 5, 15));
    }

    AssistantMessage.ToolCall call(String id, String name, String args) {
        return new AssistantMessage.ToolCall(id, "function", name, args);
    }

    ChatDtos.ChatResponse ask(String text) {
        return chat.chat(
                List.of(new ChatDtos.ChatMessage("user", text)),
                "synthetic-bearer",
                "request-test");
    }

    ChatResponse askWithTrace(String text) {
        return chat.chat(
                List.of(new ChatDtos.ChatMessage("user", text)),
                "synthetic-bearer",
                "trace-test",
                true);
    }

    ChatDtos.ChatExecutionStep step(ChatResponse response, String stage) {
        return response.executionTrace().stream()
                .filter(event -> event.stage().equals(stage))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void tracingIsOptInAndDoesNotChangePlainReplies() {
        when(llm.complete(anyList(), anyList(), anyBoolean())).thenReturn(answer("Hello."));
        assertTrue(ask("Hello").executionTrace().isEmpty());
        ChatResponse result = askWithTrace("Hello");
        assertEquals("Hello.", result.reply());
        assertEquals("none", step(result, "tool_selection").status());
        assertTrue(step(result, "initial_model").durationMs() >= 0);
        verifyNoInteractions(commerce, knowledge);
    }

    @Test
    void commerceTraceRecordsValidatedAndExecutedToolButNoPrivatePayload() {
        when(commerce.cart(anyString())).thenReturn(Map.of("private-item", "private-cart-data"));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(calls(call("private-call-id", "get_cart", "{}")), answer("Your cart."));
        ChatResponse result = askWithTrace("My cart");
        assertEquals("get_cart", step(result, "tool_selection").tool());
        assertEquals("validated", step(result, "tool_selection").status());
        assertEquals("get_cart", step(result, "tool_execution").tool());
        assertEquals("success", step(result, "tool_execution").status());
        String traceJson = json.writeValueAsString(result.executionTrace());
        assertFalse(traceJson.contains("private-"));
        assertFalse(traceJson.contains("synthetic-bearer"));
        assertFalse(traceJson.contains("My cart"));
        verify(commerce).cart("synthetic-bearer");
    }

    @Test
    void rejectedBatchDoesNotExposeUntrustedNamesOrExecuteAnyTool() {
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(
                                call("a", "get_cart", "{}"),
                                call(
                                        "b",
                                        "private-untrusted-tool",
                                        "{\"private-argument\":true}")));
        ChatResponse result = askWithTrace("Look up store information");
        assertEquals("rejected", step(result, "tool_selection").status());
        assertEquals("failed", step(result, "tool_validation").status());
        assertFalse(json.writeValueAsString(result.executionTrace()).contains("private-"));
        assertTrue(
                result.executionTrace().stream()
                        .noneMatch(event -> event.stage().equals("tool_execution")));
        verifyNoInteractions(commerce, knowledge);
    }

    @Test
    void knowledgeTraceDistinguishesRetainedEvidenceFromCitedSources() {
        SourceCitation first =
                new SourceCitation(
                        "returns:0", "Returns", "Window", "30 days", "returns.md", null, true);
        SourceCitation second =
                new SourceCitation(
                        "returns:1", "Returns", "Postage", "Demo", "returns.md", null, true);
        when(knowledge.lookup(anyString(), isNull()))
                .thenReturn(
                        new KnowledgeService.KnowledgeRetrievalResult(
                                List.of(first, second),
                                List.of(),
                                List.of(),
                                null,
                                List.of(
                                        new ExecutionEvent(
                                                "query_embedding",
                                                "success",
                                                "search_knowledge",
                                                1L,
                                                null,
                                                List.of()))));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(call("a", "search_knowledge", "{\"question\":\"private-question\"}")),
                        answer("30 days. [returns:0]"));
        ChatResponse result = askWithTrace("Return policy");
        assertTrue(
                result.executionTrace().stream()
                        .noneMatch(event -> event.stage().equals("knowledge_required")));
        assertEquals(
                List.of("returns:0", "returns:1"), step(result, "retrieval_outcome").sourceIds());
        assertEquals(2, step(result, "retrieval_outcome").resultCount());
        assertEquals(List.of("returns:0"), step(result, "citation_validation").sourceIds());
        assertEquals(List.of(first), result.sources());
        int embeddingStep =
                result.executionTrace().stream()
                        .map(ChatDtos.ChatExecutionStep::stage)
                        .toList()
                        .indexOf("query_embedding");
        int toolStep =
                result.executionTrace().stream()
                        .map(ChatDtos.ChatExecutionStep::stage)
                        .toList()
                        .indexOf("tool_execution");
        assertTrue(embeddingStep >= 0 && embeddingStep < toolStep);
        assertFalse(json.writeValueAsString(result.executionTrace()).contains("private-question"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void emptyAndUnavailableKnowledgeKeepTraceAndSkipFinalGeneration(boolean unavailable) {
        when(knowledge.lookup(anyString(), isNull()))
                .thenReturn(
                        new KnowledgeService.KnowledgeRetrievalResult(
                                List.of(),
                                List.of(),
                                List.of(),
                                unavailable ? "knowledge_unavailable" : null));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(calls(call("a", "search_knowledge", "{\"question\":\"Returns?\"}")));
        ChatResponse result = askWithTrace("Return policy");
        assertEquals(
                unavailable ? "unavailable" : "empty", step(result, "retrieval_outcome").status());
        assertEquals(0, step(result, "retrieval_outcome").resultCount());
        assertTrue(
                result.executionTrace().stream()
                        .noneMatch(event -> event.stage().equals("final_model")));
        verify(llm, times(1)).complete(anyList(), anyList(), anyBoolean());
    }

    @Test
    void modelCanAnswerDirectlyAndRejectedCitationsRemainVisibleAfterKnowledgeLookup() {
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(answer("Unverified policy"));
        ChatResponse direct = askWithTrace("Return policy");
        assertEquals("Unverified policy", direct.reply());
        assertEquals("none", step(direct, "tool_selection").status());
        SourceCitation source =
                new SourceCitation(
                        "returns:0", "Returns", "Window", "30 days", "returns.md", null, true);
        when(knowledge.lookup(anyString(), isNull()))
                .thenReturn(
                        new KnowledgeService.KnowledgeRetrievalResult(
                                List.of(source), List.of(), List.of(), null));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(call("a", "search_knowledge", "{\"question\":\"Returns?\"}")),
                        answer("No citation"));
        ChatResponse result = askWithTrace("Return policy");
        assertEquals("rejected", step(result, "citation_validation").status());
        assertTrue(result.reply().contains("couldn't verify"));
        assertTrue(result.sources().isEmpty());
    }

    @Test
    void failedModelAndToolCallsPreserveSafeFactsAndOriginalStatus() {
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenThrow(new RuntimeException("private-provider-payload"));
        ChatExecutionException providerFailure =
                assertThrows(ChatExecutionException.class, () -> askWithTrace("Hello"));
        assertEquals("initial_model", providerFailure.executionTrace().getFirst().stage());
        assertEquals("failed", providerFailure.executionTrace().getFirst().status());
        assertFalse(json.writeValueAsString(providerFailure.executionTrace()).contains("private-"));

        reset(llm);
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(calls(call("a", "get_order", "{\"order_id\":1}")));
        when(commerce.order(1, "synthetic-bearer"))
                .thenThrow(new AiException(404, "Order not found."));
        ChatExecutionException toolFailure =
                assertThrows(ChatExecutionException.class, () -> askWithTrace("My order"));
        assertEquals(
                404,
                new ApiExceptionHandler().executionFailure(toolFailure).getStatusCode().value());
        assertEquals(
                "failed",
                toolFailure.executionTrace().stream()
                        .filter(event -> event.stage().equals("tool_execution"))
                        .findFirst()
                        .orElseThrow()
                        .status());
    }

    @Test
    void greetingUsesOneCall() {
        when(llm.complete(anyList(), anyList(), anyBoolean())).thenReturn(answer("Hello."));
        assertEquals("Hello.", ask("Hello").reply());
        verifyNoInteractions(commerce, knowledge);
    }

    @Test
    void productComparisonUsesOneValidatedBatchAndSumsTokens() {
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(
                                call("a", "get_product", "{\"product_id\":1}"),
                                call("b", "get_product", "{\"product_id\":2}")),
                        answer("Compared."));
        when(commerce.product(anyLong(), anyString())).thenReturn(Map.of("name", "Demo"));
        ChatResponse result = ask("Compare products 1 and 2");
        verify(commerce).product(1, "synthetic-bearer");
        verify(commerce).product(2, "synthetic-bearer");
        assertEquals(30, result.usage().totalTokens());
        assertTrue(result.products().isEmpty());
    }

    @Test
    void invalidSecondCallPreventsTheEntireBatch() {
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(
                                call("a", "get_product", "{\"product_id\":1}"),
                                call("b", "get_cart", "{\"customer_id\":99}")));
        assertTrue(ask("Store lookup").reply().contains("couldn't interpret"));
        verifyNoInteractions(commerce, knowledge);
    }

    @Test
    void modelToolSelectionIsTheOnlyKnowledgeLookupDecision() {
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(answer("Please clarify which part of the return policy you need."));
        assertEquals(
                "Please clarify which part of the return policy you need.",
                ask("What is the return policy?").reply());
        verifyNoInteractions(knowledge);
    }

    @Test
    void policyRetrievesEvidenceAndReturnsOnlyCitedSources() {
        SourceCitation source =
                new ChatDtos.SourceCitation(
                        "returns:0",
                        "Demo returns",
                        "Window",
                        "30 calendar days",
                        "returns.md",
                        null,
                        true);
        when(knowledge.lookup(anyString(), isNull()))
                .thenReturn(
                        new KnowledgeService.KnowledgeRetrievalResult(
                                List.of(source), List.of(), List.of(), null));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(call("a", "search_knowledge", "{\"question\":\"Return window?\"}")),
                        answer("Demo returns allow 30 days. [returns:0]"));
        ChatResponse result = ask("Return policy?");
        assertEquals(List.of(source), result.sources());
        verify(llm)
                .complete(
                        argThat(
                                messages ->
                                        messages.getLast() instanceof ToolResponseMessage
                                                && !messages.toString()
                                                        .contains("synthetic-bearer")),
                        eq(List.of()),
                        eq(false));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "Invented. [unknown:1]",
                "No citation",
                "Partly valid [returns:0] [unknown:1]"
            })
    void inventedOrMissingCitationsWithholdTheAnswer(String text) {
        SourceCitation source =
                new ChatDtos.SourceCitation(
                        "returns:0", "Demo", "Window", "30 days", "returns.md", null, true);
        when(knowledge.lookup(anyString(), isNull()))
                .thenReturn(
                        new KnowledgeService.KnowledgeRetrievalResult(
                                List.of(source), List.of(), List.of(), null));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(call("a", "search_knowledge", "{\"question\":\"Returns?\"}")),
                        answer(text));
        assertTrue(ask("Returns?").reply().contains("couldn't verify"));
    }

    @Test
    void noEvidenceDoesNotInvokeFinalGeneration() {
        when(knowledge.lookup(anyString(), isNull()))
                .thenReturn(
                        new KnowledgeService.KnowledgeRetrievalResult(
                                List.of(), List.of(), List.of(), null));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(calls(call("a", "search_knowledge", "{\"question\":\"Returns?\"}")));
        assertTrue(ask("Returns?").reply().contains("couldn't find supporting"));
        verify(llm, times(1)).complete(anyList(), anyList(), anyBoolean());
    }

    @Test
    void modelToolSelectionIsTheOnlyLivePriceLookupDecision() {
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(answer("Please clarify the product criteria."));
        assertEquals("Please clarify the product criteria.", ask("Products under €500").reply());
        verifyNoInteractions(commerce);
    }

    @Test
    void searchReturnsCardsAndPagination() {
        ProductSummary product =
                new ChatDtos.ProductSummary(1, "Demo", new BigDecimal("49"), "EUR", null, "Demo");
        when(commerce.search(new BigDecimal("500"), "synthetic-bearer"))
                .thenReturn(new CommerceApi.ProductSearchResult(List.of(product), 25, true));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(call("a", "search_products", "{\"max_price_eur_exclusive\":500}")),
                        answer("One example."));
        ChatResponse result = ask("Products under €500");
        assertTrue(result.hasMore());
        assertEquals(25, result.totalMatches());
        assertEquals(product, result.products().getFirst());
    }

    @Test
    void finalToolLoopIsRejected() {
        when(commerce.cart(anyString())).thenReturn(Map.of("items", List.of(), "total", "0"));
        LlmResponse toolCallResponse = calls(call("a", "get_cart", "{}"));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(toolCallResponse, toolCallResponse);
        assertThrows(AiException.class, () -> ask("My cart"));
    }

    @Test
    void knowledgeAndBudgetResultsShareOneBatchWithCitationsAndCards() {
        SourceCitation source =
                new ChatDtos.SourceCitation(
                        "returns:0", "Demo returns", "Window", "30 days", "returns.md", null, true);
        ProductSummary product =
                new ChatDtos.ProductSummary(1, "Demo", new BigDecimal("49"), "EUR", null, "Demo");
        when(knowledge.lookup(anyString(), isNull()))
                .thenReturn(
                        new KnowledgeService.KnowledgeRetrievalResult(
                                List.of(source), List.of(), List.of(), null));
        when(commerce.search(new BigDecimal("50"), "synthetic-bearer"))
                .thenReturn(new CommerceApi.ProductSearchResult(List.of(product), 1, false));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(
                                call("a", "search_products", "{\"max_price_eur_exclusive\":50}"),
                                call("b", "search_knowledge", "{\"question\":\"Return window\"}")),
                        answer("Demo returns: 30 days. [returns:0]"));
        ChatResponse result = ask("Products under €50 and their return window?");
        assertEquals(List.of(source), result.sources());
        assertEquals(List.of(product), result.products());
    }

    @Test
    void recentOrderReturnRequestUsesOrdersAndKnowledgeFromOneModelSelectedBatch() {
        SourceCitation source =
                new ChatDtos.SourceCitation(
                        "returns:0", "Demo returns", "Window", "30 days", "returns.md", null, true);
        when(knowledge.lookup(anyString(), isNull()))
                .thenReturn(
                        new KnowledgeService.KnowledgeRetrievalResult(
                                List.of(source), List.of(), List.of(), null));
        when(commerce.orders("synthetic-bearer"))
                .thenReturn(Map.of("orders", List.of(Map.of("id", 17)), "total_orders", 1));
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(
                        calls(
                                call("call-1", "get_orders", "{}"),
                                call(
                                        "call-2",
                                        "search_knowledge",
                                        "{\"question\":\"What is the return policy for a recent order?\"}")),
                        answer(
                                "Your recent order is subject to the demo return terms. [returns:0]"));

        ChatResponse result = ask("What is the return policy for my recent order?");

        assertEquals(List.of(source), result.sources());
        verify(commerce).orders("synthetic-bearer");
        verify(knowledge).lookup("What is the return policy for a recent order?", null);
        verify(llm, times(2)).complete(anyList(), anyList(), eq(false));
    }

    @Test
    void longAnswersCanBeSentBackInHistoryAndUnknownUsagePropagates() {
        when(llm.complete(anyList(), anyList(), anyBoolean()))
                .thenReturn(answer("😀".repeat(4100)), answer("Follow-up"));
        ChatResponse first = ask("Hello");
        assertEquals(4000, first.reply().codePointCount(0, first.reply().length()));
        assertTrue(first.reply().endsWith("…"));
        ChatResponse result =
                chat.chat(
                        List.of(
                                new ChatDtos.ChatMessage("user", "Hello"),
                                new ChatDtos.ChatMessage("assistant", first.reply()),
                                new ChatDtos.ChatMessage("user", "Explain more")),
                        "synthetic-bearer",
                        "follow-up");
        assertEquals("Follow-up", result.reply());
        assertNull(
                new ChatDtos.TokenUsage(10, 5, 15)
                        .plus(ChatDtos.TokenUsage.unknown())
                        .totalTokens());
    }
}
