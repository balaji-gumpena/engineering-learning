package learning.verification;

import org.w3c.dom.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

record TestCounts(int tests, int failures, int errors, int skipped) {
    static TestCounts read(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            return new TestCounts(0, 0, 0, 0);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        int tests = 0;
        int failures = 0;
        int errors = 0;
        int skipped = 0;
        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> reports =
                    stream.filter(path -> path.getFileName().toString().startsWith("TEST-"))
                            .toList();
            for (Path report : reports) {
                Element suite =
                        factory.newDocumentBuilder().parse(report.toFile()).getDocumentElement();
                tests += Integer.parseInt(suite.getAttribute("tests"));
                failures += Integer.parseInt(suite.getAttribute("failures"));
                errors += Integer.parseInt(suite.getAttribute("errors"));
                skipped += Integer.parseInt(suite.getAttribute("skipped"));
            }
        }
        return new TestCounts(tests, failures, errors, skipped);
    }
}
