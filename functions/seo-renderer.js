'use strict';

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function absoluteUrl(value, marketingOrigin) {
  if (!value) return null;
  if (/^https?:\/\//i.test(value)) return value;
  return `${marketingOrigin}${value.startsWith('/') ? value : `/${value}`}`;
}

function descriptionFor(tenant) {
  return tenant.descripcionCorta?.trim()
    || tenant.heroSubtitulo?.trim()
    || `Conoce los servicios de ${tenant.nombreComercial} y reserva tu cita en línea.`;
}

function removeSeoTags(html) {
  return html
    .replace(/<title>[\s\S]*?<\/title>/i, '')
    .replace(/<meta\s+[^>]*(?:name|property)=["'](?:description|robots|keywords|og:[^"']+|twitter:[^"']+)["'][^>]*>\s*/gi, '')
    .replace(/<link\s+[^>]*rel=["']canonical["'][^>]*>\s*/gi, '')
    .replace(/<script\s+type=["']application\/ld\+json["'][^>]*>[\s\S]*?<\/script>\s*/gi, '');
}

function localBusinessSchema(tenant, canonical, description, image, marketingOrigin) {
  const schema = {
    '@context': 'https://schema.org',
    '@type': 'LocalBusiness',
    '@id': `${canonical}#business`,
    name: tenant.nombreComercial,
    description,
    url: canonical
  };

  if (image) schema.image = image;
  if (tenant.logoUrl) schema.logo = absoluteUrl(tenant.logoUrl, marketingOrigin);
  if (tenant.telefono || tenant.whatsapp) schema.telephone = tenant.telefono || tenant.whatsapp;
  if (tenant.correo) schema.email = tenant.correo;
  if (tenant.direccion) schema.address = { '@type': 'PostalAddress', streetAddress: tenant.direccion };
  const sameAs = [tenant.instagramUrl, tenant.facebookUrl].filter(Boolean);
  if (sameAs.length) schema.sameAs = sameAs;
  return schema;
}

function productSchema(tenant, product, canonical, description, marketingOrigin) {
  const schema = {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: product.nombre,
    description,
    url: canonical,
    brand: { '@type': 'Brand', name: tenant.nombreComercial },
    offers: {
      '@type': 'Offer',
      url: canonical,
      priceCurrency: product.moneda,
      price: Number(product.precio).toFixed(2),
      availability: product.disponible ? 'https://schema.org/InStock' : 'https://schema.org/OutOfStock'
    }
  };
  if (product.imagenUrl) schema.image = absoluteUrl(product.imagenUrl, marketingOrigin);
  return schema;
}

function productListSchema(tenant, catalog, canonical, marketingOrigin) {
  return {
    '@context': 'https://schema.org',
    '@type': 'ItemList',
    name: catalog.tienda?.titulo || `Productos de ${tenant.nombreComercial}`,
    url: canonical,
    numberOfItems: catalog.productos.length,
    itemListElement: catalog.productos.map((product, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: product.nombre,
      url: `${marketingOrigin}/e/${encodeURIComponent(tenant.slug)}/productos/${encodeURIComponent(product.slug)}`
    }))
  };
}

function renderTenantHtml(template, tenant, requestPath, marketingOrigin, productCatalog = null) {
  const cleanPath = requestPath.split('?')[0].split('#')[0];
  const canonical = `${marketingOrigin}${cleanPath}`;
  const product = productCatalog?.productos?.length === 1 && /\/productos\/[^/]+$/i.test(cleanPath)
    ? productCatalog.productos[0]
    : null;
  const isStore = Boolean(productCatalog);
  const description = product?.descripcion?.trim()
    || productCatalog?.tienda?.seoDescripcion?.trim()
    || productCatalog?.tienda?.descripcion?.trim()
    || descriptionFor(tenant);
  const title = product
    ? `${product.nombre} | ${tenant.nombreComercial}`
    : isStore
      ? productCatalog.tienda?.seoTitulo?.trim() || `${productCatalog.tienda?.titulo?.trim() || 'Productos'} | ${tenant.nombreComercial}`
      : tenant.seoTitulo?.trim() || `${tenant.nombreComercial} | Reserva tu cita en línea`;
  const image = absoluteUrl(product?.imagenUrl || tenant.heroImagenUrl || tenant.logoUrl, marketingOrigin);
  const schema = product
    ? productSchema(tenant, product, canonical, description, marketingOrigin)
    : isStore
      ? productListSchema(tenant, productCatalog, canonical, marketingOrigin)
      : localBusinessSchema(tenant, canonical, description, image, marketingOrigin);
  const tenantRuntimeJson = JSON.stringify(tenant)
    .replace(/</g, '\\u003c')
    .replace(/\u2028/g, '\\u2028')
    .replace(/\u2029/g, '\\u2029');
  const tags = [
    `<title>${escapeHtml(title)}</title>`,
    `<meta name="description" content="${escapeHtml(description)}">`,
    '<meta name="robots" content="index, follow, max-image-preview:large">',
    `<link rel="canonical" href="${escapeHtml(canonical)}">`,
    `<meta property="og:title" content="${escapeHtml(title)}">`,
    `<meta property="og:description" content="${escapeHtml(description)}">`,
    `<meta property="og:type" content="${product ? 'product' : isStore ? 'website' : 'business.business'}">`,
    '<meta property="og:locale" content="es_MX">',
    `<meta property="og:url" content="${escapeHtml(canonical)}">`,
    `<meta name="twitter:card" content="${image ? 'summary_large_image' : 'summary'}">`,
    `<meta name="twitter:title" content="${escapeHtml(title)}">`,
    `<meta name="twitter:description" content="${escapeHtml(description)}">`,
    image ? `<meta property="og:image" content="${escapeHtml(image)}">` : '',
    image ? `<meta name="twitter:image" content="${escapeHtml(image)}">` : '',
    `<script type="application/ld+json" id="tenant-structured-data">${JSON.stringify(schema).replace(/</g, '\\u003c')}</script>`,
    `<script>window.__FLUORA_TENANT__=${tenantRuntimeJson};</script>`
  ].filter(Boolean).join('\n  ');

  return removeSeoTags(template).replace('</head>', `  ${tags}\n</head>`);
}

function renderNotFound(slug, marketingOrigin) {
  const safeSlug = escapeHtml(slug);
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>Negocio no encontrado | Fluora</title></head><body><main><h1>Este sitio no está disponible</h1><p>No encontramos un negocio publicado con la dirección ${safeSlug}.</p><a href="${marketingOrigin}">Volver a Fluora</a></main></body></html>`;
}

function renderSitemap(slugs, marketingOrigin, products = []) {
  const tenantUrls = [...new Set(slugs)]
    .filter(slug => /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug))
    .map(slug => `  <url><loc>${marketingOrigin}/e/${escapeHtml(slug)}</loc><changefreq>weekly</changefreq><priority>0.8</priority></url>`);
  const productUrls = products
    .filter(item => /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(item?.sitioSlug || '')
      && /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(item?.productoSlug || ''))
    .map(item => `  <url><loc>${marketingOrigin}/e/${escapeHtml(item.sitioSlug)}/productos/${escapeHtml(item.productoSlug)}</loc><changefreq>weekly</changefreq><priority>0.7</priority></url>`);
  const storeUrls = [...new Set(productUrls.map(url => url.match(/(<loc>.*\/productos)\//)?.[1]))]
    .filter(Boolean)
    .map(loc => `  <url>${loc}</loc><changefreq>weekly</changefreq><priority>0.8</priority></url>`);
  const urls = [
    `  <url><loc>${marketingOrigin}/</loc><changefreq>weekly</changefreq><priority>1.0</priority></url>`,
    `  <url><loc>${marketingOrigin}/registro</loc><changefreq>monthly</changefreq><priority>0.8</priority></url>`,
    ...tenantUrls,
    ...storeUrls,
    ...productUrls
  ];
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls.join('\n')}\n</urlset>`;
}

module.exports = { renderNotFound, renderSitemap, renderTenantHtml };
