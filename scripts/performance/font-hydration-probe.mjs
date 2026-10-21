import { parseArgs } from 'node:util';
import { mkdir,writeFile } from 'node:fs/promises';
import path from 'node:path';
import { publicPreview } from './public-preview.mjs';
import { finishProbe } from './browser-probe.mjs';
import { probePage } from './probe-page.mjs';
import { checkHydrationProbe } from '../check-hydration-probe.mjs';
const {values}=parseArgs({options:{root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string',default:'/private/tmp/fluora-m3-r3-font-hold'},api:{type:'string',default:'http://localhost:8080'}}});
await checkHydrationProbe(path.resolve(values.root));await mkdir(values.output,{recursive:true,mode:0o700});
const root=path.join(values.root,'browser'),server=await publicPreview({root,apiOrigin:values.api,spaShell:'csr'}),results=[];let page,error;
try{
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});const origin='http://localhost:'+server.address().port;
 for(const width of [768,800,812,820,844,900,960,1000,1023,1024]){
  page=await probePage({origin,root,width,height:1180,scenario:'fontHold'});const fallback=page.before;
  await page.releaseFonts();const fonts=await page.snapshot();await page.release();await page.settled();const hydrated=await page.snapshot();page.assertConsole();
  results.push({width,fallback,fonts,hydrated,headingDelta:fonts.layout['.fl-home-preview-heading'].rect.height-fallback.layout['.fl-home-preview-heading'].rect.height});
  await writeFile(path.join(values.output,'results.json'),JSON.stringify({root,results},null,2),{mode:0o600});console.log(JSON.stringify({width,headingDelta:results.at(-1).headingDelta,beforeMainCls:fonts.cls,totalCls:hydrated.cls}));
  await page.close();page=null;
 }
}catch(e){error=e;await writeFile(path.join(values.output,'failure.json'),JSON.stringify({results,diagnostic:page?await page.diagnostic(e):e.diagnostic},null,2),{mode:0o600});}
await finishProbe(error,[()=>page?.close(),()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));}]);
