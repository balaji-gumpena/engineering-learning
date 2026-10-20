import React from 'react';
import { createRoot } from 'react-dom/client';
import ChatExecutionTraceView from './components/ChatExecutionTraceView';
import type { ChatExecutionStep } from './api/client';
function App() {
 const [text,setText]=React.useState('[]');
 const [steps,setSteps]=React.useState<ChatExecutionStep[]>([]);
 const [error,setError]=React.useState('');
 return <main><h1>Inspect a Learning-mode execution trace</h1><p>Paste the execution_trace array from a module 13 response. This viewer makes no model calls.</p><textarea aria-label="Execution trace JSON" value={text} onChange={event=>setText(event.target.value)} /><button onClick={()=>{try{const parsed=JSON.parse(text);if(!Array.isArray(parsed)||parsed.some(s=>typeof s.stage!=='string'||typeof s.status!=='string'||!Array.isArray(s.source_ids)))throw Error();setSteps(parsed);setError('');}catch{setError('Supply an array of execution steps.');}}}>Inspect</button><p role="status">{error}</p><ChatExecutionTraceView steps={steps}/></main>;
}
createRoot(document.getElementById('root')!).render(<App/>);
