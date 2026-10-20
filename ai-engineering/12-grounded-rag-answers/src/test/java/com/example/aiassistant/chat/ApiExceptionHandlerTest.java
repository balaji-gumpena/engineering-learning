package com.example.aiassistant.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anthropic.errors.AnthropicServiceException;
import com.example.aiassistant.AiProperties;
import com.example.aiassistant.AiProperties.ProviderSettings;
import com.example.aiassistant.TestProperties;
import com.example.aiassistant.llm.SpringAiGateway;
import com.openai.errors.OpenAIServiceException;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;

import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

class ApiExceptionHandlerTest {
    @Test
    void realSdkTimeoutMapsTo504WithoutALiveProvider() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/chat/completions",
                exchange -> {
                    try {
                        Thread.sleep(17000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        try {
            AiProperties b = TestProperties.create();
            AiProperties config =
                    new AiProperties(
                            b.commerceUrl(),
                            b.allowedOrigins(),
                            "test",
                            Map.of(
                                    "test",
                                    new ProviderSettings(
                                            "openai",
                                            "http://127.0.0.1:"
                                                    + server.getAddress().getPort()
                                                    + "/v1",
                                            "synthetic",
                                            "TEST_ONLY_KEY",
                                            null,
                                            null,
                                            null)),
                            b.suggestions(),
                            b.rag());
            SpringAiGateway gateway =
                    new SpringAiGateway(
                            config,
                            new MockEnvironment().withProperty("TEST_ONLY_KEY", "synthetic"),
                            TestProperties.providerAdapters());
            long started = System.nanoTime();
            RuntimeException failure =
                    assertThrows(
                            RuntimeException.class,
                            () ->
                                    gateway.complete(
                                            List.of(new UserMessage("Hello")), List.of(), false));
            StringBuilder kinds = new StringBuilder();
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                kinds.append(cause.getClass().getName()).append(" -> ");
            }
            assertEquals(504, ApiExceptionHandler.upstreamStatus(failure), kinds.toString());
            assertTrue(
                    (System.nanoTime() - started) / 1_000_000_000.0 < 17,
                    "Per-request options must preserve the 15-second deadline");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void sdkWrappedTimeoutsPreserveGatewayTimeoutAndNeverExposeDetails() {
        RuntimeException error =
                new RuntimeException(
                        "synthetic-private-detail", new SocketTimeoutException("private"));
        ResponseEntity<?> response = new ApiExceptionHandler().unexpected(error);
        assertEquals(504, response.getStatusCode().value());
        assertFalse(response.getBody().toString().contains("private"));
    }

    @Test
    void mapsActualSdkStatusTypesIncludingWrappedFailures() {
        for (int status : new int[] {401, 403, 408, 429, 500, 504}) {
            OpenAIServiceException openai = mock(OpenAIServiceException.class);
            AnthropicServiceException anthropic = mock(AnthropicServiceException.class);
            when(openai.statusCode()).thenReturn(status);
            when(anthropic.statusCode()).thenReturn(status);
            int expected =
                    status == 401 || status == 403
                            ? 503
                            : status == 408 || status == 504 ? 504 : status == 429 ? 429 : 502;
            assertEquals(
                    expected, ApiExceptionHandler.upstreamStatus(new RuntimeException(openai)));
            assertEquals(
                    expected, ApiExceptionHandler.upstreamStatus(new RuntimeException(anthropic)));
        }
    }
}
