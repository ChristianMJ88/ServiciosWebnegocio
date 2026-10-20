import { createHash } from 'node:crypto';
import { readFile, readdir } from 'node:fs/promises';
import path from 'node:path';

async function files(folder) {
  const result = [];
  for (const entry of await readdir(folder, { withFileTypes: true })) {
    const file = path.join(folder, entry.name);
    if (entry.isDirectory()) result.push(...await files(file));
    else if (entry.isFile()) result.push(file);
  }
  return result.sort();
}

export async function fingerprintPublicBuild(root) {
  await readFile(path.join(root, 'index.html'));
  const hash = createHash('sha256'), headerChunks = [];
  for (const file of await files(root)) {
    const name = path.relative(root, file).split(path.sep).join('/');
    const data = await readFile(file); hash.update(name).update(data);
    if (name.endsWith('.js') && data.includes(Buffer.from('site-header--tenant'))) headerChunks.push(name);
  }
  if (headerChunks.length !== 1) throw new Error(`Expected exactly one Header chunk, found ${headerChunks.length}`);
  return { fingerprint: hash.digest('hex'), headerChunks };
}
