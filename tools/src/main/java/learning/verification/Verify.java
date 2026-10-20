package learning.verification;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Orchestrates checks; each responsibility lives in its own small class. */
public final class Verify {
    private Verify() {}

    public static void main(String[] arguments) throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        if (arguments.length == 2 && arguments[0].equals("--package")) {
            UiPackage.check(root.resolve(arguments[1]));
            return;
        }
        Set<String> selected = new HashSet<>();
        boolean staticOnly = false;
        boolean postgres = false;
        boolean embeddings = false;
        for (int index = 0; index < arguments.length; index++) {
            switch (arguments[index]) {
                case "--module" -> selected.add(arguments[++index]);
                case "--static-only" -> staticOnly = true;
                case "--postgres" -> postgres = true;
                case "--embeddings" -> embeddings = true;
                default ->
                        throw new IllegalArgumentException("Unknown option: " + arguments[index]);
            }
        }
        Set<String> known = new HashSet<>();
        ModuleCatalog.modules().forEach(module -> known.add(module.directory()));
        if (!known.containsAll(selected)) {
            throw new IllegalArgumentException("Unknown module selection");
        }
        CheckRunner runner = new CheckRunner(root);
        runner.check("documentation-links", () -> DocumentationLinks.check(root));
        runner.check("source-boundaries-and-java-style", () -> SourceBoundaries.check(root));
        runner.check("empty-planned-modules", () -> PlannedModules.check(root));
        runner.run("java-format", root, List.of("bash", "scripts/format-java.sh", "--check"), null);
        if (!staticOnly) {
            for (CurriculumModule module : ModuleCatalog.modules()) {
                if (!selected.isEmpty() && !selected.contains(module.directory())) {
                    continue;
                }
                if (!module.implemented()) {
                    System.out.println("[" + module.directory() + "] PLANNED — excluded");
                    continue;
                }
                verifyModule(root, module, runner, postgres, embeddings);
            }
        }
        VerificationReport.write(
                root, runner.results(), !staticOnly && selected.isEmpty(), postgres, embeddings);
        System.exit(runner.results().stream().allMatch(CheckResult::passed) ? 0 : 1);
    }

    private static void verifyModule(
            Path root,
            CurriculumModule module,
            CheckRunner runner,
            boolean postgres,
            boolean embeddings)
            throws Exception {
        Path directory = module.resolve(root);
        List<String> command =
                List.of("./gradlew", "--no-daemon", "--console=plain", "test", "bootJar");
        boolean built = runner.run(module.directory(), directory, command, "test");
        if (built) {
            runner.check(
                    module.directory() + "-ui-package",
                    () -> UiPackage.check(directory.resolve("build/libs/lesson.jar")));
        }
        if (Files.isDirectory(directory.resolve("commerce"))) {
            built =
                    runner.run(
                                    module.directory() + "/commerce",
                                    directory.resolve("commerce"),
                                    command,
                                    "test")
                            && built;
        }
        if (built) {
            runner.run(
                    module.directory() + "-lifecycle",
                    directory,
                    List.of("make", "lifecycle-test"),
                    null);
        }
        runner.run(
                module.directory() + "-ui",
                directory,
                List.of("node", "--test", "ui-tests/chat.test.cjs"),
                null);
        if (Files.exists(
                directory.resolve(
                        "support/commerce-fixture/learning/fixture/FixtureHttpTest.java"))) {
            runner.run(
                    module.directory() + "-commerce-fixture",
                    directory,
                    List.of(
                            "java",
                            "support/commerce-fixture/learning/fixture/FixtureHttpTest.java"),
                    null);
        }
        if (postgres && Set.of("11", "12", "13").contains(module.directory().substring(0, 2))) {
            runner.run(
                    module.directory() + "-postgres",
                    directory,
                    List.of("./gradlew", "--no-daemon", "--console=plain", "postgresTest"),
                    "postgresTest");
        }
        if (embeddings && module.directory().startsWith("10")) {
            runner.run(
                    module.directory() + "-real-embeddings",
                    directory,
                    List.of("./gradlew", "--no-daemon", "--console=plain", "embeddingTest"),
                    "embeddingTest");
        }
    }
}
