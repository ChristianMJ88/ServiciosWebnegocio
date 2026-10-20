# Revisión 008 F4 M1: código antes de la medición

Solo leí archivos, sin modificar nada y sin ejecutar builds ni suites. No pude guardar esta revisión: la sesión no tiene herramienta de escritura. Se puede guardar como `docs/agentes/revisiones/008-f4-m1-codigo.md`, con autoría de Claude.

**Veredicto:** el diseño se implementó como se acordó y el aislamiento de application está bien. **Hay un defecto visual seguro en marketing (Alta) que hay que corregir antes de las capturas y de la medición.** Acepto el desvío del `@if ready` dentro del `@defer`. No me pronuncio sobre equivalencia visual ni sobre llegar a 85.

**Lo que revisé:**
- `tenant-public-styles.ts` y su spec
- `app.ts`, `app.html` y `app.spec.ts`
- `app.routes.marketing.ts` y `main.marketing.ts`
- `angular.json` y `package.json`
- las hojas nuevas y las generadas
- `scripts/css/marketing-subset.mjs` y `scripts/lighthouse-home.mjs`
- el `index.html` y los archivos de `dist/marketing/browser`

## Hallazgos

| Pri. | Ubicación | Hallazgo y riesgo | Corrección y prueba |
|---|---|---|---|
| **Alta** | `scripts/css/marketing-subset.mjs:54-56` y `src/styles/generated/marketing-bootstrap.css:8` | **Falta `@keyframes spinner-border`.** En Bootstrap 5.3 la animación usa una variable: `animation: … var(--bs-spinner-animation-name)` y aparte `--bs-spinner-animation-name: spinner-border`. El script solo lee las declaraciones `animation*`, así que no ve ese nombre. En la salida generada solo aparece `@keyframes placeholder-glow`. `fluora-vertical.component.html` usa `spinner-border`, por lo que el spinner de las verticales quedaría quieto. | Recoger los nombres también de las declaraciones `--bs-*-animation-name` (o de cualquier `--*animation*`). Mejor aún: conservar todo `@keyframes` cuyo nombre aparezca en cualquier valor de una regla que se conserva. Además, que el script falle si una regla conservada nombra una animación cuyo keyframe no salió. **Prueba:** ejecutar `check:marketing-css` y comprobar que el generado contiene `@keyframes spinner-border`. En el diff de estilos calculados de una vertical en carga, `animation-name` debe ser `spinner-border` y la regla debe existir en `document.styleSheets`. |
| **Media** | `tenant-public-styles.ts:30-40` | **Sin límite de tiempo.** Si la petición del CSS se queda colgada, sin `load` ni `error`, el resolver `estilos` deja la navegación al tenant bloqueada para siempre. Antes no bloqueaba: el CSS global llegaba con `media=print` y no detenía nada. | Poner un límite de 8 a 10 s que abra paso (`ready=true` y `resolve`) **sin quitar el link**, para que se aplique si llega tarde, y que no duplique la petición en el siguiente `ensure()`. **Prueba en Vitest con fake timers:** no hay `load` → el resolver se resuelve al cumplirse el límite → sigue habiendo un solo link → un `load` posterior no rompe nada. |
| Baja | `tenant-public-styles.ts:25` | **Reintento después de un error.** `ready.set(false)` desmonta el Header que ya estaba montado y lo vuelve a montar al cargar. Se pierde su estado y se repiten las peticiones de su `init`. | Bajar `ready` solo si nunca se mostró, o mantener `true` durante el reintento. **Prueba:** error → Header montado → reintento → el mismo nodo `app-header` sigue en el DOM. |
| Baja | `tenant-public-styles.ts:41-42` | **El respaldo de inserción queda mal ordenado.** En el `index.html` real, el `<style>` del CSS crítico va **antes** del link global (`dist/marketing/browser/index.html:25`). Si el selector no encuentra el link global, el tenant quedaría antes del CSS global y del crítico, y la precedencia se invierte sin avisar. Hoy el selector sí lo encuentra: webpack genera `styles.<hash>.css` y esbuild `styles-<hash>.css`. | Si el link global no aparece: registrar un error e insertar justo antes del primer `<style>` de componente, no del crítico. También vale añadir una comprobación al build que verifique que el `index.html` emitido tiene exactamente un `link[rel=stylesheet][href^=styles]` fuera de `<noscript>`. |
| Baja | `app.spec.ts:35` | La prueba con `ready=false` comprueba que no hay `app-footer`, pero el tenant no está activo, así que el footer tampoco saldría aunque estuviera listo. No cubre el footer, ni WhatsApp, ni el chat. | Activar el tenant con `setTenant` y `whatsapp` y `chat` habilitados. Con `ready=false` no deben existir footer, `.whatsapp-float` ni `.chatbot-wrapper`; con `ready=true` sí. |
| Baja | `app.ts:33` y `app.html:3-14` | **Transición SPA de marketing al tenant.** `tenantResolver` y `estilos` se resuelven en paralelo. `activo()` pasa a `true` antes del `NavigationEnd`, así que desaparecen la navegación y el footer de marketing mientras la página anterior sigue visible y el CSS aún carga. Es un cambio menor y no rompe nada. | Declararlo y capturarlo en el escenario «SPA `/negocios`→tenant» del diff. No hace falta código si las capturas no muestran saltos visibles. |
| Baja | `lighthouse-home.mjs:76` | **El orden de las corridas es fijo**: en cada iteración se mide primero la ruta principal y luego la de comparación. Eso introduce un sesgo de orden (backend y caché del sistema). | Invertir el orden en las iteraciones pares, con el patrón ABBA. Registrar el orden de cada corrida. |
| Baja | `lighthouse-home.mjs:127-128` | `commit` y `sourceDirty` describen el árbol actual, **no** la ruta de comparación (`tmp/builds/m0`). El resumen de M0 quedaría con la procedencia de M1. | En la comparación, guardar `provenance: "fingerprint-only"` (o un `meta.json` que se copie junto al build guardado) y no el commit actual. |
| Info | `marketing-subset.mjs:9-14` | Las fuentes consumidoras quedaron completas para lo que revisé. Las secciones de fluora-home, public-discovery y content entran por directorio. `core/marketing` no agrega clases. No hay nombres de clase armados por concatenación ni con `${}` en esos consumidores. `prebuild:marketing` ejecuta el check. | Mantener el recorrido (c) en Chrome como cierre: debe fallar si aparece una clase de Bootstrap o Icons fuera del conjunto. |

