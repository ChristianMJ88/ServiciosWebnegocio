import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp,mkdir,writeFile,readFile,rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { readMarketingOrigins } from './lib/marketing-origins.cjs';

const runtime=await readFile('public/runtime-config.js','utf8'),origins=readMarketingOrigins('public/runtime-config.js');
const shell='<html><head><base href="/"><link rel="stylesheet" href="styles-test.css"><script src="/runtime-config.js"></script></head><body ngcm><app-root></app-root><script type="module" src="main-test.js"></script></body></html>';
const home=shell.replace('<body ngcm>','<body>').replace('<app-root></app-root>',`<app-root ngh="0" ng-server-context="ssg"><h1 id="home-title">La agenda en línea para salones, barberías y spas.</h1><p class="fl-home-hero-lead">Reservas y ventas en tu sitio. Equipo y cobros en una app.</p><div class="fl-home-hero-actions"><a class="fl-button" href="${origins.appOrigin}/registro">Crear mi cuenta</a><a class="fl-home-hero-secondary" href="#como-funciona">Ver cómo funciona</a></div><nav class="fl-home-hero-verticals"></nav></app-root><!--nghm--><script>var _ejsas={};</script><script id="ng-state" type="application/json">{"__nghData__":[]}</script>`);
for(const scenario of ['valid','valid-empty-routes','malformed-routes','routes-missing-key','routes-extra-key','routes-array','routes-directory','missing-shell','byte-drift','nested-html','hydrated-index','prerender-routes','server','prerender-manifest','extra-html','wrong-origin','development','sourcemap','runtime-drift','polluted-csr','stale-assets','missing-manifest'])test('archived CSR guard: '+scenario,async t=>{
 const root=await mkdtemp(path.join(tmpdir(),'fluora-p-guard-'));t.after(()=>rm(root,{recursive:true,force:true}));
 const marketing=path.join(root,'marketing'),browser=path.join(marketing,'browser'),app=path.join(root,'application/browser');
 await mkdir(browser,{recursive:true});await mkdir(app,{recursive:true});
 for(const folder of [browser,app]){
  await writeFile(path.join(folder,'index.html'),folder===browser&&scenario==='hydrated-index'?home:shell);
  await writeFile(path.join(folder,'styles-test.css'),'@keyframes spinner-border{to{transform:rotate(360deg)}}');
  await writeFile(path.join(folder,'main-test.js'),'/* browser fixture */');
 }
 await writeFile(path.join(browser,'index.csr.html'),scenario==='polluted-csr'?home:scenario==='stale-assets'?shell.replace('main-test','main-old'):shell);
 await writeFile(path.join(browser,'tenant-public.css'),'/* tenant */');
 await writeFile(path.join(browser,'runtime-config.js'),scenario==='runtime-drift'?runtime+'\n// unreviewed change\n':runtime);
 if(scenario==='prerender-routes')await writeFile(path.join(marketing,'prerendered-routes.json'),JSON.stringify({routes:{'/':{}}}));
 if(scenario==='valid-empty-routes')await writeFile(path.join(marketing,'prerendered-routes.json'),JSON.stringify({routes:{}}));
 if(scenario==='malformed-routes')await writeFile(path.join(marketing,'prerendered-routes.json'),'{');
 if(scenario==='routes-directory')await mkdir(path.join(marketing,'prerendered-routes.json'));
 const wrongRoutes={'routes-missing-key':{},'routes-extra-key':{routes:{},extra:1},'routes-array':{routes:[]}};
 if(Object.hasOwn(wrongRoutes,scenario))await writeFile(path.join(marketing,'prerendered-routes.json'),JSON.stringify(wrongRoutes[scenario]));
 if(scenario==='nested-html'){await mkdir(path.join(browser,'negocios'));await writeFile(path.join(browser,'negocios/index.html'),shell);}
 if(scenario==='byte-drift')await writeFile(path.join(browser,'index.csr.html'),shell+' ');
 if(scenario==='server')await mkdir(path.join(marketing,'server'));
 if(scenario==='extra-html')await writeFile(path.join(browser,'other.html'),shell);
 if(scenario!=='missing-manifest')await writeFile(path.join(marketing,'render-origins.json'),JSON.stringify({mode:scenario==='development'?'development':'production',shell:scenario==='prerender-manifest'?'prerender':'csr',...origins,...(scenario==='wrong-origin'?{appOrigin:'https://other.test'}:{})}));
 if(scenario==='missing-shell')await writeFile(path.join(marketing,'render-origins.json'),JSON.stringify({mode:'production',...origins}));
 if(scenario==='sourcemap')await writeFile(path.join(browser,'main-test.js.map'),'{}');
 const run=spawnSync(process.execPath,['scripts/check-marketing-csr.mjs',marketing],{encoding:'utf8'});
 if(scenario==='valid'||scenario==='valid-empty-routes')assert.equal(run.status,0,run.stderr);else assert.notEqual(run.status,0);
});
