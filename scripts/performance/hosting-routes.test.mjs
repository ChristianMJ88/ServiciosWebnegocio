import {test} from 'node:test';import assert from 'node:assert/strict';
import {mkdtemp,writeFile,rm} from 'node:fs/promises';import {tmpdir} from 'node:os';import path from 'node:path';
import {publicPreview,matchesHostingPath} from './public-preview.mjs';
test('Hosting glob examples',()=>{assert.equal(matchesHostingPath('/barberias{,/**}','/barberias'),true);assert.equal(matchesHostingPath('/barberias{,/**}','/barberias/a'),true);assert.equal(matchesHostingPath('/barberias{,/**}','/barberias-extra'),false);});
for(const target of ['marketing','clientes'])test('static precedence and rewrite dispatch for '+target,async t=>{
 const root=await mkdtemp(path.join(tmpdir(),'fluora-hosting-table-'));t.after(()=>rm(root,{recursive:true,force:true}));
 const shell='<html><head><base href="/"><link rel="stylesheet" href="styles-A.css"><script src="/runtime-config.js"></script></head><body><app-root></app-root><script type="module" src="main-A.js"></script></body></html>';
 const home=target==='marketing'?shell.replace('</app-root>','<h1>Prerender</h1></app-root>'):shell;
 if(target==='marketing')await writeFile(path.join(root,'index.html'),home);await writeFile(path.join(root,'index.csr.html'),shell);
 if(target==='clientes')await writeFile(path.join(root,'fluora-host.txt'),'fluora-host:fixture-clientes:'+'a'.repeat(64)+'\n');
 const calls=[];const server=await publicPreview({root,apiOrigin:'http://localhost:8080',hostingTarget:target,spaShell:target==='marketing'?'csr':'index',functionHandler:async(name,req,res)=>{calls.push(name);res.setHeader('content-type','text/html');res.end(name);}});
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));t.after(async()=>{server.closeAllConnections();await new Promise(resolve=>server.close(resolve));});
 const origin='http://127.0.0.1:'+server.address().port;
 for(const route of ['/','/acceso','/privacidad','/inexistente','/e/fixture/servicios','/barberias','/negocios','/sitemap.xml']){
  const body=await(await fetch(origin+route,{headers:{Accept:'text/html'}})).text();
  const expected=target==='marketing'?({'/e/fixture/servicios':'tenantPage','/barberias':'landingPage','/negocios':'directoryPage','/sitemap.xml':'sitemap'}[route]||(route==='/'?home:shell)):'customDomainPage';
  assert.equal(body,expected,route);
 }
 if(target==='clientes'){assert.deepEqual(calls,Array(8).fill('customDomainPage'));assert.match(await(await fetch(origin+'/fluora-host.txt')).text(),/^fluora-host:fixture-clientes:[a-f0-9]{64}\n$/);}
 else assert.deepEqual(calls,['tenantPage','landingPage','directoryPage','sitemap']);
 if(target==='marketing')assert.equal((await fetch(origin+'/missing.js')).status,404);
 else {const before=calls.length;assert.equal(await(await fetch(origin+'/missing.js',{headers:{Accept:'*/*'}})).text(),'customDomainPage');assert.equal(calls.length,before+1);assert.equal(calls.at(-1),'customDomainPage');}
});
