package com.example.aiassistant;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class BasicChatService {
    private final Environment environment;

    public BasicChatService(Environment environment) {
        this.environment = environment;
    }

    public ChatReply answer(String question) {
        ChatResponse response =
                FirstCall.ask(
                        FirstCall.model(environment),
                        environment.getProperty(
                                "lesson.instructions",
                                "You are a helpful assistant. Admit uncertainty."),
                        question);
        return new ChatReply(response.getResult().getOutput().getText());
    }
}
