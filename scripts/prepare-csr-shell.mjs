import { copyFile, readFile } from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { parseArgs } from 'node:util';
import { assertCsrShell, assertCsrContract } from '../functions/csr-shell.js';

export async function prepareCsrShell(root) {
  const indexFile = path.join(root, 'index.html'), csrFile = path.join(root, 'index.csr.html');
  const index = await readFile(indexFile, 'utf8');
  let csr;
  try { csr = await readFile(csrFile, 'utf8'); }
  catch (error) { if (error.code !== 'ENOENT') throw error; }
  if (csr === undefined) {
    assertCsrShell(index);
    assertCsrContract(index, index);
    await copyFile(indexFile, csrFile);
  } else {
    assertCsrContract(csr, index);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const { values } = parseArgs({ options: { root: { type: 'string' } } });
  if (!values.root) throw new Error('Provide --root for the marketing browser output');
  await prepareCsrShell(path.resolve(values.root));
  console.log('Marketing CSR shell validated');
}
