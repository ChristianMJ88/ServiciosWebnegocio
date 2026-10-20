#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { readFile, mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { spawn, execFileSync } from 'node:child_process';
import { parseArgs } from 'node:util';
import { publicPreview } from './performance/public-preview.mjs';
import { fingerprintPublicBuild } from './performance/build-fingerprint.mjs';

const { values } = parseArgs({ options: {
  root: { type: 'string', default: 'dist/marketing/browser' },
  'compare-root': { type: 'string' },
  'compare-label': { type: 'string', default: 'comparison' },
  output: { type: 'string', default: 'tmp/lighthouse/home' },
  label: { type: 'string', default: 'home' },
  api: { type: 'string', default: 'http://localhost:8080' },
  port: { type: 'string', default: '4173' },
  path: { type: 'string', default: '/' },
  width: { type: 'string', default: '375' },
  height: { type: 'string', default: '812' },
  'spa-shell': { type: 'string', default: 'index' },
} });
const root = path.resolve(values.root), output = path.resolve(values.output);
if (!values.path.startsWith('/') || values.path.startsWith('//')) throw new Error('Use a local path');
const port = Number(values.port), width = Number(values.width), height = Number(values.height);
if (![port, width, height].every(Number.isSafeInteger) || port < 1024 || port > 65535 || width < 320 || height < 320) throw new Error('Invalid dimensions or local port');
const cli = process.env.LIGHTHOUSE_CLI;
if (!cli) throw new Error('Set LIGHTHOUSE_CLI to the installed lighthouse/cli/index.js (13.4.1)');
const node = process.env.LIGHTHOUSE_NODE || process.execPath;
const version = execFileSync(node, [cli, '--version'], { encoding: 'utf8' }).trim();
if (version !== '13.4.1') throw new Error(`Fixed measurement requires Lighthouse13.4.1; found ${version}`);
async function describeBuild(root, label, port) {
  const { fingerprint, headerChunks } = await fingerprintPublicBuild(root);
  const requests = [];
  return { root, label, port, fingerprint, headerChunks, requests, runs: [], server: await publicPreview({ root, apiOrigin: values.api, requestLog: requests, spaShell: values['spa-shell'] }) };
}
await mkdir(output, { recursive: true, mode: 0o700 });
const scenarios = [await describeBuild(root, values.label, port)];
if (values['compare-root']) {
  if (port === 65535 || values['compare-label'] === values.label) throw new Error('Invalid comparison port/label');
  scenarios.push(await describeBuild(path.resolve(values['compare-root']), values['compare-label'], port + 1));
}
let child = null, interrupted = false, sequence = 0;
const interrupt = () => { interrupted = true; child?.kill('SIGINT'); };
process.on('SIGINT', interrupt); process.on('SIGTERM', interrupt);
try {
  for (const scenario of scenarios) {
  const { server, port, fingerprint } = scenario;
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(port, '127.0.0.1', resolve); });
  const origin = `http://localhost:${port}`;
  scenario.url = origin + values.path;
  // Check real public API before measuring; never benchmark a failed/offline offer.
  const check = await fetch(origin + '/api/v1/publico/plataforma/soporte');
  if (!check.ok) throw new Error(`Public API is unavailable (${check.status}); no measurement recorded`);
  // Warm real backend GETs without persisting any state or using invented data.
  for (const endpoint of ['/api/v1/publico/plataforma/planes', '/api/v1/publico/marketing/landings/navegacion']) {
    if (!(await fetch(origin + endpoint)).ok) throw new Error('Public API warmup failed: ' + endpoint);
  }
  console.log(`Lighthouse${version}; SHA256 ${fingerprint}; mobile${width}×${height}; simulate; one recorded warmup + five cold navigations`);
  }
  for (let i = 0; i <= 5; i++) {
    const order = i > 0 && i % 2 === 0 ? [...scenarios].reverse() : scenarios;
    for (const scenario of order) {
    const { requests, url, headerChunks, runs, label } = scenario;
    if (interrupted) throw new Error('Interrupted');
    const before = requests.length;
    const reportPath = path.join(output, `${label}-${i === 0 ? 'warmup' : i}.json`);
    const log = [];
    await new Promise((resolve, reject) => {
      child = spawn(node, [cli, url,
        '--quiet', '--output=json', `--output-path=${reportPath}`,
        '--only-categories=performance,accessibility,best-practices,seo',
        '--form-factor=mobile', '--throttling-method=simulate',
        `--screenEmulation.width=${width}`, `--screenEmulation.height=${height}`,
        '--screenEmulation.deviceScaleFactor=1', '--screenEmulation.mobile=true',
        '--chrome-flags=--headless=new --disable-extensions --no-first-run',
      ], { stdio: ['ignore', 'pipe', 'pipe'] });
      child.stdout.on('data', chunk => log.push(chunk)); child.stderr.on('data', chunk => log.push(chunk));
      let forceTimer;
      const timer = setTimeout(() => { child?.kill('SIGINT'); forceTimer = setTimeout(() => child?.kill('SIGKILL'), 10000); }, 240000);
      child.once('error', error => { clearTimeout(timer); clearTimeout(forceTimer); reject(error); });
      child.once('exit', (code, signal) => { clearTimeout(timer); clearTimeout(forceTimer); child = null; code === 0 ? resolve() : reject(new Error(`Lighthouse exit${code}/${signal}: ${Buffer.concat(log).toString().slice(-1200)}`)); });
    });
    const report = JSON.parse(await readFile(reportPath, 'utf8'));
    if (report.runtimeError) throw new Error('Lighthouse runtime error: ' + report.runtimeError.code);
    const failedApi = requests.slice(before).filter(item => item.status >= 400);
    if (failedApi.length) throw new Error('Public API failed during this run: ' + JSON.stringify(failedApi));
    if (values.path === '/') for (const endpoint of ['/api/v1/publico/plataforma/planes', '/api/v1/publico/marketing/landings/navegacion', '/api/v1/publico/plataforma/soporte']) {
      if (!requests.slice(before).some(item => item.path === endpoint && item.status === 200)) throw new Error('The real public page did not load ' + endpoint);
    }
    const metrics = report.audits;
    const network = metrics['network-requests'].details.items;
    const item = {
      sequence: ++sequence,
      run: i === 0 ? 'warmup' : i, scores: Object.fromEntries(Object.entries(report.categories).map(([name, data]) => {
        if (typeof data.score !== 'number') throw new Error(`Missing score for ${name}`);
        return [name, Math.round(data.score * 100)];
      })),
      fcp: metrics['first-contentful-paint'].numericValue, lcp: metrics['largest-contentful-paint'].numericValue,
      tbt: metrics['total-blocking-time'].numericValue, cls: metrics['cumulative-layout-shift'].numericValue,
      speedIndex: metrics['speed-index'].numericValue,
      headerRequested: network.some(resource => headerChunks.some(name => new URL(resource.url).pathname === '/' + name)),
      tenantStylesRequested: network.some(resource => new URL(resource.url).pathname === '/tenant-public.css'),
      resources: metrics['resource-summary'].details.items,
      settings: report.configSettings, browser: report.environment.hostUserAgent,
    };
    if (i !== 0) runs.push(item);
    console.log(JSON.stringify({ label, ...item, settings: undefined, browser: undefined, resources: undefined }));
    }
  }
  const median = values => [...values].sort((a, b) => a - b)[2];
  for (const scenario of scenarios) {
  const { label, root, url, headerChunks, runs, fingerprint } = scenario;
  const summary = {
    label, provenance: 'fingerprint-only',
    measurementCode: {
      commit: execFileSync('git', ['rev-parse', 'HEAD'], { encoding: 'utf8' }).trim(),
      sourceDirty: !!execFileSync('git', ['status', '--porcelain', '--', 'src', 'scripts', 'angular.json', 'package.json', 'package-lock.json', 'tsconfig*.json'], { encoding: 'utf8' }).trim(),
      scriptHashes: Object.fromEntries(await Promise.all(['./lighthouse-home.mjs', './performance/public-preview.mjs', './performance/build-fingerprint.mjs'].map(async file => [file, createHash('sha256').update(await readFile(new URL(file, import.meta.url))).digest('hex')]))),
    },
    buildFingerprint: fingerprint, lighthouse: version, node: execFileSync(node, ['--version'], { encoding: 'utf8' }).trim(),
    root: path.relative(process.cwd(), root), url, headerChunks, runs, spaShell: values['spa-shell'],
    median: { performance: median(runs.map(run => run.scores.performance)), fcp: median(runs.map(run => run.fcp)), lcp: median(runs.map(run => run.lcp)), tbt: median(runs.map(run => run.tbt)), cls: median(runs.map(run => run.cls)), speedIndex: median(runs.map(run => run.speedIndex)) },
  };
  await writeFile(path.join(output, scenarios.length === 1 ? 'summary.json' : `${label}-summary.json`), JSON.stringify(summary, null, 2), { mode: 0o600 });
  console.log(label + ' median: ' + JSON.stringify(summary.median));
  }
} finally {
  child?.kill('SIGINT');
  for (const { server, port } of scenarios) {
    server.closeAllConnections(); await new Promise(resolve => server.close(resolve));
    console.log(`Preview${port} stopped`);
  }
  process.off('SIGINT', interrupt); process.off('SIGTERM', interrupt);
  console.log('Existing frontend/backend untouched');
}
