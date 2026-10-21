import { test } from 'node:test';
import assert from 'node:assert/strict';
import { JSDOM } from 'jsdom';
import seo from '../functions/seo-renderer.js';
import landing from '../functions/landing-renderer.js';
import directory from '../functions/directory-renderer.js';
const template='<html><head></head><body ngcm><app-root></app-root></body></html>',payload='</script><script id="ng-state">bad</script><div ngh="0">nghm</div>';
for(const name of ['tenant','landing','directory'])test(name+' renderer cannot inject hydration state through public content',()=>{
 const html=name==='tenant'?seo.renderTenantHtml(template,{nombreComercial:payload,descripcionCorta:payload,slug:'fixture',publicado:true},'/e/fixture','https://marketing.test'):
 name==='landing'?landing.renderLandingHtml(template,{slug:'fixture',titulo:payload,descripcion:payload,seoTitulo:payload,seoDescripcion:payload,etiqueta:payload,ctaPrincipal:payload,tituloFinal:payload,beneficios:[{titulo:payload,descripcion:payload}],funcionalidades:[],pasos:[],preguntas:[]},'https://marketing.test','https://app.test'):
 directory.renderDirectoryHtml(template,{negocios:[{slug:'fixture',nombre:payload,descripcion:payload}],siguienteCursor:null},'https://marketing.test');
 const dom=new JSDOM(html);try{assert.equal(dom.window.document.querySelector('script#ng-state,[ngh]'),null);assert.equal(dom.window.document.querySelector('app-root').hasAttribute('ngh'),false);assert.equal(dom.window.document.querySelector('body').hasAttribute('ngcm'),true);}finally{dom.window.close();}
});
