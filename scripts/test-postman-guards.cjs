const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');const vm=require('node:vm');const path=require('node:path');
function guard(collection,{env={},url='http://127.0.0.1:18080/api/v1/publico/citas',method='POST',name='Reserva',marker={database:'fluora_test_011_abc',nonce:'run'},account={}}={}){
 const values={base:'http://127.0.0.1:18080',entorno:'aislado',nonce:'run',...env};let skipped=false,failed=0;
 const pm={info:{requestName:name},request:{url:{toString:()=>url},method},environment:{get:k=>values[k]},variables:{get:k=>values[k],replaceIn:v=>v},execution:{skipRequest:()=>skipped=true},test:(n,fn)=>{try{fn()}catch{failed++}},sendRequest:(q,fn)=>fn(null,{code:200,json:()=>q.url.endsWith('/users/me')?account:marker})};
 const c=JSON.parse(fs.readFileSync(path.join(__dirname,'../backend/postman/'+collection+'.postman_collection.json')));
 for(const e of c.event.filter(e=>e.listen==='prerequest'))vm.runInNewContext(e.script.exec.join('\n'),{pm});
 return {skipped,failed};
}
test('Agenda bloquea entorno habitual, otro puerto y URLs ajenas',()=>{
 for(const url of ['http://127.0.0.1:8080/api/v1/publico/citas','http://localhost:18081/api/v1/publico/citas','https://api.refluora.com/api/v1/publico/citas','https://script.google.com/exec'])assert.deepEqual(guard('agenda-web',{url}),{skipped:true,failed:1});
 assert.deepEqual(guard('agenda-web',{env:{entorno:'habitual'}}),{skipped:true,failed:1});
});
test('Agenda exige base y nonce comprobados en backend',()=>{
 for(const marker of [{database:'agenda_db',nonce:'run'},{database:'fluora_test_011_abc',nonce:'distinto'}])assert.deepEqual(guard('agenda-web',{marker}),{skipped:true,failed:1});
 assert.deepEqual(guard('agenda-web'),{skipped:false,failed:0});
});
test('Solo Salud backend puede leer 8080 sin instancia aislada',()=>{
 assert.deepEqual(guard('agenda-web',{url:'http://127.0.0.1:8080/actuator/health',method:'GET',name:'Salud backend'}),{skipped:false,failed:0});
});
test('MP bloquea producción y escrituras no autorizadas',()=>{
 const env={mp_access_token:'TEST-falso',mp_ambiente:'PRUEBA',mp_cuenta_prueba_esperada:'prueba'};
 assert.deepEqual(guard('mercado-pago',{url:'https://api.mercadopago.com/v1/payments',env}),{skipped:true,failed:1});
 assert.deepEqual(guard('mercado-pago',{url:'https://api.mercadopago.com/users/me',method:'GET',env:{...env,mp_ambiente:'PRODUCCION'}}),{skipped:true,failed:1});
 assert.deepEqual(guard('mercado-pago',{url:'https://api.mercadopago.com/users/me',method:'GET',env,account:{id:'prueba'}}),{skipped:false,failed:0});
});
test('MP APP_USR requiere identidad y etiqueta de cuenta de prueba',()=>{
 const env={mp_access_token:'APP_USR-falso',mp_ambiente:'PRUEBA',mp_cuenta_prueba_esperada:'prueba'};
 const options={url:'https://api.mercadopago.com/users/me',method:'GET',env};
 assert.deepEqual(guard('mercado-pago',{...options,account:{id:'prueba',tags:['test_user']}}),{skipped:false,failed:0});
 assert.deepEqual(guard('mercado-pago',{...options,account:{id:'produccion',tags:['test_user']}}),{skipped:true,failed:1});
 assert.deepEqual(guard('mercado-pago',{...options,account:{id:'prueba',tags:[]}}),{skipped:true,failed:1});
});

test('Salud no exime producción ni una ruta arbitraria',()=>{
 for(const url of ['https://api.refluora.com/actuator/health','http://127.0.0.1:8080/api/v1/publico/citas'])assert.deepEqual(guard('agenda-web',{url,method:'GET',name:'Salud backend'}),{skipped:true,failed:1});
});
test('Cabeceras tenant únicas y coherentes con sucursal y servicio',()=>{
 const c=JSON.parse(fs.readFileSync(path.join(__dirname,'../backend/postman/agenda-web.postman_collection.json')));
 for(const f of c.item)for(const i of f.item||[]){const r=i.request;if(!r)continue;const headers=(r.header||[]).filter(h=>h.key.toLowerCase()==='x-tenant-slug');assert.ok(headers.length<=1,i.name);if(!headers.length)continue;const tenant=headers[0].value.includes('_b')?'b':'a';const text=(r.body?.raw||'')+r.url;for(const m of text.matchAll(/\{\{(?:sucursal|servicio[^}]*?)_([ab])\}\}/g))assert.equal(m[1],tenant,i.name);}
});
test('Autorización MP de escritura vence y no permite otros hosts',()=>{
 const env={mp_access_token:'TEST-falso',mp_ambiente:'PRUEBA',mp_cuenta_prueba_esperada:'prueba',mp_escritura_autorizada_hasta:String(Date.now()+60000)};
 assert.deepEqual(guard('mercado-pago',{url:'https://api.mercadopago.com/v1/orders',env,account:{id:'prueba'}}),{skipped:false,failed:0});
 for(const until of [Date.now()-1,Date.now()+300000])assert.deepEqual(guard('mercado-pago',{url:'https://api.mercadopago.com/v1/orders',env:{...env,mp_escritura_autorizada_hasta:String(until)},account:{id:'prueba'}}),{skipped:true,failed:1});
 assert.deepEqual(guard('mercado-pago',{url:'https://otro.example/users/me',method:'GET',env,account:{id:'prueba'}}),{skipped:true,failed:1});
});
