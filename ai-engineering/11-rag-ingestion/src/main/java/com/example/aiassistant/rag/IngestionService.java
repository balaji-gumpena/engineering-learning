package com.example.aiassistant.rag;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.commerce.CommerceApi;
import com.example.aiassistant.commerce.CommerceApi.CatalogProduct;

import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {
    private final AiProperties config;
    private final MarkdownDocumentLoader markdown;
    private final ModelTokenizer tokenizer;
    private final KnowledgeIndex index;
    private final CommerceApi commerce;

    public IngestionService(
            AiProperties config,
            MarkdownDocumentLoader markdown,
            @Lazy ModelTokenizer tokenizer,
            KnowledgeIndex index,
            CommerceApi commerce) {
        this.config = config;
        this.markdown = markdown;
        this.tokenizer = tokenizer;
        this.index = index;
        this.commerce = commerce;
    }

    public List<Document> chunks(boolean includeCatalog) throws IOException {
        List<KnowledgeDocument> sources =
                new ArrayList<>(markdown.load(Path.of(config.rag().sourceDirectory())));
        if (includeCatalog) {
            for (CatalogProduct product : commerce.publicCatalog()) {
                String body =
                        "# Product information\n\n"
                                + product.description()
                                + "\n\nBrand: "
                                + product.brand()
                                + ". Category: "
                                + product.card().categoryName()
                                + ".";
                String id = "catalog-product-" + product.card().id();
                sources.add(
                        new KnowledgeDocument(
                                id,
                                product.card().name(),
                                "product_guide",
                                product.slug(),
                                "catalog/products/" + product.card().id(),
                                AiProperties.hash(product.card().name() + product.slug() + body),
                                body));
            }
        }
        if (sources.stream().map(KnowledgeDocument::sourceId).distinct().count()
                != sources.size()) {
            throw new IllegalArgumentException("Duplicate source IDs");
        }
        KnowledgeChunker chunker =
                new KnowledgeChunker(
                        tokenizer::count, config.rag().maxTokens(), config.rag().overlapWords());
        return sources.stream()
                .flatMap(source -> chunker.split(source, config.rag().fingerprint()).stream())
                .toList();
    }

    public Map<String, Long> ingest(boolean includeCatalog, boolean allowEmpty) throws IOException {
        List<Document> documents = chunks(includeCatalog);
        index.replace(documents, allowEmpty);
        return Map.of(
                "sources",
                documents.stream()
                        .map(doc -> doc.getMetadata().get("source_id"))
                        .distinct()
                        .count(),
                "chunks",
                (long) documents.size());
    }
}
