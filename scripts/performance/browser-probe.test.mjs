import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Cdp,finishProbe,clsWindow,observeBeforeBootstrap,debuggerJson,closeProcessGroup } from './browser-probe.mjs';
import { JSDOM } from 'jsdom';

test('cleanup always closes remaining resources and preserves the original failure',async()=>{
 const original=new Error('assertion failed'),cleanup=new Error('profile busy');let serverClosed=false;
 await assert.rejects(finishProbe(original,[()=>{throw cleanup;},()=>{serverClosed=true;}]),error=>error instanceof AggregateError&&error.errors[0]===original&&error.errors[1]===cleanup);
 assert.equal(serverClosed,true);
});
test('session CLS does not sum shifts across separate windows',()=>{
 assert.equal(clsWindow([{time:100,value:.1},{time:800,value:.2},{time:2000,value:.25}]),.1+.2);
 assert.equal(clsWindow([{time:100,value:.1},{time:900,value:.1},{time:1800,value:.1},{time:2700,value:.1},{time:3600,value:.1},{time:4500,value:.1},{time:5300,value:.2}]),.6);
});
test('a session starts with its first shift, including a full window ending after 5000ms',()=>{
 const shifts=[600,1500,2400,3300,4200,5100,5500].map(time=>({time,value:.1}));
 assert.ok(Math.abs(clsWindow(shifts)-.7)<1e-12);
});
test('CDP rejects a closed connection immediately without leaving request timers',async()=>{
 const socket=new EventTarget();socket.readyState=3;const cdp=new Cdp(socket);
 await assert.rejects(cdp.send('Page.enable'),/socket is closed/);assert.equal(cdp.pending.size,0);
});
test('DevTools startup retries a transient socket failure, without retrying HTTP or parsing failures',async()=>{
 let calls=0;const actual=await debuggerJson('http://127.0.0.1/fixture',()=>true,async()=>{if(calls++===0)throw new TypeError('fetch failed',{cause:{code:'UND_ERR_SOCKET'}});return{ok:true,json:async()=>({ready:true})};});assert.deepEqual(actual,{ready:true});assert.equal(calls,2);
 await assert.rejects(debuggerJson('http://127.0.0.1/fixture',()=>true,async()=>({ok:false,status:403})),/HTTP 403/);
 await assert.rejects(debuggerJson('http://127.0.0.1/fixture',()=>true,async()=>{throw new TypeError('fetch failed',{cause:{code:'ENOTFOUND'}});}),error=>error.cause.code==='ENOTFOUND');
 let unexpected=0;await assert.rejects(debuggerJson('http://127.0.0.1/fixture',()=>false,async()=>{unexpected++;}),/Chrome exited/);assert.equal(unexpected,0);
});
test('CDP resolves responses and clears pending requests',async()=>{
 const socket=new EventTarget();socket.readyState=1;socket.send=value=>queueMicrotask(()=>{const request=JSON.parse(value);socket.dispatchEvent(new MessageEvent('message',{data:JSON.stringify({id:request.id,result:{ok:true}})}));});
 const cdp=new Cdp(socket);assert.deepEqual(await cdp.send('Page.enable'),{ok:true});assert.equal(cdp.pending.size,0);
});
test('waitFor retries a destroyed navigation context, but never ignores a real assertion error',async()=>{
 let calls=0;await Cdp.prototype.waitFor.call({async evaluate(){if(calls++===0)throw new Error('Inspected target navigated or closed');return true;}},'ready',1000);assert.equal(calls,2);
 await assert.rejects(Cdp.prototype.waitFor.call({async evaluate(){throw new Error('Application evaluation failed');}},'ready',1000),/Application evaluation failed/);
});
test('layout observer accepts text nodes and SVG without throwing into application console',()=>{
 const dom=new JSDOM('<p>text</p><svg class="icon"></svg>',{runScripts:'outside-only'});let callback;
 try{dom.window.PerformanceObserver=class{constructor(value){callback=value;}observe(){}};dom.window.eval(observeBeforeBootstrap);
  const rect={toJSON:()=>({x:0,y:0,width:20,height:10})};
  callback({getEntries:()=>[{hadRecentInput:false,value:.01,startTime:100,sources:[...dom.window.document.querySelector('p').childNodes,dom.window.document.querySelector('svg')].map(node=>({node,previousRect:rect,currentRect:rect}))}]});
  assert.equal(dom.window.__probe.shifts.length,1);assert.equal(dom.window.__probe.shifts[0].sources[1].cls,'icon');
 }finally{dom.window.__probe?.observer.disconnect();dom.window.close();}
});


test('group cleanup verifies members before signaling, then rechecks helpers',async()=>{
 const calls=[];await closeProcessGroup(123,(...v)=>calls.push(v),async()=>{},()=>[]);assert.deepEqual(calls,[]);
 const helper={pid:457,uid:process.getuid(),command:'Google Chrome Helper'};let killed=false;
 await closeProcessGroup(456,(pid,signal)=>{calls.push([pid,signal]);if(signal==='SIGKILL')killed=true;},async()=>{},()=>killed?[]:[helper]);
 assert.deepEqual(calls,[[-456,0],[-456,'SIGKILL']]);
});
test('group cleanup reports EPERM, foreign groups and helpers that do not terminate',async()=>{
 const helper={pid:124,uid:process.getuid(),command:'Google Chrome Helper'},denied=Object.assign(new Error('denied'),{code:'EPERM'});
 await assert.rejects(closeProcessGroup(123,()=>{throw denied;},async()=>{},()=>[helper]),error=>error===denied);
 await assert.rejects(closeProcessGroup(123,()=>{},async()=>{},()=>[helper]),/helper group failed to exit/);
 await assert.rejects(closeProcessGroup(123,()=>{throw new Error('must not signal');},async()=>{},()=>[{...helper,command:'unrelated service'}]),/unverified/);
});
test('layout sources classify descendant text in nav, actions, workflow and pillars',()=>{
 const dom=new JSDOM('<nav class="desktop-links"><a>nav</a></nav><div class="top-actions"><span class="top-cta">action</span></div><section id="como-funciona"><p>workflow</p></section><app-home-pillars><p>pillar</p></app-home-pillars>',{runScripts:'outside-only'});let callback;
 try{dom.window.PerformanceObserver=class{constructor(value){callback=value;}observe(){}};dom.window.eval(observeBeforeBootstrap);const rect={toJSON:()=>({})};
 callback({getEntries:()=>[{hadRecentInput:false,value:.01,startTime:100,sources:[...dom.window.document.querySelectorAll('a,span,p')].map(e=>({node:e.firstChild,previousRect:rect,currentRect:rect}))}]});
 assert.deepEqual(Array.from(dom.window.__probe.shifts[0].sources,s=>s.region),['nav','actions','workflow','pillars']);assert.equal(dom.window.__probe.shifts[0].sources[1].dynamicCta,true);
 }finally{dom.window.__probe?.observer.disconnect();dom.window.close();}
});
