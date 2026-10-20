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
                            "max_price_eur_exclusive"),
                    definition(
                            "get_product",
                            "Read current details of a known numeric product ID. Use several calls to compare products.",
                            "\"product_id\":{\"type\":\"integer\",\"minimum\":1}",
                            "product_id"),
                    definition("get_cart", "Read the signed-in customer's cart and total.", "", ""),
                    definition(
                            "get_order",
                            "Read a customer-owned order using its known numeric ID.",
                            "\"order_id\":{\"type\":\"integer\",\"minimum\":1}",
                            "order_id"),
                    definition(
                            "get_orders",
                            "Read up to ten recent orders for the signed-in customer.",
                            "",
                            ""),
                    definition(
                            "search_knowledge",
                            "Retrieve shipping/returns policies, FAQs, product guides, comparisons and semantic discovery advice. Live commerce values require Java APIs.",
                            "\"question\":{\"type\":\"string\",\"minLength\":1,\"maxLength\":1200},\"product_slug\":{\"type\":\"string\",\"pattern\":\"^[a-z0-9][a-z0-9-]{0,99}$\"}",
                            "question"));

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
