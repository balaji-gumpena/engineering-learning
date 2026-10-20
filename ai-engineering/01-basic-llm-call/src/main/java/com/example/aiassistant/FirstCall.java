package com.example.aiassistant;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.List;

/** Adapted from SpringAiGateway and OpenAiChatProviderAdapter: one model, one call. */
public final class FirstCall {
    private FirstCall() {}

    public static OpenAiChatModel model(Environment environment) {
        String key = environment.getProperty("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Set OPENAI_API_KEY in your terminal.");
        }
        OpenAiChatOptions options =
                OpenAiChatOptions.builder()
                        .baseUrl(
                                environment.getProperty(
                                        "lesson.base-url", "https://api.openai.com/v1"))
                        .model(environment.getRequiredProperty("lesson.model"))
                        .apiKey(key)
                        .timeout(Duration.ofSeconds(15))
                        .maxRetries(0)
                        .build();
        return OpenAiChatModel.builder().options(options).build();
    }

    public static ChatResponse ask(OpenAiChatModel model, String instructions, String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("A nonblank question is required.");
        }
        ChatResponse response =
                model.call(
                        new Prompt(
                                List.of(
                                        new SystemMessage(instructions),
                                        new UserMessage(question))));
        if (response == null
                || response.getResults().isEmpty()
                || response.getResult().getOutput().getText() == null
                || response.getResult().getOutput().getText().isBlank()) {
            throw new IllegalStateException("The model returned no answer.");
        }
        return response;
    }
}
