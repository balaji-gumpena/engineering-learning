package com.example.aiassistant.rag;

import com.example.aiassistant.AiProperties;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@Configuration
public class RagConfiguration {
    @Bean
    @Lazy
    TransformersEmbeddingModel transformersModel(AiProperties properties) {
        TransformersEmbeddingModel model = new TransformersEmbeddingModel();
        model.setModelResource(properties.rag().modelUrl());
        model.setTokenizerResource(properties.rag().tokenizerUrl());
        model.setResourceCacheDirectory(properties.rag().cacheDirectory());
        model.setTokenizerOptions(Map.of("padding", "true", "truncation", "false"));
        return model;
    }

    @Bean
    @Lazy
    @Primary
    EmbeddingModel embeddingModel(
            @Lazy TransformersEmbeddingModel transformers,
            @Lazy ModelTokenizer tokenizer,
            AiProperties properties) {
        return new ValidatingEmbeddingModel(
                transformers,
                tokenizer::count,
                properties.rag().dimensions(),
                properties.rag().maxTokens());
    }
}
