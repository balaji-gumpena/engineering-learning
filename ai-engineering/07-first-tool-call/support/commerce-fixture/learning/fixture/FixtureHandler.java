package learning.fixture;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

final class FixtureHandler implements HttpHandler {
    private final FixtureCatalog catalog = new FixtureCatalog();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                respond(exchange, 405, "{\"error\":\"method_not_allowed\"}");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/products")) {
                respond(exchange, 200, catalog.search(exchange.getRequestURI().getRawQuery()));
                return;
            }
            Optional<String> product = catalog.product(path);
            respond(
                    exchange,
                    product.isPresent() ? 200 : 404,
                    product.orElse("{\"error\":\"not_found\"}"));
        } catch (IllegalArgumentException error) {
            respond(exchange, 400, "{\"error\":\"invalid_query\"}");
        } finally {
            exchange.close();
        }
    }

    private void respond(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
