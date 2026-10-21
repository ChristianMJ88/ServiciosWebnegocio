import { parseArgs } from 'node:util';
import { mkdir,readFile,readdir,writeFile } from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import { setTimeout as delay } from 'node:timers/promises';
import { publicPreview } from './public-preview.mjs';
import { finishProbe } from './browser-probe.mjs';
import { probePage } from './probe-page.mjs';
import { checkHydrationProbe } from '../check-hydration-probe.mjs';

const {values}=parseArgs({options:{root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string',default:'/private/tmp/fluora-m3-r3c-navigation'},api:{type:'string',default:'http://localhost:8080'}}});
await checkHydrationProbe(path.resolve(values.root));await mkdir(values.output,{recursive:true,mode:0o700});
const root=path.join(values.root,'browser'),gsap=[];
for(const file of await readdir(root))if(file.endsWith('.js')&&(await readFile(path.join(root,file),'utf8')).includes('https://gsap.com'))gsap.push(file);
assert.equal(gsap.length,1,'GSAP chunk identified from actual artifact');
const server=await publicPreview({root,apiOrigin:values.api,spaShell:'csr'}),results=[];let page,error;
try{
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});const origin='http://localhost:'+server.address().port;
 for(const width of [375,1440])for(const fragment of ['planes','faq']){
  page=await probePage({origin,root,width,height:width===375?812:900,pathname:'/#'+fragment});await page.release();
  await page.cdp.waitFor('!!document.querySelector("app-home-closing")&&!!document.querySelector("app-home-faq")');await page.settled();
  await page.cdp.waitFor('!!document.querySelector(\'a[href$="#'+fragment+'"][aria-current="location"]\')');
  await page.cdp.waitFor('Math.abs(document.querySelector("#'+fragment+'").getBoundingClientRect().top-parseFloat(getComputedStyle(document.querySelector("#'+fragment+'")).scrollMarginTop))<=1');
  const position=await page.cdp.evaluate('({target:document.querySelector("#'+fragment+'").getBoundingClientRect().top,margin:parseFloat(getComputedStyle(document.querySelector("#'+fragment+'")).scrollMarginTop),bar:document.querySelector(".marketing-topbar").getBoundingClientRect().bottom,railControls:!!document.querySelector(".fl-home-rail-controls"),scrollY,calls:window.__probe.scrollCalls.length})');
  assert.ok(Math.abs(position.target-position.margin)<=1,'Anchor at its actual scroll margin');await delay(1000);assert.ok(Math.abs(await page.cdp.evaluate('scrollY')-position.scrollY)<=1,'Anchor remains stable for one second');assert.equal(await page.cdp.evaluate('window.__probe.scrollCalls.length'),position.calls,'No second automatic positioning');page.assertConsole();
  const snap=await page.snapshot();if(width===1440)assert.ok(snap.requests.some(r=>r.url===origin+'/'+gsap[0]),'Desktop loads GSAP');
  results.push({width,fragment,position,console:snap.logs});
  if(width===1440&&fragment==='faq')for(const resizeWidth of [375,1440]){
   const beforeResize=await page.snapshot();await page.cdp.viewport(resizeWidth,resizeWidth===375?812:900);await page.settled();const afterResize=await page.snapshot();assert.equal(afterResize.scrollCalls.length,beforeResize.scrollCalls.length,'Resize does not trigger automatic positioning');await delay(1000);assert.ok(Math.abs(await page.cdp.evaluate('scrollY')-afterResize.scrollY)<=1,'Resize settles without another scroll');
   await page.cdp.evaluate('document.querySelector("#faq").scrollIntoView({behavior:"instant",block:"start"})');
   await page.cdp.waitFor('!!document.querySelector(\'a[href$="#faq"][aria-current="location"]\')');assert.equal(await page.cdp.evaluate('!!document.querySelector("dialog[open]")'),false);
   results.push({resizedTo:resizeWidth,menuClosed:true,active:'faq',beforeScrollY:beforeResize.scrollY,afterScrollY:afterResize.scrollY,automaticCalls:afterResize.scrollCalls.length-beforeResize.scrollCalls.length});
  }
  await page.close();page=null;
 }
 page=await probePage({origin,root,width:1440,height:900,reducedMotion:true});await page.release();await page.settled();page.assertConsole();const reduced=await page.snapshot();
 assert.equal(reduced.requests.some(r=>r.url===origin+'/'+gsap[0]),false,'Reduced motion must not request GSAP');results.push({reducedMotion:true,gsapRequested:false});
 await writeFile(path.join(values.output,'results.json'),JSON.stringify({root,gsap,results},null,2),{mode:0o600});console.log(JSON.stringify(results));
}catch(e){error=e;await writeFile(path.join(values.output,'failure.json'),JSON.stringify({results,diagnostic:await page?.diagnostic(e)},null,2),{mode:0o600});}
await finishProbe(error,[()=>page?.close(),()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));}]);
