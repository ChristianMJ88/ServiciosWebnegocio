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
      await writeFile(path.join(root, 'index.html'), '<html><app-root></app-root></html>');
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
    await writeFile(path.join(root, 'index.html'), '<html><app-root></app-root></html>');
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

for (const spaShell of ['index','csr']) {
  test(`serves home separately from deep-link shell: ${spaShell}`, async () => {
    const root = await mkdtemp(path.join(tmpdir(), 'fluora-preview-shell-'));
    const csr = '<html><head><base href="/"><link rel="stylesheet" href="styles-A.css"><script src="/runtime-config.js"></script></head><body><app-root></app-root><script type="module" src="main-A.js"></script></body></html>';
    const home = csr.replace('</app-root>', '<h1>Home prerendered</h1></app-root>');
    let server;
    try {
      await writeFile(path.join(root,'index.html'),home);
      await writeFile(path.join(root,'index.csr.html'),csr);
      await writeFile(path.join(root,'tenant-public.css'),'body{color:purple}');
      if(spaShell==='index'){await assert.rejects(publicPreview({root,apiOrigin:'http://localhost:8080',spaShell}),/must be empty/);return;}
      server = await publicPreview({root,apiOrigin:'http://localhost:8080',spaShell});
      await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
      const origin = `http://127.0.0.1:${server.address().port}`;
      for (const route of ['/','/index.html']) assert.equal(await (await fetch(origin+route)).text(),home);
      for (const route of ['/registro','/privacidad','/inexistente','/e/negocio-prueba/servicios']) {
        assert.equal(await (await fetch(origin+route,{headers:{Accept:'text/html'}})).text(),spaShell==='csr'?csr:home);
      }
      assert.equal((await fetch(origin+'/missing.js')).status,404);
      assert.equal(await (await fetch(origin+'/tenant-public.css')).text(),'body{color:purple}');
      assert.equal((await fetch(origin+'/%2e%2e%2foutside')).status,403);
    } finally {
      if(server){server.closeAllConnections();await new Promise(resolve=>server.close(resolve));}
      await rm(root,{recursive:true,force:true});
    }
  });
}

test('refuses missing/polluted CSR and unknown mode before any listener is opened', async () => {
  const root = await mkdtemp(path.join(tmpdir(),'fluora-preview-invalid-'));
  try {
    await writeFile(path.join(root,'index.html'),'<app-root></app-root>');
    await assert.rejects(publicPreview({root,apiOrigin:'http://localhost:8080',spaShell:'csr'}),{code:'ENOENT'});
    await writeFile(path.join(root,'index.csr.html'),'<app-root><h1>Home</h1></app-root>');
    await assert.rejects(publicPreview({root,apiOrigin:'http://localhost:8080',spaShell:'csr'}),/must be empty/);
    await assert.rejects(publicPreview({root,apiOrigin:'http://localhost:8080',spaShell:'other'}),/spaShell/);
  } finally {await rm(root,{recursive:true,force:true});}
});

test('explicit split sends partial root HTML without compression; other routes stay unchanged', async () => {
 const root=await mkdtemp(path.join(tmpdir(),'fluora-preview-split-'));let server;
 const html='<html><head></head><body><nav>first</nav><app-root></app-root><main>rest</main></body></html>';
 try{
  await writeFile(path.join(root,'index.html'),html);
  await assert.rejects(publicPreview({root,apiOrigin:'http://localhost:8080',htmlSplit:{after:'missing',holdMs:100}}),/exactly once/);
  await assert.rejects(publicPreview({root,apiOrigin:'http://localhost:8080',htmlSplit:{after:'<nav>',holdMs:0}}),/Invalid/);
  server=await publicPreview({root,apiOrigin:'http://localhost:8080',htmlSplit:{after:'</nav>',holdMs:150}});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const origin='http://127.0.0.1:'+server.address().port;
  const response=await fetch(origin+'/',{headers:{'Accept-Encoding':'gzip'}});
  assert.equal(response.headers.get('content-length'),null);assert.equal(response.headers.get('content-encoding'),null);
  const reader=response.body.getReader(),first=await reader.read();assert.equal(new TextDecoder().decode(first.value),html.split('</nav>')[0]+'</nav>');
  let remainder='';for(;;){const next=await reader.read();if(next.done)break;remainder+=new TextDecoder().decode(next.value);}assert.equal(remainder,'<app-root></app-root><main>rest</main></body></html>');
  assert.equal(await(await fetch(origin+'/index.html')).text(),html);
  assert.equal((await fetch(origin+'/',{method:'HEAD',headers:{'Accept-Encoding':'identity'}})).headers.get('content-length'),String(Buffer.byteLength(html)));
 }finally{if(server){server.closeAllConnections();await new Promise(resolve=>server.close(resolve));}await rm(root,{recursive:true,force:true});}
});
