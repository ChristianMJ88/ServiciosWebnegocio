'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');

function assertParameterDefaults(source) {
  const declarations = [...source.matchAll(/\bdefineString\s*\(\s*(['"])(FLUORA_[A-Z_]+)\1\s*(?:,\s*(\{[^}]*\}))?\s*\)/g)];
  assert.equal(declarations.length, [...source.matchAll(/\bdefineString\s*\(/g)].length, 'Every parameter declaration must be inspected');
  const clients = declarations.filter(match => match[2] === 'FLUORA_CLIENTES_ORIGIN');
  assert.equal(clients.length, 1, 'One clients origin parameter required');
  for (const match of declarations) {
    if (match[2] === 'FLUORA_CLIENTES_ORIGIN') {
      assert.match(match[3] || '', /^\{\s*default\s*:\s*(['"])\1\s*\}$/, 'Clients origin must default to the empty string');
    } else {
      assert.equal(match[3], undefined, 'Other origins must come from deployment configuration, not literal defaults');
    }
  }
}

const source = readFileSync(require.resolve('./index.js'), 'utf8');
test('clients origin defaults to empty; other origin parameters have no embedded defaults', () => {
  assertParameterDefaults(source);
});
test('parameter guard rejects a client host default, a missing default and an embedded origin', () => {
  for (const invalid of [
    source.replace("{ default: '' }", "{ default: 'https://clients.fixture.test' }"),
    source.replace("'FLUORA_CLIENTES_ORIGIN', { default: '' }", "'FLUORA_CLIENTES_ORIGIN'"),
    source.replace("defineString('FLUORA_APP_ORIGIN')", "defineString('FLUORA_APP_ORIGIN', { default: 'https://app.fixture.test' })")
  ]) assert.throws(() => assertParameterDefaults(invalid));
});
