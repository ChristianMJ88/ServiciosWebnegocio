# Dictamen P3: A1 y M1

**Resultado: A1 y M1 aceptados, sin bloqueantes.** Puedes avanzar a P4 cuando se cumplan dos cosas: que la corrida de las 30 tenant termine en verde con evidencia, y que P3 quede cerrado.

Revisé el código con lectura de archivos y búsquedas. No ejecuté comandos.

## A1: corrección del medidor (aceptado, revisado en el código)

- **El control corre antes de abrir el servidor.** En `scripts/performance/public-preview.mjs:19-20`, `assertCsrShell` y `assertCsrContract` terminan antes de `prepare(root)` y de `http.createServer` (líneas 35-36). Si el shell no es válido, el servidor nunca escucha.
- **El caso `index`** exige que `app-root` esté vacío en `index.html` (`functions/csr-shell.js:10`). También rechaza `ng-server-context`, `ngh`, `ng-version` y `ng-state` (línea 11). La única excepción es `hostingTarget === 'clientes'`, porque ahí el HTML lo sirve la Function. Esa excepción es razonable.
- **El caso `csr`** exige que `index.csr.html` sea un shell vacío y que sus assets coincidan con los de `index.html` (`functions/csr-shell.js:41-46`). Si `index` también es un shell, además deben ser idénticos byte a byte.
- **Pruebas:**
  - Con una Home llena, `index` se rechaza con `/must be empty/` y `csr` pasa (`public-preview.test.mjs:74-90`).
  - Los casos negativos fallan como deben: sin `index.csr.html` da `ENOENT`, un CSR con contenido da `must be empty` y un modo desconocido también se rechaza (líneas 105-109).
  - Los fixtures de caché y de split usan `<app-root></app-root>` vacío (líneas 14, 52 y 115).
- **Todos los medidores llaman con `spaShell:'csr'`:** `hydration-probe`, `csr-hydration-probe`, `navigation-hydration-probe`, `layout-residual-probe`, `home-sections-measure`, `nav-cta-probe`, `font-`, `loading-plans-` y `caption-`. Esto coincide con lo que dices del orquestador y de P4.

## M1: ausencia de código de servidor en el bundle del navegador (aceptado con la evidencia de Codex, no reproducido)

El método es correcto: mismo builder con `--stats`, los 53 outputs JS contrastados con el metafile de esbuild, y el SHA publicado idéntico (`da5fbb28f3934f98…`), lo que descarta cambios para pasar la prueba. No lo reproduje porque esta revisión fue de solo lectura. El resumen de stats queda como evidencia privada y no lo revisé.

Lo mismo vale para B1, B2 y B3, para Node 157/157, Angular 387/92×3, backend 697/0/0/1, la matriz de 32 escenarios (máximo 0.00666), la hidratación en 375/820/1440, el replay y la navegación. Los tomo como acreditados por Codex, sin verificación mía.

## Condiciones para cerrar P3 y abrir P4 (prioridad alta)

1. **Las 30 tenant terminan en verde** sobre el mismo artefacto `da5f…` y con `--spa-shell csr`, con los logs privados del CLI. Si la corrida falla o se corta, no se informa como éxito parcial: P3 sigue abierto.
2. **El caso pendiente del emulador de marketing** tiene resultado propio en el cierre. No basta con que esté cubierto por `hosting-routes.test.mjs`.
3. **ESTADO.md (lo actualiza Codex)** registra el SHA, el modo `csr`, M1 y que las revisiones 012 quedaron sin bloqueantes.

## Observación menor (no bloquea)

Las pruebas actuales no cubren `spaShell:'index'` combinado con `hostingTarget:'clientes'` cuando falta `index.html`. Hoy el código no lo lee, lo cual es correcto, pero ninguna prueba lo deja fijado. Conviene agregarla si se modifica `public-preview.mjs`.

No creé sitios, no toqué DNS ni producción, no desplegué y no hice commits. Tampoco escribí el archivo de revisión, porque pediste solo lectura.
