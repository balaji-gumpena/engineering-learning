package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

class ValidatingEmbeddingModelTest {
    @Test
    void rejectsInvalidInputsBeforeInference() {
        EmbeddingModel delegate = mock(EmbeddingModel.class);
        ValidatingEmbeddingModel model =
                new ValidatingEmbeddingModel(delegate, String::length, 2, 10);
        assertThrows(IllegalArgumentException.class, () -> model.embed(" "));
        assertThrows(IllegalArgumentException.class, () -> model.embed("x".repeat(11)));
        verifyNoInteractions(delegate);
    }

    @Test
    void validatesCountsDimensionsAndFiniteNonzeroVectors() {
        EmbeddingModel delegate = mock(EmbeddingModel.class);
        ValidatingEmbeddingModel model =
                new ValidatingEmbeddingModel(delegate, String::length, 2, 10);
        for (float[] vector :
                List.of(
                        new float[] {0, 0},
                        new float[] {1},
                        new float[] {Float.NaN, 1},
                        new float[] {1, Float.POSITIVE_INFINITY})) {
            when(delegate.call(any()))
                    .thenReturn(new EmbeddingResponse(List.of(new Embedding(vector, 0))));
            assertThrows(IllegalArgumentException.class, () -> model.embed("valid"));
        }
        when(delegate.call(any())).thenReturn(new EmbeddingResponse(List.of()));
        assertThrows(IllegalArgumentException.class, () -> model.embed("valid"));
        when(delegate.call(any()))
                .thenReturn(new EmbeddingResponse(List.of(new Embedding(new float[] {3, 4}, 0))));
        assertArrayEquals(new float[] {.6f, .8f}, model.embed("valid"), .00001f);
    }
}
