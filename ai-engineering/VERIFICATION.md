# Earlier verification history

This report predates the Java-only utility cleanup. Its commands are historical, not current setup instructions. Use `./verify.sh` and [JAVA_VERIFICATION.md](JAVA_VERIFICATION.md) for current checks and outcomes.

# Verification report — 2026-10-20

Final verification observed at 2026-10-20T11:00:25.093789+00:00. Source historical figures remain separate. The initial extraction pass preceded module suites. This report covers the final standalone UI/backend verification after fixes.

| Check | Outcome | JUnit tests / failures / errors / skipped | Exact command (from cwd) |
|---|---|---|---|
| documentation-links | PASS | not reported as JUnit | `python3 scripts/check-docs.py` in `.` |
| runtime-boundaries | PASS | not reported as JUnit | `python3 scripts/check-boundaries.py` in `.` |
| empty-planned-modules | PASS | not reported as JUnit | `python3 scripts/check-references.py` in `.` |
| java-format | PASS | not reported as JUnit | `bash scripts/format-java.sh --check` in `.` |
| 01-basic-llm-call | PASS | 6 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/01-basic-llm-call` |
| 01-basic-llm-call-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/01-basic-llm-call/build/libs/lesson.jar` in `.` |
| 01-basic-llm-call-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/01-basic-llm-call` |
| 04-conversation-state | PASS | 8 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/04-conversation-state` |
| 04-conversation-state-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/04-conversation-state/build/libs/lesson.jar` in `.` |
| 04-conversation-state-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/04-conversation-state` |
| 07-first-tool-call | PASS | 17 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/07-first-tool-call` |
| 07-first-tool-call-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/07-first-tool-call/build/libs/lesson.jar` in `.` |
| 07-first-tool-call-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/07-first-tool-call` |
| 08-bounded-tool-orchestration | PASS | 30 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/08-bounded-tool-orchestration` |
| 08-bounded-tool-orchestration-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/08-bounded-tool-orchestration/build/libs/lesson.jar` in `.` |
| 08-bounded-tool-orchestration-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/08-bounded-tool-orchestration` |
| 09-tool-authorization-and-safety | PASS | 31 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/09-tool-authorization-and-safety` |
| 09-tool-authorization-and-safety-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/09-tool-authorization-and-safety/build/libs/lesson.jar` in `.` |
| 09-tool-authorization-and-safety/commerce | PASS | 2 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/09-tool-authorization-and-safety/commerce` |
| 09-tool-authorization-and-safety-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/09-tool-authorization-and-safety` |
| 10-embeddings-and-semantic-retrieval | PASS | 15 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/10-embeddings-and-semantic-retrieval` |
| 10-embeddings-and-semantic-retrieval-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/10-embeddings-and-semantic-retrieval/build/libs/lesson.jar` in `.` |
| 10-embeddings-and-semantic-retrieval-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/10-embeddings-and-semantic-retrieval` |
| 11-rag-ingestion | PASS | 20 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/11-rag-ingestion` |
| 11-rag-ingestion-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/11-rag-ingestion/build/libs/lesson.jar` in `.` |
| 11-rag-ingestion-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/11-rag-ingestion` |
| 12-grounded-rag-answers | PASS | 74 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/12-grounded-rag-answers` |
| 12-grounded-rag-answers-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/12-grounded-rag-answers/build/libs/lesson.jar` in `.` |
| 12-grounded-rag-answers-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/12-grounded-rag-answers` |
| 13-execution-visibility | PASS | 74 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/13-execution-visibility` |
| 13-execution-visibility-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/13-execution-visibility/build/libs/lesson.jar` in `.` |
| 13-execution-visibility-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/13-execution-visibility` |
| 14-provider-comparison | PASS | 12 / 0 / 0 / 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/14-provider-comparison` |
| 14-provider-comparison-ui-package | PASS | not reported as JUnit | `python3 scripts/check-package.py ai-engineering/14-provider-comparison/build/libs/lesson.jar` in `.` |
| 14-provider-comparison-ui-test | PASS | not reported as JUnit | `node --test ui-tests/chat.test.cjs` in `ai-engineering/14-provider-comparison` |

Planned (excluded, never counted as passing): 03 structured outputs, 05 streaming, 15 deterministic model routing. Empty planned chapters 02, 03, 05, 06, 15 and 16 have no module tests. Partial chapters stay unchecked even when their extracted tests pass.

Evidence types: Java JUnit/loopback SDK/JWKS fixtures and H2/MockMvc ownership tests are automated deterministic checks. Commerce HTTP fixtures and mocked model choices are not live model decisions. The default UI tests execute each module’s actual JavaScript with Node core and a minimal DOM fixture. ModuleUiTest checks real Spring static-resource serving when Java tests run. The old module 13 React reference is optional and is not required by the default browser UI. These checks use actual loopback HTTP to the module Spring server. No manual visual verification is implied. Original source counts and user-reported browser observations are historical only.

Optional disposable PostgreSQL checks requested: False. Real local embedding evaluation requested: False. Paid/live providers and external identity journeys were not executed. Local logs are under `.verification/`; structured outcomes are in [verification-results.json](verification-results.json).

Environment limitations and follow-up: without `--postgres`, transaction/pgvector/real-embedding integration is unverified in this checkout; without `--embeddings`, real retrieval metrics are not current evidence. No credentials were read or requested. No manual browser inspection or external network link validation is claimed.

## Standalone chatbot UI outcome — 2026-10-20

All ten implemented modules now serve their own minimal chatbot UI from their own Java app. Each owns its HTML, CSS, browser JavaScript, configuration, run script and UI tests; no shared frontend runtime or npm/Vite server is needed. Modules 02, 03, 05, 06, 15 and 16 are completely empty and unchecked.

Final aggregate `./verify.sh` exited 0: **289 Java tests and 66 UI checks passed**, with zero reported failures/errors/skips in executed Java suites. The 40 module-serving checks use actual HTTP against independent Spring servers, not only mocked servlet dispatch. Each jar was inspected for its Boot entry point and all five module-local UI assets. Module 09’s additional commerce project passed two real H2/MockMvc ownership checks.

The first UI serving assertion used MockMvc against a forwarded welcome page, which has no rendered response body under that test mechanism. It was replaced by actual loopback HTTP against each Spring server. The database-free retrieval module also retained test references to the removed database class; the existing query validation method was isolated locally and the regression tests adapted. All affected checks and the full aggregate then passed.

The earlier cache-permission blocker was resolved by approval for the independent Gradle checks and the updated aggregate verifier. No paid model calls or external identity journeys were performed. Actual local model inference, PostgreSQL/pgvector integration and a manual visual browser journey remain unverified. The Node DOM fixtures check safe rendering and interactions; they are not screenshot or real-browser visual evidence. Excluded integration tags are not included in the zero-skipped count above.

Open a module’s UI by running `./run.sh` from its directory, then visiting http://127.0.0.1:8000. Modules 01/04 need a configured model argument; provider keys stay on the server. Modules 07/08 include their own optional synthetic commerce HTTP fixture. Modules 11–13 offer UI controls for the existing corpus preparation and derived-index rebuild; these require the documented disposable dependencies and do not invoke an LLM. Module 09 allows the static page publicly but retains token/permission checks on its API.

[Per-module UI evidence](ui-verification-results.json) · [Curriculum checklist](README.md) · [Learning indexes and extraction inventory](EXTRACTION_MAP.md).
