package learning.verification;

import java.util.List;
import java.util.Set;

final class ModuleCatalog {
    private static final List<String> DIRECTORIES =
            List.of(
                    "01-basic-llm-call",
                    "02-prompt-design",
                    "03-structured-outputs",
                    "04-conversation-state",
                    "05-streaming",
                    "06-evaluation-baseline",
                    "07-first-tool-call",
                    "08-bounded-tool-orchestration",
                    "09-tool-authorization-and-safety",
                    "10-embeddings-and-semantic-retrieval",
                    "11-rag-ingestion",
                    "12-grounded-rag-answers",
                    "13-execution-visibility",
                    "14-provider-comparison",
                    "15-deterministic-model-routing",
                    "16-context-and-cost-optimization");
    private static final Set<String> PLANNED = Set.of("02", "03", "05", "06", "15", "16");

    private ModuleCatalog() {}

    static List<CurriculumModule> modules() {
        return DIRECTORIES.stream()
                .map(name -> new CurriculumModule(name, !PLANNED.contains(name.substring(0, 2))))
                .toList();
    }
}
