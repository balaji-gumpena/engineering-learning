import { useEffect, useId, useRef, useState } from 'react';
import type { ChatExecutionStep } from '../api/client';

type FlowExplanation = { title: string; path: string; description: string };

const flows: Record<string, FlowExplanation> = {
  tool_validation: {
    title: 'Validate the model’s requested tools', path: 'Model tool requests → check names, arguments and limits → accept or reject the batch',
    description: 'Before anything runs, the server validates every requested tool and argument. One invalid request rejects the whole batch. The count is the number of validated tool calls.',
  },
  query_embedding: {
    title: 'Convert the question into a search vector', path: 'Knowledge question → local embedding model → 384-dimensional query vector',
    description: 'The local MiniLM model turns the question into a validated 384-dimensional vector. This represents semantic similarity; it is not the final answer and does not train the model.',
  },
  passage_retrieval: {
    title: 'Search the knowledge passages', path: 'Query vector → PostgreSQL cosine search → ranked matching passages',
    description: 'The AI knowledge index checks its fingerprint and retrieves passages using configured filters, similarity threshold and result limit. The count is the raw retrieval count, before evidence filtering.',
  },
  live_product_validation: {
    title: 'Verify referenced products are still available', path: 'Product-linked passages → public commerce catalog → current products',
    description: 'This step runs only when passages refer to products. It loads the public catalog so removed or inactive products can be excluded. The count is catalog entries loaded, not accepted passages. If this lookup fails, policy evidence can still be used.',
  },
  evidence_selection: {
    title: 'Keep the passages the answer may use', path: 'Retrieved passages → live-product checks and context budget → retained evidence',
    description: 'Only whole passages that fit the evidence budget and product checks are retained. The listed source IDs identify retained evidence, which may exceed the sources cited by the final answer.',
  },
  retrieval_outcome: {
    title: 'Confirm that usable knowledge was found', path: 'Knowledge lookup → retained evidence or explicit fallback',
    description: 'Success means evidence was available, not that every answer claim is proven. Empty means no usable evidence remained. Unavailable means knowledge could not be used. Empty or unavailable retrieval stops final generation.',
  },
  final_model: {
    title: 'Ask the model to write the grounded answer', path: 'Conversation + completed tool results → configured model → shopper-facing answer',
    description: 'This is a second model call on tool-assisted requests. It receives the tool results and writes the shopper-facing response with further tool use disabled. There is no agent loop or automatic retry.',
  },
  citation_validation: {
    title: 'Verify the answer’s source references', path: 'Citations in the generated answer → compare with retained source IDs → show or withhold the answer',
    description: 'Every citation in a knowledge answer must refer to retained evidence. Missing or unknown references cause the draft to be withheld. This verifies source membership, not whether every sentence is fully supported.',
  },
  final_answer: {
    title: 'Reject unexpected follow-up tool requests', path: 'Second model result → require a final answer rather than another tool request',
    description: 'The final model call must return an answer. Additional tool requests fail rather than starting another execution loop.',
  },
  request: {
    title: 'Return a safe request failure', path: 'Chat execution → sanitized error response',
    description: 'The request failed. The trace includes only stages actually recorded before the failure; private payloads and raw exception messages are excluded.',
  },
};

const knowledgeToolFlow: FlowExplanation = {
  title: 'Complete the policy knowledge lookup', path: 'search_knowledge → embed question → retrieve passages → keep usable evidence → return tool result',
  description: 'This is the enclosing search_knowledge operation. It completes after the nested embedding, retrieval and evidence-selection steps, so its duration includes them. Document ingestion and index creation are a separate preparation flow.',
};

const statuses: Record<string, string> = {
  success: 'Completed', failed: 'Failed', validated: 'Validated',
  rejected: 'Rejected', none: 'No tool needed',
  empty: 'No usable evidence', unavailable: 'Unavailable',
};

function initialModelExplanation(steps: ChatExecutionStep[]): FlowExplanation {
  const selection = steps.find((step) => step.stage === 'tool_selection');
  if (selection?.status === 'validated') return {
    title: 'Ask the model which store tools to use',
    path: 'Conversation + permitted tool definitions → configured model → requested tool calls',
    description: 'This first model call does not write the final shopper response. It examines the conversation and requests one bounded batch of permitted tools. The server validates those requests before execution.',
  };
  if (selection?.status === 'none') return {
    title: 'Ask the model for a direct answer',
    path: 'Conversation + permitted tool definitions → configured model → shopper-facing answer',
    description: 'The model answered without requesting a store tool, so this is the only model call for the request.',
  };
  return {
    title: 'Ask the model whether store tools are needed',
    path: 'Conversation + permitted tool definitions → configured model → answer or requested tool calls',
    description: 'The first model call may answer directly or request one bounded batch of permitted tools. The application does not use language-specific keyword rules to override this decision.',
  };
}

