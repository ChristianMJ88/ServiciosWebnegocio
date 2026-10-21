# 008 F4 M3 · R3: diseño de hidratación y placeholders

Autor: Claude Code, 2026-10-20. Solo hice lectura. No ejecuté suites, builds ni navegador, no escribí código y no leí `.env`, `postman/` ni credenciales.

**Qué revisé:**
- PROTOCOLO, ESTADO, `008-f4-m3-diseno` §3.2, `r1-diseno`, `r4-diseno`, `r4-cierre` y `respuestas/008-f4-m3-r4` y `008-f4`.
- El código en HEAD `e53cc51`: `main.marketing.ts`, `app.config.base.ts`, rutas de marketing, `App`, MarketingNav, Footer, FluoraHome con sus secciones, MarketingOffer, Appearance, Sections, los servicios públicos, PlatformHost, el contexto de render, `runtime-config.js`, `angular.json`, `package.json`, tsconfig y el preview.

**Verificado en `node_modules` (versión 21.0.6, no en la web de la v22):**
- `@angular/platform-browser`: `provideClientHydration`, `withEventReplay`, `withNoHttpTransferCache` y `BootstrapContext`.
- `@angular/core`: `withDomHydration` (NG0505 y `ngcm`) y `withEventReplay`.
- `@angular/build`: `ngcm-attribute`, `index-html-generator` y el schema (`server`, `define`, `outputMode`, `prerender`).
- **`@angular/ssr` y `@angular/platform-server` no están instalados.** Sus APIs (`provideServerRendering`, `withRoutes`, `RenderMode`, la relación entre `prerender` y `outputMode`) hay que comprobarlas en sus `.d.ts` 21.0.6 después de instalarlos, no copiarlas de la documentación actual.

## 0. Hallazgos que condicionan el diseño

| ID | Prioridad | Evidencia | Consecuencia |
|---|---|---|---|
| H1 | Media | `MarketingNav` llama a `offer.load()` y `navegacion()` al arrancar (`marketing-nav.component.ts:36-37`). `MarketingOfferService` es singleton (`marketing-offer.service.ts:12-19`). `navegacion()` y `publicSupport()` usan `shareReplay` con caché de 60 s. `FluoraHome` es una ruta *lazy* que se crea después. | Si la API responde antes de que cargue el chunk del Home, la **primera** vista hidratada del hero ya tiene chips, etiqueta de CTA y «Prueba sin tarjeta». Angular tolera la diferencia en `@for`/`@if` (crea vistas nuevas) y en el texto, pero entonces la primera estructura del cliente no coincide con la del servidor. Se corrige con una compuerta (§1.3). |
| H2 | Media | `core.mjs:1818`: el replay de eventos ocurre en `appRef.whenStable()`, y la estabilidad espera las peticiones HTTP pendientes. | Un clic antes de hidratar se reproduce cuando terminan las GET de la API. Con 4G lenta eso puede tardar segundos. Hay que medirlo, no ocultarlo. |
| H3 | Baja | `MarketingAppearanceService.activate` (`:13,17`) guarda como «original» el `theme-color` y el `viewport` que hay en el `<head>`. Con prerender, esos tags ya vienen del servidor con `#0A0F1A` y `viewport-fit=cover`. | Al liberar, se «restaura» el valor de marketing en lugar de quitar el tag o volver al viewport original. Impacto práctico bajo: los enlaces a tenant recargan la página completa. Se corrige con un atributo de lease (§1.6). |
| H4 | Baja | Footer: `url` empieza en `null` antes de `NavigationEnd` (`footer.component.ts:21-22`). El servidor termina con la rama `@else if(fluoraContact())` creada; el primer CD del cliente no la tiene. | Es un reclamo diferido de una vista deshidratada; no debería dar NG05xx. La prueba real tiene que demostrarlo, sin cambiar código antes. |
| H5 | Info | `withDomHydration` (`core.mjs:2364-2396`): sin `__nghData__` en el TransferState, la hidratación se desactiva sola. NG0505 solo se muestra con `ngDevMode` y se suprime si `<body ngcm>`. El builder añade `ngcm` al `index.csr.html` cuando genera contenido SSR dedicado (`index-html-generator.js:39-41`). `enabledBlocking` combinado con hidratación genera un aviso de configuración (`platform-browser.mjs:661-675`). | Se mantiene la navegación inicial *non-blocking* actual. Detalles en el punto 4. |
| H6 | Info | `respuestas/008-f4.md:63-76` tiene la matriz de 16 tamaños (overflow y fuentes), pero **ninguna altura de sección**. | Los valores 650/700/720/800 de `fluora-home.component.css:1-2` no se han medido. |

