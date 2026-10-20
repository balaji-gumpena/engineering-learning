package com.example.aiassistant.rag;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.document.Document;

import java.util.Map;

public record SourcePassage(
        String title,
        String section,
        String excerpt,
        @JsonProperty("citation_id") String citationId,
        Double score) {
    public static SourcePassage from(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new SourcePassage(
                String.valueOf(metadata.getOrDefault("title", "Demo source")),
                String.valueOf(metadata.getOrDefault("section", "")),
                String.valueOf(metadata.getOrDefault("excerpt", document.getText())),
                document.getId(),
                document.getScore());
    }
}
