package com.example.aiassistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProviderChatRequest(
        @NotEmpty @Size(max = 1) List<@NotNull @Valid ProviderQuestion> messages,
        String provider) {}
