// Deterministic presentation regressions: parser pauses and separately loaded fonts.
import {parseArgs} from 'node:util';
import {readFile,mkdir,writeFile} from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import {publicPreview} from './public-preview.mjs';
import {probePage} from './probe-page.mjs';
import {finishProbe} from './browser-probe.mjs';
import {classifyNavShifts,classifyIconTransitions} from './layout-residual-criteria.mjs';
import {navGeometry,inspectNavCtaResidual} from './nav-cta-geometry.mjs';
import {fingerprintPublicBuild} from './build-fingerprint.mjs';
const {values}=parseArgs({options:{root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string'},phase:{type:'string',default:'after'},kind:{type:'string',default:'all'},widths:{type:'string'},cuts:{type:'string'},'baseline-presentation':{type:'boolean',default:false}}});
assert.ok(['before','after'].includes(values.phase));if(values.widths)assert.ok(values.widths.split(',').every(v=>Number.isInteger(Number(v))&&Number(v)>=320&&Number(v)<=3000));assert.ok(['all','nav','icons'].includes(values.kind));if(values.cuts)assert.ok(values.cuts.split(',').every(v=>['details','nav','sign-in','hero'].includes(v)));
if(values['baseline-presentation'])assert.ok(values.phase==='before'&&values.kind==='nav','Derived baseline is a nav-only negative control');
const output=values.output||`/private/tmp/fluora-m3-r3-residual-${values.phase}`,root=path.resolve(values.root,'browser'),html=await readFile(path.join(root,'index.html'),'utf8'),build=await fingerprintPublicBuild(root),results=[],violations=[];let server,page,error;
await mkdir(output,{recursive:true,mode:0o700});
const closeServer=async()=>{if(server){server.closeAllConnections();await new Promise(resolve=>server.close(resolve));server=null;}};
const checkpoint=()=>writeFile(path.join(output,'results.json'),JSON.stringify({phase:values.phase,presentationFixture:values['baseline-presentation']?'derived-before-css':'real',build,results,violations},null,2),{mode:0o600});
const geometry=`(()=>{const rect=e=>e?e.getBoundingClientRect().toJSON():null;return{icons:[...document.querySelectorAll('app-home-pillars article>i')].map(e=>rect(e)),articles:[...document.querySelectorAll('app-home-pillars article')].map(e=>rect(e)),workflow:rect(document.querySelector('#como-funciona')),pillars:rect(document.querySelector('app-home-pillars')),fonts:[...document.fonts].map(f=>({family:f.family,status:f.status}))};})()`;
try{
 if(values.kind!=='icons')for(const [cut,pattern] of [['details',/<details[^>]*class="business-menu"[^>]*>/],['nav',/<nav[^>]*class="desktop-links"[^]*?<\/nav>/],['sign-in',/<a[^>]*class="sign-in"[^]*?<\/a>/],['hero',/<h1[^>]*id="home-title"[^]*?<\/h1>/]]){
  if(values.cuts&&!values.cuts.split(',').includes(cut))continue;
  const after=html.match(pattern)?.[0];assert.ok(after,'Unique split '+cut);
  server=await publicPreview({root,apiOrigin:'http://localhost:8080',spaShell:'csr',htmlSplit:{after,holdMs:3000}});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const origin='http://localhost:'+server.address().port;
  for(const width of (values.widths?values.widths.split(',').map(Number):cut==='hero'?[1280]:[1200,1280,1350,1440]))for(const held of [false,true]){
   let partial,fontsReleasedAt;
   page=await probePage({origin,root,width,height:800,scenario:held?'fontHold':'normal',onParserPause:async cdp=>{
    await cdp.waitFor('!!document.querySelector("'+(cut==='hero'?'#home-title':'.business-menu')+'")');
    if(values['baseline-presentation'])await cdp.evaluate(`(()=>{const s=document.createElement('style');s.textContent='.marketing-topbar .desktop-links,.marketing-topbar .top-actions{visibility:visible!important}';document.head.append(s);})()`);
    await cdp.evaluate('new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(()=>r(true))))');
    partial=await cdp.evaluate(`(()=>{const r=e=>e?e.getBoundingClientRect().toJSON():null;return{summary:r(document.querySelector('summary')),actions:r(document.querySelector('.top-actions')),trigger:!!document.querySelector('.menu-trigger'),nav:r(document.querySelector('.desktop-links')),navVisibility:getComputedStyle(document.querySelector('.desktop-links')).visibility,actionsVisibility:document.querySelector('.top-actions')?getComputedStyle(document.querySelector('.top-actions')).visibility:null,hero:r(document.querySelector('#home-title')),shifts:window.__probe.shifts};})()`);
    if(cut!=='hero')assert.equal(partial.trigger,false,'Snapshot must be inside parser pause');
   }});
   if(held){fontsReleasedAt=await page.cdp.evaluate('performance.now()');await page.releaseFonts();}const before=await page.snapshot(),navBefore=await page.cdp.evaluate(navGeometry);await page.release();await page.settled();page.assertConsole();const final=await page.snapshot();
   const groups=classifyNavShifts(final.shifts,{fontsReleasedAt,releasedAt:final.releasedAt}),navAfter=await page.cdp.evaluate(navGeometry),ctaResidual=inspectNavCtaResidual(navBefore,navAfter,groups.hydration,{width,height:800});
   const visible=await page.cdp.evaluate("({nav:getComputedStyle(document.querySelector('.desktop-links')).visibility,actions:getComputedStyle(document.querySelector('.top-actions')).visibility})");
   const item={kind:'nav',presentationFixture:values['baseline-presentation']?'derived-before-css':'real',cut,width,held,partial,initialCls:final.cls,fontsReleasedAt,releasedAt:final.releasedAt,shifts:groups,navBefore,navAfter,ctaResidual,finalVisibility:visible,diagnosticOnly:cut==='hero'};results.push(item);
   if(values.phase==='after'&&cut!=='hero'&&(partial.navVisibility!=='hidden'||partial.actionsVisibility&&partial.actionsVisibility!=='hidden'||visible.nav!=='visible'||visible.actions!=='visible'||groups.parser.length||ctaResidual.structural.length||final.cls>.01))violations.push(item);
   await checkpoint();console.log(JSON.stringify({kind:'nav',cut,width,held,initialCls:final.cls,parserShifts:groups.parser.length,fontShifts:groups.font.length,hydrationShifts:groups.hydration.length,partialVisibility:partial.navVisibility}));await page.close();page=null;
  }await closeServer();
 }
 if(values.kind!=='nav'){
  server=await publicPreview({root,apiOrigin:'http://localhost:8080',spaShell:'csr'});await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const origin='http://localhost:'+server.address().port;
  for(const width of (values.widths?values.widths.split(',').map(Number):[360,375,575,576,767,768,800,1000,1023,1024,1180,1280,1440]))for(const order of [['manrope','icons'],['icons','manrope']]){
   page=await probePage({origin,root,width,height:1280,scenario:'fontHold'});await page.releaseFonts({only:'jakarta'});const states=[await page.cdp.evaluate(geometry)];
   for(const only of order){await page.releaseFonts({only});states.push(await page.cdp.evaluate(geometry));}await page.releaseFonts();const before=await page.snapshot(),navBefore=await page.cdp.evaluate(navGeometry);await page.release();await page.settled();page.assertConsole();
   const classified=classifyIconTransitions(states,order);
   const shifts=before.shifts.filter(s=>s.sources.some(v=>v.region==='workflow'));
   const item={kind:'icons',presentationFixture:'real',width,order,states,preBootstrapCls:before.cls,workflowShifts:shifts,...classified};results.push(item);if(values.phase==='after'&&!classified.stable)violations.push(item);
   await checkpoint();console.log(JSON.stringify({kind:'icons',width,order,stable:classified.stable,workflowShifts:shifts.length,preBootstrapCls:before.cls}));await page.close();page=null;
  }await closeServer();
 }
 if(violations.length)throw new Error(`${violations.length} layout violations; all samples retained`);
}catch(e){error=e;await writeFile(path.join(output,'failure.json'),JSON.stringify({error:e.stack,results,violations,diagnostic:page?await page.diagnostic(e):e.diagnostic},null,2),{mode:0o600});}
await finishProbe(error,[()=>page?.close(),closeServer]);
