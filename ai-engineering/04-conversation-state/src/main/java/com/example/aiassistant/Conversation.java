package com.example.aiassistant;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

public final class Conversation {
    private Conversation() {}

    public static List<Message> messages(ChatRequest request) {
        if (request.messages() == null
                || request.messages().isEmpty()
                || request.messages().size() > 30
                || !"user".equals(request.messages().getLast().role())) {
            throw new InvalidConversationException(
                    "Supply up to 30 messages ending with a user message.");
        }
        List<Message> result = new ArrayList<>();
        result.add(new SystemMessage("You are a helpful assistant. Admit uncertainty."));
        for (ChatMessage message : request.messages()) {
            if (message == null
                    || message.content() == null
                    || message.content().isBlank()
                    || message.content().codePointCount(0, message.content().length()) > 4000
                    || !List.of("user", "assistant").contains(message.role())) {
                throw new InvalidConversationException("Invalid history message.");
            }
            result.add(
                    message.role().equals("user")
                            ? new UserMessage(message.content().strip())
                            : new AssistantMessage(message.content().strip()));
        }
        return result;
    }
}
