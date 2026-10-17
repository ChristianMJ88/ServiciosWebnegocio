# Revisión 006 fase 1: aprobada con dos condiciones (M1 y M2)

La base cumple: el listener es síncrono y `MANDATORY`, el evento se publica en la misma transacción que el cambio y no hay `AFTER_COMMIT` ni `@Async`. Los canales y los tipos del outbox coinciden con el inventario y las guardas idempotentes siguen igual. Antes del commit hay que corregir M1 y decidir M2.

**Cómo la hice:** solo con Read, Glob y Grep. No pude ejecutar `git diff` porque no tengo Bash, así que comparé los archivos actuales y no la versión anterior. No certifico pruebas, como pediste. No escribí ninguna revisión porque no tengo herramienta de escritura. Si quieres conservar este texto, Codex puede guardarlo en `revisiones/006-fase1.md`; no toques `006.md`.

## Hallazgos

### M1 — Media: la resolución administrativa de incidencias se registra como WEBHOOK
- **Dónde:** `ServicioResolucionIncidenciasCheckout.java:87` llama a `citas.confirmar(empresa, orden)`, y esa ruta publica `CitaConfirmada` con `WEBHOOK` y sin actor (`ServicioConfirmacionCitasCheckout.java:62`).
- **Problema:** quien confirma es un administrador autenticado. El hecho queda mal atribuido justo antes de la 007, que usará `origen`/`actorId` para auditoría.
- **Respuesta a tu pregunta:** sí, necesita origen y actor propios.
- **Corrección:** agregar la sobrecarga `confirmar(Long empresaId, Long ordenId, OrigenEventoCita origen, Long actorId)`.
  - La versión de 2 argumentos delega con `WEBHOOK, null`, y `ServicioEstadoPagoCheckout` no cambia.
  - El resolutor pasa `ADMIN, usuario.usuarioId()`.
  - El listener no distingue el origen en `confirmada`, así que las notificaciones no cambian.
  - `PagoCitaRegistrado`, que se emite desde `proyeccion.proyectar`, debe seguir como `WEBHOOK`, porque el dinero lo informa el proveedor.
- **Prueba esperada:** al resolver con `REPROGRAMADA` se publican N `CitaConfirmada` con `ADMIN` y el actor del usuario. El webhook sigue con `WEBHOOK` y actor nulo. El outbox no cambia: `CITA_CONFIRMADA` + `NUEVA_CITA_NEGOCIO` + WhatsApp con consentimiento.

### M2 — Media: Staff confirma sin publicar el hecho
- **Dónde:** `ServicioAgendaStaff.java:139-154`. Cuando Staff pasa una cita de PENDIENTE a CONFIRMADA, guarda estado e historial pero no publica `CitaConfirmada`.
- **Por qué no basta con agregarlo:** hoy esa ruta no envía notificaciones. Si se publica el evento tal cual, el listener empezaría a programar el correo de confirmación, y eso es un cambio funcional fuera de "sin cambio funcional".
- **Mi recomendación:** no publicarlo en la fase 1 y dejarlo documentado en la respuesta como hueco conocido.
  - La fase 2 lo detecta igual, porque su token se deriva del historial.
  - La 007 no consume `CitaConfirmada`.
  - Que Staff envíe o no el correo de confirmación lo decide el usuario. No sugiero añadir una bandera para suprimir el correo.
- **Si el usuario aprueba el correo:** publicar con `STAFF` y su actor. Prueba: un solo evento y `CITA_CONFIRMADA` en el outbox.

### B1 — Baja: el historial de la reprogramación desde Recepción dice "cliente"
- **Dónde:** `ServicioCitasCliente.java:210-211` guarda `cambiadoPorUsuarioId = clienteId` y "Reprogramacion realizada por el cliente", aunque el evento ya lleva `RECEPCION` y el actor real.
- **Corrección:** usar `actorId` y un motivo según el origen. Es una corrección de auditoría, sin contrato nuevo.
- **Prueba:** en `EventosConfirmacionReprogramacionTest`, el caso `desdeRecepcion` debe guardar historial con usuario 11.

