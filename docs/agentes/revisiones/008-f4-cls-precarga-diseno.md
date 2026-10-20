# Diseño 008-F4: residual de precarga y calendario nulo

Solo leí archivos, sin ejecutar comandos. Revisé `booking-calendar.component.ts`, `booking.component.ts` (líneas 80–151, 443–548, 876–932, 1023–1036, 1105–1126), la plantilla (líneas 26–155 y 188–191), la parte de `booking.component.spec.ts` que usa `catalogoInicialListo`/`Subject`, `fullcalendar-angular.mjs` y `final-analysis.log`.

## Hallazgos que explican el problema

- **H1. La barrera actual se abre antes de que exista la cotización (P1).** En `booking.component.ts:919-923`, `hidratarServiciosDesdeQuery()` asigna `selectedServices` y en el mismo bloque se pone `catalogoInicialListo` en `true`. El effect `actualizarCotizacion` (`:144`) corre después. En ese intervalo `cotizando()` es `false` y `cotizacion()` es `null`, así que **cualquier barrera basada en `cotizando()` parecería completa**. Por eso hace falta un indicador que se active al hidratar.
- **H2. No hay estado por servicio para el personal (P1).** En `:1023-1036`, `cargarStaffServicio` puede salir sin hacer la petición (`:1026`) o descartar la respuesta (`:1029`), y el error escribe un solo `errorStaff` para todos los servicios. Una barrera basada en `staffByService` podría quedarse esperando para siempre en esos casos.
- **H3. Segunda cotización automática, causa probable del segundo salto (P2).** Al cargar los días, `:523` elige `onDateSelect(dias[0])`. Eso cambia `selectedDate` y lanza otra cotización. `cotizarReserva` (`:446`) pone `cotizacion` en `null` y la nota de `:155` desaparece y luego vuelve a aparecer. El log lo respalda: cada muestra de precarga registra `quotes:[200,200]`. El log no incluye rectángulos, así que no puedo confirmar que sea el movimiento de 83.14px a 820px. Lo trato en el paso B, condicionado a medición.
- **H4. Calendario (P1).** `FullCalendarComponent.calendar` empieza en `null` (`fullcalendar-angular.mjs:213`) y se asigna en `ngAfterViewInit` (`:235`). Por su parte, `getApi()` devuelve `this.calendar` (`:321`). Un `effect` normal puede ver el `viewChild` ya resuelto antes de que el hijo pase por `ngAfterViewInit`.

## 1. Calendario (`booking-calendar.component.ts`)

```ts
afterRenderEffect({ write: () => {
  const fecha = this.selectedDate();
  const api = this.calendar()?.getApi();
  if (!fecha || !api) return;
  if (api.getDate().toISOString().slice(0, 10) !== fecha) api.gotoDate(fecha);
}});
```

- **¿Basta el orden de `ngAfterViewInit`?** Sí. `afterRenderEffect` corre cuando ya terminó la detección de cambios del tick, y los `ngAfterViewInit` (incluido el de FullCalendar) ya se ejecutaron. La comprobación de API nula es solo defensa y no debería activarse.
- **Primera fecha:** el effect se registra al crearse el componente y su primera ejecución ocurre después del primer render, leyendo `selectedDate()` en ese momento. Así no se pierde una fecha elegida antes de que cargue el bloque `@defer`. Las fechas posteriores vuelven a ejecutarlo porque la señal se rastrea.
- **Comparar la fecha antes de llamar `gotoDate`:** evita navegar de nuevo en cada render. La comparación debe usar la representación local que ya usa el componente, no UTC a ciegas. Codex puede usar el mismo formateador del padre o comparar contra `api.view.currentStart`.
- Sin `setTimeout`. No cambia el `@defer` ni el import, así que el chunk sigue siendo lazy.

## 2. Barrera de precarga, paso A (cierra H1 y H2)

En `booking.component.ts`:

```ts
private readonly staffInicialPendiente = signal<ReadonlySet<number>>(new Set());
private readonly cotizacionInicialPendiente = signal(false);
readonly presentacionInicialLista = computed(() =>
  this.catalogoInicialListo() && !this.cotizacionInicialPendiente() && this.staffInicialPendiente().size === 0);
```

- **Hidratación (`:1121-1124`):** solo si `services.length > 0`, poner `cotizacionInicialPendiente` en `true` y llamar `cargarStaffServicio(s, true)`. Si no hay precarga (sin `servicios` y sin borrador), no se activa nada y la barrera equivale a `catalogoInicialListo`. El borrador del carrito pasa por la misma ruta, y es correcto que la active.
- **`cargarStaffServicio(service, inicial = false)`:** si `inicial` y se emite la petición, agregar el id al set. Luego `.pipe(takeUntilDestroyed(...), finalize(() => inicial && quitar(service.id)))`. `finalize` cubre éxito, error, respuesta obsoleta (`:1029`) y destrucción. Si sale en `:1026`, el id no se agrega y no hay bloqueo. No cambia el número de llamadas.
- **`cotizarReserva`:**
  - En la salida temprana (`:449`), poner `cotizacionInicialPendiente` en `false`.
  - En `next` y `error`, después de comprobar `version === this.quoteVersion`, también ponerlo en `false`.
  - Una respuesta obsoleta no libera la barrera; la libera la versión vigente o una salida temprana (por ejemplo, cambio de sucursal). La espera queda acotada a la última petición vigente y no copia ninguna regla del backend.
