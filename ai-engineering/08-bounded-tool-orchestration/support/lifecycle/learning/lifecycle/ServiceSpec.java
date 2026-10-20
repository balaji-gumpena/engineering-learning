package learning.lifecycle;

import java.nio.file.Path;
import java.util.List;

record ServiceSpec(
        String name,
        List<String> command,
        String identity,
        int port,
        String readinessPath,
        int readyStatus) {
    Path stateFile(Path root) {
        return root.resolve(".runtime/" + name + ".properties");
    }

    Path logFile(Path root) {
        return root.resolve(".runtime/" + name + ".log");
    }
}
