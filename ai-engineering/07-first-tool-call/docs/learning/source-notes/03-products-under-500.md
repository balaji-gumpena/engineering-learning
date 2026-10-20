# Products under a EUR budget

A customer asks “Show me products under €500.” The assistant retrieves live
catalog data, shows up to ten product cards, reports the total matching count,
and provides links to normal storefront product details.

## Data and model exchange

1. `ChatOrchestrator` recognizes product/item text with an English under/below
   numeric pattern and requires a tool call. The model supplies the numeric
   budget; the guard does not extract or verify its intended meaning.
2. `ToolService` validates the entire batch before execution. The
   `search_products` argument `max_price_eur_exclusive` must be a positive
   JSON number no greater than 100,000,000. Extra fields are rejected.
3. `CommerceApi.search` forwards the verified bearer token to
   `GET /api/products?maxPriceExclusive=500&page=0&size=10` on commerce.
4. Commerce applies active-product/EUR/exclusive-price filtering before paging.
   A €500 product does not match “under €500.”
5. The AI service checks pagination, identities, money and currency. It rejects
   invalid responses instead of inventing a zero-match result.
6. A final model call receives the tool results with further tools disabled.
   Known token counts are summed; unknown counts remain null.
7. The browser renders prose, trusted product cards and any cited knowledge.
   Selecting a card navigates to `/products/{id}`; the product page fetches
   details directly from commerce.

## Contracts and code

Browser input follows [first chat](01-first-chat-interaction.md).
The response includes `products`, `total_matches` and `has_more`.
Cards carry `id`, `name`, `price_eur`, `currency`, `image_path` and
`category_name`; `price_eur` is a decimal JSON string.

The AI implementation is `chat/ChatOrchestrator`, `tools/ToolService`,
`tools/ToolDefinitions` and `commerce/CommerceApi` beneath
`ai-service/src/main/java/com/example/aiassistant/`.
Commerce owns filtering in `commerce-service/.../product/ProductService.java`
and exposes it through `ProductController`. Frontend rendering is in
`ChatMessageView.tsx`, with route/detail behavior in `ProductDetailPage.tsx`.

Empty results are valid and distinct from unavailable commerce (503).
Invalid tool calls return a clarification without executing the batch.
The assistant cannot fetch the next result page or alter the cart.
Product stock is not a budget-search predicate.

## Verification and learning

Verified on 2026-10-16: normal AI tests cover strict arguments, required lookup,
cards/counts, money serialization, mixed budget/knowledge batches and usage;
commerce tests cover strict price filtering and pagination; frontend tests
cover rendering/navigation contracts. All 31 commerce and 25 frontend tests
passed. Model behavior was synthetic, not a paid live evaluation.

The manual examples in `ai-service/evaluation/product-search-cases.json`
describe expected behavior; they are not an executable quality score.
Fresh-seed counts can differ after catalog edits.

Learning: relational price filtering is a commerce tool, not semantic retrieval.
The model interprets the budget, but Java supplies authoritative products.
Next: evaluate real intent/argument accuracy only with explicit provider-call
authorization; see [RAG](15-commerce-rag-learning.md) for knowledge questions.
