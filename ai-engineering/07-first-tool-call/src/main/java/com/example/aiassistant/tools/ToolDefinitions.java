package com.example.aiassistant.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

public final class ToolDefinitions {
    private ToolDefinitions() {}

    public static final List<ToolCallback> ALL =
            List.of(
                    definition(
                            "search_products",
                            "Find current products strictly below a price in EUR.",
                            "\"max_price_eur_exclusive\":{\"type\":\"number\",\"exclusiveMinimum\":0,\"maximum\":100000000}",
                            "max_price_eur_exclusive"));

    private static ToolCallback definition(
            String name, String description, String properties, String required) {
        ToolDefinition definition =
                ToolDefinition.builder()
                        .name(name)
                        .description(description)
                        .inputSchema(
                                "{\"type\":\"object\",\"properties\":{"
                                        + properties
                                        + "},\"required\":["
                                        + (required.isEmpty() ? "" : "\"" + required + "\"")
                                        + "],\"additionalProperties\":false}")
                        .build();
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public String call(String input) {
                throw new IllegalStateException(
                        "Automatic tool execution is disabled; validate the whole batch first.");
            }
        };
    }
}
