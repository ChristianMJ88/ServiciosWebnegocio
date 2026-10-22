import { parseArgs } from 'node:util';
import { mkdir,readFile,writeFile } from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import { setTimeout as delay } from 'node:timers/promises';
import { publicPreview } from './public-preview.mjs';
import { isolatedChrome,finishProbe } from './browser-probe.mjs';
import { checkMarketingCsr } from '../check-marketing-csr.mjs';
import { checkHydrationProbe } from '../check-hydration-probe.mjs';
import seo from '../../functions/seo-renderer.js';
import landingRenderer from '../../functions/landing-renderer.js';
import directoryRenderer from '../../functions/directory-renderer.js';

const {values}=parseArgs({options:{rendering:{type:'string',default:'prerender'},root:{type:'string',default:'dist/marketing-hydration-probe/production'},output:{type:'string'},api:{type:'string',default:'http://localhost:8080'},tenant:{type:'string'},logo:{type:'string',default:'real'}}});
if(!['real','null'].includes(values.logo))throw new Error('Invalid logo fixture');
if(!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(values.tenant||''))throw new Error('Provide an existing public tenant slug with --tenant');
if(!['csr','prerender'].includes(values.rendering))throw new Error('Unknown rendering mode');
if(values.rendering==='csr')await checkMarketingCsr(path.resolve(values.root));else await checkHydrationProbe(path.resolve(values.root));
const template=await readFile(path.join(values.root,'browser/index.csr.html'),'utf8'),main=template.match(/<script[^>]+src="([^"]*main-[^"]+\.js)"/)?.[1];assert.ok(main);
const mode=JSON.parse(await readFile(path.join(values.root,'render-origins.json'),'utf8')).mode;
values.output??=path.join('/private/tmp/fluora-m3-r3c-csr',mode+'-'+values.logo+'-'+Date.now());await mkdir(values.output,{recursive:true,mode:0o700});
const server=await publicPreview({root:path.join(values.root,'browser'),apiOrigin:values.api,spaShell:'csr'}),results=[],failures=[];let chrome,error,currentDiagnostic;
try{
 await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});const origin='http://localhost:'+server.address().port,appOrigin=origin.replace('localhost','127.0.0.1');
 const get=async route=>{const response=await fetch(origin+'/api/v1/publico/'+route,{signal:AbortSignal.timeout(20000)});assert.equal(response.ok,true,'Public API '+route);return response.json();};
 const [tenant,nav,directory]=await Promise.all([get('sitio/'+values.tenant),get('marketing/landings/navegacion'),get('marketplace')]);
 assert.ok(nav.length,'A published landing is needed to verify its CSR');const landing=await get('marketing/landings/'+encodeURIComponent(nav[0].slug));
 const tenantPath='/e/'+values.tenant;
 const rendered=[{name:values.logo==='real'?'tenant-renderer':'tenant-renderer-sin-logo',pathname:tenantPath,fixtureOverrides:values.logo==='null'?{logoUrl:null}:{},html:seo.renderTenantHtml(template,values.logo==='real'?tenant:{...tenant,logoUrl:null},tenantPath,origin)},
  {name:'landing-renderer',pathname:'/'+landing.slug,html:landingRenderer.renderLandingHtml(template,landing,origin,origin)},
  {name:'directory-renderer',pathname:'/negocios',html:directoryRenderer.renderDirectoryHtml(template,directory,origin)}];
 for(const fixture of [...['/acceso','/privacidad','/negocios','/'+landing.slug].map(pathname=>({name:pathname,pathname})),...rendered]){
  chrome=await isolatedChrome();const {cdp}=chrome,logs=[],exceptions=[],pending=new Set(),publicApiOverrides=[];let held,lastActivity=Date.now(),lastDocument=Date.now(),redirectState,redirectUrl,documents=0,appDocuments=0;
  const readState='({features:performance.getEntriesByType("mark").filter(e=>e.name==="mark_feature_usage").map(e=>e.detail?.feature),sentinel:!!document.querySelector("[data-probe-csr]"),firstChildRemoved:!window.__csr.child||!window.__csr.child.isConnected,path:location.pathname,redirectComponent:!!document.querySelector("app-platform-redirect"),images:[...document.images].map(i=>({src:i.getAttribute("src"),naturalWidth:i.naturalWidth,naturalHeight:i.naturalHeight,width:i.getBoundingClientRect().width,height:i.getBoundingClientRect().height}))})';
  currentDiagnostic=()=>({name:fixture.name,logs,exceptions,documents,appDocuments,redirectUrl,redirectState,held});
  await cdp.send('Runtime.addBinding',{name:'probeBeforeRedirect'});cdp.on('Runtime.bindingCalled',e=>{if(e.name==='probeBeforeRedirect')redirectState=JSON.parse(e.payload);});
  cdp.on('Runtime.consoleAPICalled',e=>logs.push({type:e.type,text:e.args.map(a=>a.value??a.description??'').join(' ')}));
  cdp.on('Log.entryAdded',e=>logs.push({type:e.entry.level,text:e.entry.text}));cdp.on('Runtime.exceptionThrown',e=>exceptions.push(e.exceptionDetails.exception?.description||e.exceptionDetails.text));
  cdp.on('Network.requestWillBeSent',e=>{if(e.request.url.includes('/api/')){pending.add(e.requestId);lastActivity=Date.now();}});
  for(const event of ['Network.loadingFinished','Network.loadingFailed'])cdp.on(event,e=>{if(pending.delete(e.requestId))lastActivity=Date.now();});
  const mainUrl=new URL(main,origin+'/').href,documentUrl=origin+fixture.pathname;
  cdp.on('Fetch.requestPaused',e=>{
   if(e.resourceType==='Document'&&e.request.url===documentUrl){documents++;lastDocument=Date.now();}
   if(e.request.url===mainUrl){held=e.requestId;return;}
   if(e.request.url===origin+'/runtime-config.js'){cdp.send('Fetch.fulfillRequest',{requestId:e.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/javascript'}],body:Buffer.from('window.__AGENDA_API_BASE_URL__="/api/v1";window.__FLUORA_APP_ORIGIN__='+JSON.stringify(appOrigin)+';window.__FLUORA_MARKETING_ORIGIN__='+JSON.stringify(origin)+';').toString('base64')}).catch(e=>exceptions.push(e.message));return;}
   if(e.request.url.startsWith(appOrigin+'/')){appDocuments++;lastDocument=Date.now();redirectUrl=e.request.url;return;}
   if(values.logo==='null'&&new URL(e.request.url).pathname==='/api/v1/publico/marketplace'){
    fetch(e.request.url,{signal:AbortSignal.timeout(20000)}).then(async response=>{assert.equal(response.ok,true);const original=await response.json();const derived={...original,negocios:original.negocios.map(b=>({...b,imagenUrl:null}))};publicApiOverrides.push({path:'/api/v1/publico/marketplace',field:'negocios[*].imagenUrl',value:null,count:derived.negocios.length});await cdp.send('Fetch.fulfillRequest',{requestId:e.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from(JSON.stringify(derived)).toString('base64')});}).catch(error=>{exceptions.push(error.message);cdp.send('Fetch.failRequest',{requestId:e.requestId,errorReason:'Failed'}).catch(()=>{});});return;
   }
   if(e.resourceType==='Document'&&fixture.html){const html=fixture.html.replace(/<app-root([^>]*)>/,'<app-root$1><span data-probe-csr>CSR sentinel</span>');cdp.send('Fetch.fulfillRequest',{requestId:e.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'text/html;charset=utf-8'}],body:Buffer.from(html).toString('base64')}).catch(e=>exceptions.push(e.message));}
   else cdp.send('Fetch.continueRequest',{requestId:e.requestId}).catch(e=>exceptions.push(e.message));
  });
  await cdp.viewport(375,812);await cdp.send('Fetch.enable',{patterns:[{urlPattern:mainUrl},{urlPattern:origin+'/runtime-config.js'},{urlPattern:appOrigin+'/*',resourceType:'Document'},{urlPattern:documentUrl,resourceType:'Document'},...(values.logo==='null'?[{urlPattern:origin+'/api/v1/publico/marketplace*'}]:[])]});
  await cdp.send('Page.addScriptToEvaluateOnNewDocument',{source:'window.__csr={};new MutationObserver(()=>{const r=document.querySelector("app-root");if(r&&!window.__csr.root){window.__csr.root=r;window.__csr.child=r.firstElementChild;}}).observe(document,{childList:true,subtree:true});window.addEventListener("beforeunload",()=>window.probeBeforeRedirect(JSON.stringify('+readState+')));'});
  const navigation=await cdp.send('Page.navigate',{url:documentUrl});assert.equal(navigation.errorText,undefined);
  for(let n=0;n<200&&!held;n++)await delay(50);assert.ok(held);
  await cdp.send('Fetch.continueRequest',{requestId:held});
  let state;
  if(fixture.pathname==='/acceso'){
   const deadline=Date.now()+20000;while(Date.now()<deadline&&(!redirectState||!redirectUrl))await delay(50);assert.ok(redirectState,'CSR redirect component must navigate to the separate application origin');while(Date.now()-lastDocument<1000&&Date.now()<deadline)await delay(50);assert.equal(redirectUrl,appOrigin+'/acceso');assert.equal(redirectState.redirectComponent,true);assert.equal(appDocuments,1);assert.equal(documents,1);if(mode==='development')assert.equal(logs.filter(l=>l.text==='Angular is running in development mode.').length,1);state=redirectState;
  }else{
   await cdp.waitFor('!!document.querySelector("app-root[ng-version]")&&!!document.querySelector("router-outlet")');
   const deadline=Date.now()+20000;while(Date.now()<deadline&&(pending.size||Date.now()-lastActivity<700||Date.now()-lastDocument<1000))await delay(50);assert.equal(pending.size,0);assert.equal(appDocuments,0);assert.equal(documents,1);await cdp.evaluate('new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(()=>r(true))))');state=await cdp.evaluate(readState);
  }
  const item={name:fixture.name,mode,logoFixture:values.logo,runtimeConfig:'intercepted',origins:{marketingOrigin:origin,appOrigin},fixtureOverrides:fixture.fixtureOverrides,publicApiOverrides,...state,documents,appDocuments,logs,exceptions};results.push(item);await writeFile(path.join(values.output,'results-'+Date.now()+'.json'),JSON.stringify(results,null,2),{mode:0o600});
  assert.equal(state.features.includes('NgHydration'),false);assert.equal(state.sentinel,false);if(fixture.html)assert.equal(state.firstChildRemoved,true);
  assert.deepEqual(exceptions,[]);const warnings=logs.filter(l=>['error','warning','warn'].includes(l.type));if(warnings.length)failures.push(new Error(fixture.name+': '+JSON.stringify(warnings)));console.log(JSON.stringify(item));
  await chrome.close();chrome=null;
 }
 if(failures.length)throw new AggregateError(failures,'CSR console is not clean; no warnings were suppressed');
}catch(e){error=e;await writeFile(path.join(values.output,'failure.json'),JSON.stringify({error:e.stack,results,current:currentDiagnostic?.()},null,2),{mode:0o600});}
await finishProbe(error,[()=>chrome?.close(),()=>{server.closeAllConnections();return new Promise(resolve=>server.close(resolve));}]);
