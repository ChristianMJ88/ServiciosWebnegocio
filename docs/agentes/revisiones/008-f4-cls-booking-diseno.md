# Dictamen: CLS de Agendar (`booking-cart-column`)

**Apruebo tu propuesta, con cuatro ajustes.** Revisé el código en modo lectura. No ejecuté nada ni leí los traces.

**Por qué funciona:** `booking-cart-column` vive dentro de `.card.booking-shell` (`booking.component.html:28` → `:632`). Debajo de 992 px la columna del carrito queda bajo `booking-main`. Cuando llega el catálogo, `booking-main` crece y empuja el carrito; eso explica los 0.219 en 820. La medición de CLS no cuenta los nodos con `visibility:hidden`, y cuando el shell se revela cuenta como contenido nuevo, no como desplazamiento. Si el shell se oculta durante la carga inicial, el crecimiento ocurre sin contarse.

Descarto las alternativas:
- **`min-height` o skeleton:** obligan a adivinar alturas.
- **Reordenar columnas:** cambia el diseño.
- **`@if` sobre el shell:** destruye el formulario y el outlet, y al aparecer mueve lo que está debajo.

## Hallazgos priorizados

**P1. Usar una bandera nueva de un solo sentido, no `loadingCatalog`.**
- `loadingCatalog` vuelve a `true` en cada `onBranchChange` (`:377`) y en el reinicio tras reservar (`:865`). Si la reutilizas, el shell se oculta otra vez.
- `loadServices` solo lo pone en `false` si la sucursal sigue coincidiendo (`:903`).
- Propuesta: `readonly catalogoInicialListo = signal(false)`, que solo pasa de `false` a `true` y no se reinicia nunca. `onBranchChange` y el reinicio tras reservar no la tocan.

**P1. Condiciones exactas para revelar:**

| Evento | Dónde | Acción |
|---|---|---|
| `getBranches` falla | `error` en `:892` | revelar (se ve el mensaje de error que ya existe) |
| Llegan sucursales y no se elige ninguna (lista vacía, o varias sin `?sucursal` ni borrador) | `next` en `:880`, si `!branch` | revelar |
| Hay sucursal elegida | `:889` | **no** revelar; queda pendiente de `loadServices` |
| `loadServices` responde para la sucursal actual | `next` en `:905`, **después** de `hidratarServiciosDesdeQuery()` y `selectCatalogGroup` | revelar |
| `loadServices` falla para la sucursal actual | `error` en `:920` | revelar |
| Respuesta de otra sucursal (obsoleta) | ya hay un `return` temprano | no revelar |

Notas:
- Revelar en `next`/`error` y no en `finalize`, porque `finalize` también corre al destruir el componente.
- Mientras el shell está oculto, el usuario no puede cambiar de sucursal. Por eso la única forma de que llegue una respuesta obsoleta es por código. La guarda de sucursal que ya existe basta.
- `loadBranches` pone `loadingCatalog` en `false` y lo vuelve a `true` de forma síncrona (`:881`→`:901`), así que no se pinta nada entre los dos pasos.

**P1. Accesibilidad sin cambiar la geometría.**
- `visibility:hidden` ya saca el shell del árbol de accesibilidad y del foco. Por eso un `aria-busy` puesto en el propio shell no lo anuncia nadie.
- En el shell: `[class.booking-shell--pending]="!catalogoInicialListo()"` y `[inert]="!catalogoInicialListo()"` (el `inert` es una defensa extra). En CSS: `.booking-shell--pending{visibility:hidden}`, sin tocar alturas.
- Fuera del shell (por ejemplo, justo antes, en `:26`): un `<p class="visually-hidden" role="status">` con «Cargando servicios disponibles…» mientras esté pendiente, y vacío después. Al ser `visually-hidden` no ocupa espacio.
- La nota de estado de `:115` se queda como está para los cambios de sucursal posteriores.

**P2. URL con servicios precargados.** El cambio no evita las oleadas de peticiones que llegan después de revelar. Cuando la URL o el borrador traen servicios, `hidratarServiciosDesdeQuery` dispara:
- la cotización (POST de solo lectura), cuyos avisos se insertan **arriba** del paso 1 (`:59-65`) y el texto de `:154`;
- los profesionales (`:1118`), que añaden secciones (`:137-153`);
- los días disponibles, que pueden autoseleccionar una fecha y encadenar las franjas.

Esperar todo eso amplía el alcance y crea cadenas de espera; no lo recomiendo en esta tarea. Propongo medir aparte `?sucursal=…&servicios=…`. Si el CLS ahí es material, lo trato en un diseño separado (posible: ampliar la espera solo a la primera cotización, con su `quoteVersion`). Ni el precio ni el anticipo se recalculan en ningún caso.

**P2. Riesgos a vigilar.**
- Si `getBranches` o `getServices` no responden nunca, el shell sigue oculto, incluida la tarjeta de WhatsApp del aside. Hoy, en ese caso, igual solo se ve un select deshabilitado. Es aceptable sin añadir esperas por tiempo, pero queda registrado.
- Si el LCP actual está dentro del shell, puede retrasarse hasta el revelado. Hay que comparar el LCP antes y después.
- El residual de 0.0045 en 1440: en esa anchura el carrito está al lado y no debajo. Si persiste tras el cambio, viene de fuera del shell (hero o header) y queda fuera de este alcance. Codex debe confirmarlo con los rectángulos.
- Confirmar con rectángulos que el footer empieza fuera del viewport (`min-vh-100`) en las tres anchuras. Si es visible y se mueve, el ocultamiento no basta.

## Alcance para Codex

Solo `booking.component.ts`, `.html`, `.css` y `.spec.ts`. No tocar servicios, cotización, reservas, pagos, carrito, backend ni M3.

Pruebas, usando `Subject` en `getBranches`/`getServices`:
1. Tras `ngOnInit`, con las sucursales pendientes, la bandera es `false`.
2. Si las sucursales fallan, se revela y aparece `errorMessage`.
3. Con `[]` se revela.
4. Con varias sucursales y sin query ni borrador, se revela y `getServices` no se llama.
5. Con `?sucursal=10`, sigue oculto hasta que `getServices` emite. Al revelar, `selectedServices` ya está hidratado en ese mismo paso.
6. Si `getServices` falla, se revela y aparece el error.
7. Después de revelar, `onBranchChange('20')` con servicios pendientes deja la bandera en `true` aunque `loadingCatalog` vuelva a `true`.
8. El reinicio tras una reserva exitosa no oculta el shell.
9. Las pruebas de cotización y reserva que ya existen pasan sin cambios: mismos argumentos de `cotizar` y mismo número de llamadas.
10. Si montar el componente con fixture es viable (ojo con FullCalendar), una prueba de DOM: clase e `inert` en el shell y el estado visible solo para lectores de pantalla fuera de él.

**Validación:** Lighthouse en frío y en caliente, a 390, 820 y 1440, en Agendar sin parámetros, y como dato informativo con `sucursal`+`servicios`. Comprobar además que Servicios e Inicio no empeoran, que `booking-cart-column` desaparece de las fuentes de desplazamiento y que el LCP se mantiene. No apruebo métricas por adelantado; las reviso cuando Codex entregue resultados.

No creé archivo de revisión: escribir esta respuesta en `docs/agentes/revisiones/` queda para quien esté autorizado.
