import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ChatExecutionTraceView from './ChatExecutionTraceView';

test('retrieval trace preserves zero results and separates recorded events from flow explanations', async () => {
  const user = userEvent.setup();
  render(<ChatExecutionTraceView steps={[
    { stage: 'retrieval_outcome', status: 'empty', tool: 'search_knowledge', duration_ms: 0, result_count: 0, source_ids: [] },
    { stage: 'evidence_selection', status: 'success', tool: 'search_knowledge', duration_ms: null, result_count: 1, source_ids: ['policy:0'] },
  ]} />);
  await user.click(screen.getByText('How this answer was produced'));
  expect(screen.getByText('No usable evidence')).toBeVisible();
  expect(screen.getByText('0 ms · 0 sources')).toBeVisible();
  expect(screen.getByText('Source IDs: policy:0')).toBeVisible();
  await user.click(screen.getAllByRole('link', { name: 'View flow' })[0]);
  expect(screen.getByRole('region', { name: 'Flow explanation' })).toHaveTextContent('Empty means no usable evidence remained.');
  expect(screen.queryByText('Convert the question into a search vector')).not.toBeInTheDocument();
});

test('names the two model calls by their distinct responsibilities', async () => {
  const user = userEvent.setup();
  render(<ChatExecutionTraceView steps={[
    { stage: 'initial_model', status: 'success', tool: null, duration_ms: 10, result_count: null, source_ids: [] },
    { stage: 'tool_validation', status: 'success', tool: null, duration_ms: 1, result_count: 1, source_ids: [] },
    { stage: 'tool_selection', status: 'validated', tool: 'search_knowledge', duration_ms: null, result_count: null, source_ids: [] },
    { stage: 'final_model', status: 'success', tool: null, duration_ms: 20, result_count: null, source_ids: [] },
  ]} />);
  await user.click(screen.getByText('How this answer was produced'));

  expect(screen.getByText('Ask the model which store tools to use')).toBeVisible();
  expect(screen.getByText('Ask the model to write the grounded answer')).toBeVisible();
  expect(screen.getByText(/1 tool call/)).toBeVisible();
});
