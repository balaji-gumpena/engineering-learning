package com.example.aiassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Actual loopback HTTP to this module. No LLM, embedding inference or external identity request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModuleUiTest {
    @LocalServerPort int port;

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                                .timeout(Duration.ofSeconds(5))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void pageIsServedByThisModule() throws Exception {
        HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("id=\"composer\""));
    }

    @Test
    void chatClientIsServedByThisModule() throws Exception {
        HttpResponse<String> response = get("/chat.js");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("class ChatSession"));
    }

    @Test
    void localLessonConfigurationIsServedByThisModule() throws Exception {
        HttpResponse<String> response = get("/lesson-config.js");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("window.LESSON"));
    }

    @Test
    void invalidChatIsRejectedBeforeAnyModelCall() throws Exception {
        HttpResponse<String> response =
                HttpClient.newHttpClient()
                        .send(
                                HttpRequest.newBuilder(
                                                URI.create("http://127.0.0.1:" + port + "/chat"))
                                        .timeout(Duration.ofSeconds(5))
                                        .header("Content-Type", "application/json")
                                        .POST(
                                                HttpRequest.BodyPublishers.ofString(
                                                        "{\"messages\":[]}"))
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
    }
}
