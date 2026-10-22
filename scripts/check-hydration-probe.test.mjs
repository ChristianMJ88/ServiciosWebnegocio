import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp,mkdir,writeFile,rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { checkHydrationProbe } from './check-hydration-probe.mjs';

const state='<script id="ng-state" type="application/json">{"__nghData__":[]}</script>';
const content='<app-root ngh="0" ng-server-context="ssg"><h1 id="home-title">La agenda en línea para salones, barberías y spas.</h1><p class="fl-home-hero-lead">Reservas y ventas en tu sitio. Equipo y cobros en una app.</p><div class="fl-home-hero-actions"><a class="fl-button" href="https://app.test/registro">Crear mi cuenta</a><a class="fl-home-hero-secondary" href="#como-funciona">Ver cómo funciona</a></div><nav class="fl-home-hero-verticals"></nav></app-root><!--nghm--><script>var _ejsas={};</script>'+state;
const document=(body,attribute='')=>'<html><head><base href="/"><link rel="stylesheet" href="styles-test.css"><script src="runtime-config.js"></script></head><body '+attribute+'>'+body+'<script type="module" src="main-test.js"></script></body></html>';
async function fixture(t){
 const root=await mkdtemp(path.join(tmpdir(),'fluora-hydration-contract-'));t.after(()=>rm(root,{recursive:true,force:true}));
 await mkdir(path.join(root,'browser'));
 const origins={marketingOrigin:'https://marketing.test',appOrigin:'https://app.test'};
 const runtime=path.join(root,'runtime.js');await writeFile(runtime,'window.__FLUORA_MARKETING_ORIGIN__="https://marketing.test";window.__FLUORA_APP_ORIGIN__="https://app.test";');
 await writeFile(path.join(root,'render-origins.json'),JSON.stringify({mode:'production',shell:'prerender',...origins}));
 await writeFile(path.join(root,'browser/runtime-config.js'),await import('node:fs/promises').then(fs=>fs.readFile(runtime,'utf8')));
 await writeFile(path.join(root,'prerendered-routes.json'),JSON.stringify({routes:{'/':{}}}));
 await writeFile(path.join(root,'browser/index.html'),document(content));
 await writeFile(path.join(root,'browser/index.csr.html'),document('<app-root></app-root>','ngcm'));
 return {root,runtime,async html(value){await writeFile(path.join(root,'browser/index.html'),document(value));}};
}
test('accepts a root-only hydration artifact and matching empty CSR fallback',async t=>{
 const f=await fixture(t);assert.deepEqual((await checkHydrationProbe(f.root,f.runtime)).stateKeys,['__nghData__']);
});
for(const [name,transform] of [
 ['missing approved title',s=>s.replace('La agenda en línea para salones, barberías y spas.','Other title')],
 ['plan price class',s=>s+'<p class="fl-home-plan-price">Amount</p>'],
 ['tenant bootstrap',s=>s+'<script>window.__FLUORA_TENANT__={}</script>'],
 ['tenant link',s=>s+'<a href="/e/example">Tenant</a>'],
 ['absolute tenant link',s=>s+'<a href="https://marketing.test/e/example">Tenant</a>'],
 ['dynamic trial copy',s=>s+'<p>Prueba un plan durante 14 días. Sin tarjeta.</p>'],
 ['trial property',s=>s+'<p>diasPrueba</p>'],
 ['price amount',s=>s+'<p>$100.00</p>'],
 ['currency',s=>s+'<p>MXN</p>'],
 ['monthly rate',s=>s+'<p>/mes</p>'],
 ['missing approved lead',s=>s.replace('Reservas y ventas en tu sitio. Equipo y cobros en una app.','Other lead')],
 ['missing category container',s=>s.replace('<nav class="fl-home-hero-verticals"></nav>','')],
 ['HTTP transfer state',s=>s.replace('"__nghData__":[]','"__nghData__":[],"http-response":{"price":10}')],
 ['business content',s=>s+'<div class="fl-home-plan-card">Plan</div>'],
 ['missing replay',s=>s.replace('_ejsas','other')],
 ['foreign origin',s=>s+'<a href="https://foreign.test/">x</a>'],
 ['missing root hydration',s=>s.replace('ngh="0"','')],
 ['missing state',s=>s.replace(state,'')],
 ['relative CTA',s=>s.replace('https://app.test/registro','/registro')],
 ['foreign CTA',s=>s.replace('https://app.test/registro','https://foreign.test/registro')],
 ['trial copy',s=>s+'<p>Prueba sin tarjeta</p>'],
 ['localhost',s=>s+'<p>localhost</p>'],
 ['duplicate state',s=>s+state],
 ['protocol-relative foreign URL',s=>s+'<img src="//foreign.test/image.png">'],
 ['uppercase foreign URL',s=>s+'<a href="HTTPS://FOREIGN.TEST/">x</a>'],
 ['foreign srcset',s=>s+'<img srcset="/one.png 1x, https://foreign.test/two.png 2x">']
])test('rejects '+name,async t=>{const f=await fixture(t);await f.html(transform(content));await assert.rejects(checkHydrationProbe(f.root,f.runtime));});
test('rejects extra emitted routes',async t=>{const f=await fixture(t);await writeFile(path.join(f.root,'prerendered-routes.json'),JSON.stringify({routes:{'/':{},'/negocios':{}}}));await assert.rejects(checkHydrationProbe(f.root,f.runtime));});
test('rejects CSR carrying hydration state',async t=>{const f=await fixture(t);await writeFile(path.join(f.root,'browser/index.csr.html'),document(content,'ngcm'));await assert.rejects(checkHydrationProbe(f.root,f.runtime));});
test('rejects extra HTML even with root-only manifest',async t=>{const f=await fixture(t);await mkdir(path.join(f.root,'browser/negocios'));await writeFile(path.join(f.root,'browser/negocios/index.html'),'extra');await assert.rejects(checkHydrationProbe(f.root,f.runtime));});
test('rejects a manifest differing from runtime',async t=>{const f=await fixture(t);await writeFile(path.join(f.root,'render-origins.json'),JSON.stringify({mode:'production',marketingOrigin:'https://marketing.test',appOrigin:'https://other.test'}));await assert.rejects(checkHydrationProbe(f.root,f.runtime));});
test('rejects ngcm on the server document',async t=>{const f=await fixture(t);await writeFile(path.join(f.root,'browser/index.html'),document(content,'ngcm'));await assert.rejects(checkHydrationProbe(f.root,f.runtime));});
test('rejects missing ngcm on CSR',async t=>{const f=await fixture(t);await writeFile(path.join(f.root,'browser/index.csr.html'),document('<app-root></app-root>'));await assert.rejects(checkHydrationProbe(f.root,f.runtime));});

for(const shell of [undefined,'csr'])test('rejects non-prerender manifest '+String(shell),async t=>{const f=await fixture(t);const {readFile}=await import('node:fs/promises');const file=path.join(f.root,'render-origins.json'),manifest=JSON.parse(await readFile(file));if(shell===undefined)delete manifest.shell;else manifest.shell=shell;await writeFile(file,JSON.stringify(manifest));await assert.rejects(checkHydrationProbe(f.root,f.runtime),/Prerender manifest required/);});
