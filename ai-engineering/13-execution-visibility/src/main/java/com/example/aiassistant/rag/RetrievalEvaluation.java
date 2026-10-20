package com.example.aiassistant.rag;

import org.springframework.ai.document.Document;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Retrieval diagnostics only; unsupported questions are reported, not graded as abstentions. */
public final class RetrievalEvaluation {
    private RetrievalEvaluation() {}

    public static Map<String, Object> evaluate(
            JsonNode cases, Function<String, List<Document>> search, int limit) {
        if (!cases.isArray() || limit < 1 || limit > 20) {
            throw new IllegalArgumentException("Invalid evaluation cases or limit");
        }
        int count = 0;
        int found = 0;
        double reciprocal = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode item : cases) {
            if (!item.path("question").isString()
                    || item.path("question").asText().isBlank()
                    || !item.has("source_id")
                    || !item.has("section")
                    || !item.path("source_id").isNull()
                            && (!item.path("source_id").isString()
                                    || !item.path("section").isString())) {
                throw new IllegalArgumentException("Invalid evaluation case");
            }
            String question = item.path("question").asText();
            long started = System.nanoTime();
            List<Document> hits = search.apply(question);
            Integer rank = null;
            String expected = item.path("source_id").asText(null);
            String section = item.path("section").asText(null);
            if (expected != null) {
                count++;
                for (int i = 0; i < Math.min(limit, hits.size()); i++) {
                    Map<String, Object> metadata = hits.get(i).getMetadata();
                    if (expected.equals(metadata.get("source_id"))
                            && String.valueOf(metadata.get("section")).contains(section)) {
                        rank = i + 1;
                        break;
                    }
                }
                if (rank != null) {
                    found++;
                    reciprocal += 1.0 / rank;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
            result.put("expected_source", expected);
            result.put("expected_section", section);
            result.put("rank", rank);
            result.put("latency_ms", (System.nanoTime() - started) / 1_000_000.0);
            result.put(
                    "retrieved",
                    hits.stream()
                            .limit(limit)
                            .map(
                                    hit -> {
                                        Map<String, Object> passage = new LinkedHashMap<>();
                                        passage.put(
                                                "source_id", hit.getMetadata().get("source_id"));
                                        passage.put("section", hit.getMetadata().get("section"));
                                        passage.put("score", hit.getScore());
                                        return passage;
                                    })
                            .toList());
            results.add(result);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("answerable_cases", count);
        report.put("limit", limit);
        report.put("hit_rate_at_k", count == 0 ? null : (double) found / count);
        report.put("mean_reciprocal_rank_at_k", count == 0 ? null : reciprocal / count);
        if (limit == 3) {
            report.put("hit_rate_at_3", report.get("hit_rate_at_k"));
            report.put("mrr_at_3", report.get("mean_reciprocal_rank_at_k"));
        }
        report.put("cases", results);
        report.put(
                "limitation",
                "Retrieval only; no generated-answer evaluation. Unsupported cases are diagnostics, not scored abstentions. Similarity is not proof of evidence.");
        return report;
    }
}
