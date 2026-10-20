# AI engineering curriculum

Preserved progressive modules extracted from [ai-commerce](https://github.com/balaji-gumpena/ai-commerce). Begin at 01; planned lessons remain visible, and the tools path can proceed without streaming. Run modules individually. Modules 02, 03, 05, 06, 15 and 16 are completely empty planned folders. Partial implemented modules provide existing runnable subsets; their full proposed learning scope is incomplete.

## High-level module overview

Basic LLM and RAG are the main learning areas. Most populated modules break those areas into smaller, independently runnable lessons. Commerce tool calling, security, provider integration and execution visibility cover supporting capabilities already present in ai-commerce. The 16-module curriculum does not mean 16 separate capabilities have been completed.

| Module | What it contains | Relationship to ai-commerce |
| --- | --- | --- |
| 01 — Basic LLM call | A configured model call, message roles, failures and token usage | Existing basic chat capability |
| 04 — Conversation state | Passing bounded previous messages to the model | Existing chat history handling; no persistent memory |
| 07 — First tool call | Product search, tool results and the model's final answer | Existing commerce tool calling |
| 08 — Bounded tool orchestration | Several read-only tool calls, such as comparing products | Existing bounded orchestration; no autonomous agent |
| 09 — Tool authorization and safety | Permissions, bearer-token forwarding and customer-owned order access | Existing application security |
| 10 — Embeddings and semantic retrieval | Creating vectors and finding relevant passages | Existing RAG retrieval; the proposed lexical-versus-semantic comparison exercise is absent |
| 11 — RAG ingestion | Reading documents, chunking content and storing embeddings in pgvector | Existing RAG ingestion |
| 12 — Grounded RAG answers | Answers using retrieved content, citations and fallbacks | Existing RAG answering; generated-answer quality evaluation is absent |
| 13 — Execution visibility | Tool/retrieval events, timings and token usage | Existing Learning mode; monetary cost estimation is absent |
| 14 — Provider comparison | Explicit provider selection and OpenAI-compatible/Anthropic protocol adapters | Provider integration exists; an actual provider comparison benchmark is absent |

The remaining modules are completely empty and unchecked. Their proposed standalone capability or exercise is absent from ai-commerce:

| Planned module | Missing scope |
| --- | --- |
| 02 — Prompt design | Dedicated prompt-design comparison exercises; the application does have a system prompt |
| 03 — Structured outputs | Structured model-output generation; tool argument schemas are a separate capability |
| 05 — Streaming | Incremental model responses |
| 06 — Evaluation baseline | Generated-answer quality benchmarks; existing retrieval evaluation is covered under RAG |
| 15 — Deterministic model routing | Automatic routing; explicit provider aliases are a separate capability |
| 16 — Context and cost optimization | Measured optimization experiments; bounded history and answer limits already exist |

Standalone lesson UIs, Make commands and lifecycle helpers were added for this learning repository. They make the extracted capabilities independently runnable; they are not additional AI features from ai-commerce.

## Extraction and verification status

The checkboxes below track extraction and verification, **not your personal learning completion**. A checked module has verified extracted scope. Unchecked populated modules still have missing proposed scope or pending integration checks; they may already have passing UI and unit checks.

- [x] [01-basic-llm-call](./01-basic-llm-call/README.md) — Verified — independent UI, tests and packaging
- [ ] [02-prompt-design](./02-prompt-design/) — Planned — empty
- [ ] [03-structured-outputs](./03-structured-outputs/) — Planned — empty
- [x] [04-conversation-state](./04-conversation-state/README.md) — Verified — independent UI, tests and packaging
- [ ] [05-streaming](./05-streaming/) — Planned — empty
- [ ] [06-evaluation-baseline](./06-evaluation-baseline/) — Planned — empty
- [x] [07-first-tool-call](./07-first-tool-call/README.md) — Verified — independent UI, tests and packaging
- [x] [08-bounded-tool-orchestration](./08-bounded-tool-orchestration/README.md) — Verified — independent UI, tests and packaging
- [x] [09-tool-authorization-and-safety](./09-tool-authorization-and-safety/README.md) — Verified — independent UI, tests and packaging
- [ ] [10-embeddings-and-semantic-retrieval](./10-embeddings-and-semantic-retrieval/README.md) — Partially extracted — UI and unit checks verified
- [ ] [11-rag-ingestion](./11-rag-ingestion/README.md) — Extracted — UI/unit checks verified; RAG integration pending
- [ ] [12-grounded-rag-answers](./12-grounded-rag-answers/README.md) — Extracted — UI/unit checks verified; RAG integration pending
- [ ] [13-execution-visibility](./13-execution-visibility/README.md) — Partially extracted — UI and unit checks verified
- [ ] [14-provider-comparison](./14-provider-comparison/README.md) — Partially extracted — UI and unit checks verified
- [ ] [15-deterministic-model-routing](./15-deterministic-model-routing/) — Planned — empty
- [ ] [16-context-and-cost-optimization](./16-context-and-cost-optimization/) — Planned — empty

[Extraction map](EXTRACTION_MAP.md) · [Progress ledger](PROGRESS.md) · [Verification report](JAVA_VERIFICATION.md) · [Local infrastructure](LOCAL_INFRASTRUCTURE.md) · [Attribution](ATTRIBUTION.md).

Run all implemented checks from the repository root: `./verify.sh`. It continues after independent failures and exits nonzero if any required check fails. Planned modules are reported as planned, not passed. Optional local integration: `--postgres` and `--embeddings`; these do not make LLM provider calls.

Empty planned folders contain no README, source notes, scaffolding or `.gitkeep`. Their index entries remain unchecked. Git does not track empty directories; these folders exist in the local workspace.

Every implemented module now owns a small chatbot UI served by its Java app. Run its `make start` and open http://127.0.0.1:8000; follow its README for configuration. Independent browser-client checks are `node --test ui-tests/chat.test.cjs` from that module. Full Java build/serving verification is reported separately. Future modules 02, 03, 05, 06, 15 and 16 stay completely empty.

Verification and fixtures are Java-only. Use `./verify.sh`; current results are in [JAVA_VERIFICATION.md](JAVA_VERIFICATION.md). The verifier rejects Python source and Java `var` declarations.
