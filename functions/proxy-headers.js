'use strict';

function copyUpstreamHeaders(upstreamHeaders, response) {
  const directHeaders = [
    ['content-type', 'Content-Type'],
    ['cache-control', 'Cache-Control'],
    ['location', 'Location'],
    ['retry-after', 'Retry-After']
  ];

  for (const [upstreamName, responseName] of directHeaders) {
    const value = upstreamHeaders.get(upstreamName);
    if (value) response.set(responseName, value);
  }

  const cookies = typeof upstreamHeaders.getSetCookie === 'function'
    ? upstreamHeaders.getSetCookie()
    : [upstreamHeaders.get('set-cookie')].filter(Boolean);
  for (const cookie of cookies) {
    response.append('Set-Cookie', cookie);
  }

  if (!upstreamHeaders.get('cache-control')) {
    response.set('Cache-Control', 'no-store');
  }
}

module.exports = { copyUpstreamHeaders };
