Ya terminé el inventario. Te entrego el diseño de R4 abajo.

**No pude crear el archivo:** en esta sesión solo tengo herramientas de lectura (Read, Glob, Grep). El documento está completo aquí. Guárdalo como `docs/agentes/revisiones/008-f4-m3-r4-diseno.md`, o vuelve a correr la revisión con `scripts/claude-review.sh`, que guarda la salida. No ejecuté suites, no modifiqué código y no leí `postman/` ni ningún `.env`.

---

# 008 F4 M3 · R4: preparar el código para el servidor (diseño)

Autor: Claude Code, 2026-10-20. Leí los archivos citados en la solicitud (HEAD `00ec341`). Confirmo el inventario de Codex y agrego tres puntos que faltaban: el footer, el contenido del hero que viene de la API y el tiempo de vida del contexto de host.

**Fuera de R4:**
- no se agregan `@angular/ssr`, `outputMode`, `provideClientHydration`, `platform-server` ni rutas de prerender;
- ningún `main.*.ts` cambia, salvo lo que se indica abajo;
- no hay cambios de negocio, backend, RBAC, datos, Hosting ni despliegue.

## 0. Hallazgos adicionales al inventario

- **H1 · Alta. Sin contexto, el servidor renderiza el árbol equivocado.**
  - En el servidor, el host queda vacío, así que `isCustomTenantHost()` da `true`.
  - Con eso, `tenantHostMatch` hace que `''` (la portada) cargue `HomeComponent` del tenant con `tenantHostResolver` (`app.routes.marketing.ts:19`), en lugar de `FluoraHome`.
  - `App.esNavegacionMarketing` da `false`, así que se renderiza `<app-header>` diferido en vez de `<app-marketing-nav>` (`app.html:3-20`).
  - No es solo un problema de URLs: se renderiza otro árbol, y en R3 eso sería un error de hidratación seguro. **El contexto explícito es requisito de R4, no una mejora.**
- **H2 · Alta. El timer de 60 s deja la app inestable para siempre.**
  - Está en `subscription-access-notice.component.ts:56`.
  - El prerender espera a que la app esté estable, así que no terminaría nunca (o terminaría por timeout).
- **H3 · Media. Llamadas a la API que no estaban en el inventario.** Además de Nav y Home:
  - `FooterComponent` (`footer.component.ts:26-30`) pide `publicSupport()` cuando `fluoraContact()`;
  - `HomeHeroComponent` pinta los chips de verticales (`hero.component.html:8`) y el CTA (`offer.label()`, `offer.recommended()`), y los dos dependen de la API.
  - En el servidor, `environment.apiBaseUrl` es `''` porque `runtime-config.js` no se ejecuta en Node. Esas llamadas serían relativas y fallarían.
- **H4 · Baja.** `footer.currentYear` queda fijo en el build; después de hidratar, el cliente lo corrige porque es un texto, no un cambio de estructura. Queda registrado para R3; no se toca.

## 1. Contexto de render del servidor

### 1.1 Archivo nuevo: `src/app/core/platform/marketing-render-context.ts`

```ts
export interface MarketingRenderContext {
  readonly route: '/';
  readonly marketingOrigin: string; // origen https/http validado
  readonly appOrigin: string;
}
export const MARKETING_RENDER_CONTEXT =
  new InjectionToken<MarketingRenderContext | null>('Marketing render context', { providedIn: 'root', factory: () => null });

/** Valida y congela. Lanza error si algo es inválido; nunca completa ni inventa valores. */
export function parseMarketingRenderContext(raw: unknown): MarketingRenderContext;
/** Lo usa solo la configuración de servidor futura de marketing (paso prerender). */
export function provideMarketingRenderContext(raw: unknown): Provider; // useValue: parse(raw)
```

**Qué valida `parseMarketingRenderContext`:**
- `route === '/'`, exactamente;
- cada origen:
  - se puede parsear con `new URL`;
  - su protocolo es `https:` o `http:`;
  - no tiene `username` ni `password`;
  - su `pathname` es `/`, sin `search` ni `hash`;
  - `url.origin` es igual al valor después de quitar la barra final.