function toolSelectionExplanation(step: ChatExecutionStep): FlowExplanation {
  if (step.status === 'validated') return {
    title: `Accept the ${step.tool ?? 'requested'} tool call`,
    path: 'Validated model request → permitted tool call → execution',
    description: 'The model requested this tool and the complete batch passed server validation. A later execution step confirms that the tool finished.',
  };
  if (step.status === 'none') return {
    title: 'Continue without a store tool',
    path: 'Model response → no tool requested → use the direct answer',
    description: 'The model returned a shopper-facing answer directly, so no commerce or knowledge tool ran.',
  };
  return {
    title: 'Reject the invalid tool requests',
    path: 'Model tool requests → batch validation failed → safe fallback',
    description: 'The requested batch could not be validated. No tool from an invalid batch is executed.',
  };
}

const commerceToolTitles: Record<string, string> = {
  search_products: 'Search the current product catalog', get_product: 'Load current product details',
  get_cart: 'Load the customer’s current cart', get_order: 'Load the requested order',
  get_orders: 'Load the customer’s recent orders',
};

function explanation(step: ChatExecutionStep, steps: ChatExecutionStep[]): FlowExplanation {
  if (step.stage === 'initial_model') return initialModelExplanation(steps);
  if (step.stage === 'tool_selection') return toolSelectionExplanation(step);
  if (step.stage === 'tool_execution' && step.tool === 'search_knowledge') return knowledgeToolFlow;
  if (step.stage === 'tool_execution') return {
    title: commerceToolTitles[step.tool ?? ''] ?? 'Run the validated commerce tool',
    path: 'Validated tool → authenticated commerce HTTP API → current result',
    description: 'The AI service calls the Java commerce backend through HTTP. Commerce enforces permissions and customer ownership; the AI service does not query the commerce database directly.',
  };
  return flows[step.stage] ?? { title: 'Execution step', path: 'Recorded server operation',
    description: 'This is an execution event recorded by the AI service.' };
}

function resultLabel(step: ChatExecutionStep): string | null {
  if (step.result_count == null) return null;
  const noun = step.stage === 'tool_validation' ? 'tool call'
    : step.stage === 'passage_retrieval' ? 'passage'
      : step.stage === 'citation_validation' ? 'citation'
        : ['evidence_selection', 'retrieval_outcome'].includes(step.stage) ? 'source'
          : step.stage === 'live_product_validation' ? 'catalog product' : 'result';
  return `${step.result_count} ${noun}${step.result_count === 1 ? '' : 's'}`;
}

export default function ChatExecutionTraceView({ steps, failed = false }: { steps: ChatExecutionStep[]; failed?: boolean }) {
  const [selectedStep, setSelectedStep] = useState<number | null>(null);
  const flowId = useId();
  const flowRef = useRef<HTMLElement>(null);
  useEffect(() => { if (selectedStep !== null) flowRef.current?.focus(); }, [selectedStep]);
  const selected = selectedStep === null ? null : explanation(steps[selectedStep], steps);

  if (steps.length === 0) return <p className="chat-trace-note">Execution details were not recorded. Enable Learning mode before sending a question.</p>;

  return (
    <details className="chat-execution-trace">
      <summary>{failed ? 'How this request failed' : 'How this answer was produced'}</summary>
      <p className="chat-trace-note">Recorded execution facts, in completion order. Nested durations overlap; do not add them together.</p>
      <ol>
        {steps.map((step, index) => (
          <li key={`${step.stage}-${index}`}>
            <strong>{explanation(step, steps).title}</strong>
            <span className={`chat-trace-status ${step.status}`}>{statuses[step.status] ?? step.status}</span>
            {step.tool && <code>{step.tool}</code>}
            <small>{[
              step.duration_ms != null ? `${step.duration_ms} ms` : null,
              resultLabel(step),
            ].filter(Boolean).join(' · ')}</small>
            {step.source_ids?.length > 0 && <small>Source IDs: {step.source_ids.join(', ')}</small>}
            <a href={`#${flowId}`} onClick={(event) => { event.preventDefault(); setSelectedStep(index); }}>View flow</a>
          </li>
        ))}
      </ol>
      {selected && <section id={flowId} ref={flowRef} tabIndex={-1} className="chat-flow-explanation" aria-label="Flow explanation">
        <strong>{selected.title}</strong>
        <p className="chat-flow-path">{selected.path}</p>
        <p>{selected.description}</p>
        <small>This explains the flow. The recorded steps above describe this request.</small>
      </section>}
    </details>
  );
}
