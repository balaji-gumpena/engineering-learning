package com.example.aiassistant.rag;

import java.util.Set;

public final class RetrievalQueryValidator {
    private RetrievalQueryValidator() {}

    public static void validate(String question, String sourceType, int limit, double minScore) {
        if (question == null
                || question.isBlank()
                || limit < 1
                || limit > 20
                || !Double.isFinite(minScore)
                || minScore < -1
                || minScore > 1
                || sourceType != null
                        && !Set.of("policy", "faq", "product_guide").contains(sourceType)) {
            throw new IllegalArgumentException("Invalid knowledge query");
        }
    }
}
