# First LLM call — adapted lesson

Date: 2026-10-20. Status: Verified — independent UI, tests and packaging.

Problem: One OpenAI-compatible model call with system/user roles, a deadline, safe CLI failure output and provider usage. No tools, retrieval, routing, authentication or storefront.

Concepts and boundary: One OpenAI-compatible model call with system/user roles, a deadline, safe CLI failure output and provider usage. No tools, retrieval, routing, authentication or storefront.

Design decisions: Use the current Java 25 / Spring Boot 4.1.1 / Spring AI 2.0.1 direction and Gradle 9.6.0 wrapper. Copy only the required behavior into an independent module. Small adaptations make the lesson boundary visible; this is not an exact historical snapshot. Synthetic fixtures separate deterministic orchestration checks from unverified live model decisions.

Request/data flow:

```text
Browser question → local /chat → credential presence check → OpenAiChatModel → Prompt(system,user) → response bubble
```

Code locations: Start at [FirstCall.java](../../src/main/java/com/example/aiassistant/FirstCall.java). Production code is under `src/main/java`; fixture/unit tests are under `src/test/java`. Each module has its own wrapper and pinned build. 

Exercise: Change only the system instruction and question. Explain what is sent in each role and why a missing key fails before HTTP.

Substantive source reading: [the original notes index](source-notes/README.md) maps the relevant sections; the originals preserve the implementation discussions, payloads, decisions and verification provenance. Read those explanations alongside this adapted boundary. Source runtime paths and historical results apply to the integrated application, not this module.

Limitations: One OpenAI-compatible model call with system/user roles, a deadline, safe CLI failure output and provider usage. No tools, retrieval, routing, authentication or storefront. Passing deterministic checks cannot establish real model answer quality or a complete identity/browser journey.

Verification: exact commands are in [the module README](../../README.md), observed outcomes and skipped checks in [the current verification report](../../../VERIFICATION.md). No module test had executed when the initial extraction guide was prepared; the final standalone UI run has now passed. Current status is updated only after the final aggregate run. Historical source “passed” statements remain history.

Next step: [Prompt design](../../../02-prompt-design/).

## Standalone chatbot interface — 2026-10-20

The user requested a minimal interface for each implemented lesson. This module now owns a dependency-free chat page in `src/main/resources/static/` and a local `run.sh`. Spring serves the page and calls to this module’s existing learning operations on the same port. This is new presentation/transport work around existing ai-commerce capabilities; it does not implement absent AI lessons.

Flow: browser composer → same-origin module endpoint → existing lesson operation → text/source/execution response → safe DOM rendering. `chat.js` keeps only successful user/assistant history where that lesson supports it. Early single-turn, retrieval and provider-selection lessons send only the latest question. No framework or cross-module source import hides the lesson.

Code: `static/index.html`, `chat.css`, `chat.js` and `lesson-config.js`; browser contract tests in `ui-tests/chat.test.cjs`; Spring asset-serving checks in `ModuleUiTest`. Existing CLI-only lessons have thin HTTP controllers; startup no longer invokes a paid operation or evaluates embeddings automatically. Module 09 permits only the page assets publicly and retains its API token/authority checks.

Verification: consult the current track report. Node fixture results validate request/history/rendering/error behavior, not a browser visual run, a live model decision or Java packaging. Java unit/fixture and actual HTTP serving checks have executed successfully; real provider/embedding/PostgreSQL checks remain separate. Future modules remain completely empty and unchecked.

## Java-only utilities and class responsibilities — 2026-10-20

The user required no Python code and emphasized small, modular Java classes with explicit types. The previous verification/fixture scripts were replaced with Java source-launch programs and shell entry points. `./verify.sh` is now the aggregate entry point; the original earlier Python command references in historical verification artifacts are not current instructions.

HTTP controllers now delegate to lesson services. Request/response records, source-passage projection, error advice and command startup have separate classes. The first lesson keeps one model adapter and one small service; conversation conversion stays separate from its HTTP controller. Semantic retrieval separates the evaluation command from the existing search algorithm and query validation. Ingestion UI services retain the existing chunk/index methods and distinguish preview from index replacement. No new AI capability or shared runtime framework was introduced.

The verifier rejects Python source files and Java `var` declarations. Future modules remain completely empty. Observed checks and counts are recorded in [the current Java verification report](../../../JAVA_VERIFICATION.md), with remaining live/integration limits separate.

## IDE and consistent Make workflow — 2026-10-20

The user does not want terminal-only application operations. Run this module’s Spring Boot application class in the IDE; its controllers and minimal UI load normally. For Make-based execution, every implemented module provides the same start/restart/stop/status/build/test targets. Future folders remain completely empty and have no invented runtime.

Removed question/provider CLI runners and terminal-driven knowledge/evaluation runners. The existing retrieval evaluation now belongs to a service behind `/lesson/evaluation` and an “Evaluate retrieval” UI control. Existing tests assert returned evaluation reports rather than captured terminal output. No absent AI capabilities were implemented.

The module-local lifecycle helpers build jars, start owned processes, wait for HTTP readiness and retain PID plus start-time/identity records in `.runtime/`. Stop/restart refuses unrelated or reused PIDs. Modules 07/08 manage their own synthetic commerce fixture; 09 manages its included H2 order backend. External credentials, identity configuration and disposable RAG infrastructure remain explicit prerequisites. No provider calls occur merely by starting/stopping the app.

Verification adds lifecycle smoke checks of the actual packaged app: readiness, idempotent start/stop, a fresh PID after restart and refusal to stop an unowned PID. See the current Java verification report for observed results.
