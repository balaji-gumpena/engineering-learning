# Commerce knowledge retrieval and grounded answers

RAG means retrieval-augmented generation: find relevant passages, supply them
to the language model with the question, then generate an answer supported by
that evidence. It does not train the generation model on the documents.

The feature runs inside the storefront's embedded assistant. It covers product
guidance, compatibility, comparison, intent-based discovery, FAQs, shipping and
returns. Sources are authored demo material and public catalog descriptions,
not certified manufacturer manuals or real legal policies.

## Two paths

Source preparation runs on startup/bootstrap or explicit ingestion:

```text
Markdown + commerce HTTP catalog
  → validated documents and source metadata
  → heading-aware chunks
  → local embeddings
  → atomic PostgreSQL index snapshot
```

A customer question runs retrieval and generation:

```text
POST /ai/chat → configured model → validated search_knowledge
  → embed query → retrieve passages → validate live product candidates
  → final model answer → checked citations and displayed source excerpts
```

Commerce remains authoritative for descriptions, prices, stock, carts and
orders. The AI service obtains commerce data through HTTP APIs, never commerce
tables. Prices, stock and private customer data are not embedded.

## Sources, chunks and embeddings

`ai-service/knowledge/` contains seven demo Markdown documents. Front matter
requires `source_id`, `title`, `source_type` and `demo: true`; a
`product_slug` is optional. Supported types are `policy`, `faq` and
`product_guide`. `MarkdownDocumentLoader` uses safe YAML parsing, rejects duplicates
and unsupported links, and hashes source content.

`IngestionService` optionally reads the public paged catalog through
`CommerceApi.publicCatalog`. It indexes description, brand and category.
It does not index mutable price/stock or private customer information.

A chunk is a passage small enough for the embedding model. `KnowledgeChunker`
keeps the heading hierarchy, ignores headings inside fenced code, and creates
overlapping word windows. Token measurement includes the title and heading.
The configured chunk budget is 240 tokens; oversized headings/words fail
explicitly rather than silently losing text.

An embedding is a numeric representation used to compare meaning. The pinned
MiniLM ONNX model runs locally with its matching tokenizer and produces
384-dimensional vectors. `ValidatingEmbeddingModel` checks token budgets, result
counts, dimensions, finite values and nonzero vectors, normalizes results and
serializes inference. No embedding-provider API key is needed. The first run
may download public model/native resources; later runs use the local cache.

Code is under `ai-service/src/main/java/com/example/aiassistant/rag/`:
`MarkdownDocumentLoader`, `KnowledgeChunker`, `ModelTokenizer`,
`ValidatingEmbeddingModel`, `RagConfiguration` and `IngestionService`.

