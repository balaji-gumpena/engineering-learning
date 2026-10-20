package com.example.aiassistant.tools;

import com.example.aiassistant.chat.ChatDtos.ProductSummary;
import com.example.aiassistant.chat.ChatDtos.SourceCitation;
import com.example.aiassistant.commerce.CommerceApi;
import com.example.aiassistant.commerce.CommerceApi.ProductSearchResult;
import com.example.aiassistant.execution.ExecutionEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ToolService {
    private static final int MAX_TOOL_CALLS = 4;
    private static final int MAX_KNOWLEDGE_QUESTION_CHARACTERS = 1200;
    private static final BigDecimal MAX_SEARCH_PRICE = new BigDecimal("100000000");
    private final CommerceApi commerce;
    private final JsonMapper json;

    public ToolService(CommerceApi commerce, JsonMapper json) {
        this.commerce = commerce;
        this.json = json;
    }

    public List<ValidatedToolCall> validate(List<ToolCall> calls) {
        if (calls.isEmpty() || calls.size() > MAX_TOOL_CALLS) {
            throw new IllegalArgumentException("Expected one to four calls");
        }
        List<ValidatedToolCall> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (ToolCall call : calls) {
            if (!"function".equals(call.type())
                    || call.id() == null
                    || call.id().isBlank()
                    || !ids.add(call.id())) {
                throw new IllegalArgumentException("Invalid tool call ID/type");
            }
            JsonNode arguments = json.readTree(call.arguments());
            if (arguments == null || !arguments.isObject()) {
                throw new IllegalArgumentException("Expected an argument object");
            }
            Set<String> allowedArguments =
                    switch (call.name()) {
                        case "search_products" -> Set.of("max_price_eur_exclusive");
                        case "get_product" -> Set.of("product_id");
                        case "get_order" -> Set.of("order_id");
                        case "get_cart", "get_orders" -> Set.of();
                        default -> throw new IllegalArgumentException("Unknown tool");
                    };
            if (arguments.properties().stream()
                    .anyMatch(entry -> !allowedArguments.contains(entry.getKey()))) {
                throw new IllegalArgumentException("Unexpected tool argument");
            }
            validateArguments(call.name(), arguments);
            result.add(new ValidatedToolCall(call.id(), call.name(), arguments));
        }
        return result;
    }

    private void validateArguments(String toolName, JsonNode arguments) {
        switch (toolName) {
            case "get_product" -> validatePositiveId(arguments.path("product_id"));
            case "get_order" -> validatePositiveId(arguments.path("order_id"));
            case "search_products" -> validatePrice(arguments.path("max_price_eur_exclusive"));
            default -> {}
        }
    }

    private void validatePrice(JsonNode value) {
        if (!value.isNumber()) {
            throw new IllegalArgumentException("Price must be numeric");
        }
        BigDecimal price = new BigDecimal(value.asText());
        if (price.signum() <= 0 || price.compareTo(MAX_SEARCH_PRICE) > 0) {
            throw new IllegalArgumentException("Invalid price bound");
        }
    }

    private void validatePositiveId(JsonNode value) {
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() <= 0) {
            throw new IllegalArgumentException("Expected a positive integral ID");
        }
    }

    public ToolExecutionResult execute(ValidatedToolCall call, String bearer) {
        JsonNode arguments = call.arguments;
        return switch (call.name) {
            case "get_product" ->
                    ToolExecutionResult.commerce(
                            commerce.product(arguments.path("product_id").asLong(), bearer));
            case "get_order" ->
                    ToolExecutionResult.commerce(
                            commerce.order(arguments.path("order_id").asLong(), bearer));
            case "get_cart" -> ToolExecutionResult.commerce(commerce.cart(bearer));
            case "get_orders" -> ToolExecutionResult.commerce(commerce.orders(bearer));
            case "search_products" -> {
                ProductSearchResult result =
                        commerce.search(
                                new BigDecimal(arguments.path("max_price_eur_exclusive").asText()),
                                bearer);
                yield new ToolExecutionResult(
                        json.valueToTree(result),
                        List.of(),
                        result.products(),
                        result,
                        false,
                        false);
            }
            default -> throw new IllegalArgumentException("Unknown tool");
        };
    }

    public record ValidatedToolCall(String id, String name, JsonNode arguments) {}

    public record ToolExecutionResult(
            Object payload,
            List<SourceCitation> sources,
            List<ProductSummary> products,
            CommerceApi.ProductSearchResult search,
            boolean knowledge,
            boolean unavailable,
            @JsonIgnore List<ExecutionEvent> executionEvents) {
        public ToolExecutionResult {
            executionEvents = List.copyOf(executionEvents);
        }

        public ToolExecutionResult(
                Object payload,
                List<SourceCitation> sources,
                List<ProductSummary> products,
                CommerceApi.ProductSearchResult search,
                boolean knowledge,
                boolean unavailable) {
            this(payload, sources, products, search, knowledge, unavailable, List.of());
        }

        static ToolExecutionResult commerce(Object payload) {
            return new ToolExecutionResult(payload, List.of(), List.of(), null, false, false);
        }
    }
}
