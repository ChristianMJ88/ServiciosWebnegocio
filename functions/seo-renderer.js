'use strict';

const MARKETING_ORIGIN = 'https://refluora.com';

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function absoluteUrl(value) {
  if (!value) return null;
  if (/^https?:\/\//i.test(value)) return value;
  return `${MARKETING_ORIGIN}${value.startsWith('/') ? value : `/${value}`}`;
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

function localBusinessSchema(tenant, canonical, description, image) {
  const schema = {
    '@context': 'https://schema.org',
    '@type': 'LocalBusiness',
    '@id': `${canonical}#business`,
    name: tenant.nombreComercial,
    description,
    url: canonical
  };

  if (image) schema.image = image;
  if (tenant.logoUrl) schema.logo = absoluteUrl(tenant.logoUrl);
  if (tenant.telefono || tenant.whatsapp) schema.telephone = tenant.telefono || tenant.whatsapp;
  if (tenant.correo) schema.email = tenant.correo;
  if (tenant.direccion) schema.address = { '@type': 'PostalAddress', streetAddress: tenant.direccion };
  const sameAs = [tenant.instagramUrl, tenant.facebookUrl].filter(Boolean);
  if (sameAs.length) schema.sameAs = sameAs;
  return schema;
}

function renderTenantHtml(template, tenant, requestPath) {
  const cleanPath = requestPath.split('?')[0].split('#')[0];
  const canonical = `${MARKETING_ORIGIN}${cleanPath}`;
  const description = descriptionFor(tenant);
  const title = `${tenant.nombreComercial} | Reserva tu cita en línea`;
  const image = absoluteUrl(tenant.heroImagenUrl || tenant.logoUrl);
  const schema = localBusinessSchema(tenant, canonical, description, image);
  const tags = [
    `<title>${escapeHtml(title)}</title>`,
    `<meta name="description" content="${escapeHtml(description)}">`,
    '<meta name="robots" content="index, follow, max-image-preview:large">',
    `<link rel="canonical" href="${escapeHtml(canonical)}">`,
    `<meta property="og:title" content="${escapeHtml(title)}">`,
    `<meta property="og:description" content="${escapeHtml(description)}">`,
    '<meta property="og:type" content="business.business">',
    '<meta property="og:locale" content="es_MX">',
    `<meta property="og:url" content="${escapeHtml(canonical)}">`,
    `<meta name="twitter:card" content="${image ? 'summary_large_image' : 'summary'}">`,
    `<meta name="twitter:title" content="${escapeHtml(title)}">`,
    `<meta name="twitter:description" content="${escapeHtml(description)}">`,
    image ? `<meta property="og:image" content="${escapeHtml(image)}">` : '',
    image ? `<meta name="twitter:image" content="${escapeHtml(image)}">` : '',
    `<script type="application/ld+json" id="tenant-structured-data">${JSON.stringify(schema).replace(/</g, '\\u003c')}</script>`
  ].filter(Boolean).join('\n  ');

  return removeSeoTags(template).replace('</head>', `  ${tags}\n</head>`);
}

function renderNotFound(slug) {
  const safeSlug = escapeHtml(slug);
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>Negocio no encontrado | Fluora</title></head><body><main><h1>Este sitio no está disponible</h1><p>No encontramos un negocio publicado con la dirección ${safeSlug}.</p><a href="${MARKETING_ORIGIN}">Volver a Fluora</a></main></body></html>`;
}

function renderSitemap(slugs) {
  const tenantUrls = [...new Set(slugs)]
    .filter(slug => /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug))
    .map(slug => `  <url><loc>${MARKETING_ORIGIN}/e/${escapeHtml(slug)}</loc><changefreq>weekly</changefreq><priority>0.8</priority></url>`);
  const urls = [
    `  <url><loc>${MARKETING_ORIGIN}/</loc><changefreq>weekly</changefreq><priority>1.0</priority></url>`,
    `  <url><loc>${MARKETING_ORIGIN}/registro</loc><changefreq>monthly</changefreq><priority>0.8</priority></url>`,
    ...tenantUrls
  ];
  return `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls.join('\n')}\n</urlset>`;
}

module.exports = { renderNotFound, renderSitemap, renderTenantHtml };
