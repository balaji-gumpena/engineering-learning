package com.example.aiassistant.rag;

public record KnowledgeDocument(
        String sourceId,
        String title,
        String sourceType,
        String productSlug,
        String sourcePath,
        String contentHash,
        String body) {}