The 2026-10-17 naming review made loader/validator/command responsibilities
explicit (`MarkdownDocumentLoader`, `ValidatingEmbeddingModel`,
`KnowledgeCommandRunner`) and renamed the lookup payload to
`KnowledgeRetrievalResult`. Algorithms, configuration keys, fingerprints and
JSON properties are unchanged. All four disposable PostgreSQL/local-embedding
tests passed after the renames; see [type-name meanings](05-chat-product-details.md#self-explanatory-type-names).

## Index updates, filtering and availability

`KnowledgeIndex` stores only derived data in the `rag_java` PostgreSQL schema.
Spring AI's `PgVectorStore` writes vectors; parameterized SQL performs exact
cosine retrieval with deterministic ID tie ordering.

Ingestion replaces the complete snapshot under an advisory transaction lock.
Changed/deleted documents therefore replace/remove their previous passages.
Chunks and fingerprint state commit together or roll back together. Empty
replacement requires an explicit flag. Inference currently runs inside the
transaction, which is suitable for this small corpus; larger corpora would
justify staging to shorten transactions.

The fingerprint identifies embedding model/tokenizer, dimensions, token budget
and overlap settings. Search validates fingerprint state and reads evidence on
the same repeatable-read snapshot. A missing/incompatible index yields unavailable
knowledge, rather than pretending the sources contain no answer. Re-ingest
after changing model/chunk configuration. Changing vector dimensions requires
deliberate schema maintenance.

Search supports product-slug and source-type filters, limits 1–20 and cosine
thresholds -1–1. Chat defaults to four passages, a 0.25 floor and a 6,000-character
evidence budget. Entire passages are kept or omitted; evidence is not cut
mid-statement. Similarity measures relatedness, not factual sufficiency.

Application PostgreSQL connections enforce a 30-second statement timeout and
five-second lock timeout. Health reports unavailable when the configured index
is missing/incompatible or the database cannot be checked.

## Live data, grounding and citations

`KnowledgeService` validates product-linked passages against the current public
catalog. Removed/inactive products are suppressed; live prices and stock are
hydrated at query time. A catalog outage can retain policy evidence while
excluding unverifiable product candidates.

`ChatOrchestrator` treats retrieved passages as untrusted data. The prompt
requires explicit uncertainty for unspecified facts and exact bracketed chunk
IDs such as `[returns-policy:2]`. Missing or unknown references withhold the
draft answer. No evidence or unavailable retrieval produces an explicit fallback.

The browser receives cited sources with title, section, excerpt, source path,
product slug and demo flag. It displays escaped text alongside prose/product
cards. Citation validation proves reference membership, not that every claim
is semantically supported. The assistant cannot approve returns, issue refunds,
invent shipping dates or substitute old document values for live commerce data.

The runtime uses one validated batch of at most four read-only tools and one
final model response. This flow needs neither an agent nor a model router.

## Execution visibility discussion — 2026-10-17

Question: should chat show selected tools, how RAG ran, and links to the flow?
The initial recommendation was subsequently authorized for implementation.
Implemented: an optional Learning mode, off by default, with a collapsed
“How this answer was produced” disclosure per assistant response. “View flow”
links open keyboard-focused explanations inside the same chat panel. These
describe the general algorithm; recorded events describe the particular request.

### API and execution facts

`POST /ai/chat` (proxied to `/chat`) accepts optional `learning_mode: true`.
Missing, null or false retains the existing behavior and omits `execution_trace`.
An opted-in success response adds `execution_trace`, with at most 40 steps:

```json
{"stage":"retrieval_outcome","status":"success","tool":"search_knowledge",
 "duration_ms":null,"result_count":2,"source_ids":["returns:0","returns:1"]}
```

`ChatExecutionTrace` is created for each request. It records initial/final model
calls, batch validation, model-selected tool names and completed tool execution.
The removed English keyword rules no longer emit live-search/knowledge-required
events. Untrusted names/arguments are not echoed when validation rejects a
batch. Failure events preserve no raw exception messages. When an opted-in
operation throws, `ChatExecutionException` retains only the safe steps;
`ApiExceptionHandler` returns them alongside the existing sanitized detail and
original error-status mapping. HTTP/auth/request validation failures before
orchestration do not have an execution trace.

The actual RAG path records query embedding in `KnowledgeIndex`, PostgreSQL
passage retrieval, optional live-product catalog loading in `KnowledgeService`,
evidence selection and the retrieval outcome. Empty/unavailable knowledge
retains its trace while skipping final generation. Live catalog failure can
still retain policy evidence. Citation rejection records why the draft was
withheld at the reference-validation stage, without exposing the rejected draft.

Counts have distinct meanings: passage retrieval counts raw matches; live
product validation counts public catalog entries loaded; evidence selection and
retrieval outcome count retained passages; citation validation counts referenced
source IDs. Retained evidence can include passages not cited in the final answer.
Only actually executed stages are recorded; no model-generated explanation is
used to reconstruct the flow. Document ingestion/chunking/index creation is a
separate preparation path and is never claimed to run on every chat message.

Timings use monotonic elapsed milliseconds around actual operations. Steps are
listed in completion order. A tool duration includes nested embedding/search
time, so durations overlap and must not be added. Selection/outcome events
without measured operations have null duration, not an invented zero.

### UI, ownership and limitations

`ChatWidget` owns the opt-in checkbox. `useChatConversation` retains each answer's
trace separately from conversation content and keeps failed-request traces
beside their error. `ChatExecutionTraceView` renders collapsed events with tool
names, outcomes, counts, source IDs, timings and inline flow links.
Turning Learning mode off hides these details. Previously untraced answers
cannot gain execution history retroactively; the UI explains this.
Only role/content is sent in follow-up history; traces never enter model context.

Normal answers, product cards, citations, prompts, tool selection, authentication,
commerce rules and model-call limits remain unchanged. No bearer token, question,
raw tool arguments, private order/cart payload, model reasoning or exception
text is placed in the trace. This is request-local demo instrumentation, not a
persisted telemetry service, an agent, a router or a streaming protocol.

Verification observed by Codex on 2026-10-17: 97 normal AI tests and 40 frontend
tests passed; AI jar packaging, frontend production build, pinned Java formatter
and whitespace checks passed. New cases cover opt-in/backward JSON compatibility,
plain/commerce/knowledge paths, rejected batches, required lookups, retained
versus cited sources, empty/unavailable knowledge, citation rejection, provider
and commerce failures, real index-method instrumentation with mocked infrastructure,
catalog-failure filtering, collapsed UI/flow focus, failed-request display and
trace exclusion from follow-up history. No paid-provider, identity or real
PostgreSQL/local-embedding tests were run for this change. Browser visual checks
remain unverified because browser/Computer Use access was previously unavailable.

User-reported storefront verification on 2026-10-17: with Learning mode enabled,
“Tell me about return policy” completed the expected successful policy path:
deterministic knowledge requirement, initial model call, one validated
`search_knowledge` request, query embedding, four retrieved and retained passages,
enclosing knowledge-tool completion, successful retrieval outcome, final model
generation and validation of four cited source IDs. The enclosing tool event
appeared after its nested retrieval events because the trace is ordered by
completion, and its 31 ms duration included the nested 13 ms embedding and 9 ms
retrieval durations. This is user-reported verification, not a Codex-observed
browser run. Plain-chat, commerce-tool and unavailable-knowledge browser paths
remain unverified.

Learning: citations establish which evidence is referenced, while execution facts
establish which operations ran. Neither proves every claim is correct. Next:
restart the AI service, enable Learning mode and smoke-test plain, commerce,
policy and unavailable-knowledge examples in the trusted storefront. Live-provider
verification still requires explicit authorization.

### Model-selected knowledge and commerce tools — 2026-10-17

The runtime no longer detects English price or policy terms with regular
expressions. The initial provider call uses automatic native tool selection and
can request zero to four tools. Spring AI converts those native calls into the
existing typed tool-call list; no custom tool-selection JSON protocol was added.
For a recent-order return question the model can request `get_orders` and
`search_knowledge` together, after which the server validates the complete batch,
executes both calls, supplies both results to the final model call and checks the
knowledge citations.

This preserves the distinction between probabilistic intent selection and
deterministic execution safety. The model decides which permitted information it
needs. Application code still restricts names and arguments, rejects duplicate
IDs and invalid batches, caps calls at four, enforces read-only boundaries, and
withholds a knowledge-backed draft when citations are missing or unknown. The
final generation call has no tools, so dependent tool planning and iterative
loops remain unsupported.

The deliberate limitation is that a direct model answer is now accepted even
for a question that a removed keyword rule previously forced through retrieval.
This fixes the regex's language and phrasing fragility but makes tool-choice
quality an evaluation concern. The next useful experiment is a dataset covering
paraphrases, multiple languages, direct answers, single tools and mixed batches,
compared by required-tool precision/recall. Codex observed 98 normal AI tests,
AI jar packaging, 43 frontend tests, the frontend build, Java formatting and
whitespace checks passing. A unit test covers the exact two-tool order/knowledge
case. No paid-provider, identity, PostgreSQL/embedding or browser run was made.

### Expandable end-to-end examples

These are representative, sanitized examples of the implemented contracts, not
captured live-provider traffic. Model names, tokens, timings, order data and
retrieved passage counts vary at runtime. The model payloads use the normalized
shape discussed in this project; OpenAI-compatible and Anthropic adapters use
their provider-specific wire envelopes, which Spring AI maps to and from
`AssistantMessage.ToolCall`.

Knowledge only:

```text
Browser POST /ai/chat
  → initial model selects search_knowledge
  → validate the complete tool batch
  → local query embedding
  → PostgreSQL passage retrieval
  → evidence selection
  → return the knowledge tool result to the model
  → final grounded answer
  → citation validation
  → browser response
```

<details>
<summary>Example 1 — expand knowledge-only requests and responses</summary>

#### 1. Browser → AI service

`POST /ai/chat` (the development frontend proxies this as `POST /chat`):

```json
{
  "messages": [
    {
      "role": "user",
      "content": "Tell me about the return policy"
    }
  ],
  "learning_mode": true
}
```

The bearer token is an HTTP header and is deliberately omitted from examples
and Learning traces.

#### 2. AI service → initial model

Normalized request outline:

```json
{
  "messages": [
    { "role": "system", "content": "AI Commerce instructions ..." },
    { "role": "user", "content": "Tell me about the return policy" }
  ],
  "tools": [
    "search_products",
    "get_product",
    "get_cart",
    "get_order",
    "get_orders",
    "search_knowledge"
  ],
  "tool_choice": "auto"
}
```

Normalized model response:

```json
{
  "tool_calls": [
    {
      "id": "call-1",
      "name": "search_knowledge",
      "arguments": {
        "question": "Tell me about the return policy"
      }
    }
  ]
}
```

`ToolService.validate` accepts this only after checking the batch size, unique
call ID, permitted name, argument names and knowledge-question limits.

#### 3. AI service → knowledge retrieval

Internal validated request:

```json
{
  "question": "Tell me about the return policy",
  "product_slug": null,
  "limit": 4,
  "minimum_score": 0.25
}
```

Representative tool result returned to the final model call:

```json
{
  "sources": [
    {
      "citation_id": "returns-policy:1",
      "title": "Demo returns policy",
      "section": "Returns policy > Return window and condition",
      "excerpt": "Under this demo policy, customers may request a change-of-mind return within 30 calendar days of delivery.",
      "source_path": "policies/returns.md",
      "product_slug": null,
      "demo": true
    },
    {
      "citation_id": "returns-policy:3",
      "title": "Demo returns policy",
      "section": "Returns policy > Who pays for return shipping",
      "excerpt": "For a change-of-mind return, the customer pays return postage under these demo terms.",
      "source_path": "policies/returns.md",
      "product_slug": null,
      "demo": true
    }
  ],
  "products": [],
  "live_products": [],
  "error": null
}
```

The real result can contain up to four retained passages. Embeddings and vectors
are internal search data and are not sent to the browser.

#### 4. AI service → final model

Normalized request outline:

```json
{
  "messages": [
    { "role": "system", "content": "AI Commerce instructions ..." },
    { "role": "user", "content": "Tell me about the return policy" },
    {
      "role": "assistant",
      "tool_calls": [
        {
          "id": "call-1",
          "name": "search_knowledge",
          "arguments": {
            "question": "Tell me about the return policy"
          }
        }
      ]
    },
    {
      "role": "tool",
      "tool_call_id": "call-1",
      "name": "search_knowledge",
      "content": "{\"sources\":[...],\"products\":[],\"live_products\":[],\"error\":null}"
    }
  ],
  "tools": [],
  "tool_choice": "none"
}
```

Normalized model response:

```json
{
  "content": "The fictional demo policy allows change-of-mind returns within 30 calendar days of delivery [returns-policy:1]. Customers normally pay change-of-mind return postage [returns-policy:3]."
}
```

The service extracts the bracketed IDs and verifies that every cited ID belongs
to the retained knowledge result before releasing the answer.

#### 5. AI service → browser

Representative successful HTTP response:

```json
{
  "reply": "The fictional demo policy allows change-of-mind returns within 30 calendar days of delivery [returns-policy:1]. Customers normally pay change-of-mind return postage [returns-policy:3].",
  "model": "configured-model",
  "usage": {
    "input_tokens": 1320,
    "output_tokens": 78,
    "total_tokens": 1398
  },
  "products": [],
  "total_matches": null,
  "has_more": false,
  "sources": [
    {
      "citation_id": "returns-policy:1",
      "title": "Demo returns policy",
      "section": "Returns policy > Return window and condition",
      "excerpt": "Under this demo policy, customers may request a change-of-mind return within 30 calendar days of delivery.",
      "source_path": "policies/returns.md",
      "product_slug": null,
      "demo": true
    },
    {
      "citation_id": "returns-policy:3",
      "title": "Demo returns policy",
      "section": "Returns policy > Who pays for return shipping",
      "excerpt": "For a change-of-mind return, the customer pays return postage under these demo terms.",
      "source_path": "policies/returns.md",
      "product_slug": null,
      "demo": true
    }
  ],
  "execution_trace": [
    { "stage": "initial_model", "status": "success", "tool": null, "duration_ms": 1100, "result_count": null, "source_ids": [] },
    { "stage": "tool_validation", "status": "success", "tool": null, "duration_ms": 0, "result_count": 1, "source_ids": [] },
    { "stage": "tool_selection", "status": "validated", "tool": "search_knowledge", "duration_ms": null, "result_count": null, "source_ids": [] },
    { "stage": "query_embedding", "status": "success", "tool": "search_knowledge", "duration_ms": 13, "result_count": null, "source_ids": [] },
    { "stage": "passage_retrieval", "status": "success", "tool": "search_knowledge", "duration_ms": 9, "result_count": 4, "source_ids": [] },
    { "stage": "evidence_selection", "status": "success", "tool": "search_knowledge", "duration_ms": null, "result_count": 2, "source_ids": ["returns-policy:1", "returns-policy:3"] },
    { "stage": "tool_execution", "status": "success", "tool": "search_knowledge", "duration_ms": 31, "result_count": null, "source_ids": [] },
    { "stage": "retrieval_outcome", "status": "success", "tool": "search_knowledge", "duration_ms": null, "result_count": 2, "source_ids": ["returns-policy:1", "returns-policy:3"] },
    { "stage": "final_model", "status": "success", "tool": null, "duration_ms": 2000, "result_count": null, "source_ids": [] },
    { "stage": "citation_validation", "status": "success", "tool": null, "duration_ms": null, "result_count": 2, "source_ids": ["returns-policy:1", "returns-policy:3"] }
  ]
}
```

</details>

Knowledge plus a commerce tool:

```text
Browser POST /ai/chat
  → initial model selects get_orders + search_knowledge
  → validate both calls before executing either
  → get_orders calls the authenticated commerce API
  → search_knowledge embeds and retrieves policy evidence
  → return both tool results to the model together
  → final answer combines current order data with cited policy evidence
  → citation validation
  → browser response
```

<details>
<summary>Example 2 — expand get_orders plus search_knowledge requests and responses</summary>

#### 1. Browser → AI service

```json
{
  "messages": [
    {
      "role": "user",
      "content": "What is the return policy for my recent order?"
    }
  ],
  "learning_mode": true
}
```

#### 2. Initial model selection

Normalized model response:

```json
{
  "tool_calls": [
    {
      "id": "call-1",
      "name": "get_orders",
      "arguments": {}
    },
    {
      "id": "call-2",
      "name": "search_knowledge",
      "arguments": {
        "question": "What is the return policy for a recent order?"
      }
    }
  ]
}
```

Both calls are validated as one batch. If either call is invalid, neither call
runs.

#### 3. `get_orders` → commerce service

Authenticated request; the bearer value is intentionally omitted:

```http
GET /api/orders HTTP/1.1
Host: commerce-service:8080
Authorization: Bearer <forwarded customer token>
```

Representative commerce response:

```json
[
  {
    "id": 17,
    "customerId": 42,
    "status": "CONFIRMED",
    "subtotal": 129.99,
    "paymentStatus": "SUCCESS",
    "paymentReference": "demo-payment-17",
    "createdAt": "2026-10-16T10:15:30Z",
    "items": [
      {
        "id": 31,
        "productId": 7,
        "productName": "Wireless Headphones",
        "quantity": 1,
        "priceAtPurchase": 129.99,
        "lineTotal": 129.99
      }
    ]
  }
]
```

The AI service projects that response before exposing it to the model. It drops
`customerId`, `paymentReference` and order-item IDs, validates every order, and
limits the model view to ten orders:

```json
{
  "orders": [
    {
      "id": 17,
      "status": "CONFIRMED",
      "subtotal": 129.99,
      "paymentStatus": "SUCCESS",
      "createdAt": "2026-10-16T10:15:30Z",
      "items": [
        {
          "productId": 7,
          "productName": "Wireless Headphones",
          "quantity": 1,
          "priceAtPurchase": 129.99,
          "lineTotal": 129.99
        }
      ]
    }
  ],
  "total_orders": 1,
  "has_more": false
}
```

#### 4. `search_knowledge` result

```json
{
  "sources": [
    {
      "citation_id": "returns-policy:1",
      "title": "Demo returns policy",
      "section": "Returns policy > Return window and condition",
      "excerpt": "Under this demo policy, customers may request a change-of-mind return within 30 calendar days of delivery.",
      "source_path": "policies/returns.md",
      "product_slug": null,
      "demo": true
    }
  ],
  "products": [],
  "live_products": [],
  "error": null
}
```

#### 5. Both tool results → final model

Normalized request excerpt:

```json
{
  "messages": [
    { "role": "user", "content": "What is the return policy for my recent order?" },
    {
      "role": "assistant",
      "tool_calls": [
        { "id": "call-1", "name": "get_orders", "arguments": {} },
        {
          "id": "call-2",
          "name": "search_knowledge",
          "arguments": {
            "question": "What is the return policy for a recent order?"
          }
        }
      ]
    },
    {
      "role": "tool",
      "tool_call_id": "call-1",
      "name": "get_orders",
      "content": "{\"orders\":[{\"id\":17,...}],\"total_orders\":1,\"has_more\":false}"
    },
    {
      "role": "tool",
      "tool_call_id": "call-2",
      "name": "search_knowledge",
      "content": "{\"sources\":[{\"citation_id\":\"returns-policy:1\",...}],\"products\":[],\"live_products\":[],\"error\":null}"
    }
  ],
  "tools": [],
  "tool_choice": "none"
}
```

Normalized model response:

```json
{
  "content": "Your most recent order is #17 for Wireless Headphones. Under the fictional demo policy, a change-of-mind return may be requested within 30 calendar days of delivery if the item remains complete, clean and undamaged [returns-policy:1]. The assistant cannot start or approve the return."
}
```

#### 6. AI service → browser

```json
{
  "reply": "Your most recent order is #17 for Wireless Headphones. Under the fictional demo policy, a change-of-mind return may be requested within 30 calendar days of delivery if the item remains complete, clean and undamaged [returns-policy:1]. The assistant cannot start or approve the return.",
  "model": "configured-model",
  "usage": {
    "input_tokens": 1510,
    "output_tokens": 92,
    "total_tokens": 1602
  },
  "products": [],
  "total_matches": null,
  "has_more": false,
  "sources": [
    {
      "citation_id": "returns-policy:1",
      "title": "Demo returns policy",
      "section": "Returns policy > Return window and condition",
      "excerpt": "Under this demo policy, customers may request a change-of-mind return within 30 calendar days of delivery.",
      "source_path": "policies/returns.md",
      "product_slug": null,
      "demo": true
    }
  ],
  "execution_trace": [
    { "stage": "initial_model", "status": "success", "tool": null, "duration_ms": 1150, "result_count": null, "source_ids": [] },
    { "stage": "tool_validation", "status": "success", "tool": null, "duration_ms": 0, "result_count": 2, "source_ids": [] },
    { "stage": "tool_selection", "status": "validated", "tool": "get_orders", "duration_ms": null, "result_count": null, "source_ids": [] },
    { "stage": "tool_selection", "status": "validated", "tool": "search_knowledge", "duration_ms": null, "result_count": null, "source_ids": [] },
    { "stage": "tool_execution", "status": "success", "tool": "get_orders", "duration_ms": 18, "result_count": null, "source_ids": [] },
    { "stage": "query_embedding", "status": "success", "tool": "search_knowledge", "duration_ms": 13, "result_count": null, "source_ids": [] },
    { "stage": "passage_retrieval", "status": "success", "tool": "search_knowledge", "duration_ms": 9, "result_count": 4, "source_ids": [] },
    { "stage": "evidence_selection", "status": "success", "tool": "search_knowledge", "duration_ms": null, "result_count": 1, "source_ids": ["returns-policy:1"] },
    { "stage": "tool_execution", "status": "success", "tool": "search_knowledge", "duration_ms": 31, "result_count": null, "source_ids": [] },
    { "stage": "retrieval_outcome", "status": "success", "tool": "search_knowledge", "duration_ms": null, "result_count": 1, "source_ids": ["returns-policy:1"] },
    { "stage": "final_model", "status": "success", "tool": null, "duration_ms": 2050, "result_count": null, "source_ids": [] },
    { "stage": "citation_validation", "status": "success", "tool": null, "duration_ms": null, "result_count": 1, "source_ids": ["returns-policy:1"] }
  ]
}
```

The browser receives the final answer, cited source cards and optional safe
execution facts. It does not receive the private order tool payload, bearer token,
retrieved vectors, complete prompts or provider reasoning.

</details>

### Context propagation design review — 2026-10-17

Question: should the mutable `ChatExecutionTrace` continue to be passed through
the orchestrator, tool service, knowledge service and knowledge index, or should
an ambient mechanism such as `ThreadLocal` hide that plumbing?

The explicit parameter is correct and testable for the implemented increment,
but it does not scale cleanly. It changes unrelated method signatures and makes
the tool and RAG packages depend on a chat-layer type. A raw `ThreadLocal` is not
the replacement: correctness would depend on thread identity and cleanup, while
servlet async work, executors, virtual threads and a future Reactor/WebFlux flow
may execute different stages on different threads. Reactor Context or a scoped
value would solve only particular execution models and would keep product-visible
response data ambient and harder to reason about.

Implemented decision: carry Learning-mode events upward with the operation
results that produced them. `KnowledgeIndex.searchObserved` returns an
`ObservedOperationResult<List<Document>>`; `KnowledgeRetrievalResult` and
`ToolExecutionResult` carry immutable, sanitized `ExecutionEvent` lists that are
excluded from tool JSON. `ChatOrchestrator` combines these with its own local
events only when Learning mode is enabled. The neutral `execution` package owns
measurement results and sanitized failures, so RAG and tools no longer import
or receive `ChatExecutionTrace`. This data-oriented design works for the current
synchronous flow, `Mono`/`Flux`, and a later SSE event stream because diagnostics
travel with results rather than with a worker thread.

The existing unobserved `KnowledgeIndex.search` methods delegate to the observed
implementation, return the same `List<Document>` contract and unwrap the
original runtime cause. Command/evaluation callers therefore do not need to
understand execution metadata. Lower layers collect the small event list even
when Learning mode is off; the orchestrator discards it, and the public response
remains unchanged. Nested RAG events are appended before the enclosing tool
completion event, preserving completion order.

Operational telemetry is a separate concern. Request correlation, metrics and
distributed spans should use the application's observability stack (for example,
Micrometer/OpenTelemetry context propagation) and should not become the source
of the shopper-visible Learning trace. `ThreadLocal`/MDC remains acceptable only
for best-effort synchronous log correlation with strict cleanup, not for response
correctness.

