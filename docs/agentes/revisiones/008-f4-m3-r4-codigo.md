Terminé la lectura. No pude crear `docs/agentes/revisiones/008-f4-m3-r4-codigo.md` porque en esta sesión solo tengo Read, Glob y Grep. Abajo está el texto completo para guardarlo con ese nombre. Tampoco pude correr `git diff`, así que comparé cada archivo actual contra el diseño, no línea por línea contra `00ec341`. No ejecuté suites ni builds, y no leí `.env`, `postman/` ni credenciales.

---

# 008 F4 M3 · R4: revisión de implementación

Autor: Claude Code, 2026-10-20. Revisión de solo lectura de `solicitudes/008-f4-m3-r4-codigo.md`, comparada con `revisiones/008-f4-m3-r4-diseno.md`.

## Veredicto

**El código queda aprobado, con dos condiciones:**
1. Atender M1 o justificar por qué no.
2. Registrar las verificaciones medidas que faltan (§4).

No hay hallazgos altos. La implementación sigue el diseño archivo por archivo y no agrega nada fuera de alcance.

## 1. Qué quedó bien

**Contexto y host** (`marketing-render-context.ts`, `platform-host.service.ts:20-30,70-72`)
- La validación cumple §1.1, el resultado se congela y el mensaje de error es fijo.
- El servidor sin contexto lanza error.
- El token solo se inyecta del lado del servidor, así que el navegador no lo lee.
- La API pública no cambió.

**Guardas de los grep**
- `MARKETING_RENDER_CONTEXT` y `provideMarketingRenderContext` solo aparecen en el archivo que los define, en `platform-host.service.ts` y en dos specs. No aparecen en `main*.ts` ni en `app.config*.ts`.
- No hay `@angular/ssr`, `platform-server`, `outputMode`, `prerender` ni `provideClientHydration` en `package.json`, `angular.json` ni en `src/**/*.ts`.

**A1/A2** (`auth.service.ts:69`, `platform-auth.service.ts:20`)
- El campo `browser` se declara antes de `sesion`, que es el riesgo principal que señalaba el diseño.
- `loadSession`, `cargar`, `saveSession`, `guardar`, `limpiarSesion` y `cerrar` no tocan el storage en el servidor.
- El `removeItem` dentro del catch queda cubierto por el guard.

**B1:** el timer solo se crea en el navegador (`subscription-access-notice.component.ts:56`).

**C1:** `browser` se declara antes de `context` (`marketing-tracking.service.ts:33-35`).
- `restore`, `capture` y `emit` no hacen nada en el servidor.
- `registrationUrl` usa `new URL(base)`. Si `base` es relativo, lanza error, y la prueba lo comprueba.

**C2:** en el spec de tracking se usa `runInInjectionContext(() => new …)`. Así cada caso tiene una instancia nueva que restaura desde el storage de verdad, y las aserciones no cambiaron. Me parece mejor que `TestBed.inject`.

**D1–D3**
- `App` usa `DOCUMENT` y el tema solo se aplica en el navegador.
- `prepareMarketingHome` termina antes de inyectar el host. Eso evita que el fail-closed se dispare en el warm-up.
- En el servidor, `provideTenantPublicStyles` devuelve un no-op.

**E1/E2/F1**
- En Nav, todo lo que depende del navegador está dentro del guard: gutter, `NavigationEnd`/`closeMenu`, los listeners, `matchMedia` y el `onDestroy`.
- `offer.load`, `navegacion` y `activate(true)` quedan fuera del guard, como pedía el diseño.
- `MarketingAppearanceService` solo usa `Meta`, así que es válido en el servidor.
- Home y la directiva terminan antes de `matchMedia` y de IO.

**§3:** los tres métodos devuelven `EMPTY`.
- `MarketingOfferService.load` (línea 19) solo pone `loading` en `false` en `next` o `error`. Con `EMPTY`, `loading()` sigue en `true` y la etiqueta del CTA es la misma que en el primer render del cliente. Es la consecuencia que buscaba el diseño.
- Footer y Home tampoco reciben valores sintéticos.

**Agregado en el parse** (`marketing-render-context.ts:30`)
- Rechaza el mismo hostname con puertos distintos. Es correcto: sin esto, el servidor quedaría como app y marketing a la vez y `FluoraHome:61` redirigiría a `/acceso`.

## 2. Hallazgos

### M1 · Media. Los globals tóxicos no tienen un control positivo

**Dónde:** `marketing-server-shell.spec.ts:23-28,45-47` y `marketing-server-contract.spec.ts:25-26`.

**Problema:** las aserciones solo dicen «no se llamó». Si un veneno no está conectado a lo que el código realmente lee, la prueba pasa sin demostrar nada. Dos ejemplos:
- `vi.spyOn(window,'addEventListener')`: en el global jsdom de vitest esa propiedad es un getter. Además, el código llama por `document.defaultView`.
- `matchMedia` no existe en jsdom, así que `win?.matchMedia?.()` puede no tocar nunca el stub.

