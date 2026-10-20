# Dictamen: CLS en las páginas públicas de los negocios

**Veredicto:** apruebo la dirección de la propuesta, con tres ajustes. El primer pintado del Header tiene que salir ya con sus clases definitivas. Hay que añadir una regla que muestre el contenido cuando un slug no tiene negocio. Y no hace falta `afterNextRender` ni un temporizador nuevo. Solo leí los archivos autorizados y no ejecuté nada. Mientras revisaba, `app.spec.ts` cambió de numeración, así que cito sus pruebas por nombre.

## Causa (según el código)

1. **Se borra un contexto ya cargado.** En `src/app/app.ts:47`, el constructor llama a `sincronizarTenantConRuta(this.router.url)`. Antes de la primera navegación, `router.url` vale `'/'`. En un slug `/e/x` sin host propio, eso llama a `clearTenant()` (`app.ts:127`) aunque `publicPath` (`app.ts:31`) ya tenga la ruta correcta.
2. **El Header arranca con la ruta equivocada.** En `src/app/components/header/header.component.ts:26`, `rutaActual` se toma de `router.url`, que también vale `'/'`. Con el negocio todavía inactivo, `fluoraHomeActiva` (`:36`) sale verdadero y el Header se pinta como el de Fluora, en `position: fixed` (`header.component.css:24-27`). Al activarse el negocio, `tenantHomeActiva` (`:37`) compara contra esa `'/'` vieja y da falso, así que el Header pasa a `sticky`, dentro del flujo (`css:1-3`). Eso empuja el `<main>` hacia abajo. Coincide con el filmstrip (primero Fluora, luego el negocio) y con el `main y0→72/74`.
3. **El Header puede montarse después de que el contenido ya se pintó.** En `src/app/app.html:6-11`, el Header se monta con `@defer` + `tenantStyles.ready()`, sin esperar a que el negocio esté activo. El `<main class="min-vh-100">` (`:18`) pinta antes, y en las páginas internas el Header `sticky` lo desplaza al aparecer.

## Diseño

### D1. Usar la ruta real desde el inicio (prioridad alta)
- `app.ts:47`: cambiar a `this.sincronizarTenantConRuta(this.publicPath())`.
- `header.component.ts:26`: inyectar `Location` e inicializar con `normalizarUrl(router.navigated ? router.url : (location.path() || '/'))`. Así se evita la clase Fluora y la comparación con `homeLink()` falla menos antes de `NavigationEnd`.

### D2. Montar el Header solo con su estado final (prioridad alta)
- `app.ts`: añadir `navegacionResuelta = signal(false)`. Se pone en `true` con `NavigationEnd | NavigationCancel | NavigationError`. Ahora solo se escucha `NavigationEnd` (`:54`).
- `app.html:7`: cambiar la condición a `tenantStyles.ready() && (tenantContext.activo() || navegacionResuelta())`. Con esto no aparece la variante Fluora en `/e/*` mientras el resolver trabaja. Si el slug no existe, el Header se monta igual cuando la navegación se resuelve. Al pasar de un negocio a otro la condición sigue siendo verdadera, así que **se conserva la misma instancia** del Header.

### D3. Ocultar el contenido sin destruir el outlet (prioridad alta)
- Poner una ref local `#tenantHeader` en `<app-header>` y otra `#tenantHeaderError` en un nuevo bloque `@error { <div #tenantHeaderError class="public-header-slot" aria-hidden="true"></div> }`. El slot se queda sin altura.
- En `app.ts`, usar consultas por string con `read`:
  `private readonly headerRef = viewChild('tenantHeader', { read: ElementRef });`
  `private readonly headerErrorRef = viewChild('tenantHeaderError', { read: ElementRef });`
  **No usar `viewChild(HeaderComponent)`**: referenciar la clase desde TypeScript vuelve la dependencia eager y rompe el `@defer`. Además hay que quitar `HeaderComponent` del array `imports`. Codex debe confirmar que, con el defer, el compilador no lo marca como dependencia eager. La prueba de `getDeferBlocks()` ya lo cubre.
- Añadir un estado derivado, sin flags de negocio:
  ```
  contenidoPendiente = computed(() =>
    mostrarChromePublico() && !esNavegacionMarketing() &&
    !headerRef() && !headerErrorRef() &&
    !(navegacionResuelta() && !tenantContext.activo()));
  ```
- En `app.html:18`: `[class.public-content-pending]="contenidoPendiente()"` y `[attr.aria-busy]="contenidoPendiente() || null"`. El `<router-outlet>` sigue vivo, sin `@if`.
- En `app.css`: `.public-content-pending{visibility:hidden}`. Uso `visibility` y no `opacity` porque los elementos con `visibility:hidden` no cuentan como desplazamiento.
- El footer, WhatsApp y el chat (`app.html:22,27,35`) también deben exigir `!contenidoPendiente()`.

