package com.example.aiassistant.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class KnowledgeSearchService {
    private final KnowledgeIndex search;

    public KnowledgeSearchService(KnowledgeIndex search) {
        this.search = search;
    }

    public KnowledgeReply answer(String question) throws IOException {
        List<Document> hits = search.search(question, null, 4, 0.25);
        String text =
                hits.isEmpty()
                        ? "No passages matched this search."
                        : "Found "
                                + hits.size()
                                + " matching passages. These are retrieved sources, not a generated answer.";
        return new KnowledgeReply(text, hits.stream().limit(12).map(SourcePassage::from).toList());
    }
}
