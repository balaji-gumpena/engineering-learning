package com.example.aiassistant.rag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record KnowledgeChatRequest(
        @NotEmpty @Size(max = 1) List<@NotNull @Valid KnowledgeQuestion> messages) {}
