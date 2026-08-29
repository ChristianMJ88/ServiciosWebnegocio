'use strict';

const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { onRequest } = require('firebase-functions/v2/https');
const { logger } = require('firebase-functions');
const { defineString } = require('firebase-functions/params');
const { renderNotFound, renderSitemap, renderTenantHtml } = require('./seo-renderer');

const apiBaseUrlParam = defineString('FLUORA_API_BASE_URL');
const marketingOriginParam = defineString('FLUORA_MARKETING_ORIGIN');
const backendOriginParam = defineString('FLUORA_BACKEND_ORIGIN');
const template = readFileSync(join(__dirname, 'template', 'index.html'), 'utf8');

function runtimeConfig() {
  const apiBaseUrl = apiBaseUrlParam.value().trim().replace(/\/+$/, '');
  const marketingOrigin = marketingOriginParam.value().trim().replace(/\/+$/, '');
  if (!apiBaseUrl || !marketingOrigin) {
    throw new Error('Faltan FLUORA_API_BASE_URL o FLUORA_MARKETING_ORIGIN');
  }
  return { apiBaseUrl, marketingOrigin };
}

exports.apiProxy = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 60, maxInstances: 20 },
  async (request, response) => {
    const backendOrigin = backendOriginParam.value().trim().replace(/\/+$/, '');
    if (!backendOrigin) {
      response.status(503).json({ mensaje: 'La API no está configurada' });
      return;
    }

    const targetUrl = `${backendOrigin}${request.originalUrl || request.url}`;
    const headers = {};
    for (const [name, value] of Object.entries(request.headers)) {
      if (!value || ['host', 'content-length', 'connection'].includes(name.toLowerCase())) continue;
      headers[name] = Array.isArray(value) ? value.join(', ') : value;
    }

    try {
      const method = request.method.toUpperCase();
      const upstream = await fetch(targetUrl, {
        method,
        headers,
        body: ['GET', 'HEAD'].includes(method) ? undefined : request.rawBody,
        redirect: 'manual',
        signal: AbortSignal.timeout(55000)
      });

      const contentType = upstream.headers.get('content-type');
      const cacheControl = upstream.headers.get('cache-control');
      if (contentType) response.set('Content-Type', contentType);
      if (cacheControl) response.set('Cache-Control', cacheControl);
      else response.set('Cache-Control', 'no-store');

      response.status(upstream.status).send(Buffer.from(await upstream.arrayBuffer()));
    } catch (error) {
      logger.error('No fue posible conectar con el backend de Fluora.', { error });
      response.status(503).set('Cache-Control', 'no-store').json({ mensaje: 'La API no está disponible temporalmente' });
    }
  }
);

exports.tenantPage = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 15, maxInstances: 10 },
  async (request, response) => {
    const { apiBaseUrl, marketingOrigin } = runtimeConfig();
    const match = request.path.match(/^\/e\/([a-z0-9]+(?:-[a-z0-9]+)*)(?:\/.*)?$/i);
    const slug = match?.[1]?.toLowerCase();
    if (!slug) {
      response.status(404).set('Cache-Control', 'public, max-age=60').send(renderNotFound('solicitada', marketingOrigin));
      return;
    }

    try {
      const apiResponse = await fetch(`${apiBaseUrl}/publico/sitio/${encodeURIComponent(slug)}`, {
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(8000)
      });

      if (!apiResponse.ok) {
        response.status(apiResponse.status === 404 ? 404 : 502)
          .set('Cache-Control', 'public, max-age=60')
          .send(renderNotFound(slug, marketingOrigin));
        return;
      }

      const tenant = await apiResponse.json();
      if (!tenant?.publicado || !tenant?.nombreComercial) {
        response.status(404).set('Cache-Control', 'public, max-age=60').send(renderNotFound(slug, marketingOrigin));
        return;
      }

      response.status(200)
        .set('Content-Type', 'text/html; charset=utf-8')
        .set('Cache-Control', 'public, max-age=300, s-maxage=600, stale-while-revalidate=86400')
        .send(renderTenantHtml(template, tenant, request.originalUrl || request.path, marketingOrigin));
    } catch (error) {
      logger.error('No fue posible renderizar el SEO del tenant.', { slug, error });
      response.status(503)
        .set('Retry-After', '60')
        .set('Cache-Control', 'no-store')
        .send(renderNotFound(slug, marketingOrigin));
    }
  }
);

exports.sitemap = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 15, maxInstances: 5 },
  async (_request, response) => {
    const { apiBaseUrl, marketingOrigin } = runtimeConfig();
    try {
      const apiResponse = await fetch(`${apiBaseUrl}/publico/sitio/indice`, {
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(8000)
      });
      if (!apiResponse.ok) throw new Error(`El índice público respondió ${apiResponse.status}`);

      const index = await apiResponse.json();
      const slugs = Array.isArray(index?.slugs) ? index.slugs : [];
      response.status(200)
        .set('Content-Type', 'application/xml; charset=utf-8')
        .set('Cache-Control', 'public, max-age=300, s-maxage=3600, stale-while-revalidate=86400')
        .send(renderSitemap(slugs, marketingOrigin));
    } catch (error) {
      logger.error('No fue posible generar el sitemap dinámico.', { error });
      response.status(503).set('Retry-After', '60').set('Cache-Control', 'no-store').send('');
    }
  }
);
