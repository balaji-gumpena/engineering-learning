package com.example.aiassistant.rag;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;

import com.example.aiassistant.AiProperties;

import org.springframework.ai.transformers.ResourceCacheService;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/** Uses the embedding model's tokenizer, not a different LLM's token counter. */
@Component
@Lazy
public class ModelTokenizer implements AutoCloseable {
    private final HuggingFaceTokenizer tokenizer;

    public ModelTokenizer(AiProperties config) throws IOException {
        ResourceCacheService cache = new ResourceCacheService(config.rag().cacheDirectory());
        Resource resource =
                cache.getCachedResource(
                        new DefaultResourceLoader().getResource(config.rag().tokenizerUrl()));
        try (InputStream input = resource.getInputStream()) {
            tokenizer =
                    HuggingFaceTokenizer.newInstance(
                            input, Map.of("truncation", "false", "padding", "false"));
        }
    }

    public synchronized int count(String text) {
        return tokenizer.encode(text).getIds().length;
    }

    @Override
    public void close() {
        tokenizer.close();
    }
}
