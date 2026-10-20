/* Independent teaching UI. No framework, cross-module import or browser-stored credentials. */
(function (global) {
  'use strict';
  function errorMessage(status) {
    if (status === 401) return 'Please supply a valid access token for this lesson.';
    if (status === 403) return 'This account does not have permission for that action.';
    if (status === 422 || status === 400) return 'Check your message and try again.';
    if (status === 429) return 'The provider is busy. Please try again later.';
    return 'The request could not finish. Check the local service configuration and try again.';
  }
  class ChatSession {
    constructor(config, request) { this.config=config; this.request=request; this.history=[]; this.pending=false; }
    reset() { if (this.pending) return false; this.history=[]; return true; }
    async send(text, options={}) {
      text=Array.from(String(text).trim()).slice(0,4000).join('');
      if (!text || this.pending) return null;
      this.pending=true;
      const user={role:'user',content:text};
      const body={messages:this.config.history && !options.action ? [...this.history.slice(-29),user] : [user]};
      if (this.config.learning) body.learning_mode=Boolean(options.learning);
      if (this.config.providers && options.provider) body.provider=options.provider;
      if (options.action) body.action=options.action;
      const headers={'Content-Type':'application/json'};
      if (this.config.auth && options.token) headers.Authorization='Bearer '+options.token.trim();
      try {
        const response=await this.request(options.action === 'evaluate' ? '/lesson/evaluation' : options.action ? '/lesson/ingestion' : '/chat',{method:'POST',headers,body:JSON.stringify(body)});
        if (!response.ok) {
          const failure=new Error(errorMessage(response.status));
          if(this.config.learning) { try { failure.trace=(await response.json()).execution_trace; } catch {} }
          throw failure;
        }
        const data=await response.json();
        if (!data || typeof data.reply!=='string' || !data.reply.trim()) throw new Error('The service returned no answer. Please try again.');
        if(!options.action) this.history.push(user,{role:'assistant',content:Array.from(data.reply).slice(0,4000).join('')});
        return data;
      } catch (error) {
        if (error instanceof Error && (error.message.startsWith('The service returned') || error.message===errorMessage(0) || [400,401,403,422,429].some(status=>error.message===errorMessage(status)))) throw error;
        throw new Error('Could not reach the local lesson service. Check that it is running.');
      } finally { this.pending=false; }
    }
  }
  function mount(document, request, config) {
    const find=id=>document.getElementById(id);
    const session=new ChatSession(config,request);
    const list=find('messages'),form=find('composer'),input=find('question'),send=find('send'),status=find('status'),reset=find('reset');
    find('module-number').textContent='MODULE '+config.number;
    find('lesson-title').textContent=config.title;
    find('lesson-description').textContent=config.description;
    find('welcome-text').textContent=config.welcome;
    find('example').textContent=config.example;
    find('scope').textContent=config.scope;
    document.title=config.title+' · Engineering learning';
    const controls=find('controls');
    controls.hidden=!(config.auth || config.providers || config.learning);
    for (const [key,id] of [['auth','token-label'],['providers','provider-label'],['learning','learning-label']]) find(id).hidden=!config[key];
    find('actions').hidden=!(config.ingestion || config.evaluation);
    find('preview').hidden=!config.ingestion;find('ingest').hidden=!config.ingestion;find('evaluate').hidden=!config.evaluation;
    function paragraph(parent,text,className) { const node=document.createElement('p');node.className=className||'';node.textContent=String(text);parent.appendChild(node);return node; }
    function bubble(role,text,failed=false) {
      find('welcome').hidden=true;
      const node=document.createElement('article');node.className='message '+role+(failed?' error':'');
      paragraph(node,role==='user'?'You':'Lesson assistant','speaker');
      paragraph(node,text,'message-text');list.appendChild(node);return node;
    }
    function details(parent,data) {
      if(data.evaluation) {
        const report=document.createElement('details'),summary=document.createElement('summary');summary.textContent='Retrieval scores';report.appendChild(summary);
        paragraph(report,JSON.stringify(data.evaluation,null,2),'message-text');parent.appendChild(report);
      }
      if (Array.isArray(data.sources)) for (const source of data.sources) {
        const card=document.createElement('div');card.className='source';
        const title=document.createElement('strong');title.textContent=source.title||'Source';card.appendChild(title);
        paragraph(card,[source.section,source.citation_id].filter(Boolean).join(' · '));
        paragraph(card,source.excerpt||'');if(typeof source.score==='number') paragraph(card,'Similarity '+source.score.toFixed(3));parent.appendChild(card);
      }
      if (Array.isArray(data.products)) for (const product of data.products) paragraph(parent,product.name+' · '+(product.price_eur||'')+' '+(product.currency||''),'source');
      if (config.learning && Array.isArray(data.execution_trace) && data.execution_trace.length) {
        const disclosure=document.createElement('details'),summary=document.createElement('summary');summary.textContent=data.failed?'How this request failed':'How this answer was produced';disclosure.appendChild(summary);
        paragraph(disclosure,'Recorded in completion order. Nested durations overlap.');
        const steps=document.createElement('ol');
        for (const step of data.execution_trace) { const item=document.createElement('li');item.textContent=[step.stage,step.status,step.tool,step.duration_ms!=null?step.duration_ms+' ms':null,step.result_count!=null?step.result_count+' results':null].filter(value=>value!=null).join(' · ');steps.appendChild(item); }
        disclosure.appendChild(steps);parent.appendChild(disclosure);
      }
    }
    async function submit(text,action) {
      if (!String(text).trim() || session.pending) return;
      if(config.providers && !find('provider').value) { status.textContent='Choose a configured provider first.';return; }
      bubble('user',text);input.value='';send.disabled=true;reset.disabled=true;input.disabled=true;find('preview').disabled=true;find('ingest').disabled=true;find('evaluate').disabled=true;status.textContent='Working…';
      try {
        const data=await session.send(text,{action,learning:find('learning-mode').checked,provider:find('provider').value,token:find('access-token').value});
        if (data) { const reply=bubble('assistant',data.reply);details(reply,data); }
        status.textContent='';
      } catch(error) { const failure=bubble('assistant',error.message,true);details(failure,{failed:true,execution_trace:error.trace});status.textContent='Request failed. Failed turns will not be added to later requests.'; }
      finally { send.disabled=false;reset.disabled=false;input.disabled=false;find('preview').disabled=false;find('ingest').disabled=false;find('evaluate').disabled=false;list.scrollTop=list.scrollHeight;input.focus(); }
    }
    form.addEventListener('submit',event=>{event.preventDefault();submit(input.value);});
    input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();submit(input.value);}});
    find('example').addEventListener('click',()=>{input.value=config.example;input.focus();});
    reset.addEventListener('click',()=>{if(!session.reset())return;for(const node of Array.from(list.children)){if(node.id!=='welcome')node.remove();}find('welcome').hidden=false;status.textContent='';input.value='';input.focus();});
    find('preview').addEventListener('click',()=>submit('Preview the local demo corpus.','chunk'));
    find('ingest').addEventListener('click',()=>submit('Rebuild the disposable demo index.','ingest'));
    find('evaluate').addEventListener('click',()=>submit('Evaluate the demo retrieval dataset.','evaluate'));
    if(config.providers) {
      send.disabled=true;status.textContent='Loading configured providers…';
      request('/lesson/providers').then(response=>{if(!response.ok)throw Error();return response.json();}).then(providers=>{
        if(!Array.isArray(providers)||!providers.length)throw Error();
        for(const alias of providers){const option=document.createElement('option');option.value=String(alias);option.textContent=String(alias);find('provider').appendChild(option);}
        status.textContent='';send.disabled=false;
      }).catch(()=>{status.textContent='Could not load providers. Restart this page after checking the local service.';});
    }
    return {session,submit};
  }
  if(typeof module!=='undefined'&&module.exports) module.exports={ChatSession,mount,errorMessage};
  else { global.LessonChat={ChatSession,mount,errorMessage};mount(global.document,global.fetch.bind(global),global.LESSON); }
})(typeof window!=='undefined'?window:globalThis);
