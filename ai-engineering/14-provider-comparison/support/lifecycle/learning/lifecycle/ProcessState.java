package learning.lifecycle;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

record ProcessState(long pid, Instant started, String identity, String moduleRoot) {
    static Optional<ProcessState> read(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            values.load(reader);
        }
        return Optional.of(
                new ProcessState(
                        Long.parseLong(values.getProperty("pid")),
                        Instant.parse(values.getProperty("started")),
                        values.getProperty("identity"),
                        values.getProperty("moduleRoot")));
    }

    void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Properties values = new Properties();
        values.setProperty("pid", Long.toString(pid));
        values.setProperty("started", started.toString());
        values.setProperty("identity", identity);
        values.setProperty("moduleRoot", moduleRoot);
        try (Writer writer = Files.newBufferedWriter(file)) {
            values.store(writer, "Module-owned process; no credentials");
        }
    }

    Optional<ProcessHandle> liveHandle() {
        return ProcessHandle.of(pid).filter(ProcessHandle::isAlive);
    }

    boolean matches(ProcessHandle handle, ServiceSpec spec, Path root) {
        ProcessHandle.Info info = handle.info();
        return identity.equals(spec.identity())
                && moduleRoot.equals(root.toString())
                && info.startInstant().map(started::equals).orElse(false)
                && info.arguments()
                        .map(arguments -> Arrays.asList(arguments).contains(identity))
                        .orElse(false);
    }
}
