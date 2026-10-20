const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
const assets = path.join(root, 'src/main/resources/static');
const { ChatSession, mount } = require(path.join(assets, 'chat.js'));
const context = { window: {} };
vm.runInNewContext(fs.readFileSync(path.join(assets,'lesson-config.js'),'utf8'),context);
const config = context.window.LESSON;
const ok = data => ({ok:true,status:200,json:async()=>data});
const reply = {reply:'Fixture answer'};

test('the lesson owns all UI assets and no framework or cross-module script is loaded', () => {
  const html=fs.readFileSync(path.join(assets,'index.html'),'utf8');
  for(const resource of ['chat.css','chat.js','lesson-config.js','favicon.svg']) assert.ok(fs.existsSync(path.join(assets,resource)));
  assert.match(html,/aria-label="Conversation"/);
  assert.match(html,/aria-label="Send message"/);
  assert.doesNotMatch(html,/<script[^>]+(?:https?:|\.\.\/)/);
  assert.doesNotMatch(fs.readFileSync(path.join(assets,'chat.js'),'utf8'),/innerHTML|localStorage|sessionStorage/);
});

test('requests follow this lesson’s actual single-turn or history contract', async () => {
  const requests=[];const session=new ChatSession(config,async(url,options)=>{requests.push({url,...options});return ok(reply);});
  const options={provider:'fixture',learning:true,token:'synthetic-token'};
  await session.send(' Hello ',options);await session.send('Follow up',options);
  assert.equal(requests[0].url,'/chat');
  const first=JSON.parse(requests[0].body),second=JSON.parse(requests[1].body);
  assert.deepEqual(first.messages,[{role:'user',content:'Hello'}]);
  assert.equal(second.messages.length,config.history?3:1);
  if(config.history)assert.equal(second.messages[1].content,'Fixture answer');
  assert.equal('provider' in first,config.providers);
  assert.equal('learning_mode' in first,config.learning);
  assert.equal('Authorization' in requests[0].headers,config.auth);
  assert.ok(!requests[0].body.includes('synthetic-token'));
});

test('a failed request does not poison follow-up history or disclose server error bodies', async () => {
  let fail=true;const session=new ChatSession(config,async()=>fail?{ok:false,status:503,json:async()=>({detail:'private-upstream-payload'})}:ok(reply));
  await assert.rejects(session.send('Failed question'),error=>!error.message.includes('private-upstream-payload'));
  assert.equal(session.history.length,0);assert.equal(session.pending,false);
  fail=false;await session.send('Retry');assert.equal(session.history.length,2);
});

test('concurrent submits are ignored, reset is guarded, and the next turn stays bounded', async () => {
  let release;const session=new ChatSession(config,()=>new Promise(resolve=>{release=resolve;}));
  const pending=session.send('First');assert.equal(await session.send('Duplicate'),null);assert.equal(session.reset(),false);
  release(ok(reply));await pending;assert.equal(session.reset(),true);assert.equal(session.history.length,0);
  const requests=[];session.request=async(url,options)=>{requests.push(JSON.parse(options.body));return ok({reply:'😀'.repeat(4100)});};
  session.history=Array.from({length:60},(_,i)=>({role:i%2?'assistant':'user',content:'Earlier'}));
  await session.send('😀'.repeat(4100));
  assert.equal(Array.from(requests[0].messages.at(-1).content).length,4000);
  assert.ok(requests[0].messages.length<=30);
  assert.equal(Array.from(session.history.at(-1).content).length,4000);
});

class Element {
  constructor(id=''){this.id=id;this.children=[];this.listeners={};this.value='';this.checked=true;this.hidden=false;this.disabled=false;this.textContent='';}
  appendChild(child){child.parent=this;this.children.push(child);return child;}
  addEventListener(name,listener){this.listeners[name]=listener;}
  remove(){if(this.parent)this.parent.children=this.parent.children.filter(child=>child!==this);}
  focus(){this.focused=true;}
}
function documentFixture(){const elements=new Map();return {title:'',getElementById(id){if(!elements.has(id))elements.set(id,new Element(id));return elements.get(id);},createElement(){return new Element();}};}
function texts(node){return [node.textContent,...node.children.flatMap(texts)];}

