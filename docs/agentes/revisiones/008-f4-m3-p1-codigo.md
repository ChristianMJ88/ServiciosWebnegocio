# Revisión P1: build productivo local de marketing

Revisé en solo lectura los archivos que pediste. No ejecuté builds, pruebas, Git ni servicios. Lo que diga el build local en verificación no sale de esta revisión.

## Lo que se cumple

- **Un único camino de build.** La configuración `marketing` de `angular.json:75-90` incluye `server`, `outputMode: 'static'` y `tsconfig.marketing.server.json`. Ya no existe `marketing-hydration-probe`. `build:marketing` y el wrapper `build-marketing-hydration-probe.mjs:5` llaman a la misma función `buildMarketing`. Los orígenes salen de `readMarketingOrigins` y se pasan con `--define`. `marketing-hydration-wiring.test.mjs:23-32` comprueba esta estructura.
- **Solo la raíz.** `check-hydration-probe.mjs:14` exige que `prerendered-routes.json` contenga únicamente `/`. La línea 51 exige que el directorio tenga exactamente `index.html` e `index.csr.html`. Hay pruebas negativas para ambas cosas (`.test.mjs:44,46`).
- **Rechazo del shell contaminado.** Lo cubren `assertCsrContract` (`functions/csr-shell.js:41-47`, que exige `app-root` vacío, sin `ngh`, `ng-state` ni `ng-server-context`, y los mismos main, estilos y base), la comprobación de `ngcm` (`check-hydration-probe.mjs:30-31`) y `prepareCsrShell`, que se niega a copiar un home que no esté vacío. Esto último tiene prueba en los casos `polluted` y `stale` de `prepare-csr-shell.test.mjs`.
- **Guardas sobre el artefacto.** `postbuild:marketing` ejecuta `prepare-csr-shell` y `check-hydration-probe dist/marketing`. `check-public-builds.cjs:14-17` llama a `checkHydrationProbe`. Las líneas 34-43 siguen pasando `index.csr.html` a los renderers SEO, y la línea 48 obliga a que el rewrite `**` de marketing apunte a `/index.csr.html`. Ese rewrite es lo que impide que las demás rutas reciban el home.
- **`application` y Functions intactos.** `build:app` y la configuración `application` no cambian. `functions/` y `firebase.json` no aparecen modificados.

## Hallazgos

### 1. Media: un artefacto de desarrollo puede pasar como productivo
- **Dónde:** `build-marketing.mjs:9-10,30` y `check-hydration-probe.mjs:11-12`.
- **Motivo:**
  - `node scripts/build-marketing.mjs --mode development` escribe en `dist/marketing` por defecto. Como se invoca directamente, el `postbuild` no se ejecuta.
  - `check-public-builds` (el `predeploy`) solo compara los orígenes del manifiesto, no `manifest.mode`.
  - Un build `development` no tiene optimización, incluye sourcemaps y usa `environment.development.ts`. La guarda de `localhost` solo revisa el HTML, no el JS.
- **Corrección:**
  - `checkHydrationProbe` (o `check-public-builds` para `dist/marketing`) debe exigir `manifest.mode === 'production'` y que no haya archivos `*.map` en `browser/`.
  - Como opción adicional, `buildMarketing` puede rechazar `mode: 'development'` cuando la salida resuelva a `dist/marketing`.
- **Prueba:** casos negativos con `{mode:'development'}` y con un `.map` presente, más un caso positivo con `production`.

### 2. Media: la guarda de planes, precios y tenant depende de unas pocas clases
- **Dónde:** `check-hydration-probe.mjs:32-33`.
- **Motivo:**
  - Solo bloquea `.fl-home-plan-card`, `.fl-home-trial-banner`, `[data-fluora-support]`, los enlaces de las categorías y el literal «Prueba sin tarjeta».
  - El banner real (`plans.component.html:3`) dice «Prueba {{nombre}} durante N días… Sin tarjeta.», así que ese literal no lo detecta.
  - Tampoco se cubren `.fl-home-plan-price`, un marcador de tenant como `__FLUORA_TENANT__` ni los enlaces `/e/<slug>`.
  - Si un cambio de marcado renombra la tarjeta, se podrían hornear precios sin que la guarda falle.
- **Corrección:** agregar `.fl-home-plan-price` al selector y rechazar `__FLUORA_TENANT__`, `href="/e/` (o un `/e/` absoluto), `Sin tarjeta`, `/\bdiasPrueba\b/` y patrones de moneda como `/\$\s?\d|MXN|\/mes\b/`.
- **Prueba:** un caso negativo por patrón en `check-hydration-probe.test.mjs`.

### 3. Baja: el origen se valida contra el fuente, no contra el archivo que se publica
- **Dónde:** `check-hydration-probe.mjs:9` y `check-public-builds.cjs:17`.
- **Motivo:** `runtimeFile` se resuelve con `path.resolve('public/runtime-config.js')`, que depende del directorio de trabajo. Lo que se publica es `dist/marketing/browser/runtime-config.js`.
- **Corrección:** en `check-public-builds`, pasar `path.join(root, 'runtime-config.js')`, o comprobar también que ese archivo sea byte a byte igual al de `public/`. Así la guarda funciona desde cualquier directorio y valida exactamente lo que se despliega.
- **Prueba:** un caso con el `runtime-config.js` del artefacto distinto al del fuente, que debe fallar.

### 4. Baja: el wiring no fija la cadena de guardas
- **Dónde:** `marketing-hydration-wiring.test.mjs:23-32`.
- **Motivo:** la prueba no verifica `postbuild:marketing` ni la combinación de configuraciones `production,marketing`. Si alguien quita la guarda del `postbuild`, la prueba sigue en verde.
- **Corrección:** comprobar que `pkg.scripts['postbuild:marketing']` incluye `prepare-csr-shell.mjs --root dist/marketing/browser` y `check-hydration-probe.mjs dist/marketing`, y que `build-marketing.mjs` contiene `',marketing'` y no define otra configuración.

### 5. Baja: falta una prueba positiva de `check-public-builds` con el home prerenderizado
- **Dónde:** `prepare-csr-shell.test.mjs:30-41`.
- **Motivo:** solo hay un caso negativo, cuando falta el shell. Nada prueba de punta a punta que un `dist` con home prerenderizado, shell vacío del mismo build, manifiestos y `application` pase la guarda, ni que la rechace si `index.csr.html` es igual al home.
- **Corrección:** agregar ese fixture, con un caso positivo y uno negativo (shell igual al home).

### 6. Informativo, sin acción
`tsconfig.marketing.server.json:3` agrega `types: ["node"]` también al entry del navegador. Es el patrón habitual de Angular SSR. La comprobación de que `platform-server` no termina en los JS del navegador ya está prevista en P3.

## Límites
- No revisé `firebase.json` (P2), `public-preview.mjs`, `src/` más allá de las clases que usa la guarda, ni las mediciones.
- No confirmé que Angular 21 limpie `dist/marketing` antes de cada build. Que no queden HTML o `prerendered-routes.json` viejos depende de `deleteOutputPath`, cuyo valor por defecto es `true`.

**Prioridad para Codex:** corregir 1 y 2 antes de cerrar P1. Los hallazgos 3 a 5 pueden ir en el mismo cambio, sin ampliar el alcance.
