package learning.fixture;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/** Synthetic product HTTP fixture only; no database, authentication or model decisions. */
public final class CommerceFixture {
    private CommerceFixture() {}

    static HttpServer create(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", new FixtureHandler());
        return server;
    }

    public static void main(String[] arguments) throws IOException {
        int port = arguments.length == 0 ? 8080 : Integer.parseInt(arguments[0]);
        HttpServer server = create(port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        System.out.println(
                "Synthetic commerce fixture on http://127.0.0.1:"
                        + server.getAddress().getPort()
                        + "; no authentication or real business data.");
    }
}
