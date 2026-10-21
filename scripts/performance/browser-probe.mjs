// Isolated automated browser tests. Never attach to an existing Chrome profile.
import { spawn,execFileSync } from 'node:child_process';
import { mkdtemp,readFile,rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';

export class Cdp {
  constructor(socket){
    this.socket=socket;this.next=0;this.pending=new Map();this.listeners=new Map();
    socket.addEventListener('message',event=>{
      let message;try{message=JSON.parse(event.data);}catch{socket.close();return;}
      if(message.id){const pending=this.pending.get(message.id);if(!pending)return;this.pending.delete(message.id);clearTimeout(pending.timer);message.error?pending.reject(new Error(pending.method+': '+JSON.stringify(message.error))):pending.resolve(message.result);}
      else for(const listener of this.listeners.get(message.method)||[])listener(message.params);
    });
    socket.addEventListener('close',()=>{for(const p of this.pending.values()){clearTimeout(p.timer);p.reject(new Error('Browser connection closed'));}this.pending.clear();});
  }
  on(name,listener){const listeners=this.listeners.get(name)||new Set();listeners.add(listener);this.listeners.set(name,listeners);return()=>listeners.delete(listener);}
  send(method,params={},timeout=15000){return new Promise((resolve,reject)=>{
    if(this.socket.readyState!==WebSocket.OPEN){reject(new Error('Browser socket is closed'));return;}
    const id=++this.next,timer=setTimeout(()=>{this.pending.delete(id);reject(new Error('CDP timeout: '+method));},timeout);
    this.pending.set(id,{resolve,reject,timer,method});try{this.socket.send(JSON.stringify({id,method,params}));}catch(error){clearTimeout(timer);this.pending.delete(id);reject(error);}
  });}
  async evaluate(expression,timeout=15000){const result=await this.send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true},timeout);
    if(result.exceptionDetails)throw new Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;}
  async waitFor(expression,timeout=15000){const end=Date.now()+timeout;do{try{if(await this.evaluate(expression,Math.max(1,end-Date.now())))return;}catch(error){if(!/Execution context was destroyed|Cannot find context|Inspected target navigated or closed/.test(error.message))throw error;}await delay(50);}while(Date.now()<end);throw new Error('Condition timed out: '+expression);}
  async viewport(width,height){await this.send('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:width<768});await this.send('Emulation.setTouchEmulationEnabled',{enabled:width<1200});}
  async click(selector){const point=await this.evaluate(`(()=>{const e=document.querySelector(${JSON.stringify(selector)});if(!e)throw Error('Missing click target');const r=e.getBoundingClientRect();return{x:r.x+r.width/2,y:r.y+r.height/2};})()`);
    await this.send('Input.dispatchMouseEvent',{type:'mousePressed',button:'left',clickCount:1,...point});await this.send('Input.dispatchMouseEvent',{type:'mouseReleased',button:'left',clickCount:1,...point});}
  close(){this.socket.close();}
}

export async function isolatedChrome(){
  if(typeof WebSocket!=='function')throw new Error('Browser probes require Node22 native WebSocket');
  const profile=await mkdtemp(path.join(tmpdir(),'fluora-hydration-chrome-'));
  const child=spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',[
    '--headless=new','--disable-extensions','--no-first-run','--no-default-browser-check',
    '--use-mock-keychain','--password-store=basic','--disable-background-networking','--disable-component-update',
    '--disable-sync','--disable-default-apps','--no-service-autorun','--disable-breakpad',
    '--remote-debugging-port=0','--remote-debugging-address=127.0.0.1','--user-data-dir='+profile,'about:blank'
  ],{stdio:['ignore','ignore','pipe'],shell:false,detached:true});
  let cdp,browserCdp,spawnError,stderr='',closed=false;child.once('error',error=>{spawnError=error;});child.stderr.on('data',data=>{stderr=(stderr+data).slice(-8192);});
  const running=()=>child.exitCode===null&&child.signalCode===null&&!spawnError;
  const exited=()=>running()?new Promise(resolve=>child.once('exit',resolve)):Promise.resolve();
  const signalGroup=signal=>{if(!child.pid)return;try{process.kill(-child.pid,signal);}catch(error){if(error.code!=='ESRCH')throw error;}};
  const close=async()=>{if(closed)return;closed=true;
    try{if(running()&&browserCdp)await browserCdp.send('Browser.close',{},2000);}catch{/* Graceful exit may close the socket before acknowledging. */}
    cdp?.close();browserCdp?.close();
    await Promise.race([exited(),delay(2000)]);
    if(running()){signalGroup('SIGTERM');await Promise.race([exited(),delay(2000)]);}
    if(running()){signalGroup('SIGKILL');await Promise.race([exited(),delay(2000)]);}
    if(running())throw new Error('Isolated Chrome failed to exit');
    if(child.pid)await closeProcessGroup(child.pid);
    await rm(profile,{recursive:true,force:true,maxRetries:8,retryDelay:100});};
  try{
    let port;for(let n=0;n<200;n++){
      if(spawnError)throw spawnError;if(child.exitCode!==null)throw new Error('Isolated Chrome exited');
      try{port=Number((await readFile(path.join(profile,'DevToolsActivePort'),'utf8')).split('\n')[0]);if(port)break;}catch(error){if(error.code!=='ENOENT')throw error;}
      await delay(50);
    }
    if(!port)throw new Error('Chrome debugging port unavailable');
    const endpoint='http://127.0.0.1:'+port;
    const version=await debuggerJson(endpoint+'/json/version',running);
    browserCdp=await connectCdp(version.webSocketDebuggerUrl);
    const targets=await debuggerJson(endpoint+'/json/list',running);
    const target=targets.find(target=>target.type==='page');if(!target)throw new Error('No isolated page');
    cdp=await connectCdp(target.webSocketDebuggerUrl);await cdp.send('Page.enable');await cdp.send('Runtime.enable');await cdp.send('Log.enable');await cdp.send('Network.enable');await cdp.waitFor('document.readyState==="complete"&&location.href==="about:blank"');
    return{cdp,close,profile};
  }catch(error){error.message+='\nIsolated Chrome startup diagnostics: '+stderr;try{await close();}catch(cleanupError){throw new AggregateError([error,cleanupError],'Browser startup and cleanup failed');}throw error;}
}