- Si algo falla, lanza un `Error` con un mensaje fijo, sin repetir el valor recibido.

### 1.2 `PlatformHostService` (`platform-host.service.ts`)

**Calcular el estado con una función pura exportada:**

```ts
export type HostInput =
  | { kind: 'browser'; location: Pick<Location,'origin'|'hostname'> | undefined; runtime: RuntimeWithFluoraOrigins }
  | { kind: 'server'; context: MarketingRenderContext | null };
export function resolvePlatformHostSnapshot(input: HostInput): HostSnapshot; // currentOrigin, currentHostname, marketingOrigin, appOrigin
```

**Navegador:** produce exactamente los mismos valores que las líneas 46-54 actuales: lee `globalThis.location` y `__FLUORA_*_ORIGIN__`, con el mismo fallback. Si alguien provee el token, se **ignora**.

**Servidor con contexto:**
- `currentOrigin` y `marketingOrigin` toman `context.marketingOrigin`;
- `appOrigin` toma `context.appOrigin`;
- `currentHostname` es el hostname del origen de marketing.
- **No lee `globalThis`** (ni `location` ni las variables de runtime), para no depender de estado global del proceso.
- Resultado:
  - `isMarketingHost()` da `true`;
  - `isAppHost()` da `false`, salvo que alguien configure el mismo hostname para los dos, y eso lo valida el parse;
  - `isCustomTenantHost()` da `false`.

**Servidor sin contexto: falla cerrado.** Lanza un error explícito («render de servidor sin contexto de marketing»).
- No se clasifica como tenant ni como marketing.
- Justificación: cualquier render de servidor futuro (tenant, app o un SSR accidental) que no haya declarado su identidad no puede convertirse en marketing en silencio. Si eso ocurre en el build, el build falla, que es lo correcto.

**Implementación:**
- `private readonly snapshot = resolvePlatformHostSnapshot(isPlatformBrowser(inject(PLATFORM_ID)) ? {kind:'browser', location: globalThis.location, runtime: globalThis as …} : {kind:'server', context: inject(MARKETING_RENDER_CONTEXT)})`;
- los métodos públicos leen `snapshot`;
- **la API pública no cambia.**

### 1.3 Cómo se construye «sin host» (contrato para el paso de prerender, no para R4)

- La futura `app.config.marketing.server.ts` será **la única** que llame a `provideMarketingRenderContext({route:'/', marketingOrigin, appOrigin})`.
- Los valores vienen de la configuración del build (`define` del builder, solo en la configuración server de marketing). No se escriben en el código de la app.
- Si falta el valor, `parse` lanza error y el build falla.
- `check:public-builds` tendrá que comprobar que esos orígenes son los mismos que los valores por defecto de producción de `public/runtime-config.js`. Así, los `href` del HTML prerenderizado coinciden con los del primer render del cliente.
- El prerender se limita con `routes:['/']` y sin descubrimiento de rutas, y la guarda comprueba que no se generó ningún otro HTML.
- Con esto, **otros hosts no pueden convertirse en marketing en el servidor**, por tres razones:
  1. solo esa configuración provee el token;
  2. sin token, el servidor lanza error;
  3. el contexto solo admite la ruta `/`.
- **R4 solo entrega el tipo, el validador y la lógica del host, con pruebas.** No hay ningún proveedor en `main.marketing.ts`, `main.application.ts`, `main.ts`, `app.config*.ts` ni en el CSR general. Esto se comprueba con grep en la revisión.

## 2. Cambios por archivo (todos con `isPlatformBrowser(inject(PLATFORM_ID))`; el navegador se comporta igual)

