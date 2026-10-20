package com.example.aiassistant;

import com.example.aiassistant.chat.ChatDtos.TokenUsage;

public record ProviderChatReply(String reply, String model, TokenUsage usage) {}
