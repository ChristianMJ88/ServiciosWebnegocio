// Reversible visual proposal only: style is injected into the isolated test page.
import { parseArgs } from 'node:util';
import { mkdir,writeFile } from 'node:fs/promises';
import path from 'node:path';
import { publicPreview } from './public-preview.mjs';
import { finishProbe } from './browser-probe.mjs';
import { probePage } from './probe-page.mjs';
import { checkHydrationProbe } from '../check-hydration-probe.mjs';
const {values}=parseArgs({options:{root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string',default:'/private/tmp/fluora-m3-r3-caption-preview'},api:{type:'string',default:'http://localhost:8080'}}});
await checkHydrationProbe(path.resolve(values.root));await mkdir(values.output,{recursive:true,mode:0o700});
const root=path.join(values.root,'browser'),server=await publicPreview({root,apiOrigin:values.api,spaShell:'csr'}),results=[];let page,error;
try{
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});const origin='http://localhost:'+server.address().port;
 for(const width of [820,1000]){
  page=await probePage({origin,root,width,height:1180});await page.release();await page.settled();page.assertConsole();
  const read=()=>page.cdp.evaluate('Object.fromEntries([".fl-home-hero",".fl-home-preview-heading",".fl-home-preview-caption"].map(s=>[s,document.querySelector(s).getBoundingClientRect().toJSON()]))');
  const before=await read();let screenshot=await page.cdp.send('Page.captureScreenshot',{format:'png'});await writeFile(path.join(values.output,width+'-before.png'),Buffer.from(screenshot.data,'base64'),{mode:0o600});
  await page.cdp.evaluate('(()=>{const style=document.createElement("style");style.textContent="@media(min-width:768px) and (max-width:1023px){.fl-home-preview-caption{flex-basis:100%}}";document.head.append(style);})()');await page.settled();
  const after=await read();screenshot=await page.cdp.send('Page.captureScreenshot',{format:'png'});await writeFile(path.join(values.output,width+'-proposal.png'),Buffer.from(screenshot.data,'base64'),{mode:0o600});
  results.push({width,before,after,heroIncrease:after['.fl-home-hero'].height-before['.fl-home-hero'].height});await page.close();page=null;
 }
 await writeFile(path.join(values.output,'measurements.json'),JSON.stringify(results,null,2),{mode:0o600});console.log(JSON.stringify(results));
}catch(e){error=e;}
await finishProbe(error,[()=>page?.close(),()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));}]);