**Coincidencias servidor/cliente comprobadas leyendo el código:**
- `mobileTabs` da `true` en `/` con cualquier snapshot (`marketing-route.ts:7`).
- `isHome`, `esNavegacionMarketing`, `mostrarChromePublico` y `contenidoPendiente` coinciden en los dos lados.
- Los `href`, que difieren entre local y producción, son atributos: el primer CD los corrige y no son errores de estructura.
- `currentYear` es texto (R4-H4).

## 1. Decisiones por punto

### 1.1 Proveedor y configuración compartida (punto 1)

Archivo nuevo: `src/app/app.config.marketing.ts`. Es la única definición, y la importan el cliente y el servidor.

```ts
export function provideMarketingHydration(): EnvironmentProviders {
  return provideClientHydration(withEventReplay(), withNoHttpTransferCache());
}
export function createMarketingAppConfig(hydrate: boolean): ApplicationConfig {
  return mergeApplicationConfig(createAppConfig(marketingRoutes), {providers: [
    provideTenantPublicStyles(), provideEnvironmentInitializer(prepareMarketingHome),
    ...(hydrate ? [provideMarketingHydration()] : [])]});
}
```

- `registerLocaleData(es, es-MX)` pasa a `src/app/marketing-locale.ts` y lo importan los dos `main`.
- `app.config.base.ts`, `main.ts`, `main.application.ts` y `app.config.ts` no cambian.
- Sin `withIncrementalHydration` ni `withHttpTransferCacheOptions`.

### 1.2 Límite entre R3 y prerender (punto 2)

**Recomendación:** una configuración local **no predeterminada**, `marketing-hydration-probe`, que usa el builder real (`outputMode:'static'`, entrada `server` y solo `/`). Su salida va a `dist/marketing-hydration-probe/`.

**Descarto `renderApplication` en un Node empaquetado a mano:** sería una segunda cadena de build distinta de la que llevará el prerender, y no produciría el mismo `index.csr.html`/`ngcm`, la misma CSS crítica ni el mismo contrato de event replay.

**Lo mínimo del prerender que entra en R3 (justificado):**
- Los paquetes `@angular/platform-server` y `@angular/ssr` en `21.0.6` exactos (`--save-dev --save-exact`, sin `ng add`).
- `main.server.ts`, la configuración de servidor, las rutas de servidor y un tsconfig propio.
- La configuración del probe.

Sin eso no se puede probar el DOM reutilizado en un navegador.

**Fuera de R3:**
- `build:marketing`, `firebase.json`, `predeploy`, Functions, `prepare-csr-shell` y `check-public-builds` en producción siguen exactamente igual.
- El `index.html` de producción sigue siendo CSR, y el `index.csr.html` sigue idéntico byte a byte.

### 1.3 Datos de la API y compuerta posterior a la hidratación (punto 3)

- El servidor ya devuelve `EMPTY` en las tres GET (R4). `withNoHttpTransferCache` deja explícito que no hay caché.
- Las respuestas HTTP siempre llegan en una macrotarea, así que no se pueden intercalar dentro de la creación de una vista. Hidratar una vista es síncrono. El único hueco real es el de H1.