- **Sin latch extra:** ambas señales solo pueden pasar de pendiente a resuelto, y solo la hidratación (protegida por `queryHydrated`) las activa. Una vez `true`, `presentacionInicialLista` no vuelve a `false`. `addSelectedService` usa `inicial = false`, y las cotizaciones posteriores no tocan la barrera. `catalogoInicialListo` no cambia.
- **Plantilla (`:26`, `:29`):** cambiar las tres referencias de la tarjeta (`inert`, `aria-busy`, la clase `--pending`) a `presentacionInicialLista()`. En el `role="status"`, mostrar "Cargando servicios disponibles…" mientras `!catalogoInicialListo()`, luego "Preparando tu selección…" y después vacío. El hero queda fuera de la tarjeta y sigue visible.
- **No se toca** `cargarDiasSeleccion`, el flujo de slots ni el auto-`onDateSelect`. Siguen corriendo mientras la tarjeta está `inert`, porque `inert` solo bloquea la interacción.
- **Errores:** el error de personal o de cotización libera la barrera, y la alerta con "Reintentar cotización" (`:66`) queda usable al mostrarse la tarjeta. No hay precio inventado: hasta que la cotización vigente llegue, se muestra lo mismo que ahora.

## 3. Paso B, condicional (H3)

Solo si, después del paso A, la traza a 820px sigue mostrando un movimiento atribuible a la nota de `:155` o a los avisos de `:60-65`:

- Hacer que `:155` sea `@if (cotizacion(); as p) {…} @else if (cotizando()) { <p class="booking-inline-note" role="status">Confirmando condiciones de tu reserva…</p> }`. Así se mantiene el contenedor sin mostrar montos viejos ni inventados.
- No conservar `cotizacion` anterior durante la recotización: el botón de envío (`:590`) ya bloquea con `cotizando()`, pero mostrar un anticipo anterior podría engañar al cliente.
- Si el salto restante viene de otro nodo, se documenta y se deja fuera de alcance. No se extiende la barrera a días ni slots.

## Pruebas (con `Subject` reales, sin alturas fijas)

1. Servicios con `of()` y cotización/personal en `Subject` pendientes. Justo después de emitir servicios, **antes** de que corran los effects: `catalogoInicialListo` es `true`, `presentacionInicialLista` es `false` y `inert` está presente. Esto cubre el caso del effect que todavía no arrancó.
2. Llega el personal y la cotización sigue pendiente: la barrera sigue en `false`. Llega la cotización: `true` y `inert` desaparece. Repetir con el orden inverso.
3. Error de personal y error de cotización: la barrera se libera, aparecen la alerta y el botón de reintentar, y el botón responde.
4. Obsolescencia: la primera cotización pendiente queda reemplazada por una nueva (fecha automática o cambio de sucursal). La emisión vieja no libera la barrera; la nueva respuesta o la salida temprana sí.
5. Sin precarga: la barrera es `true` en cuanto el catálogo está listo, sin llamadas a personal ni cotización.
6. Después de mostrarse: `addSelectedService` con personal pendiente y una cotización nueva pendiente mantienen `presentacionInicialLista()` en `true` y sin `inert`.
7. Conteo de llamadas: `getPublicStaff` una vez por servicio. `cotizarCitas` y `getAvailableDays` con el mismo número y los mismos argumentos que antes. `getAvailableDays` se llama mientras la barrera sigue pendiente.
8. Calendario (nuevo `booking-calendar.component.spec.ts`):
   - Con fecha asignada antes de crear el componente, `getApi().getDate()` termina en esa fecha.
   - Un cambio posterior navega a la nueva fecha.
   - Con `getApi` espiado para devolver `null`, no hay excepción, y al restaurarlo y cambiar la fecha sí navega.
   - El espía de `console.error` no registra nada.
9. Si se hace el paso B: mientras se recotiza se muestra el texto neutral y nunca un importe.

## Riesgos

- **Petición colgada:** si una petición nunca responde, la tarjeta queda `inert`. Es el mismo riesgo que ya tiene `catalogoInicialListo`. No propongo un timer arbitrario. Si se quiere una cota, debe venir de una política HTTP común, no de esta pantalla.
- **Más tiempo sin interactuar con precarga:** se suma la latencia de personal y cotización, que corren en paralelo. El LCP es el h1 del hero, así que no debería cambiar, pero conviene vigilar el INP y el TBT.
- **`finalize` y `takeUntilDestroyed`:** liberar la barrera al destruir el componente no tiene efectos visibles.
- **Comparación de fechas:** si se hace en UTC, pueden aparecer navegaciones de más o de menos alrededor de la medianoche.

## Condiciones de cierre (no apruebo métricas por adelantado)

- Las suites y los tres builds en verde, y el chunk del calendario sigue separado.
- Las pruebas 1–8 pasan (y la 9 si se hace el paso B).
- Sin `TypeError` de `gotoDate` en consola en ninguna de las 6 muestras de precarga.
- Las 24 muestras sin parámetros no empeoran respecto a 5c793…
- En la traza de precarga, `booking-service-browser` ya no se desplaza con la llegada de profesionales y la cotización inicial. Cualquier residual se reporta con el rectángulo y el nodo que lo causa, sin atribuirlo a App/Header, M1 ni M2.
- Sin cambios de backend, reglas de cobro, consultas o parámetros, ni HomeMarketing o M3.

Codex puede guardar esto como `docs/agentes/revisiones/008-f4-cls-precarga-diseno.md` si lo necesita en el canal.
