import assert from 'node:assert/strict';
import { readFile,readdir } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { parseArgs } from 'node:util';
import { assertCsrShell,assertCsrContract } from '../functions/csr-shell.js';
import { hostingBuildId,clientesSiteId } from './prepare-clientes-site.mjs';
export async function checkClientesSite(root,marketing,configFile='firebase.json'){
 await assert.rejects(readFile(path.join(root,'index.html')),{code:'ENOENT'},'Root must reach customDomainPage, never a static index');
 const index=await readFile(path.join(root,'index.csr.html'),'utf8');assertCsrShell(index);
 assert.equal(index,await readFile(path.join(marketing,'index.csr.html'),'utf8'));
 assertCsrContract(index,await readFile(path.join(marketing,'index.html'),'utf8'));
 const expected='fluora-host:'+await clientesSiteId()+':'+await hostingBuildId(marketing)+'\n';
 assert.equal(await readFile(path.join(root,'fluora-host.txt'),'utf8'),expected);
 await assert.rejects(readFile(path.join(marketing,'fluora-host.txt')),{code:'ENOENT'});
 assert.doesNotMatch(await readFile(path.join(root,'robots.txt'),'utf8'),/^\s*Sitemap\s*:/im);
 const clientFiles=[];
 async function walk(folder){for(const e of await readdir(folder,{withFileTypes:true})){
  const file=path.join(folder,e.name);assert.ok(!e.isSymbolicLink());
  if(e.isDirectory())await walk(file);else {
   assert.ok(!e.name.endsWith('.map'),'No client sourcemaps');clientFiles.push(path.relative(root,file));
   if(e.name.endsWith('.html')){const html=await readFile(file,'utf8');assertCsrShell(html);assert.doesNotMatch(html,/id=["']home-title["']|La agenda en línea para salones, barberías y spas\./i);}
  }
 }}await walk(root);
 const sourceFiles=[];
 async function collect(folder){for(const e of await readdir(folder,{withFileTypes:true})){const file=path.join(folder,e.name);if(e.isDirectory())await collect(file);else sourceFiles.push(path.relative(marketing,file));}}await collect(marketing);
 assert.deepEqual(clientFiles.sort(),[...sourceFiles.filter(f=>f!=='index.html'),'fluora-host.txt'].sort(),'Exactly the same assets plus the client marker');
 for(const file of sourceFiles)if(!['index.html','index.csr.html','robots.txt'].includes(file))assert.deepEqual(await readFile(path.join(root,file)),await readFile(path.join(marketing,file)),'Client asset differs: '+file);
 const site=JSON.parse(await readFile(configFile,'utf8')).hosting.find(s=>s.target==='clientes');assert.ok(site);
 assert.equal(site.public,'dist/clientes/browser');assert.ok(site.predeploy.includes('node scripts/check-public-builds.cjs && node scripts/check-clientes-site.mjs'));
 assert.equal(site.headers.find(h=>h.source==='/index.csr.html')?.headers.find(h=>h.key==='X-Robots-Tag')?.value,'noindex, nofollow');
 assert.equal(site.rewrites.length,2);
 assert.equal(site.rewrites[0].source,'/api/**');assert.equal(site.rewrites[0].function.functionId,'apiProxy');
 assert.deepEqual(site.rewrites.at(-1),{source:'**',function:{functionId:'customDomainPage',region:'us-central1'}});
 for(const pattern of ['/','**/*.html','/runtime-config.js','/fluora-host.txt'])assert.match(site.headers.find(h=>h.source===pattern)?.headers.find(h=>h.key==='Cache-Control')?.value||'',/no-store/);
 return {csrBytes:Buffer.byteLength(index),siteId:await clientesSiteId(),buildId:expected.trim().split(':')[2]};
}
if(process.argv[1]&&import.meta.url===pathToFileURL(path.resolve(process.argv[1])).href){
 const {values}=parseArgs({options:{root:{type:'string',default:'dist/clientes/browser'},marketing:{type:'string',default:'dist/marketing/browser'},config:{type:'string',default:'firebase.json'}}});
 console.log(JSON.stringify(await checkClientesSite(values.root,values.marketing,values.config)));
}
