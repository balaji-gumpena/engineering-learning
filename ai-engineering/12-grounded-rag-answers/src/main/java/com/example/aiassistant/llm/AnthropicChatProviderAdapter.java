package com.example.aiassistant.llm;

import com.anthropic.models.messages.OutputConfig.Effort;
import com.anthropic.models.messages.ToolChoice;
import com.anthropic.models.messages.ToolChoiceAny;
import com.example.aiassistant.AiProperties.ProviderSettings;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class AnthropicChatProviderAdapter implements ChatProviderAdapter {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_OUTPUT_TOKENS = 1200;

    @Override
    public String id() {
        return "anthropic";
    }

    @Override
    public ChatModel createModel(ProviderSettings provider, String apiKey) {
        AnthropicChatOptions options =
                options(provider).baseUrl(provider.baseUrl()).apiKey(apiKey).build();
        return AnthropicChatModel.builder().options(options).build();
    }

    @Override
    public AnthropicChatOptions requestOptions(
            ProviderSettings provider, List<ToolCallback> tools, boolean requireTool) {
        AnthropicChatOptions.Builder builder = options(provider).toolCallbacks(tools);
        if (requireTool) {
            builder.toolChoice(ToolChoice.ofAny(ToolChoiceAny.builder().build()));
        }
        return builder.build();
    }

    private AnthropicChatOptions.Builder options(ProviderSettings provider) {
        AnthropicChatOptions.Builder builder =
                AnthropicChatOptions.builder()
                        .model(provider.model())
                        .timeout(REQUEST_TIMEOUT)
                        .maxRetries(0)
                        .maxTokens(MAX_OUTPUT_TOKENS);
        if (provider.effort() != null) {
            builder.thinkingAdaptive().effort(Effort.of(provider.effort()));
        }
        return builder;
    }
}
