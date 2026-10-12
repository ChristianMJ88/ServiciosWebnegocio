'use strict';
const {escapeHtml: escape,removeSeoTags}=require('./seo-renderer');
function renderDirectoryHtml(template,directory,origin,params=new URLSearchParams()){
 const canonical=origin+'/negocios';
 const filtered=[...params.keys()].length>0;
 const businesses=directory.negocios || [];
 const path=b=>origin+'/e/'+encodeURIComponent(b.slug);
 const list=businesses.map((b,index)=>({'@type':'ListItem',position:index+1,url:path(b),name:b.nombre}));
 const head=`<title>Directorio de negocios | Fluora</title>
 <meta name="description" content="Encuentra negocios y servicios. Consulta sus datos públicos y agenda directamente con cada negocio.">
 <meta name="robots" content="${filtered?'noindex,follow':'index,follow,max-image-preview:large'}">
 <link rel="canonical" href="${escape(canonical)}">
 <meta property="og:title" content="Directorio de negocios | Fluora">
 <meta property="og:type" content="website"><meta property="og:url" content="${escape(canonical)}">
 <script type="application/ld+json">${JSON.stringify({'@context':'https://schema.org','@type':'CollectionPage',name:'Directorio de negocios',url:canonical,mainEntity:{'@type':'ItemList',itemListElement:list}}).replaceAll('<','\\u003c')}</script>`;
 const articles=businesses.map(b=>`<article><h2><a href="${escape(path(b))}">${escape(b.nombre)}</a></h2>
 ${b.descripcion?`<p>${escape(b.descripcion)}</p>`:''}${b.direccion?`<p>${escape(b.direccion)}</p>`:''}
 ${(b.sucursales||[]).map(branch=>`<section><h3>${escape(branch.nombre)}</h3><p>${escape([branch.municipio,branch.estadoRegion,branch.codigoPostal,branch.paisCodigo].filter(Boolean).join(', '))}</p>${(branch.horariosComerciales||[]).map(h=>`<p>${escape(['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'][h.dia-1])} ${escape(h.apertura)}–${escape(h.cierre)}</p>`).join('')}</section>`).join('')}
 ${b.telefono?`<p>${escape(b.telefono)}</p>`:''}
 <ul>${(b.servicios||[]).map(s=>`<li>${escape(s.nombre)} · ${escape(s.duracionMinutos)} min · ${escape(s.moneda)} ${escape(Number(s.precio).toFixed(2))}</li>`).join('')}</ul>
 <a href="${escape(path(b))}/servicios">Ver servicios</a> <a href="${escape(path(b))}/agendar">Agendar</a></article>`).join('');
 const next=new URLSearchParams(params);
 next.set('cursor',String(directory.siguienteCursor));
 const body=`<main><h1>Encuentra tu próximo lugar favorito</h1><p>Explora negocios, conoce sus servicios y reserva directamente con ellos.</p>
 ${articles || '<p>No encontramos negocios en esta búsqueda.</p>'}
 ${directory.siguienteCursor!=null?`<a href="${escape(canonical+'?'+next)}">Explorar más negocios</a>`:''}</main>`;
 const clean=removeSeoTags(template).replace(/<noscript\b[^>]*>[\s\S]*?<\/noscript>/gi,b=>/<h1\b/i.test(b)?'':b);
 return clean.replace('</head>',head+'\n</head>').replace(/<app-root[^>]*>[\s\S]*?<\/app-root>/i,'<app-root>'+body+'</app-root>');
}
module.exports={renderDirectoryHtml};
