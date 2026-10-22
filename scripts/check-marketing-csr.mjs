import assert from 'node:assert/strict';
import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import csr from '../functions/csr-shell.js';
import origins from './lib/marketing-origins.cjs';

export async function checkMarketingCsr(root) {
  const browser = path.join(root, 'browser');
  const manifest = JSON.parse(await readFile(path.join(root, 'render-origins.json'), 'utf8'));
  assert.equal(manifest.mode, 'production', 'Production mode required');
  assert.equal(manifest.shell, 'csr', 'Production marketing must use CSR');
  const runtime = path.join(browser, 'runtime-config.js');
  assert.equal(await readFile(runtime, 'utf8'), await readFile('public/runtime-config.js', 'utf8'), 'Runtime must match reviewed source');
  const configured = origins.readMarketingOrigins(runtime);
  for (const key of Object.keys(configured)) assert.equal(manifest[key], configured[key]);
  const index = await readFile(path.join(browser, 'index.html'), 'utf8');
  const shell = await readFile(path.join(browser, 'index.csr.html'), 'utf8');
  csr.assertCsrShell(index);
  csr.assertCsrContract(shell, index);
  for (const name of ['server']) {
    await assert.rejects(stat(path.join(root, name)), { code: 'ENOENT' }, 'No prerender artifacts in production');
  }
  // Angular also emits this metadata for CSR, with an empty routes object.
  // Accept that exact shape; never remove it to conceal an accidental prerender.
  try {
    const routes = JSON.parse(await readFile(path.join(root, 'prerendered-routes.json'), 'utf8'));
    assert.deepEqual(routes, { routes: {} }, 'No prerendered routes in production CSR');
  } catch (error) { if (error.code !== 'ENOENT') throw error; }
  const html = [];
  async function collect(folder) {
    for (const entry of await readdir(folder, { withFileTypes: true })) {
      const file = path.join(folder, entry.name);
      if (entry.isDirectory()) await collect(file);
      else {
        assert.ok(!entry.name.endsWith('.map'), 'No production sourcemaps');
        if (entry.name.endsWith('.html')) html.push(path.relative(browser, file));
      }
    }
  }
  await collect(browser);
  assert.deepEqual(html.sort(), ['index.csr.html', 'index.html']);
  return { shell: 'csr', htmlBytes: Buffer.byteLength(index), origins: configured };
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  if (!process.argv[2]) throw new Error('A marketing output root is required');
  console.log(JSON.stringify(await checkMarketingCsr(path.resolve(process.argv[2]))));
}
