**Dictamen:** los dos hallazgos que pedí corregir (P2 y P3) quedan cerrados en el código. El paso B coincide con el diseño. Solo leí archivos: no ejecuté pruebas, builds ni Lighthouse, así que no apruebo cifras, suites ni builds.

## Hallazgos de la revisión de código

- **P2 cerrado.** El caso `booking-shell.spec.ts:19-80` cubre todo lo que pedí:
  - Precarga con `servicios:'1'`, y cotización y personal en `Subject`.
  - Mientras espera: `inert` y `aria-busy="true"`; el estado dice «Preparando tu selección», está fuera de la tarjeta (`:53`) y `.booking-flow` sigue siendo el mismo nodo (`:54`, `:62`).
  - Si llega el personal pero no la cotización, `inert` sigue puesto (`:56`). Con la cotización, `inert` se quita y el estado queda vacío (`:60-61`).
  - Variante de error: el botón «Reintentar cotización» se puede usar. Al hacer clic, `cotizarCitas` se llama dos veces y llega la cotización del mock (`:64-69`).
- **P3 cerrado.** Hay un espía de `console.error` con `not.toHaveBeenCalled()` en los dos primeros casos de `booking-calendar.component.spec.ts` (`:35/41`, `:45/52`).
- **El P3 opcional sigue abierto.** No hay prueba con el personal todavía pendiente durante `onBranchChange`. No bloquea; queda como deuda opcional.

## Paso B frente al diseño

- **Activación.** Se cumplió la condición que puse para activarlo. La traza en frío muestra dos movimientos de `booking-service-browser` de 83.14px, ida y vuelta, durante la segunda cotización. Encaja con H3 (la nota desaparece y vuelve a aparecer).
- **Plantilla.** `booking.component.html:155-156` es `@if(cotizacion(); as presupuesto) {…} @else if (cotizando()) {…}` con el texto «Confirmando condiciones de tu reserva…», tal como se diseñó.
  - No muestra montos viejos ni inventados.
  - `booking-quote-status` solo identifica el nodo, sin CSS nuevo.
- **Sin hueco entre estados.** `cotizarReserva` pone `cotizacion` en null y `cotizando` en true dentro de la misma llamada síncrona (`booking.component.ts:450-454`). No hay un render intermedio sin nota. En la salida temprana (`:453`) no aparece la nota neutral, que es lo correcto.
- **Prueba 9 cubierta** (`booking-shell.spec.ts:71-79`): texto exacto, sin `$`, `cotizacion()` en null, tarjeta activa y luego reemplazada por la respuesta.
  - **P3 de solidez, opcional.** La aserción final (`:79`) solo comprueba que ya no dice «Confirmando». Sería más firme comprobar `toContain('pendiente de confirmación')`, porque fija que llegó la respuesta del backend.

## Riesgo antes de medir

- **Puede quedar un salto menor.** La nota neutral ocupa una línea, y la nota con cotización a 820px probablemente ocupa más (el salto medido fue de 83px). Es posible que quede un movimiento residual más pequeño, y eso no contradice el diseño, que pide no fijar alturas.
- **Si aparece:** hay que reportarlo con el rectángulo y el nodo que lo causa. No se debe agregar `min-height`, conservar la cotización anterior ni extender la barrera sin un nuevo diseño.

## Pendiente para cerrar 008-F4 CLS

- La suite completa y los tres builds en verde, con el chunk del calendario aún separado.
- La medición del paso B con 820px en frío y en caliente: sin `TypeError` de `getApi`/`gotoDate` y sin que empeoren las 24 muestras sin parámetros.

Sigue sin abrirse M3, backend, reglas de negocio ni commit.
