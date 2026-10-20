import { createHash } from 'node:crypto';
import { readFile, readdir, mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import postcss from 'postcss';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const checking = process.argv.includes('--check');
const consumers = [
  'src/app/app.html', 'src/app/components/marketing-nav', 'src/app/components/footer',
  'src/app/pages/fluora-home', 'src/app/pages/fluora-vertical', 'src/app/pages/legal',
  'src/app/pages/marketplace', 'src/app/shared/components/public-discovery',
  'src/app/pages/platform-redirect', 'src/app/core/subscription/subscription-access-notice.component.ts',
];
async function sources(name) {
  const absolute = path.join(root, name);
  if (/\.(?:html|ts)$/.test(name)) return [absolute];
  const files = [];
  for (const item of await readdir(absolute, { withFileTypes: true })) {
    const file = path.join(name, item.name);
    if (item.isDirectory()) files.push(...await sources(file));
    else if (/\.(?:html|ts)$/.test(file) && !file.endsWith('.spec.ts')) files.push(path.join(root, file));
  }
  return files;
}
// Include every matching TS token, not just static template classes. This
// deliberately over-approximates icons and conditional class expressions.
const tokens = new Set();
for (const consumer of consumers) for (const file of await sources(consumer)) {
  for (const match of (await readFile(file, 'utf8')).matchAll(/[A-Za-z_][A-Za-z0-9_-]*/g)) tokens.add(match[0]);
}
const classNames = selector => [...selector.matchAll(/\.([A-Za-z_][A-Za-z0-9_-]*)/g)].map(match => match[1]);
const hash = value => createHash('sha256').update(value).digest('hex');
async function output(file, content) {
  const absolute = path.join(root, file);
  if (checking) {
    if (await readFile(absolute, 'utf8').catch(() => null) !== content) throw new Error(`Outdated generated CSS: ${file}; run npm run css:marketing`);
  } else {
    await mkdir(path.dirname(absolute), { recursive: true }); await writeFile(absolute, content);
  }
}
for (const [name, source, packageName] of [
  ['marketing-bootstrap.css', 'node_modules/bootstrap/dist/css/bootstrap.min.css', 'bootstrap'],
  ['marketing-icons.css', 'node_modules/bootstrap-icons/font/bootstrap-icons.css', 'bootstrap-icons'],
]) {
  const sourceCss = await readFile(path.join(root, source), 'utf8');
  const version = JSON.parse(await readFile(path.join(root, 'node_modules', packageName, 'package.json'), 'utf8')).version;
  const parsed = postcss.parse(sourceCss);
  const used = new Set();
  parsed.walkRules(rule => classNames(rule.selector).filter(value => tokens.has(value)).forEach(value => used.add(value)));
  const animations = new Set();
  parsed.walkRules(rule => {
    if (!classNames(rule.selector).some(value => used.has(value))) return;
    // Bootstrap names animations through custom properties as well as animation.
    rule.walkDecls(declaration => {
      for (const match of declaration.value.matchAll(/[A-Za-z_][A-Za-z0-9_-]*/g)) animations.add(match[0]);
    });
  });
  const result = postcss.root();
  function select(node) {
    if (node.type === 'comment') return node.text.startsWith('!') ? node.clone() : null;
    if (node.type === 'rule') return classNames(node.selector).some(value => used.has(value)) ? node.clone() : null;
    if (node.type !== 'atrule') return null;
    if (node.name === 'font-face') return node.clone();
    if (/keyframes$/.test(node.name)) return animations.has(node.params) ? node.clone() : null;
    if (!node.nodes) return null;
    const wrapper = node.clone({ nodes: [] });
    for (const child of node.nodes) { const selected = select(child); if (selected) wrapper.append(selected); }
    return wrapper.nodes.length ? wrapper : null;
  }
  for (const node of parsed.nodes) { const selected = select(node); if (selected) result.append(selected); }
  const emittedAnimations = new Set();
  result.walkAtRules(/keyframes$/, rule => emittedAnimations.add(rule.params));
  parsed.walkAtRules(/keyframes$/, rule => {
    if (animations.has(rule.params) && !emittedAnimations.has(rule.params)) throw new Error(`Missing retained keyframes: ${rule.params}`);
  });
  const destination = `src/styles/generated/${name}`;
  result.walkDecls(declaration => {
    declaration.value = declaration.value.replace(/url\((["']?)([^)'"\s]+)\1\)/g, (full, _quote, url) => {
      if (/^(?:data:|https?:|\/)/.test(url)) return full;
      const [file, query] = url.split('?');
      const absolute = path.resolve(root, path.dirname(source), file);
      const relative = path.relative(path.resolve(root, path.dirname(destination)), absolute).split(path.sep).join('/');
      return `url("${relative}${query ? '?' + query : ''}")`;
    });
  });
  const header = `/* Generated from ${packageName}@${version}; SHA256 ${hash(sourceCss)}.\nClasses: ${[...used].sort().join(' ')}\nRun npm run css:marketing; do not edit by hand. */\n`;
  await output(destination, header + result.toString() + '\n');
}

// Keep public overrides verbatim and in their original cascade order. The
// application entry stays unchanged; --check catches drift in the source.
const global = await readFile(path.join(root, 'src/styles.css'), 'utf8');
const begin = global.indexOf('/* Global visual system */');
const admin = global.indexOf('.admin-sidebar .sidebar-menu-group {');
const tail = global.indexOf('@media (max-width: 991.98px) {', admin);
if (begin < 0 || admin <= begin || tail < admin) throw new Error('Public CSS boundaries changed; review the inventory');
await output('src/styles/public-base.css', global.slice(begin, admin) + global.slice(tail));
const material = await readFile(path.join(root, 'node_modules/@angular/material/prebuilt-themes/azure-blue.css'), 'utf8');
const outline = material.match(/--mat-sys-outline-variant:\s*([^;}]+)/)?.[1].trim();
if (!outline) throw new Error('Material public token not found; review public consumers');
await output('src/styles/material-public-tokens.css', `/* Public consumer: booking-experience.css. Derived from the installed azure-blue theme. */\nhtml{--mat-sys-outline-variant:${outline}}\n`);
console.log(checking ? 'Marketing CSS and public token/base drift checks passed' : 'Marketing CSS subsets/public base generated from installed sources');
