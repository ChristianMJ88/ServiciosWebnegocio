'use strict';
const { escapeHtml: escape, removeSeoTags } = require('./seo-renderer');
function renderLandingHtml(template,landing,marketingOrigin,appOrigin){
 const canonical=marketingOrigin+'/'+landing.slug;
 const register=appOrigin+'/registro';
 const head=[
 '<title>'+escape(landing.seoTitulo)+'</title>',
 '<meta name="description" content="'+escape(landing.seoDescripcion)+'">',
 '<meta name="robots" content="index,follow,max-image-preview:large">',
 '<link rel="canonical" href="'+escape(canonical)+'">',
 ...Object.entries({'og:title':landing.seoTitulo,'og:description':landing.seoDescripcion,'og:type':'website','og:url':canonical,'og:locale':'es_MX'}).map(([key,value])=>'<meta property="'+key+'" content="'+escape(value)+'">'),
 '<meta name="twitter:card" content="'+(landing.imagenUrl?'summary_large_image':'summary')+'">',
 '<meta name="twitter:title" content="'+escape(landing.seoTitulo)+'">',
 '<meta name="twitter:description" content="'+escape(landing.seoDescripcion)+'">',
 ...(landing.imagenUrl?['<meta property="og:image" content="'+escape(landing.imagenUrl)+'">','<meta name="twitter:image" content="'+escape(landing.imagenUrl)+'">']:[]),
 '<script type="application/ld+json">'+JSON.stringify({'@context':'https://schema.org','@type':'WebPage',name:landing.seoTitulo,description:landing.seoDescripcion,url:canonical}).replaceAll('<','\\u003c')+'</script>'
 ].join('\n');
 const blocks=(title,items)=>'<section><h2>'+title+'</h2>'+items.map(b=>'<article><h3>'+escape(b.titulo)+'</h3><p>'+escape(b.descripcion)+'</p></article>').join('')+'</section>';
 const body='<main><section><p>'+escape(landing.etiqueta)+'</p><h1>'+escape(landing.titulo)+'</h1><p>'+escape(landing.descripcion)+'</p><a href="'+escape(register)+'">'+escape(landing.ctaPrincipal)+'</a></section>'
 +blocks('Beneficios',landing.beneficios)+blocks('Funcionalidades',landing.funcionalidades)+blocks('Cómo funciona',landing.pasos)
 +'<section><h2>Preguntas frecuentes</h2>'+landing.preguntas.map(q=>'<details><summary>'+escape(q.pregunta)+'</summary><p>'+escape(q.respuesta)+'</p></details>').join('')+'</section>'
 +'<section><h2>'+escape(landing.tituloFinal)+'</h2><a href="'+escape(register)+'">'+escape(landing.ctaPrincipal)+'</a></section></main>';
 // La plantilla general contiene contenido de inicio para navegadores sin JS.
 // La landing ya ofrece su propio contenido inicial; conserva solo los noscript
 // técnicos (por ejemplo, la hoja de estilos) para evitar textos y H1 ajenos.
 const clean=removeSeoTags(template).replace(/<noscript\b[^>]*>[\s\S]*?<\/noscript>/gi, block => /<h1\b/i.test(block) ? '' : block);
 return clean.replace('</head>',head+'\n</head>').replace(/<app-root[^>]*>[\s\S]*?<\/app-root>/i,'<app-root>'+body+'</app-root>');
}
module.exports={renderLandingHtml};
