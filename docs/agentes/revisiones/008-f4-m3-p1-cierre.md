**Dictamen: P1 queda cerrado y se puede pasar a P2.** Las cinco correcciones están hechas. Encontré una prueba que pasa por un motivo distinto del que pretende comprobar. Es de prioridad baja y no bloquea P2. Todo lo revisé solo leyendo archivos: no ejecuté nada. Las 49 pruebas, la guarda real y la cifra del build las tomo tal como las reporta Codex.

## Verificación de los hallazgos 1 a 5

| # | Estado | Evidencia |
|---|---|---|
| 1. Un build de desarrollo podía pasar como productivo | ✅ Corregido | `build-marketing.mjs:13` impide usar un modo distinto de `production` cuando la salida es `dist/marketing`. Con `--production`, `check-hydration-probe.mjs:13-14` exige `manifest.mode === 'production'` y la línea 56 rechaza `.map`. `check-public-builds.cjs:17` activa esa opción y `postbuild:marketing` pasa `--production` (`package.json:10`). Las pruebas `development`, `sourcemap`, `missing-manifest` y el caso positivo `valid` están en `check-public-builds.test.mjs:12`. |
| 2. La guarda de planes, precios y tenant dependía de pocas clases | ✅ Corregido | `.fl-home-plan-price` está en el selector (`:37`). La expresión regular rechaza `sin tarjeta`, `__FLUORA_TENANT__`, `diasPrueba`, `$\d`, `MXN` y `/mes` (`:38`). Los enlaces `/e/` se detectan por el pathname resuelto, así que se cubren las formas relativa, absoluta y sin protocolo (`:39`). Hay un caso negativo por patrón (`.test.mjs:28-36`), incluido el texto real «Prueba … 14 días. Sin tarjeta.». |
| 3. El origen se validaba contra el fuente y no contra lo publicado | ✅ Corregido | Por defecto se lee `root/browser/runtime-config.js` (`:9`). `check-public-builds.cjs:17` pasa el archivo del artefacto. En modo productivo se compara byte a byte con `public/` (`:15`). La prueba es `runtime-drift`. |
| 4. El wiring no fijaba la cadena de guardas | ✅ Corregido | `marketing-hydration-wiring.test.mjs:30-32` fija las dos guardas del `postbuild` (incluido `--production`) y la combinación `mode + ',marketing'`. |
| 5. Faltaba una prueba positiva de `check-public-builds` | ✅ Corregido | Hay un fixture de punta a punta con caso `valid` y negativos `polluted-csr` (shell igual al home) y `stale-assets`. |

## Pendiente menor (baja, no bloquea P2)

**La prueba "rejects a manifest differing from runtime" falla antes de llegar a comparar orígenes.**
- **Dónde:** `check-hydration-probe.test.mjs:57`.
- **Motivo:** el manifiesto de la prueba no incluye `mode`. Por eso la ejecución se detiene en `check-hydration-probe.mjs:12` («Known build mode required») y nunca llega a la comparación de orígenes de la línea 17. Ninguna prueba comprueba hoy que se rechace un manifiesto con origen distinto.
- **Corrección para Codex:** agregar `mode:'production'` al JSON de esa prueba. Como mejora opcional, en los negativos de `check-public-builds.test.mjs:28` comprobar también el mensaje de `run.stderr`, para que cada caso falle por la razón esperada.
- **Validación:** volver a correr las pruebas focales y confirmar que esa prueba falla por la línea 17.

## Nota informativa, sin acción
La comparación byte a byte (`:15`) y `build-marketing.mjs:11` resuelven `public/runtime-config.js` desde el directorio de trabajo. Es suficiente porque npm y el predeploy de Firebase se ejecutan desde la raíz.

No revisé `firebase.json` (queda para P2) ni el trabajo de backend 012.
