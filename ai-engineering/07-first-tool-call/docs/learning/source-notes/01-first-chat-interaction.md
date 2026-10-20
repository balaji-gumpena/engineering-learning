# First chat interaction

The embedded storefront assistant runs in `ai-service/` on port 8000. The
customer opens Chat, sends a message, and sees plain text, any product cards
and cited sources. Model names and token usage remain in the response/state
for accounting but are not rendered to shoppers.

## Request flow

1. `ChatWidget` and `useChatConversation` retain conversation state in the
   browser. Guests are asked to log in before sending.
2. The API helper refreshes the access token and sends `POST /ai/chat`.
   Vite proxies this to `POST /chat` on the AI service.
3. Spring Security verifies the JWT and requires `USERS_WRITE`.
   `ChatController` validates up to 30 user/assistant messages, each limited
   to 4,000 characters, ending with a user message.
4. `ChatOrchestrator` prepends the system instructions and calls one configured
   model through `SpringAiGateway`.
5. A plain reply needs one model call. Tool requests use one fully validated
   batch of up to four read-only calls, then one final model call with tools
   disabled. There is no autonomous planning loop.
6. The browser receives `reply`, `model`, `usage`, `products`,
   `total_matches`, `has_more`, and `sources`. Opted-in Learning mode requests
   also receive `execution_trace`; ordinary requests retain the previous JSON.

Example request:

```json
{"messages":[{"role":"user","content":"Hello"}]}
```

The request carries the user's bearer token. The model never receives that
token. Commerce tools forward it only to the commerce HTTP API.

### Learning mode — implemented 2026-10-17

Problem: citations alone did not show which tools or retrieval stages ran.
The chat now has a Learning mode checkbox, off by default. Enabling it adds
`learning_mode: true` to new requests and exposes a collapsed “How this answer
was produced” view. Recorded steps link to inline explanations, which receive
keyboard focus. Failures with a trace show “How this request failed”. Disabling
the mode hides details, and older untraced answers explain their lack of history.

