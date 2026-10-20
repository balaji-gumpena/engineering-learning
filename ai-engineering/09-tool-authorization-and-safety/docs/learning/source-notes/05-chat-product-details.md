# Product details in chat

For “Tell me about product 1,” the model requests
`get_product({"product_id":1})`. `ToolService` validates a strict positive
long ID and the complete batch before `CommerceApi.product` calls authenticated
`GET /api/products/1` on commerce.

Commerce owns visibility and product rules. The AI service validates/projects
name, description, brand, price, currency, optional image/category, nonnegative
stock and rating before the final model call. It does not query commerce tables.
The browser shows prose and summed available usage.

404 becomes `not_found`, 401/403 become `access_denied`; malformed responses,
timeouts and commerce server failures produce chat 503. No customer ID or
arbitrary endpoint is accepted. The prompt asks for a product ID when unknown;
automatic name-search-then-detail is not implemented.

Code: `ai-service/.../tools/ToolService.java`,
`commerce/CommerceApi.java`, `chat/ChatOrchestrator.java`, and commerce
`product/ProductController.java` under `commerce-service/`.
[First chat](01-first-chat-interaction.md) describes browser/model contracts.

Verification on 2026-10-16: normal Java tests cover IDs, full-batch validation,
HTTP response projection, malformed stock/rating/text, token forwarding and
orchestration. Live model selection and a complete browser journey remain
unverified. Learning: the model selects a capability; validated code and commerce
provide facts and authorization. Next: trusted storefront/live evaluation.

## Java readability

On 2026-10-16, the user requested moderate, easily understandable Java across
both services, including tests. All production/test Java files were checked for
inferred local declarations, wildcard imports, missing control-flow braces and
compressed formatting. The common style uses explicit local types,
interface-typed collections, descriptive names and one statement per line.
The shared four-space formatter covers both services, not archived code.

Named helpers distinguish catalog parsing, field projection, tool-argument
validation, chat message preparation, bounded tool execution and citation
extraction. Evaluation separates command setup from filtered in-memory search.
The private chat batch record groups results already collected together; it
does not add an agent loop or a new architectural layer. No HTTP contract or
validation behavior was intentionally changed.

`var` infers a static local type at compile time; it is not dynamic typing.
Explicit types are useful when a method's return type is not obvious. A
declaration such as `List<Product> products = new ArrayList<>()` combines a
readable interface contract with a concrete implementation. The
[OpenJDK style guide](https://openjdk.org/projects/amber/guides/lvti-style-guide)
recommends choosing inferred or explicit types according to readability.

Use `bash scripts/format-java.sh --check` to detect formatting drift and
`--write` to format both source and test trees. The pinned formatter is
checksum-verified and cached; the first run needs a public Maven download.
Naming, interface selection and method responsibilities still require review.

Verification: Codex reran 73 normal AI tests, four disposable PostgreSQL/local-
embedding tests and 31 commerce tests on 2026-10-16; all passed. The three new
synthetic evaluation-command cases cover filters, thresholds including negative
scores, deterministic ties, limits and database dispatch without provider calls.
Formatting and whitespace checks passed. Live providers and identity flows
were excluded; frontend tests were not rerun because no frontend code changed.
Next: retain the shared style for future changes and review runtime behavior
separately from readability.

### Self-explanatory type names

On 2026-10-17, the user found `Turn` unclear and requested a project-wide
type-name review. Both Java services' declarations were inspected. Changes
concentrated on the AI service's generic imported records and ambiguous helper
names; established commerce names such as `ProductService`, `CartItem` and
`OrderRepository` already describe their domain/responsibility and were kept.

| Previous name | Current name | Meaning |
|---|---|---|
| `Turn` | `LlmResponse` | One model call's assistant message, model name and token usage |
| `Usage` | `TokenUsage` | Input, output and total token counts |
| `Call` | `ValidatedToolCall` | A tool invocation accepted by argument/batch validation |
| `Execution` | `ToolExecutionResult` | A tool's payload, products, evidence and availability state |
| `Lookup` | `KnowledgeRetrievalResult` | Retrieved evidence and validated live products |
| `SearchResult` | `ProductSearchResult` | Live product-search results and paging summary |
| `Message`, `Request`, `Response` | `ChatMessage`, `ChatRequest`, `ChatResponse` | Browser chat API records |
| `Product`, `Source` (chat DTOs) | `ProductSummary`, `SourceCitation` | Product-card data and evidence references |

`LlmResponse` can contain tool requests, so it is not necessarily a final answer
to the customer. `ChatResponse` is the separate browser response produced by
the orchestrator. This distinction is more useful than the conversational
shorthand “turn.” Types should remain clear after imports remove their outer
class/package context.

Configuration types are `ProviderSettings`, `RagSettings` and `ChatSuggestion`.
Helpers now name their job: `ApiExceptionHandler`, `MarkdownDocumentLoader`,
`ValidatingEmbeddingModel`, `KnowledgeCommandRunner` and `ToolBatchResult`.
`ChatProviderAdapter` (and its OpenAI/Anthropic implementations) explicitly
identifies chat-model integration rather than all image/generation capabilities.
Matching tests, constructors, imports, local names and code-guide references
were updated. Framework-owned names and archived code were not changed.

No customer flow, JSON property, configuration key or validation rule was
intentionally changed. `ChatDtosTest` explicitly verifies request round trips
and the full response JSON shape, including usage, prices, products and citations.
Next: retain meaningful payload/responsibility names when adding new types.

Verification on 2026-10-17: Codex observed 84 normal AI tests (including the
two new JSON contract cases), four disposable PostgreSQL/local-embedding tests
and 31 commerce tests passing. AI jar packaging, shared formatting and
whitespace checks passed. PostgreSQL tests also exercise renamed configuration
record binding and Spring component wiring. No live providers, identity flow
or frontend tests were run for this naming-only change.
