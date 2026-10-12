const {test}=require('node:test');
const assert=require('node:assert/strict');
const {renderDirectoryHtml}=require('./directory-renderer');
const template='<html><head><title>Inicio</title><meta name="robots" content="noindex"></head><body><app-root></app-root></body></html>';
test('el directorio renderiza únicamente los negocios recibidos y escapa contenido',()=>{
 const html=renderDirectoryHtml(template,{negocios:[{slug:'ejemplo',nombre:'<script>mal</script>',direccion:'Calle & centro',servicios:[]}],siguienteCursor:15},'https://example.test');
 assert.match(html,/index,follow,max-image/);assert.match(html,/CollectionPage/);
 assert.match(html,/&lt;script&gt;/);assert.doesNotMatch(html,/<script>mal/);
 assert.match(html,/https:\/\/example.test\/e\/ejemplo/);assert.match(html,/cursor=15/);
});
test('las búsquedas y paginación conservan canonical y no generan páginas indexables duplicadas',()=>{
 const html=renderDirectoryHtml(template,{negocios:[],siguienteCursor:null},'https://example.test',new URLSearchParams('ubicacion=Puebla'));
 assert.match(html,/noindex,follow/);assert.match(html,/rel="canonical" href="https:\/\/example.test\/negocios"/);
 assert.doesNotMatch(html,/Explorar más negocios/);
});
