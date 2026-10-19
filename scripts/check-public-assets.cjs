// Prevent global third-party render blockers and missing self-hosted font assets.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');

for (const file of ['src/index.html', 'src/index.app.html']) {
  assert.doesNotMatch(read(file), /fonts\.googleapis\.com|fonts\.gstatic\.com|cdnjs\.cloudflare\.com/i, file);
}
const fonts = read('src/app/shared/styles/tenant-fonts.css');
const faces = [...fonts.matchAll(/@font-face\s*\{([^}]+)\}/g)];
assert.equal(faces.length, 4, 'Preserve every Playfair Unicode subset');
for (const [, face] of faces) {
  assert.match(face, /font-family:\s*'Playfair Display'/);
  assert.match(face, /font-weight:\s*700/);
  assert.match(face, /font-display:\s*swap/);
  assert.match(face, /unicode-range:/);
  const url = face.match(/url\((\/fonts\/tenant\/[a-z0-9-]+\.woff2)\)/)?.[1];
  assert.ok(url, 'Font URL must be a local tenant asset');
  const bytes = fs.readFileSync(path.join(root, 'public', url));
  assert.equal(bytes.subarray(0, 4).toString(), 'wOF2', url);
}
const animations = read('src/app/pages/booking/booking-animate.css');
assert.match(animations, /Permission is hereby granted/);
assert.doesNotMatch(animations, /:root|var\(--animate-/);
const templates = read('src/app/pages/booking/booking.component.html');
const effects = [...new Set([...templates.matchAll(/animate__([a-zA-Z]+)/g)].map(m => m[1]))].filter(name => name !== 'animated');
for (const effect of effects) {
  assert.ok(animations.includes(`.animate__${effect} {`), `Missing Booking effect: ${effect}`);
  assert.ok(animations.includes(`@keyframes ${effect} {`), `Missing keyframes: ${effect}`);
}
assert.match(read('src/app/pages/booking/booking.component.ts'), /styleUrls:[^\n]+'\.\/booking-animate\.css'/);
assert.match(read('src/app/pages/booking/booking.component.css'), /prefers-reduced-motion:\s*reduce/);
console.log(`Public assets verified: ${faces.length} local font subsets, ${effects.length} scoped Booking effects, no global font/animation CDN.`);
