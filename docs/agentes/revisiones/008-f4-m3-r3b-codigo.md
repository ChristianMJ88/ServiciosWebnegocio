# Revisión R3-b: probe de build antes del navegador

**Veredicto: R3-b aprobado para pasar a R3-c, sin bloqueantes.** Antes de la primera corrida de R3-c hay que aplicar M1. M2 y los puntos bajos pueden ir dentro de R3-c.

Solo hice lectura con Read, Glob y Grep. No ejecuté builds, suites, pruebas Node ni navegador, así que las cifras (exit 0, 1 ruta, 18/18) son de Codex y no las verifiqué yo. No leí `.env`, `postman/` ni credenciales. Esta sesión no tiene herramienta de escritura, por lo que **no guardé `docs/agentes/revisiones/008-f4-m3-r3b-codigo.md`**; el script de revisión puede guardar este texto tal cual.

## Decisión sobre `prerender` con `outputMode: static`: suficiente

Lo comprobé en el código 21.0.6:
- **El builder fuerza el prerender.** En `@angular/build/.../application/options.js:120-131`, con `outputMode` activo hace `options.prerender = !!options.server` y solo avisa si alguien pasa `prerender`. Después, en `:147-148`, `discoverRoutes` queda en `true` por defecto. Poner `discoverRoutes:false` no tendría efecto y daría una falsa sensación de control, así que hicisteis bien en no ponerlo.
- **Lo que manda son las ServerRoutes.** En `@angular/ssr/fesm2022/ssr.mjs:700-801`, el "descubrimiento" recorre el router y aplica a cada ruta el `renderMode` del árbol de ServerRoutes.
  - Con `''→Prerender` y `'**'→Client` (`app.routes.marketing.server.ts:4-5`), ninguna otra ruta se prerenderiza.
  - Un patrón de servidor sin ruta equivalente en el cliente es error de build (`:789`), y una ruta sin patrón también (`:544`). El comodín cubre ese caso.
- **La guarda cierra el resto:** exige que el manifiesto sea exactamente `['/']` (`check-hydration-probe.mjs:14`) y que el HTML emitido sea exactamente `index.html` e `index.csr.html` (`:44`).

**Alternativa en v21, que no recomiendo:** el modo heredado, sin `outputMode`, con `server`, `ssr:false` y `prerender:{discoverRoutes:false, routesFile}`. Es soportado, pero es otra cadena distinta de la que llevará el prerender productivo (`outputMode:'static'`), el mismo motivo por el que el diseño §1.2 descartó un render hecho a mano.

**Condición pequeña:** que la guarda estática fije la decisión. En `marketing-origins.test.mjs:24`, añadir:
- `outputMode === 'static'` (nunca `'server'`);
- que no existan las claves `prerender`, `ssr` ni `appShell` en `marketing-hydration-probe`.

Así nadie podrá añadir después un `prerender` que el builder ignora solo con un aviso.

## Hallazgos

