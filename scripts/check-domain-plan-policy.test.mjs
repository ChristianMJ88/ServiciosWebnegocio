import {test} from 'node:test';import assert from 'node:assert/strict';import {readFile,readdir} from 'node:fs/promises';import path from 'node:path';
test('domain implementation depends on capabilities, never a plan name or hierarchy',async()=>{
 const roots=['backend/src/main/java/com/techprotech/agenda/modulos/dominios','backend/src/test/java/com/techprotech/agenda/modulos/dominios','src/app/core/domains','src/app/pages/platform-domains'];
 async function scan(root){for(const e of await readdir(root,{withFileTypes:true})){const file=path.join(root,e.name);if(e.isDirectory())await scan(file);else assert.doesNotMatch(await readFile(file,'utf8'),/\b(?:PROFESIONAL|EXPANSION|EXPANSIÓN|ESENCIAL|INICIAL|PREMIUM|BASICO|BÁSICO)\b/i,file);}}
 for(const root of roots)await scan(root);
 const sql=await readFile('backend/src/main/resources/db/migration/V141__dominios_propios_verificados.sql','utf8');assert.doesNotMatch(sql,/INSERT\s+(?:IGNORE\s+)?INTO\s+plataforma_plan_(?:modulo|limite)/i);
 const service=await readFile(roots[0]+'/aplicacion/ServicioDominios.java','utf8');assert.match(service,/exigirModulo\(empresa,"DOMINIO_PROPIO"\)/);assert.match(service,/exigirLimite\(empresa,"DOMINIOS_PROPIOS_MAX"/);
});

test('legacy site configuration cannot assign an unverified domain',async()=>{const source=await readFile('backend/src/main/java/com/techprotech/agenda/modulos/admin/aplicacion/ServicioAdminCitas.java','utf8');assert.doesNotMatch(source,/request\.dominioPrincipal\(/);assert.doesNotMatch(source,/setDominioPrincipal\(/);});

test('applied V141 retains its Flyway checksum',async()=>{const text=await readFile('backend/src/main/resources/db/migration/V141__dominios_propios_verificados.sql','utf8');let crc=0xffffffff;for(const line of text.split(/\r?\n/))for(const b of Buffer.from(line)){crc^=b;for(let n=0;n<8;n++)crc=(crc>>>1)^((crc&1)?0xedb88320:0);}assert.equal((crc^0xffffffff)|0,1601200476);});
