# Current Java verification

Observed at 2026-10-20T11:57:31.696392Z. Entry point: `./verify.sh`.

| Check | Result | JUnit tests/failures/errors/skipped | UI checks | Command and directory |
|---|---|---|---|---|
| documentation-links | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| source-boundaries-and-java-style | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| empty-planned-modules | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| java-format | PASS | — | 0 | `bash scripts/format-java.sh --check` in `.` |
| 01-basic-llm-call | PASS | 7/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/01-basic-llm-call` |
| 01-basic-llm-call-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 01-basic-llm-call-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/01-basic-llm-call` |
| 01-basic-llm-call-ui | PASS | — | 6 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/01-basic-llm-call` |
| 04-conversation-state | PASS | 10/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/04-conversation-state` |
| 04-conversation-state-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 04-conversation-state-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/04-conversation-state` |
| 04-conversation-state-ui | PASS | — | 6 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/04-conversation-state` |
| 07-first-tool-call | PASS | 17/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/07-first-tool-call` |
| 07-first-tool-call-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 07-first-tool-call-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/07-first-tool-call` |
| 07-first-tool-call-ui | PASS | — | 6 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/07-first-tool-call` |
| 07-first-tool-call-commerce-fixture | PASS | — | 0 | `java support/commerce-fixture/learning/fixture/FixtureHttpTest.java` in `ai-engineering/07-first-tool-call` |
| 08-bounded-tool-orchestration | PASS | 30/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/08-bounded-tool-orchestration` |
| 08-bounded-tool-orchestration-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 08-bounded-tool-orchestration-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/08-bounded-tool-orchestration` |
| 08-bounded-tool-orchestration-ui | PASS | — | 6 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/08-bounded-tool-orchestration` |
| 08-bounded-tool-orchestration-commerce-fixture | PASS | — | 0 | `java support/commerce-fixture/learning/fixture/FixtureHttpTest.java` in `ai-engineering/08-bounded-tool-orchestration` |
| 09-tool-authorization-and-safety | PASS | 31/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/09-tool-authorization-and-safety` |
| 09-tool-authorization-and-safety-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 09-tool-authorization-and-safety/commerce | PASS | 2/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/09-tool-authorization-and-safety/commerce` |
| 09-tool-authorization-and-safety-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/09-tool-authorization-and-safety` |
| 09-tool-authorization-and-safety-ui | PASS | — | 7 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/09-tool-authorization-and-safety` |
| 10-embeddings-and-semantic-retrieval | PASS | 15/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/10-embeddings-and-semantic-retrieval` |
| 10-embeddings-and-semantic-retrieval-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 10-embeddings-and-semantic-retrieval-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/10-embeddings-and-semantic-retrieval` |
| 10-embeddings-and-semantic-retrieval-ui | PASS | — | 7 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/10-embeddings-and-semantic-retrieval` |
| 11-rag-ingestion | PASS | 22/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/11-rag-ingestion` |
| 11-rag-ingestion-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 11-rag-ingestion-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/11-rag-ingestion` |
| 11-rag-ingestion-ui | PASS | — | 8 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/11-rag-ingestion` |
| 12-grounded-rag-answers | PASS | 76/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/12-grounded-rag-answers` |
| 12-grounded-rag-answers-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 12-grounded-rag-answers-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/12-grounded-rag-answers` |
| 12-grounded-rag-answers-ui | PASS | — | 8 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/12-grounded-rag-answers` |
| 13-execution-visibility | PASS | 76/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/13-execution-visibility` |
| 13-execution-visibility-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 13-execution-visibility-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/13-execution-visibility` |
| 13-execution-visibility-ui | PASS | — | 9 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/13-execution-visibility` |
| 14-provider-comparison | PASS | 12/0/0/0 | 0 | `./gradlew --no-daemon --console=plain test bootJar` in `ai-engineering/14-provider-comparison` |
| 14-provider-comparison-ui-package | PASS | — | 0 | `./verify.sh --static-only` in `.` |
| 14-provider-comparison-lifecycle | PASS | — | 0 | `make lifecycle-test` in `ai-engineering/14-provider-comparison` |
| 14-provider-comparison-ui | PASS | — | 7 | `node --test ui-tests/chat.test.cjs` in `ai-engineering/14-provider-comparison` |

Executed Java tests: 298. UI fixture checks: 70.

Planned modules 02, 03, 05, 06, 15 and 16 are empty, unchecked and excluded, never counted as passed.

Node checks use the actual browser client with HTTP/DOM fixtures. JUnit includes actual Spring loopback HTTP and synthetic provider/JWKS/H2 tests. Fixture evidence is not live model behavior or manual visual browser evidence.

Optional PostgreSQL checks requested: false; real local embedding evaluation requested: false. Without those options, they remain unverified. No paid providers or external identity journeys are called.

## IDE and Make lifecycle outcome — 2026-10-20

Final `./verify.sh` exited 0: 298 JUnit tests, 70 browser-client fixture checks, ten module lifecycle suites, and eight Java commerce-fixture HTTP checks passed. Each lifecycle suite launched the actual packaged app, waited for its page to respond, checked idempotent start/stop, restarted to a fresh PID, and verified refusal to stop an unowned/reused PID. Modules 07/08 included their owned HTTP fixture and module 09 included its H2 backend. All ten module Makefiles have identical core recipes. No Make-owned processes or PID records remain after the checks.

Every implemented module supports `make start`, `make restart`, `make stop`, `make status`, `make build`, and `make test`. Root Make forwards the same targets with `MODULE=...`. Start builds before launching, returns after readiness, and writes logs/state under ignored `.runtime/`. Stop only acts on that module’s owned PID/start-time/command identity; it does not stop IDE processes or kill by port/name. Restart stops before rebuilding/starting.

For IDE execution, run the module’s documented Spring Boot application class. Controllers load automatically; no application question/provider/knowledge CLI path remains. Existing retrieval evaluation now has a service/controller and UI button. UI operational actions stay out of subsequent model conversation history. Tagged embedding/PostgreSQL tests remain optional and were not executed here.

Independent modules 02, 03, 05, 06, 15 and 16 remain completely empty and unchecked. No Python or Java var declarations were introduced. Source checkout status, original learning notes and recorded source-document hashes are unchanged. No paid provider calls, real identity journeys, source edits, commits or publication occurred.