async function connectCdp(url){const socket=new WebSocket(url);await new Promise((resolve,reject)=>{const timer=setTimeout(()=>{socket.close();reject(new Error('Chrome WebSocket open timed out'));},10000);socket.addEventListener('open',()=>{clearTimeout(timer);resolve();},{once:true});socket.addEventListener('error',()=>{clearTimeout(timer);reject(new Error('Chrome WebSocket error'));},{once:true});});return new Cdp(socket);}

// DevToolsActivePort may be written just before the HTTP endpoint is ready.
// Retry only that startup transport, never a navigation or an application assertion.
export async function debuggerJson(url,running,fetchImpl=fetch){
 const deadline=Date.now()+5000;let last;
 do{if(!running())throw new Error('Chrome exited before DevTools was ready',{cause:last});
  try{const response=await fetchImpl(url,{signal:AbortSignal.timeout(1000)});if(!response.ok)throw new Error('DevTools HTTP '+response.status);return await response.json();}
  catch(error){if(!['ECONNREFUSED','ECONNRESET','UND_ERR_SOCKET'].includes(error.cause?.code)&&error.name!=='TimeoutError')throw error;last=error;}
  await delay(50);
 }while(Date.now()<deadline);throw new Error('DevTools startup transport did not become ready',{cause:last});
}

// Detect orphaned helpers after the main process exits; never hide EPERM.
export function processGroupMembers(pid){
 const rows=execFileSync('/bin/ps',['-axo','pid=,pgid=,uid=,comm='],{encoding:'utf8',timeout:5000}).trim().split('\n');
 return rows.map(row=>row.trim().match(/^(\d+)\s+(\d+)\s+(\d+)\s+(.+)$/)).filter(m=>m&&Number(m[2])===pid).map(m=>({pid:Number(m[1]),uid:Number(m[3]),command:m[4]}));
}
export async function closeProcessGroup(pid,kill=process.kill.bind(process),wait=delay,members=processGroupMembers){
 const owned=()=>{const rows=members(pid);if(rows.some(p=>p.uid!==process.getuid()||!/Chrome|chrome_crashpad_handler/.test(p.command)))throw new Error('Refusing to signal an unverified process group');return rows.length>0;};
 // ps verifies existence/ownership before signaling a dead or possibly reused group.
 if(!owned())return;
 try{kill(-pid,0);}catch(error){if(error.code==='ESRCH'&&!owned())return;throw error;}
 try{kill(-pid,'SIGKILL');}catch(error){if(error.code==='ESRCH'&&!owned())return;throw error;}
 for(let n=0;n<40;n++){if(!owned())return;await wait(50);}
 throw new Error('Isolated Chrome helper group failed to exit');
}

/** Close independent resources even if one cleanup throws; preserve the original error. */
export async function finishProbe(error,cleanups){const errors=error?[error]:[];for(const cleanup of cleanups){try{await cleanup();}catch(e){errors.push(e);}}if(errors.length===1)throw errors[0];if(errors.length)throw new AggregateError(errors,'Probe and cleanup errors');}

export function clsWindow(shifts){let start,last,sum=0,max=0;for(const e of shifts){if(start===undefined||e.time-last>1000||e.time-start>5000){start=e.time;sum=0;}sum+=e.value;last=e.time;max=Math.max(max,sum);}return max;}