**Compuerta en `FluoraHome`, que solo afecta a la presentación:**

```ts
private readonly hydrated = signal(false);
constructor(){ …; afterNextRender(() => this.hydrated.set(true)); }   // en el servidor no se ejecuta
readonly heroVerticals = computed(() => this.hydrated() ? this.verticals() : []);
readonly dataReady = computed(() => this.hydrated() && this.contactsReady() && this.verticalsReady() && !this.offer.loading());
```

- El hero recibe `[verticals]="heroVerticals()"` y un input nuevo, `offerReady = input(false)`, enlazado a `hydrated()`.
- En la plantilla del hero:
  - `{{offerReady() ? offer.label() : offer.fallbackLabel}}`;
  - `@if(offerReady() && offer.recommended(); as plan)`.
- `MarketingOfferService` expone `readonly fallbackLabel = 'Crear mi cuenta'` y lo usa también dentro de `label`. Así queda una sola fuente, sin duplicar el texto.
- **Sin datos estáticos, sin timers y sin `setTimeout`.** Las anclas siguen esperando `dataReady()`.
- En CSR solo se añade un render después del primero.

### 1.4 CSR y Functions (punto 4)

**Recomendación:** el cliente solo provee la hidratación en la portada de marketing. Para decidirlo **reutiliza las funciones puras de R4**; no clasifica de nuevo.

```ts
// platform-host.service.ts: exportar getHostnameFromOrigin y
export function isMarketingSnapshot(s: HostSnapshot){ return isMarketingHostname(s.currentHostname, getHostnameFromOrigin(s.marketingOrigin)); }
// PlatformHostService.isMarketingHost() pasa a llamar a isMarketingSnapshot(this.snapshot)
export function isHydratableMarketingHome(loc, runtime){ return !!loc && loc.pathname === '/' && isMarketingSnapshot(resolvePlatformHostSnapshot({kind:'browser', location: loc, runtime})); }
// main.marketing.ts
bootstrapApplication(App, createMarketingAppConfig(isHydratableMarketingHome(globalThis.location, globalThis as …)))
```

**Por qué así:**
- `/e/**`, las verticales, `/negocios`, `/acceso` y las páginas legales cargan **sin el proveedor**. No hay NG0505 posible, y el CSR borra el `<app-root>` igual que hoy (el SEO de las Functions incluido).
- Si un dominio propio recibe por error el home prerenderizado, el cliente hace CSR limpio en lugar de hidratar el árbol del tenant sobre el de marketing. Es defensa en profundidad además de R2.

**Por qué no un proveedor incondicional:** en un host mal configurado provocaría un desajuste de hidratación en lugar de un CSR limpio.

**Sin `ngSkipHydration`, sin suprimir la consola y sin lógica de tenant ni de backend.**

**Endurecimiento de Functions:** prueba de que `renderTenantHtml`, `renderLandingHtml` y `renderDirectoryHtml` escapan los datos, de forma que no se puedan inyectar `<script id="ng-state">`, `ngh` ni `nghm`. Se usa un fixture con una cadena maliciosa.

### 1.5 Host del servidor (punto 5)

`src/app/app.config.marketing.server.ts`:

```ts
declare const FLUORA_RENDER_MARKETING_ORIGIN: string|undefined;
declare const FLUORA_RENDER_APP_ORIGIN: string|undefined;
export function createMarketingServerConfig(raw: unknown): ApplicationConfig {
  const ctx = parseMarketingRenderContext(raw);
  if (![ctx.marketingOrigin, ctx.appOrigin].every(o => o.startsWith('https://')) || ctx.marketingOrigin === ctx.appOrigin)
    throw new Error('Invalid production marketing render origins');
  return mergeApplicationConfig(createMarketingAppConfig(true), {providers: [
    provideServerRendering(withRoutes(marketingServerRoutes)),   // verificar API 21.0.6
    {provide: MARKETING_RENDER_CONTEXT, useValue: ctx}]});
}
export const marketingServerConfig = () => createMarketingServerConfig({route: '/',
  marketingOrigin: typeof FLUORA_RENDER_MARKETING_ORIGIN === 'string' ? FLUORA_RENDER_MARKETING_ORIGIN : undefined,
  appOrigin: typeof FLUORA_RENDER_APP_ORIGIN === 'string' ? FLUORA_RENDER_APP_ORIGIN : undefined});
```