test('actual view renders user text safely, sources and recorded traces, and resets', async () => {
  const document=documentFixture();document.getElementById('messages').appendChild(document.getElementById('welcome'));
  const calls=[];
  const app=mount(document,async(url,options)=>{
    if(url==='/lesson/providers')return ok(['fixture']);
    calls.push({url,options});return ok({reply:'<img src=x onerror=alert(1)>',sources:[{title:'Demo returns',excerpt:'Original passage',citation_id:'returns:0'}],execution_trace:[{stage:'initial_model',status:'success',duration_ms:0,result_count:0}]});
  },config);
  await new Promise(resolve=>setImmediate(resolve));document.getElementById('provider').value='fixture';
  document.getElementById('access-token').value='synthetic-token';
  await app.submit('Hello');
  assert.ok(texts(document.getElementById('messages')).includes('<img src=x onerror=alert(1)>'));
  assert.ok(texts(document.getElementById('messages')).includes('Original passage'));
  if(config.learning)assert.ok(texts(document.getElementById('messages')).some(text=>text.includes('initial_model')&&text.includes('0 ms')));
  assert.equal(document.getElementById('send').disabled,false);
  assert.equal(document.getElementById('question').focused,true);
  document.getElementById('reset').listeners.click();
  assert.equal(app.session.history.length,0);assert.equal(document.getElementById('welcome').hidden,false);
});

test('example fills the composer without making a model call', () => {
  const document=documentFixture();let calls=0;
  mount(document,async()=>{calls++;return ok(['fixture']);},config);
  const before=calls;document.getElementById('example').listeners.click();
  assert.equal(document.getElementById('question').value,config.example);assert.equal(calls,before);
});

if(config.ingestion) test('ingestion actions route only to the existing ingestion transport', async () => {
  let captured;const session=new ChatSession(config,async(url,options)=>{captured={url,...options};return ok({reply:'Prepared fixture chunks'});});
  await session.send('Preview',{action:'chunk'});
  assert.equal(captured.url,'/lesson/ingestion');assert.equal(JSON.parse(captured.body).action,'chunk');
});

if(config.auth) test('an expired token produces a safe login error and is not retained in history', async () => {
  const session=new ChatSession(config,async()=>({ok:false,status:401,json:async()=>({detail:'private-identity-details'})}));
  await assert.rejects(session.send('My order',{token:'synthetic-expired-token'}),/valid access token/);
  assert.equal(session.history.length,0);assert.ok(!JSON.stringify(session).includes('synthetic-expired-token'));
});
if(config.providers) test('a provider-list failure blocks submission rather than making an unintended default call', async () => {
  const document=documentFixture();let posts=0;
  const app=mount(document,async(url)=>{if(url==='/lesson/providers')return {ok:false,status:503};posts++;return ok(reply);},config);
  await new Promise(resolve=>setImmediate(resolve));await app.submit('Hello');
  assert.equal(posts,0);assert.equal(app.session.history.length,0);assert.equal(document.getElementById('send').disabled,true);
});
if(config.learning) test('safe execution facts survive failures without exposing the upstream error body', async () => {
  const document=documentFixture();
  const app=mount(document,async()=>({ok:false,status:503,json:async()=>({detail:'private-provider-payload',execution_trace:[{stage:'initial_model',status:'failed',duration_ms:0}]})}),config);
  await app.submit('Hello');
  const rendered=texts(document.getElementById('messages')).join(' ');
  assert.match(rendered,/How this request failed/);assert.match(rendered,/initial_model/);assert.doesNotMatch(rendered,/private-provider-payload/);
});

if(config.evaluation) test('retrieval evaluation stays in its UI endpoint without calling chat', async () => {
  let captured;const session=new ChatSession(config,async(url,options)=>{captured={url,...options};return ok({reply:'Retrieval scores',evaluation:{answerable_cases:1}});});
  await session.send('Evaluate',{action:'evaluate'});
  assert.equal(captured.url,'/lesson/evaluation');assert.equal(JSON.parse(captured.body).action,'evaluate');assert.equal(session.history.length,0);
});
