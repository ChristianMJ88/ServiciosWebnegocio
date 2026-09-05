'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { copyUpstreamHeaders } = require('./proxy-headers');

test('propaga todas las cookies y encabezados relevantes del backend', () => {
  const values = new Map([
    ['content-type', 'application/json'],
    ['cache-control', 'private, no-store'],
    ['retry-after', '60']
  ]);
  const upstreamHeaders = {
    get: (name) => values.get(name) ?? null,
    getSetCookie: () => [
      'fluora_refresh=token; Path=/api/v1/auth; Secure; HttpOnly; SameSite=None',
      'otra=valor; Path=/; Secure'
    ]
  };
  const assigned = new Map();
  const appended = [];
  const response = {
    set: (name, value) => assigned.set(name, value),
    append: (name, value) => appended.push([name, value])
  };

  copyUpstreamHeaders(upstreamHeaders, response);

  assert.equal(assigned.get('Content-Type'), 'application/json');
  assert.equal(assigned.get('Cache-Control'), 'private, no-store');
  assert.equal(assigned.get('Retry-After'), '60');
  assert.equal(appended.length, 2);
  assert.match(appended[0][1], /fluora_refresh=.*HttpOnly/);
});

test('evita cachear respuestas cuando el backend no define política', () => {
  const upstreamHeaders = { get: () => null, getSetCookie: () => [] };
  const assigned = new Map();
  const response = {
    set: (name, value) => assigned.set(name, value),
    append: () => undefined
  };

  copyUpstreamHeaders(upstreamHeaders, response);

  assert.equal(assigned.get('Cache-Control'), 'no-store');
});
