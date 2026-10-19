No toco ningún archivo, solo leo. Revisé `app.html`/`app.ts`, Header (TS/CSS), Footer, rutas de las dos builds, `app.spec.ts` y el manejo de anclas de Home. Esta es la revisión.

# Revisión 008 F4: reducir el bundle inicial

## Hechos que deciden el diseño

- **Header solo aparece en superficies de tenant.** `esNavegacionMarketing()` (`src/app/app.ts:31`) es falso únicamente si hay tenant activo, si es host propio o si la ruta es `/e/…`. Home, verticales, legales y `/negocios` usan MarketingNav. Diferir Header no afecta Home.
- **El salto de marketing a tenant recarga la página completa.** El directorio enlaza con `[href]` (`marketplace.component.html:12-24`) y Header/Footer con `marketingUrl()` (también `href`). En `/e/…` y en host propio la rama de Header existe desde el primer render, porque `publicPath` sale de `Location.path()` en el constructor. Por eso el chunk se pide al inicio, en paralelo con el resolver HTTP y el chunk de la página. Las dos cosas que necesita la página tardan más que ese chunk local pequeño.
- **La altura de Header no es fija.** Es `sticky` y ocupa ~78px en escritorio y ~72px en ≤991px (`header.component.css:549-555, 579-586, 739-749`). En la home del tenant pasa a `fixed` (`--over-hero`, `:16-22`) y no ocupa espacio en el flujo. Lo decide `tenantHomeActiva()` dentro de Header (`header.component.ts:37-38`). Si App reservara una altura, duplicaría esa decisión. En la home del tenant, que es la página con más tráfico, 78px reservados causarían justo el salto que se quiere evitar.
- **Footer no tiene `id`**, así que ninguna ancla apunta a él. Pero `FluoraHomeComponent` hace `scrollIntoView` del fragmento cuando están listas *sus* secciones (`fluora-home.component.ts:63-68`), sin contar con el Footer. Con el Footer como placeholder la página es más corta, y `#faq` o `#planes` pueden quedar recortados al final del scroll (sobre todo en escritorio). Footer además lanza `publicSupport()` en marketing (`footer.component.ts:26-30`).

## Recomendación: P1 diferir solo Header. Footer se queda como está en F4

### Cambio (solo `src/app/app.html`, `src/app/app.spec.ts` y opcionalmente `src/app/app.css`)

`src/app/app.html:2-4`:
```html
@if (mostrarChromePublico()) {
  @if (esNavegacionMarketing()) { <app-marketing-nav /> }
  @else {
    @defer (on immediate) { <app-header></app-header> }
    @placeholder { <div class="public-header-slot" aria-hidden="true"></div> }
  }
}
```
- En `app.ts:19`, `HeaderComponent` **se queda en `imports`**. Fuera del `@defer` no puede haber ninguna otra referencia a él (ni `viewChild`, ni en el TS). Así el compilador quita el import estático y genera el `import()` dinámico.
- **El placeholder no ocupa altura** (`.public-header-slot{display:block}` o un `div` vacío, sin `min-height`). Inventar una altura rompe la home del tenant. Lo que evita el salto es el orden de carga: el chunk sale en t≈0 y la página necesita resolver + chunk. No se añaden `@loading` ni `minimum`, porque solo retrasarían el montaje.
- No hace falta `prefetch`: `on immediate` ya descarga al renderizar la rama, y en Home la rama nunca se renderiza.
- Header no cambia en nada: componente, contratos, CSS y la regla global `styles.css:69` siguen igual. Su constructor lee `router.url` y `scrollY` al montarse y se suscribe a `NavigationEnd`, igual que hoy, así que funciona aunque se cree más tarde.

### Tests (`src/app/app.spec.ts`)
Angular 21 usa `DeferBlockBehavior.Playthrough` en TestBed, así que las aserciones síncronas de `:35` y `:36` van a fallar.
1. Marketing (`/`): `fixture.getDeferBlocks()` no encuentra el bloque de Header, `app-header` es null y `app-marketing-nav` está presente.
2. `/e/x` y host propio: tras `detectChanges()` existe `.public-header-slot` y todavía no `app-header`. Tras `await fixture.whenStable(); fixture.detectChanges();` (o `(await fixture.getDeferBlocks())[0].render(DeferBlockState.Complete)`), `app-header` está montado y `app-marketing-nav` sigue en null.
3. Se mantienen sin cambios las aserciones del Footer en esos dos tests y `expectNone` de soporte.

