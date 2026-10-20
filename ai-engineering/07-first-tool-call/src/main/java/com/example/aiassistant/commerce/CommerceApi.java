package com.example.aiassistant.commerce;

import com.example.aiassistant.AiProperties;
import com.example.aiassistant.chat.AiException;
import com.example.aiassistant.chat.ChatDtos.ProductSummary;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads commerce data through HTTP and projects only validated fields for the model. Commerce
 * business rules and customer authorization remain in the commerce service.
 */
@Component
public class CommerceApi {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(3);
    private static final int MODEL_RESULT_LIMIT = 10;
    private static final int CATALOG_PAGE_SIZE = 50;
    private static final int MAX_CATALOG_PAGES = 100;

    private static final Set<String> OPTIONAL_TEXT_FIELDS = Set.of("imagePath", "categoryName");
    private static final Set<String> AMOUNT_FIELDS =
            Set.of("total", "subtotal", "price", "unitPrice", "lineTotal", "priceAtPurchase");
    private static final Set<String> POSITIVE_INTEGER_FIELDS =
            Set.of("quantity", "id", "productId");
    private static final Set<String> TEXT_FIELDS =
            Set.of(
                    "name",
                    "description",
                    "brand",
                    "currency",
                    "productName",
                    "status",
                    "paymentStatus");

    private final RestClient client;
    private final JsonMapper json;

    @Autowired
    public CommerceApi(AiProperties config, JsonMapper json) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(HTTP_TIMEOUT);

