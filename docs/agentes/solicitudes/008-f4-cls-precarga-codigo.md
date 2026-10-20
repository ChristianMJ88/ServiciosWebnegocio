# Revisión de paso A y calendario

Claude, solo lectura. Contrasta tu `revisiones/008-f4-cls-precarga-diseno.md` con booking.component.ts/.html/.spec.ts, booking-calendar.component.ts y nuevo spec; no ejecutar comandos ni tocar código. No hay Lighthouse en curso.

Implementado paso A: set privado de peticiones iniciales de staff, pending de cotización activado antes de selectedServices en la hidratación única, limpieza final de staff/salida temprana o respuesta vigente de quote. Computed presentacionInicialLista sustituye solo presentación del shell/estado. No cambia llamadas, payloads ni reglas de backend; cargar días sigue corriendo. No se hizo paso B todavía, condicionado a medición.

Calendario cambia effect por afterRenderEffect fase write, API nula protegida, comparación con año/mes/día locales antes de gotoDate. No usa UTC ni temporizadores nuevos, no cambia defer. Test doble reproduce contrato de FullCalendar: API nula hasta ngAfterViewInit. **Ambas regresiones fallaron antes con el TypeError real**, log privado `/private/tmp/fluora-cls-calendar-before.log`. Ahora se prueban primera fecha, cambios posteriores, nulabilidad y día local ya seleccionado. Pruebas focales actuales en curso; no afirmar verdes aún.

Revisa señales unidireccionales, obsolescencia, errores y que una actualización posterior no vuelva a ocultar. El spec original y asserts de precios/anticipo/reservas permanecen. La prueba de DOM real añadida antes sigue comprobando mismo formulario/estado fuera/inert. Si hace falta una aserción DOM adicional para la espera de cotización, indícala sin pedir rediseño de calendario.

No aprobar métricas ni suites futuras. Devolver hallazgos concretos necesarios y límites, brevemente. No abrir M3/hosting ni cambios de negocio.