export const observeBeforeBootstrap=String.raw`(()=>{
 const selectors={title:'#home-title',cta:'.fl-home-hero-actions .fl-button',brand:'.brand',footer:'footer.site-footer',plans:'.plans-placeholder',navCtaIcon:'.top-cta > i.bi-arrow-up-right'};
 const nodes={},shifts=[],events=[],scrollCalls=[];window.__probe={nodes,shifts,events,scrollCalls};
 const scrollIntoView=Element.prototype.scrollIntoView;Element.prototype.scrollIntoView=function(...args){scrollCalls.push({id:this.id,time:performance.now(),options:args[0]});return Reflect.apply(scrollIntoView,this,args);};
 const capture=()=>{for(const [name,selector] of Object.entries(selectors))if(!nodes[name]){const node=document.querySelector(selector);if(node)nodes[name]=node;}};
 const observer=new MutationObserver(list=>{capture();if(list.some(e=>e.target instanceof HTMLDialogElement&&e.attributeName==='open'))events.push({type:'dialog',time:performance.now(),open:!!document.querySelector('dialog[open]')});});observer.observe(document,{subtree:true,childList:true,attributes:true,attributeFilter:['open']});window.__probe.observer=observer;capture();
 document.addEventListener('click',event=>{if(event.target.closest?.('.mobile-tabs button'))events.push({type:'more-click',time:performance.now()});},true);
 new PerformanceObserver(list=>{for(const e of list.getEntries())if(!e.hadRecentInput)shifts.push({value:e.value,time:e.startTime,sources:(e.sources||[]).map(s=>{const element=s.node?.nodeType===1?s.node:s.node?.parentElement;return{tag:s.node?.tagName,cls:s.node?.getAttribute?.('class'),id:element?.id,dynamicCta:!!element?.closest?.('.top-cta'),knownNavCta:s.node===window.__probe.nodes.navCtaIcon,region:element?.closest?.('.desktop-links')?'nav':element?.closest?.('.top-actions')?'actions':element?.closest?.('#como-funciona')?'workflow':element?.closest?.('app-home-pillars')?'pillars':null,section:element?.closest?.('#producto,#planes,#faq')?.id,previous:s.previousRect.toJSON(),current:s.currentRect.toJSON()};})});}).observe({type:'layout-shift',buffered:true});
})();`;

export const snapshotExpression=String.raw`(()=>{
 const selectors={title:'#home-title',cta:'.fl-home-hero-actions .fl-button',brand:'.brand',footer:'footer.site-footer',plans:'.plans-placeholder',navCtaIcon:'.top-cta > i.bi-arrow-up-right'};
 const nodes={};for(const [name,selector] of Object.entries(selectors)){const node=document.querySelector(selector);nodes[name]={exists:!!node,same:!!node&&window.__probe.nodes[name]===node};}
 const layout={};for(const selector of ['h1','.fl-home-hero','.fl-home-hero-copy','.fl-home-hero-preview','.fl-home-preview-heading','.fl-home-preview-caption','.fl-home-hero-actions .fl-button','.marketing-topbar','.desktop-links','.top-actions','.top-cta','h3.footer-brand-title','body','.fl-home-hero-reassurance','.fl-home-hero-verticals']){const e=document.querySelector(selector);if(e){const s=getComputedStyle(e);layout[selector]={rect:e.getBoundingClientRect().toJSON(),font:s.fontFamily,lineHeight:s.lineHeight,text:selector==='body'||selector==='.fl-home-hero'?undefined:e.textContent.trim()};}}
 const sections={};for(const [name,selector] of Object.entries({product:'#producto',plans:'#planes',faq:'#faq',closing:'app-fluora-home .marketing-home>div:last-child'})){const e=document.querySelector(selector);if(e)sections[name]={rect:e.getBoundingClientRect().toJSON(),placeholder:!!e.querySelector('.section-placeholder')};}
 const root=getComputedStyle(document.documentElement),theme={};for(const key of ['--tenant-heading-font','--tenant-body-font','--primary-light','--primary-border','--primary-shadow'])theme[key]=root.getPropertyValue(key);
 return{nodes,layout,sections,theme,width:innerWidth,height:innerHeight,clientWidth:document.documentElement.clientWidth,gutter:innerWidth-document.documentElement.clientWidth,overflow:document.documentElement.scrollWidth>document.documentElement.clientWidth,marks:performance.getEntriesByType('mark').filter(e=>e.name==='mark_feature_usage').map(e=>e.detail?.feature),shifts:window.__probe.shifts,shiftSum:window.__probe.shifts.reduce((n,e)=>n+e.value,0),chips:document.querySelectorAll('.fl-home-hero-verticals a').length,plans:document.querySelectorAll('.fl-home-plan-card').length,faqs:document.querySelectorAll('.fl-home-faq-list>article').length,replayPending:!!window._ejsas?.ng,events:window.__probe.events,scrollCalls:window.__probe.scrollCalls,scrollY,resources:performance.getEntriesByType('resource').filter(e=>/\.(woff2|css)(?:\?|$)/.test(e.name)).map(e=>({name:e.name,start:e.startTime,responseEnd:e.responseEnd}))};
})()`;