Relevant code: `ChatDtos`, `ChatController`, `ChatOrchestrator`,
`ChatExecutionTrace`, `ChatExecutionException`, `ApiExceptionHandler`,
`frontend/src/api/client.ts`, `useChatConversation`, `ChatWidget`,
`ChatMessageView` and `ChatExecutionTraceView`. Trace metadata is not sent in
follow-up model history. Prompts, authorization, replies and orchestration limits
are unchanged. Read [the execution flow and contract](15-commerce-rag-learning.md#execution-visibility-discussion--2026-10-17)
for RAG instrumentation, outcomes, counts, timings and privacy boundaries.

Codex observed 97 normal AI tests and 40 frontend tests passing, successful AI
jar/frontend build, and formatting/whitespace checks. New cases cover opt-in
API success/error paths, sanitized failures, genuine recorded stages, disclosure
visibility, flow-link focus and trace exclusion from history. No live model,
real identity or PostgreSQL/embedding run was performed for this task; browser
visual verification remains unavailable. Learning: observable execution facts
belong outside the model's messages and do not require another model call.
Next: restart the AI backend and smoke-test the Learning mode in a trusted
browser; live provider calls still require authorization.

## Configuration and code

Java code is under `ai-service/src/main/java/com/example/aiassistant/`:
`chat/ChatController`, `ChatDtos`, `ChatOrchestrator`, `ApiExceptionHandler`,
`llm/SpringAiGateway`, and `tools/ToolService`. Provider selection, model IDs,
key-variable names and suggestions live in `src/main/resources/application.yml`.
Prompts live in `src/main/resources/system-prompt.txt`.

`LlmGateway.LlmResponse` represents one model call, which may request tools
instead of answering. Its `TokenUsage` records available token counts.
`ChatDtos.ChatResponse` is the final browser payload after orchestration;
`ChatRequest` and `ChatMessage` represent incoming conversation data. The
2026-10-17 naming review clarified these boundaries without changing JSON
properties. See [type-name meanings](05-chat-product-details.md#self-explanatory-type-names).

DeepSeek uses the OpenAI-compatible adapter; OpenAI and Anthropic are configured
alternatives. The chat flow uses the configured default provider. Other Java
callers can explicitly choose a different configured provider/model alias per
operation without changing that default or adding automatic routing.
Credentials come from terminal/platform variables named in configuration.
Do not put keys into source, browser variables, environment files or chat.

Each provider request explicitly uses a 15-second deadline and zero SDK
retries. OpenAI reasoning effort, DeepSeek thinking settings, and Anthropic
adaptive thinking/effort stay isolated in the provider adapter.

### Runtime provider selection and extension

On 2026-10-16 the user requested maintainable integrations and multiple providers
for different operations, such as titles, content and image understanding.
`SpringAiGateway` now depends on injected `ChatProviderAdapter` implementations,
not vendor-specific branches. `OpenAiChatProviderAdapter` and
`AnthropicChatProviderAdapter` build their protocol's model and fresh request options.
`LlmGateway.complete(providerId, messages, tools, requireTool)` selects an alias
for one call; the original overload delegates to the configured default.

Clients are initialized lazily and cached independently by alias with
concurrent initialization protection. Clients and request options are isolated;
each client uses its alias's model and credential configuration. Missing credentials affect only the selected
alias. Duplicate adapter IDs and unsupported configured protocols fail startup;
unknown runtime aliases fail rather than silently using the default.

OpenAI-compatible endpoints require configuration only; a genuinely new
protocol needs one Spring-managed adapter and any necessary SDK dependency.
Adapter identifiers are validated syntactically and resolved against injected
implementations, not a fixed name list. Configuration may use task aliases for
different models from the same vendor. See the
[provider extension guide](../../ai-service/README.md#multiple-providers-at-runtime).

The new HTTP fixture exposed a request-options model default overriding the
configured model (`gpt-5-mini` instead of the synthetic configured name).
Both adapters now set the selected model explicitly for every request; wire
assertions protect initial and final calls. Existing deadlines, tool control
and unknown-usage semantics are retained.

Image-bearing Spring AI messages are passed through and their serialized media
reference is tested against a loopback fixture. No image-upload UI, real visual
understanding, image generation or image editing has been implemented/verified
by this change. Image generation is a separate capability, not another chat
provider flag. No title/content feature or automatic routing policy was added.

## Validation, usage and errors

Empty/invalid requests return 422. Guests receive 401 and missing permissions
403. Missing credentials or unavailable commerce/identity verification produce
503. Provider timeouts produce 504; rate limits 429; other provider failures
502. Public errors exclude raw exception text.

Replies are bounded to 4,000 characters so they can be reused in history.
Unknown token counts stay null; known counts are summed across both model
calls. Missing usage never means zero tokens. Successful reply and fallback
paths log request ID, model, latency and available usage without message text
or credentials. Conversation state and usage are not persisted on the server.

## Verification and learning

### Storefront presentation — 2026-10-17

Problem: chat exposed developer metadata and needed a clearer shopper interface.
Implemented in `frontend/src/components/ChatWidget.tsx`, `ChatMessageView.tsx`
and `styles.css`: one icon-and-label launcher, a 360px rounded panel above it,
avatar/title/status, SVG close/send icons, and full-screen presentation below
640px. The shell reserves an 84px bottom strip so the closed launcher cannot
overlap scrolling product labels. Closing with Escape or the close button
returns focus to the launcher; drafts and conversations survive closing.
The send button is disabled for whitespace or while waiting. Starter chips
now fill the input; see [suggestions](02-initial-suggestion-chips.md).

The conversation hook, requests, backend, prompts and response processing are
unchanged. Product cards and citations remain available. This is presentation
work, introducing no new AI concept; accounting data need not be shopper UI.

Codex observed all 34 frontend tests passing across 12 files and a successful
production build. Chat tests verify chip filling without requests, explicit
submission, metadata hiding, close/reopen, errors, history and cited responses.
Desktop/mobile visual verification and requested screenshots remain unverified:
neither Chrome nor the in-app browser connector was available, and native
Computer Use reported that permissions were not granted. No live model call or
identity journey was performed. Next: obtain browser access and capture cart,
collections, chat and header at desktop/mobile sizes.

Verified on 2026-10-16 through the Java normal suite, synthetic HTTP provider
fixtures, and frontend component tests. `ChatApiTest`, `ChatOrchestratorTest`,
`ApiExceptionHandlerTest` and `ChatProviderAdapterTest` cover these boundaries, including an
actual SDK deadline against a local slow server and both provider adapters.
No paid model call or complete browser/identity journey was performed.

Provider-extension verification on 2026-10-16: Codex ran all 82 normal AI tests
and four disposable PostgreSQL/local-embedding tests successfully and packaged
the AI jar. The nine added cases cover adapter identifier validation, injected
protocol extension, multiple aliases, concurrent per-alias initialization,
credential/failure boundaries, invalid registration, empty responses and the
real SDK's isolated HTTP requests/media-reference serialization. Earlier
frontend/commerce results were not rerun for this AI-only change. Next: use
explicit aliases when implementing each requested task; verify live model
capabilities and quality only with authorization.

Messages, tools and business authorization have separate responsibilities:
the model suggests a lookup; deterministic code validates execution; commerce
owns the underlying data and access rules.

### Model-selected multi-tool batches — 2026-10-17

Tool selection is now provider-native and language-independent. The first model
call receives all six tool definitions with automatic selection enabled and may
return no tool, one tool, or up to four independent tool calls. Spring AI maps
the provider response to `AssistantMessage.ToolCall`; the application does not
parse a second custom `{tool_required: ...}` response. A request about the return
policy for a recent order can therefore select both `get_orders` and
`search_knowledge` in the same response and execute them as one bounded batch.

The removed budget/policy regular expressions no longer override the model or
reject a batch for omitting a keyword-associated tool. Deterministic application
controls still validate every tool name, ID and argument, reject the whole batch
when any call is invalid, cap the batch at four, expose only read-only tools, and
require valid retrieved-source citations whenever `search_knowledge` runs. The
second model call receives no tools, so this remains a two-call orchestration
flow rather than an agent loop.

Tradeoff: when the first model call answers directly, the service no longer has
an independent policy/price classifier that can prove a lookup was necessary.
Prompt instructions and tool definitions guide selection, but multilingual and
paraphrased tool-choice quality needs evaluation. Codex verified the exact
`get_orders` plus `search_knowledge` batch with a mocked native tool-call response;
no paid-provider request was made.

Next: a trusted storefront smoke test and explicitly authorized live-model
evaluation can assess the external journey and answer quality. See
[authentication](04-authentication-and-authorization.md),
[commerce tools](05-chat-product-details.md), and [RAG](15-commerce-rag-learning.md).
