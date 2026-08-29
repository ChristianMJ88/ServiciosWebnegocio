'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { renderSitemap, renderTenantHtml } = require('./seo-renderer');

const template = '<!doctype html><html><head><title>Fluora</title><meta name="description" content="General"><link rel="canonical" href="https://refluora.com/"></head><body><app-root></app-root></body></html>';

test('inyecta metadatos del negocio y conserva la aplicación Angular', () => {
  const html = renderTenantHtml(template, {
    slug: 'nail-art',
    nombreComercial: 'Nail Air',
    descripcionCorta: 'Belleza y cuidado personal.',
    heroImagenUrl: '/hero.png',
    logoUrl: '/logo.png',
    publicado: true
  }, '/e/nail-art');

  assert.match(html, /<title>Nail Air \| Reserva tu cita en línea<\/title>/);
  assert.match(html, /https:\/\/refluora\.com\/e\/nail-art/);
  assert.match(html, /property="og:image" content="https:\/\/refluora\.com\/hero\.png"/);
  assert.match(html, /"@type":"LocalBusiness"/);
  assert.match(html, /window\.__FLUORA_TENANT__=/);
  assert.match(html, /"slug":"nail-art"/);
  assert.match(html, /<app-root><\/app-root>/);
  assert.doesNotMatch(html, /content="General"/);
});

test('escapa contenido proporcionado por el tenant', () => {
  const html = renderTenantHtml(template, {
    nombreComercial: '<script>alert(1)</script>',
    descripcionCorta: 'Seguro & confiable',
    publicado: true
  }, '/e/seguro');

  assert.doesNotMatch(html, /<title><script>/);
  assert.doesNotMatch(html, /window\.__FLUORA_TENANT__=.*<script>/);
  assert.match(html, /&lt;script&gt;/);
  assert.match(html, /Seguro &amp; confiable/);
});

test('genera sitemap con páginas base y slugs válidos sin duplicados', () => {
  const xml = renderSitemap(['nail-art', 'barberia-centro', 'nail-art', '../privado']);

  assert.match(xml, /https:\/\/refluora\.com\/<\/loc>/);
  assert.match(xml, /https:\/\/refluora\.com\/registro<\/loc>/);
  assert.match(xml, /https:\/\/refluora\.com\/e\/nail-art<\/loc>/);
  assert.match(xml, /https:\/\/refluora\.com\/e\/barberia-centro<\/loc>/);
  assert.equal((xml.match(/\/e\/nail-art/g) || []).length, 1);
  assert.doesNotMatch(xml, /privado/);
});
