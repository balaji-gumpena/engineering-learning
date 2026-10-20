# Engineering learning

Independent engineering lessons extracted from the integrated [ai-commerce project](https://github.com/balaji-gumpena/ai-commerce). Each implemented AI module owns its Java/Spring app, minimal chatbot UI, tests and consistent Makefile.

- [ ] [AI engineering curriculum](ai-engineering/README.md) — verified subsets plus partial/planned lessons; future modules remain empty.

See the [high-level module overview](ai-engineering/README.md#high-level-module-overview) for how Basic LLM, RAG and supporting commerce capabilities map to ai-commerce. Curriculum checkboxes track extraction and verification, not personal learning completion.

Open an implemented module’s `build.gradle` in your IDE and run its Spring Boot application class. The README identifies the class and private model/credential configuration. Controllers load automatically. Open http://127.0.0.1:8000.

The same optional Make interface is available in every implemented module:

```make
make start
make restart
make stop
make status
make test
make build
```

From this root, select a module with `make start MODULE=01-basic-llm-call` (likewise restart/stop/status/test/build). `make verify` checks all implemented modules. Make-started processes and logs are owned per module under `.runtime/`; IDE-started apps are stopped with the IDE.

Application operations use the UI/controllers. No question/provider or knowledge CLI mode is required. Browser UI is module-local; there is no separate frontend server. Modules 07–09 automatically manage their included commerce fixture/backend through Make; RAG databases and identity/provider configuration remain documented prerequisites.

[Current verification](ai-engineering/JAVA_VERIFICATION.md) · [Extraction map](ai-engineering/EXTRACTION_MAP.md) · [Java tools](tools/README.md) · [Progress](ai-engineering/PROGRESS.md).

Future subjects: infrastructure, Kafka and Flink are reserved for later work. All application/fixture/verification code is Java with explicit types and no `var`; browser assets/tests are JavaScript. No Python interpreter is required. No source edits, commits or publication are included.
