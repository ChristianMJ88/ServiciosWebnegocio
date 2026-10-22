import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
import { test } from 'node:test';

const source = file => readFile(new URL('../'+file,import.meta.url),'utf8');

test('hydration is declared once in the dedicated marketing config, without HTTP transfer cache',async()=>{
  const config=await source('src/app/app.config.marketing.ts');
  assert.equal([...config.matchAll(/\bprovideClientHydration\(/g)].length,1);
  assert.match(config,/provideClientHydration\(withEventReplay\(\), withNoHttpTransferCache\(\)\)/);
  assert.match(config,/hydrate \? \[provideMarketingHydration\(\)\] : \[\]/);
  assert.doesNotMatch(config,/withIncrementalHydration|withHttpTransferCacheOptions/);
});

test('general and application bootstraps cannot opt into marketing hydration or render context',async()=>{
  for(const file of ['src/main.ts','src/main.application.ts','src/app/app.config.ts','src/app/app.config.base.ts']){
    assert.doesNotMatch(await source(file),/provideClientHydration|createMarketingAppConfig|MARKETING_RENDER_CONTEXT|provideServerRendering/,file);
  }
  assert.match(await source('src/main.marketing.ts'),/createMarketingAppConfig\(isHydratableMarketingHome\(/);
  for(const file of ['src/main.marketing.ts','src/app/app.config.marketing.ts'])assert.doesNotMatch(await source(file),/app\.config\.marketing\.server|@angular\/ssr|platform-server/,file);
});

test('production and diagnostic share the single marketing prerender configuration',async()=>{
  const pkg=JSON.parse(await source('package.json')),angular=JSON.parse(await source('angular.json'));
  assert.equal(pkg.scripts.build,'npm run build:marketing && npm run build:app && npm run build:clientes');
  const marketing=angular.projects.ServiciosWebnegocio.architect.build.configurations.marketing;
  assert.equal(marketing.server,'src/main.marketing.server.ts');assert.equal(marketing.outputMode,'static');assert.equal(marketing.tsConfig,'tsconfig.marketing.server.json');
  assert.equal(angular.projects.ServiciosWebnegocio.architect.build.configurations['marketing-prerender'],undefined);
  assert.equal(angular.projects.ServiciosWebnegocio.architect.build.configurations['marketing-hydration-probe'],undefined);
  assert.equal(pkg.scripts['build:marketing'],'node scripts/build-marketing.mjs');
  assert.match(pkg.scripts['postbuild:marketing'],/prepare-csr-shell\.mjs --root dist\/marketing\/browser/);
  assert.match(pkg.scripts['postbuild:marketing'],/check-hydration-probe\.mjs dist\/marketing --production/);
  assert.doesNotMatch(pkg.scripts['postbuild:marketing'],/check-marketing-csr/);
  assert.match(await source('scripts/build-marketing.mjs'),/mode \+ ',marketing'/);
  assert.match(await source('scripts/build-marketing-hydration-probe.mjs'),/buildMarketing\(/);
  assert.doesNotMatch(await source('firebase.json'),/hydration-probe/);
});

 test('diagnostics cannot overwrite, nest inside or contain production output',async()=>{
   const {buildMarketing}=await import('./build-marketing.mjs');
   for(const output of ['dist/marketing','dist/marketing/browser','dist','.']){
     await assert.rejects(buildMarketing({output,mode:'development'}),/must not overlap production/);
     if(output!=='dist/marketing')await assert.rejects(buildMarketing({output}),/must not overlap production/);
   }
 });
