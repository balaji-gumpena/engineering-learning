package com.example.aiassistant.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class IngestionUiService {
    private final IngestionService ingestion;

    public IngestionUiService(IngestionService ingestion) {
        this.ingestion = ingestion;
    }

    public KnowledgeReply prepare(String action) throws IOException {
        if ("chunk".equals(action)) {
            List<Document> documents = ingestion.chunks(false);
            return new KnowledgeReply(
                    "Prepared "
                            + documents.size()
                            + " chunks from the local demo corpus. The index was not changed.",
                    documents.stream().limit(12).map(SourcePassage::from).toList());
        }
        if ("ingest".equals(action)) {
            Map<String, Long> counts = ingestion.ingest(false, false);
            return new KnowledgeReply(
                    "Indexed "
                            + counts.get("sources")
                            + " demo sources as "
                            + counts.get("chunks")
                            + " chunks in the disposable local index.",
                    List.of());
        }
        throw new IllegalArgumentException("Unknown ingestion action");
    }
}
