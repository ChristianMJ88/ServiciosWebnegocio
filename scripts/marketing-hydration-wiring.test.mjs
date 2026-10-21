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

test('production build and Hosting have no hydration probe or SSR configuration in R3-a',async()=>{
  const pkg=JSON.parse(await source('package.json')),angular=JSON.parse(await source('angular.json'));
  assert.equal(pkg.scripts.build,'npm run build:marketing && npm run build:app');
  const marketing=angular.projects.ServiciosWebnegocio.architect.build.configurations.marketing;
  assert.equal(marketing.server,undefined);assert.equal(marketing.outputMode,undefined);assert.equal(marketing.prerender,undefined);
  assert.doesNotMatch(await source('firebase.json'),/hydration-probe/);
});
