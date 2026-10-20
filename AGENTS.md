# Engineering learning conventions

- Keep ai-commerce read-only; implement changes only in this learning repository.
- Use Java 25 and the pinned per-module Spring/Gradle stack. Do not add Python code, including fixtures or verification utilities.
- Use explicit Java types; do not use `var`. Keep classes small and named for their responsibility.
- Controllers handle HTTP only; keep request/response records, model/knowledge services, projections, errors and application bootstrap in their own classes.
- Preserve independent module builds and module-local minimal chatbot UIs. Limited duplication is preferable to a framework or cross-module runtime dependency.
- Future modules 02, 03, 05, 06, 15 and 16 must remain completely empty and unchecked until explicitly requested.
- Do not implement missing AI capabilities to fill curriculum gaps. Do not read real secrets or make paid/live model calls without authorization.
- Run `./verify.sh` for aggregate Java/UI/package checks; use `--module DIRECTORY` or `--static-only` when appropriate. Record actual evidence and keep optional integration/live results separate.
- Run `bash scripts/format-java.sh --check`. Update the affected learning documents and track progress after substantive changes. Preserve original source notes unchanged.

- Each implemented module must keep consistent make start/restart/stop/status/build/test targets and support running its Spring application class directly in an IDE. Do not reintroduce terminal-only application operations; expose them through the UI/controllers. Process lifecycle helpers must verify PID ownership/start time and never kill processes merely by port or name.
