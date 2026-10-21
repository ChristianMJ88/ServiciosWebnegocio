import http from 'node:http';
import { readFile, readdir, stat } from 'node:fs/promises';
import path from 'node:path';
import { gzipSync } from 'node:zlib';
import { createHash } from 'node:crypto';
import { assertCsrContract } from '../../functions/csr-shell.js';

/** Local production preview. Never proxies authentication or write endpoints. */
export async function publicPreview({ root, apiOrigin, requestLog = [], allowQuote = false, tenantCssRevalidate = false, spaShell = 'index', htmlSplit }) {
  root = path.resolve(root);
  if (!['index', 'csr'].includes(spaShell)) throw new Error('spaShell must be index or csr');
  if (htmlSplit && (typeof htmlSplit.after !== 'string' || !htmlSplit.after || !Number.isInteger(htmlSplit.holdMs) || htmlSplit.holdMs < 1 || htmlSplit.holdMs > 5000)) throw new Error('Invalid test HTML split');
  if (htmlSplit) {
    const home = await readFile(path.join(root, 'index.html'), 'utf8');
    if (home.split(htmlSplit.after).length !== 2) throw new Error('HTML split marker must occur exactly once');
  }
  if (spaShell === 'csr') assertCsrContract(await readFile(path.join(root, 'index.csr.html'), 'utf8'), await readFile(path.join(root, 'index.html'), 'utf8'));
  const api = new URL(apiOrigin);
  if (api.protocol !== 'http:' || !['localhost', '127.0.0.1', '[::1]'].includes(api.hostname) || api.username || api.password) {
    throw new Error('The API must be a local HTTP origin without credentials');
  }
  const mime = { '.html': 'text/html;charset=utf-8', '.js': 'application/javascript', '.css': 'text/css', '.json': 'application/json', '.woff2': 'font/woff2', '.woff': 'font/woff', '.svg': 'image/svg+xml', '.png': 'image/png', '.webp': 'image/webp', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.ico': 'image/x-icon' };
  const compressed = new Map();
  async function prepare(folder) {
    for (const item of await readdir(folder, { withFileTypes: true })) {
      const file = path.join(folder, item.name);
      if (item.isDirectory()) await prepare(file);
      else if (item.isFile() && /\.(?:html|css|js|svg)$/.test(item.name)) compressed.set(file, gzipSync(await readFile(file)));
    }
  }
  // Compression must finish before listening, not during the first audit.
  await prepare(root);
  return http.createServer(async (req, res) => {
    try {
      const origin = `http://localhost:${res.socket.localPort}`;
      const url = new URL(req.url, origin);
      // Optional visual QA only: the existing quote endpoint is read-only.
      const quote = allowQuote && req.method === 'POST' && url.pathname === '/api/v1/publico/citas/cotizar';
      if (!['GET', 'HEAD'].includes(req.method) && !quote) { res.writeHead(405); return res.end(); }
      if (url.pathname.startsWith('/api/')) {
        if (!url.pathname.startsWith('/api/v1/publico/')) { res.writeHead(403); return res.end(); }
        const headers = { accept: 'application/json' };
        if (quote) headers['content-type'] = 'application/json';
        if (typeof req.headers['x-tenant-slug'] === 'string') headers['x-tenant-slug'] = req.headers['x-tenant-slug'];
        const target = new URL(url.pathname + url.search, api);
        // Attribute a request to the audit that started it, even if an older
        // tab completes/aborts it while the next Lighthouse audit is running.
        const observation = { path: url.pathname, status: 0 };
        requestLog.push(observation);
        const upstream = http.request(target, { method: req.method, headers }, response => {
          observation.status = response.statusCode;
          res.writeHead(response.statusCode, response.headers); response.pipe(res);
        });
        upstream.setTimeout(15000, () => upstream.destroy(new Error('API timeout')));
        upstream.on('error', error => {
          observation.status = 502;
          observation.errorCode = error.code || error.message;
          observation.clientClosed = res.destroyed;
          if (!res.headersSent) res.writeHead(502); res.end();
        });
        if (quote) req.pipe(upstream); else upstream.end(); return;
      }
      if (url.pathname === '/runtime-config.js') {
        res.writeHead(200, { 'Content-Type': mime['.js'], 'Cache-Control': 'no-store' });
        return res.end(`window.__AGENDA_API_BASE_URL__='/api/v1';window.__FLUORA_APP_ORIGIN__=${JSON.stringify(origin)};window.__FLUORA_MARKETING_ORIGIN__=${JSON.stringify(origin)};`);
      }
      let file = path.resolve(root, '.' + decodeURIComponent(url.pathname));
      if (!file.startsWith(root + path.sep) && file !== root) { res.writeHead(403); return res.end(); }
      if (url.pathname === '/') file = path.join(root, 'index.html');
      const info = await stat(file).catch(() => null);
      if (!info?.isFile()) {
        if (req.headers.accept?.includes('text/html')) file = path.join(root, spaShell === 'csr' ? 'index.csr.html' : 'index.html');
        else { res.writeHead(404); return res.end(); }
      }
      const extension = path.extname(file);
      const data = await readFile(file);
      // Explicit local regression fixture: exercise a parser pause, not a data API.
      if (htmlSplit && url.pathname === '/' && req.method === 'GET') {
        const offset = Buffer.byteLength(data.toString('utf8').split(htmlSplit.after)[0] + htmlSplit.after);
        res.writeHead(200, { 'Content-Type': mime['.html'], 'Cache-Control': 'no-store' });
        res.write(data.subarray(0, offset));
        const timer = setTimeout(() => res.end(data.subarray(offset)), htmlSplit.holdMs);
        res.once('close', () => clearTimeout(timer));
        return;
      }
      const revalidate = tenantCssRevalidate && url.pathname === '/tenant-public.css';
      const etag = revalidate ? '"' + createHash('sha256').update(data).digest('hex') + '"' : null;
      if (revalidate && req.headers['if-none-match'] === etag) {
        requestLog.push({ path: url.pathname, status: 304, kind: 'asset' });
        res.writeHead(304, { ETag: etag, 'Cache-Control': 'no-cache' }); return res.end();
      }
      const gzip = /\bgzip\b/.test(req.headers['accept-encoding'] || '') && ['.html', '.css', '.js', '.svg'].includes(extension);
      const body = gzip ? compressed.get(file) : data;
      res.writeHead(200, {
        'Content-Type': mime[extension] || 'application/octet-stream',
        'Cache-Control': revalidate ? 'no-cache' : extension === '.html' ? 'no-store' : 'public,max-age=604800',
        ...(revalidate ? { ETag: etag } : {}),
        ...(gzip ? { 'Content-Encoding': 'gzip', Vary: 'Accept-Encoding' } : {}),
        'Content-Length': body.length,
      });
      res.end(req.method === 'HEAD' ? undefined : body);
    } catch {
      if (!res.headersSent) res.writeHead(500); res.end();
    }
  });
}
