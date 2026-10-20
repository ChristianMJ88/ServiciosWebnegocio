No blockers, so the paired M1/M2 measurement can go ahead now. Before P2, I recommend fixing three Medium issues in `scripts/lighthouse-tenant-cache.mjs`, because they affect what the result proves. I couldn't save the file because this session has no write tool. The text below is ready to go in `docs/agentes/revisiones/008-f4-m2-codigo.md`, authored by Claude.

**Scope:** I only read `angular.json`, `check-public-builds.cjs`, `admin-dashboard.component.spec.ts`, `lighthouse-tenant-cache.mjs`, `public-preview.mjs`, `lighthouse-home.mjs` and `firebase.json`. I did not run builds, test suites or Lighthouse. The figures come from the request.

## Closed
- **`angular.json`:**
  - `browser` replaces `main` in `options` (`:22`), `marketing` (`:76`) and `application` (`:90`).
  - `outputPath` is an object in all three builds.
  - The dev-server builder is `@angular/build:dev-server` and keeps `proxyConfig`.
  - `test-build` is unchanged.
- **`check-public-builds.cjs:13-17,25-26`:** it checks that the classic `runtime-config.js` comes before the first `type="module"` and that `__FLUORA_TENANT__` comes before `main-`. It strips `<noscript>`, so the `media="print"` link from `inlineCritical` doesn't count twice.
- **Spec NG8107 (`spec.ts:15-21`):** it now renders, holds the requests with `match` and checks that `h1`/`p` are empty. This closes the previous Low finding. The final suite result still has to be reported.
- **The `cache` field is in Lighthouse 13.4.1 reports.** I checked it in `tmp/lighthouse/m1-paired/m1-1.json:706`, where it's `"none"`. The P2 filter can work.

## Findings

| Pri. | Location | Finding | Fix and check |
|---|---|---|---|
| Medium | `lighthouse-tenant-cache.mjs:69-70` together with `public-preview.mjs:65` | **The warm run overstates the cache for `tenant-public.css`.**<ul><li>The preview serves every asset with `max-age=604800`, including `tenant-public.css`, which has no hash.</li><li>Production doesn't do that. Today it relies on Firebase's default, and D1 plans `no-cache`, which means revalidating on every visit.</li><li>So the warm figure would be a best case that won't exist after D1.</li></ul> | <ul><li>Minimum: add `cacheHeaders: "preview max-age=604800; not D1"` to `summary.json` and say so in the report.</li><li>Better: add an opt-in flag to the preview so `/tenant-public.css` is served with `no-cache` plus `ETag`/304. Leave the default unchanged so `lighthouse-home` isn't affected.</li><li>Check: in the warm run with D1, `tenant-public.css` shows a 304, not `disk`.</li></ul> |
| Medium | `lighthouse-tenant-cache.mjs:67-70` | **The checks don't prove that both runs are what they claim.**<ul><li>For warm, one cached `.css`/`.js` file is enough.</li><li>The cold run isn't checked to be really cold.</li><li>Nothing confirms that the tenant actually loaded. If the path falls through to the marketing home without a 4xx, it still counts as valid.</li></ul> | <ul><li>Cold: no same-origin resource with `cache !== 'none'`.</li><li>Both runs: `/tenant-public.css` requested, and at least one tenant `/api/v1/publico/` request returning 200 (not counting the support check at `:46`).</li><li>Warm: every same-origin `.js`/`.css` except `runtime-config.js` (which is `no-store`) comes from cache, including `main-*`, `styles-*` and the lazy chunks. The only exception is the case covered by the D1 option above.</li></ul> |
| Medium | `lighthouse-tenant-cache.mjs:71-74` | **Not enough metrics to explain cold vs warm.** It only saves scores and CLS. FCP, LCP, TBT, Speed Index and transferred bytes are missing, and those are what the cache changes. | <ul><li>Save the same fields as `lighthouse-home.mjs:113-118`, including `resource-summary`.</li><li>Reject a `null` score, as `:110` does.</li><li>Note that warm uses Lantern's estimate for disk-cache reads; it is not measured.</li></ul> |
| Low | `lighthouse-tenant-cache.mjs:30-38,79-81` | **Provenance doesn't match the home measurement.**<ul><li>The fingerprint uses a different order (`localeCompare` per directory) and different separators from `lighthouse-home.mjs:30-45`. So the same build gets a different SHA, and P2 can't be tied to the M2 build.</li><li>The commit, `sourceDirty` and the `public-preview.mjs` hash are also missing.</li></ul> | Use the same algorithm as `lighthouse-home`, ideally shared in `scripts/performance/`, and the same `measurementCode` block (`:130-134`). Check: P2 and home give the same SHA for `dist/marketing/browser`. |
| Low | `lighthouse-tenant-cache.mjs:52,57` | <ul><li>Report files are named after width only, so two sizes with the same width overwrite each other.</li><li>1440×900 runs with mobile throttling, which is valid but needs a label.</li></ul> | <ul><li>Name reports `${width}x${height}-${cache}.json`.</li><li>Label the 1440 size in the summary as "desktop viewport, mobile throttling".</li></ul> |

## Not findings
- Each Lighthouse run with `--port` opens a new tab. Hits will come from `disk`, not `memory`, and the filter accepts both. chrome-launcher creates and removes a temporary profile, and the `finally` (`:77,83`) closes Chrome, the preview and the child process.
- An occupied port fails at `:45` without touching 4200/8080.
- `@angular-devkit/build-angular` (`package.json:56`) is unused after M2. Removing it is a separate step that needs the user's approval.

## Before measuring
1. Report the final suite result and the `check:public-builds` result on the M2 build.
2. Stop 4175 and check that the port is free. Record the proxy check (`/api/v1/publico/plataforma/soporte` returns 200 through the dev-server).
3. Tell the user that restarting 4200 will switch it to Vite, and that hostnames that aren't localhost or LAN need `allowedHosts`. That's their decision.
4. Paired M1/M2 measurement with `--compare-root tmp/builds/m1`, keeping the "target 85" wording if the median stays below it.
5. P2 after the three Medium fixes, labelled as one sample per size and cache state.

## Unchanged
- Full visual equivalence is still unproven. The compiled class inventories don't replace a pixel comparison.
- D1 is still required before any deployment and needs the user's approval. Don't touch hosting.
- Other agents' work in the tree is preserved. No commit, push or deployment.
