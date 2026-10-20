package com.example.aiassistant.llm;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.AiProperties.ProviderSettings;
import com.example.aiassistant.chat.AiException;
import com.example.aiassistant.chat.ChatDtos.TokenUsage;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Executes calls against explicitly selected providers; it does not make routing decisions. */
@Component
public class SpringAiGateway implements LlmGateway {
    private final String defaultProviderId;
    private final Map<String, ProviderSettings> providers;
    private final Map<String, ChatProviderAdapter> adapters;
    private final Environment environment;
    private final ConcurrentMap<String, ChatModel> models = new ConcurrentHashMap<>();

    public SpringAiGateway(
            AiProperties config, Environment environment, List<ChatProviderAdapter> adapters) {
        this.defaultProviderId = config.activeProvider();
        this.providers = Map.copyOf(config.providers());
        this.environment = environment;
        this.adapters = registerAdapters(adapters);
        for (ProviderSettings provider : providers.values()) {
            if (!this.adapters.containsKey(provider.adapter())) {
                throw new IllegalArgumentException(
                        "No registered AI adapter: " + provider.adapter());
            }
        }
    }

    private Map<String, ChatProviderAdapter> registerAdapters(
            List<ChatProviderAdapter> availableAdapters) {
        Map<String, ChatProviderAdapter> registered = new HashMap<>();
        for (ChatProviderAdapter adapter : availableAdapters) {
            if (registered.putIfAbsent(adapter.id(), adapter) != null) {
                throw new IllegalArgumentException("Duplicate AI adapter: " + adapter.id());
            }
        }
        return Map.copyOf(registered);
    }

    @Override
    public LlmResponse complete(
            List<Message> messages, List<ToolCallback> tools, boolean requireTool) {
        return complete(defaultProviderId, messages, tools, requireTool);
    }

    @Override
    public LlmResponse complete(
            String providerId,
            List<Message> messages,
            List<ToolCallback> tools,
            boolean requireTool) {
        ProviderSettings provider = configuredProvider(providerId);
        ChatProviderAdapter adapter = adapters.get(provider.adapter());
        ChatModel model =
                models.computeIfAbsent(providerId, ignored -> createModel(provider, adapter));
        ChatOptions options = adapter.requestOptions(provider, tools, requireTool);
        // Adapters expose tool requests; only the orchestrator validates and executes them.
        ChatResponse response = model.call(new Prompt(messages, options));
        return toLlmResponse(response, provider);
    }

    private ProviderSettings configuredProvider(String providerId) {
        ProviderSettings provider = providerId == null ? null : providers.get(providerId);
        if (provider == null) {
            throw new AiException(503, "Requested AI provider is not configured.");
        }
        return provider;
    }

    private ChatModel createModel(ProviderSettings provider, ChatProviderAdapter adapter) {
        String apiKey = environment.getProperty(provider.apiKeyEnv());
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiException(
                    503, "AI service is not configured. Set " + provider.apiKeyEnv() + ".");
        }
        return adapter.createModel(provider, apiKey);
    }

    private LlmResponse toLlmResponse(ChatResponse response, ProviderSettings provider) {
        if (response == null || response.getResults().isEmpty()) {
            throw new AiException(502, "The model returned no answer.");
        }
        org.springframework.ai.chat.metadata.Usage usage = response.getMetadata().getUsage();
        String modelName = response.getMetadata().getModel();
        return new LlmResponse(
                response.getResult().getOutput(),
                modelName == null || modelName.isBlank() ? provider.model() : modelName,
                usage == null || usage instanceof EmptyUsage
                        ? TokenUsage.unknown()
                        : new TokenUsage(
                                usage.getPromptTokens(),
                                usage.getCompletionTokens(),
                                usage.getTotalTokens()));
    }
}
