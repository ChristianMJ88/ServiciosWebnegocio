'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { loadCsrTemplate } = require('./csr-shell');

const shell = '<html><head><title>Fluora</title><base href="/"><script src="/runtime-config.js"></script></head><body><app-root></app-root><script type="module" src="main-A.js"></script></body></html>';
for (const [handler, requestPath] of [['tenantPage','/e/negocio-prueba/servicios'], ['landingPage','/barberias'], ['directoryPage','/negocios']]) {
 for (const scenario of ['missing','polluted','success']) {
  test(`${handler}: ${scenario} CSR, without loading home`, async () => {
    const urls = [];
    const fetchStub = async url => {
      urls.push(String(url));
      if (String(url).endsWith('/index.csr.html')) {
        if (scenario==='missing') return new Response('', {status:404});
        return new Response(scenario==='polluted'?shell.replace('</app-root>','<h1>Foreign home</h1></app-root>'):shell);
      }
      if (String(url).includes('/publico/marketing/landings/')) return Response.json({slug:'barberias',titulo:'Landing de prueba',beneficios:[],funcionalidades:[],pasos:[],preguntas:[]});
      return Response.json({slug:'negocio-prueba',publicado:true,nombreComercial:'Negocio de prueba',negocios:[],siguienteCursor:null});
    };
    const config = {FLUORA_API_BASE_URL:'https://api.example.test/api/v1',FLUORA_MARKETING_ORIGIN:'https://marketing.example.test',FLUORA_APP_ORIGIN:'https://app.example.test'};
    const functions = {};
    const realRequire = require('node:module').createRequire(__filename);
    vm.runInNewContext(fs.readFileSync(require.resolve('./index.js'),'utf8'), {
      exports:functions,fetch:fetchStub,URL,URLSearchParams,AbortSignal,Buffer,
      require(name) {
        if (name==='firebase-functions/v2/https') return {onRequest:(_options,fn)=>fn};
        if (name==='firebase-functions') return {logger:{error(){}}};
        if (name==='firebase-functions/params') return {defineString:key=>({value:()=>config[key]||''})};
        if (name==='./csr-shell') return {loadCsrTemplate:origin=>loadCsrTemplate(origin,fetchStub)};
        return realRequire(name);
      }
    });
    const response = {statusCode:0,headers:{},body:null,status(code){this.statusCode=code;return this;},set(key,value){this.headers[key]=value;return this;},send(body){this.body=body;return this;}};
    await functions[handler]({path:requestPath,url:requestPath,originalUrl:requestPath},response);
    assert.equal(response.statusCode,scenario==='success'?200:503);
    if (scenario!=='success') assert.equal(response.headers['Cache-Control'],'no-store');
    else {
      const expected = {tenantPage:/<title>Negocio de prueba/,landingPage:/<h1>Landing de prueba<\/h1>/,directoryPage:/CollectionPage/};
      assert.match(response.body,expected[handler]);
      assert.match(response.body,/src="main-A\.js"/);
      assert.doesNotMatch(response.body,/Foreign home/);
    }
    assert.equal(urls.filter(url=>url.endsWith('/index.csr.html')).length,1);
    assert.equal(urls.filter(url=>url.endsWith('/index.html')).length,0);
  });
 }
}
