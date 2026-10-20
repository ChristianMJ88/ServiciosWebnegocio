import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import http from 'node:http';
import { publicPreview } from './public-preview.mjs';

for (const revalidate of [false, true]) {
  test(`tenant CSS cache policy: revalidate=${revalidate}`, async () => {
    const root = await mkdtemp(path.join(tmpdir(), 'fluora-preview-cache-'));
    let server;
    try {
      await writeFile(path.join(root, 'index.html'), '<html></html>');
      await writeFile(path.join(root, 'tenant-public.css'), 'body{color:purple}');
      const requestLog = [];
      server = await publicPreview({ root, apiOrigin: 'http://localhost:8080', tenantCssRevalidate: revalidate, requestLog });
      await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
      const origin = `http://127.0.0.1:${server.address().port}`;
      const first = await fetch(origin + '/tenant-public.css');
      assert.equal(first.status, 200);
      assert.equal(await first.text(), 'body{color:purple}');
      if (revalidate) {
        assert.equal(first.headers.get('cache-control'), 'no-cache');
        assert.ok(first.headers.get('etag'));
        const warm = await fetch(origin + '/tenant-public.css', { headers: { 'if-none-match': first.headers.get('etag') } });
        assert.equal(warm.status, 304); assert.equal(await warm.text(), '');
        assert.deepEqual(requestLog, [{ path: '/tenant-public.css', status: 304, kind: 'asset' }]);
      } else {
        assert.equal(first.headers.get('cache-control'), 'public,max-age=604800');
        assert.equal(first.headers.get('etag'), null);
      }
      assert.equal((await fetch(origin + '/api/v1/auth/app-login', { method: 'POST' })).status, 405);
    } finally {
      if (server) { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
      await rm(root, { recursive: true, force: true });
    }
  });
}

test('attributes an overlapping response to the audit that started the request', async () => {
  const root = await mkdtemp(path.join(tmpdir(), 'fluora-preview-overlap-'));
  let release, started;
  const pending = new Promise(resolve => { release = resolve; });
  const incoming = new Promise(resolve => { started = resolve; });
  const upstream = http.createServer(async (req, res) => {
    if (req.url.endsWith('/slow')) { started(); await pending; }
    res.writeHead(200, { 'Content-Type': 'application/json' }); res.end('{}');
  });
  let server;
  try {
    await writeFile(path.join(root, 'index.html'), '<html></html>');
    await new Promise(resolve => upstream.listen(0, '127.0.0.1', resolve));
    const requestLog = [];
    server = await publicPreview({ root, apiOrigin: `http://127.0.0.1:${upstream.address().port}`, requestLog });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const origin = `http://127.0.0.1:${server.address().port}`;
    const previous = fetch(origin + '/api/v1/publico/slow');
    await incoming;
    const before = requestLog.length;
    assert.equal(before, 1); assert.equal(requestLog[0].status, 0);
    await (await fetch(origin + '/api/v1/publico/fast')).text();
    release(); await (await previous).text();
    assert.deepEqual(requestLog.slice(before), [{ path: '/api/v1/publico/fast', status: 200 }]);
    assert.equal(requestLog[0].status, 200);
  } finally {
    release();
    if (server) { server.closeAllConnections(); await new Promise(resolve => server.close(resolve)); }
    upstream.closeAllConnections(); await new Promise(resolve => upstream.close(resolve));
    await rm(root, { recursive: true, force: true });
  }
});
