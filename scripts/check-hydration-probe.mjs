import assert from 'node:assert/strict';
import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { JSDOM } from 'jsdom';
import csrTools from '../functions/csr-shell.js';
import originsTools from './lib/marketing-origins.cjs';

export async function checkHydrationProbe(root, runtimeFile=path.resolve('public/runtime-config.js')) {
  const origins=originsTools.readMarketingOrigins(runtimeFile);
  const manifest=JSON.parse(await readFile(path.join(root,'render-origins.json'),'utf8'));
  assert.equal(manifest.marketingOrigin,origins.marketingOrigin);assert.equal(manifest.appOrigin,origins.appOrigin);
  const routes=JSON.parse(await readFile(path.join(root,'prerendered-routes.json'),'utf8'));
  assert.deepEqual(Object.keys(routes.routes),['/'],'Only the marketing root may be prerendered');
  const browser=path.join(root,'browser');
  const html=await readFile(path.join(browser,'index.html'),'utf8'),csr=await readFile(path.join(browser,'index.csr.html'),'utf8');
  csrTools.assertCsrContract(csr,html);
  const page=new JSDOM(html),shell=new JSDOM(csr);
  try {
    const d=page.window.document;
    assert.ok(d.querySelector('app-root[ngh][ng-server-context="ssg"]'));
    assert.ok(d.querySelector('h1#home-title')?.textContent?.trim());
    assert.ok(d.querySelector('.fl-home-hero-lead')?.textContent?.trim());
    assert.equal(d.querySelector('.fl-home-hero-actions .fl-button')?.textContent?.trim(),'Crear mi cuenta');
    const cta=new URL(d.querySelector('.fl-home-hero-actions .fl-button').getAttribute('href'));
    assert.equal(cta.origin,origins.appOrigin);assert.equal(cta.pathname,'/registro');
    assert.ok(html.includes('nghm')&&html.includes('_ejsas'),'Hydration integrity and replay must be present');
    assert.equal(d.body.hasAttribute('ngcm'),false);
    assert.equal(shell.window.document.body.hasAttribute('ngcm'),true);
    assert.equal(d.querySelector('.fl-home-plan-card,.fl-home-trial-banner,[data-fluora-support],.fl-home-hero-verticals a'),null);
    assert.doesNotMatch(html,/mailto:|tel:|Prueba sin tarjeta|localhost|127\.0\.0\.1/);
    const states=[...d.querySelectorAll('script[type="application/json"]')];
    assert.equal(states.length,1);assert.equal(states[0].id,'ng-state');
    const state=JSON.parse(states[0].textContent);
    assert.ok(Array.isArray(state.__nghData__));
    assert.ok(Object.keys(state).every(key=>['__nghData__','__nghDeferData__'].includes(key)),'No HTTP or business transfer state');
    const checkUrl=value=>{if(!value||! /^(?:https?:|\/\/)/i.test(value.trim()))return;
      const url=new URL(value,origins.marketingOrigin);
      assert.equal(url.protocol,'https:');assert.ok([origins.marketingOrigin,origins.appOrigin].includes(url.origin),'Unexpected absolute origin');};
    for(const element of d.querySelectorAll('[href],[src],[action],[srcset],meta[content]')){
      for(const name of ['href','src','action','content'])checkUrl(element.getAttribute(name));
      for(const candidate of (element.getAttribute('srcset')||'').split(','))checkUrl(candidate.trim().split(/\s+/)[0]);
    }
    assert.doesNotMatch(html,/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|\bsk_(?:live|test)_[A-Za-z0-9]{20,}|\bAPP_USR-[0-9]{10,}-[A-Za-z0-9-]{20,}/);
    const emitted=[];
    async function collect(folder){for(const item of await readdir(folder,{withFileTypes:true})){
      const file=path.join(folder,item.name);if(item.isDirectory())await collect(file);else if(item.name.endsWith('.html'))emitted.push(path.relative(browser,file));
    }}
    await collect(browser);assert.deepEqual(emitted.sort(),['index.csr.html','index.html']);
    return {routes:['/'],stateKeys:Object.keys(state),htmlBytes:Buffer.byteLength(html),csrBytes:Buffer.byteLength(csr),origins};
  }finally{page.window.close();shell.window.close();}
}

if(process.argv[1]&&import.meta.url===pathToFileURL(path.resolve(process.argv[1])).href){
  const root=process.argv[2];if(!root)throw new Error('A probe output root is required');
  console.log(JSON.stringify(await checkHydrationProbe(path.resolve(root))));
}
