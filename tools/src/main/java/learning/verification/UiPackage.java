package learning.verification;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarFile;

final class UiPackage {
    private UiPackage() {}

    static void check(Path archive) throws IOException {
        try (JarFile jar = new JarFile(archive.toFile())) {
            String main = jar.getManifest().getMainAttributes().getValue("Main-Class");
            String start = jar.getManifest().getMainAttributes().getValue("Start-Class");
            if (!"org.springframework.boot.loader.launch.JarLauncher".equals(main)
                    || start == null) {
                throw new IOException("Missing standalone Boot entry point: " + archive);
            }
            for (String asset :
                    List.of(
                            "index.html",
                            "chat.css",
                            "chat.js",
                            "lesson-config.js",
                            "favicon.svg")) {
                if (jar.getEntry("BOOT-INF/classes/static/" + asset) == null) {
                    throw new IOException("Missing packaged UI asset: " + asset);
                }
            }
        }
        System.out.println(archive.getFileName() + " contains the complete independent UI.");
    }
}
