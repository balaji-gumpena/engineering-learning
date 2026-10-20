package com.example.aiassistant.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public final class ChatDtos {
    private ChatDtos() {}

    public record ChatMessage(
            @Pattern(regexp = "user|assistant") @NotBlank String role, @NotBlank String content) {}

    public record ChatRequest(
            @NotEmpty @Size(max = 30) List<@NotNull @Valid ChatMessage> messages,
            @JsonProperty("learning_mode") @JsonInclude(JsonInclude.Include.NON_NULL)
                    Boolean learningMode) {
        public void validateConversation() {
            if (!messages.getLast().role().equals("user")
                    || messages.stream()
                            .anyMatch(
                                    message ->
                                            message.content()
                                                            .codePointCount(
                                                                    0, message.content().length())
                                                    > 4000)) {
                throw new AiException(
                        422,
                        "End with a user message; each message is limited to 4000 characters.");
            }
        }
    }

    public record TokenUsage(
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("output_tokens") Integer outputTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
        public static TokenUsage unknown() {
            return new TokenUsage(null, null, null);
        }

        public TokenUsage plus(TokenUsage other) {
            return new TokenUsage(
                    add(inputTokens, other.inputTokens),
                    add(outputTokens, other.outputTokens),
                    add(totalTokens, other.totalTokens));
        }

        private static Integer add(Integer a, Integer b) {
            return a == null || b == null ? null : a + b;
        }
    }

    public record ProductSummary(
            long id,
            String name,
            @JsonProperty("price_eur") @JsonFormat(shape = Shape.STRING) BigDecimal priceEur,
            String currency,
            @JsonProperty("image_path") String imagePath,
            @JsonProperty("category_name") String categoryName) {}

    public record SourceCitation(
            @JsonProperty("citation_id") String citationId,
            String title,
            String section,
            String excerpt,
            @JsonProperty("source_path") String sourcePath,
            @JsonProperty("product_slug") String productSlug,
            boolean demo) {}

    public record ChatResponse(
            String reply,
            String model,
            TokenUsage usage,
            List<ProductSummary> products,
            @JsonProperty("total_matches") Integer totalMatches,
            @JsonProperty("has_more") boolean hasMore,
            List<SourceCitation> sources,
            @JsonProperty("execution_trace") @JsonInclude(JsonInclude.Include.NON_EMPTY)
                    List<ChatExecutionStep> executionTrace) {
        public ChatResponse(
                String reply,
                String model,
                TokenUsage usage,
                List<ProductSummary> products,
                Integer totalMatches,
                boolean hasMore,
                List<SourceCitation> sources) {
            this(reply, model, usage, products, totalMatches, hasMore, sources, List.of());
        }
    }

    public record ChatExecutionStep(
            String stage,
            String status,
            String tool,
            @JsonProperty("duration_ms") Long durationMs,
            @JsonProperty("result_count") Integer resultCount,
            @JsonProperty("source_ids") List<String> sourceIds) {}
}