- **`main.server.ts`:** `export default (c: BootstrapContext) => bootstrapApplication(App, marketingServerConfig(), c);`
- **`app.routes.marketing.server.ts`:** `[{path:'', renderMode: Prerender}, {path:'**', renderMode: Client}]`. Sin descubrimiento de rutas.
- **Origen de los valores:** el `define` de la configuración del probe en `angular.json`. Es configuración, no código, y el dominio ya aparece en `index.html` y en `runtime-config.js`.
- **Sin `define`, el render lanza error y el build falla.**
- **Guarda:** evalúa `public/runtime-config.js` en `vm` con un `window` de hostname `build.invalid` y exige igualdad exacta con el `define`, protocolo HTTPS y orígenes distintos.
- No hay proveedor en CSR ni en application, y se comprueba con grep (§3).
- **Nota:** la extracción de rutas de `@angular/ssr` arranca la app en el servidor. Con el contexto siempre provisto eso es correcto; si fallara, debe fallar a la vista.

### 1.6 B3: primer pintado y tema (punto 6)

Valores por defecto de `:root` (`public-base.css:15-36`) frente a lo que aplica `App.aplicarTemaTenant(null)` (`app.ts:153-167`):

| Variable | Valor por defecto | Valor aplicado por App |
|---|---|---|
| `--tenant-primary`, `--tenant-secondary`, `--primary-*` | iguales | iguales |
| `--primary-light` | alfa .12 | `#…1F` (alfa .122) |
| `--primary-border` | alfa .34 | `#…55` (alfa .333) |
| `--primary-shadow` | alfa .14 | `#…26` (alfa .149) |
| `--tenant-heading-font` | `Inter, "SF Pro Display"…` | `"Plus Jakarta Sans", Inter…` |
| `--tenant-body-font` | `Inter, "SF Pro Text", …` | `Inter, "Segoe UI", …` |

- `data-tenant-theme` pasa de ausente a `BASE` (sin reglas propias para marketing).
- Dentro de `.fluora-marketing` mandan `--fl-font-*` (`marketing-tokens.css:10-14`). El footer de marketing fija Manrope (`footer.component.css:317`), pero su `h3.footer-brand-title` sigue la regla `h1-h3` de `public-base.css:184`.
- `MarketingNav` aplica `scrollbar-gutter: stable` al arrancar.

**Medición en el probe, sin cambiar código:**
- Con `main-*.js` retenido (layout puro del servidor) frente a después de estable, en 375, 820 y 1440:
  - el `getComputedStyle(html)` de las variables de arriba;
  - la `font-family` resuelta y el rect de `h1`, de la barra, de `h3`/`h4` del footer y del `body`;
  - las entradas `layout-shift` con sus `sources`.
- **Criterio:** se corrige solo si hay un desplazamiento > 0 o un cambio visible por encima del pliegue. Esa corrección sería, previa revisión, alinear los valores por defecto de `:root` con el fallback de App; no se toca el catálogo de temas.

**H3:** `MarketingAppearanceService` marca los tags que crea con un atributo, `data-fl-lease="created"` o `data-fl-original="<contenido>"`. Así, el cliente toma el original desde el HTML del servidor en lugar de inferirlo. Prueba unitaria con un `<head>` prerenderizado simulado.

### 1.7 Placeholders medidos (punto 7)

