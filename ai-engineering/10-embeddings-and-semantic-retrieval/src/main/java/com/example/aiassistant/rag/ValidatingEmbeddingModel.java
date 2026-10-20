package com.example.aiassistant.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Validate every inference boundary, including direct CLI and VectorStore calls. */
public final class ValidatingEmbeddingModel implements EmbeddingModel {
    private final EmbeddingModel delegate;
    private final ToIntFunction<String> tokens;
    private final int dimensions;
    private final int maxTokens;

    public ValidatingEmbeddingModel(
            EmbeddingModel delegate, ToIntFunction<String> tokens, int dimensions, int maxTokens) {
        this.delegate = delegate;
        this.tokens = tokens;
        this.dimensions = dimensions;
        this.maxTokens = maxTokens;
    }

    @Override
    public synchronized EmbeddingResponse call(EmbeddingRequest request) {
        for (String text : request.getInstructions()) {
            if (text == null || text.isBlank() || tokens.applyAsInt(text) > maxTokens) {
                throw new IllegalArgumentException(
                        "Embedding input is blank or exceeds its token budget");
            }
        }
        EmbeddingResponse response = delegate.call(request);
        if (response == null || response.getResults().size() != request.getInstructions().size()) {
            throw new IllegalArgumentException("Embedding count does not match input count");
        }
        List<Embedding> normalized = new ArrayList<>();
        for (Embedding embedding : response.getResults()) {
            float[] vector = embedding.getOutput().clone();
            validate(vector, dimensions);
            double norm = 0;
            for (float value : vector) {
                norm += (double) value * value;
            }
            norm = Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
            normalized.add(new Embedding(vector, embedding.getIndex(), embedding.getMetadata()));
        }
        return new EmbeddingResponse(normalized, response.getMetadata());
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public String getEmbeddingContent(Document document) {
        return document.getText();
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    public static void validate(float[] vector, int dimensions) {
        if (vector == null || vector.length != dimensions) {
            throw new IllegalArgumentException("Invalid embedding dimensions");
        }
        boolean nonzero = false;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Non-finite embedding");
            }
            nonzero |= value != 0;
        }
        if (!nonzero) {
            throw new IllegalArgumentException("Zero embedding cannot be used for cosine search");
        }
    }
}