Status: implemented without `ThreadLocal`, Reactor Context or a request-scoped
proxy. The chat JSON schema, UI, tool payloads, tool selection, RAG behavior and
error mapping are unchanged. Verification observed by Codex on 2026-10-17:
97 normal AI tests and AI jar packaging passed. Tests cover result-carried event
ordering, immutable observed success/failure facts, retained catalog-failure
evidence and preservation of the original unobserved index exception type. The
pinned Java formatter passed after formatting. No live provider, identity or
real PostgreSQL/local-embedding run was performed for this refactor.

## Commands and evaluation

From the repository root:

```bash
make rag-ingest
make rag-search
make rag-evaluate
make rag-evaluate-memory
```

In `ai-service/`, command options include `--ai.cases`, `--ai.limit`,
`--ai.min-score`, `--ai.source-type`, `--ai.product-slug`,
`--ai.include-catalog` and `--ai.rag.source-directory`.
`--ai.in-memory=true` evaluates real embeddings without a reachable database.
See [the service guide](../../ai-service/README.md) for executable examples.

`RetrievalEvaluation` reads a configurable question dataset and reports expected
source/section, retrieved passages/scores, rank, per-query latency, backend and
fingerprint. Both answerable and unsupported cases remain visible. Unsupported
cases are diagnostics, not scored abstentions.

