package com.example.aiassistant.llm;

import com.example.aiassistant.AiProperties.ProviderSettings;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Shared by OpenAI and providers exposing its chat-completions protocol. */
@Component
public class OpenAiChatProviderAdapter implements ChatProviderAdapter {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    @Override
    public String id() {
        return "openai";
    }

    @Override
    public ChatModel createModel(ProviderSettings provider, String apiKey) {
        OpenAiChatOptions options =
                options(provider).baseUrl(provider.baseUrl()).apiKey(apiKey).build();
        return OpenAiChatModel.builder().options(options).build();
    }

    @Override
    public OpenAiChatOptions requestOptions(
            ProviderSettings provider, List<ToolCallback> tools, boolean requireTool) {
        return options(provider)
                .toolCallbacks(tools)
                .toolChoice(toolChoice(tools, requireTool))
                .build();
    }

    private OpenAiChatOptions.Builder options(ProviderSettings provider) {
        OpenAiChatOptions.Builder builder =
                OpenAiChatOptions.builder()
                        .model(provider.model())
                        .timeout(REQUEST_TIMEOUT)
                        .maxRetries(0);
        if (provider.reasoningEffort() != null) {
            builder.reasoningEffort(provider.reasoningEffort());
        }
        if ("disabled".equals(provider.thinking())) {
            builder.extraBody(Map.of("thinking", Map.of("type", "disabled")));
        }
        return builder;
    }

    private String toolChoice(List<ToolCallback> tools, boolean requireTool) {
        if (tools.isEmpty()) {
            return "none";
        }
        return requireTool ? "required" : "auto";
    }
}
