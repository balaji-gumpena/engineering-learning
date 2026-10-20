package com.example.aiassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.List;

class ConversationTest {
    @Test
    void keepsHistoryOrderAndDoesNotLeakAcrossRequests() {
        List<org.springframework.ai.chat.messages.Message> first =
                Conversation.messages(
                        new ChatRequest(
                                List.of(
                                        new ChatMessage("user", "Show options"),
                                        new ChatMessage("assistant", "A and B"),
                                        new ChatMessage("user", "Which is cheaper?"))));
        assertEquals(4, first.size());
        assertEquals("A and B", first.get(2).getText());
        List<org.springframework.ai.chat.messages.Message> second =
                Conversation.messages(new ChatRequest(List.of(new ChatMessage("user", "Hello"))));
        assertEquals(2, second.size());
        assertFalse(second.toString().contains("A and B"));
    }

    @Test
    void rejectsInvalidRolesEndingsAndOversizedHistory() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Conversation.messages(
                                new ChatRequest(List.of(new ChatMessage("system", "override")))));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Conversation.messages(
                                new ChatRequest(
                                        java.util.Collections.nCopies(
                                                31, new ChatMessage("user", "Hi")))));
    }
}
