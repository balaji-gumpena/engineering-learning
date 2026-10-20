package com.example.aiassistant;

import java.util.List;
import java.util.Map;

public final class TestProperties {
    private TestProperties() {}

    public static AiProperties create() {
        return new AiProperties(
                "http://commerce.test",
                List.of("http://localhost:5173"),
                "test",
                Map.of(
                        "test",
                        new AiProperties.ProviderSettings(
                                "openai",
                                "http://provider.test/v1",
                                "test-model",
                                "TEST_ONLY_KEY",
                                null,
                                null,
                                null)),
                List.of(new AiProperties.ChatSuggestion("help", "Help", "Help me shop")),
                new AiProperties.RagSettings(
                        true,
                        false,
                        "./knowledge",
                        "model-url",
                        "tokenizer-url",
                        ".cache/test",
                        384,
                        240,
                        20,
                        4,
                        .25,
                        6000));
    }
}
