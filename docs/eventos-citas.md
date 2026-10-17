# Eventos internos de citas — 006 fase 1

Los servicios mantienen las decisiones de negocio, RBAC, bloqueos por tenant/sucursal y las transacciones existentes. Después de persistir el hecho publican records con `ApplicationEventPublisher`. `DatosEventoCita` contiene identificadores de empresa/sucursal/cita/cliente, origen operativo, actor, momento UTC (`Instant` del Clock compartido) e inicio/fin en la hora de agenda. No contiene entidades JPA mutables ni credenciales.

`NotificacionesEventosCita` consume con `@EventListener` síncrono y propagación `MANDATORY`. Solo programa el outbox; no envía al proveedor. Cambio, historial y notificación se confirman o revierten juntos. No configurar un multicaster asincrónico, `@Async` o `AFTER_COMMIT` para estos consumidores. Un futuro consumidor financiero de la 007 también debe ser síncrono y transaccional.

| Hecho | Productores | Reacción de notificación conservada |
|---|---|---|
| CitaFinalizada | Cierre común: Administración, Recepción, Staff | Gracias por correo y WhatsApp con consentimiento/teléfono |
| CitaNoAsistio | Cierre común: las tres rutas | Correo No asistió |
| CitaCancelada | Cancelación común del negocio y cancelación cliente | Negocio: correo y WhatsApp; cliente: solo su correo |
| CitaConfirmada | Administración, Recepción/check-in, cliente y checkout | Mismos canales previos; checkout avisa también al negocio |
| CitaReprogramada | Cliente y Recepción | Correo y solicitud de confirmación por WhatsApp |
| PagoCitaRegistrado | Caja, venta mixta, Point y venta online | Sin avisos nuevos; punto de extensión |
| CitaApartadoVencido | Liberación de checkout | Correo de apartado vencido |
| PagoCitaConIncidencia | Checkout aprobado con incidencia | Correo de incidencia al negocio |

La confirmación de Staff conserva su comportamiento anterior: estado e historial sin avisos ni evento en esta fase. Agregar correo a esa ruta requiere autorización de negocio. Finalizar y marcar No asistió desde Staff sí usan el cierre común. La reprogramación de una incidencia administrativa conserva la confirmación del grupo y sus avisos existentes; no añade un aviso de reprogramación pendiente.

El registro inicial usa `NotificacionesRegistroCita`, un adaptador transaccional que devuelve el acuse booleano real del outbox al contrato público. No se lo confunde con una cita confirmada ni se usa un evento mutable para devolver resultados. Se conservan las sobrecargas y la cantidad de servicios del WhatsApp agrupado.

`PublicadorPagosCitas` informa solo el importe del servicio acreditado a cada cita. En ventas agrupa los detalles SERVICIO por cita y carga sus snapshots con un único IN por tenant, verificando también la sucursal. Productos y envío no forman parte del importe del evento. Las guardas existentes de pago/venta evitan repetir la publicación al reprocesar un cierre, una proyección o la aprobación Point.

El origen es operativo, no un rol ni una regla de acceso: `ADMIN` incluye el cobro del personal en Caja; `WEBHOOK` identifica la confirmación del proveedor, con actor nulo. El iniciador Point permanece en la venta enlazada. La confirmación de una incidencia resuelta por un administrador lleva `ADMIN` y su actor real. Ninguna de estas etiquetas concede permisos.

Los recordatorios programados permanecen en su programador. Los tipos, payloads, deduplicación y configuración por tenant del outbox no cambian. El listener conserva las consultas de consentimiento por cita que ya existían; no introduce una caché compartida entre tenants.

No hay actualización automática del navegador en esta fase. El token de versión, polling visible cada 20 segundos y sus pruebas pertenecen a 006 fase 2, después de 007. Tampoco se implementan custodia, condonación, SSE, migraciones o contratos nuevos.