Readability review on 2026-10-16 separated evaluation command setup from
in-memory index preparation/search, named query/document vectors and cosine
norms explicitly, and applied the shared Java style to ingestion, chunking,
retrieval and their tests. The retrieval algorithm and report contract are
unchanged. Three synthetic command tests now verify source/product filters,
thresholds (including negative scores), tie ordering, limits, custom dataset
loading and PostgreSQL option forwarding. They use mock embeddings and no
database or LLM provider. All 73 normal AI tests and four disposable
PostgreSQL/local-embedding tests passed after the readability changes.

## Verification, limits and learning

Verified on 2026-10-16 through source inspection, normal Java tests, four
disposable PostgreSQL/local-embedding tests and packaged command execution:

- Source preparation, token budgets, vector checks, context limits and mixed
  knowledge/budget responses have regression coverage.
- Real PostgreSQL checks cover rollback, update/delete, filters, missing and
  incompatible index state, SQL timeout settings, combined catalog ingestion,
  current-price hydration and suppression of removed product candidates.
- PostgreSQL and database-free evaluators each hit the expected passage for
  12/12 answerable Markdown questions at three, with MRR 0.9583333333333334.
  Both unsupported questions appear in reports.
- These metrics use the seven Markdown sources; they are not a catalog-search
  or generated-answer quality score. The first database query includes cold
  model startup, while in-memory mode embeds the corpus before query timing;
  those latency measurements are not directly comparable.

No live-provider evaluation or complete browser/identity journey was performed.
The optional identity test was compiled but not run against a realm. Semantic
support of each generated claim and calibrated abstention remain unverified.
The 0.25 threshold is an initial heuristic, not a confidence probability.

Learning: document preparation, chunking, embedding, retrieval and generation
solve different problems. An index-maintenance failure differs from insufficient
evidence, and a citation differs from proof of grounding.

Next: a trusted storefront smoke test and explicitly authorized live evaluation
can assess answer accuracy, grounding, citations, unsupported answers, latency
and token use. No router or autonomous agent is planned for this step.