| # | Archivo | Cambio | Navegador |
|---|---|---|---|
| A1 | `core/auth/auth.service.ts` | Campo `browser` declarado **antes** de `sesion`, porque el inicializador de `sesion` llama a `loadSession()`. En el servidor: `loadSession()` devuelve `null` sin tocar el storage; `saveSession` y `limpiarSesion` no tocan el storage (`sesion.set(null)` se mantiene). `getTokenAcceso()` queda `null`. | Igual |
| A2 | `core/platform/platform-auth.service.ts` | Lo mismo: `cargar()` devuelve `null` en el servidor, sin lectura y sin el `removeItem` del catch; `guardar` y `cerrar` no tocan el storage en el servidor. | Igual |
| A3 | `core/auth/auth.interceptor.ts` | **Sin cambios.** Con A1, A2 y §3 queda a salvo en el servidor. | — |
| B1 | `core/subscription/subscription-access-notice.component.ts:56` | `timer(60000,60000)` solo en el navegador. El effect y la suscripción al router se quedan como están; en el servidor `autenticado()` es `false` y no hay `refresh`. | Igual |
| C1 | `core/marketing/marketing-tracking.service.ts` | Campo `browser` antes de `context`. En el servidor: `restore()` devuelve `{campaign:{}}` sin storage; `capture()` y `emit()` no hacen nada; `registrationUrl` usa `new URL(base)`, sin `globalThis.location`. En el servidor, `base` siempre es absoluto porque viene de `appUrl` con un contexto validado; si llegara uno relativo, el error tiene que verse, no ocultarse. | Igual |
| C2 | `marketing-tracking.service.spec.ts` | Las 4 instancias `new MarketingTrackingService()` pasan a `TestBed.inject` (o `runInInjectionContext`). Las aserciones no cambian. | — |
| D1 | `app.ts` | `inject(DOCUMENT)`; `aplicarTemaTenant` solo en el navegador (reemplaza `typeof document`), con los mismos valores fallback. `recargarNavegacion` usa `document.defaultView?.location.reload()`. | Igual |
| D2 | `core/marketing/marketing-bootstrap.ts` | Primera línea: si no es navegador, `return`. Sin preloads ni import del Home en el servidor. Meter preloads estáticos en el `<head>` prerenderizado es una decisión de rendimiento del paso de prerender o medición, no de R4. | Igual |
| D3 | `core/styles/tenant-public-styles.ts` | Si no es navegador, `provideTenantPublicStyles()` devuelve el no-op (`ready:true`, `ensure` resuelto). No crea `<link>` ni timer. Además, en `/` con contexto, `App` no llama a `ensure()` porque la navegación es de marketing. | Igual |
| E1 | `components/marketing-nav/marketing-nav.component.ts` | En el constructor, dentro de un `if (browser)`: `scrollbarGutter`, la suscripción `NavigationEnd`, `closeMenu`, los listeners de scroll, click, keydown y matchMedia, y el `onDestroy` que restaura todo eso. Fuera del guard: `offer.load()`, `navegacion()` y `MarketingAppearanceService.activate(true)` (que usa Meta y está permitido en el servidor), con su `release` en su propio `onDestroy`. **Uso un guard en el constructor y no `afterNextRender`, para no cambiar el momento en que se ejecuta en el navegador** (`scrollbarGutter` antes del primer pintado, y las pruebas de Escape y foco). | Mismo momento |
| E2 | `core/marketing/marketing-sections.directive.ts` | `ngAfterViewInit` termina enseguida si no es navegador. `ngOnDestroy` se queda igual. | Igual |
| F1 | `pages/fluora-home/fluora-home.component.ts` | Los listeners de wheel, touchstart y keydown (líneas 69-73) solo en el navegador. `ngAfterViewInit` termina enseguida si no es navegador, **antes** de `win.matchMedia` (en el servidor `defaultView` puede existir sin `matchMedia`). SEO, Meta, Title y `applyMarketing()` se ejecutan en el servidor, a propósito. `afterEveryRender` no se ejecuta en el servidor y no hace falta tocarlo. | Igual |
| F2 | `sections/plans.component.ts` | **Sin cambios.** Está dentro de `@defer`, y en el servidor `@defer` solo renderiza el placeholder, así que el componente nunca se instancia. Queda inventariado y se prueba (§4, T7). | — |
| G1 | Servicios de la API pública (ver §3) | `EMPTY` en el servidor. | Igual |

**Sin cambios, con justificación:**
- `SeoService` y `MarketingAppearanceService` (Angular `Meta`/`Title`/`DOCUMENT`, válidos en el servidor);
- `api-base-url.ts` y `environment.ts`, que se evalúan al cargar el módulo. En Node dan `''` porque no hay runtime ni `location`, y el guard de §3 hace que eso no importe. No se cambian los orígenes en el navegador;
- `TenantSiteService` (fuera de la cadena del Home);
- `UserProfileService` y `Header` (fuera de la cadena del Home; registrados como fuera de alcance).

