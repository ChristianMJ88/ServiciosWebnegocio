import { parseArgs } from 'node:util';
import { mkdir,writeFile } from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import { publicPreview } from './public-preview.mjs';
import { finishProbe,clsWindow } from './browser-probe.mjs';
import { probePage } from './probe-page.mjs';
import { checkHydrationProbe } from '../check-hydration-probe.mjs';
import { fingerprintPublicBuild } from './build-fingerprint.mjs';
import { homeSizes } from './home-viewports.mjs';

const {values}=parseArgs({options:{root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string',default:'/private/tmp/fluora-m3-r3d'},api:{type:'string',default:'http://localhost:8080'},sizes:{type:'string'},scenarios:{type:'string',default:'normal,plans503,support503,nav503'},repetitions:{type:'string',default:'2'}}});
const sizes=values.sizes?values.sizes.split(',').map(value=>value.split('x').map(Number)):homeSizes;
if(sizes.some(size=>size.length!==2||size.some(n=>!Number.isInteger(n)||n<320||n>3000)))throw new Error('Invalid viewport');
const scenarios=values.scenarios.split(',');if(scenarios.some(s=>!['normal','plans503','support503','nav503'].includes(s)))throw new Error('Invalid scenario');
const repetitions=Number(values.repetitions);if(![1,2].includes(repetitions))throw new Error('Invalid repetition count');
const artifact=await checkHydrationProbe(path.resolve(values.root)),build=await fingerprintPublicBuild(path.join(values.root,'browser'));
await mkdir(values.output,{recursive:true,mode:0o700});const resultFile=path.join(values.output,build.fingerprint.slice(0,12)+'-'+Date.now()+'.json');
const results=[],violations=[],criterion=()=>({limit:.01,max:Math.max(0,...results.filter(r=>r.scenario==='normal').map(r=>r.initial.cls)),violations}),server=await publicPreview({root:path.join(values.root,'browser'),apiOrigin:values.api,spaShell:'csr'});let page,error;
try{
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});const origin='http://localhost:'+server.address().port;
 if(!(await fetch(origin+'/api/v1/publico/plataforma/soporte',{signal:AbortSignal.timeout(20000)})).ok)throw new Error('Public API unavailable');
 for(const [width,height] of sizes)for(const scenario of scenarios)for(let repeat=1;repeat<=(scenario==='normal'?repetitions:1);repeat++){
  page=await probePage({origin,root:path.join(values.root,'browser'),width,height,scenario});
  await page.release();await page.settled();
  const components={product:'app-home-product',plans:'app-home-plans',faq:'app-home-faq',closing:'app-home-closing'};
  const critical=Object.entries(page.before.sections).filter(([,s])=>s.placeholder&&s.rect.top<height&&s.rect.bottom>0);
  for(const [name] of critical)await page.cdp.waitFor('!!document.querySelector('+JSON.stringify(components[name])+')');
  if(critical.length)await page.settled();const initial=await page.snapshot(),sections={};
  if(scenario==='normal'&&initial.cls>.01)violations.push({width,height,repeat,initialCls:initial.cls,sources:initial.shifts});
  if(scenario==='normal')for(const [name,before] of critical){const loaded=initial.sections[name];assert.equal(loaded.placeholder,false);assert.ok(Math.abs(loaded.rect.top-before.rect.top)<=.5,'Critical section top must remain stable');assert.ok(loaded.rect.height>=before.rect.height,'Critical reserve may only grow');assert.equal(initial.shifts.some(s=>s.sources.some(source=>source.section===({product:'producto',plans:'planes',faq:'faq'}[name]))),false,'No initial shift from replacing a visible section');}
  for(const [name,selector,component] of [['product','#producto','app-home-product'],['plans','#planes','app-home-plans'],['faq','#faq','app-home-faq'],['closing','app-fluora-home .marketing-home>div:last-child','app-home-closing']]){
   page.setPhase('section-'+name);const startedAt=await page.cdp.evaluate('performance.now()');await page.cdp.evaluate('document.querySelector('+JSON.stringify(selector)+').scrollIntoView({block:"start",behavior:"instant"})');
   await page.cdp.waitFor('!!document.querySelector('+JSON.stringify(component)+')');
   await page.cdp.evaluate('Promise.all([...document.querySelectorAll('+JSON.stringify(component+' img')+')].map(i=>i.decode())).then(()=>true)');
   await page.settled();const snap=await page.snapshot(),delta=snap.shifts.filter(s=>s.time>=startedAt);assert.equal(snap.overflow,false);sections[name]={...snap.sections[name],cls:snap.cls,startedAt,deltaCls:clsWindow(delta),deltaShifts:delta};
  }
  page.assertConsole();const final=await page.snapshot();results.push({width,height,scenario,repeat,before:page.before,initial,sections,final});
  await writeFile(resultFile,JSON.stringify({artifact,build,api:values.api,results,initialClsCriterion:criterion()},null,2),{mode:0o600});
  console.log(JSON.stringify({width,height,scenario,repeat,initialCls:initial.cls,finalCls:final.cls,heights:Object.fromEntries(Object.entries(sections).map(([name,s])=>[name,s.rect.height])),resultFile}));
  await page.close();page=null;
 }
 if(violations.length)throw new Error(`${violations.length} samples exceed initial CLS .01; all samples retained`);
}catch(e){error=e;await writeFile(resultFile,JSON.stringify({artifact,build,results,initialClsCriterion:criterion(),diagnostic:page?await page.diagnostic(e):e.diagnostic},null,2),{mode:0o600});}
await finishProbe(error,[()=>page?.close(),()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));}]);
