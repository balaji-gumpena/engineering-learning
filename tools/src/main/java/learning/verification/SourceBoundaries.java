package learning.verification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class SourceBoundaries {
    private static final Pattern SOURCE_PATH =
            Pattern.compile(
                    "/Users/[^\\s]+/ai-commerce|\\.\\./(?:\\.\\./)*ai-commerce|includeBuild\\(");
    private static final Pattern CROSS_MODULE =
            Pattern.compile("ai-engineering/\\d\\d-[^\\s]+/src");
    private static final Pattern INFERRED_LOCAL =
            Pattern.compile("\\bvar\\s+[A-Za-z_$][A-Za-z0-9_$]*\\s*[=:]");
    private static final Pattern CODE =
            Pattern.compile(".*\\.(java|gradle|yml|properties|sh|ts|tsx|js|cjs|html|css)$");
    private static final Pattern LATER_IMPORT =
            Pattern.compile(
                    "org\\.springframework\\.ai\\.(tool|vectorstore|embedding)|com\\.example\\.aiassistant\\.(tools|rag)");
    private static final Pattern LATER_DEPENDENCY =
            Pattern.compile(
                    "(?:implementation|runtimeOnly) [^\\n]*(?:postgres|security|pgvector|transformers|anthropic)");

    private SourceBoundaries() {}

    static void check(Path root) throws IOException {
        List<String> failures = new ArrayList<>();
        int count = 0;
        for (Path file : RepositoryFiles.list(root)) {
            String name = file.toString();
            if (name.endsWith(".py")) {
                failures.add("Python source is not allowed: " + root.relativize(file));
            }
            if (name.contains("/source-notes/") || !CODE.matcher(name).matches()) {
                continue;
            }
            String text = Files.readString(file);
            count++;
            boolean moduleCode = name.contains("/ai-engineering/");
            if (moduleCode
                    && (SOURCE_PATH.matcher(text).find() || CROSS_MODULE.matcher(text).find())) {
                failures.add("Checkout/module dependency: " + root.relativize(file));
            }
            if (name.endsWith(".java") && INFERRED_LOCAL.matcher(text).find()) {
                failures.add("Use an explicit Java type: " + root.relativize(file));
            }
            boolean early =
                    name.contains("/01-basic-llm-call/")
                            || name.contains("/04-conversation-state/");
            if (early
                    && ((name.endsWith(".java") && LATER_IMPORT.matcher(text).find())
                            || (name.endsWith("build.gradle")
                                    && LATER_DEPENDENCY.matcher(text).find()))) {
                failures.add("Early lesson includes a later capability: " + root.relativize(file));
            }
        }
        for (CurriculumModule module : ModuleCatalog.modules()) {
            if (!module.implemented()) {
                continue;
            }
            Path makefile = module.resolve(root).resolve("Makefile");
            if (!Files.exists(makefile)) {
                failures.add("Missing module Makefile: " + module.directory());
                continue;
            }
            String commands = Files.readString(makefile);
            for (String target :
                    java.util.List.of(
                            "start:", "restart:", "stop:", "status:", "test:", "build:")) {
                if (!commands.contains(target)) {
                    failures.add(
                            "Missing consistent Make target "
                                    + target
                                    + " in "
                                    + module.directory());
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new IOException(String.join("\n", failures));
        }
        System.out.println(
                count
                        + " code/config files checked; no Python, Java var or module dependency leaks.");
    }
}
