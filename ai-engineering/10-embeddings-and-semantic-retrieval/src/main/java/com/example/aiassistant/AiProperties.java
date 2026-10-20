package com.example.aiassistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ConfigurationProperties("ai")
@Validated
public record AiProperties(
        @NotBlank String commerceUrl,
        @NotEmpty List<String> allowedOrigins,
        @NotBlank String activeProvider,
        @NotEmpty Map<String, @NotNull @Valid ProviderSettings> providers,
        @NotEmpty List<@NotNull @Valid ChatSuggestion> suggestions,
        @NotNull @Valid RagSettings rag) {
    @AssertTrue
    public boolean isProviderConfigured() {
        return providers != null && providers.containsKey(activeProvider);
    }

    @AssertTrue
    public boolean isSuggestionIdsUnique() {
        return suggestions != null
                && suggestions.stream()
                                .filter(Objects::nonNull)
                                .map(ChatSuggestion::id)
                                .distinct()
                                .count()
                        == suggestions.size();
    }

    public ProviderSettings provider() {
        ProviderSettings provider = providers.get(activeProvider);
        if (provider == null) {
            throw new IllegalStateException("Configured AI provider is unknown");
        }
        return provider;
    }

    public record ProviderSettings(
            @NotBlank @Pattern(regexp = "[a-z][a-z0-9-]*") String adapter,
            @NotBlank String baseUrl,
            @NotBlank String model,
            @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]*") String apiKeyEnv,
            String thinking,
            String reasoningEffort,
            String effort) {
        public ProviderSettings {
            adapter = stripped(adapter);
            baseUrl = stripped(baseUrl);
            model = stripped(model);
            apiKeyEnv = stripped(apiKeyEnv);
            thinking = stripped(thinking);
            reasoningEffort = stripped(reasoningEffort);
            effort = stripped(effort);
        }
    }

    public record ChatSuggestion(
            @NotBlank String id,
            @NotBlank @Size(max = 80) String label,
            @NotBlank @Size(max = 500) String prompt) {
        public ChatSuggestion {
            id = stripped(id);
            label = stripped(label);
            prompt = stripped(prompt);
        }
    }

    private static String stripped(String value) {
        return value == null ? null : value.strip();
    }

    public record RagSettings(
            boolean enabled,
            boolean bootstrap,
            @NotBlank String sourceDirectory,
            @NotBlank String modelUrl,
            @NotBlank String tokenizerUrl,
            @NotBlank String cacheDirectory,
            @Min(1) @Max(2000) int dimensions,
            @Min(32) @Max(256) int maxTokens,
            @Min(0) @Max(100) int overlapWords,
            @Min(1) @Max(8) int topK,
            @DecimalMin("-1") @DecimalMax("1") double minScore,
            @Min(1000) @Max(12000) int maxContextCharacters) {
        public String fingerprint() {
            return hash(
                    String.join(
                            "|",
                            "java-rag-v2",
                            modelUrl,
                            tokenizerUrl,
                            String.valueOf(dimensions),
                            String.valueOf(maxTokens),
                            String.valueOf(overlapWords)));
        }
    }

    public static String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
