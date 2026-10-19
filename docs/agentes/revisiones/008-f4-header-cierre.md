# Revisión 008 F4: Header diferido

Solo leí archivos. No cambié nada. Revisé `app.html`, `app.ts`, `app.spec.ts`, `header.component.ts`, `angular.json` y los artefactos `dist/marketing` y `dist/application`. No tengo herramienta de escritura, así que la revisión va aquí; Codex o tú pueden guardarla como `revisiones/008-f4-header-cierre.md`.

## Lo que confirmo

**El chunk está realmente separado.**
- `HeaderComponent` solo se menciona en `app.ts:9` (import) y `app.ts:19` (`imports`). Fuera de `app.html:6-10` no hay ninguna otra referencia en el template ni en el TS. Por eso el compilador puede cambiar el import estático por uno dinámico.
- **Build marketing:** el hash `876238215e39f020` de `stats.json` coincide con el que reportaste.
  - `main.c303409e8fd8ee17.js` solo contiene `_.e(385).then(...).then(P=>P.HeaderComponent)` y el nombre de etiqueta `app-header`. Ni la clase ni su template están en el inicial.
  - `site-header--tenant` aparece únicamente en `385.cbcf017d73513c88.js` y en `styles.*.css`. En el CSS viene de la regla global `html[data-tenant-theme] .site-header--tenant .header-shell` (`styles.css`), que ya estaba prevista y no cambia.
  - `index.html` solo carga `runtime-config`, `styles`, `runtime` y `main`. No hay preload de 385, así que Home no lo pide.
- **Build application:** existe `dist/application/browser/main.ba5eb915ad6fc723.js` con el mismo `import()` diferido, y `site-header--tenant` solo en `385.a47c5cd76727a747.js`. Pero como dices que el build seguía en curso, falta registrar su exit code, su hash y el `Initial total`. **Ojo:** `dist/ServiciosWebnegocio/browser` es una salida antigua sin hashes y no sirve como evidencia.

**Ciclo de vida y compuertas.**
- La rama `@defer` está dentro de `mostrarChromePublico() && !esNavegacionMarketing()`. En Home nunca se renderiza.
- En `/e/…` y en host propio la condición es estable entre rutas internas del tenant, así que Header no se desmonta al navegar dentro del tenant.
- El constructor de Header lee `router.url` y `scrollY` cuando se monta y se suscribe a `NavigationEnd`. Funciona igual si se monta antes o después de la navegación inicial.
- El placeholder no tiene altura, ni `@loading`, ni `minimum`, ni `prefetch`, que es lo que se pidió.

**Pruebas (`app.spec.ts`).** Cubren lo pedido:
- Home: ningún bloque diferido y ningún Header (`:35`).
- Slug y host propio: el placeholder sin Header, luego `render(Complete)` monta Header y no aparecen MarketingNav ni Footer ajeno (`:37-52`).
- Auth y panel: ninguna barra ni footer públicos (`:53-58`).
- Footer con contexto se mantiene (`:59-71`).

## Riesgos antes de cerrar F4

| Prioridad | Riesgo | Evidencia | Validación propuesta |
|---|---|---|---|
| **P2 (pendiente, no defecto)** | Que Header se monte después del contenido. `on immediate` empieza la descarga después del bootstrap y del primer ciclo de detección de cambios de App, no con el HTML. Si el chunk de la página ya está en caché y el resolver es rápido, el `router-outlet` puede pintarse antes y empujarse 72–78px en páginas `sticky` (servicios, productos, checkout). En la home del tenant no pasa, porque ahí Header va `fixed`. | `header.component.css:549-555, 739-749` | Performance panel con Slow 4G, caché vacía y caché caliente en `/e/{slug}/servicios` y en host propio `/servicios`. CLS en Lighthouse ≤ al valor anterior. **Sin esto no se cierra F4.** |
| **P3** | No hay `@error`. Si el chunk falla (por ejemplo, una pestaña abierta durante un deploy que cambia hashes), el tenant se queda sin navegación, login ni CTA. Antes Header iba en `main`, así que este modo de fallo es nuevo para el chrome. Lo atenúa que los chunks de ruta ya tienen el mismo riesgo. | `app.html:6-10` | Por ahora solo documentarlo. Si se trata, que sea en otro paso: un `@error` mínimo con enlace a `homeLink`, sin duplicar lógica de Header. |
| **P3** | La primera navegación SPA de auth/panel hacia una página pública del tenant (en la build application) muestra un frame sin Header mientras baja el chunk. Lighthouse no lo mide, pero el usuario sí lo ve. | `app.ts:58` cambia la rama tras `NavigationEnd` | Prueba manual: `/acceso` → enlace al tenant en 390px. |
| **P3** | Las pruebas no cubren que Header siga montado después de navegar dentro del tenant. | `app.spec.ts:37-52` | Opcional: navegar `/e/x` → `/e/x/servicios` con un router con rutas reales y comprobar que la misma instancia de `app-header` sigue ahí. |
| Info | La suite necesitó una configuración temporal de un solo worker. Confirmé que no quedó en el repo (sin `maxWorkers`). Pero es la segunda vez que pasa (ver respuesta 005), lo que apunta a timeouts que fallan intermitentemente con carga en paralelo. | — | Registrar en ESTADO como deuda de pruebas, sin ampliar timeouts. |

## Dictamen

- **El cambio de código queda aprobado:** está bien aislado, sin bloqueantes y dentro del alcance.
- **F4 todavía no se cierra.** Faltan:
  1. Exit code, hash y métricas del build application.
  2. Las cinco corridas de Lighthouse en Home: mediana ≥85 como meta del usuario (la revisión anterior pedía ≥81 como mínimo), A/BP/SEO 100, CLS ≤0.001, y sin petición del chunk de Header en la red.
  3. El orden de montaje y el CLS en el tenant (fila P2) a 390, 820 y 1440.
  4. Navegación tenant ↔ marketing, menú móvil, Escape y `resize`.
- **No apruebo la meta de Performance 85 por adelantado.** El ahorro fue de 36.8kB en el inicial y 4.84kB en el estimado. Es real pero modesto, y no tiene por qué bastar para Home, porque Home nunca cargaba Header en su ruta crítica de render: solo se descargaba y parseaba dentro de `main`.
- Lo de Playfair y Booking queda como evidencia declarada. No forma parte de este diff y no lo revisé.
