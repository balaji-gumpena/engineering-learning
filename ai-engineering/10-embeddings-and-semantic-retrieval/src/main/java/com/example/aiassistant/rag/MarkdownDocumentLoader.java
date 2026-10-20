package com.example.aiassistant.rag;

import com.example.aiassistant.AiProperties;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class MarkdownDocumentLoader {
    public List<KnowledgeDocument> load(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Knowledge directory is missing");
        }
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        List<KnowledgeDocument> documents = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path :
                    paths.filter(path -> path.toString().endsWith(".md")).sorted().toList()) {
                if (Files.isSymbolicLink(path)
                        || !path.toRealPath().startsWith(root.toRealPath())) {
                    throw new IllegalArgumentException("Knowledge source links are not supported");
                }
                String text = Files.readString(path).replace("\r\n", "\n");
                int end = text.indexOf("\n---\n", 4);
                if (!text.startsWith("---\n") || end < 0) {
                    throw new IllegalArgumentException("Missing knowledge front matter");
                }
                Map<String, Object> metadata = yaml.load(text.substring(4, end));
                if (metadata == null
                        || !Set.of("source_id", "title", "source_type", "product_slug", "demo")
                                .containsAll(metadata.keySet())
                        || !Boolean.TRUE.equals(metadata.get("demo"))) {
                    throw new IllegalArgumentException("Expected demo source metadata");
                }
                String id = required(metadata, "source_id");
                String title = required(metadata, "title");
                String type = required(metadata, "source_type");
                String slug =
                        metadata.get("product_slug") == null
                                ? null
                                : required(metadata, "product_slug");
                if (!id.matches("[a-z0-9][a-z0-9-]{0,99}")
                        || title.length() > 160
                        || !Set.of("policy", "faq", "product_guide").contains(type)
                        || slug != null && !slug.matches("[a-z0-9][a-z0-9-]{0,99}")) {
                    throw new IllegalArgumentException("Invalid source metadata");
                }
                if (!ids.add(id)) {
                    throw new IllegalArgumentException("Duplicate knowledge source ID");
                }
                String body = text.substring(end + 5).strip();
                if (body.isEmpty()) {
                    throw new IllegalArgumentException("Empty knowledge source");
                }
                documents.add(
                        new KnowledgeDocument(
                                id,
                                title,
                                type,
                                slug,
                                root.relativize(path).toString().replace('\\', '/'),
                                AiProperties.hash(text),
                                body));
            }
        }
        return documents;
    }

    private String required(Map<String, Object> metadata, String key) {
        if (!(metadata.get(key) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("Missing source metadata");
        }
        return value.strip();
    }
}
