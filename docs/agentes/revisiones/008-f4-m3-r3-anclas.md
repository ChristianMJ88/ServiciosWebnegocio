**Dictamen:** la propuesta va bien encaminada, pero tal como está **no garantiza** el resultado. Es aprobable con dos correcciones obligatorias (C1 y C2). Revisé todo solo en lectura y no ejecuté nada.

## La causa coincide con la evidencia

En `failure.json`, el FAQ queda con `top=99.625`. Le restas el `scroll-margin` de 78 y salen 21.625 px, justo una línea del `<p>` del encabezado de Planes, que en móvil mide 13px (`plans.component.css:13`).

La secuencia es esta:
1. `fluora-home.component.ts:68` solo comprueba que `plansSection` existe.
2. Hace el scroll y borra `pendingAnchor` (`:70-71`).
3. Después, `plans.component.ts:13-14` pone `canScroll=true`.
4. Aparece el bloque `@if(canScroll())` de `plans.component.html:2`, el título se estrecha y el texto gana una línea.

Descarto la imagen de Producto como causa, porque tiene `width`/`height` y su espacio ya está reservado. El `aria-current=Planes` que mide `MarketingSectionsDirective` es una consecuencia del desplazamiento, no un segundo fallo; ahí no hay que tocar nada.

## C1 (alta): `firstRailMeasured` no dispara render en zoneless

La app usa Angular 21.0.6 sin zone.js. En ese modo, escribir una señal solo programa un render si la lee una plantilla o un efecto.

- `firstRailMeasured` solo lo leería `layoutReady`, y `layoutReady` solo se leería dentro de `afterEveryRender`. Ninguno de los dos es reactivo.
- Si `canScroll` no cambia (por ejemplo, en escritorio sin overflow, donde pasa de false a false), el aviso del ResizeObserver no produce ningún render. `afterEveryRender` no vuelve a ejecutarse y el ancla queda pendiente para siempre.
- El fallback con `queueMicrotask` tiene el mismo problema.

**Corrección:** cambiar el `afterEveryRender` de Home por un `afterRenderEffect` que lea `plans()?.layoutReady()`, `dataReady()` y los `viewChild`. Así el cambio de `firstRailMeasured` programa el render por sí solo. Se mantiene la guarda `if(!this.pendingAnchor) return;`, igual que las cancelaciones por wheel/touch/teclado y su limpieza en `DestroyRef`.

## C2 (alta): orden de escritura en el callback del observer

Para que los controles ya estén en el DOM cuando se haga el scroll, `canScroll.set(...)` y `firstRailMeasured.set(true)` tienen que ir en el mismo callback, de forma síncrona y sin `await` entre ellos.

Con eso, el orden queda así:
1. El ResizeObserver escribe las dos señales.
2. El scheduler lanza una sola pasada de detección de cambios.
3. Esa pasada renderiza el `@if(canScroll())` con los controles.
4. Solo después se ejecuta la fase posterior al render.
5. `scrollIntoView` fuerza el layout, que ya incluye los controles.

Esto es lo que da la garantía, sin sleeps ni scroll repetido.

## Puntos de la propuesta que confirmo

- `layoutReady = !loading && (failed || !plans.length || firstRailMeasured)` es correcto. El query del rail se actualiza en la misma pasada que muestra la lista, antes de que se ejecuten los hooks posteriores al render. Por eso el reset a false en el setter llega a tiempo.
- `viewChild<HomePlansComponent>('plansReady')`: `#plansReady` en el componente ya devuelve la instancia, sin `read:`.
- El alcance es correcto: no toca reglas, precios, capacidades ni backend.

## Límite a verificar (media)

El primer aviso del ResizeObserver solo llega si el rail tiene un tamaño distinto de 0×0. Hay que confirmar que `.fl-home-plans-rail` no queda con `display:none` en ningún breakpoint. Si alguna vez lo quedara, el comportamiento sería no hacer scroll, lo que es aceptable, pero debe quedar documentado.

## Pruebas propuestas

**Unitarias en `fluora-home.component.spec.ts`**, con un `ResizeObserver` falso que capture el callback:
1. **Con overflow:** tras renderizar con planes, el scroll no se ha llamado. Se fijan `scrollWidth > clientWidth` en el rail, se invoca el callback y se espera `await fixture.whenStable()` **sin `fixture.detectChanges()`**, para demostrar que el scheduler se activa solo. El mock de `scrollIntoView` debe comprobar **en el momento de la llamada** que existe `.fl-home-rail-controls`. Se verifica una sola llamada.
2. **Sin overflow:** el mismo flujo con `canScroll` en false debe hacer scroll una vez. Esta prueba es la que detecta C1.
3. **Sin ResizeObserver** (`vi.stubGlobal('ResizeObserver', undefined)`): el fallback por microtask hace scroll una vez.
4. Un plan con error (503) y uno vacío hacen scroll sin medir. Las pruebas actuales `:29-30` deben seguir verdes sin cambios.
5. Un gesto (wheel) antes del callback del observer cancela y no hay scroll.
6. Si ocurre un resize después del scroll, no se vuelve a hacer scroll.

**Unitaria en `plans.component.spec.ts`:** `layoutReady` es false antes del callback y true después, y vuelve a false al cambiar el rail (estado loading tras reintentar).

**Real con `navigation-hydration-probe`:** `/#faq` y `/#planes` en 375×812 y en escritorio, más un resize posterior. Se acepta si `top` está dentro de 78±1, `aria-current` coincide con el ancla y hay 0 errores de consola y NG05xx. Comparar con el log actual.

## Alternativa (no recomendada ahora)

Reservar siempre el hueco de los controles, ocultos e inertes cuando `canScroll` es false. Eliminaría el desplazamiento de raíz, pero cambia el layout de escritorio y el CSS ya aprobado. Solo la consideraría si C1 y C2 no bastan en la medición real.

No hay cierre de R3, commit ni deploy. Si quieres que esto quede como revisión en `docs/agentes/revisiones/`, hay que guardarlo desde una sesión con permiso de escritura en docs; esta sesión es solo de lectura.
