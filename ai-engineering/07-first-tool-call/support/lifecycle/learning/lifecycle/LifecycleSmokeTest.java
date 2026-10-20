package learning.lifecycle;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Starts real packaged applications without submitting model requests. */
public final class LifecycleSmokeTest {
    private LifecycleSmokeTest() {}

    public static void main(String[] arguments) throws Exception {
        Path root = Path.of("").toRealPath();
        if (Files.isDirectory(root.resolve(".runtime"))) {
            try (java.util.stream.Stream<Path> files = Files.list(root.resolve(".runtime"))) {
                if (files.anyMatch(file -> file.toString().endsWith(".properties"))) {
                    throw new IllegalStateException(
                            "Existing managed processes: stop them before lifecycle-test");
                }
            }
        }
        int port = freePort();
        int commercePort = freePort();
        while (port == commercePort) {
            commercePort = freePort();
        }
        List<ServiceSpec> specs = ModuleServices.list(root, port, commercePort);
        try {
            ModuleLifecycle.stop(root, port, commercePort);
            ModuleLifecycle.start(root, port, commercePort, false);
            long firstPid = ProcessState.read(specs.getLast().stateFile(root)).orElseThrow().pid();
            ModuleLifecycle.start(root, port, commercePort, false);
            require(
                    firstPid
                            == ProcessState.read(specs.getLast().stateFile(root))
                                    .orElseThrow()
                                    .pid(),
                    "Start must be idempotent");
            ModuleLifecycle.stop(root, port, commercePort);
            ModuleLifecycle.start(root, port, commercePort, false);
            require(
                    firstPid
                            != ProcessState.read(specs.getLast().stateFile(root))
                                    .orElseThrow()
                                    .pid(),
                    "Restart must launch a new process");
            ModuleLifecycle.stop(root, port, commercePort);
            ModuleLifecycle.stop(root, port, commercePort);
            verifyOwnershipGuard(root);
            System.out.println(
                    "Lifecycle checks passed: readiness, idempotent start/stop, restart and PID ownership.");
        } finally {
            ModuleLifecycle.stop(root, port, commercePort);
        }
    }

    private static void verifyOwnershipGuard(Path root) throws Exception {
        ServiceSpec spec =
                new ServiceSpec("ownership-test", List.of(), "not-this-process", 0, "/", 200);
        new ProcessState(
                        ProcessHandle.current().pid(),
                        Instant.EPOCH,
                        spec.identity(),
                        root.toString())
                .write(spec.stateFile(root));
        boolean refused = false;
        try {
            new ManagedService(root, spec).stop();
        } catch (IllegalStateException expected) {
            refused = true;
        } finally {
            Files.deleteIfExists(spec.stateFile(root));
        }
        require(refused && ProcessHandle.current().isAlive(), "Never stop an unowned PID");
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket =
                new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
