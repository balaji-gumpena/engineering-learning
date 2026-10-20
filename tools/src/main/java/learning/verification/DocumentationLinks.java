package learning.verification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DocumentationLinks {
    private static final Pattern FENCES = Pattern.compile("```.*?```", Pattern.DOTALL);
    private static final Pattern LINKS = Pattern.compile("\\[[^\\]]*\\]\\(([^)]+)\\)");

    private DocumentationLinks() {}

    static void check(Path root) throws IOException {
        List<String> failures = new ArrayList<>();
        int count = 0;
        for (Path page : RepositoryFiles.list(root)) {
            if (!page.toString().endsWith(".md") || isOriginal(page)) {
                continue;
            }
            Matcher matcher = LINKS.matcher(FENCES.matcher(Files.readString(page)).replaceAll(""));
            while (matcher.find()) {
                String link = matcher.group(1);
                if (link.contains("://") || link.startsWith("#") || link.startsWith("mailto:")) {
                    continue;
                }
                String target = link.split("#", 2)[0];
                count++;
                if (!Files.exists(page.getParent().resolve(target).normalize())) {
                    failures.add(root.relativize(page) + ": " + link);
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new IOException(String.join("\n", failures));
        }
        System.out.println(
                count + " authored local links resolve; original notes use source maps.");
    }

    private static boolean isOriginal(Path page) {
        return page.toString().contains("/source-notes/")
                && !page.getFileName().toString().equals("README.md");
    }
}
