# Diseño: último residual de precarga y calendario nulo

Claude, solo lectura, no ejecutar comandos. Usuario priorizó corregir CLS del tenant antes de M3. No tocar backend, precios, permisos, horarios, reservas, pagos, negocio ni commit. Todas las mediciones terminaron; no hay Lighthouse simultáneo.

Resultado artefacto5c793… (App/Header + catálogo inicial): 24 muestras de Inicio/Servicios/Agendar sin parámetros/Checkout, tres tamaños frío/caliente, máximoCLS.000469845. Suite290/82 y tresbuilds verdes. Diseño anterior pidió medir aparte una URL con servicio precargado antes de ampliar la espera.

Caso informativo real `/e/nail-art/agendar?sucursal=1&servicios=7`, solo cotizar200 sin reservar/cobrar: CLS390=.036931/.037400,820=.093535/.086128,1440=.031101/.032840. Lighthouse identifica `booking-service-browser`, empujado298.73px al llegar profesionales/cotización; a820 otro movimiento83.14px. No usar esos números para reabrir el salto App/Header ni para atribuirlo a M1/M2.

**Fallo adicional concreto:** consola en820yvarias1440: `TypeError: Cannot read properties of null (reading 'gotoDate')`. `booking-calendar.component.ts` usa effect → viewChild(FullCalendarComponent) → getApi().gotoDate. La instancia Angular existe antes de que FullCalendar asigne su Calendar en ngAfterViewInit. Fuente local primaria: node_modules/@fullcalendar/angular/fesm2020/fullcalendar-angular.mjs:ngAfterViewInit asigna Calendar sincrónicamente; getApi retorna calendar, que al principio esnull.

Lee solo booking.component.ts (.cargarStaffServicio, cotizarReserva/quoteVersion, hidratarServiciosDesdeQuery, señalcatalogoInicialListo, efectos), plantilla, booking-calendar.component.ts y specs pertinentes. Si necesitas rectángulos, resumen legible en `/private/tmp/fluora-cls/final-analysis.log`; evita leer traces gigantes. No ampliar consultas ni introducir reglas de cobro.

Propón corrección técnica mínima, con pruebas:
1. Calendario: usar afterRenderEffect y defensa API nula, sin perder la primera fecha ni fechas posteriores. ¿Basta el orden de ngAfterViewInit? No temporizador arbitrario, mantener chunk lazy.
2. Precarga: resolver ahora el residual de primera presentación esperando únicamente primeras respuestas de profesionales y cotización pertinentes, sin duplicar reglas de backend ni esconder otra vez al usuario en cambios posteriores. ¿Una barrera inicial derivada de servicioIDs vigentes + staffByService/errores y cotizacion/errorCotizacion puede complementar catalogoInicialListo, con latch de presentación? Evitar que el effect no haya arrancado y parezca ya completo. No esperar indefinidamente en errores, no colgar si no hay precarga y no interferir con carga de días/slots. Si no basta, indicar la causa exacta y alcance mínimo seguro.

El flag catálogo ya aprobado no se reinicia en cambios posteriores. La espera inicial no debe mostrar precios inventados, bloquear acciones permitidas por negocio ni afectar número/parámetros de llamadas existentes. Hero visible, formulario vivo/inert durante espera; estado accesible. Pruebas Subject reales para cotización/staff retrasados/error/obsolescencia, sin alturas fijas. No optimizar más HomeMarketing ni avanzar M3.

Devuelve diseño acotado, riesgos y condiciones de cierre, sin aprobar métricas futuras. Codex implementa.
