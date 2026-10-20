package learning.lifecycle;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class ManagedService {
    private final Path root;
    private final ServiceSpec spec;

    ManagedService(Path root, ServiceSpec spec) {
        this.root = root;
        this.spec = spec;
    }

    boolean running() throws Exception {
        Optional<ProcessState> state = ProcessState.read(spec.stateFile(root));
        if (state.isEmpty()) {
            return false;
        }
        Optional<ProcessHandle> process = state.get().liveHandle();
        if (process.isEmpty()) {
            Files.deleteIfExists(spec.stateFile(root));
            return false;
        }
        if (!state.get().matches(process.get(), spec, root)) {
            throw new IllegalStateException("Refusing an unowned/reused PID for " + spec.name());
        }
        return true;
    }

    boolean start() throws Exception {
        if (running()) {
            System.out.println(spec.name() + " is already running");
            return false;
        }
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), spec.port()));
        }
        Files.createDirectories(spec.logFile(root).getParent());
        Process process =
                new ProcessBuilder(spec.command())
                        .directory(root.toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(spec.logFile(root).toFile())
                        .start();
        ProcessState state =
                new ProcessState(
                        process.pid(),
                        process.info().startInstant().orElseThrow(),
                        spec.identity(),
                        root.toString());
        state.write(spec.stateFile(root));
        try {
            awaitReady(process);
        } catch (Exception error) {
            stop();
            throw error;
        }
        System.out.println(
                spec.name() + " started on port " + spec.port() + " (PID " + process.pid() + ")");
        return true;
    }

    void stop() throws Exception {
        if (!running()) {
            System.out.println("No Make-managed " + spec.name() + " process.");
            return;
        }
        ProcessState state = ProcessState.read(spec.stateFile(root)).orElseThrow();
        ProcessHandle process = state.liveHandle().orElseThrow();
        if (!state.matches(process, spec, root)) {
            throw new IllegalStateException("Process ownership changed");
        }
        process.destroy();
        try {
            process.onExit().get(12, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            if (!state.matches(process, spec, root)) {
                throw new IllegalStateException("Process ownership changed");
            }
            process.destroyForcibly();
            process.onExit().get(5, TimeUnit.SECONDS);
        }
        Files.deleteIfExists(spec.stateFile(root));
        System.out.println(spec.name() + " stopped");
    }

    private void awaitReady(Process process) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        URI uri = URI.create("http://127.0.0.1:" + spec.port() + spec.readinessPath());
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        while (System.nanoTime() < deadline && process.isAlive()) {
            try {
                HttpResponse<Void> response =
                        client.send(
                                HttpRequest.newBuilder(uri)
                                        .timeout(Duration.ofSeconds(1))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == spec.readyStatus()) {
                    return;
                }
            } catch (java.io.IOException unavailable) {
                /* Wait for this owned process to bind. */
            }
            Thread.sleep(150);
        }
        throw new IllegalStateException(
                "Service did not become ready: " + spec.name() + "; inspect " + spec.logFile(root));
    }
}
