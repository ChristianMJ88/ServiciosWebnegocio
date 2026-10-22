'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const path = require('node:path');
const runner = ['scripts/test-api.py', '--mysql-env', '/missing/credentials-must-not-be-read.json'];
for (const extra of [
  [],
  ['--fresh-schema'],
  ['--fresh-schema', '--copy-local'],
  ['--fresh-schema', '--source-dump', '/missing/dump.sql'],
  ['--fresh-schema', '--mysql-collation', 'utf8mb4_ci;DROP DATABASE agenda_db'],
  ['--fresh-schema', '--mysql-collation', 'latin1_swedish_ci']
]) test('rechaza opciones inseguras antes de leer credenciales o conectar MySQL: ' + extra.join(' '), () => {
  const result = spawnSync('python3', runner.concat(extra), { encoding: 'utf8', cwd: path.resolve(__dirname, '..') });
  assert.equal(result.status, 2);
  assert.match(result.stderr, /Selecciona exactamente|Collation utf8mb4/);
  assert.doesNotMatch(result.stderr, /FileNotFoundError|Traceback/);
});
