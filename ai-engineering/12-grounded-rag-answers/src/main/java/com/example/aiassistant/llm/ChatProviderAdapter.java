package com.example.aiassistant.llm;

import com.example.aiassistant.AiProperties.ProviderSettings;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/** Adapts an API protocol, not a provider brand, to Spring AI. */
public interface ChatProviderAdapter {
    String id();

    ChatModel createModel(ProviderSettings provider, String apiKey);

    ChatOptions requestOptions(
            ProviderSettings provider, List<ToolCallback> tools, boolean requireTool);
}
