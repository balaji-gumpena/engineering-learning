package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

class RetrievalEvaluationTest {
    @Test
    void preservesUnsupportedDiagnosticsRanksScoresAndTiming() {
        JsonNode cases =
                JsonMapper.builder()
                        .build()
                        .readTree(
                                """
                [{"question":"Known","source_id":"returns","section":"Window"},
                 {"question":"Unknown","source_id":null,"section":null}]
                """);
        Document wrong =
                Document.builder()
                        .id("wrong")
                        .text("Wrong section")
                        .metadata(Map.of("source_id", "returns", "section", "Postage"))
                        .score(.9)
                        .build();
        Document correct =
                Document.builder()
                        .id("right")
                        .text("30 days")
                        .metadata(Map.of("source_id", "returns", "section", "Return Window"))
                        .score(.8)
                        .build();
        Map<String, Object> report =
                RetrievalEvaluation.evaluate(cases, query -> List.of(wrong, correct), 3);
        assertEquals(1, report.get("answerable_cases"));
        assertEquals(1.0, report.get("hit_rate_at_k"));
        assertEquals(.5, report.get("mean_reciprocal_rank_at_k"));
        List<Map<String, Object>> results = (List<Map<String, Object>>) report.get("cases");
        assertEquals(2, results.size());
        assertEquals(2, results.getFirst().get("rank"));
        assertNull(results.getLast().get("rank"));
        assertNotNull(results.getFirst().get("latency_ms"));
        assertEquals(2, ((List<?>) results.getLast().get("retrieved")).size());
        assertNull(
                RetrievalEvaluation.evaluate(
                                JsonMapper.builder().build().readTree("[]"), query -> List.of(), 1)
                        .get("hit_rate_at_k"));
    }

    @Test
    void invalidSearchOptionsFailBeforeInference() {
        for (double score : new double[] {Double.NaN, -1.1, 1.1}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> RetrievalQueryValidator.validate("question", null, 3, score));
        }
        assertThrows(
                IllegalArgumentException.class,
                () -> RetrievalQueryValidator.validate(" ", null, 3, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> RetrievalQueryValidator.validate("q", "private_orders", 3, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> RetrievalQueryValidator.validate("q", null, 21, 0));
    }
}
