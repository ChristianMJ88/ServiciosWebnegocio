import { parseArgs } from 'node:util';
import { mkdir,writeFile } from 'node:fs/promises';
import path from 'node:path';
import { publicPreview } from './public-preview.mjs';
import { finishProbe } from './browser-probe.mjs';
import { probePage } from './probe-page.mjs';
import { checkHydrationProbe } from '../check-hydration-probe.mjs';
import { homeSizes } from './home-viewports.mjs';
const {values}=parseArgs({options:{root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string',default:'/private/tmp/fluora-m3-r3-plans-loading'},api:{type:'string',default:'http://localhost:8080'}}});
await checkHydrationProbe(path.resolve(values.root));await mkdir(values.output,{recursive:true,mode:0o700});
const root=path.join(values.root,'browser'),server=await publicPreview({root,apiOrigin:values.api,spaShell:'csr'}),results=[];let page,error;
try{
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});const origin='http://localhost:'+server.address().port;
 for(const [width,height] of homeSizes){
  page=await probePage({origin,root,width,height,scenario:'plansLoading'});await page.release();
  await page.cdp.waitFor('performance.getEntriesByType("mark").some(e=>e.detail?.feature==="NgHydration")');
  await page.cdp.evaluate('document.querySelector("#planes").scrollIntoView({behavior:"instant",block:"start"})');
  await page.cdp.waitFor('document.querySelector(".fl-home-plans-state")?.textContent.includes("Consultando")');
  await page.cdp.evaluate('document.fonts.ready.then(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(()=>r(true)))))');const loading=await page.snapshot();
  await page.releasePlans();await page.settled();page.assertConsole();const loaded=await page.snapshot();results.push({width,height,loading,loaded});
  await writeFile(path.join(values.output,'results.json'),JSON.stringify({root,results},null,2),{mode:0o600});console.log(JSON.stringify({width,height,loadingHeight:loading.sections.plans.rect.height,loadedHeight:loaded.sections.plans.rect.height,cls:loaded.cls}));
  await page.close();page=null;
 }
}catch(e){error=e;await writeFile(path.join(values.output,'failure.json'),JSON.stringify({results,diagnostic:page?await page.diagnostic(e):e.diagnostic},null,2),{mode:0o600});}
await finishProbe(error,[()=>page?.close(),()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));}]);