        this.client =
                RestClient.builder()
                        .baseUrl(config.commerceUrl())
                        .requestFactory(requestFactory)
                        .build();
        this.json = json;
    }

    public CommerceApi(RestClient client, JsonMapper json) {
        this.client = client;
        this.json = json;
    }

    public Map<String, Object> product(long id, String token) {
        JsonNode response = read("/api/products/" + id, token);

        return projectFields(
                response,
                "id",
                "name",
                "description",
                "brand",
                "price",
                "currency",
                "imagePath",
                "categoryName",
                "stockQuantity",
                "rating");
    }

    public Map<String, Object> cart(String token) {
        JsonNode response = read("/api/cart", token);

        if (response.has("error")) {
            return projectFields(response, "error");
        }

        Map<String, Object> cart = projectFields(response, "total");
        List<Map<String, Object>> items =
                projectItems(
                        response.path("items"),
                        "productId",
                        "productName",
                        "quantity",
                        "unitPrice",
                        "lineTotal");
        cart.put("items", items);

        return cart;
    }

    public Map<String, Object> order(long id, String token) {
        JsonNode response = read("/api/orders/" + id, token);
        return projectOrder(response);
    }

    public Map<String, Object> orders(String token) {
        JsonNode response = read("/api/orders", token);

        if (response.has("error")) {
            return projectFields(response, "error");
        }

        if (!response.isArray()) {
            throw new AiException(503, "Invalid commerce response.");
        }

        // Validate all orders before limiting the model's view of the history.
        List<Map<String, Object>> orders = new ArrayList<>();
        for (JsonNode order : response) {
            orders.add(projectOrder(order));
        }

        return Map.of(
                "orders", orders.stream().limit(MODEL_RESULT_LIMIT).toList(),
                "total_orders", response.size(),
                "has_more", response.size() > MODEL_RESULT_LIMIT);
    }

    public ProductSearchResult search(BigDecimal maxPrice, String token) {
        String path =
                "/api/products?maxPriceExclusive="
                        + maxPrice.toPlainString()
                        + "&page=0&size="
                        + MODEL_RESULT_LIMIT;
        JsonNode response = read(path, token);
        validatePage(response);

        List<ProductSummary> products = new ArrayList<>();
        for (JsonNode productNode : response.path("content")) {
            ProductSummary product = createProductCard(productNode);

            if (!product.currency().equals("EUR") || product.priceEur().compareTo(maxPrice) >= 0) {
                throw new AiException(503, "Invalid commerce product response.");
            }

            products.add(product);
        }

        int totalMatches = response.path("totalElements").asInt();
        if (products.size() > MODEL_RESULT_LIMIT || totalMatches < products.size()) {
            throw new AiException(503, "Invalid commerce pagination.");
        }

        boolean hasMore = totalMatches > products.size();
        return new ProductSearchResult(products, totalMatches, hasMore);
    }

    public List<CatalogProduct> publicCatalog() {
        Map<Long, CatalogProduct> productsById = new LinkedHashMap<>();

        for (int page = 0; page < MAX_CATALOG_PAGES; page++) {
            String path = "/api/products?page=" + page + "&size=" + CATALOG_PAGE_SIZE;
            JsonNode response = read(path, null);
            validatePage(response);

            for (JsonNode productNode : response.path("content")) {
                CatalogProduct product = readCatalogProduct(productNode);

                if (productNode.path("active").asBoolean(true)) {
                    productsById.put(product.card().id(), product);
                }
            }

            int totalProducts = response.path("totalElements").asInt();
            if ((page + 1) * CATALOG_PAGE_SIZE >= totalProducts) {
                return List.copyOf(productsById.values());
            }

            if (response.path("content").isEmpty()) {
                break;
            }
        }

        throw new AiException(503, "Catalog exceeds the supported demo size.");
    }

    private JsonNode read(String path, String bearer) {
        try {
            RestClient.RequestHeadersSpec<?> request = client.get().uri(path);

            if (bearer != null) {
                request.headers(headers -> headers.setBearerAuth(bearer));
            }

            String responseBody = request.retrieve().body(String.class);
            return json.readTree(responseBody);
        } catch (RestClientResponseException error) {
            int status = error.getStatusCode().value();

            if (status == 404) {
                return json.valueToTree(Map.of("error", "not_found"));
            }

            if (status == 401 || status == 403) {
                return json.valueToTree(Map.of("error", "access_denied"));
            }

            throw new AiException(503, "Commerce information is temporarily unavailable.");
        } catch (Exception error) {
            // Upstream exception messages can contain private request details.
            throw new AiException(503, "Commerce information is temporarily unavailable.");
        }
    }

    private Map<String, Object> projectOrder(JsonNode response) {
        if (response.has("error")) {
            return projectFields(response, "error");
        }

        Map<String, Object> order =
                projectFields(response, "id", "status", "subtotal", "paymentStatus", "createdAt");
        List<Map<String, Object>> items =
                projectItems(
                        response.path("items"),
                        "productId",
                        "productName",
                        "quantity",
                        "priceAtPurchase",
                        "lineTotal");
        order.put("items", items);

        return order;
    }

    private CatalogProduct readCatalogProduct(JsonNode productNode) {
        ProductSummary card = createProductCard(productNode);
        String slug = readText(productNode.path("slug"), false);

        if (!slug.matches("[a-z0-9][a-z0-9-]*")) {
            throw new AiException(503, "Invalid product slug.");
        }

        String description = readText(productNode.path("description"), true);
        String brand = readText(productNode.path("brand"), true);
        readText(productNode.path("categoryName"), true);

        JsonNode stock = productNode.path("stockQuantity");
        validateStock(stock, "Invalid catalog stock.");

        if (productNode.has("active") && !productNode.path("active").isBoolean()) {
            throw new AiException(503, "Invalid catalog activity.");
        }

        return new CatalogProduct(
                slug,
                description,
                brand,
                card,
                stock.asInt(),
                productNode.path("rating").asText(null));
    }

    private ProductSummary createProductCard(JsonNode productNode) {
        try {
            JsonNode id = productNode.path("id");
            JsonNode name = productNode.path("name");

            if (!id.isIntegralNumber()
                    || !id.canConvertToLong()
                    || id.asLong() <= 0
                    || !name.isString()
                    || name.asText().isBlank()) {
                throw new IllegalArgumentException("Invalid product identity");
            }

            validateAmount(productNode.path("price"));

            return new ProductSummary(
                    id.asLong(),
                    name.asText(),
                    new BigDecimal(productNode.required("price").asText()),
                    readText(productNode.path("currency"), false),
                    readOptionalText(productNode.path("imagePath")),
                    readOptionalText(productNode.path("categoryName")));
        } catch (Exception error) {
            throw new AiException(503, "Invalid catalog product.");
        }
    }

    private Map<String, Object> projectFields(JsonNode response, String... fieldNames) {
        Map<String, Object> fields = new LinkedHashMap<>();

        if (response.has("error")) {
            fields.put("error", response.path("error").asText());
            return fields;
        }

        if (!response.isObject()) {
            throw new AiException(503, "Invalid commerce response.");
        }

        for (String fieldName : fieldNames) {
            if (!response.has(fieldName) && OPTIONAL_TEXT_FIELDS.contains(fieldName)) {
                fields.put(fieldName, null);
                continue;
            }

            if (!response.has(fieldName)) {
                throw new AiException(503, "Incomplete commerce response.");
            }

            JsonNode value = response.get(fieldName);
            validateField(fieldName, value);
            fields.put(fieldName, json.convertValue(value, Object.class));
        }

        return fields;
    }

    private void validateField(String fieldName, JsonNode value) {
        if (AMOUNT_FIELDS.contains(fieldName) || fieldName.equals("rating")) {
            validateAmount(value);
        } else if (POSITIVE_INTEGER_FIELDS.contains(fieldName)) {
            if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() <= 0) {
                throw new AiException(503, "Invalid commerce numeric field.");
            }
        } else if (fieldName.equals("stockQuantity")) {
            validateStock(value, "Invalid commerce stock.");
        } else if (TEXT_FIELDS.contains(fieldName)) {
            readText(value, true);
        } else if (OPTIONAL_TEXT_FIELDS.contains(fieldName)) {
            readOptionalText(value);
        } else if (fieldName.equals("createdAt")) {
            validateTimestamp(value);
        }
    }

    private void validatePage(JsonNode response) {
        JsonNode content = response.path("content");
        JsonNode totalElements = response.path("totalElements");

        if (!content.isArray()
                || !totalElements.isIntegralNumber()
                || !totalElements.canConvertToInt()
                || totalElements.asInt() < content.size()) {
            throw new AiException(503, "Invalid commerce pagination.");
        }
    }

    private void validateStock(JsonNode stock, String message) {
        if (!stock.isIntegralNumber() || !stock.canConvertToInt() || stock.asInt() < 0) {
            throw new AiException(503, message);
        }
    }

    private void validateTimestamp(JsonNode value) {
        try {
            Instant.parse(value.asText());
        } catch (RuntimeException error) {
            throw new AiException(503, "Invalid commerce timestamp.");
        }
    }

    private String readText(JsonNode value, boolean allowBlank) {
        if (!value.isString() || (!allowBlank && value.asText().isBlank())) {
            throw new AiException(503, "Invalid commerce text field.");
        }

        return value.asText();
    }

    private String readOptionalText(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        return readText(value, true);
    }

    private void validateAmount(JsonNode value) {
        try {
            if ((!value.isString() && !value.isNumber())
                    || new BigDecimal(value.asText()).signum() < 0) {
                throw new IllegalArgumentException("Invalid amount");
            }
        } catch (RuntimeException error) {
            throw new AiException(503, "Invalid commerce amount.");
        }
    }

    private List<Map<String, Object>> projectItems(JsonNode items, String... fieldNames) {
        if (!items.isArray()) {
            throw new AiException(503, "Invalid commerce response.");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonNode item : items) {
            result.add(projectFields(item, fieldNames));
        }

        return result;
    }

    public record ProductSearchResult(
            List<ProductSummary> products,
            @JsonProperty("total_matches") int totalMatches,
            @JsonProperty("has_more") boolean hasMore) {}

    public record CatalogProduct(
            String slug,
            String description,
            String brand,
            ProductSummary card,
            int stockQuantity,
            String rating) {}
}
