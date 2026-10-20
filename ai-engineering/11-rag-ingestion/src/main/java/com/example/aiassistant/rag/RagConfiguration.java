package com.example.aiassistant.rag;

import com.example.aiassistant.AiProperties;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Bean
    @Lazy
    VectorStore knowledgeVectorStore(
            JdbcTemplate jdbc, @Lazy EmbeddingModel embeddings, AiProperties properties) {
        return PgVectorStore.builder(jdbc, embeddings)
                .schemaName("rag_java")
                .vectorTableName("knowledge_chunks")
                .idType(PgVectorStore.PgIdType.TEXT)
                .dimensions(properties.rag().dimensions())
                .indexType(PgVectorStore.PgIndexType.NONE)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(true)
                .vectorTableValidationsEnabled(true)
                .build();
    }
}
