'use strict';
const { renderTenantHtml, renderNotFound } = require('./seo-renderer');
const { loadCsrTemplate } = require('./csr-shell');

// The forwarded host identifies the lookup only. The backend decides ownership,
// capability and the canonical origin; no URL supplied by a visitor is fetched.
function forwardedHostname(request) {
  const value = request.headers?.['x-forwarded-host'] || request.headers?.host;
  if (typeof value !== 'string' || value.length > 253 || value !== value.trim()) return null;
  const host = value.toLowerCase();
  return /^(?=.{1,253}$)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$/.test(host) ? host : null;
}

function createCustomDomainHandler({ config, fetchImpl = fetch, templateLoader = loadCsrTemplate, log = () => {} }) {
  return async (request, response) => {
    response.set('Cache-Control', 'no-store').set('Content-Type', 'text/html; charset=utf-8');
    if (!['GET', 'HEAD'].includes(request.method)) return response.status(405).set('Allow', 'GET, HEAD').send('');
    const accept = request.headers?.accept || 'text/html';
    if (!/(?:text\/html|application\/xhtml\+xml|\*\/\*)/i.test(accept)) return response.status(404).send('');
    let marketingOrigin;
    try {
      const runtime = config();
      marketingOrigin = new URL(runtime.marketingOrigin).origin;
      const api = new URL(runtime.apiBaseUrl);
      const clientesOrigin = new URL(runtime.clientesOrigin);
      if (clientesOrigin.protocol !== 'https:' || clientesOrigin.username || clientesOrigin.password || clientesOrigin.pathname !== '/' || clientesOrigin.search || clientesOrigin.hash) throw new Error('Client origin configuration');
      if (!['http:', 'https:'].includes(api.protocol) || !/^https:$/.test(new URL(marketingOrigin).protocol)) throw new Error('Configuration');
      const host = forwardedHostname(request);
      if (!host) return response.status(404).send(renderNotFound('solicitada', marketingOrigin));
      const path = request.path || '/';
      if (/^\/e(?:\/|$)/.test(path) || ['/privacidad','/terminos','/eliminacion-de-datos'].includes(path)) {
        const target = new URL(request.originalUrl || path, marketingOrigin);
        if (target.origin !== marketingOrigin) return response.status(400).send('');
        return response.status(302).set('Location', target.href).send('');
      }
      if (!/^\/(?:servicios|agendar|contacto|politicas|checkout|registro|productos(?:\/[a-z0-9]+(?:-[a-z0-9]+)*)?)?\/?$/i.test(path)) {
        return response.status(404).send(renderNotFound('solicitada', marketingOrigin));
      }
      const data = await fetchImpl(`${api.href.replace(/\/$/, '')}/publico/sitio/host/${encodeURIComponent(host)}`, {
        headers: { Accept: 'application/json' }, redirect: 'error', signal: AbortSignal.timeout(8000)
      });
      if (!data.ok) return response.status(data.status === 404 ? 404 : 503).send(renderNotFound('solicitada', marketingOrigin));
      const tenant = await data.json();
      if (!tenant?.publicado || !tenant?.nombreComercial || !/^([a-z0-9-]+\.)+[a-z0-9-]+$/.test(tenant.dominioPrincipal || '') || !/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(tenant.slug || '')) {
        return response.status(404).send(renderNotFound('solicitada', marketingOrigin));
      }
      const normalizedPath = path.toLowerCase().replace(/\/$/, '') || '/';
      if (path !== normalizedPath) {
        const query = new URL(request.originalUrl || path, marketingOrigin).search;
        return response.status(301).set('Location', 'https://' + host + normalizedPath + query).send('');
      }
      let catalog = null;
      const product = path.match(/^\/productos(?:\/([a-z0-9]+(?:-[a-z0-9]+)*))?\/?$/i);
      if (product) {
        const result = await fetchImpl(`${api.href.replace(/\/$/, '')}/publico/productos/sitio/${encodeURIComponent(tenant.slug)}${product[1] ? '/' + encodeURIComponent(product[1].toLowerCase()) : ''}`, {
          headers: { Accept: 'application/json' }, redirect: 'error', signal: AbortSignal.timeout(8000)
        });
        if (!result.ok) return response.status(result.status === 404 ? 404 : 503).send(renderNotFound('productos', marketingOrigin));
        catalog = await result.json();
      }
      const template = await templateLoader(clientesOrigin.origin, fetchImpl);
      const html = renderTenantHtml(template, tenant, path, marketingOrigin, catalog)
        .replace('</head>', `<script>window.__FLUORA_CUSTOM_HOST__=${JSON.stringify(host)};</script></head>`);
      return response.status(200).send(request.method === 'HEAD' ? '' : html);
    } catch (error) {
      log({ type: error?.name || 'Error' });
      return response.status(503).set('Retry-After', '60').send(marketingOrigin ? renderNotFound('solicitada', marketingOrigin) : 'Sitio temporalmente no disponible');
    }
  };
}
module.exports = { createCustomDomainHandler, forwardedHostname };
