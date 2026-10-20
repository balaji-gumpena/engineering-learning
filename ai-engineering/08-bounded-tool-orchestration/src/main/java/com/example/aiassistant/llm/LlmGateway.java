package com.example.aiassistant.llm;

import com.example.aiassistant.chat.ChatDtos.TokenUsage;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

public interface LlmGateway {
    LlmResponse complete(List<Message> messages, List<ToolCallback> tools, boolean requireTool);

    /** Select a configured provider/model alias for this call, without changing the default. */
    LlmResponse complete(
            String providerId,
            List<Message> messages,
            List<ToolCallback> tools,
            boolean requireTool);

    /** One LLM call's assistant message (text or tool requests), model name and token usage. */
    record LlmResponse(AssistantMessage message, String model, TokenUsage usage) {}
}
