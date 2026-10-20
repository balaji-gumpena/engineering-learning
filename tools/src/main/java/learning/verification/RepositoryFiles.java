package learning.verification;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class RepositoryFiles {
    private static final Set<String> EXCLUDED =
            Set.of(
                    ".runtime",
                    ".git",
                    ".gradle",
                    ".cache",
                    "node_modules",
                    "build",
                    "dist",
                    ".verification");

    private RepositoryFiles() {}

    static List<Path> list(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(
                root,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory, BasicFileAttributes attributes) {
                        return EXCLUDED.contains(directory.getFileName().toString())
                                ? FileVisitResult.SKIP_SUBTREE
                                : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                        if (attributes.isRegularFile()) {
                            files.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        return files;
    }
}
