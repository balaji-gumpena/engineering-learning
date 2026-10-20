package learning.fixture;

import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Exercises real loopback HTTP, including strict price exclusion and malformed queries. */
public final class FixtureHttpTest {
    private FixtureHttpTest() {}

    public static void main(String[] arguments) throws Exception {
        HttpServer server = CommerceFixture.create(0);
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            HttpResponse<String> search = get(base + "/api/products?maxPriceExclusive=500");
            require(
                    search.statusCode() == 200
                            && search.body().contains("Demo product 1")
                            && !search.body().contains("Demo product 2"),
                    "Exclusive price filter");
            require(
                    get(base + "/api/products/2").body().contains("Demo product 2"),
                    "Product detail");
            require(get(base + "/api/products/99").statusCode() == 404, "Missing product");
            require(
                    get(base + "/api/products?maxPriceExclusive=invalid").statusCode() == 400,
                    "Malformed price");
            System.out.println("4 Java commerce fixture HTTP checks passed.");
        } finally {
            server.stop(0);
        }
    }

    private static HttpResponse<String> get(String url) throws Exception {
        return HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create(url))
                                .timeout(Duration.ofSeconds(3))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