**Script nuevo:** `scripts/performance/home-sections-measure.mjs`. Usa Chrome por CDP con el `WebSocket` nativo de Node 22, sin dependencias nuevas, sobre el build del probe con la misma CSS y la API del preview.

**Qué mide en los 16 tamaños de `008-f4.md:65-74`:**
1. Con JS retenido: el `top`/`height` de cada placeholder y si corta el viewport inicial.
2. Con JS, para cada sección por separado (`scrollIntoView`, esperar contenido y estable): altura final y `layout-shift`.

**En qué estados de la API (CDP `Fetch`, sin backend nuevo ni datos inventados):**
- normal;
- fallo de planes;
- soporte nulo (fallo → `catchError` → `null`, igual que el soporte oculto);
- fallo de navegación.

Además, la altura del estado «Consultando planes…».

**Hero (está por encima del pliegue):**
- antes y después de los datos: líneas del CTA y de la reaseguranza, y chips;
- si «Prueba sin tarjeta ·» o la etiqueta de la API añaden líneas en algún tamaño, `min-height` en `lh` igual a lo medido, solo en ese rango.

**Regla para la CSS:**
- Los rangos son los de la propia CSS: ≤767, 768–1023, 1024–1199 y ≥1200.
- La reserva de cada rango es el **mínimo** medido en estado cargado dentro del rango, para que solo pueda crecer, nunca encoger.
- Se guarda como `--fl-reserve-{product,plans,faq,closing}` en `fluora-home.component.css`, con un comentario que apunta al JSON de evidencia y a la huella del build.

**Lo que no se toca:**
- Ni el conteo de planes ni el de FAQ. El rail de planes es horizontal: el conteo cambia el ancho, no la altura. El cierre usa `<details>` compacto.
- Los placeholders siguen siendo un único elemento `aria-hidden`, solo con `min-height` y sin `height`.
- El hero sigue sin `aria-busy`.
- Scrollspy, anclas y las condiciones de GSAP no cambian.

**Si una sección corta el viewport inicial en algún tamaño, se reporta como caso crítico.**

## 2. Etapas dentro de R3 (cada una con su respuesta y revisión)

- **R3-a (sin paquetes nuevos):** §1.1, §1.3, §1.4 y H3, con sus pruebas unitarias.
  - El build de producción sigue siendo CSR. Se reporta la diferencia de tamaño de marketing, porque entra el runtime de hidratación y del replay.
  - **Detenerse para revisión.**
- **R3-b (probe de build):**
  - instalar los dos paquetes en `21.0.6` exactos y revisar el diff del lock;
  - `main.server.ts`, la configuración de servidor y las rutas de servidor;
  - `tsconfig.marketing.server.json` (extiende el de marketing y añade `main.server.ts`);
  - en `angular.json`, la configuración `marketing-hydration-probe` (`server`, `outputMode:"static"`, `outputPath` propio y `define`);
  - el script `build:marketing:hydration-probe`, fuera de `build`;
  - `scripts/check-hydration-probe.cjs`, con sus pruebas `node --test` sobre fixtures.
- **R3-c (`scripts/performance/hydration-probe.mjs`):**
  - preview con `spaShell:'csr'` en un puerto efímero, nunca 4200, 8080 ni 4300;
  - Chrome con perfil temporal;
  - corridas con el build de producción y con el de desarrollo (este da los mensajes completos y la línea «Angular hydrated N component(s)… 0 component(s) were skipped»).
- **R3-d:** medición de §1.6 y §1.7, aplicación de las reservas y nueva medición.
- **R3-e:** suites, Node, builds y guardas completas, y respuesta con sus límites.

## 3. Pruebas

