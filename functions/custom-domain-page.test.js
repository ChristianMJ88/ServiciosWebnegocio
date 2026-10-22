'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createCustomDomainHandler, forwardedHostname } = require('./custom-domain-page');
const shell = '<html><head><base href="/"><title>Shell</title></head><body><app-root></app-root></body></html>';
const tenant = { slug:'fixture', nombreComercial:'Negocio fixture', publicado:true, dominioPrincipal:'marca.mx' };
const response = () => ({statusCode:0,headers:{},body:'',status(n){this.statusCode=n;return this;},set(k,v){this.headers[k]=v;return this;},send(v){this.body=v;return this;}});
const request = (path='/',extra={}) => ({method:'GET',path,originalUrl:path,headers:{'x-forwarded-host':'marca.mx',host:'function.internal',accept:'text/html'},...extra});
const config=()=>({apiBaseUrl:'https://api.fixture.test/api/v1',marketingOrigin:'https://marketing.fixture.test',clientesOrigin:'https://clientes.fixture.test'});
function handler(fetchImpl){return createCustomDomainHandler({config,fetchImpl,templateLoader:async()=>shell});}

test('forwarded host is unique and strict, not a request-selected URL',()=>{
 assert.equal(forwardedHostname(request()),'marca.mx');
 for(const value of ['marca.mx,evil.mx','marca.mx:443','https://marca.mx','marca.mx/path',' marca.mx','127.0.0.1:443'])assert.equal(forwardedHostname(request('/',{headers:{'x-forwarded-host':value}})),null);
 assert.equal(forwardedHostname(request('/',{headers:{host:'marca.mx'}})),'marca.mx');
});
for(const path of ['/','/servicios','/agendar'])test('active custom domain SEO '+path,async()=>{
 const urls=[];const res=response();await handler(async url=>{urls.push(url);return Response.json(tenant);})(request(path+'?ignored',{path,originalUrl:path+'?utm_source=fixture'}),res);
 assert.equal(res.statusCode,200);assert.equal(res.headers['Cache-Control'],'no-store');assert.match(res.body,new RegExp('rel="canonical" href="https://marca.mx'+path+'"'));
 assert.match(res.body,/<app-root><\/app-root>/);assert.doesNotMatch(res.body,/La agenda en línea para salones/);
 assert.deepEqual(urls,['https://api.fixture.test/api/v1/publico/sitio/host/marca.mx']);
});
test('another active alias uses the backend canonical, never the header as canonical',async()=>{
 const res=response();await handler(async()=>Response.json(tenant))(request('/',{headers:{accept:'text/html','x-forwarded-host':'www.marca.mx'}}),res);
 assert.equal(res.statusCode,200);assert.match(res.body,/canonical" href="https:\/\/marca.mx\//);
});
for(const status of [404,402,500])test('public backend denial '+status,async()=>{
 const res=response();await handler(async()=>new Response('',{status}))(request(),res);assert.equal(res.statusCode,status===404?404:503);assert.equal(res.headers['Cache-Control'],'no-store');assert.match(res.body,/noindex/);
});
for(const body of [{...tenant,publicado:false},{...tenant,dominioPrincipal:null},{...tenant,slug:'../otro'}])test('invalid/unpublished response fails closed '+JSON.stringify(body),async()=>{
 const res=response();await handler(async()=>Response.json(body))(request(),res);assert.equal(res.statusCode,404);assert.match(res.body,/noindex/);
});
test('product catalog is requested only for the slug resolved by backend',async()=>{
 const urls=[];const res=response();await handler(async url=>{urls.push(url);return Response.json(url.includes('/sitio/host/')?tenant:{tienda:{},productos:[{nombre:'Artículo',slug:'articulo',precio:80,moneda:'MXN',disponible:true}]});})(request('/productos'),res);
 assert.equal(res.statusCode,200);assert.deepEqual(urls,['https://api.fixture.test/api/v1/publico/sitio/host/marca.mx','https://api.fixture.test/api/v1/publico/productos/sitio/fixture']);assert.match(res.body,/https:\/\/marca.mx\/productos\/articulo/);
});
test('cross-slug redirect keeps query on the configured marketing origin',async()=>{
 const res=response();await handler(()=>{throw Error('Must not resolve tenant');})(request('/e/otro/servicios?from=fixture',{path:'/e/otro/servicios'}),res);
 assert.equal(res.statusCode,302);assert.equal(res.headers.Location,'https://marketing.fixture.test/e/otro/servicios?from=fixture');
});
for(const path of ['/barberias','/negocios','/sitemap.xml','/missing.js'])test('no Fluora content or HTML for foreign paths '+path,async()=>{
 const res=response();await handler(()=>{throw Error('Unexpected API');})(request(path),res);assert.equal(res.statusCode,404);
});
test('non HTML and write requests do not invoke API',async()=>{
 for(const req of [request('/',{method:'POST'}),request('/',{headers:{accept:'application/json','x-forwarded-host':'marca.mx'}})]){const res=response();await handler(()=>{throw Error('Unexpected API');})(req,res);assert.ok([404,405].includes(res.statusCode));}
});
test('HEAD checks activation but sends no page',async()=>{
 const res=response();await handler(async()=>Response.json(tenant))(request('/',{method:'HEAD'}),res);assert.equal(res.statusCode,200);assert.equal(res.body,'');
});
test('upstream failure returns temporary no-store/noindex response without secrets',async()=>{
 const logs=[];const res=response();await createCustomDomainHandler({config,fetchImpl:async()=>{throw new Error('secret');},log:v=>logs.push(v)})(request(),res);
 assert.equal(res.statusCode,503);assert.match(res.body,/noindex/);assert.doesNotMatch(JSON.stringify(logs),/secret/);
});

test('template comes from configured clients origin, independent of marketing deployments',async()=>{
 const seen=[];const res=response();await createCustomDomainHandler({config,fetchImpl:async()=>Response.json(tenant),templateLoader:async origin=>{seen.push(origin);return shell;}})(request(),res);
 assert.equal(res.statusCode,200);assert.deepEqual(seen,['https://clientes.fixture.test']);assert.match(res.body,/__FLUORA_CUSTOM_HOST__/);
});
for(const clientesOrigin of [undefined,'','http://clientes.fixture.test','https://x.test/path'])test('client origin fails closed '+JSON.stringify(clientesOrigin),async()=>{
 let apiCalls=0,templateCalls=0;
 const res=response();await createCustomDomainHandler({config:()=>({...config(),clientesOrigin}),fetchImpl:()=>{apiCalls++;throw Error('Must not fetch');},templateLoader:()=>{templateCalls++;throw Error('Must not load template');}})(request(),res);
 assert.equal(res.statusCode,503);assert.equal(apiCalls,0);assert.equal(templateCalls,0);assert.equal(res.headers['Retry-After'],'60');
});
for(const path of ['/SERVICIOS','/servicios/','/productos/ARTICULO'])test('normalized route '+path,async()=>{
 const res=response();await handler(async()=>Response.json(tenant))(request(path+'?utm=x',{path}),res);assert.equal(res.statusCode,301);assert.equal(res.headers.Location,'https://marca.mx'+path.toLowerCase().replace(/\/$/,'')+'?utm=x');
});
test('Accept omitted supports preview bots',async()=>{const res=response();await handler(async()=>Response.json(tenant))(request('/',{headers:{'x-forwarded-host':'marca.mx'}}),res);assert.equal(res.statusCode,200);});

test('legal links in the shared footer open on marketing, including direct navigation',async()=>{
 for(const path of ['/privacidad','/terminos','/eliminacion-de-datos']){const res=response();await handler(()=>{throw Error('Must not load tenant');})(request(path),res);assert.equal(res.statusCode,302);assert.equal(res.headers.Location,'https://marketing.fixture.test'+path);}
});
