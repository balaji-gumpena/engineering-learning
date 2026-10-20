# Local lesson prerequisites

Run an implemented module’s Spring application class in the IDE, or use its consistent `make start`, `make restart` and `make stop` targets. No application CLI mode is needed. Corpus preview, index rebuild, search and retrieval evaluation are available through the browser UI/controllers.

Java 25 is required. The module’s Gradle wrapper resolves pinned dependencies. Model credentials belong only in the private IDE run environment or the environment inherited by Make; no environment file is loaded. Opening a page and lifecycle verification never sends a paid provider request.

Modules 07–08: Make manages the module-local synthetic commerce HTTP fixture. It is demo data without authentication or a real database. In IDE-only mode, the fixture is an additional local dependency; the module README identifies its Java main class.

Module 09: Make manages both the AI app and included H2 order backend. For IDE-only execution, run both documented Spring application classes; use the demo profile for the backend seed. Trusted identity configuration and a short-lived user token are still needed for protected requests. Make does not create a real identity realm or bypass token validation.

Modules 11–13: provide a disposable PostgreSQL instance with pgvector using Docker Desktop or your local infrastructure tooling. Configure image `pgvector/pgvector:pg17`, database `rag_test`, user `postgres`, and loopback host port `127.0.0.1:55439` mapped to container port 5432. For this isolated instance only, `POSTGRES_HOST_AUTH_METHOD=trust` permits an empty password. Use no host data volume and never point a lesson at the integrated application database. Remove only your disposable instance when finished. Make start/stop manages application processes; this explicitly external database is separate.

The module JDBC defaults are `jdbc:postgresql://127.0.0.1:55439/rag_test`, user `postgres`, empty password. Derived data is restricted to the `rag_java` schema. Run these lessons individually and rebuild the index using the same module before querying. Do not run concurrent modules against the same derived schema.

In the UI, use Preview chunks to inspect the corpus without changing the index, Rebuild demo index to replace its derived snapshot, and Evaluate retrieval to inspect passage-retrieval metrics. No generated-answer quality score is implied. Product-linked evidence is omitted when its live public catalog cannot be verified; policy evidence can still be used.

Embedding operations may download the source-pinned MiniLM ONNX model and matching tokenizer (revision `c9745ed1d9f207416be6d2e6f8de32d1f16199bf`) and native resources. The module-local cache is ignored. These operations require no LLM provider key. Module 10 uses in-memory retrieval and needs no PostgreSQL.

Normal `make test` checks use fixtures/mocks and actual local HTTP UI serving. Optional database and real-embedding checks remain explicit in the Java verifier (`make verify` runs the normal aggregate only); the IDE can run their tagged integration tests after disposable dependencies are ready. Live provider and identity journeys remain separately authorized work.