**Unitarias (Vitest/jsdom, en `describe` llamados «contrato», nunca «SSR»):**
- `isHydratableMarketingHome`: marketing/`www` en `/` da `true`; `/acceso`, `/e/x`, app, dominio propio y `location` ausente dan `false`. Los casos actuales de `isMarketingHost` no cambian.
- `createMarketingServerConfig`: lanza error con `undefined`, con `http:`, con orígenes iguales o con ruta distinta de `/`; con `https://marketing.test` y `https://app.test` incluye el contexto. El mensaje de error no repite el valor recibido.
- `createMarketingAppConfig(false)` no incluye hidratación (se prueba resolviendo el `TransferState` y el número de proveedores, o con la guarda estática).
- Compuerta del Home: con planes ya cargados y la navegación en caché (`of()` síncrono), el primer render muestra 0 chips, `fallbackLabel` y no muestra «Prueba sin tarjeta»; después de `afterNextRender` muestra los datos. Las pruebas actuales de anclas y de T7 de R4 siguen verdes, sin ampliar timeouts.
- Appearance: el original se toma del atributo del servidor; la liberación restaura o quita el tag correctamente.

**Guarda estática:**
- `provideClientHydration(` aparece solo en `app.config.marketing.ts`.
- `MARKETING_RENDER_CONTEXT` y `provideServerRendering` solo en los archivos de servidor.
- Nada de eso en `main.ts`, `main.application.ts` ni `app.config.ts`.
- `firebase.json` y el script `build` no hacen referencia al probe.

**`check-hydration-probe.cjs` sobre el HTML real:**
- Existen exactamente `index.html` e `index.csr.html`, y la lista de prerender tiene solo `/`.
- `index.html`:
  - tiene `h1#home-title`, el lead, los CTA, `nghm`, `ngh` y `_ejsas`;
  - en `<body>` no tiene `ngcm`;
  - el JSON de `ng-state` solo tiene las claves de hidratación, sin entradas de caché HTTP;
  - no tiene `.fl-home-plan-card`, `.fl-home-trial-banner`, `data-fluora-support`, `mailto:`, `tel:`, «Prueba sin tarjeta» ni ningún `<a>` en `.fl-home-hero-verticals`;
  - el CTA dice `Crear mi cuenta`;
  - no contiene `localhost` ni `http://`, ni patrones de token o de secreto;
  - todos los orígenes absolutos coinciden con el `define`.
- `index.csr.html` pasa `assertCsrShell` y tiene `ngcm`. Los tres renderers de Functions aplicados sobre él siguen pasando el validador de R1. Esto **anticipa** el siguiente paso, sin activarlo.
- El log del build no tiene `ERROR` ni `NG0`.
- La huella de los chunks del navegador se compara con la del build de producción: si difiere, se documenta la causa.

**Navegador real (probe, en 375×812, 820×1180 y 1440×900):**
- **Reutilización del DOM:** un `MutationObserver` instalado con `addScriptToEvaluateOnNewDocument` guarda los primeros nodos de `h1`, el CTA del hero, `.brand`, el footer y `.plans-placeholder`. Después de estable deben ser **los mismos objetos**.
- Están presentes las marcas `NgHydration` y `NgEventReplay`.
- **Consola:** 0 mensajes `NG0?5\d\d`, 0 `console.error`/`warn` y, en el build de desarrollo, «0 component(s) were skipped».
- **Event replay (375):** se retiene `main-*.js`, se hace clic real en «Más», se libera el script, se abre `dialog[open]` con `aria-expanded=true` y Escape devuelve el foco a «Más». Si falla el foco por el `currentTarget` reproducido, se reporta. Se mide el tiempo entre clic y replay con la API normal y con la API lenta (H2).
- **H4:** el footer de marketing termina igual que en CSR y sin NG05xx.
- **Anclas, scrollspy y resize:** `/#planes` y `/#faq` en 375 y 1440 (destino bajo la barra, `aria-current`); resize 1440→375→1440 con el menú cerrado y el scrollspy recalculado.
- **GSAP:** a 1440 se pide el chunk de GSAP; con reduced-motion emulado, no.
- **CSR:** `/acceso`, `/privacidad`, `/negocios`, `/barberias` y HTML de Functions servido por `Fetch.fulfillRequest` desde los renderers sobre `index.csr.html` con los fixtures de sus tests. Resultado esperado:
  - sin marca `NgHydration`;
  - sin NG0505, también en el build de desarrollo;
  - `<app-root>` reemplazado como hoy.
