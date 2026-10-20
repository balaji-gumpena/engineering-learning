package com.example.aiassistant.rag;

import com.example.aiassistant.AiProperties;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Service
public class SemanticSearch {
    private final AiProperties properties;
    private final EmbeddingModel embeddings;
    private final ModelTokenizer tokenizer;

    public SemanticSearch(
            AiProperties properties,
            @Lazy EmbeddingModel embeddings,
            @Lazy ModelTokenizer tokenizer) {
        this.properties = properties;
        this.embeddings = embeddings;
        this.tokenizer = tokenizer;
    }

    public List<Document> search(String question) throws IOException {
        RetrievalQueryValidator.validate(
                question, null, properties.rag().topK(), properties.rag().minScore());
        if (question.length() > 4000) {
            throw new IllegalArgumentException("Invalid question");
        }
        return inMemorySearch(null, null, properties.rag().topK(), properties.rag().minScore())
                .apply(question);
    }

    public Function<String, List<Document>> inMemorySearch(
            String productSlug, String sourceType, int limit, double minimumScore)
            throws IOException {
        List<Document> documents =
                new MarkdownDocumentLoader()
                        .load(Path.of(properties.rag().sourceDirectory())).stream()
                                .flatMap(
                                        source ->
                                                new KnowledgeChunker(
                                                                tokenizer::count,
                                                                properties.rag().maxTokens(),
                                                                properties.rag().overlapWords())
                                                                .split(
                                                                        source,
                                                                        properties
                                                                                .rag()
                                                                                .fingerprint())
                                                                .stream())
                                .toList();
        List<float[]> documentVectors =
                embeddings.embed(documents.stream().map(Document::getText).toList());
        return question -> {
            float[] queryVector = embeddings.embed(question);
            List<Document> hits = new ArrayList<>();
            for (int documentIndex = 0; documentIndex < documents.size(); documentIndex++) {
                Document document = documents.get(documentIndex);
                if (!matchesFilters(document, productSlug, sourceType)) {
                    continue;
                }
                double score = cosine(queryVector, documentVectors.get(documentIndex));
                if (score >= minimumScore) {
                    hits.add(
                            Document.builder()
                                    .id(document.getId())
                                    .text(document.getText())
                                    .metadata(document.getMetadata())
                                    .score(score)
                                    .build());
                }
            }
            return hits.stream()
                    .sorted(
                            Comparator.comparingDouble((Document document) -> document.getScore())
                                    .reversed()
                                    .thenComparing(Document::getId))
                    .limit(limit)
                    .toList();
        };
    }

    private boolean matchesFilters(Document document, String productSlug, String sourceType) {
        boolean matchesSource =
                sourceType == null || sourceType.equals(document.getMetadata().get("source_type"));
        boolean matchesProduct =
                productSlug == null
                        || productSlug.equals(document.getMetadata().get("product_slug"));
        return matchesSource && matchesProduct;
    }

    private static double cosine(float[] queryVector, float[] documentVector) {
        double dotProduct = 0;
        double querySquaredNorm = 0;
        double documentSquaredNorm = 0;
        for (int dimension = 0; dimension < queryVector.length; dimension++) {
            dotProduct += (double) queryVector[dimension] * documentVector[dimension];
            querySquaredNorm += (double) queryVector[dimension] * queryVector[dimension];
            documentSquaredNorm += (double) documentVector[dimension] * documentVector[dimension];
        }
        return Math.max(
                -1, Math.min(1, dotProduct / Math.sqrt(querySquaredNorm * documentSquaredNorm)));
    }
}
