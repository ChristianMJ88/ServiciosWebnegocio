import { spawn } from 'node:child_process';
import { mkdir, writeFile, rm } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { parseArgs } from 'node:util';
import { readMarketingOrigins } from './lib/marketing-origins.cjs';

// Production and diagnostics share the single root-only marketing prerender configuration.
export async function buildMarketing({ mode = 'production', output = 'dist/marketing', stats = false } = {}) {
  if (!['production', 'development'].includes(mode)) throw new Error('Invalid marketing build mode');
  const origins = readMarketingOrigins(path.resolve('public/runtime-config.js'));
  output = path.resolve(output);
  const production = path.resolve('dist/marketing');
  const contains = (parent, child) => { const relative = path.relative(parent, child); return relative === '' || (!relative.startsWith('..' + path.sep) && relative !== '..' && !path.isAbsolute(relative)); };
  const overlapsProduction = contains(production, output) || contains(output, production);
  if (overlapsProduction && (mode !== 'production' || output !== production)) throw new Error('Diagnostic output must not overlap production marketing');
  await rm(path.join(output, 'render-origins.json'), { force: true });
  const args = [path.resolve('node_modules/@angular/cli/bin/ng.js'), 'build',
    '--configuration=' + mode + ',marketing', '--output-path=' + output, '--output-hashing=all',
    '--define', 'FLUORA_RENDER_MARKETING_ORIGIN=' + JSON.stringify(origins.marketingOrigin),
    '--define', 'FLUORA_RENDER_APP_ORIGIN=' + JSON.stringify(origins.appOrigin)];
  if(stats) args.push('--stats-json');
  const child = spawn(process.execPath, args, { stdio: 'inherit', shell: false });
  const interrupt = () => child.kill('SIGINT'), terminate = () => child.kill('SIGTERM');
  process.on('SIGINT', interrupt); process.on('SIGTERM', terminate);
  let code;
  try { code = await new Promise((resolve, reject) => { child.once('error', reject); child.once('exit', n => resolve(n ?? 1)); }); }
  finally { process.off('SIGINT', interrupt); process.off('SIGTERM', terminate); }
  if (code !== 0) throw new Error('Marketing build failed: ' + code);
  await mkdir(output, { recursive: true });
  await writeFile(path.join(output, 'render-origins.json'), JSON.stringify({ mode, shell: 'prerender', ...origins }, null, 2) + '\n');
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const { values } = parseArgs({ options: { mode: { type: 'string', default: 'production' }, output: { type: 'string', default: 'dist/marketing' }, stats: {type:'boolean',default:false} } });
  await buildMarketing(values);
}
