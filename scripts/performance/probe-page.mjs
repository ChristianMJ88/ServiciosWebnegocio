import { readFile } from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { setTimeout as delay } from 'node:timers/promises';
import { isolatedChrome,observeBeforeBootstrap,snapshotExpression,clsWindow } from './browser-probe.mjs';

export async function probePage({origin,root,width,height,scenario='normal',pathname='/',reducedMotion=false,replay=false,onParserPause,trialFixture,failPlans=false}){
 if(trialFixture&&!['positive','zero'].includes(trialFixture))throw new Error('Invalid derived trial fixture');
 const html=await readFile(path.join(root,'index.html'),'utf8');
 const main=html.match(/<script[^>]+src="([^"]*main-[^"]+\.js)"[^>]*>/)?.[1];assert.ok(main,'One exact hashed main entry required');
 const mainUrl=new URL(main,origin+'/').href;
 const chrome=await isolatedChrome(),{cdp}=chrome,logs=[],exceptions=[],requests=[],pending=new Set(),pendingChunks=new Set(),responses=new Map(),injected=[],asyncErrors=[],hashJobs=new Set(),heldFonts=[],heldPlans=[];
 const releasedFontKinds=new Set();
 const derivedFixtures=[];
 let held,mainSeen=0,lastNetwork=Date.now(),phase='opening',releasedAt,fontsReleased=false,plansReleased=false;
 const watchdog=setTimeout(()=>{asyncErrors.push(new Error('Sample exceeded 90 seconds'));cdp.close();},90000);
 const abort=()=>{asyncErrors.push(new Error('Probe interrupted'));cdp.close();};
 process.on('SIGINT',abort);process.on('SIGTERM',abort);
 const close=async()=>{clearTimeout(watchdog);process.off('SIGINT',abort);process.off('SIGTERM',abort);await chrome.close();};
 cdp.on('Runtime.consoleAPICalled',e=>logs.push({type:e.type,text:e.args.map(a=>a.value??a.description??'').join(' ')}));
 cdp.on('Runtime.exceptionThrown',e=>exceptions.push(e.exceptionDetails.exception?.description||e.exceptionDetails.text));
 cdp.on('Log.entryAdded',e=>logs.push({type:e.entry.level,text:e.entry.text,url:e.entry.url}));
 cdp.on('Network.requestWillBeSent',e=>{const url=new URL(e.request.url);if(url.pathname.startsWith('/api/')){pending.add(e.requestId);lastNetwork=Date.now();requests.push({id:e.requestId,url:e.request.url,method:e.request.method});}else{requests.push({url:e.request.url,kind:'asset'});if(url.origin===origin&&url.pathname.endsWith('.js')){pendingChunks.add(e.requestId);lastNetwork=Date.now();}}});
 cdp.on('Network.responseReceived',e=>{if(pending.has(e.requestId))responses.set(e.requestId,{status:e.response.status,path:new URL(e.response.url).pathname});});
 cdp.on('Network.loadingFinished',e=>{if(pendingChunks.delete(e.requestId))lastNetwork=Date.now();if(pending.delete(e.requestId)){lastNetwork=Date.now();const response=responses.get(e.requestId);if(response){const job=cdp.send('Network.getResponseBody',{requestId:e.requestId}).then(value=>{const body=Buffer.from(value.body,value.base64Encoded?'base64':'utf8');response.sha256=createHash('sha256').update(body).digest('hex');try{const data=JSON.parse(body);response.count=Array.isArray(data)?data.length:undefined;response.present=data!==null;}catch{/* Status remains visible. */}}).catch(error=>asyncErrors.push(error));hashJobs.add(job);job.finally(()=>hashJobs.delete(job));}}});
 cdp.on('Network.loadingFailed',e=>{const chunk=pendingChunks.delete(e.requestId),api=pending.delete(e.requestId);if(chunk||api){lastNetwork=Date.now();asyncErrors.push(new Error((api?'API':'Chunk')+' failed: '+e.errorText));}});
 const handleFetch=async e=>{
  if(e.request.url===mainUrl){if(++mainSeen!==1)throw new Error('Main paused more than once');held=e.requestId;return;}
  const route=new URL(e.request.url).pathname;
  if(scenario==='fontHold'&&!fontsReleased&&!['manrope','icons','jakarta'].some(kind=>releasedFontKinds.has(kind)&&route.includes(({manrope:'manrope.',icons:'bootstrap-icons',jakarta:'plusjakartasans.'})[kind]))&&(/\/fonts\/marketing\/.+\.woff2$/.test(route)||/\/media\/bootstrap-icons/.test(route))){heldFonts.push({id:e.requestId,route});return;}
  if(trialFixture&&route.endsWith('/plataforma/planes')){
   const response=await fetch(e.request.url);assert.equal(response.status,200);const plans=await response.json();assert.ok(Array.isArray(plans)&&plans.length>0,'Real plans required for derived trial fixture');
   const data=plans.map(plan=>({...plan,diasPrueba:trialFixture==='positive'?1:0}));derivedFixtures.push({field:'diasPrueba',fixture:trialFixture,count:data.length});
   await cdp.send('Fetch.fulfillRequest',{requestId:e.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from(JSON.stringify(data)).toString('base64')});return;
  }
  if(scenario==='plansLoading'&&!plansReleased&&route.endsWith('/plataforma/planes')){heldPlans.push(e.requestId);return;}
  const failed=((scenario==='plans503'||failPlans)&&route.endsWith('/plataforma/planes'))||(scenario==='support503'&&route.endsWith('/plataforma/soporte'))||(scenario==='nav503'&&route.endsWith('/marketing/landings/navegacion'));
  if(failed){injected.push({url:e.request.url,status:503});await cdp.send('Fetch.fulfillRequest',{requestId:e.requestId,responseCode:503,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from('{}').toString('base64')});return;}
  if(scenario==='apiSlow'&&route.startsWith('/api/v1/publico/'))await delay(1500);
  await cdp.send('Fetch.continueRequest',{requestId:e.requestId});
 };
 cdp.on('Fetch.requestPaused',e=>{handleFetch(e).catch(error=>{asyncErrors.push(error);cdp.close();});});
 const snapshot=async()=>{const value=await cdp.evaluate(snapshotExpression);return{...value,cls:clsWindow(value.shifts),responses:[...responses.values()],requests:[...requests],logs:[...logs],exceptions:[...exceptions],phase,releasedAt,scenario,emulation:{mobile:width<768,touch:width<1200,deviceScaleFactor:1},injected:[...injected]};};
 const settled=async()=>{
  const end=Date.now()+20000;let stableSince;
  while(Date.now()<end){if(asyncErrors.length)throw new AggregateError(asyncErrors,'Browser asynchronous errors');
   const state=await cdp.evaluate('({replay:!!window._ejsas?.ng,lastShift:window.__probe.shifts.at(-1)?.time||0,now:performance.now()})');
   if(!state.replay&&!pending.size&&!pendingChunks.size&&Date.now()-lastNetwork>=500&&state.now-state.lastShift>=500){stableSince??=Date.now();if(Date.now()-stableSince>=100){await Promise.all([...hashJobs]);if(asyncErrors.length)throw new AggregateError(asyncErrors,'Response hashes failed');await cdp.evaluate('new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(()=>r(true))))');return;}}else stableSince=undefined;
   await delay(50);
  }throw new Error('Page did not settle');
 };
 const assertConsole=()=>{
  assert.deepEqual(exceptions,[]);const failures=logs.filter(l=>['error','warning','warn'].includes(l.type));
  const expected=failures.filter(l=>injected.some(item=>item.url===l.url)&&/Failed to load resource.*503/.test(l.text));
  assert.equal(failures.length,expected.length,'Unexpected console warning/error: '+JSON.stringify(failures));
  assert.equal(expected.length,injected.length,'Every injected503 must be reported exactly once');assert.equal(logs.some(l=>/NG0?5\d\d/.test(l.text)),false);
 };
 try{
  await cdp.viewport(width,height);if(reducedMotion)await cdp.send('Emulation.setEmulatedMedia',{features:[{name:'prefers-reduced-motion',value:'reduce'}]});
  if(scenario==='netSlow'){await cdp.send('Network.emulateNetworkConditions',{offline:false,latency:150,downloadThroughput:200000,uploadThroughput:93750});await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});}
  await cdp.send('Page.addScriptToEvaluateOnNewDocument',{source:observeBeforeBootstrap});
  await cdp.send('Fetch.enable',{patterns:[{urlPattern:mainUrl,requestStage:'Request'},{urlPattern:origin+'/api/v1/publico/*',requestStage:'Request'},...(scenario==='fontHold'?[{urlPattern:origin+'/fonts/marketing/*'},{urlPattern:origin+'/media/bootstrap-icons*'}]:[])]});
  const navigation=await cdp.send('Page.navigate',{url:origin+pathname});assert.equal(navigation.errorText,undefined);phase='before-bootstrap';
  if(onParserPause)await onParserPause(cdp);
  await cdp.waitFor('!!window.__probe&&!!document.querySelector("#home-title")');
  for(let n=0;n<200&&!held;n++)await delay(50);assert.ok(held,'Exact main held');
  await cdp.waitFor('[...document.querySelectorAll("link[rel=stylesheet][href*=styles]")].some(l=>l.sheet&&(l.media==="all"||l.media===""))');
  if(scenario==='fontHold'){for(let n=0;n<200&&heldFonts.length<2;n++)await delay(50);assert.ok(heldFonts.length>=2,'Marketing fonts intercepted');await delay(500);}
  else await cdp.evaluate('document.fonts.ready.then(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(()=>r(true)))))');
  const before=await snapshot();assert.equal(before.chips,0);assert.equal(before.marks.includes('NgHydration'),false);assert.equal(before.replayPending,true);assert.equal(before.layout['.fl-home-hero-actions .fl-button'].text,'Crear mi cuenta');
  if(replay)await cdp.click('.mobile-tabs button');
  const release=async()=>{phase='after-bootstrap';releasedAt=await cdp.evaluate('performance.now()');await cdp.send('Fetch.continueRequest',{requestId:held});held=undefined;};
  const releaseFonts=async({only}={})=>{
   if(only&&!['manrope','icons','jakarta'].includes(only))throw new Error('Unknown font kind');
   if(!only)fontsReleased=true;else releasedFontKinds.add(only);
   const matches=route=>!only||route.includes(({manrope:'manrope.',icons:'bootstrap-icons',jakarta:'plusjakartasans.'})[only]);
   for(let i=heldFonts.length-1;i>=0;i--)if(matches(heldFonts[i].route)){const [font]=heldFonts.splice(i,1);await cdp.send('Fetch.continueRequest',{requestId:font.id});}
   const family=({manrope:'Manrope',icons:'bootstrap-icons',jakarta:'Jakarta'})[only];
   await cdp.evaluate(only?'Promise.all([...document.fonts].filter(f=>f.family.includes('+JSON.stringify(family)+')).map(f=>f.load())).then(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(()=>r(true)))))':'document.fonts.ready.then(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(()=>r(true)))))');
  };
  const releasePlans=async()=>{plansReleased=true;for(const requestId of heldPlans.splice(0))await cdp.send('Fetch.continueRequest',{requestId});};
  return{cdp,before,derivedFixtures,release,releaseFonts,releasePlans,settled,snapshot,assertConsole,close,setPhase(value){phase=value;},async diagnostic(error){return{error:error?.stack,phase,logs,exceptions,requests,injected,asyncErrors:asyncErrors.map(e=>e.message),snapshot:await snapshot().catch(()=>null)};}};
 }catch(error){error.diagnostic={phase,logs,exceptions,requests,injected,snapshot:await snapshot().catch(()=>null)};try{await close();}catch(cleanupError){const combined=new AggregateError([error,cleanupError],'Page setup and cleanup failed');combined.diagnostic=error.diagnostic;throw combined;}throw error;}
}
