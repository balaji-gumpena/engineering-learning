# Source-to-module extraction map

Date: 2026-10-20. Source HEAD `bdb8c3e061b6288263908477b888c44850a73dbe`. Source: local ai-commerce; target: engineering-learning, distinct sibling checkouts. Target was empty and git-clean. The source was read-only.

Uncommitted source content used: modified `AGENTS.md` (instructions), modified `docs/learning/README.md` (index), untracked `docs/learning/16-learning-module-structure.md` (curriculum). The untracked extraction prompt supplied task requirements; its source file was not used as production material. No uncommitted production-code changes were present. See [initial status](SOURCE_STATUS_BEFORE.txt).

No legacy stack was consulted. Current ai-service, commerce-service and frontend provided the material. Active frontend uses complete JSON responses, not streaming. No evidence of structured model outputs, routing, agents, skills or a measured model/context comparison was found. Retrieval evaluation is distinct from generated-answer evaluation; tool argument validation is distinct from structured output.

## [01-basic-llm-call](01-basic-llm-call/README.md)

Classification: **extractable**. One OpenAI-compatible model call with system/user roles, a deadline, safe CLI failure output and provider usage. No tools, retrieval, routing, authentication or storefront.

Source material: 

- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatOrchestrator.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/OpenAiChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/SpringAiGateway.java`
- `ai-service/src/test/java/com/example/aiassistant/llm/ChatProviderAdapterTest.java`

Relevant original document sections: Request flow, Configuration and code, Validation, usage and errors. See [learning/source notes index](01-basic-llm-call/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Flattened the gateway and one compatible adapter into FirstCall; removed providers/tools/RAG/security from early lessons. Contract fixtures use a synthetic loopback HTTP server.

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. 

Missing capabilities/limits: One OpenAI-compatible model call with system/user roles, a deadline, safe CLI failure output and provider usage. No tools, retrieval, routing, authentication or storefront.

## [02-prompt-design](02-prompt-design/)

Status: **Planned — empty; unchecked**. The folder intentionally contains no code, documents, references, tests or placeholder files, following the user’s instruction. It is excluded from runnable verification.

Source capability assessment: The integrated application’s existing system prompt and its documented design decisions. No standalone prompt comparison experiment exists in ai-commerce.

Future work will be added as the curriculum progresses. No implementation or results are claimed for this chapter.

## [03-structured-outputs](03-structured-outputs/)

Status: **Planned — empty; unchecked**. The folder intentionally contains no code, documents, references, tests or placeholder files, following the user’s instruction. It is excluded from runnable verification.

Source capability assessment: Proposed conversion of model output into a validated shopping-requirements DTO. No structured model-output implementation was found. Tool-argument schemas and ordinary request DTOs are different boundaries.

Future work will be added as the curriculum progresses. No implementation or results are claimed for this chapter.

## [04-conversation-state](04-conversation-state/README.md)

Classification: **extractable**. Caller-carried user/assistant history converted into Spring AI messages. Every request supplies its own state; there is no server-side memory or session store.

Source material: 

- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatOrchestrator.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/OpenAiChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/SpringAiGateway.java`
- `ai-service/src/test/java/com/example/aiassistant/llm/ChatProviderAdapterTest.java`
- `frontend/src/hooks/useChatConversation.ts`

