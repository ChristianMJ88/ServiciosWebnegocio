'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { createSitemapHandler } = require('./sitemap-handler');

const config = () => ({ apiBaseUrl: 'https://api.fixture.test/api/v1', marketingOrigin: 'https://marketing.fixture.test' });
const response = () => ({ statusCode: 0, headers: {}, body: '', status(n) { this.statusCode = n; return this; }, set(k,v) { this.headers[k] = v; return this; }, send(v) { this.body = v; return this; } });
async function run(overrides = {}, runtime = config) {
  const logs = [], urls = [], res = response();
  const indices = {
    '/publico/sitio/indice': { slugs: ['tenant-real'] },
    '/publico/productos/sitio/indice': { productos: [{ sitioSlug: 'tenant-real', productoSlug: 'articulo-real' }] },
    '/publico/marketing/landings/indice': ['giro-real']
  };
  await createSitemapHandler({ config: runtime, log: {
    warn: (...args) => logs.push(args), error: (...args) => logs.push(args)
  }, fetchImpl: async url => {
    urls.push(url);
    const path = new URL(url).pathname.replace('/api/v1', '');
    return overrides[path] ? overrides[path]() : Response.json(indices[path]);
  } })({}, res);
  return { res, logs, urls };
}

test('publica los sitios, productos y landings devueltos por el backend configurado', async () => {
  const { res, logs, urls } = await run();
  assert.equal(res.statusCode, 200);
  assert.match(res.body, /<urlset/);
  assert.match(res.body, /\/e\/tenant-real<\/loc>/);
  assert.match(res.body, /\/e\/tenant-real\/productos\/articulo-real<\/loc>/);
  assert.match(res.body, /\/giro-real<\/loc>/);
  assert.equal(res.headers['Content-Type'], 'application/xml; charset=utf-8');
  assert.equal(res.headers['Cache-Control'], 'public, max-age=300, s-maxage=3600, stale-while-revalidate=86400');
  assert.equal(urls.length, 3);
  assert.ok(urls.every(url => url.startsWith(config().apiBaseUrl)));
  assert.deepEqual(logs, []);
});

for (const [path, missing, kept, index] of [
  ['/publico/productos/sitio/indice', /\/productos/, /\/giro-real<\/loc>/, 'productos'],
  ['/publico/marketing/landings/indice', /\/giro-real/, /\/productos\/articulo-real/, 'landings']
]) {
  for (const [kind, fail] of [
    ['500', () => new Response('PRIVATE RESPONSE', { status: 500 })],
    ['404', () => new Response('', { status: 404 })],
    ['red/timeout', () => { throw new Error('PRIVATE NETWORK DATA'); }],
    ['JSON inválido', () => new Response('PRIVATE MALFORMED JSON')],
    ['forma inválida', () => Response.json({ productos: {} })],
    ['timeout real', () => Promise.reject(new DOMException('PRIVATE TIMEOUT', 'TimeoutError'))]
  ]) test(`${index} opcional: ${kind} conserva XML200 y omite solo sus URL`, async () => {
    const { res, logs } = await run({ [path]: fail });
    assert.equal(res.statusCode, 200);
    assert.match(res.body, /\/e\/tenant-real<\/loc>/);
    assert.doesNotMatch(res.body, missing);
    assert.match(res.body, kept);
    assert.deepEqual(logs, [['Índice opcional del sitemap no disponible.', { index }]]);
    assert.doesNotMatch(JSON.stringify(logs), /PRIVATE|https/);
  });
}

test('ambos índices opcionales pueden faltar sin perder sitios ni páginas base', async () => {
  const fail = () => new Response('', { status: 500 });
  const { res, logs } = await run({ '/publico/productos/sitio/indice': fail, '/publico/marketing/landings/indice': fail });
  assert.equal(res.statusCode, 200);
  assert.match(res.body, /<urlset/);
  assert.match(res.body, /\/e\/tenant-real<\/loc>/);
  assert.match(res.body, /\/negocios<\/loc>/);
  assert.doesNotMatch(res.body, /\/productos|\/giro-real/);
  assert.equal(logs.length, 2);
});

for (const [kind, fail] of [
  ['500', () => new Response('', { status: 500 })],
  ['red', () => { throw new Error('PRIVATE'); }],
  ['JSON', () => new Response('PRIVATE')],
  ['sin slugs', () => Response.json({})],
  ['slugs null', () => Response.json({ slugs: null })]
]) test(`sitios obligatorio: ${kind} falla con 503 sin XML parcial`, async () => {
  const { res, logs } = await run({ '/publico/sitio/indice': fail });
  assert.equal(res.statusCode, 503);
  assert.equal(res.headers['Cache-Control'], 'no-store');
  assert.equal(res.headers['Retry-After'], '60');
  assert.equal(res.body, '');
  assert.deepEqual(logs, [['No fue posible generar el sitemap dinámico.', { cause: 'sitios' }]]);
});

test('configuración ausente también falla con 503', async () => {
  const { res, urls } = await run({}, () => { throw new Error('PRIVATE CONFIG'); });
  assert.equal(res.statusCode, 503);
  assert.deepEqual(urls, []);
});