**Agravante:** `FluoraHome.ngAfterViewInit` es `async`. Si el guard fallara, el veneno se convertiría en una promesa rechazada, no en un error durante `detectChanges`.

**Qué hacer:**
1. Cambiar `forbidden` por un `vi.fn` que registre las llamadas y lance error. Aplicarlo a `matchMedia`, IO y RO, y verificar `not.toHaveBeenCalled()` en cada uno.
2. Agregar un caso de control en el mismo archivo, con los mismos venenos y `PLATFORM_ID` en `'browser'`, que monte `MarketingNavComponent` o `App`. Ese caso debe comprobar que:
   - `windowListeners` recibe `'scroll'`;
   - `documentListeners` recibe `'click'` y `'keydown'`;
   - el storage se lee al menos una vez.
3. O, como mínimo, comprobar `document.defaultView === globalThis`.

**Cómo validar:** el control falla si se quita el guard de `marketing-nav.component.ts:40`, y el caso del servidor sigue en verde.

**Estado:** según la solicitud, la prueba integradora seguía en ejecución, así que todavía no está cerrada.

### B1 · Baja. Se acepta un origen integrado idéntico

**Dónde:** `marketing-render-context.ts:30`.

**Situación:** se desvía un poco del diseño, pero se puede aceptar. Con orígenes idénticos, `hasDedicatedAppHost()` da `false` y no hay redirección; es la misma clasificación que tiene hoy el navegador local.

**Qué hacer:**
- En el spec (`marketing-render-context.spec.ts:25-28`), comprobar además que `isMarketingHost()` da `true` y `isCustomTenantHost()` da `false`.
- Anotar para el paso de prerender que `check:public-builds` debe exigir orígenes de producción distintos y `https:`.
- Registrar esta decisión en la respuesta.

### B2 · Baja. El shell no comprueba los efectos de `<html>`

**Dónde:** `marketing-server-shell.spec.ts`.

**Qué falta:** comprobar que siguen sin aplicarse dos cosas que los guards ahora evitan:
- `document.documentElement.style.scrollbarGutter === ''` (gutter de Nav);
- `dataset['tenantTheme']` y `--tenant-primary` ausentes (tema de `App`).

**Por qué importa:** son baratas de agregar, y es justo lo que protegen los guards de E1 y D1.

### B3 · Baja. Diferencia del primer pintado, para R3

**Situación:**
- En el servidor no se aplica el tema, así que el HTML prerenderizado no tendrá `data-tenant-theme` ni las variables inline.
- Los valores por defecto de `:root` en `src/styles.css:24-45` no son idénticos a los que aplica `App`. Por ejemplo, la pila de `--tenant-body-font` es distinta.

**No es un error de R4:** no causa problemas de hidratación, porque `<html>` queda fuera del árbol de la app.

**Qué hacer en R3:**
- comprobar si la hoja de marketing usa esas variables;
- si las usa, aplicar los mismos valores por defecto también en el servidor o medir el cambio antes y después del bootstrap.

### B4 · Baja. La caracterización no cubre `cleanOrigin` ni `location` ausente

**Dónde:** `platform-host.service.spec.ts:5-36`.

**Qué falta:** casos para valores de runtime con barra final o espacios, y para `location` en `undefined`.

**Por qué importa:** como no pude ver el diff, la equivalencia del navegador se apoya en la caracterización 9/9 previa al refactor. Agregar esos dos casos la refuerza sin costo.

## 3. Sin hallazgos (revisado)

- Interceptor, Footer, SEO, Appearance, Plans y `api-base-url` no cambiaron, y eso es correcto.
- `marketing-sections.directive.spec.ts:10-19` ya cubre RAF e IO en el servidor. Eso justifica el límite declarado de no envenenar RAF en la prueba integradora.
- Contar timers en el spec del aviso (`vi.getTimerCount()`) es una medida indirecta, pero suficiente; queda como observación.

## 4. Lo que falta para cerrar R4 (lo mide Codex)

1. M1 resuelto, y la prueba integradora en verde con su conteo.
2. Dos `npm test` normales seguidos (Node 20.19, `TZ=America/Mexico_City`, workers predeterminados), con conteos.
3. Las 37 pruebas en Node 22, los dos builds y `check-public-builds`.
4. `index.csr.html` idéntico byte a byte a `index.html`.
5. Tamaño inicial de marketing dentro de ±1 kB de 476.17 kB, con la diferencia reportada.
6. Revisión de UI en 375, 820 y 1440, en 4200/8080, con pestaña propia y sin usar 4300.
7. La respuesta registra los límites: no es SSR real, la hidratación queda para R3 y el contexto queda sin proveedor.

Después de esto, detenerse para la revisión del usuario antes de R3. Sin commit, push ni despliegue.