Relevant original document sections: Request flow, Validation, usage and errors. See [learning/source notes index](04-conversation-state/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Flattened the gateway and one compatible adapter into FirstCall; removed providers/tools/RAG/security from early lessons. Contract fixtures use a synthetic loopback HTTP server.

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. 

Missing capabilities/limits: Caller-carried user/assistant history converted into Spring AI messages. Every request supplies its own state; there is no server-side memory or session store.

## [05-streaming](05-streaming/)

Status: **Planned — empty; unchecked**. The folder intentionally contains no code, documents, references, tests or placeholder files, following the user’s instruction. It is excluded from runnable verification.

Source capability assessment: Proposed incremental answer delivery and cancellation. The active frontend awaits a complete JSON reply; no runnable streaming implementation was found.

Future work will be added as the curriculum progresses. No implementation or results are claimed for this chapter.

## [06-evaluation-baseline](06-evaluation-baseline/)

Status: **Planned — empty; unchecked**. The folder intentionally contains no code, documents, references, tests or placeholder files, following the user’s instruction. It is excluded from runnable verification.

Source capability assessment: The existing manual product-search cases and original descriptions of contract tests. No executable generated-answer evaluation baseline exists in ai-commerce. Retrieval evaluation is extracted in module 10.

Future work will be added as the curriculum progresses. No implementation or results are claimed for this chapter.

## [07-first-tool-call](07-first-tool-call/README.md)

Classification: **extractable**. One native search_products request, strict arguments, a commerce HTTP lookup and one final answer. Only search_products is exposed; batches larger than one are rejected.

Source material: 

- `ai-service/.env.example`
- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/AiApplication.java`
- `ai-service/src/main/java/com/example/aiassistant/AiProperties.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/AiException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionTrace.java`
- `ai-service/src/main/java/com/example/aiassistant/commerce/CommerceApi.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionEvent.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionObservation.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationException.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationResult.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/AnthropicChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/ChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/LlmGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/OpenAiChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/SpringAiGateway.java`
- `ai-service/src/test/java/com/example/aiassistant/TestProperties.java`
- `ai-service/src/test/java/com/example/aiassistant/commerce/CommerceApiTest.java`

Relevant original document sections: Model-selected multi-tool batches — 2026-10-17, Data and model exchange, Java readability. See [learning/source notes index](07-first-tool-call/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Focused source subsets retain package/type names; local entry points replace integrated startup. 

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. 

Missing capabilities/limits: One native search_products request, strict arguments, a commerce HTTP lookup and one final answer. Only search_products is exposed; batches larger than one are rejected.

## [08-bounded-tool-orchestration](08-bounded-tool-orchestration/README.md)

Classification: **extractable**. Up to four read-only commerce calls, whole-batch validation before execution, call-ID preservation and one final model call without tools. No RAG or autonomous agent loop.

Source material: 

- `ai-service/.env.example`
- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/AiApplication.java`
- `ai-service/src/main/java/com/example/aiassistant/AiProperties.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/AiException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionTrace.java`
- `ai-service/src/main/java/com/example/aiassistant/commerce/CommerceApi.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionEvent.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionObservation.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationException.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationResult.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/AnthropicChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/ChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/LlmGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/OpenAiChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/SpringAiGateway.java`
- `ai-service/src/test/java/com/example/aiassistant/TestProperties.java`
- `ai-service/src/test/java/com/example/aiassistant/commerce/CommerceApiTest.java`

Relevant original document sections: Model-selected multi-tool batches — 2026-10-17, Data and model exchange. See [learning/source notes index](08-bounded-tool-orchestration/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Focused source subsets retain package/type names; local entry points replace integrated startup. 

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. 

Missing capabilities/limits: Up to four read-only commerce calls, whole-batch validation before execution, call-ID preservation and one final model call without tools. No RAG or autonomous agent loop.

## [09-tool-authorization-and-safety](09-tool-authorization-and-safety/README.md)

Classification: **extractable**. JWT validation and permission enforcement in the AI API; forwarded bearer credentials and response projection; an independently runnable H2 order backend copied from commerce with customer ownership enforced behind HTTP. No browser identity journey.

Source material: 

- `ai-service/.env.example`
- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/AiApplication.java`
- `ai-service/src/main/java/com/example/aiassistant/AiProperties.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/AiException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionTrace.java`
- `ai-service/src/main/java/com/example/aiassistant/commerce/CommerceApi.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionEvent.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionObservation.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationException.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationResult.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/AnthropicChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/ChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/LlmGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/OpenAiChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/SpringAiGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/security/SecurityConfiguration.java`
- `ai-service/src/test/java/com/example/aiassistant/TestProperties.java`
- `ai-service/src/test/java/com/example/aiassistant/commerce/CommerceApiTest.java`
- `ai-service/src/test/java/com/example/aiassistant/security/JwtValidationTest.java`
- `commerce-service/gradle/wrapper/gradle-wrapper.jar`
- `commerce-service/gradle/wrapper/gradle-wrapper.properties`
- `commerce-service/gradlew`
- `commerce-service/gradlew.bat`
- `commerce-service/settings.gradle`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/common/BadRequestException.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/common/NotFoundException.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/customer/CurrentCustomerService.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/customer/Customer.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/customer/CustomerRepository.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/order/CustomerOrder.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/order/OrderController.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/order/OrderItem.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/order/OrderItemResponse.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/order/OrderRepository.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/order/OrderResponse.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/order/OrderService.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/order/OrderStatus.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/payment/PaymentStatus.java`
- `commerce-service/src/main/java/com/example/aicommerce/commerce/security/SecurityConfig.java`

Relevant original document sections: Java API enforcement, Order details in chat, Validation, usage and errors. See [learning/source notes index](09-tool-authorization-and-safety/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Focused source subsets retain package/type names; local entry points replace integrated startup. Extracted only order/customer/payment-status entities and ownership API into commerce/, using H2 generated tables and optional synthetic demo seed instead of full checkout/storefront.

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. 

Missing capabilities/limits: JWT validation and permission enforcement in the AI API; forwarded bearer credentials and response projection; an independently runnable H2 order backend copied from commerce with customer ownership enforced behind HTTP. No browser identity journey.

## [10-embeddings-and-semantic-retrieval](10-embeddings-and-semantic-retrieval/README.md)

Classification: **partially extractable**. Local ONNX embeddings, safe Markdown preparation, cosine ranking and source/section retrieval metrics in memory. The source has no lexical-search comparison experiment, so that proposed portion remains missing.

Source material: 

- `ai-service/.env.example`
- `ai-service/evaluation/product-search-cases.json`
- `ai-service/evaluation/questions.json`
- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/knowledge/faqs/shopping.md`
- `ai-service/knowledge/policies/returns.md`
- `ai-service/knowledge/policies/shipping.md`
- `ai-service/knowledge/products/laptop-stand.md`
- `ai-service/knowledge/products/outdoor-selection.md`
- `ai-service/knowledge/products/usb-c-hub.md`
- `ai-service/knowledge/products/wireless-headphones.md`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/AiApplication.java`
- `ai-service/src/main/java/com/example/aiassistant/AiProperties.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/AiException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionTrace.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionEvent.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionObservation.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationException.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationResult.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeChunker.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeCommandRunner.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeDocument.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/MarkdownDocumentLoader.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/ModelTokenizer.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/RagConfiguration.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/RetrievalEvaluation.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/ValidatingEmbeddingModel.java`
- `ai-service/src/test/java/com/example/aiassistant/TestProperties.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgePreparationTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/RetrievalEvaluationTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/ValidatingEmbeddingModelTest.java`

Relevant original document sections: Sources, chunks and embeddings, Commands and evaluation, Verification, limits and learning. See [learning/source notes index](10-embeddings-and-semantic-retrieval/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Focused source subsets retain package/type names; local entry points replace integrated startup. Removed database index and catalog dependency from the in-memory retrieval path.

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. Real local embedding retrieval is optional and recorded separately. 

Missing capabilities/limits: Local ONNX embeddings, safe Markdown preparation, cosine ranking and source/section retrieval metrics in memory. The source has no lexical-search comparison experiment, so that proposed portion remains missing.

## [11-rag-ingestion](11-rag-ingestion/README.md)

Classification: **extractable**. Safe Markdown/catalog preparation, token-aware chunks, metadata, fingerprints and transactional PostgreSQL/pgvector replacement. No generation endpoint or LLM client is included.

Source material: 

- `ai-service/.env.example`
- `ai-service/evaluation/product-search-cases.json`
- `ai-service/evaluation/questions.json`
- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/knowledge/faqs/shopping.md`
- `ai-service/knowledge/policies/returns.md`
- `ai-service/knowledge/policies/shipping.md`
- `ai-service/knowledge/products/laptop-stand.md`
- `ai-service/knowledge/products/outdoor-selection.md`
- `ai-service/knowledge/products/usb-c-hub.md`
- `ai-service/knowledge/products/wireless-headphones.md`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/AiApplication.java`
- `ai-service/src/main/java/com/example/aiassistant/AiProperties.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/AiException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionTrace.java`
- `ai-service/src/main/java/com/example/aiassistant/commerce/CommerceApi.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionEvent.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionObservation.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationException.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationResult.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/IngestionService.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeChunker.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeCommandRunner.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeDocument.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeIndex.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeReadiness.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeService.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/MarkdownDocumentLoader.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/ModelTokenizer.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/RagConfiguration.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/RetrievalEvaluation.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/ValidatingEmbeddingModel.java`
- `ai-service/src/test/java/com/example/aiassistant/TestProperties.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgeCommandRunnerTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgeIndexTraceTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgePreparationTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/PostgresRagIT.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/RetrievalEvaluationTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/ValidatingEmbeddingModelTest.java`

Relevant original document sections: Sources, chunks and embeddings, Index updates, filtering and availability. See [learning/source notes index](11-rag-ingestion/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Focused source subsets retain package/type names; local entry points replace integrated startup. 

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. Disposable PostgreSQL/pgvector and local ONNX integration are opt-in; unit tests mock dependencies and do not replace integration evidence. 

Missing capabilities/limits: Safe Markdown/catalog preparation, token-aware chunks, metadata, fingerprints and transactional PostgreSQL/pgvector replacement. No generation endpoint or LLM client is included.

## [12-grounded-rag-answers](12-grounded-rag-answers/README.md)

Classification: **extractable**. Native knowledge-tool selection, evidence budgets, live public product checks, final generation and citation membership checks. Module 12 does not expose Learning-mode traces; no generated-answer quality score is claimed.

Source material: 

- `ai-service/.env.example`
- `ai-service/evaluation/product-search-cases.json`
- `ai-service/evaluation/questions.json`
- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/knowledge/faqs/shopping.md`
- `ai-service/knowledge/policies/returns.md`
- `ai-service/knowledge/policies/shipping.md`
- `ai-service/knowledge/products/laptop-stand.md`
- `ai-service/knowledge/products/outdoor-selection.md`
- `ai-service/knowledge/products/usb-c-hub.md`
- `ai-service/knowledge/products/wireless-headphones.md`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/AiApplication.java`
- `ai-service/src/main/java/com/example/aiassistant/AiProperties.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/AiException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ApiExceptionHandler.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionTrace.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatOrchestrator.java`
- `ai-service/src/main/java/com/example/aiassistant/commerce/CommerceApi.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionEvent.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionObservation.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationException.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationResult.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/AnthropicChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/ChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/LlmGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/OpenAiChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/SpringAiGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/IngestionService.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeChunker.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeCommandRunner.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeDocument.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeIndex.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeReadiness.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeService.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/MarkdownDocumentLoader.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/ModelTokenizer.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/RagConfiguration.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/RetrievalEvaluation.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/ValidatingEmbeddingModel.java`
- `ai-service/src/main/java/com/example/aiassistant/tools/ToolDefinitions.java`
- `ai-service/src/main/java/com/example/aiassistant/tools/ToolService.java`
- `ai-service/src/main/resources/system-prompt.txt`
- `ai-service/src/test/java/com/example/aiassistant/TestProperties.java`
- `ai-service/src/test/java/com/example/aiassistant/chat/ApiExceptionHandlerTest.java`
- `ai-service/src/test/java/com/example/aiassistant/chat/ChatDtosTest.java`
- `ai-service/src/test/java/com/example/aiassistant/chat/ChatOrchestratorTest.java`
- `ai-service/src/test/java/com/example/aiassistant/commerce/CommerceApiTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgeCommandRunnerTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgeIndexTraceTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgePreparationTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgeServiceTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/PostgresRagIT.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/RetrievalEvaluationTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/ValidatingEmbeddingModelTest.java`
- `ai-service/src/test/java/com/example/aiassistant/tools/ToolValidationTest.java`

Relevant original document sections: Live data, grounding and citations, Model-selected knowledge and commerce tools — 2026-10-17. See [learning/source notes index](12-grounded-rag-answers/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Focused source subsets retain package/type names; local entry points replace integrated startup. Removed browser authentication from the teaching endpoint and use only public product/catalog data. Module 12 keeps trace events internal; module 13 exposes opt-in traces and adds the isolated source trace viewer.

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. Disposable PostgreSQL/pgvector and local ONNX integration are opt-in; unit tests mock dependencies and do not replace integration evidence. 

Missing capabilities/limits: Native knowledge-tool selection, evidence budgets, live public product checks, final generation and citation membership checks. Module 12 does not expose Learning-mode traces; no generated-answer quality score is claimed.

## [13-execution-visibility](13-execution-visibility/README.md)

Classification: **partially extractable**. Request-local safe execution events, completion ordering, durations and token counts; an isolated React trace viewer copied from the storefront. Cost estimation was not implemented in the source.

Source material: 

- `ai-service/.env.example`
- `ai-service/evaluation/product-search-cases.json`
- `ai-service/evaluation/questions.json`
- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/knowledge/faqs/shopping.md`
- `ai-service/knowledge/policies/returns.md`
- `ai-service/knowledge/policies/shipping.md`
- `ai-service/knowledge/products/laptop-stand.md`
- `ai-service/knowledge/products/outdoor-selection.md`
- `ai-service/knowledge/products/usb-c-hub.md`
- `ai-service/knowledge/products/wireless-headphones.md`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/AiApplication.java`
- `ai-service/src/main/java/com/example/aiassistant/AiProperties.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/AiException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ApiExceptionHandler.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionTrace.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatOrchestrator.java`
- `ai-service/src/main/java/com/example/aiassistant/commerce/CommerceApi.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionEvent.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionObservation.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationException.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationResult.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/AnthropicChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/ChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/LlmGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/OpenAiChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/SpringAiGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/IngestionService.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeChunker.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeCommandRunner.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeDocument.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeIndex.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeReadiness.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/KnowledgeService.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/MarkdownDocumentLoader.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/ModelTokenizer.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/RagConfiguration.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/RetrievalEvaluation.java`
- `ai-service/src/main/java/com/example/aiassistant/rag/ValidatingEmbeddingModel.java`
- `ai-service/src/main/java/com/example/aiassistant/tools/ToolDefinitions.java`
- `ai-service/src/main/java/com/example/aiassistant/tools/ToolService.java`
- `ai-service/src/main/resources/system-prompt.txt`
- `ai-service/src/test/java/com/example/aiassistant/TestProperties.java`
- `ai-service/src/test/java/com/example/aiassistant/chat/ApiExceptionHandlerTest.java`
- `ai-service/src/test/java/com/example/aiassistant/chat/ChatDtosTest.java`
- `ai-service/src/test/java/com/example/aiassistant/chat/ChatOrchestratorTest.java`
- `ai-service/src/test/java/com/example/aiassistant/commerce/CommerceApiTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgeCommandRunnerTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgeIndexTraceTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgePreparationTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/KnowledgeServiceTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/PostgresRagIT.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/RetrievalEvaluationTest.java`
- `ai-service/src/test/java/com/example/aiassistant/rag/ValidatingEmbeddingModelTest.java`
- `ai-service/src/test/java/com/example/aiassistant/tools/ToolValidationTest.java`
- `frontend/src/components/ChatExecutionTraceView.test.tsx`
- `frontend/src/components/ChatExecutionTraceView.tsx`

Relevant original document sections: Execution visibility discussion — 2026-10-17, Context propagation design review — 2026-10-17. See [learning/source notes index](13-execution-visibility/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Focused source subsets retain package/type names; local entry points replace integrated startup. Removed browser authentication from the teaching endpoint and use only public product/catalog data. Module 12 keeps trace events internal; module 13 exposes opt-in traces and adds the isolated source trace viewer.

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. Node/npm component tests, type check and Vite production build. Disposable PostgreSQL/pgvector and local ONNX integration are opt-in; unit tests mock dependencies and do not replace integration evidence. 

Missing capabilities/limits: Request-local safe execution events, completion ordering, durations and token counts; an isolated React trace viewer copied from the storefront. Cost estimation was not implemented in the source.

## [14-provider-comparison](14-provider-comparison/README.md)

Classification: **partially extractable**. Explicit provider aliases, isolated protocol adapters and fixture tests of model/options/credential boundaries. The source has no same-dataset comparison experiment or measured quality/cost table.

Source material: 

- `ai-service/.env.example`
- `ai-service/gradle/wrapper/gradle-wrapper.jar`
- `ai-service/gradle/wrapper/gradle-wrapper.properties`
- `ai-service/gradlew`
- `ai-service/gradlew.bat`
- `ai-service/settings.gradle`
- `ai-service/src/main/java/com/example/aiassistant/AiApplication.java`
- `ai-service/src/main/java/com/example/aiassistant/AiProperties.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/AiException.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatDtos.java`
- `ai-service/src/main/java/com/example/aiassistant/chat/ChatExecutionTrace.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionEvent.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ExecutionObservation.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationException.java`
- `ai-service/src/main/java/com/example/aiassistant/execution/ObservedOperationResult.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/AnthropicChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/ChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/LlmGateway.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/OpenAiChatProviderAdapter.java`
- `ai-service/src/main/java/com/example/aiassistant/llm/SpringAiGateway.java`
- `ai-service/src/test/java/com/example/aiassistant/TestProperties.java`
- `ai-service/src/test/java/com/example/aiassistant/llm/CompatibleProvidersTest.java`
- `ai-service/src/test/java/com/example/aiassistant/llm/SpringAiGatewayTest.java`

Relevant original document sections: Runtime provider selection and extension. See [learning/source notes index](14-provider-comparison/docs/learning/README.md).

Adaptation: Independent wrapper/build; local configuration; source-specific absolute paths and real secrets excluded. Focused source subsets retain package/type names; local entry points replace integrated startup. 

Dependencies and verification: Java build, relevant JUnit/loopback fixtures, packaging, pinned formatter and documentation-link/runtime-boundary checks. 

Missing capabilities/limits: Explicit provider aliases, isolated protocol adapters and fixture tests of model/options/credential boundaries. The source has no same-dataset comparison experiment or measured quality/cost table.

## [15-deterministic-model-routing](15-deterministic-model-routing/)

Status: **Planned — empty; unchecked**. The folder intentionally contains no code, documents, references, tests or placeholder files, following the user’s instruction. It is excluded from runnable verification.

Source capability assessment: Proposed deterministic routing by logical tiers with a strongest-model baseline. No implemented router was found; configured provider aliases are explicit selection, not routing.

Future work will be added as the curriculum progresses. No implementation or results are claimed for this chapter.

## [16-context-and-cost-optimization](16-context-and-cost-optimization/)

Status: **Planned — empty; unchecked**. The folder intentionally contains no code, documents, references, tests or placeholder files, following the user’s instruction. It is excluded from runnable verification.

Source capability assessment: The existing browser history window and character bounds, preserved as source reference. No measured context or cost optimization experiment exists in ai-commerce. Conversation handling is runnable in module 04.

Future work will be added as the curriculum progresses. No implementation or results are claimed for this chapter.

## Dependency adaptations

Java 25, Spring Boot 4.1.1, Spring AI BOM 2.0.1, dependency-management plugin 1.1.7, AI Gradle 9.6.0, commerce Gradle 9.5.1 and google-java-format 1.35.0 are copied from the source. Early modules retain only Boot core/web, validation as needed and the single OpenAI protocol dependency. No early PostgreSQL, Keycloak, tool service or RAG dependency is introduced. Retrieval/ingestion use real source local-embedding components; module 11 owns its disposable database. Module 09’s H2 backend excludes checkout, products, payment execution and administration. No cross-module source-code imports exist. Shared scripts provide verification and a clearly synthetic HTTP fixture, not an AI framework.

## Authorized presentation adaptation — standalone chatbot UIs, 2026-10-20

The user explicitly requested a minimal independent chatbot interface for each existing implemented lesson. This authorizes new presentation and HTTP transport adapters around existing capabilities; it does not authorize implementation of missing AI capabilities. Future module folders remain empty.

Each implemented module owns `src/main/resources/static/{index.html,chat.css,chat.js,lesson-config.js,favicon.svg}`, `ui-tests/chat.test.cjs`, `run.sh`, and `ModuleUiTest`. Java serves the UI and its local endpoints on the same origin. Small copies keep lessons independent; no generic shared AI/frontend framework was introduced. The browser UI and tests are newly authored, not represented as code copied unchanged from ai-commerce. Source flow inspiration remains the existing ChatWidget/history, source cards and execution facts.

Module 01’s FirstCall and module 14’s explicit-provider gateway now have thin `/chat` adapters. Their CLI runners require explicit `lesson.cli=true`, so opening the app never automatically calls a model. Module 10 exposes the existing in-memory cosine path for search; the query validator is adapted from KnowledgeIndex without bringing in PostgreSQL. Embedding/tokenizer initialization is lazy. Module 11 exposes existing chunk/ingest/search operations; 12–13 expose the existing ingestion preparation as optional controls. None of these introduces generated-answer evaluation, measured routing, cost estimation or any absent capability.

Module 09 permits only its page/static resources publicly and retains JWT verification and USERS_WRITE on chat. Its browser-supplied access token stays in page memory and is forwarded only as an HTTP credential, never in model history. Other teaching endpoints bind loopback. Module-local product fixtures in 07/08 are synthetic HTTP test data; business rules are not transferred into model prompts.

Every implemented module passed its independently invoked Node checks and Java suite, real Spring HTTP serving tests, jar packaging and static-asset inspection in the final aggregate. Real provider decisions, semantic answer quality, ONNX inference and PostgreSQL integration remain separate unverified evidence. See VERIFICATION.md for exact commands and counts.

## Java-only verification and focused class cleanup — 2026-10-20

Explicit user clarification: no Python code in this project, explicit Java types without `var`, and small classes with separate responsibilities. Removed all project Python verification utilities and synthetic fixture programs. Replaced them with `tools/src/main/java/learning/verification/` classes, root `verify.sh`, and independent Java commerce fixtures with `commerce-fixture.sh` launchers. This is tooling/structure work around existing capabilities, not new AI behavior.

New Java utilities handle links, empty module policy, source boundaries, package inspection, process execution and JUnit reporting in separate classes, without third-party dependencies. Module 07/08 each own their fixture source and have no cross-module runtime dependency. Shell remains only for startup and the pinned formatter; browser JavaScript remains appropriate for the minimal UI.

Focused application refactoring: 01 separates BasicChatService, request/reply types, error advice and QuestionCommand from bootstrap/controller; 04 separates conversation transport/service/types from startup; 10 separates SemanticSearch, query validation and RetrievalCommand; 10/11 separate knowledge transport, typed source projection and errors; 11–13 separate ingestion HTTP/service/types; 14 separates provider transport/service/types/error advice. Existing model, history, retrieval and commerce boundaries are preserved. Original source-note copies remain unchanged.

Current independent verification entry point is `./verify.sh`. See [the Java utility index](../tools/README.md) and [current observed Java verification](JAVA_VERIFICATION.md).

## IDE/Make startup and controller-based operations — 2026-10-20

The user requested IDE/Spring-controller use or a consistent Make interface, instead of application CLI operation. Each implemented module now has identical Make targets (start/restart/stop/status/build/test) and independent Java lifecycle helpers under support/lifecycle. They retain only module-owned PID/start-time/command-identity state, reject port collisions and never stop unrelated/IDE processes. Module 07/08 fixtures and the module 09 H2 backend are managed automatically; external RAG/identity/provider prerequisites remain explicit.

QuestionCommand, ProviderCommand, RetrievalCommand and KnowledgeCommandRunner are removed. Existing evaluation logic is adapted into KnowledgeEvaluationService and KnowledgeEvaluationController, preserving retrieval-only metrics and tests. The UI exposes evaluation next to corpus preparation. Bootstrap classes are plain Spring application entry points, with no CLI dispatch. This changes presentation/startup only, not the implemented AI capabilities. Historical source notes remain unchanged.