**Comprobado sin hallazgos:**
- **Aislamiento:** `provideTenantPublicStyles` solo se registra en `main.marketing.ts:16`. `app.routes.ts` reutiliza `publicContentRoutes`, y ahí el resolver usa el token que no hace nada, así que application no cambia.
- **Hojas y cascada:**
  - `angular.json` solo modifica la configuración `marketing`.
  - Hay un único woff2 de Icons en `dist`.
  - El link se inserta antes de `<noscript>` y de los `<style>` de componentes.
  - El `media=print` del link global no altera la cascada.
- **Material:**
  - El único `--mat-sys-*` público es `booking-experience.css:106,109`.
  - Las páginas públicas solo usan CDK (`a11y` y `layout`), sin componentes Material ni sus tokens.
- **Desvío del `@defer`:** es correcto. El chunk se descarga en cuanto se muestra el placeholder, y el placeholder interno es el mismo `.public-header-slot`, así que no debería causar CLS.
- **Host propio:** los legales no tienen resolver, así que su contenido se pinta con el subconjunto de marketing y después llega la hoja del tenant. Puede haber un salto de layout ahí. Hay que declararlo como límite, igual que el resto del host propio.

## Antes de medir M1

1. Corregir el hallazgo Alta y el Media. Después: `check:marketing-css`, los dos builds, la suite y `check:public-assets`.
2. Volver a comprobar que el CSS de application es idéntico byte a byte y que `git diff --exit-code src/styles.css` pasa.
3. Estabilizar las capturas antes del diff:
   - activar `prefers-reduced-motion` y `animation-play-state: paused` solo en el harness;
   - esperar a `document.fonts.ready`;
   - en el tenant, esperar a que el link de `tenant-public.css` esté cargado.

   Los rects distintos por 32 px hay que **explicarlos** antes de aceptarlos. No hay que añadirlos a una allowlist sin causa.
4. Al diff de estilos calculados hay que sumarle tres comprobaciones:
   - `animation-name` del spinner en una vertical;
   - el borde de `.booking-professionals` en `rgb(196,198,208)`;
   - el escenario SPA tenant→`/privacidad`→tenant.

## Incompatibilidades que veo para M2 (`@angular/build:application`, sin implementar)

1. **`outputPath`:** el builder application agrega `/browser` por su cuenta. Con la configuración actual, la salida quedaría en `dist/marketing/browser/browser` y en `dist/application/browser/browser`. Se puede usar `{"base":"dist/marketing","browser":"browser"}` o ajustar las rutas. Eso afecta a `firebase.json`, a `check-public-assets`, a la ruta por defecto de `lighthouse-home.mjs:10` y a la copia en `tmp/builds/m0`.
2. **`main`:** en las dos configuraciones pasa a llamarse `browser`. El `dev-server` también debe cambiar a `@angular/build:dev-server`.
3. **Media en otra carpeta:** las fuentes salen en `media/` con nombres del tipo `bootstrap-icons-<hash>.woff2`. Hay que revisar las globs de headers en `firebase.json` y el `mime` de `public-preview.mjs`, que no tiene `.woff` (solo importa si se pide).
4. **Nombres de archivo:** `tenant-public.css` sigue saliendo sin hash con `inject:false` (`bundles:'[name]'`). El global pasa a `styles-<hash>.css`, y el selector `href^="styles"` sigue sirviendo. Conviene la comprobación del build que propongo arriba, para que un cambio de nombre no pase sin avisar.
5. **Chunk del Header:** esbuild divide los chunks de otra forma, y la regla de «exactamente 1 chunk» con `site-header--tenant` puede fallar con razón. También puede aparecer un `modulepreload` de un chunk compartido con el Header que cambie lo que se pide en Home. Hay que volver a validar `headerRequested=false` y `tenantStylesRequested=false` en Home.
6. **D1 sigue pendiente:** `Cache-Control: no-cache` para `/tenant-public.css` es un requisito antes de cualquier despliegue, con M1 o con M2, y necesita tu aprobación.

F4 sigue abierta. M1 no está medido y no hay que declarar resultado antes de corregir el hallazgo Alta y repetir las capturas.