### Cuándo mostrar el contenido
Basta el `computed` dentro de la plantilla. La consulta por señal se actualiza en el mismo ciclo de detección de cambios en que el bloque diferido inserta el Header, y la vista de App se vuelve a evaluar antes de pintar. Header y contenido aparecen en el mismo frame y el Header ya trae sus clases (D1 y D2). `afterNextRender` solo retrasaría un frame sin ganar nada.

**No hace falta un temporizador nuevo:**
- Si el CSS falla o tarda, ya se muestra igual por el `onerror` y por el timeout de 10 s en `tenant-public-styles.ts:30-43`.
- Si falla el chunk, entra `@error`.
- Si el negocio no existe, `navegacionResuelta && !activo`.
- El único riesgo que queda es un chunk colgado sin error de red. Es el mismo riesgo que ya tiene cualquier ruta lazy, así que lo acepto.

### Riesgo a revisar
Si alguna clase de `tenant-public.css` o de las páginas públicas fuerza `visibility: visible`, ese elemento se vería durante la espera. La búsqueda solo encontró esa regla en `marketing-bootstrap.css` (generado), en el admin y en caja. Codex debe confirmar que `styles.tenant-public.css` no tiene utilidades como `.visible`.

### Fuera del alcance
No hay alturas de placeholder ni cambios en branding, backend, RBAC, M3 ni hosting. `HomeMarketing` no se toca: `esNavegacionMarketing` deja `contenidoPendiente` en falso, no crea defer y no carga CSS.

## Pruebas exigidas (`app.spec.ts` / `header.component.spec.ts`)

1. **URL inicial:** `router.url` en `'/'` con `Location.path()='/e/x'` y un negocio cargado antes de `createComponent`. Tras el constructor, `tenantContext.activo()` sigue `true` y no se llamó a `clearTenant`. Repetir con host propio.
2. **Header con ruta real:** se crea con `Location.path()='/e/x'` y el negocio activo antes de `NavigationEnd`. `fluoraHomeActiva()` es falso y `tenantHomeActiva()` coincide con `homeLink()`. La clase `site-header--fluora-home` no aparece en ningún momento.
3. **CSS retrasado** (`ready=false`): `main` tiene `.public-content-pending` y `aria-busy`, y no hay Header, footer ni elementos flotantes. Con `ready=true` y el bloque en Complete, desaparece la clase en el mismo `detectChanges` + `whenStable`.
4. **Negocio retrasado:** CSS listo y defer en Complete, pero negocio inactivo y sin navegación resuelta. No hay Header y el contenido queda pendiente. Al llamar `setTenant`, aparecen el Header con su marca y el contenido, y nunca se renderiza el texto "Fluora".
5. **Chunk con error:** `block.render(DeferBlockState.Error)`. No hay Header, existe el slot de `@error` y el contenido queda visible.
6. **Slug inexistente:** emitir `NavigationError` (o `NavigationEnd`) sin negocio. El contenido queda visible y no se queda colgado.
7. **HomeMarketing:** sin bloques defer, sin `ensure`, sin `app-header` y sin `.public-content-pending`.
8. **`/acceso` y `/admin/resumen`:** sin chrome y sin `.public-content-pending`.
9. **Cambio de negocio:** con el router de prueba, navegar `/e/a` → `/e/b`. La instancia de `app-header` es la misma (`toBe`) y el contenido nunca vuelve a ocultarse.
10. **Salida a marketing:** `/e/a` → `/`. Se destruye `app-header`, aparece `app-marketing-nav` y el contenido queda visible.
11. **Defer intacto:** `getDeferBlocks()` sigue en 1 en las rutas de negocio. Comprobar en el build que el chunk del Header sigue separado (`scripts/check-public-builds.cjs`).

## Validación posterior (sin aprobar métricas por adelantado)

- **Medir CLS:** después de implementar, frío y caliente en 390, 820 y 1440 para home, servicios, booking y checkout del negocio. Comparar contra M0/M2: `.0853/.0812`, `.0613/.0608` y `.2205`.
- **Trace:** el `main` debe salir con su `y` final al hacerse visible, sin el salto 0→72/74. El filmstrip no debe mostrar el Header de Fluora en `/e/*`.
- **Accesibilidad y marca:** Header y contenido aparecen en el mismo frame. Revisar foco y `aria-busy`, y que los colores y fuentes del negocio no cambien.
- **Regresiones:** suite completa, más `HomeMarketing` sin el CSS del negocio y el panel sin chrome.

Si con D1–D3 el CLS a 1440 sigue alto, la siguiente hipótesis es el cambio de fuentes o imágenes del hero, no el Header, y hay que buscarla en otro trace.
