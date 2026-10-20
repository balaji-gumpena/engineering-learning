package learning.verification;

import java.nio.file.Path;

record CurriculumModule(String directory, boolean implemented) {
    Path resolve(Path root) {
        return root.resolve("ai-engineering").resolve(directory);
    }
}
