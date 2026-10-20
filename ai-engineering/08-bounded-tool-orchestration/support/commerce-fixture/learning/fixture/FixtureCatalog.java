package learning.fixture;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class FixtureCatalog {
    private static final List<DemoProduct> PRODUCTS =
            List.of(
                    new DemoProduct(1, new BigDecimal("49")),
                    new DemoProduct(2, new BigDecimal("500")));

    String search(String query) {
        BigDecimal maximum = maximumPrice(query);
        List<DemoProduct> matches =
                PRODUCTS.stream()
                        .filter(
                                product ->
                                        maximum == null || product.price().compareTo(maximum) < 0)
                        .toList();
        return "{\"content\":["
                + matches.stream().map(DemoProduct::json).collect(Collectors.joining(","))
                + "],\"totalElements\":"
                + matches.size()
                + "}";
    }

    Optional<String> product(String path) {
        return PRODUCTS.stream()
                .filter(product -> path.equals("/api/products/" + product.id()))
                .map(DemoProduct::json)
                .findFirst();
    }

    private BigDecimal maximumPrice(String query) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts[0].equals("maxPriceExclusive")) {
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Missing price");
                }
                BigDecimal maximum =
                        new BigDecimal(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                if (maximum.signum() <= 0) {
                    throw new IllegalArgumentException("Invalid price");
                }
                return maximum;
            }
        }
        return null;
    }
}
