import { parseArgs } from 'node:util';
import { mkdir,readFile,writeFile } from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import { publicPreview } from './public-preview.mjs';
import { finishProbe } from './browser-probe.mjs';
import { probePage } from './probe-page.mjs';
import { checkHydrationProbe } from '../check-hydration-probe.mjs';
import { fingerprintPublicBuild } from './build-fingerprint.mjs';

const {values}=parseArgs({options:{root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string',default:'/private/tmp/fluora-m3-r3c'},api:{type:'string',default:'http://localhost:8080'},sizes:{type:'string',default:'375x812,820x1180,1440x900'},scenario:{type:'string',default:'normal'},replay:{type:'boolean',default:false}}});
const sizes=values.sizes.split(',').map(value=>value.split('x').map(Number));
if(sizes.some(size=>size.length!==2||size.some(n=>!Number.isInteger(n)||n<320||n>3000)))throw new Error('Invalid viewport');
if(!['normal','apiSlow','netSlow'].includes(values.scenario))throw new Error('Invalid hydration scenario');
const artifact=await checkHydrationProbe(path.resolve(values.root)),mode=JSON.parse(await readFile(path.join(values.root,'render-origins.json'),'utf8')).mode;
const build=await fingerprintPublicBuild(path.join(values.root,'browser'));
await mkdir(values.output,{recursive:true,mode:0o700});const resultFile=path.join(values.output,build.fingerprint.slice(0,12)+'-'+Date.now()+'.json');
const requests=[],results=[],server=await publicPreview({root:path.join(values.root,'browser'),apiOrigin:values.api,spaShell:'csr',requestLog:requests});
let page,error;
try{
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});
 const origin='http://localhost:'+server.address().port;
 if(!(await fetch(origin+'/api/v1/publico/plataforma/soporte',{signal:AbortSignal.timeout(20000)})).ok)throw new Error('Public API unavailable');
 for(const [width,height] of sizes){
  page=await probePage({origin,root:path.join(values.root,'browser'),width,height,scenario:values.scenario,replay:values.replay});
  await page.release();await page.settled();const after=await page.snapshot();
  results.push({width,height,before:page.before,after});await writeFile(resultFile,JSON.stringify({artifact,build,mode,api:values.api,results},null,2),{mode:0o600});
  page.assertConsole();
  for(const [name,node] of Object.entries(after.nodes)){if(name==='plans'&&!node.exists){assert.ok(await page.cdp.evaluate('!!document.querySelector("app-home-plans")'),'Placeholder replaced by triggered defer');continue;}assert.ok(node.same,'DOM reused: '+name);}
  assert.ok(after.marks.includes('NgHydration'));assert.ok(after.marks.includes('NgEventReplay'));assert.equal(after.replayPending,false);assert.equal(after.overflow,false);
  if(mode==='development')assert.equal(after.logs.filter(l=>/Angular hydrated.*0 component\(s\) were skipped/.test(l.text)).length,1,'Development hydration completion log');
  if(values.replay){assert.ok(await page.cdp.evaluate('!!document.querySelector("dialog[open]")&&document.querySelector(".mobile-tabs button").getAttribute("aria-expanded")==="true"'));
   const click=after.events.find(e=>e.type==='more-click'),opened=after.events.find(e=>e.type==='dialog'&&e.open);assert.ok(click&&opened,'Real click and replayed dialog observed');after.replayLatencyMs=opened.time-click.time;
   await page.cdp.send('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await page.cdp.send('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});
   await page.cdp.waitFor('!document.querySelector("dialog[open]")');assert.ok(await page.cdp.evaluate('document.activeElement===document.querySelector(".mobile-tabs button")'),'Focus returns to More');
  }
  await writeFile(resultFile,JSON.stringify({artifact,build,mode,api:values.api,results},null,2),{mode:0o600});
  const screenshot=await page.cdp.send('Page.captureScreenshot',{format:'png'});await writeFile(path.join(values.output,width+'x'+height+'.png'),Buffer.from(screenshot.data,'base64'),{mode:0o600});
  console.log(JSON.stringify({width,height,cls:after.cls,marks:after.marks,logs:after.logs,replayLatencyMs:after.replayLatencyMs,domReused:true,resultFile}));
  await page.close();page=null;
 }
}catch(e){error=e;await writeFile(resultFile,JSON.stringify({artifact,build,mode,results,diagnostic:page?await page.diagnostic(e):e.diagnostic},null,2),{mode:0o600});}
await finishProbe(error,[()=>page?.close(),()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));}]);
