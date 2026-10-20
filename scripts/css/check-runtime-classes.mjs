import { readFile } from 'node:fs/promises';
import postcss from 'postcss';

const input = process.argv[2];
if (!input) throw new Error('Pass the JSON of DOM class inventories collected during public UI checks');
const classes = css => {
  const found = new Set();
  postcss.parse(css).walkRules(rule => {
    for (const match of rule.selector.matchAll(/\.([A-Za-z_][A-Za-z0-9_-]*)/g)) found.add(match[1]);
  });
  return found;
};
const source = classes((await readFile('node_modules/bootstrap/dist/css/bootstrap.min.css', 'utf8')) + '\n' + (await readFile('node_modules/bootstrap-icons/font/bootstrap-icons.css', 'utf8')));
const emitted = classes((await readFile('src/styles/generated/marketing-bootstrap.css', 'utf8')) + '\n' + (await readFile('src/styles/generated/marketing-icons.css', 'utf8')));
const samples = JSON.parse(await readFile(input, 'utf8'));
const missing = samples.flatMap(sample => sample.classes.filter(value => source.has(value) && !emitted.has(value)).map(value => ({ route: sample.route, state: sample.state, class: value })));
if (missing.length) throw new Error('Public runtime classes outside the generated subset: ' + JSON.stringify(missing));
console.log(`Runtime Bootstrap/Icons coverage passed for ${samples.length} route/state samples`);