## 3. Límite con la API: guard en R4, sin TransferState

**Decisión:** en el servidor, **ninguna petición HTTP** a la API pública. Se aplica en los servicios que usa la cadena del Home, para que cubra a todos los consumidores:
- `PublicPlansService.listar()`;
- `LandingMarketingService.navegacion()`;
- `PlatformContactService.publicSupport()`.

En el servidor devuelven `EMPTY`, con el comentario «sin datos dinámicos en el servidor; R3 decide». Los demás métodos (panel, guardado) no cambian.

**Por qué esto le corresponde a R4 y no a R3:**
1. Sin el guard, el render del servidor hace peticiones relativas (`apiBaseUrl === ''`) que fallan o dejan la app inestable. Es un problema de que el código funcione en el servidor, que es justo el objetivo de R4.
2. Cualquier respuesta quedaría horneada en el HTML (precios, soporte, verticales), y eso va contra M3 §3.2-R3.
3. **Consecuencia útil para R3:** el servidor sin datos produce el mismo HTML que el primer render del cliente, que también arranca sin datos:
   - `offer.loading()` en `true` y `label()` en «Crear mi cuenta»;
   - chips vacíos, `support` en `null` y `dataReady()` en `false`;
   - `@defer` en placeholder.

   No hay ramas distintas entre los dos.

**Para R3 (no se hace ahora):**
- usar `provideClientHydration(withEventReplay(), withNoHttpTransferCache())` para dejar explícito que no hay caché;
- el hero (chips con `min-height:44px` y el texto del CTA) y la reaseguranza cambian **después** de hidratar cuando llega la API. Eso no es un error de hidratación, pero sí puede causar CLS o un cambio de LCP. R3 lo tiene que medir y reservar con medidas reales, no con una altura inventada.

## 4. Pruebas de R4 (en jsdom, simulando el servidor; no es SSR real)

**Herramientas:**
- `{provide: PLATFORM_ID, useValue: 'server'}` en TestBed;
- **globals tóxicos:**
  - `vi.spyOn(Storage.prototype, 'getItem' | 'setItem' | 'removeItem')` que lanzan error;
  - `matchMedia`, `IntersectionObserver`, `ResizeObserver` y `requestAnimationFrame` con stub que lanza error;
  - espías en `window.addEventListener` y `document.addEventListener`, filtrando por los tipos de evento (scroll, wheel, touchstart, keydown, click, resize) que la app no debe registrar;
  - `location` no se puede sustituir en jsdom (es *unforgeable*), así que se prueba a través de `resolvePlatformHostSnapshot` con un objeto `location` cuyos getters lanzan error.
- Los nombres de los `describe` dicen «contrato de servidor simulado», nunca «SSR».

**Casos:**

