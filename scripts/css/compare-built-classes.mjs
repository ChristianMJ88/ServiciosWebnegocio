// Structural comparison across minifiers; never claims pixel equivalence.
import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
import postcss from 'postcss';

const [before, after] = process.argv.slice(2);
if (!before || !after) throw new Error('Pass the reference and candidate CSS paths');
async function inventory(file) {
  const classes = new Set(), animations = new Set();
  const ast = postcss.parse(await readFile(file, 'utf8'));
  ast.walkRules(rule => {
    for (const match of rule.selector.matchAll(/\.([A-Za-z_][A-Za-z0-9_-]*)/g)) classes.add(match[1]);
  });
  ast.walkAtRules(rule => { if (/keyframes$/.test(rule.name)) animations.add(rule.params); });
  return { classes: [...classes].sort(), animations: [...animations].sort() };
}
const reference = await inventory(before), candidate = await inventory(after);
assert.deepEqual(candidate.classes, reference.classes, 'CSS classes must survive the builder migration');
assert.deepEqual(candidate.animations, reference.animations, 'Global animations must survive the builder migration');
console.log(`Matching global CSS inventory: ${candidate.classes.length} classes, ${candidate.animations.length} animations; visual comparison remains separate`);
