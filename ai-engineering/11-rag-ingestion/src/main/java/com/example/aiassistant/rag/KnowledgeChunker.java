package com.example.aiassistant.rag;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KnowledgeChunker {
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*#*\\s*$");
    private final ToIntFunction<String> tokens;
    private final int maxTokens;
    private final int overlap;

    public KnowledgeChunker(ToIntFunction<String> tokens, int maxTokens, int overlap) {
        if (maxTokens < 1 || overlap < 0) {
            throw new IllegalArgumentException("Invalid chunk limits");
        }
        this.tokens = tokens;
        this.maxTokens = maxTokens;
        this.overlap = overlap;
    }

    public List<Document> split(KnowledgeDocument source, String fingerprint) {
        List<Document> result = new ArrayList<>();
        NavigableMap<Integer, String> headings = new TreeMap<>();
        String section = "Overview";
        StringBuilder text = new StringBuilder();
        boolean code = false;
        for (String line : source.body().split("\\R")) {
            if (line.stripLeading().startsWith("```") || line.stripLeading().startsWith("~~~")) {
                code = !code;
            }
            Matcher match = HEADING.matcher(line);
            if (!code && match.matches()) {
                append(source, section, text.toString(), fingerprint, result);
                int level = match.group(1).length();
                headings.tailMap(level).clear();
                headings.put(level, match.group(2));
                section = String.join(" / ", headings.values());
                text.setLength(0);
            } else {
                text.append(line).append('\n');
            }
        }
        append(source, section, text.toString(), fingerprint, result);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Knowledge source has no passages");
        }
        return result;
    }

    private void append(
            KnowledgeDocument source,
            String section,
            String text,
            String fingerprint,
            List<Document> target) {
        if (text.isBlank()) {
            return;
        }
        String[] words = text.strip().split("\\s+");
        String prefix = source.title() + "\n" + section + "\n";
        int start = 0;
        while (start < words.length) {
            int end = start;
            while (end < words.length
                    && tokens.applyAsInt(
                                    prefix
                                            + String.join(
                                                    " ", Arrays.copyOfRange(words, start, end + 1)))
                            <= maxTokens) {
                end++;
            }
            if (end == start) {
                throw new IllegalArgumentException(
                        "A heading or word exceeds the embedding token limit");
            }
            String excerpt = String.join(" ", Arrays.copyOfRange(words, start, end));
            String id = source.sourceId() + ":" + target.size();
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("citation_id", id);
            metadata.put("source_id", source.sourceId());
            metadata.put("title", source.title());
            metadata.put("section", section);
            metadata.put("excerpt", excerpt);
            metadata.put("source_path", source.sourcePath());
            metadata.put("source_type", source.sourceType());
            metadata.put("content_hash", source.contentHash());
            metadata.put("fingerprint", fingerprint);
            metadata.put("demo", true);
            if (source.productSlug() != null) {
                metadata.put("product_slug", source.productSlug());
            }
            target.add(Document.builder().id(id).text(prefix + excerpt).metadata(metadata).build());
            if (end == words.length) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
        }
    }
}
