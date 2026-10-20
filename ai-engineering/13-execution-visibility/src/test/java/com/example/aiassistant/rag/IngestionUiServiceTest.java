package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

class IngestionUiServiceTest {
    private final IngestionService ingestion = mock(IngestionService.class);
    private final IngestionUiService service = new IngestionUiService(ingestion);

    @Test
    void previewPreservesCitationJsonWithoutWritingAnIndex() throws Exception {
        when(ingestion.chunks(false))
                .thenReturn(
                        List.of(
                                Document.builder()
                                        .id("returns:0")
                                        .text("Demo policy")
                                        .metadata(Map.of("title", "Returns"))
                                        .build()));
        KnowledgeReply reply = service.prepare("chunk");
        assertEquals("returns:0", reply.sources().getFirst().citationId());
        assertTrue(
                JsonMapper.builder()
                        .build()
                        .writeValueAsString(reply)
                        .contains("\"citation_id\":\"returns:0\""));
        verify(ingestion, never()).ingest(false, false);
    }

    @Test
    void rebuildUsesOnlyTheLocalCorpusAndDoesNotAllowEmptyReplacement() throws Exception {
        when(ingestion.ingest(false, false)).thenReturn(Map.of("sources", 7L, "chunks", 12L));
        KnowledgeReply reply = service.prepare("ingest");
        assertTrue(reply.reply().contains("7 demo sources"));
        assertTrue(reply.sources().isEmpty());
        verify(ingestion).ingest(false, false);
    }
}
