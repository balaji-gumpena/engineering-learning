package com.example.aiassistant.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.AiProperties.ProviderSettings;
import com.example.aiassistant.TestProperties;
import com.example.aiassistant.tools.ToolDefinitions;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.MimeTypeUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Real SDK requests to loopback fixtures, not to the named providers. */
class CompatibleProvidersTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final ConcurrentMap<String, JsonNode> requests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> authorizations = new ConcurrentHashMap<>();

    @Test
    void deepSeekAndGrokAliasesUseIndependentEndpointsCredentialsModelsAndOptions()
            throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        registerProvider(server, "deepseek");
        registerProvider(server, "grok");
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            AiProperties baseline = TestProperties.create();
            AiProperties properties =
                    new AiProperties(
                            baseline.commerceUrl(),
                            baseline.allowedOrigins(),
                            "deepseek",
                            Map.of(
                                    "deepseek",
                                    new ProviderSettings(
                                            "openai",
                                            baseUrl + "/deepseek/v1",
                                            "deepseek-test-model",
                                            "DEEPSEEK_TEST_KEY",
                                            "disabled",
                                            null,
                                            null),
                                    "grok",
                                    new ProviderSettings(
                                            "openai",
                                            baseUrl + "/grok/v1",
                                            "grok-test-model",
                                            "GROK_TEST_KEY",
                                            null,
                                            null,
                                            null)),
                            baseline.suggestions(),
                            baseline.rag());
            LlmGateway gateway =
                    new SpringAiGateway(
                            properties,
                            new MockEnvironment()
                                    .withProperty("DEEPSEEK_TEST_KEY", "synthetic-deepseek")
                                    .withProperty("GROK_TEST_KEY", "synthetic-grok"),
                            TestProperties.providerAdapters());
            List<Message> messages = List.of(new UserMessage("Synthetic question"));

            assertEquals(
                    "deepseek-test-model",
                    gateway.complete("deepseek", messages, ToolDefinitions.ALL, true).model());
            assertEquals("deepseek-test-model", requests.get("deepseek").path("model").asText());
            assertEquals("required", requests.get("deepseek").path("tool_choice").asText());
            assertEquals(
                    "disabled", requests.get("deepseek").path("thinking").path("type").asText());
            assertEquals("Bearer synthetic-deepseek", authorizations.get("deepseek"));

            String imageUrl = baseUrl + "/synthetic-image.png";
            UserMessage imageQuestion =
                    UserMessage.builder()
                            .text("Describe the synthetic image")
                            .media(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl)))
                            .build();
            assertEquals(
                    "grok-test-model",
                    gateway.complete("grok", List.of(imageQuestion), List.of(), false).model());
            assertEquals("grok-test-model", requests.get("grok").path("model").asText());
            assertEquals("none", requests.get("grok").path("tool_choice").asText());
            assertTrue(requests.get("grok").path("thinking").isMissingNode());
            assertEquals("Bearer synthetic-grok", authorizations.get("grok"));
            JsonNode imageContent =
                    requests.get("grok").path("messages").get(0).path("content").get(1);
            assertEquals("image_url", imageContent.path("type").asText());
            assertEquals(imageUrl, imageContent.path("image_url").path("url").asText());

            assertEquals(
                    "deepseek-test-model", gateway.complete(messages, List.of(), false).model());
            assertEquals("none", requests.get("deepseek").path("tool_choice").asText());
        } finally {
            server.stop(0);
        }
    }

    private void registerProvider(HttpServer server, String providerId) {
        server.createContext(
                "/" + providerId + "/v1/chat/completions",
                exchange -> {
                    requests.put(
                            providerId,
                            json.readTree(
                                    new String(
                                            exchange.getRequestBody().readAllBytes(),
                                            StandardCharsets.UTF_8)));
                    authorizations.put(
                            providerId, exchange.getRequestHeaders().getFirst("Authorization"));
                    String response =
                            """
                    {"id":"synthetic","object":"chat.completion","created":1,"model":"%s-test-model",
                     "choices":[{"index":0,"message":{"role":"assistant","content":"Synthetic answer"},"finish_reason":"stop"}]}
                    """
                                    .formatted(providerId);
                    byte[] body = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
    }
}
