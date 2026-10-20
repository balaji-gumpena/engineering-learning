package com.example.aiassistant.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.AiProperties.ProviderSettings;
import com.example.aiassistant.TestProperties;
import com.example.aiassistant.chat.AiException;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class SpringAiGatewayTest {
    private final List<Message> messages = List.of(new UserMessage("Hello"));

    @Test
    void springDiscoversANewProtocolAdapterWithoutGatewayChanges() {
        ProviderSettings provider = provider("native-api", "native-model", "TEST_ONLY_KEY");
        ChatProviderAdapter adapter = adapter("native-api", provider);
        new ApplicationContextRunner()
                .withBean(AiProperties.class, () -> properties(Map.of("title", provider)))
                .withBean(ChatProviderAdapter.class, () -> adapter)
                .withBean(SpringAiGateway.class)
                .withPropertyValues("TEST_ONLY_KEY=synthetic")
                .run(
                        context -> {
                            LlmGateway gateway = context.getBean(LlmGateway.class);
                            assertEquals(
                                    "native-model",
                                    gateway.complete("title", messages, List.of(), false).model());
                            verify(adapter).createModel(provider, "synthetic");
                        });
    }

    @Test
    void multipleProvidersStayIsolatedAndExplicitCallsDoNotChangeTheDefault() {
        ProviderSettings title = provider("first", "title-model", "TITLE_TEST_KEY");
        ProviderSettings content = provider("second", "content-model", "CONTENT_TEST_KEY");
        ChatProviderAdapter titleAdapter = adapter("first", title);
        ChatProviderAdapter contentAdapter = adapter("second", content);
        SpringAiGateway gateway =
                new SpringAiGateway(
                        properties(Map.of("title", title, "content", content)),
                        new MockEnvironment()
                                .withProperty("TITLE_TEST_KEY", "synthetic-title")
                                .withProperty("CONTENT_TEST_KEY", "synthetic-content"),
                        List.of(titleAdapter, contentAdapter));

        assertEquals(
                "content-model", gateway.complete("content", messages, List.of(), false).model());
        assertEquals("title-model", gateway.complete(messages, List.of(), false).model());
        assertEquals(
                "content-model", gateway.complete("content", messages, List.of(), false).model());
        assertEquals("title-model", gateway.complete(messages, List.of(), false).model());

        verify(titleAdapter).createModel(title, "synthetic-title");
        verify(contentAdapter).createModel(content, "synthetic-content");
        verify(titleAdapter, times(2)).requestOptions(title, List.of(), false);
        verify(contentAdapter, times(2)).requestOptions(content, List.of(), false);
    }

    @Test
    void concurrentCallsInitializeEachProviderOnlyOnce() throws Exception {
        ProviderSettings provider = provider("native-api", "title-model", "TEST_ONLY_KEY");
        ProviderSettings content = provider("native-api", "content-model", "CONTENT_TEST_KEY");
        ChatProviderAdapter adapter = adapter("native-api", provider);
        ChatModel contentModel = mock(ChatModel.class);
        when(adapter.createModel(content, "synthetic-content")).thenReturn(contentModel);
        when(adapter.requestOptions(content, List.of(), false))
                .thenReturn(ChatOptions.builder().build());
        when(contentModel.call(any(Prompt.class)))
                .thenReturn(
                        new ChatResponse(
                                List.of(
                                        new Generation(
                                                new AssistantMessage("Synthetic content")))));
        SpringAiGateway gateway =
                new SpringAiGateway(
                        properties(Map.of("title", provider, "content", content)),
                        new MockEnvironment()
                                .withProperty("TEST_ONLY_KEY", "synthetic")
                                .withProperty("CONTENT_TEST_KEY", "synthetic-content"),
                        List.of(adapter));
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService workers = Executors.newFixedThreadPool(8)) {
            List<Future<String>> results = new ArrayList<>();
            for (int call = 0; call < 8; call++) {
                String providerId = call % 2 == 0 ? "title" : "content";
                results.add(
                        workers.submit(
                                () -> {
                                    start.await();
                                    return gateway.complete(providerId, messages, List.of(), false)
                                            .model();
                                }));
            }
            start.countDown();
            for (int call = 0; call < results.size(); call++) {
                String expectedModel = call % 2 == 0 ? "title-model" : "content-model";
                assertEquals(expectedModel, results.get(call).get(5, TimeUnit.SECONDS));
            }
        }
        verify(adapter).createModel(provider, "synthetic");
        verify(adapter).createModel(content, "synthetic-content");
        verify(adapter, times(4)).requestOptions(provider, List.of(), false);
        verify(adapter, times(4)).requestOptions(content, List.of(), false);
    }

    @Test
    void missingSecondaryCredentialsDoNotBlockTheDefaultOrTriggerFallback() {
        ProviderSettings title = provider("native-api", "title-model", "TITLE_TEST_KEY");
        ProviderSettings content = provider("native-api", "content-model", "CONTENT_TEST_KEY");
        ChatProviderAdapter adapter = adapter("native-api", title);
        SpringAiGateway gateway =
                new SpringAiGateway(
                        properties(Map.of("title", title, "content", content)),
                        new MockEnvironment().withProperty("TITLE_TEST_KEY", "synthetic"),
                        List.of(adapter));

        assertEquals("title-model", gateway.complete(messages, List.of(), false).model());
        AiException failure =
                assertThrows(
                        AiException.class,
                        () -> gateway.complete("content", messages, List.of(), false));
        assertEquals(503, failure.status());
        verify(adapter, never()).createModel(eq(content), anyString());
        verify(adapter, times(1)).requestOptions(title, List.of(), false);
    }

    @Test
    void unknownProviderFailsBeforeCredentialsOrModelsAreAccessed() {
        ProviderSettings provider = provider("native-api", "title-model", "TEST_ONLY_KEY");
        ChatProviderAdapter adapter = adapter("native-api", provider);
        Environment environment = mock(Environment.class);
        SpringAiGateway gateway =
                new SpringAiGateway(
                        properties(Map.of("title", provider)), environment, List.of(adapter));
        clearInvocations(adapter);

        for (String providerId : new String[] {"unknown", "", null}) {
            AiException failure =
                    assertThrows(
                            AiException.class,
                            () -> gateway.complete(providerId, messages, List.of(), false));
            assertEquals(503, failure.status());
        }
        verifyNoInteractions(environment, adapter);
    }

    @Test
    void duplicateAndUnregisteredAdaptersAreRejectedAtStartup() {
        ProviderSettings provider = provider("native-api", "title-model", "TEST_ONLY_KEY");
        ChatProviderAdapter adapter = adapter("native-api", provider);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SpringAiGateway(
                                properties(Map.of("title", provider)),
                                new MockEnvironment(),
                                List.of(adapter, adapter)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SpringAiGateway(
                                properties(Map.of("title", provider)),
                                new MockEnvironment(),
                                List.of()));
    }

    @Test
    void emptyProviderResponsesPreserveTheExisting502Error() {
        ProviderSettings provider = provider("native-api", "title-model", "TEST_ONLY_KEY");
        ChatProviderAdapter adapter = adapter("native-api", provider);
        ChatModel model = adapter.createModel(provider, "synthetic");
        when(model.call(any(Prompt.class))).thenReturn(null, new ChatResponse(List.of()));
        SpringAiGateway gateway =
                new SpringAiGateway(
                        properties(Map.of("title", provider)),
                        new MockEnvironment().withProperty("TEST_ONLY_KEY", "synthetic"),
                        List.of(adapter));
        for (int call = 0; call < 2; call++) {
            AiException failure =
                    assertThrows(
                            AiException.class, () -> gateway.complete(messages, List.of(), false));
            assertEquals(502, failure.status());
        }
    }

    private ChatProviderAdapter adapter(String id, ProviderSettings provider) {
        ChatProviderAdapter adapter = mock(ChatProviderAdapter.class);
        ChatModel model = mock(ChatModel.class);
        when(adapter.id()).thenReturn(id);
        when(adapter.createModel(eq(provider), anyString())).thenReturn(model);
        when(adapter.requestOptions(provider, List.of(), false))
                .thenReturn(ChatOptions.builder().build());
        when(model.call(any(Prompt.class)))
                .thenReturn(
                        new ChatResponse(
                                List.of(new Generation(new AssistantMessage("Synthetic answer")))));
        return adapter;
    }

    private ProviderSettings provider(String adapter, String model, String keyEnvironment) {
        return new ProviderSettings(
                adapter, "http://provider.test", model, keyEnvironment, null, null, null);
    }

    private AiProperties properties(Map<String, ProviderSettings> providers) {
        AiProperties baseline = TestProperties.create();
        return new AiProperties(
                baseline.commerceUrl(),
                baseline.allowedOrigins(),
                "title",
                providers,
                baseline.suggestions(),
                baseline.rag());
    }
}
