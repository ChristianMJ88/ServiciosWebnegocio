import { spawn } from 'node:child_process';
import { mkdir, writeFile, rm } from 'node:fs/promises';
import { parseArgs } from 'node:util';
import path from 'node:path';
import { readMarketingOrigins } from './lib/marketing-origins.cjs';

const {values}=parseArgs({options:{mode:{type:'string',default:'production'}}});
if(!['production','development'].includes(values.mode))throw new Error('Invalid probe build mode');
const origins=readMarketingOrigins(path.resolve('public/runtime-config.js'));
const output=path.resolve('dist/marketing-hydration-probe',values.mode);
// A failed build must not leave an old artifact eligible for verification.
await rm(path.join(output,'render-origins.json'),{force:true});
const args=[path.resolve('node_modules/@angular/cli/bin/ng.js'),'build',
  '--configuration='+values.mode+',marketing,marketing-hydration-probe','--output-path='+output,
  '--output-hashing=all',
  '--define','FLUORA_RENDER_MARKETING_ORIGIN='+JSON.stringify(origins.marketingOrigin),
  '--define','FLUORA_RENDER_APP_ORIGIN='+JSON.stringify(origins.appOrigin)];
const child=spawn(process.execPath,args,{stdio:'inherit',shell:false});
const interrupt=()=>child.kill('SIGINT'),terminate=()=>child.kill('SIGTERM');
process.on('SIGINT',interrupt);process.on('SIGTERM',terminate);
const code=await new Promise((resolve,reject)=>{child.once('error',reject);child.once('exit',code=>resolve(code??1));});
process.off('SIGINT',interrupt);process.off('SIGTERM',terminate);
if(code!==0)process.exit(code);
await mkdir(output,{recursive:true});
await writeFile(path.join(output,'render-origins.json'),JSON.stringify({mode:values.mode,...origins},null,2)+'\n');
