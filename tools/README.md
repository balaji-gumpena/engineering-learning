# Java verification utilities

The repository uses Java 25 for application code, demo HTTP fixtures and verification utilities. Shell scripts only launch Java/Gradle or the pinned Java formatter. Browser assets and their Node fixture tests remain JavaScript.

From the repository root:

```bash
./verify.sh
./verify.sh --module 01-basic-llm-call
./verify.sh --static-only
```

The launcher uses Java 25 multi-file source execution; no extra build, JSON library or Python interpreter is required for these utilities. The module applications retain their own pinned Gradle builds.

Responsibilities:

- `Verify`: orchestration and module selection, continuing independent failures.
- `ModuleCatalog` / `CurriculumModule`: implemented/planned module inventory.
- `DocumentationLinks`: local authored Markdown links.
- `SourceBoundaries`: no Python source, no Java `var`, no source-checkout/cross-module imports, and early lesson boundaries.
- `PlannedModules`: completely empty folders and unchecked curriculum entries.
- `UiPackage`: runnable jar entry point and packaged static assets.
- `CheckRunner` / `CheckResult`: process execution, logs and observed outcomes.
- `TestCounts`: counts from actual JUnit XML with external XML access disabled.
- `VerificationReport`: current Markdown evidence in `ai-engineering/JAVA_VERIFICATION.md` and local `.verification/latest.md`.
- `RepositoryFiles`: traversal that skips caches, dependency directories and generated outputs.

Optional `--postgres` and `--embeddings` remain separate local integration checks. No verifier mode calls paid models or real identity journeys. For a single packaged jar use `./verify.sh --package ai-engineering/01-basic-llm-call/build/libs/lesson.jar`.

Module 07/08 demo fixtures are independent Java source programs in their own `support/commerce-fixture/` folders, managed automatically by `make start` / `make stop`. They split product data, catalog filtering, HTTP handling and startup into separate classes. These fixtures are synthetic test data, not a replacement real commerce backend.

Every implemented module has consistent Make start/restart/stop/status/build/test targets, plus a lifecycle-test used by the verifier. It launches real packaged apps on disposable ports, makes readiness GETs only, checks idempotency/restart, and refuses unowned PIDs. Module 07/08 fixtures and module 09 H2 commerce are managed along with their app. The project’s question/provider/knowledge CLI runners are removed; existing retrieval evaluation is a service/controller/UI operation. The opt-in real embedding evaluator is now a tagged JUnit task.