| ID | Caso |
|---|---|
| T1 | Pruebas de caracterización **primero, antes del refactor**: el `snapshot` del navegador para marketing, `www`, app, local y un dominio propio, con los mismos resultados que hoy (los specs actuales de `isMarketingHostname` se quedan como están). |
| T2 | `parseMarketingRenderContext`: acepta https y http válidos; rechaza credenciales, path, query, hash, `ftp:` o `javascript:`, una ruta distinta de `/` y valores que no son string. El mensaje de error no incluye el valor recibido. Los orígenes de prueba usan el TLD reservado `.test` (`https://marketing.test`, `https://app.test`), sin dominios reales. |
| T3 | Servidor con contexto: marketing `true`, app y tenant `false`; `appUrl('/registro')` es absoluta; no se lee `location` tóxico ni las variables de runtime. Servidor sin contexto: lanza error. Navegador con el token provisto: lo ignora. |
| T4 | `tenantHostMatch` en el servidor con contexto da `false`, y `tenantHostResolver` no se invoca. |
| T5 | Auth y PlatformAuth en el servidor: `sesionActual()` y `token()` en `null`; `getTokenAcceso()` en `null`; ninguna llamada al storage. Navegador: las pruebas actuales siguen en verde. |
| T6 | `SubscriptionAccessNotice` en el servidor con `vi.useFakeTimers()`: al crearse no queda ningún timer del componente; avanzar 120 s no llama a `refresh`. Navegador: el timer existe. |
| T7 | `App` en `/` con contexto y PLATFORM_ID servidor, más `HttpTestingController`: renderiza `app-marketing-nav` y `app-fluora-home`, no `app-header`; los 4 `@defer` en placeholder (`HomePlans` no se instancia); `httpMock.verify()` con **0 peticiones**; sin storage, listeners, observers, `matchMedia` ni RAF tóxicos; GSAP no se importa (espía en `loadAnimation`). |
| T8 | `MarketingTracking` en el servidor: `restore`, `capture` y `emit` sin storage ni `dataLayer`; `registrationUrl` absoluta con UTMs vacíos. |
| T9 | `prepareMarketingHome` en el servidor: ni `load` ni `<link>`. `provideTenantPublicStyles` en el servidor: `ready()` en `true`, sin `<link>` y sin timer. |
| T10 | Regresiones del navegador (los specs existentes, sin cambiar sus aserciones): menú con Escape y foco, details de negocios, scrollspy y resize/RAF, condición de GSAP, fuentes y precarga, CTA y UTM. |

**Prueba en Node puro:** **no la recomiendo en R4.**
- El builder `@angular/build:unit-test` inicializa `BrowserTestingModule` con DOM para todo el run. Un archivo con `@vitest-environment node` pondría en riesgo la suite completa (y el T1 que costó estabilizar), sin demostrar nada que T3 y T7 no demuestren.
- **La prueba real en Node es el build de prerender**, que ejecuta `platform-server`. R4 declara este límite: no garantiza ausencia de errores de hidratación ni que domino funcione.

## 5. Criterios de cierre de R4

1. Los cambios de §2 y §3 están aplicados y nada más. `git diff --stat` solo muestra los archivos de la tabla, sus specs y `marketing-render-context.ts`.
2. Los grep confirman que:
   - `MARKETING_RENDER_CONTEXT` y `provideMarketingRenderContext` no aparecen en `src/main*.ts` ni en `app.config*.ts`;
   - `package.json` no tiene `@angular/ssr` ni `@angular/platform-server`;
   - `angular.json` no tiene `outputMode`, `server` ni `prerender`.
3. T1–T10 en verde. `npm test` con la configuración normal (Node 20.19, `TZ=America/Mexico_City`, workers predeterminados) en verde **2 veces seguidas**, con el conteo registrado.
4. Las 37 pruebas Node 22 (functions y preview/csr), `npm run build` (marketing y application) y `check-public-builds` en verde. `index.csr.html` sigue siendo idéntico byte a byte a `index.html`.
5. **El bundle del navegador no cambia de comportamiento:** el tamaño inicial de marketing queda dentro de ±1 kB de 476.17 kB, y se reporta la diferencia.
6. Revisión de la interfaz en los servicios habituales del usuario (4200/8080), sin crear datos y sin usar 4300:
   - Home en 375, 820 y 1440: menú, Escape, foco, scrollspy, GSAP en ≥1200, CTA con UTM;
   - un tenant `/e/...` y `/acceso`;
   - 0 errores en consola.
7. La respuesta `respuestas/008-f4-m3-r4.md` registra los límites: no es SSR real, la hidratación queda para R3 y el contexto queda sin proveedor hasta el paso de prerender.
8. **Detenerse para la revisión del usuario antes de R3.** Sin commit (salvo que el usuario lo autorice), push ni despliegue.

## 6. Prioridades para Codex

**Implementar primero, en este orden:**
1. **T1** (caracterización, antes del refactor);
2. **§1** (contexto y host);
3. **B1** (timer);
4. **A1/A2** (Auth);
5. **§3** (API);
6. **E1, F1, E2, D1–D3, C1/C2**;
7. **T7** como prueba integradora final.

**Riesgo principal:** el orden de los campos en A1 y C1. Si el campo `browser` se declara después del campo que llama a `loadSession()` o `restore()`, en el servidor queda `undefined` y el código lee el storage igual. T5 y T8 detectan ese error.
