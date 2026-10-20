package learning.verification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

final class PlannedModules {
    private PlannedModules() {}

    static void check(Path root) throws IOException {
        String index = Files.readString(root.resolve("ai-engineering/README.md"));
        int count = 0;
        for (CurriculumModule module : ModuleCatalog.modules()) {
            if (module.implemented()) {
                continue;
            }
            Path folder = module.resolve(root);
            if (!Files.isDirectory(folder)) {
                throw new IOException("Missing planned folder: " + module.directory());
            }
            try (Stream<Path> contents = Files.list(folder)) {
                if (contents.findAny().isPresent()) {
                    throw new IOException(
                            "Planned folder must be completely empty: " + module.directory());
                }
            }
            if (!index.contains("- [ ] [" + module.directory() + "]")) {
                throw new IOException("Planned module must stay unchecked: " + module.directory());
            }
            count++;
        }
        System.out.println(count + " planned folders are completely empty and unchecked.");
    }
}
