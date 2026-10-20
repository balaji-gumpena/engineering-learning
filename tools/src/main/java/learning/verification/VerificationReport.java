package learning.verification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

final class VerificationReport {
    private VerificationReport() {}

    static void write(
            Path root,
            List<CheckResult> results,
            boolean full,
            boolean postgres,
            boolean embeddings)
            throws IOException {
        StringBuilder text = new StringBuilder("# Current Java verification\n\nObserved at ");
        text.append(Instant.now()).append(". Entry point: `./verify.sh`.\n\n");
        text.append(
                "| Check | Result | JUnit tests/failures/errors/skipped | UI checks | Command and directory |\n");
        text.append("|---|---|---|---|---|\n");
        int javaTests = 0;
        int uiTests = 0;
        for (CheckResult result : results) {
            TestCounts counts = result.junit();
            String count =
                    counts == null
                            ? "—"
                            : counts.tests()
                                    + "/"
                                    + counts.failures()
                                    + "/"
                                    + counts.errors()
                                    + "/"
                                    + counts.skipped();
            javaTests += counts == null ? 0 : counts.tests();
            uiTests += result.uiTests();
            text.append("| ")
                    .append(result.name())
                    .append(" | ")
                    .append(result.passed() ? "PASS" : "FAIL")
                    .append(" | ")
                    .append(count)
                    .append(" | ")
                    .append(result.uiTests())
                    .append(" | `")
                    .append(String.join(" ", result.command()))
                    .append("` in `")
                    .append(result.directory().isEmpty() ? "." : result.directory())
                    .append("` |\n");
        }
        text.append("\nExecuted Java tests: ")
                .append(javaTests)
                .append(". UI fixture checks: ")
                .append(uiTests)
                .append(".\n\n");
        text.append(
                "Planned modules 02, 03, 05, 06, 15 and 16 are empty, unchecked and excluded, never counted as passed.\n\n");
        text.append(
                "Node checks use the actual browser client with HTTP/DOM fixtures. JUnit includes actual Spring loopback HTTP and synthetic provider/JWKS/H2 tests. Fixture evidence is not live model behavior or manual visual browser evidence.\n\n");
        text.append("Optional PostgreSQL checks requested: ")
                .append(postgres)
                .append("; real local embedding evaluation requested: ")
                .append(embeddings)
                .append(
                        ". Without those options, they remain unverified. No paid providers or external identity journeys are called.\n");
        Path logs = root.resolve(".verification");
        Files.createDirectories(logs);
        Files.writeString(logs.resolve("latest.md"), text);
        if (full) {
            Files.writeString(root.resolve("ai-engineering/JAVA_VERIFICATION.md"), text);
        }
    }
}
