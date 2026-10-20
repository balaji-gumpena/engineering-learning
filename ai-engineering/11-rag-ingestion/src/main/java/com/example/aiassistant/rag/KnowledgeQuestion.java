package com.example.aiassistant.rag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record KnowledgeQuestion(
        @NotBlank @Pattern(regexp = "user") String role,
        @NotBlank @Size(max = 4000) String content) {}