### B2 — Baja: la prueba de arquitectura revisa una lista fija de productores
- **Dónde:** `EventosCitaArquitecturaTest.java:14-17`. Un productor nuevo fuera de esa lista podría volver a llamar directo al outbox sin que la prueba lo detecte.
- **Estado hoy:** el invariante se cumple. Con grep, solo `NotificacionesEventosCita`, `NotificacionesRegistroCita`, `ProgramadorRecordatoriosWhatsapp` y las propias clases de outbox usan `ServicioOutbox(Correo|Whatsapp)Citas`.
- **Corrección:** invertir la prueba. Recorrer todo `src/main` y aceptar esas clases solo si están en una lista de permitidas.

### B3 — Baja: el origen de los cobros en Caja y Point es ambiguo
- **Caja:** se publica como `ADMIN` (`PublicadorPagosCitas.java:22`, `ServicioVentasCaja.java:242` y `:396`), aunque el cobro lo hace Caja y no el panel administrativo.
- **Point:** se publica como `WEBHOOK` sin actor (`ServicioVentasCaja.java:263`), aunque existe `venta.getCreadaPorUsuarioId()`.
- **Opciones:** agregar `CAJA` a `OrigenEventoCita` (es un enum interno, no cambia el API) o documentar que `ADMIN` significa "personal del negocio". Conviene definirlo antes de la 007.

### B4 — Baja: una consulta de cliente por cada evento con WhatsApp
- **Dónde:** `NotificacionesEventosCita.java:76`. En un grupo de checkout de N citas se hacen N consultas del mismo cliente.
- **Pendiente:** como no pude ver el diff, Codex debe confirmar si el código anterior también consultaba por cada cita. Si consultaba una sola vez por grupo, son N−1 consultas extra; es poco, pero conviene registrarlo.

## Verificado sin hallazgos
- **Atomicidad:** la prueba con JPA/H2 real cubre el rollback del estado, el historial y los dos canales, la falla del consumidor y el rechazo fuera de transacción. `confirmar` del checkout decide que es incidencia antes de publicar nada, así que no puede publicar a medias.
- **Idempotencia:**
  - Al reintentar un cierre, termina antes de publicar (`ServicioCierreCita.java:68`).
  - En el checkout se omiten las citas ya CONFIRMADA, y `liberar` solo toca las PENDIENTE_PAGO.
  - La proyección se protege con `existsBy...`, Point con el pago POINT existente y `cobrarPendiente` con el estado POR_COBRAR.
  - La administración solo publica si el estado cambió.
- **Canales:**
  - La cancelación del CLIENTE solo envía correo.
  - La cancelación del negocio envía correo y WhatsApp con consentimiento y teléfono.
  - Gracias, No asistió, reprogramada, apartado vencido e incidencia se conservan.
  - El registro inicial devuelve el booleano real a través del adaptador.
- **Venta mixta:** el monto por cita sale de `SERVICIO.total`, igual que `findPagosConciliadosPorCita`, y no incluye productos ni envío. Hay una sola consulta `IN` por venta.
- **Aislamiento:** la guarda de empresa y sucursal en `PublicadorPagosCitas.venta` puede lanzar una excepción dentro del webhook. No se dispara con datos válidos, porque `ServicioOrdenCheckout.java:201` ya exige que todas las citas sean de la misma sucursal.

## Para la 007 (sin cambiar nada ahora)
- La resolución `REPROGRAMADA` mueve horarios y solo publica `CitaConfirmada`, no `CitaReprogramada`.
- No hay evento para reembolsos (`REEMBOLSADA_FUERA_DEL_SISTEMA`, `marcarReembolsadaPoint`) ni para `cancelarPoint`. El token de la fase 2 ya los cubre mediante el estado de la venta.
- La administración puede pasar a CONFIRMADA una cita CANCELADA, FINALIZADA o NO_ASISTIO (`ServicioAdminCitas.java:1454-1475`); esto ya existía. La 007 debe considerarlo para la custodia del anticipo.

## Validación pendiente
- Suites completas en verde, con las aserciones previas sin cambios.
- Pruebas nuevas de M1 y, si aplica, de B1.
- La decisión del usuario sobre M2, registrada en la respuesta.
