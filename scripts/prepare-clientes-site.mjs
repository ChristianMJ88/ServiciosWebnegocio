import { cp,readFile,readdir,rm,mkdir,writeFile } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { parseArgs } from 'node:util';
import { createHash } from 'node:crypto';
import { assertCsrContract } from '../functions/csr-shell.js';

export async function hostingBuildId(root) {
 const hash=createHash('sha256');
 async function walk(folder){for(const e of (await readdir(folder,{withFileTypes:true})).sort((a,b)=>a.name<b.name?-1:a.name>b.name?1:0)){
  const file=path.join(folder,e.name);if(e.isSymbolicLink())throw Error('No symlinks in published artifacts');
  if(e.isDirectory())await walk(file);else if(e.isFile()){const data=await readFile(file);hash.update(path.relative(root,file)).update('\0').update(String(data.length)).update('\0').update(data);}
 }}
 await walk(root);return hash.digest('hex');
}
export async function clientesSiteId(){
 const config=JSON.parse(await readFile(new URL('../.firebaserc',import.meta.url),'utf8'));
 const targets=config.targets?.[config.projects?.default]?.hosting?.clientes;
 if(!Array.isArray(targets)||targets.length!==1||!/^[a-z0-9][a-z0-9-]{0,62}$/.test(targets[0]))throw Error('One proposed client Hosting site ID must be configured');
 return targets[0];
}
export async function prepareClientesSite(source,destination){
 source=path.resolve(source);destination=path.resolve(destination);
 if(source===destination||destination.startsWith(source+path.sep)||source.startsWith(destination+path.sep))throw Error('Independent output directories required');
 const shell=await readFile(path.join(source,'index.csr.html'),'utf8');
 assertCsrContract(shell,await readFile(path.join(source,'index.html'),'utf8'));
 const id=await hostingBuildId(source);
 await rm(destination,{recursive:true,force:true});await mkdir(destination,{recursive:true});
 await cp(source,destination,{recursive:true,errorOnExist:true,force:false});
 // Hosting serves index.html before rewrites. Remove it so the root also reaches
 // customDomainPage; retaining it would silently bypass tenant SEO/activation.
 await rm(path.join(destination,'index.html'));
 // The empty CSR shell remains available as an asset.
 await writeFile(path.join(destination,'index.csr.html'),shell);
 const robots=await readFile(path.join(source,'robots.txt'),'utf8');
 await writeFile(path.join(destination,'robots.txt'),robots.split(/\r?\n/).filter(line=>!/^\s*Sitemap\s*:/i.test(line)).join('\n'));
 await writeFile(path.join(destination,'fluora-host.txt'),'fluora-host:'+await clientesSiteId()+':'+id+'\n');
 return id;
}
if(process.argv[1]&&import.meta.url===pathToFileURL(path.resolve(process.argv[1])).href){
 const {values}=parseArgs({options:{source:{type:'string',default:'dist/marketing/browser'},output:{type:'string',default:'dist/clientes/browser'}}});
 console.log('Client hosting artifact prepared: '+await prepareClientesSite(values.source,values.output));
}
