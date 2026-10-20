# 11 — RAG ingestion

Objective and boundary: Safe Markdown/catalog preparation, token-aware chunks, metadata, fingerprints and transactional PostgreSQL/pgvector replacement. No generation endpoint or LLM client is included.

Implementation status: **Extracted — UI/unit checks verified; RAG integration pending**. A module-local chat UI has been added. This module passed 8 browser-client checks and 22 Java tests, including actual HTTP serving and invalid-request checks; its runnable jar contains the complete UI. Its remaining curriculum scope stays unchecked.

Prerequisites: Java 25 and this folder’s Gradle wrapper. Node 22+ runs UI checks without npm dependencies; Make is needed only for the optional Make workflow.

Run from the IDE (preferred):

1. Open this module’s `build.gradle` as a Gradle project using Java 25.
2. Run **`com.example.aiassistant.AiApplication`** as a Spring Boot application. Controllers are loaded automatically; they do not have a standalone `main` method.
3. Open **http://127.0.0.1:8000**. Configure model/credential variables privately in the IDE run configuration.

Or use the same Make targets as every implemented module:

```make
make start
make restart
make stop
make status
make test
make build
```

`make start` builds the module and starts its app in the background, returning after its UI responds. `make restart` stops, rebuilds and starts it. Logs and owned PID/start-time records are in `.runtime/`. `make stop` affects only processes owned by this module; it does not kill IDE-started apps or other processes using a port. Use the IDE’s Stop button for an IDE-started app. Starting on an occupied port fails clearly.

Override ports consistently with `make start PORT=8010 COMMERCE_PORT=8090`. In the IDE, use `SERVER_PORT` to change the app port. Run modules individually by default. Make manages the application process; PostgreSQL/pgvector remains the separately documented disposable prerequisite. Corpus preview, index rebuild and retrieval evaluation are UI buttons, not terminal application commands.

No separate frontend server or application CLI mode is required. This module owns its Makefile, lifecycle helpers and UI assets.

Setup and configuration: The page uses the existing local embedding/tokenizer components. First use may download the pinned public model/native resources. No LLM provider key is needed. Start only the disposable pgvector database described in [local infrastructure](../LOCAL_INFRASTRUCTURE.md). In the page, “Preview chunks” reads the local corpus without changing the index; “Rebuild demo index” replaces only its derived demo index. Then ask a question. These actions never call an LLM. Do not point the lesson at real application data.

Using the interface:

1. Click the suggested question to fill the composer, or type your own.
2. Click Send, or press Enter; Shift+Enter adds a line break.
3. Inspect the reply and any lesson-specific source cards or execution steps.
4. Use New chat to clear this tab’s successful history.

Each question is sent independently. Previous chat bubbles are visible for inspection but are not sent to the model/search operation. Empty messages are ignored; requests have a 4000-character message limit. Failed turns are excluded from follow-up history. There is one pending request at a time. Text is rendered as text, not HTML. Provider keys are never requested by the UI; module 09’s short-lived API access token is distinct from a provider key.

Expected behavior: source chunks, ingestion counts and index search results; no generated answer.

Independent verification:

```bash
node --test ui-tests/chat.test.cjs
./gradlew --no-daemon --console=plain test bootJar
```

The Node checks run this folder’s actual UI JavaScript and configuration with core Node test/DOM fixtures; no npm install, provider, database or browser is required. Java `ModuleUiTest` checks that this module actually serves its page/client/configuration. Transport tests use mocked boundaries and no paid calls. Real embedding, database, model, browser and identity evidence must be reported separately. The Java build also packages all static assets in `lesson.jar`. From the repository root, `./verify.sh --module 11-rag-ingestion` runs this module’s required checks plus common static checks; the aggregate without a selection covers every implemented module.

Local-only security: the teaching app binds 127.0.0.1. Except module 09, its endpoints are unauthenticated teaching endpoints. Do not expose them as a production application. No source checkout or cross-module code dependency is used.

Learning steps: read [the adapted lesson](docs/learning/lesson.md), [learning index](docs/learning/README.md) and [original source notes](docs/learning/source-notes/README.md), then try the suggested question and the exercise. [Observed verification and remaining limitations](../JAVA_VERIFICATION.md).

Completion checklist:

- [x] Relevant code and configuration extracted
- [x] Module boundary and independent startup documented
- [x] Learning guide and original source notes included
- [x] Relevant tests extracted/adapted
- [ ] Final verification passed


Required automated UI/unit/packaging checks passed. The final curriculum checkbox remains open for missing scope or unexecuted RAG integration. See the track verification report.

Code conventions: explicit Java types (no `var`), small classes with separate HTTP controllers, validated request/response records, lesson services, response projection, error handling and command startup. Demo fixtures and verification are Java; see [verification utilities](../../tools/README.md).

The final aggregate also passed this module’s real packaged lifecycle checks (startup/readiness, repeated start/stop, restart with a fresh PID and refusal to stop unowned PIDs). All Make-owned processes were stopped after verification.
