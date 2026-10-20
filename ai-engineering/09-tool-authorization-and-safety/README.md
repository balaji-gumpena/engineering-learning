# 09 — Tool authorization and safety

Objective and boundary: JWT validation and permission enforcement in the AI API; forwarded bearer credentials and response projection; an independently runnable H2 order backend copied from commerce with customer ownership enforced behind HTTP. No browser identity journey.

Implementation status: **Verified — independent UI, tests and packaging**. A module-local chat UI has been added. This module passed 7 browser-client checks and 31 Java tests, including actual HTTP serving and invalid-request checks; its runnable jar contains the complete UI. Its extracted learning scope is verified.

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

Override ports consistently with `make start PORT=8010 COMMERCE_PORT=8090`. In the IDE, use `SERVER_PORT` to change the app port. Run modules individually by default. Make builds and manages the included H2 commerce backend with its demo profile. For IDE-only mode, also run `com.example.aicommerce.AiCommerceApplication` from commerce/ with the demo profile. Supply your trusted identity configuration and short-lived access token as documented below.

No separate frontend server or application CLI mode is required. This module owns its Makefile, lifecycle helpers and UI assets.

Setup and configuration: Configure the selected provider credential privately on the server. The source default alias is `deepseek`; override with `--ai.active-provider=openai` when appropriate. Opening the page performs no paid call; submitting a chat message can call the selected provider. Make starts the included order backend automatically; the IDE alternative is described above. Configure the trusted `OIDC_ISSUER_URL`, `OIDC_JWKS_URL` and `OIDC_AUDIENCE` for both services. The demo seed uses issuer `http://localhost:8081/realms/lesson` and subjects `alice`/`bob`. Enter a short-lived access token in the UI’s session-only password field. It requires USERS_WRITE at AI and USERS_READ for orders; it is never put in model messages or browser storage. Real login/identity journeys are not supplied or executed here.

Using the interface:

1. Click the suggested question to fill the composer, or type your own.
2. Click Send, or press Enter; Shift+Enter adds a line break.
3. Inspect the reply and any lesson-specific source cards or execution steps.
4. Use New chat to clear this tab’s successful history.

Earlier successful turns are sent with follow-ups, bounded to 30 messages. Empty messages are ignored; requests have a 4000-character message limit. Failed turns are excluded from follow-up history. There is one pending request at a time. Text is rendered as text, not HTML. Provider keys are never requested by the UI; module 09’s short-lived API access token is distinct from a provider key.

Expected behavior: a reply from the selected configured model, plus the data relevant to this lesson. Live wording is model-dependent and is not a verified quality result.

Independent verification:

```bash
node --test ui-tests/chat.test.cjs
./gradlew --no-daemon --console=plain test bootJar
```

The Node checks run this folder’s actual UI JavaScript and configuration with core Node test/DOM fixtures; no npm install, provider, database or browser is required. Java `ModuleUiTest` checks that this module actually serves its page/client/configuration. Transport tests use mocked boundaries and no paid calls. Real embedding, database, model, browser and identity evidence must be reported separately. The Java build also packages all static assets in `lesson.jar`. From the repository root, `./verify.sh --module 09-tool-authorization-and-safety` runs this module’s required checks plus common static checks; the aggregate without a selection covers every implemented module.

Local-only security: the teaching app binds 127.0.0.1. Except module 09, its endpoints are unauthenticated teaching endpoints. Do not expose them as a production application. No source checkout or cross-module code dependency is used.

Learning steps: read [the adapted lesson](docs/learning/lesson.md), [learning index](docs/learning/README.md) and [original source notes](docs/learning/source-notes/README.md), then try the suggested question and the exercise. [Observed verification and remaining limitations](../JAVA_VERIFICATION.md).

Completion checklist:

- [x] Relevant code and configuration extracted
- [x] Module boundary and independent startup documented
- [x] Learning guide and original source notes included
- [x] Relevant tests extracted/adapted
- [x] Final verification passed


Code conventions: explicit Java types (no `var`), small classes with separate HTTP controllers, validated request/response records, lesson services, response projection, error handling and command startup. Demo fixtures and verification are Java; see [verification utilities](../../tools/README.md).

The final aggregate also passed this module’s real packaged lifecycle checks (startup/readiness, repeated start/stop, restart with a fresh PID and refusal to stop unowned PIDs). All Make-owned processes were stopped after verification.