### Validación
- **Bundle:** `dist/marketing/stats.json` y la tabla de la build deben mostrar menos `Initial total` y un lazy chunk nuevo. Con grep de `site-header--tenant` y `brand-initials` solo en ese chunk, nunca en `main-*.js`. La build `application` debe seguir correcta (allí Header casi nunca aparece).
- **Home Lighthouse:** cinco corridas, mediana ≥81, A/BP/SEO 100, CLS ≤0.001. No debería aparecer ninguna petición del chunk de Header en la cascada de red.
- **Tenant, en móvil 390, tablet 820 y escritorio 1440, con Slow 4G y caché vacía:**
  - `/e/{slug}`: hero con header `fixed`, sin salto.
  - `/e/{slug}/servicios`, `/productos` y `/checkout`: `sticky`, y el `top: var(--tenant-header-height)` de `.store-navigation` y `.checkout-summary` sigue correcto.
  - Host propio en `/` y `/servicios`.
  - En el Performance panel, Header tiene que montarse antes que el contenido del `router-outlet`. CLS de Lighthouse en tenant home e interna ≤ al valor actual.
- **Navegación:** dentro del tenant (inicio → servicios → agendar → contacto) Header no se desmonta, el menú móvil abre y cierra, Escape funciona y el `resize` a ≥992 lo cierra. Tenant → enlace de marketing es recarga completa y muestra MarketingNav. Las rutas de auth y panel (layout `auth`/`panel`) siguen sin chrome.
- **Sin cambios de comportamiento:** la marca "Fluora" que se ve antes de que resuelva el tenant ya ocurre hoy y no empeora. Queda fuera de alcance.

## Footer: no diferirlo en F4

Hay una sola referencia (`app.html:10-12`), así que un `@defer` bastaría para sacarlo del inicial. No lo recomiendo ahora:
1. **Anclas:** con `on viewport`, la página es más corta en el momento del `scrollIntoView` y `#faq`/`#planes` pueden quedar recortados. Arreglarlo exige que Home espere al Footer o reservar altura. Esa altura depende de `tenantFooterColumnas()`, de `support()` y del breakpoint, así que sería inventada.
2. **Tenant:** `when tenantContext.activo()` sobra, porque la compuerta `@if` ya la exige. En tenant el ahorro solo importa en la build marketing, y ahí Footer ya va debajo del contenido.
3. Diferirlo también retrasa `publicSupport()`. Mejora la red de Home, pero cambia cuándo se pide un dato que hoy funciona bien, y eso merece su propia validación.

Si más adelante se quiere intentar (otro paso, medido después de P1): un único bloque `@defer (on viewport; on idle)` alrededor de la referencia existente, dentro de la misma compuerta. El placeholder sería un `<div>` vacío con las mismas clases `marketing-public-footer` y `marketing-bottom-space`, para conservar el `padding-bottom` de la barra inferior móvil (`app.css:133`). La condición de aceptación sería pasar pruebas de anclas profundas (`/#faq`, `/#planes` en 390/820/1440), con el objetivo en `top≈0` después de montar el Footer.

## Fuera de alcance (cambios mayores)

- Separar MarketingNav o CDK del inicial: CDK (~42kB fuente) es la siguiente pieza grande. Primero hay que ver qué importa CDK en el inicial; probablemente MarketingNav o el aviso de suscripción.
- Hacer que la home del tenant precargue Header desde el resolver, o mover la decisión `fixed`/`sticky` a App para poder reservar altura. Ambos tocan contratos de Header y rutas.
- Dividir `styles.css` global: necesita inventario, como ya está restringido.

**Prioridad:** P1 diferir Header (bajo riesgo y no toca Home). P3 Footer, después y medido.
