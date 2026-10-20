export interface ChatExecutionStep {
  stage: string;
  status: string;
  tool: string | null;
  duration_ms: number | null;
  result_count: number | null;
  source_ids: string[];
}
