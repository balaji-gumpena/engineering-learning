# Order details in chat

“Where is order 7?” requests `get_order({"order_id":7})`.
`ToolService` validates a positive integral long ID before
`CommerceApi.order` calls authenticated `GET /api/orders/7`.

Commerce verifies ownership. The AI service validates and projects order status,
payment status, subtotal, creation time and purchased items, omitting customer
IDs and payment references. A final model call produces prose and combined
available usage. Missing IDs use [order history](08-chat-order-history.md).

A missing or other-customer order produces the same `not_found` tool result.
401/403 produce `access_denied`; invalid data, timeouts and upstream failures
produce chat 503. The assistant cannot cancel an order, alter payment, invent
shipment tracking or promise an arrival date.

Code: `ToolService`, `CommerceApi.order`, `ChatOrchestrator` and
`system-prompt.txt` in `ai-service/`; `OrderController` and
`OrderService.detailForCustomer` in `commerce-service/`.
[First chat](01-first-chat-interaction.md) describes the shared exchange.

Verification on 2026-10-16: Java tests check strict arguments, projection,
HTTP errors and orchestration; commerce tests check ownership. Live model/browser
behavior remains unverified. Learning: model-selected IDs never confer access;
the commerce service independently authorizes the caller.
Next: trusted customer-isolation smoke checks.
