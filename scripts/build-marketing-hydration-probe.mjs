import { parseArgs } from 'node:util';
import { buildMarketing } from './build-marketing.mjs';

const { values } = parseArgs({ options: { mode: { type: 'string', default: 'production' } } });
await buildMarketing({ mode: values.mode, output: 'dist/marketing-hydration-probe/' + values.mode });
