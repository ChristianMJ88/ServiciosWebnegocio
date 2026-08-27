import { copyFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptsDirectory = dirname(fileURLToPath(import.meta.url));
const functionsDirectory = resolve(scriptsDirectory, '..');
const source = resolve(functionsDirectory, '../dist/ServiciosWebnegocio/browser/index.html');
const targetDirectory = resolve(functionsDirectory, 'template');
const target = resolve(targetDirectory, 'index.html');

await mkdir(targetDirectory, { recursive: true });
await copyFile(source, target);
console.log(`Plantilla SEO preparada desde ${source}`);
