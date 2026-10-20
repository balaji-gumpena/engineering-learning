package com.example.aiassistant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatTransportTest {
    @Test
    void validBrowserRequestUsesTheLessonServiceAndPreservesTheReplyContract() throws Exception {
        BasicChatService service = mock(BasicChatService.class);
        when(service.answer("Hi")).thenReturn(new ChatReply("Fixture reply"));
        MockMvc mvc =
                MockMvcBuilders.standaloneSetup(new BasicChatController(service))
                        .setControllerAdvice(new ChatErrorHandler())
                        .build();
        mvc.perform(
                        post("/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"messages\":[{\"role\":\"user\",\"content\":\"Hi\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Fixture reply"));
        verify(service).answer("Hi");
    }
}
