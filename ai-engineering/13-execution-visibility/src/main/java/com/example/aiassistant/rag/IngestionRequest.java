package com.example.aiassistant.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IngestionRequest(@NotBlank @Pattern(regexp = "chunk|ingest") String action) {}
