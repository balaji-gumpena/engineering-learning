package learning.lifecycle;

import java.nio.file.Files;
import java.nio.file.Path;

final class ModuleBuild {
    private ModuleBuild() {}

    static void build(Path root) throws Exception {
        buildProject(root);
        if (Files.isDirectory(root.resolve("commerce"))) {
            buildProject(root.resolve("commerce"));
        }
    }

    private static void buildProject(Path root) throws Exception {
        Process process =
                new ProcessBuilder("./gradlew", "--no-daemon", "--console=plain", "bootJar")
                        .directory(root.toFile())
                        .inheritIO()
                        .start();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("Build failed: " + root.getFileName());
        }
    }
}
