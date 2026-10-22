const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { parseArgs } = require('node:util');
const { assertCsrShell, assertCsrContract } = require('../functions/csr-shell');
const { values } = parseArgs({ options: { 'dist-root': { type: 'string', default: path.resolve(__dirname, '../dist') } } });

async function main() {
for (const entry of ['marketing', 'application']) {
  const root = path.resolve(values['dist-root'], entry, 'browser');
  const index = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
  const shell = entry === 'marketing' ? fs.readFileSync(path.join(root, 'index.csr.html'), 'utf8') : index;
  assertCsrShell(shell);
  if (entry === 'marketing') {
    assertCsrContract(shell, index);
    const { checkHydrationProbe } = await import('./check-hydration-probe.mjs');
    await checkHydrationProbe(path.resolve(values['dist-root'], entry),undefined,{production:true});
  }
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
    const rendered = renderTenantHtml(shell, { slug: 'build-check', nombreComercial: 'Build check', publicado: true }, '/e/build-check', 'https://example.test');
    const main = rendered.match(/<script\b[^>]*src=["']main[.-][^"']+["'][^>]*>/i);
    assert.ok(main && rendered.indexOf('window.__FLUORA_TENANT__=') < main.index, 'SEO tenant bootstrap precedes main');
    const { renderLandingHtml } = require('../functions/landing-renderer');
    const { renderDirectoryHtml } = require('../functions/directory-renderer');
    const landing = renderLandingHtml(shell, { slug:'build-check', titulo:'Landing check', beneficios:[], funcionalidades:[], pasos:[], preguntas:[] }, 'https://example.test', 'https://app.example.test');
    assert.match(landing, /<h1>Landing check<\/h1>/);
    const directory = renderDirectoryHtml(shell, {negocios:[],siguienteCursor:null}, 'https://example.test');
    assert.match(directory, /CollectionPage/);
  }
}
const hosting = JSON.parse(fs.readFileSync(path.resolve(__dirname, '../firebase.json'), 'utf8')).hosting;
const marketing = hosting.find(site => site.target === 'marketing'), app = hosting.find(site => site.target === 'app');
assert.equal(marketing.rewrites.find(rule=>rule.source==='**').destination, '/index.csr.html');
assert.ok(marketing.predeploy.includes('node scripts/check-public-builds.cjs'));
assert.equal(app.rewrites.find(rule=>rule.source==='**').destination, '/index.html');
for (const site of [marketing, app]) {
  assert.equal(site.rewrites.filter(rule=>rule.source==='**').length, 1, 'Expected one SPA fallback');
  assert.equal(site.rewrites.at(-1).source, '**', 'SPA fallback must follow every Function rewrite');
}
console.log('Public build paths, empty CSR, asset contracts, SEO renderers and hosting fallback verified');
}
main().catch(error => { console.error(error); process.exitCode = 1; });
