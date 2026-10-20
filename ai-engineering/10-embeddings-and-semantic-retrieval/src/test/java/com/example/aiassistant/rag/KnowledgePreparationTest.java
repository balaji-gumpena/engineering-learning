package com.example.aiassistant.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aiassistant.AiProperties.RagSettings;
import com.example.aiassistant.TestProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class KnowledgePreparationTest {
    @TempDir Path directory;
    String header = "---\nsource_id: sample\ntitle: Sample\nsource_type: faq\ndemo: true\n---\n";

    @Test
    void demoCorpusContainsPoliciesAndProducts() throws Exception {
        List<KnowledgeDocument> documents = new MarkdownDocumentLoader().load(Path.of("knowledge"));
        assertEquals(7, documents.size());
        assertTrue(documents.stream().anyMatch(doc -> doc.sourceType().equals("product_guide")));
    }

    @Test
    void sourceMetadataAndDuplicateIdsAreValidated() throws Exception {
        Files.writeString(directory.resolve("first.md"), header + "Useful text");
        Files.writeString(directory.resolve("second.md"), header + "More text");
        assertThrows(
                IllegalArgumentException.class, () -> new MarkdownDocumentLoader().load(directory));
    }

    @Test
    void headingsAndOverlapPreserveEveryWordWithinTokenBudget() {
        String words =
                IntStream.range(0, 45).mapToObj(i -> "word" + i).collect(Collectors.joining(" "));
        KnowledgeDocument document =
                new KnowledgeDocument(
                        "sample",
                        "Sample",
                        "faq",
                        null,
                        "sample.md",
                        "hash",
                        "# Guide\n## Details\n" + words);
        List<Document> chunks =
                new KnowledgeChunker(text -> text.split("\\s+").length, 15, 3)
                        .split(document, "fingerprint");
        Set<String> found = new HashSet<>();
        for (Document chunk : chunks) {
            assertTrue(chunk.getText().split("\\s+").length <= 15);
            assertEquals("Guide / Details", chunk.getMetadata().get("section"));
            found.addAll(List.of(chunk.getMetadata().get("excerpt").toString().split(" ")));
        }
        assertTrue(found.containsAll(List.of(words.split(" "))));
    }

    @Test
    void oversizedWordsAreRejectedInsteadOfTruncated() {
        KnowledgeDocument document =
                new KnowledgeDocument(
                        "sample", "Sample", "faq", null, "sample.md", "hash", "x".repeat(100));
        assertThrows(
                IllegalArgumentException.class,
                () -> new KnowledgeChunker(String::length, 40, 3).split(document, "fp"));
    }

    @Test
    void blankMissingAndLinkedSourcesAreRejected() throws Exception {
        Files.writeString(directory.resolve("source.md"), "No front matter");
        assertThrows(
                IllegalArgumentException.class, () -> new MarkdownDocumentLoader().load(directory));
        Files.writeString(directory.resolve("source.md"), header + "Useful text");
        Files.createSymbolicLink(directory.resolve("link.md"), directory.resolve("source.md"));
        assertThrows(
                IllegalArgumentException.class, () -> new MarkdownDocumentLoader().load(directory));
    }

    @Test
    void fingerprintIncludesModelAndChunking() {
        assertNotEquals(
                TestProperties.create().rag().fingerprint(),
                new RagSettings(
                                true,
                                false,
                                "./knowledge",
                                "other-model",
                                "tokenizer-url",
                                ".cache/test",
                                384,
                                240,
                                20,
                                4,
                                .25,
                                6000)
                        .fingerprint());
    }

    @Test
    void fencedHeadingsRemainDataAndLargeOverlapStillMakesProgress() {
        KnowledgeDocument source =
                new KnowledgeDocument(
                        "sample",
                        "Sample",
                        "faq",
                        null,
                        "sample.md",
                        "hash",
                        "# Real heading\n```text\n# Not a heading\n```\n" + "word ".repeat(40));
        List<Document> chunks =
                new KnowledgeChunker(text -> text.split("\\s+").length, 12, 100)
                        .split(source, "fp");
        assertTrue(chunks.size() > 1);
        assertTrue(
                chunks.stream()
                        .allMatch(
                                chunk ->
                                        "Real heading".equals(chunk.getMetadata().get("section"))));
        assertTrue(chunks.getFirst().getText().contains("Not a heading"));
    }
}
