package com.example.aiassistant.chat;

import com.example.aiassistant.chat.ChatDtos.ChatResponse;
import com.example.aiassistant.chat.ChatDtos.TokenUsage;
import com.example.aiassistant.commerce.CommerceApi.ProductSearchResult;
import com.example.aiassistant.execution.ExecutionObservation;
import com.example.aiassistant.execution.ObservedOperationException;
import com.example.aiassistant.execution.ObservedOperationResult;
import com.example.aiassistant.llm.LlmGateway;
import com.example.aiassistant.llm.LlmGateway.LlmResponse;
import com.example.aiassistant.tools.ToolDefinitions;
import com.example.aiassistant.tools.ToolService;
import com.example.aiassistant.tools.ToolService.ToolExecutionResult;
import com.example.aiassistant.tools.ToolService.ValidatedToolCall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatOrchestrator {
    private static final Logger LOG = LoggerFactory.getLogger(ChatOrchestrator.class);
    private static final int MAX_ANSWER_CHARACTERS = 4000;
    private static final Pattern CITATION = Pattern.compile("\\[([A-Za-z0-9_-]+:[^\\]\\r\\n]+)\\]");
    private final LlmGateway model;
    private final ToolService tools;
    private final JsonMapper json;
    private final String instructions;

    public ChatOrchestrator(LlmGateway model, ToolService tools, JsonMapper json)
            throws IOException {
        this.model = model;
        this.tools = tools;
        this.json = json;
        instructions =
                new ClassPathResource("system-prompt.txt")
                        .getContentAsString(StandardCharsets.UTF_8);
    }

    public ChatDtos.ChatResponse chat(
            List<ChatDtos.ChatMessage> history, String bearer, String requestId) {
        return chat(history, bearer, requestId, false);
    }

    public ChatDtos.ChatResponse chat(
            List<ChatDtos.ChatMessage> history,
            String bearer,
            String requestId,
            boolean learningMode) {
        long started = System.nanoTime();
        ChatExecutionTrace trace = new ChatExecutionTrace(learningMode);
        try {
            ChatResponse response = respond(history, bearer, requestId, trace);
            LOG.info(
                    "chat request_id={} model={} latency_ms={} input_tokens={} output_tokens={}",
                    requestId,
                    response.model(),
                    (System.nanoTime() - started) / 1_000_000,
                    response.usage().inputTokens(),
                    response.usage().outputTokens());
            return new ChatResponse(
                    response.reply(),
                    response.model(),
                    response.usage(),
                    response.products(),
                    response.totalMatches(),
                    response.hasMore(),
                    response.sources(),
                    trace.steps());
        } catch (RuntimeException error) {
            LOG.warn(
                    "chat request_id={} error_type={}",
                    requestId,
                    error.getClass().getSimpleName());
            if (learningMode) {
                trace.record("request", "failed", null);
                throw new ChatExecutionException(error, trace.steps());
            }
            throw error;
        }
    }

    private ChatDtos.ChatResponse respond(
            List<ChatDtos.ChatMessage> history,
            String bearer,
            String requestId,
            ChatExecutionTrace trace) {
        List<Message> messages = messagesFor(history);
        LlmResponse initialLlmResponse =
                trace.measure(
                        "initial_model",
                        null,
                        () -> model.complete(messages, ToolDefinitions.ALL, false));
        if (initialLlmResponse.message().getToolCalls().isEmpty()) {
            trace.record("tool_selection", "none", null);
            return plain(initialLlmResponse, answer(initialLlmResponse.message()));
        }
        List<ToolService.ValidatedToolCall> calls;
        try {
            calls =
                    trace.measure(
                            "tool_validation",
                            null,
                            () -> tools.validate(initialLlmResponse.message().getToolCalls()));
        } catch (RuntimeException error) {
            trace.record("tool_selection", "rejected", null);
            return plain(
                    initialLlmResponse,
                    "I couldn't interpret that store lookup. Check the product/order ID or rephrase your question.");
        }
        for (ValidatedToolCall call : calls) {
            trace.record("tool_selection", "validated", call.name());
        }
        messages.add(initialLlmResponse.message());
        ToolBatchResult batch = executeTools(calls, bearer, requestId, trace);
        if (batch.knowledgeRequested()
                && (batch.knowledgeUnavailable() || batch.sources().isEmpty())) {
            return plain(
                    initialLlmResponse,
                    batch.knowledgeUnavailable()
                            ? "Store knowledge is temporarily unavailable. Please try again later."
                            : "I couldn't find supporting information in the store knowledge sources. Please rephrase or contact support; I can't confirm an answer.");
        }
        messages.add(ToolResponseMessage.builder().responses(batch.responses()).build());
        // One bounded batch followed by final generation. No agent or retry loop.
        LlmResponse finalLlmResponse =
                trace.measure(
                        "final_model", null, () -> model.complete(messages, List.of(), false));
        if (!finalLlmResponse.message().getToolCalls().isEmpty()) {
            trace.record("final_answer", "rejected", null);
            throw new AiException(502, "The model did not return a final answer.");
        }
        String text = answer(finalLlmResponse.message());
        TokenUsage usage = initialLlmResponse.usage().plus(finalLlmResponse.usage());
        Set<String> cited = citationIds(text);
        if (batch.knowledgeRequested()
                && (cited.isEmpty() || !batch.sources().keySet().containsAll(cited))) {
            trace.record("citation_validation", "rejected", null);
            return new ChatDtos.ChatResponse(
                    "I couldn't verify the source references for that answer. Please try again.",
                    finalLlmResponse.model(),
                    usage,
                    List.of(),
                    null,
                    false,
                    List.of());
        }
        if (batch.knowledgeRequested()) {
            trace.record(
                    "citation_validation",
                    "success",
                    null,
                    null,
                    cited.size(),
                    cited.stream().sorted().toList());
        }
        ProductSearchResult search = batch.search();
        return new ChatDtos.ChatResponse(
                text,
                finalLlmResponse.model(),
                usage,
                search == null
                        ? batch.products().values().stream().limit(10).toList()
                        : search.products(),
                search == null ? null : search.totalMatches(),
                search != null && search.hasMore(),
                batch.sources().values().stream()
                        .filter(source -> cited.contains(source.citationId()))
                        .toList());
    }

    private List<Message> messagesFor(List<ChatDtos.ChatMessage> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(instructions));
        for (ChatDtos.ChatMessage message : history) {
            messages.add(
                    message.role().equals("user")
                            ? new UserMessage(message.content().strip())
                            : new AssistantMessage(message.content().strip()));
        }
        return messages;
    }

    private ToolBatchResult executeTools(
            List<ValidatedToolCall> calls,
            String bearer,
            String requestId,
            ChatExecutionTrace trace) {
        List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
        Map<String, ChatDtos.SourceCitation> sources = new LinkedHashMap<>();
        Map<Long, ChatDtos.ProductSummary> products = new LinkedHashMap<>();
        ProductSearchResult search = null;
        boolean knowledgeRequested = false;
        boolean knowledgeUnavailable = false;
        for (ValidatedToolCall call : calls) {
            ObservedOperationResult<ToolExecutionResult> observedExecution;
            try {
                observedExecution =
                        ExecutionObservation.measure(
                                "tool_execution", call.name(), () -> tools.execute(call, bearer));
            } catch (ObservedOperationException error) {
                trace.addAll(error.executionEvents());
                throw error.operationCause();
            }
            ToolExecutionResult execution = observedExecution.value();
            trace.addAll(execution.executionEvents());
            trace.addAll(observedExecution.executionEvents());
            if (execution.knowledge()) {
                String outcome =
                        execution.unavailable()
                                ? "unavailable"
                                : execution.sources().isEmpty() ? "empty" : "success";
                trace.record(
                        "retrieval_outcome",
                        outcome,
                        call.name(),
                        null,
                        execution.sources().size(),
                        execution.sources().stream()
                                .map(ChatDtos.SourceCitation::citationId)
                                .toList());
            }
            responses.add(
                    new ToolResponseMessage.ToolResponse(
                            call.id(), call.name(), json.writeValueAsString(execution.payload())));
            execution.sources().forEach(source -> sources.put(source.citationId(), source));
            execution.products().forEach(product -> products.put(product.id(), product));
            if (execution.search() != null) {
                search = execution.search();
            }
            knowledgeRequested |= execution.knowledge();
            knowledgeUnavailable |= execution.unavailable();
            LOG.info("chat request_id={} tool={}", requestId, call.name());
        }
        return new ToolBatchResult(
                responses, sources, products, search, knowledgeRequested, knowledgeUnavailable);
    }

    private Set<String> citationIds(String text) {
        Set<String> cited = new HashSet<>();
        Matcher matcher = CITATION.matcher(text);
        while (matcher.find()) {
            cited.add(matcher.group(1));
        }
        return cited;
    }

    private record ToolBatchResult(
            List<ToolResponseMessage.ToolResponse> responses,
            Map<String, ChatDtos.SourceCitation> sources,
            Map<Long, ChatDtos.ProductSummary> products,
            ProductSearchResult search,
            boolean knowledgeRequested,
            boolean knowledgeUnavailable) {}

    private ChatDtos.ChatResponse plain(LlmResponse llmResponse, String text) {
        return new ChatDtos.ChatResponse(
                text, llmResponse.model(), llmResponse.usage(), List.of(), null, false, List.of());
    }

    private String answer(AssistantMessage message) {
        String text = message.getText();
        if (text == null || text.isBlank()) {
            throw new AiException(502, "The model returned no answer.");
        }
        text = text.strip();
        return text.codePointCount(0, text.length()) <= MAX_ANSWER_CHARACTERS
                ? text
                : text.substring(0, text.offsetByCodePoints(0, MAX_ANSWER_CHARACTERS - 1)) + "…";
    }
}