- **Matriz de §1.7:** los 16 tamaños, con evidencia JSON y capturas en 375, 820 y 1440.

**Las verificaciones Node son SSR real.** El prerender corre en un worker de Node sin `window`, `localStorage` ni `document` global, así que cualquier acceso hace fallar el build. A eso se suma la guarda del HTML.

## 4. Criterios de cierre de R3

1. Solo cambian los archivos de §2. `git diff --stat` lo confirma. Ningún cambio en Functions, `firebase.json`, application, backend, RBAC ni datos.
2. El build de producción sigue siendo CSR:
   - `check-public-builds` pasa sin cambios;
   - `index.csr.html` es idéntico byte a byte;
   - la diferencia de tamaño de marketing se reporta con su causa.
3. El probe de build y su guarda están en verde. El probe de navegador cumple §3 en los dos modos (producción y desarrollo), con evidencia en `/private/tmp`.
4. B3 está medido y decidido. Las reservas están medidas y aplicadas, y el residual por rango está reportado. CLS de carga en el probe ≤ 0.01 en los 16 tamaños (informativo, no es el Lighthouse pareado).
5. `npm test` en configuración normal: **cinco corridas consecutivas en verde** (condición heredada de T2). Más las 37 pruebas Node 22, las pruebas nuevas, los dos builds, las guardas y `diff --check`.
6. Revisión de interfaz en 4200/8080 sin crear datos: Home en 375, 820 y 1440, un tenant y `/acceso`, 0 errores.
7. La respuesta declara los límites y **se detiene para la revisión del usuario.** Sin commit propio de R3 salvo autorización, sin push y sin deploy.

## 5. Límite exacto con el paso siguiente (prerender productivo)

**R3 entrega:**
- la hidratación compartida;
- la decisión por host y ruta;
- la configuración de servidor y la de probe;
- la guarda y el probe de navegador;
- las reservas medidas.

**El paso siguiente, cada parte con su revisión:**
- convertir `production,marketing` en `outputMode:'static'` (o fusionar en él la configuración del probe y retirar el probe);
- adaptar `prepare-csr-shell` y `check-public-builds`: `index.html` prerenderizado, `index.csr.html` nativo con `ngcm` y sin igualdad byte a byte (R1-H4);
- el `assertCsrContract` del preview frente a `<head>` con la CSS crítica de SSR (ajustarlo con evidencia, no relajarlo antes);
- el sitio `tenant-domains` (R2) antes de cualquier despliegue;
- el orden de despliegue Hosting → Functions;
- después, la serie pareada de Lighthouse 5 + 5, el LCP en el h1 o el lead, y las 30 muestras de P2 del tenant.

**Pendientes globales de M3, no acreditados por R3:** meta Home 85, A/BP/SEO 100 y CLS en serie, la matriz de regresión del tenant, el sitio R2, el despliegue, y en dispositivos físicos iOS y host propio.

## 6. Orden para Codex

1. T2 cerrado (cinco suites).
2. R3-a: primero la caracterización de `isMarketingHost`, después la refactorización pura; luego configuración, compuerta y Appearance.
3. Revisión.
4. R3-b.
5. R3-c.
6. R3-d.
7. R3-e.

**Riesgos principales:**
- copiar opciones de la v22 sin verificarlas en 21.0.6;
- aceptar como evidencia una corrida con avisos en consola;
- fijar reservas con una sola medición en lugar del mínimo por rango.

---
Esta sesión solo tiene herramientas de lectura: no guardé `docs/agentes/revisiones/008-f4-m3-r3-diseno.md`. El script de revisión puede guardar este texto tal cual.
