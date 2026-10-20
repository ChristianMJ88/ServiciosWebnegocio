No encontré errores bloqueantes en el paso A ni en el calendario. Solo leí el código: no ejecuté pruebas, así que no afirmo que estén en verde. Falta una aserción DOM, que se pide abajo.

## Comparación con el diseño

- **Las señales solo avanzan de pendiente a resuelto.** `cotizacionInicialPendiente` solo pasa a `true` en `booking.component.ts:1128`. Eso ocurre dentro de la hidratación, protegida por `queryHydrated` (`:1113-1114`), y antes de `selectedServices.set` (`:1129`), así que se cierra H1. En `staffInicialPendiente` solo se agregan ids con `inicial = true` (`:1031`), y únicamente la hidratación lo pasa (`:1132`). `addSelectedService` (`:499`) usa el valor por defecto. `onBranchChange` (`:375`) y `handleSuccess` (`:849`) marcan `queryHydrated`, así que no pueden reactivar la barrera.
- **Respuestas obsoletas y errores:**
  - **Cotización:** la barrera se libera en tres puntos: la salida temprana (`:453`), y `next`/`error` después de comprobar la versión (`:460-461`). Una respuesta obsoleta no la libera.
  - **Personal:** `finalize` (`:1032-1033`) cubre éxito, error, respuesta descartada (`:1036`) y destrucción. La salida temprana (`:1030`) nunca agrega el id. No cambia el número de llamadas.
- **Plantilla:** las tres referencias de `:29` y el estado de `:26` siguen el diseño. El texto de estado queda fuera de la tarjeta.
- **Calendario:** `afterRenderEffect` está en fase write, la API nula está protegida y la comparación usa año/mes/día locales (`booking-calendar.component.ts:18-25`). No hay UTC ni temporizadores, y el `@defer` no cambia. El test doble reproduce bien el contrato real: la API es nula hasta `ngAfterViewInit`.

## Hallazgos

1. **P2. Falta la aserción DOM durante la espera de cotización.** Las pruebas de la barrera (`booking.component.spec.ts:29-77`) solo miran la señal. `booking-shell.spec.ts` solo cubre el caso sin precarga. Propongo un único caso nuevo en `booking-shell.spec.ts`, sin tocar el calendario:
   - Con `servicios: '1'`, cotización y personal en `Subject`, y `fixture.detectChanges()` tras el catálogo:
     - `.booking-shell` tiene `inert` y `aria-busy="true"`.
     - El estado de fuera dice "Preparando tu selección".
     - `.booking-flow` es el mismo nodo de antes.
   - Al emitir personal y cotización: desaparece `inert` y el estado queda vacío.
   - Variante de error: con `quote.error(...)`, el botón "Reintentar cotización" aparece fuera de `inert` y al hacer clic vuelve a llamar a `cotizarCitas`. Esto cubre la prueba 3 del diseño, que hoy solo comprueba señales (`:44-51`).
2. **P3. Falta el espía de `console.error` en `booking-calendar.component.spec.ts`.** El diseño lo pedía en la prueba 8.4. Basta un `vi.spyOn(console, 'error')` con `not.toHaveBeenCalled()` en los dos primeros casos (`:34-48`).
3. **P3, opcional. Personal pendiente al cambiar de sucursal.** En `:62-67`, el personal ya llegó antes del cambio. Un caso con `staff` todavía pendiente durante `onBranchChange('20')` confirmaría que la barrera depende de `finalize` y no de la nueva sucursal. Hoy funciona por código; esto solo lo fija como regresión.

## Límites (no los cambiaría ahora)

- **Cotización que termina sin emitir:** si `cotizarCitas` completara sin `next` ni `error`, la barrera quedaría pendiente. HttpClient no hace eso, y es el mismo riesgo de petición colgada que ya documenté. No propongo un temporizador.
- **API nula en el primer render:** el calendario no reintenta hasta que cambie `selectedDate`, porque la API no es una señal. Es aceptable porque `afterRenderEffect` corre después de `ngAfterViewInit`. La prueba de `:41-48` documenta ese comportamiento.
- **Formateador local duplicado:** la fecha local en `booking-calendar.component.ts:23` duplica `formatearFechaLocal` del padre. Es aceptable por aislamiento del chunk.
- **Paso B:** sigue condicionado a la traza a 820px. No apruebo métricas, suites ni builds por adelantado. No abro M3, hosting ni cambios de negocio.
