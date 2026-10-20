const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

for (const entry of ['marketing', 'application']) {
  const root = path.resolve(__dirname, '..', 'dist', entry, 'browser');
  const index = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
  const active = index.replace(/<noscript\b[^>]*>[\s\S]*?<\/noscript>/gi, '');
  const links = [...active.matchAll(/<link\b[^>]*>/gi)].map(match => match[0]);
  const global = links.filter(link => /\brel=["']stylesheet["']/i.test(link) && /\bhref=["']styles[.-]/i.test(link));
  assert.equal(global.length, 1, `${entry}: exactly one known global stylesheet anchor`);
  assert.ok(fs.existsSync(path.join(root, global[0].match(/href=["']([^"']+)/i)[1])));
  const runtime = active.match(/<script\b[^>]*src=["']\/runtime-config\.js["'][^>]*>/i);
  assert.ok(runtime, `${entry}: preserve classic runtime configuration`);
  assert.doesNotMatch(runtime[0], /type=["']module["']/i);
  const module = active.match(/<script\b[^>]*type=["']module["'][^>]*>/i);
  assert.ok(module && runtime.index < module.index, `${entry}: runtime precedes module entry`);
  if (entry === 'marketing') {
    assert.ok(fs.existsSync(path.join(root, 'tenant-public.css')));
    assert.doesNotMatch(active, /<link\b[^>]*href=["'][^"']*tenant-public\.css/);
    const styles = fs.readFileSync(path.join(root, global[0].match(/href=["']([^"']+)/i)[1]), 'utf8');
    assert.match(styles, /@keyframes spinner-border\b/, 'Keep the Bootstrap loading animation');
    const { renderTenantHtml } = require('../functions/seo-renderer');
    const rendered = renderTenantHtml(index, { slug: 'build-check', nombreComercial: 'Build check', publicado: true }, '/e/build-check', 'https://example.test');
    const main = rendered.match(/<script\b[^>]*src=["']main[.-][^"']+["'][^>]*>/i);
    assert.ok(main && rendered.indexOf('window.__FLUORA_TENANT__=') < main.index, 'SEO tenant bootstrap precedes main');
  }
}
console.log('Public build paths, stylesheet anchors and lazy tenant CSS verified');
