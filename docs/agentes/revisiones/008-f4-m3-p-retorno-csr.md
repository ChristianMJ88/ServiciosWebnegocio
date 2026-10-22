# Decisión del usuario: volver marketing a CSR

**Veredicto:** la reversión es viable sin tocar lógica de aplicación, planes, RBAC ni UI. No hay bloqueantes de código, pero sí cuatro condiciones (P1) que deben cumplirse antes de dar la reversión por hecha. Solo leí archivos: no ejecuté builds, suites ni git, y no escribí revisión en `revisiones/` (en esta sesión no tengo herramienta de escritura).

## Condición previa (P0, de proceso)

**No restaurar archivos completos con `git checkout`/`restore` contra `4e93dd1`.** Los cambios de P están mezclados con los de 012 en los mismos archivos: `firebase.json` (target `clientes`), `functions/index.js`, `scripts/check-public-builds.cjs`, `prepare/check-clientes-site`, `.firebaserc` y la preview. La reversión debe ser una edición por fragmentos de los cuatro puntos P1. Así 012 queda íntegra.

## Correcciones necesarias (P1)

**1. `angular.json:75-90`: separar la configuración CSR de la de prerender.**
- La configuración `marketing` debe volver a ser CSR: sin `server` ni `outputMode`, y con `tsConfig: "tsconfig.marketing.json"`.
- Crear una configuración solo de diagnóstico (por ejemplo `marketing-prerender`) con exactamente esas tres claves: `server`, `outputMode: "static"` y `tsConfig: "tsconfig.marketing.server.json"`. Se combinaría como `production,marketing,marketing-prerender`.
- El resto se comparte tal cual (styles, index, outputPath).

**2. `scripts/build-marketing.mjs:9-19`: CSR por defecto, prerender solo por parámetro.**
- Añadir el parámetro `prerender=false`. Cuando valga `true`, se agrega la configuración de diagnóstico.
- Si `prerender` es `true` y la salida es `dist/marketing`, está dentro de ella o la contiene, el script debe fallar. La comprobación de la línea 13 solo compara igualdad exacta: hay que ampliarla a contención en ambos sentidos.
- `render-origins.json` debe registrar también `shell: 'csr'|'prerender'`, para que la guarda productiva pueda rechazar un manifiesto de prerender.
- `build-marketing-hydration-probe.mjs:5` debe pasar `prerender: true`. Su salida actual (`dist/marketing-hydration-probe/<mode>`) ya cumple.

**3. `package.json:10` y `check-public-builds.cjs:14-18`: nueva guarda productiva CSR.**
Hoy `postbuild:marketing` y el predeploy llaman a `checkHydrationProbe(..., {production:true})`. Esa función exige `prerendered-routes.json`, `ngh` y `ng-state`, así que un CSR válido fallaría con certeza. Hay que sustituirla por una guarda productiva CSR que:
- llame a `assertCsrShell(index)`. Ya rechaza `ngh`, `ng-server-context`, `ng-state` y un `app-root` con contenido, es decir, cualquier HTML hidratado;
- exija `index === index.csr.html` byte a byte (`csr-shell.js:46` ya lo hace cuando `index` es un shell);
- exija en el manifiesto `mode: 'production'` y `shell: 'csr'`, con orígenes iguales a `runtime-config.js`;
- exija que `browser/runtime-config.js` sea idéntico a `public/runtime-config.js`;
- rechace cualquier `.map`;
- exija que los únicos HTML emitidos sean `index.html` e `index.csr.html`;
- rechace `prerendered-routes.json` y el directorio `dist/marketing/server`. Si `deleteOutputPath` no limpia un artefacto viejo de P, esto lo detecta.

`check-hydration-probe.mjs` se queda tal cual, invocado solo por el script de diagnóstico. Las líneas 21-54 de `check-public-builds.cjs` (assets, SEO renderers, fallback `**`→`/index.csr.html`) no cambian.

