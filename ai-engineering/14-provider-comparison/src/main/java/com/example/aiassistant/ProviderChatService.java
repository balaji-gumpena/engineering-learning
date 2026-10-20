package com.example.aiassistant;

import com.example.aiassistant.llm.LlmGateway;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderChatService {
    private final LlmGateway gateway;
    private final AiProperties config;
    private final Environment environment;

    public ProviderChatService(LlmGateway gateway, AiProperties config, Environment environment) {
        this.gateway = gateway;
        this.config = config;
        this.environment = environment;
    }

    public List<String> aliases() {
        return config.providers().keySet().stream().sorted().toList();
    }

    public ProviderChatReply answer(ProviderChatRequest request) {
        String alias =
                request.provider() == null
                        ? environment.getProperty("lesson.provider", "openai")
                        : request.provider();
        LlmGateway.LlmResponse response =
                gateway.complete(
                        alias,
                        List.of(new UserMessage(request.messages().getFirst().content())),
                        List.of(),
                        false);
        return new ProviderChatReply(
                response.message().getText(), response.model(), response.usage());
    }
}
