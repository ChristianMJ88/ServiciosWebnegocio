#!/usr/bin/env node
// P2: Lighthouse CLI on an isolated Chrome, cold then warm on the same profile.
import { createHash } from 'node:crypto';
import { execFileSync, spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { mkdir, readFile, stat, writeFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import path from 'node:path';
import { parseArgs } from 'node:util';
import { publicPreview } from './performance/public-preview.mjs';
import { fingerprintPublicBuild } from './performance/build-fingerprint.mjs';

const { values } = parseArgs({ options: {
  root: { type: 'string', default: 'dist/marketing/browser' },
  path: { type: 'string' },
  api: { type: 'string', default: 'http://localhost:8080' },
  output: { type: 'string', default: 'tmp/lighthouse/tenant-cache' },
  port: { type: 'string', default: '4173' },
  sizes: { type: 'string', default: '390x844,820x1180,1440x900' },
  baseline: { type: 'boolean', default: false },
  'allow-quote': { type: 'boolean', default: false },
  traces: { type: 'boolean', default: false },
  'spa-shell': { type: 'string', default: 'index' }
} });
if (!values.path?.startsWith('/') || values.path.startsWith('//')) throw new Error('Provide the public tenant path');
const sizes = values.sizes.split(',').map(size => size.split('x').map(Number));
if (sizes.some(size => size.length !== 2 || size.some(value => !Number.isSafeInteger(value) || value < 320))) throw new Error('Invalid sizes');
const port = Number(values.port);
if (!Number.isSafeInteger(port) || port < 1024 || port > 65535) throw new Error('Invalid port');
const cli = process.env.LIGHTHOUSE_CLI, node = process.env.LIGHTHOUSE_NODE || process.execPath;
if (!cli || execFileSync(node, [cli, '--version'], { encoding: 'utf8' }).trim() !== '13.4.1') throw new Error('Requires LIGHTHOUSE_CLI13.4.1');
const require = createRequire(path.resolve(cli));
const { launch } = await import(pathToFileURL(require.resolve('chrome-launcher')).href);
const root = path.resolve(values.root), output = path.resolve(values.output), requests = [], results = [], warmups = [];
if (values.baseline) {
  try {
    await stat(path.join(root, 'tenant-public.css'));
    throw new Error('Baseline mode requires an artifact without deferred tenant-public.css');
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
}
const { fingerprint, headerChunks } = await fingerprintPublicBuild(root);
await mkdir(output, { recursive: true, mode: 0o700 });
// Exercise the proposed D1 policy locally; this does not modify hosting.
const server = await publicPreview({ root, apiOrigin: values.api, requestLog: requests, allowQuote: values['allow-quote'], tenantCssRevalidate: !values.baseline, spaShell: values['spa-shell'] });
let chrome, child, interrupted = false;
const interrupt = () => { interrupted = true; child?.kill('SIGINT'); };
process.on('SIGINT', interrupt); process.on('SIGTERM', interrupt);
try {
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(port, '127.0.0.1', resolve); });
  if (!(await fetch(`http://localhost:${port}/api/v1/publico/plataforma/soporte`)).ok) throw new Error('API unavailable');
  for (const [width, height] of sizes) {
    chrome = await launch({ chromeFlags: ['--headless=new', '--disable-extensions', '--no-first-run'], handleSIGINT: false });
    try {
      // Discard a navigation first to warm the real tenant GETs. The cold audit
      // afterwards still resets browser cache/storage through Lighthouse.
      for (const cache of ['warmup', 'cold', 'warm']) {
        if (interrupted) throw new Error('Interrupted');
        const reportPath = path.join(output, `${width}x${height}-${cache}.json`), before = requests.length;
        await new Promise((resolve, reject) => {
          const errors = [];
          child = spawn(node, [cli, `http://localhost:${port}${values.path}`, '--quiet', '--output=json', `--output-path=${reportPath}`,
            `--port=${chrome.port}`, '--only-categories=performance,accessibility,best-practices,seo',
            '--form-factor=mobile', '--throttling-method=simulate', `--screenEmulation.width=${width}`, `--screenEmulation.height=${height}`,
            '--screenEmulation.deviceScaleFactor=1', '--screenEmulation.mobile=true', ...(values.traces ? ['--save-assets'] : []), ...(cache === 'warm' ? ['--disable-storage-reset'] : [])],
          { stdio: ['ignore', 'pipe', 'pipe'] });
          child.stdout.on('data', value => errors.push(value)); child.stderr.on('data', value => errors.push(value));
          let forceTimer;
          const timer = setTimeout(() => { child?.kill('SIGINT'); forceTimer = setTimeout(() => child?.kill('SIGKILL'), 10000); }, 240000);
          child.once('error', error => { clearTimeout(timer); clearTimeout(forceTimer); reject(error); });
          child.once('exit', code => { clearTimeout(timer); clearTimeout(forceTimer); child = null; code === 0 ? resolve() : reject(new Error(Buffer.concat(errors).toString().slice(-1000))); });
        });
        const report = JSON.parse(await readFile(reportPath, 'utf8'));
        const failedApi = requests.slice(before).filter(item => item.status >= 400);
        const pendingApiRequests = requests.slice(before).filter(item => item.status === 0).map(item => item.path);
        if (report.runtimeError || failedApi.length) {
          await writeFile(path.join(output, 'failed-run.json'), JSON.stringify({ width, height, cache, runtimeError: report.runtimeError, failedApi }, null, 2), { mode: 0o600 });
          throw new Error('Audit or public API failed: ' + JSON.stringify({ runtimeError: report.runtimeError, failedApi }));
        }
        const resources = report.audits['network-requests'].details.items;
        const own = resources.filter(item => item.url.startsWith(`http://localhost:${port}/`));
        if (cache === 'cold' && own.some(item => item.cache !== 'none')) throw new Error('Cold audit used cached resources');
        const tenantCss = own.find(item => new URL(item.url).pathname === '/tenant-public.css');
        if ((!values.baseline && !tenantCss) || !own.some(item => headerChunks.includes(new URL(item.url).pathname.slice(1))) || !requests.slice(before).some(item => item.path.startsWith('/api/v1/publico/') && !item.path.startsWith('/api/v1/publico/plataforma/') && item.status === 200)) throw new Error('Tenant did not load its Header, required CSS and public API');
        const cached = resources.filter(item => item.url.startsWith(`http://localhost:${port}/`) && ['memory', 'disk'].includes(item.cache));
        if (cache === 'warm') {
          const code = own.filter(item => /\.(?:css|js)$/.test(new URL(item.url).pathname) && !['/runtime-config.js', '/tenant-public.css'].includes(new URL(item.url).pathname));
          if (!code.length || code.some(item => !['memory', 'disk'].includes(item.cache))) throw new Error('Warm audit fetched uncached code/styles');
          if (!values.baseline && !requests.slice(before).some(item => item.path === '/tenant-public.css' && item.status === 304)) throw new Error('Tenant CSS was not revalidated with304');
        }
        const item = { width, height, cache, cls: report.audits['cumulative-layout-shift'].numericValue,
          pendingApiRequests,
          scores: Object.fromEntries(Object.entries(report.categories).map(([name, category]) => {
            if (typeof category.score !== 'number') throw new Error('Missing category score');
            return [name, Math.round(category.score * 100)];
          })),
          fcp: report.audits['first-contentful-paint'].numericValue, lcp: report.audits['largest-contentful-paint'].numericValue,
          tbt: report.audits['total-blocking-time'].numericValue, speedIndex: report.audits['speed-index'].numericValue,
          resources: report.audits['resource-summary'].details.items,
          tenantCssRevalidated: !values.baseline && cache === 'warm',
          cachedResources: cached.map(item => ({ url: new URL(item.url).pathname, cache: item.cache })),
          settings: report.configSettings, browser: report.environment.hostUserAgent };
        (cache === 'warmup' ? warmups : results).push(item);
        console.log(JSON.stringify({ ...item, settings: undefined, browser: undefined, resources: undefined, cachedResources: cached.length }));
      }
    } finally { await chrome.kill(); chrome = null; }
  }
  await writeFile(path.join(output, 'summary.json'), JSON.stringify({ buildFingerprint: fingerprint, lighthouse: '13.4.1',
    path: values.path, sampleCountPerSizeAndCache: 1,
    baseline: values.baseline, traces: values.traces, allowReadOnlyQuote: values['allow-quote'], headerChunks, spaShell: values['spa-shell'],
    backendWarmup: 'One discarded tenant navigation per size before cold/warm; cold still resets browser cache/storage',
    viewportProfile: 'All sizes, including desktop viewport1440, use mobile throttling simulate CPU4/150ms/1.6Mbps',
    cacheHeaders: values.baseline ? 'Baseline assets max-age604800; no deferred tenant CSS or D1 guard' : 'Preview assets max-age604800; tenant-public.css no-cache with ETag/304 simulates proposed D1, not deployed hosting',
    warmTiming: 'Lantern simulated disk cache timing, not measured disk read latency',
    measurementCode: {
      commit: execFileSync('git', ['rev-parse', 'HEAD'], { encoding: 'utf8' }).trim(),
      sourceDirty: !!execFileSync('git', ['status', '--porcelain', '--', 'src', 'scripts', 'angular.json', 'package.json', 'package-lock.json', 'tsconfig*.json'], { encoding: 'utf8' }).trim(),
      scriptHashes: Object.fromEntries(await Promise.all(['./lighthouse-tenant-cache.mjs', './performance/public-preview.mjs', './performance/build-fingerprint.mjs'].map(async file => [file, createHash('sha256').update(await readFile(new URL(file, import.meta.url))).digest('hex')]))),
    }, warmups, results }, null, 2), { mode: 0o600 });
} finally {
  child?.kill('SIGINT'); await chrome?.kill(); server.closeAllConnections(); await new Promise(resolve => server.close(resolve));
  process.off('SIGINT', interrupt); process.off('SIGTERM', interrupt);
  console.log('Own preview and Chrome closed; existing4200/8080 preserved');
}
