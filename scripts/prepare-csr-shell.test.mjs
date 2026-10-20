import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, readFile, rm, mkdir } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { prepareCsrShell } from './prepare-csr-shell.mjs';

const shell = '<html><head><base href="/"><link rel="stylesheet" href="styles-A.css"><script src="/runtime-config.js"></script></head><body><app-root></app-root><script type="module" src="main-A.js"></script></body></html>';
for (const scenario of ['copy', 'existing', 'polluted', 'missing-index', 'stale']) {
  test(`marketing shell preparation: ${scenario}`, async () => {
    const root = await mkdtemp(path.join(tmpdir(), 'fluora-csr-'));
    const home = shell.replace('</app-root>', '<h1>Home</h1></app-root>');
    try {
      if (scenario !== 'missing-index') await writeFile(path.join(root, 'index.html'), ['existing','polluted'].includes(scenario) ? home : shell);
      if (scenario === 'existing' || scenario === 'stale') await writeFile(path.join(root, 'index.csr.html'), scenario === 'stale' ? shell.replace('main-A.js','main-B.js') : shell);
      if (['polluted', 'missing-index', 'stale'].includes(scenario)) {
        await assert.rejects(prepareCsrShell(root));
      } else {
        await prepareCsrShell(root);
        assert.equal(await readFile(path.join(root, 'index.csr.html'), 'utf8'), shell);
        await prepareCsrShell(root);
        assert.equal(await readFile(path.join(root, 'index.html'), 'utf8'), scenario === 'existing' ? home : shell);
        assert.equal(await readFile(path.join(root, 'index.csr.html'), 'utf8'), shell);
      }
    } finally { await rm(root, {recursive:true,force:true}); }
  });
}

test('predeploy check exits nonzero for a direct build lacking CSR; never creates it', async () => {
  const root = await mkdtemp(path.join(tmpdir(), 'fluora-predeploy-'));
  try {
    const marketing = path.join(root, 'marketing/browser');
    await mkdir(marketing, {recursive:true});
    await writeFile(path.join(marketing, 'index.html'), shell);
    const result = spawnSync(process.execPath, ['scripts/check-public-builds.cjs','--dist-root',root], {encoding:'utf8'});
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /index\.csr\.html/);
    await assert.rejects(readFile(path.join(marketing, 'index.csr.html')), {code:'ENOENT'});
  } finally { await rm(root, {recursive:true,force:true}); }
});
