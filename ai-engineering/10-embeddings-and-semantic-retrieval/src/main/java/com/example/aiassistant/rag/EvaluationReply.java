package com.example.aiassistant.rag;

import java.util.Map;

public record EvaluationReply(String reply, Map<String, Object> evaluation) {}
