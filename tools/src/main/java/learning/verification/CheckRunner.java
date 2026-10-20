package learning.verification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CheckRunner {
    @FunctionalInterface
    interface Check {
        void execute() throws Exception;
    }

    private final Path root;
    private final List<CheckResult> results = new ArrayList<>();

    CheckRunner(Path root) {
        this.root = root;
    }

    List<CheckResult> results() {
        return List.copyOf(results);
    }

    void check(String name, Check check) {
        check(name, List.of("./verify.sh", "--static-only"), check);
    }

    void check(String name, List<String> command, Check check) {
        int exit = 0;
        try {
            check.execute();
        } catch (Exception error) {
            System.err.println(name + ": " + error.getMessage());
            exit = 1;
        }
        results.add(new CheckResult(name, ".", command, exit, null, 0));
        status(name, exit);
    }

    boolean run(String name, Path directory, List<String> command, String testTask)
            throws Exception {
        Path logs = root.resolve(".verification");
        Files.createDirectories(logs);
        Path log = logs.resolve(name.replace('/', '-') + ".log");
        System.out.println("[" + name + "] " + String.join(" ", command));
        int exit;
        try {
            Process process =
                    new ProcessBuilder(command)
                            .directory(directory.toFile())
                            .redirectErrorStream(true)
                            .redirectOutput(log.toFile())
                            .start();
            exit = process.waitFor();
        } catch (IOException error) {
            Files.writeString(log, error.toString());
            exit = 127;
        }
        String output = Files.readString(log);
        TestCounts counts =
                exit == 0 && testTask != null
                        ? TestCounts.read(directory.resolve("build/test-results/" + testTask))
                        : null;
        Matcher matcher = Pattern.compile("(?:ℹ|#) tests (\\d+)").matcher(output);
        int uiTests = matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        results.add(
                new CheckResult(
                        name,
                        root.relativize(directory).toString(),
                        command,
                        exit,
                        counts,
                        uiTests));
        status(name, exit);
        if (exit != 0) {
            List<String> lines = output.lines().toList();
            lines.subList(Math.max(0, lines.size() - 24), lines.size())
                    .forEach(System.err::println);
        }
        return exit == 0;
    }

    private void status(String name, int exit) {
        System.out.println("[" + name + "] " + (exit == 0 ? "PASS" : "FAIL (exit " + exit + ")"));
    }
}
