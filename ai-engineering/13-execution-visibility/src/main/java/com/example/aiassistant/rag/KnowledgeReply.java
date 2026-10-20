package com.example.aiassistant.rag;

import java.util.List;

public record KnowledgeReply(String reply, List<SourcePassage> sources) {}
