'use strict';
const test=require('node:test');
const assert=require('node:assert/strict');
const {renderLandingHtml}=require('./landing-renderer');
const {renderSitemap}=require('./seo-renderer');
const landing={slug:'barberias',titulo:'Agenda <script>alert(1)</script>',descripcion:'Reservas & equipo',etiqueta:'Fluora',seoTitulo:'Barberías | Fluora',seoDescripcion:'Organiza citas',ctaPrincipal:'Crear cuenta',tituloFinal:'Comienza',imagenUrl:null,beneficios:[{titulo:'Agenda',descripcion:'Citas'}],funcionalidades:[],pasos:[],preguntas:[]};
test('HTML inicial incluye contenido real, SEO y registro sin ejecutar contenido',()=>{
 const html=renderLandingHtml('<html><head><title>Anterior</title><meta name="robots" content="noindex"></head><body><app-root></app-root></body></html>',landing,'https://marketing.example.test','https://app.example.test');
 assert.ok(html.includes('href="https://app.example.test/registro"'));
 assert.ok(html.includes('<h1>Agenda &lt;script&gt;'));
 assert.ok(html.includes('href="https://marketing.example.test/barberias"'));
 assert.ok(!html.includes('<script>alert(1)</script>'));
 assert.ok(!html.includes('<title>Anterior</title>'));
});
test('sitemap incorpora solo slugs válidos de landings sin duplicados',()=>{
 const xml=renderSitemap([],'https://marketing.example.test',[],['barberias','barberias','../privado']);
 assert.equal((xml.match(/\/barberias</g)||[]).length,1);
 assert.ok(!xml.includes('privado'));
});
