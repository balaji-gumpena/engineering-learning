package com.example.aiassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.mock.env.MockEnvironment;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

class FirstCallTest {
    @Test
    void rolesModelAndUsageCrossTheRealSdkAgainstALoopbackFixture() throws Exception {
        AtomicReference<String> request = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/chat/completions",
                exchange -> {
                    request.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    byte[] body =
                            "{\"id\":\"fixture\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"fixture-model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"Hello from fixture\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}"
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
        try {
            MockEnvironment environment =
                    new MockEnvironment()
                            .withProperty("OPENAI_API_KEY", "synthetic-fixture")
                            .withProperty(
                                    "lesson.base-url",
                                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
                            .withProperty("lesson.model", "fixture-model");
            ChatResponse response =
                    FirstCall.ask(FirstCall.model(environment), "Admit uncertainty", "Hello");
            assertEquals("Hello from fixture", response.getResult().getOutput().getText());
            assertEquals(15, response.getMetadata().getUsage().getTotalTokens());
            assertTrue(request.get().contains("system"));
            assertTrue(request.get().contains("fixture-model"));
            assertFalse(request.get().contains("synthetic-fixture"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void credentialsAndBlankQuestionsFailBeforeNetworkAccess() {
        assertThrows(IllegalArgumentException.class, () -> FirstCall.model(new MockEnvironment()));
        assertThrows(IllegalArgumentException.class, () -> FirstCall.ask(null, "Helpful", " "));
    }
}
