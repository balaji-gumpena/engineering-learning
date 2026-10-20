package com.example.aiassistant;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {
    private final Environment environment;

    public ConversationService(Environment environment) {
        this.environment = environment;
    }

    public ChatReply answer(ChatRequest request) {
        List<Message> messages = Conversation.messages(request);
        ChatResponse response = FirstCall.model(environment).call(new Prompt(messages));
        if (response == null
                || response.getResults().isEmpty()
                || response.getResult().getOutput().getText() == null
                || response.getResult().getOutput().getText().isBlank()) {
            throw new IllegalStateException("No answer");
        }
        return new ChatReply(response.getResult().getOutput().getText());
    }
}
