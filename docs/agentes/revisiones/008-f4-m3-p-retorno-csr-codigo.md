**Veredicto: no encontré bloqueantes.** La guarda productiva CSR es segura y coherente con 012, y no veo regresiones. Quedan tres hallazgos de prioridad baja y algunas pruebas por añadir. Solo leí archivos: no ejecuté builds, suites ni git, y no creé `revisiones/008-f4-m3-p-retorno-csr-codigo.md` (en esta sesión no puedo escribir archivos). Codex puede registrarla a partir de este texto.

## Lo que confirmé

- **`angular.json:78-111`:** `marketing` es CSR (sin `server` ni `outputMode`, con `tsconfig.marketing.json`). `marketing-prerender` tiene exactamente las tres claves, y las opciones base (`:19-36`) no aportan `server` ni `prerender`, así que no hay nada que heredar.
- **`build-marketing.mjs`:**
  - `:14-16`: la comprobación de contención funciona en ambos sentidos (igual, dentro y contiene), y un directorio vecino como `dist/marketing-hydration-probe` no da un falso positivo.
  - `:35`: la línea de comandos no expone `--prerender`, y `parseArgs` estricto rechaza esa opción. El prerender solo se activa desde el wrapper.
  - `:17`: borrar `render-origins.json` antes del build garantiza que un build fallido no deje un manifiesto válido.
  - `:31`: registra `shell`.
- **Desviación aceptada: `prerendered-routes.json`.** En `check-marketing-csr.mjs:26-29`, `deepEqual` estricto acepta solo `{routes:{}}`. El JSON corrupto, `EISDIR` y cualquier otra estructura se vuelven a lanzar, porque `error.code !== 'ENOENT'`, así que no hay forma de que pase por error. No se borra el archivo. Me parece correcto.
- **Guarda productiva (`check-marketing-csr.mjs`).** Cubre cada punto del diseño:
  - manifiesto `production` + `csr` con orígenes iguales al runtime (`:11-16`);
  - runtime idéntico a la fuente revisada;
  - `assertCsrShell(index)`;
  - igualdad byte a byte por `csr-shell.js:46`;
  - rechazo de `server/`;
  - rechazo de `.map`;
  - solo `index.html` e `index.csr.html`, con recorrido recursivo que detecta subrutas prerenderizadas.
  - Un manifiesto anterior al cambio, sin `shell`, se rechaza.
- **Cableado.** `postbuild:marketing` ya no llama a `check-hydration-probe`. El script del probe no dispara `postbuild:marketing`, porque npm asocia los hooks al nombre exacto del script.
- **Coherencia con 012.**
  - El predeploy de `clientes` (`firebase.json:189`) ahora pasa por `checkMarketingCsr`.
  - `prepare-clientes-site.mjs:26` y `check-clientes-site.mjs:12` aceptan CSR, ya que el contrato exige igualdad byte a byte.
  - El orden `build:marketing` (con postbuild) → `build:app` → `build:clientes` sigue garantizando que exista `index.csr.html` antes de calcular `hostingBuildId`. Se añadió el caso `valid-csr`.
  - El fallback `**`→`/index.csr.html` sigue en su sitio.

## Hallazgos

| # | Prioridad | Ubicación | Problema | Propuesta |
|---|---|---|---|---|
| B1 | Baja (ya existía, no es regresión) | `build-marketing.mjs:16` | Un build de producción CSR con salida que se solapa sin ser igual sigue permitido. Con `--output dist`, `deleteOutputPath` borraría también `dist/application` y `dist/clientes`. Con `--output dist/marketing/browser`, la salida quedaría anidada dentro de la carpeta pública de Hosting. | Aplicar `if (overlapsProduction && (mode !== 'production' \|\| prerender \|\| output !== production)) throw …`. Prueba: añadir `dist` y `dist/marketing/browser` con `mode:'production'` sin prerender a `marketing-hydration-wiring.test.mjs:41`. |
| B2 | Baja | `csr-hydration-probe.mjs:32` | El probe CSR no visita `/`. Es intencional: `isHydratableMarketingHome` activa `NgHydration` en `/`, y la aserción de `:66` fallaría (es la observación baja del diseño). La Home en CSR, por tanto, no tiene verificación automática en navegador. | No cambiar código. La verificación funcional manual a 375/820/1440 debe incluir `/` (anclas, menú, Escape, CTA `/registro`) y registrarlo por separado. |
| B3 | Baja | `csr-hydration-probe.mjs:60`, `check-hydration-probe.mjs:13-16` | Hay ramas muertas: en `--rendering csr` solo se acepta producción, así que la rama `mode==='development'` nunca se ejecuta, y nadie pasa ya `--production` a `checkHydrationProbe`. Además, el diagnóstico no exige `manifest.shell === 'prerender'`. | Solo documentarlo. Si se toca más adelante, añadir `assert.equal(manifest.shell,'prerender')` en el diagnóstico. |

## Pruebas faltantes (`check-public-builds.test.mjs:12`)

- **Estructura de `prerendered-routes.json`.** La solicitud dice que se rechaza «cualquier ruta u otra estructura», pero solo se prueba `{routes:{'/':{}}}`. Añadir negativos para `{}`, `{routes:{},extra:1}`, `{routes:[]}`, JSON inválido y un directorio con ese nombre.
- **Manifiesto anterior sin `shell`.** Añadir un manifiesto `{mode:'production',...origins}` y esperar el rechazo.
- **Desvío de bytes:** `index.html` e `index.csr.html` como shells vacíos válidos que difieren en un espacio. Debe fallar por `csr-shell.js:46`.
- **HTML anidado:** `browser/negocios/index.html`. Hoy `extra-html` solo prueba la raíz.
- **Motivo del rechazo (opcional).** Los negativos solo comprueban `status !== 0`. Comparar `run.stderr` con el mensaje esperado de cada escenario evitaría que pasen por una causa distinta. El riesgo es bajo, porque `valid` y `valid-empty-routes` anclan el mismo fixture.

## Límites

- No confirmé empíricamente que Angular 21 emita `{routes:{}}` en CSR: me baso en la evidencia de build20.
- `path.resolve` no resuelve enlaces simbólicos, así que un symlink hacia `dist/marketing` esquivaría la comprobación de solapamiento. Es aceptable como límite local.
- `public/runtime-config.js` se lee de forma relativa al directorio actual. Funciona porque npm y el predeploy de Firebase se ejecutan desde la raíz.
- La validación sigue pendiente en el orden del diseño:
  1. build20/22;
  2. `check:public-builds` y clientes;
  3. comprobar que `server/` no existe y `prerendered-routes.json` está vacío;
  4. huella de `dist/marketing` antes y después del probe;
  5. stats con 0 entradas de servidor;
  6. batería Node frente a 158;
  7. Angular tres veces;
  8. verificación en navegador.

  No hace falta una serie Lighthouse nueva.
