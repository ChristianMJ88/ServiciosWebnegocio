'use strict';

const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { onRequest } = require('firebase-functions/v2/https');
const { logger } = require('firebase-functions');
const { renderNotFound, renderSitemap, renderTenantHtml } = require('./seo-renderer');

const API_BASE_URL = 'https://api.refluora.com/api/v1';
const template = readFileSync(join(__dirname, 'template', 'index.html'), 'utf8');

exports.tenantPage = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 15, maxInstances: 10 },
  async (request, response) => {
    const match = request.path.match(/^\/e\/([a-z0-9]+(?:-[a-z0-9]+)*)(?:\/.*)?$/i);
    const slug = match?.[1]?.toLowerCase();
    if (!slug) {
      response.status(404).set('Cache-Control', 'public, max-age=60').send(renderNotFound('solicitada'));
      return;
    }

    try {
      const apiResponse = await fetch(`${API_BASE_URL}/publico/sitio/${encodeURIComponent(slug)}`, {
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(8000)
      });

      if (!apiResponse.ok) {
        response.status(apiResponse.status === 404 ? 404 : 502)
          .set('Cache-Control', 'public, max-age=60')
          .send(renderNotFound(slug));
        return;
      }

      const tenant = await apiResponse.json();
      if (!tenant?.publicado || !tenant?.nombreComercial) {
        response.status(404).set('Cache-Control', 'public, max-age=60').send(renderNotFound(slug));
        return;
      }

      response.status(200)
        .set('Content-Type', 'text/html; charset=utf-8')
        .set('Cache-Control', 'public, max-age=300, s-maxage=600, stale-while-revalidate=86400')
        .send(renderTenantHtml(template, tenant, request.originalUrl || request.path));
    } catch (error) {
      logger.error('No fue posible renderizar el SEO del tenant.', { slug, error });
      response.status(503)
        .set('Retry-After', '60')
        .set('Cache-Control', 'no-store')
        .send(renderNotFound(slug));
    }
  }
);

exports.sitemap = onRequest(
  { region: 'us-central1', memory: '256MiB', timeoutSeconds: 15, maxInstances: 5 },
  async (_request, response) => {
    try {
      const apiResponse = await fetch(`${API_BASE_URL}/publico/sitio/indice`, {
        headers: { Accept: 'application/json' },
        signal: AbortSignal.timeout(8000)
      });
      if (!apiResponse.ok) throw new Error(`El índice público respondió ${apiResponse.status}`);

      const index = await apiResponse.json();
      const slugs = Array.isArray(index?.slugs) ? index.slugs : [];
      response.status(200)
        .set('Content-Type', 'application/xml; charset=utf-8')
        .set('Cache-Control', 'public, max-age=300, s-maxage=3600, stale-while-revalidate=86400')
        .send(renderSitemap(slugs));
    } catch (error) {
      logger.error('No fue posible generar el sitemap dinámico.', { error });
      response.status(503).set('Retry-After', '60').set('Cache-Control', 'no-store').send('');
    }
  }
);
