'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { assertCsrShell, assertCsrContract, loadCsrTemplate } = require('./csr-shell');
const shell = '<html><head><base href="/"><script src="/runtime-config.js"></script><link rel="stylesheet" href="styles-A.css"><noscript><link rel="stylesheet" href="styles-A.css"></noscript></head><body><app-root> <!-- CSR --> </app-root><script type="module" src="main-A.js"></script></body></html>';

test('accepts only one empty root, including inert comments', () => {
  assert.equal(assertCsrShell(shell), shell);
  for (const bad of [shell.replace('</app-root>', '<h1>Home</h1></app-root>'), shell + '<app-root></app-root>', shell.replace('<app-root>', '<other-root>')]) {
    assert.throws(() => assertCsrShell(bad));
  }
});

test('rejects hydration markers anywhere without removing them', () => {
  for (const marker of ['ng-server-context="ssg"', 'ngh="0"', 'ng-version="21"', '<script id="ng-state">{}</script>']) {
    assert.throws(() => assertCsrShell(shell.replace('<body>', '<body>' + marker)), /hydration state/);
  }
});

test('rejects stale assets/base/runtime/noscript contracts; permits future home content', () => {
  assert.doesNotThrow(() => assertCsrContract(shell, shell));
  assert.doesNotThrow(() => assertCsrContract(shell, shell.replace('</app-root>', '<h1>Home</h1></app-root>')));
  for (const other of [shell.replace('href="/"', 'href="/other/"'), shell.replaceAll('main-A.js', 'main-B.js'), shell.replaceAll('styles-A.css', 'styles-B.css'), shell.replace('src="/runtime-config.js"', 'type="module" src="/runtime-config.js"'), shell.replace('<noscript>', '<noscript>extra')]) {
    assert.throws(() => assertCsrContract(shell, other));
  }
  assert.throws(() => assertCsrContract(shell, shell.replace('<!-- CSR -->', '<!-- Different empty shell -->')), /byte-identical/);
});

test('loads the remote CSR, with timeout and without redirects', async () => {
  const requests = [];
  const result = await loadCsrTemplate('https://marketing.example.test', async (url, options) => {
    requests.push(url);
    assert.equal(options.headers.Accept, 'text/html');
    assert.ok(options.signal instanceof AbortSignal);
    assert.equal(options.redirect, 'error');
    return new Response(shell);
  });
  assert.equal(result, shell);
  assert.deepEqual(requests, ['https://marketing.example.test/index.csr.html']);
});

test('404, timeout and polluted HTML fail without any fallback request', async () => {
  for (const failure of ['404', 'timeout', 'home']) {
    const urls = [];
    await assert.rejects(loadCsrTemplate('https://marketing.example.test', async url => {
      urls.push(url);
      if (failure === 'timeout') throw new DOMException('Timeout', 'TimeoutError');
      return failure === '404' ? new Response('', {status:404}) : new Response(shell.replace('</app-root>', '<h1>Home</h1></app-root>'));
    }));
    assert.deepEqual(urls, ['https://marketing.example.test/index.csr.html']);
  }
});
