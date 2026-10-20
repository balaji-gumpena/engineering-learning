# Progress ledger — 2026-10-20

- [x] Existing source capabilities extracted with independent chatbot UIs
- [x] Java-only tooling, explicit Java types and focused classes
- [x] Empty/unchecked future modules 02, 03, 05, 06, 15, 16 preserved
- [x] Remove optional application CLI runners; expose existing evaluation via UI/controllers
- [x] Plain Spring application startup for IDE execution
- [x] Consistent make start/restart/stop/status/build/test in every implemented module
- [x] Module-owned lifecycle helpers with readiness, PID ownership and startup rollback
- [x] Run all module suites, UI/package checks and real lifecycle smoke checks
- [x] Record current evidence and source integrity

Preferred workflow: run the Spring application class from the module’s IDE Gradle project. Optional Make workflow: make start/restart/stop, selecting MODULE=... from the root. Controllers load with the app. No question/provider/knowledge terminal interaction is needed. UI retrieval evaluation preserves existing source capability; it does not evaluate generated answer quality.

The Java verifier remains ./verify.sh (also make verify). It tests actual packaged startup/stop/restart and PID safety without sending model requests. Logs and PID records are ignored under .runtime/. Do not stop unrelated or IDE-owned processes. RAG PostgreSQL, private provider credentials and trusted identity remain explicit external prerequisites.

No Python, no Java var declarations, no source edits, no commits/publication. Keep live providers, real local embedding inference, PostgreSQL integration and identity journeys separate from automated fixture evidence.

Final evidence: ./verify.sh exited 0; 298 JUnit tests, 70 UI fixture checks, ten packaged lifecycle suites and eight fixture HTTP checks passed. All ten core Makefiles are byte-identical. No owned PID files/processes remain; six future folders are still empty. Current commands and counts are in JAVA_VERIFICATION.md and verification-results.json.
