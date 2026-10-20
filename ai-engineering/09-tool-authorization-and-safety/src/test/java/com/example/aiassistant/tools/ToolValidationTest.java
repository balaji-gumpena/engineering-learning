package com.example.aiassistant.tools;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.example.aiassistant.commerce.CommerceApi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;

import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;

class ToolValidationTest {
    ToolService tools = new ToolService(mock(CommerceApi.class), JsonMapper.builder().build());

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.5", "true", "\"1\"", "9223372036854775808"})
    void idsMustBePositiveIntegralLongs(String value) {
        assertThrows(
                RuntimeException.class,
                () ->
                        tools.validate(
                                List.of(
                                        new ToolCall(
                                                "id",
                                                "function",
                                                "get_order",
                                                "{\"order_id\":" + value + "}"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "true", "\"500\"", "100000001"})
    void priceBoundsAreValidated(String value) {
        assertThrows(
                RuntimeException.class,
                () ->
                        tools.validate(
                                List.of(
                                        new ToolCall(
                                                "id",
                                                "function",
                                                "search_products",
                                                "{\"max_price_eur_exclusive\":" + value + "}"))));
    }

    @Test
    void batchSizeDuplicateIdsAndUnknownToolsAreRejected() {
        ToolCall call = new ToolCall("same", "function", "get_cart", "{}");
        assertThrows(RuntimeException.class, () -> tools.validate(Collections.nCopies(5, call)));
        assertThrows(RuntimeException.class, () -> tools.validate(List.of(call, call)));
        assertThrows(
                RuntimeException.class,
                () -> tools.validate(List.of(new ToolCall("id", "function", "delete_cart", "{}"))));
    }
}
