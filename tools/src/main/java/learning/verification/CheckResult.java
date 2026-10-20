package learning.verification;

import java.util.List;

record CheckResult(
        String name,
        String directory,
        List<String> command,
        int exitCode,
        TestCounts junit,
        int uiTests) {
    boolean passed() {
        return exitCode == 0;
    }
}
