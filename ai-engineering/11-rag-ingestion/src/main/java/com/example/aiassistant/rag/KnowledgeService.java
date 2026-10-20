package com.example.aiassistant.rag;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.chat.ChatDtos.ProductSummary;
import com.example.aiassistant.chat.ChatDtos.SourceCitation;
import com.example.aiassistant.commerce.CommerceApi;
import com.example.aiassistant.commerce.CommerceApi.CatalogProduct;
import com.example.aiassistant.execution.ExecutionEvent;
import com.example.aiassistant.execution.ExecutionObservation;
import com.example.aiassistant.execution.ObservedOperationException;
import com.example.aiassistant.execution.ObservedOperationResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeService {
    private final AiProperties config;
    private final KnowledgeIndex index;
    private final ModelTokenizer tokenizer;
    private final CommerceApi commerce;

    public KnowledgeService(
            AiProperties config,
            KnowledgeIndex index,
            @Lazy ModelTokenizer tokenizer,
            CommerceApi commerce) {
        this.config = config;
        this.index = index;
        this.tokenizer = tokenizer;
        this.commerce = commerce;
    }

    public KnowledgeRetrievalResult lookup(String question, String slug) {
        List<ExecutionEvent> events = new ArrayList<>();
        if (!config.rag().enabled()) {
            return new KnowledgeRetrievalResult(
                    List.of(), List.of(), List.of(), "knowledge_unavailable", events);
        }
        try {
            if (tokenizer.count(question) > config.rag().maxTokens()) {
                throw new IllegalArgumentException("Knowledge query is too long");
            }
            ObservedOperationResult<List<Document>> searchResult =
                    index.searchObserved(
                            question, slug, config.rag().topK(), config.rag().minScore());
            events.addAll(searchResult.executionEvents());
            List<Document> hits = searchResult.value();
            Map<String, CommerceApi.CatalogProduct> live = new HashMap<>();
            if (hits.stream().anyMatch(doc -> doc.getMetadata().containsKey("product_slug"))) {
                try {
                    ObservedOperationResult<List<CatalogProduct>> catalogResult =
                            ExecutionObservation.measure(
                                    "live_product_validation",
                                    "search_knowledge",
                                    commerce::publicCatalog);
                    events.addAll(catalogResult.executionEvents());
                    List<CatalogProduct> catalog = catalogResult.value();
                    for (CatalogProduct product : catalog) {
                        live.put(product.slug(), product);
                    }
                } catch (ObservedOperationException error) {
                    events.addAll(error.executionEvents());
                    /* Keep policy evidence; exclude unverifiable product candidates. */
                }
            }
            List<SourceCitation> sources = new ArrayList<>();
            Set<String> included = new LinkedHashSet<>();
            int remaining = config.rag().maxContextCharacters();
            for (Document document : hits) {
                Map<String, Object> metadata = document.getMetadata();
                String productSlug = (String) metadata.get("product_slug");
                if (productSlug != null && !live.containsKey(productSlug)) {
                    continue;
                }
                String excerpt = (String) metadata.get("excerpt");
                if (excerpt.length() > remaining) {
                    break;
                }
                remaining -= excerpt.length();
                sources.add(
                        new SourceCitation(
                                (String) metadata.get("citation_id"),
                                (String) metadata.get("title"),
                                (String) metadata.get("section"),
                                excerpt,
                                (String) metadata.get("source_path"),
                                productSlug,
                                true));
                if (productSlug != null) {
                    included.add(productSlug);
                }
            }
            events.add(
                    new ExecutionEvent(
                            "evidence_selection",
                            sources.isEmpty() ? "empty" : "success",
                            "search_knowledge",
                            null,
                            sources.size(),
                            sources.stream().map(SourceCitation::citationId).toList()));
            return new KnowledgeRetrievalResult(
                    sources,
                    included.stream().map(key -> live.get(key).card()).toList(),
                    included.stream().map(live::get).toList(),
                    null,
                    events);
        } catch (ObservedOperationException error) {
            events.addAll(error.executionEvents());
            LoggerFactory.getLogger(KnowledgeService.class)
                    .warn(
                            "Knowledge unavailable error_type={}",
                            error.operationCause().getClass().getSimpleName());
            return new KnowledgeRetrievalResult(
                    List.of(), List.of(), List.of(), "knowledge_unavailable", events);
        } catch (Exception error) {
            LoggerFactory.getLogger(KnowledgeService.class)
                    .warn("Knowledge unavailable error_type={}", error.getClass().getSimpleName());
            return new KnowledgeRetrievalResult(
                    List.of(), List.of(), List.of(), "knowledge_unavailable", events);
        }
    }

    public record KnowledgeRetrievalResult(
            List<SourceCitation> sources,
            List<ProductSummary> products,
            @JsonProperty("live_products") List<CommerceApi.CatalogProduct> liveProducts,
            String error,
            @JsonIgnore List<ExecutionEvent> executionEvents) {
        public KnowledgeRetrievalResult {
            executionEvents = List.copyOf(executionEvents);
        }

        public KnowledgeRetrievalResult(
                List<SourceCitation> sources,
                List<ProductSummary> products,
                List<CommerceApi.CatalogProduct> liveProducts,
                String error) {
            this(sources, products, liveProducts, error, List.of());
        }
    }
}