**4. Pruebas Node que hoy fijan el comportamiento de P.**
- **`marketing-hydration-wiring.test.mjs:23-34`:** invertir. Debe afirmar que `marketing` no tiene `server`/`outputMode`, que la configuración de diagnóstico tiene exactamente esas tres claves y que `postbuild` no contiene `check-hydration-probe`. Añadir el rechazo de prerender hacia `dist/marketing`.
- **`check-public-builds.test.mjs`:** el escenario `valid` debe ser CSR con `index` igual a `csr`. Negativos:
  - index hidratado
  - `prerendered-routes.json` presente
  - `server/` presente
  - manifiesto `shell:'prerender'`
  - modo development
  - runtime alterado
  - sourcemap
  - assets de otro build (stale)
  - CSR contaminado
  - manifiesto ausente
- **`check-clientes-site`:** añadir un caso positivo con marketing CSR y conservar el rechazo de assets ajenos (líneas 27-28). No necesita cambios de código: `prepare-clientes-site.mjs:32` ya elimina `index.html` y `check-clientes-site.mjs:9,22` ya acepta CSR.

## Sin cambios (confirmado)

- **Functions/SEO:** cargan `/index.csr.html` en remoto (`csr-shell.js:49-55`), así que no se ven afectadas.
- **`firebase.json`:** el fallback de marketing `**`→`/index.csr.html` y el sitio `clientes` siguen correctos.
- **Preview local:** con `--spa-shell csr`, `public-preview.mjs:20` acepta un `index` idéntico al shell.
- **`@angular/ssr` y `platform-server`:** se quedan en devDependencies para el diagnóstico. La comprobación de stats (0 entradas de servidor en el browser) debe repetirse.

## Observación baja (no corregir ahora)

`isHydratableMarketingHome` (`platform-host.service.ts:67-74`) devuelve `true` en `/` aunque el HTML sea CSR, así que el cliente activa `provideClientHydration` sin estado de hidratación. En producción es inocuo, y en desarrollo puede dar un aviso NG0505 (aviso, no error). El build CSR de `4e93dd1`, que dio el baseline de 71, ya funcionaba así. Cambiarlo sería un microajuste de aplicación que el usuario excluyó: solo documentarlo.

## Evidencias (`008-f4-m3-p-resumen.json`)

- **`status`:** pasarlo a «P4 por debajo de la meta; usuario decidió volver a CSR; prerender `da5fbb…` descartado como candidato». No borrar ninguna corrida.
- **`prerender.spaShell: "csr"`:** es ambiguo, porque describe el fallback y no la Home. Añadir `homeShell: "prerender"` y `fallbackShell: "csr"`, o un campo equivalente.
- **`criteria.clsAtMost001`:** el nombre sugiere .001 pero el umbral es .01. Renombrarlo o aclararlo.
- **Nuevo SHA CSR:** registrarlo en un bloque separado (`csr-revert`), sin heredar el 71 del build archivado `86a0c0…` ni el 82 de `da5fbb…`, y sin serie Lighthouse nueva para buscar el 85.

## Validación propuesta (secuencial, sin builds ni suites simultáneos)

1. `npm run build` completo, con Node 20 y con Node 22.
2. `check:public-builds`, `build:clientes` y `check-clientes-site`, todos con exit 0.
3. Comprobar en `dist/marketing/browser` que `index` y `index.csr.html` son idénticos y que no existen `server/` ni `prerendered-routes.json`.
4. Correr el diagnóstico `build:marketing:hydration-probe` hacia su propia carpeta y confirmar que `dist/marketing` no cambia (huella antes y después).
5. Ejecutar la batería Node completa y anotar el nuevo total frente a 158.
6. Ejecutar Angular 387/92 tres veces seguidas en modo normal.
7. Hacer una verificación funcional CSR en preview a 375, 820 y 1440:
   - navegación, anclas, menú, Escape y foco;
   - CTA hacia `/registro`;
   - rutas de tenant, landing y directorio;
   - sin overflow y sin errores de consola.
8. Reportar el SHA de la verificación funcional sin afirmar la meta de 85.

Sin commit, sin sitios, sin despliegue y sin empezar 013.
