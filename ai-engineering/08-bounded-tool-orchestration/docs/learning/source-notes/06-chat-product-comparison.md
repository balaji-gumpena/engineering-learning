# Product comparison in chat

“Compare products 1 and 2” requests two `get_product` calls in one model turn.
`ToolService` validates every call before any HTTP request. Unknown tools,
extra fields, non-integral/nonpositive IDs, duplicate/blank call IDs and batches
over four calls are rejected.

`CommerceApi` calls authenticated `GET /api/products/{id}` for each product.
Each result retains its tool-call ID. The final model call has further tools
disabled and generates comparison prose. There is one batch and one final
answer, not repeated autonomous planning. The assistant needs known product IDs.

Code: `ChatOrchestrator`, `ToolService`, `ToolDefinitions`, `CommerceApi`
under `ai-service/src/main/java/com/example/aiassistant/`.
See [product details](05-chat-product-details.md) for response validation/errors
and [first chat](01-first-chat-interaction.md) for the browser exchange.

Verification on 2026-10-16: `ChatOrchestratorTest` checks both product calls
and summed usage; `ToolValidationTest` checks bounded/strict batches.
The normal suite uses synthetic model/commerce responses. No live comparison
quality or full browser journey is claimed.

Learning: a batch of explicit tools can solve comparison without an agent.
Next: assess factual comparison quality through an explicitly authorized live
evaluation before adding any more orchestration.
