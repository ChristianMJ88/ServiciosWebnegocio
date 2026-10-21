import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp,writeFile,rm,readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import origins from './lib/marketing-origins.cjs';

test('public runtime is the single source of production origins, without process or require',async()=>{
 const root=await mkdtemp(path.join(tmpdir(),'fluora-origins-'));
 try{const file=path.join(root,'runtime.js');await writeFile(file,'if(typeof process!=="undefined"||typeof require!=="undefined")throw Error();window.__FLUORA_MARKETING_ORIGIN__="https://marketing.test";window.__FLUORA_APP_ORIGIN__="https://app.test";');
  assert.deepEqual(origins.readMarketingOrigins(file),{marketingOrigin:'https://marketing.test',appOrigin:'https://app.test'});
 }finally{await rm(root,{recursive:true,force:true});}
});
for(const value of [undefined,'http://marketing.test','https://marketing.test/','https://user:private-value@marketing.test','https://marketing.test/path','https://build.invalid']){
 test('rejects missing, noncanonical or insecure configuration '+String(value),()=>{
  assert.throws(()=>origins.validateMarketingOrigins({marketingOrigin:value,appOrigin:'https://app.test'}),{message:'Invalid marketing build origins'});
 });
}
test('rejects distinct ports sharing a hostname and equal origins',()=>{
 for(const appOrigin of ['https://marketing.test','https://marketing.test:444'])assert.throws(()=>origins.validateMarketingOrigins({marketingOrigin:'https://marketing.test',appOrigin}));
});
test('probe config contains neither define nor duplicate domain literals; production stays CSR',async()=>{
 const config=JSON.parse(await readFile(new URL('../angular.json',import.meta.url),'utf8')).projects.ServiciosWebnegocio.architect.build.configurations;
 assert.equal(config['marketing-hydration-probe'].define,undefined);assert.doesNotMatch(JSON.stringify(config['marketing-hydration-probe']),/https?:\/\//);
 assert.equal(config['marketing-hydration-probe'].outputMode,'static');
 for(const key of ['prerender','ssr','appShell'])assert.equal(Object.hasOwn(config['marketing-hydration-probe'],key),false);
 assert.equal(config.marketing.outputMode,undefined);assert.equal(config.application.server,undefined);
});
