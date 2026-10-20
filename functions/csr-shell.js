'use strict';

const assert = require('node:assert/strict');

function assertCsrShell(html) {
  const roots = [...html.matchAll(/<app-root\b[^>]*>([\s\S]*?)<\/app-root\s*>/gi)];
  assert.equal(roots.length, 1, 'CSR requires exactly one app-root');
  assert.equal((html.match(/<app-root\b/gi) || []).length, 1, 'CSR has duplicate roots');
  assert.equal((html.match(/<\/app-root\s*>/gi) || []).length, 1, 'CSR has duplicate closing roots');
  assert.equal(roots[0][1].replace(/<!--[\s\S]*?-->/g, '').trim(), '', 'CSR app-root must be empty');
  assert.doesNotMatch(html, /\bng-server-context\b|\bngh\s*=|\bng-version\s*=|<script\b[^>]*\bid\s*=\s*(?:"ng-state"|'ng-state'|ng-state(?=[\s>]))/i, 'CSR must not contain hydration state');
  return html;
}

function assetContract(html) {
  const active = html.replace(/<noscript\b[^>]*>[\s\S]*?<\/noscript>/gi, '');
  const scripts = [...active.matchAll(/<script\b[^>]*>/gi)];
  const src = tag => tag.match(/\bsrc=["']([^"']+)["']/i)?.[1];
  const runtime = scripts.find(tag => /(?:^|\/)runtime-config\.js$/.test(src(tag[0]) || ''));
  const module = scripts.find(tag => /\btype=["']module["']/i.test(tag[0]));
  assert.ok(runtime && module && runtime.index < module.index, 'Classic runtime must precede module entry');
  assert.doesNotMatch(runtime[0], /\btype=["']module["']/i);
  const main = scripts.map(tag => src(tag[0])).filter(value => /(?:^|\/)main[.-].+\.js$/.test(value || ''));
  assert.equal(main.length, 1, 'Expected one main entry');
  const globalStyles = [...active.matchAll(/<link\b[^>]*>/gi)]
    .filter(tag => /\brel=["']stylesheet["']/i.test(tag[0]))
    .map(tag => tag[0].match(/\bhref=["']([^"']+)["']/i)?.[1])
    .filter(value => /(?:^|\/)styles[.-]/.test(value || ''));
  assert.equal(globalStyles.length, 1, 'Expected one global stylesheet');
  const bases = [...active.matchAll(/<base\b[^>]*\bhref=["']([^"']+)["'][^>]*>/gi)];
  assert.equal(bases.length, 1, 'Expected one base href');
  return {
    base: bases[0][1], runtime: src(runtime[0]), main,
    polyfills: scripts.map(tag => src(tag[0])).filter(value => /(?:^|\/)polyfills[.-].+\.js$/.test(value || '')),
    globalStyles,
    noscriptStyles: [...html.matchAll(/<noscript\b[^>]*>[\s\S]*?<\/noscript>/gi)]
      .map(tag => tag[0]).filter(tag => /<link\b[^>]*rel=["']stylesheet["']/i.test(tag))
  };
}

function assertCsrContract(csr, index) {
  assertCsrShell(csr);
  assert.deepEqual(assetContract(csr), assetContract(index), 'CSR assets must belong to the same build as index');
  let indexIsShell = false;
  try { assertCsrShell(index); indexIsShell = true; } catch { /* Future prerendered home is intentionally different. */ }
  if (indexIsShell) assert.equal(csr, index, 'Before prerender, CSR must be byte-identical to index');
}

async function loadCsrTemplate(origin, fetchImpl = fetch) {
  const response = await fetchImpl(new URL('/index.csr.html', origin).href, {
    headers: { Accept: 'text/html' }, signal: AbortSignal.timeout(5000), redirect: 'error'
  });
  if (!response.ok) throw new Error(`Marketing CSR responded ${response.status}`);
  return assertCsrShell(await response.text());
}

module.exports = { assertCsrShell, assertCsrContract, loadCsrTemplate };