| ID | Prior. | Ubicación | Hallazgo | Prueba esperada |
|---|---|---|---|---|
| M1 | **Media** | `check-hydration-probe.mjs:11-12,35-38`; `build-marketing-hydration-probe.mjs:22` | **Nada demuestra que el `define` llegó al bundle del servidor.** `render-origins.json` lo escribe el propio script a partir de `readMarketingOrigins`, y la guarda lo compara con esa misma función: la comparación siempre coincide. El bucle de orígenes absolutos solo pasa sin quejarse si no hay URLs absolutas; no comprueba que estén. | Exigir en positivo que el `href` del CTA del hero empiece por `${appOrigin}/registro`. Viene de `offer.href()` → `host.appUrl('/registro')` (`marketing-offer.service.ts:17`) en el servidor, así que es la evidencia directa del contexto. Actualizar el fixture (hoy `<a class="fl-button">` no tiene `href`) y añadir dos casos negativos: `href` relativo y `href` con otro origen. Dejar el manifiesto solo como trazabilidad. |
| M2 | Media-baja | `check-hydration-probe.test.mjs:25-34` | Hay ramas de la guarda sin ningún caso negativo, aunque el diseño las pedía: | Añadir los casos de rechazo: |
| | | | - HTML extra emitido con el manifiesto correcto (`:44`) | `browser/negocios/index.html` con manifiesto `['/']` |
| | | | - manifiesto distinto del runtime | `render-origins.json` con otro `appOrigin` |
| | | | - `ngcm` ausente en CSR o presente en `index.html` | ambos casos por separado |
| | | | - «Prueba sin tarjeta» o `localhost` en el HTML | un caso por cada texto |
| | | | - dos `script[type=application/json]` | un segundo `script` de estado |
| B1 | Baja | `package.json:8,12` | El probe se salta `prebuild:marketing` (`check:marketing-css`). R3-d va a medir reservas con este build: con un subconjunto de CSS desactualizado, el probe pasa y el build de producción falla. | El script del probe ejecuta antes `check:marketing-css`, o R3-c lo exige y registra su resultado. |
| B2 | Baja | `marketing-hydration-wiring.test.mjs:15-20` | Nada impide que `main.marketing.ts` (navegador) importe `app.config.marketing.server` o `@angular/ssr`. | Añadir `doesNotMatch(/app\.config\.marketing\.server\|@angular\/ssr\|platform-server/)` sobre `src/main.marketing.ts` y `app.config.marketing.ts`. |
| B3 | Baja | `check-hydration-probe.mjs:35-36` | El control de orígenes ignora `//host` (protocolo relativo), `srcset`, `action` y `content` de las meta, y `HTTPS:` en mayúsculas. Que no se compruebe `http://` como texto es aceptable si es por los `xmlns` de SVG, pero hay que registrarlo como límite. | Normalizar y cubrir `//` y `srcset`, o documentar el límite en la respuesta. |
| B4 | Baja | `build-marketing-hydration-probe.mjs:18-22` | Si `ng build` falla antes de limpiar la salida (al validar opciones), quedan artefactos de una corrida anterior que la guarda aprobaría. | R3-c ejecuta la guarda solo después de un build con exit 0 en la misma cadena. Opcional: incluir en el manifiesto el nombre de `main-*.js` y que la guarda lo compare con `index.html`. |
| I1 | Info | `tsconfig.marketing.server.json:3` | `types:["node"]` también se aplica al compilar el bundle de navegador del probe. Producción no se ve afectada porque usa `tsconfig.marketing.json`. Ayuda a explicar la diferencia de huella del probe. | Mencionarlo en la respuesta. |

## Seguridad, limpieza y límites: correcto

- **Orígenes:**
  - la evaluación en `vm` usa un contexto mínimo, sin `process` ni `require`, con `codeGeneration` desactivado y `timeout` (`marketing-origins.cjs:20-22`);
  - se valida que cada origen sea canónico, HTTPS, sin credenciales ni ruta, distinto de `build.invalid` y con hostnames distintos entre sí (`:9,14`);
  - los mensajes de error no repiten el valor recibido.
- **`define`:** usa `JSON.stringify` sobre el origen ya validado y un array de argumentos con `shell:false` (`build-marketing-hydration-probe.mjs:11-15`). No hay `define` ni literales en `angular.json`, y una prueba lo comprueba.
- **Servidor:** falla cerrado sin `define`. `parseProductionMarketingRenderContext` exige HTTPS y orígenes distintos (`marketing-render-context.ts:38-43`), y el spec cubre `undefined`, `http:`, orígenes iguales y una ruta distinta de `/`.
- **Limpieza y errores:**
  - se reenvían `SIGINT`/`SIGTERM` al proceso hijo y se retiran los listeners al terminar;
  - una salida por señal da `code ?? 1`;
  - los fixtures se borran con `t.after(rm)` y las ventanas de JSDOM se cierran en `finally`; JSDOM no ejecuta scripts.
- **Lock:** solo añade `@angular/platform-server` y `@angular/ssr` en 21.0.6 exactos, como `dev`, más `xhr2` 0.2.1. No vi cambios de versión en otros paquetes.
- **Producción:**
  - la configuración `marketing` no tiene `server`, `outputMode` ni `prerender`;
  - `build` y `firebase.json` no hacen referencia al probe;
  - `assertCsrContract` no cambia (`functions/csr-shell.js:41-47`), y su rama de "home prerenderizado distinto" ya estaba prevista.

**Límites de esta revisión:**
- No revisé el diff completo de `package-lock.json`, solo las entradas nuevas.
- Tampoco revisé el build en modo desarrollo, que sigue en curso.
- No hay acreditados ni reutilización de DOM ni event replay, y nada de R3-c, R3-d ni R3-e.

**Para R3-c:** aplicar M1 y la condición sobre `outputMode` antes de la primera corrida de navegador, porque el probe da por buenos los orígenes del artefacto. M2 y B1–B4 pueden entrar en R3-c, con su registro en la respuesta. Esta revisión no autoriza commit, push, deploy ni el prerender productivo.
