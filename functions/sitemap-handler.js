'use strict';

const { renderSitemap } = require('./seo-renderer');

function createSitemapHandler({ config, fetchImpl = fetch, log }) {
  async function optionalIndex(url, indexName, valid) {
    try {
      const result = await fetchImpl(url, {
        headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(8000)
      });
      if (!result.ok) throw new Error('Index unavailable');
      const body = await result.json();
      if (!valid(body)) throw new Error('Invalid index');
      return body;
    } catch {
      // Only a fixed index identifier is logged: no response bodies, URLs or tenant data.
      log.warn('Índice opcional del sitemap no disponible.', { index: indexName });
      return null;
    }
  }

  return async (_request, response) => {
    let cause = 'config';
    try {
      const { apiBaseUrl, marketingOrigin } = config();
      cause = 'sitios';
      const [sites, products, landings] = await Promise.all([
        fetchImpl(`${apiBaseUrl}/publico/sitio/indice`, {
          headers: { Accept: 'application/json' }, signal: AbortSignal.timeout(8000)
        }).then(async result => {
          if (!result.ok) throw new Error('Required index unavailable');
          const body = await result.json();
          if (!Array.isArray(body?.slugs)) throw new Error('Invalid required index');
          return body;
        }),
        optionalIndex(`${apiBaseUrl}/publico/productos/sitio/indice`, 'productos', body => Array.isArray(body?.productos)),
        optionalIndex(`${apiBaseUrl}/publico/marketing/landings/indice`, 'landings', Array.isArray)
      ]);
      cause = 'render';
      response.status(200)
        .set('Content-Type', 'application/xml; charset=utf-8')
        .set('Cache-Control', 'public, max-age=300, s-maxage=3600, stale-while-revalidate=86400')
        .send(renderSitemap(
          Array.isArray(sites?.slugs) ? sites.slugs : [], marketingOrigin,
          Array.isArray(products?.productos) ? products.productos : [],
          Array.isArray(landings) ? landings : []
        ));
    } catch {
      log.error('No fue posible generar el sitemap dinámico.', { cause });
      response.status(503).set('Retry-After', '60').set('Cache-Control', 'no-store').send('');
    }
  };
}

module.exports = { createSitemapHandler };
