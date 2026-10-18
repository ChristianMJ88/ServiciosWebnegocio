# Revisión 010: implementación completa en borrador

Autor: Claude Code (arquitectura y revisión, solo lectura), 2026-10-17. Esta sesión no tiene herramientas de escritura, así que esta respuesta no se guardó en ningún archivo; corresponde a `revisiones/010-implementacion.md`.

**Qué revisé:**
- El flujo Point: `ServicioMercadoPagoPoint`, `ClienteMercadoPagoPoint`, `ErrorProveedorPoint` y `PagosPrevistosPoint`.
- Caja: `ServicioVentasCaja`, `PlanPagosVenta`, `ServicioSaldoCitas` y `ServicioCaja` (cierre y resumen).
- Los repositorios de orden, sesión, detalle y pago.
- V139 y su prueba MySQL, el bot (`crearCitaBot`) y `app-caja-cobro`.

**Límites:**
- No ejecuté pruebas ni `git diff`. Por eso no confirmo si los hallazgos marcados como «posiblemente previo» ya existían antes de 010.
- No revisé las plantillas de Cobros y Punto de venta línea por línea.

## Veredicto
- **V139 y el DTO cumplen 010-migracion (D1–D6).** Lo detallo al final.
- B1 está bien corregido y no encontré inversión en el orden de bloqueos.
- **Hay un bloqueante (A1) y cuatro medios (A2–A5) que corregir antes de cerrar las suites.**

---

## A1 · Alta: `REQUIERE_REVISION` y `CREANDO` sin respuesta del proveedor no tienen salida, y dejan bloqueada la caja de la sucursal

**Evidencia**
- Al pasar de 20 minutos se marca `REQUIERE_REVISION` (`ServicioMercadoPagoPoint.java:156-163`). Después, `consultar` ya no reenvía, porque solo reenvía en `CREANDO` (`:206-207`).
- `cancelar` exige `proveedorOrdenId` (`:216`).
- El webhook busca la orden por `proveedorOrdenId` (`:242`), que en estos casos es NULL. Si el cliente pagó, el pago nunca se acredita.
- Ese estado cuenta como «en curso» en tres lugares:
  - la terminal (`OrdenPointCajaRepositorio.java:13`), que bloquea `crearOrden:138`;
  - el saldo apartado (`VentaDetalleRepositorio.java:21`), que hace que las citas respondan 409;
  - el corte (`CajaSesionRepositorio.java:24`), que hace que `cerrarSesion` responda 409 (`ServicioCaja.java:180`).
- En la UI, `esperando` lo trata como espera y el formulario queda bloqueado (`caja-cobro.component.ts:44`). El sondeo se detiene (`caja-payments-section.component.ts:185`, `caja-sales-section.component.ts:196`).

**Impacto**
- Basta un timeout de red, o que el cajero cierre la pestaña antes de que alguien consulte la orden, para que la terminal, las citas y **el cierre de caja** queden bloqueados para siempre.
- Ninguna acción lo resuelve, ni en la API ni en la UI.

**Corrección**
1. Agregar una acción de resolución explícita con un permiso existente (sin crear permisos nuevos), auditada con usuario, momento UTC y motivo. Tiene dos salidas:
   - **«No se cobró»:** la orden pasa a `CANCELADO` y se llama `ventas.cancelarPoint`, que libera el saldo y el inventario. Las partes previstas no se registran.
   - **«Sí se cobró»:** se captura el ID de orden de Mercado Pago, se consulta con `cliente.consultar` y se valida que `external_reference == referencia(publicId)` y que el importe coincide. Después se aplica `aplicarEstado` con el flujo normal.
2. Opcional: si Mercado Pago permite buscar órdenes por `external_reference`, usarlo antes de pedir la resolución manual. Esto hay que **verificarlo en la documentación**; no lo doy por hecho.
3. En la UI, `REQUIERE_REVISION` debe mostrar la acción de resolución, no la vista de espera.

**Pruebas**
- Orden en `CREANDO` de más de 20 minutos: consultar → `REQUIERE_REVISION`; resolver «no cobrado» → pagos directos 200, cierre de caja 200 y terminal libre.
- Resolver «cobrado» con una referencia que no corresponde → 409 sin cambios.
- Usuario de otra sucursal → 403.

## A2 · Media: un reembolso hecho desde el panel de Mercado Pago sobre un pago dividido descuadra el corte y vuelve a abrir el saldo completo

**Evidencia**
- La guarda de `reembolsar:225` solo cubre la acción iniciada desde la app.
- El webhook con `REEMBOLSADO` llama a `marcarReembolsadaPoint` sin mirar las partes previstas (`:258`). Así la venta completa pasa a `REEMBOLSADA`.
- `findPagosVigentesDeSesion` filtra por venta `PAGADA` (`PagoVentaRepositorio.java:17`). Los $300 en efectivo desaparecen de los totales por método, pero el movimiento `INGRESO_VENTA` sigue sumando en el saldo esperado.
- `sumarServiciosPagadosPorCitas` también solo cuenta `PAGADA`, así que la cita vuelve a deber $700 cuando solo se reembolsaron $400.

**Corrección**
- Con partes previstas y estado `REEMBOLSADO`: la orden pasa a `REEMBOLSADO`, la venta **no cambia de estado** y se llama `marcarRevisionPoint` con un log `warn`. La reversión completa queda para 007-2.

**Prueba**
- 700 = 300 en efectivo + 400 Point aprobado, seguido de un webhook de reembolso: venta `PAGADA` con observación de revisión, corte con $300 en efectivo y saldo de la cita en 0.

## A3 · Media: una aprobación tardía hace fallar T2 en bucle o acredita la orden equivocada

**Evidencia**
- `aplicarEstado:253` solo protege `APROBADO` y `REEMBOLSADO`. Una orden local en `CANCELADO`, `RECHAZADO` o `EXPIRADO` puede pasar a `APROBADO` y llamar a `confirmarPoint`.
- `confirmarPoint:267` lanza 409 si la venta no está en `PENDIENTE_PAGO`. Eso revierte T2, la orden nunca queda `APROBADO`, el webhook falla en cada reintento y el cargo a la tarjeta queda sin registrar.
- Caso Staff: la orden A se cancela, la venta vuelve a `POR_COBRAR` (`:311`) y se reenvía como orden B. Si A se aprueba tarde, `confirmarPoint` acredita el importe y las partes previstas **de A**. Cuando B se aprueba, `:266` sale temprano porque la venta «ya tiene un pago Point», y el cargo de B no se registra.

**Corrección**
- Pasar la orden a `confirmarPoint`.
- La idempotencia debe basarse en la referencia del pago del proveedor (`proveedorPagoId`) y no en «existe algún pago Point».
- Si la venta no está en `PENDIENTE_PAGO`, o la orden no es la orden vigente de la venta: la orden queda `APROBADO`, **no** se registran `pago_venta` ni partes previstas, se marca para revisión con log y se termina sin excepción para que el webhook reciba 200.

**Pruebas**
- `CANCELADO` → `APROBADO` tardío: 200, sin pagos y con revisión.
- Staff A cancelada, B en curso y A aprobada tarde: B sigue pendiente y A queda en revisión.

## A4 · Media: falta el alcance por sucursal en `consultar`, `cancelar`, `reembolsar` y `simular`, y un GET ya puede reenviar el cobro (posiblemente previo)

**Evidencia**
- `buscar` filtra solo por empresa (`ServicioMercadoPagoPoint.java:293`).
- El controlador no pasa `sucursalesPermitidas` en estos endpoints (`ControladorMercadoPagoPoint.java:57-79`).
- Con 010, `GET /ordenes/{id}` con `CAJA_ACCESO`, que es un permiso de lectura, puede reenviar la orden a la terminal y cambiar su estado (`:206`, `:156-163`).

**Corrección**
- Validar la sucursal de la terminal de la orden contra `sucursalesPermitidas` en los cuatro endpoints.
- Mover el reenvío a un `POST .../reintentar` con `CAJA_COBRAR`, o exigir ese permiso para reenviar. El GET debe quedar en lectura y consulta remota.

**Pruebas**
- Usuario de la sucursal B con una orden de la A → 403 en los cuatro.
- GET con solo `CAJA_ACCESO` sobre una orden en `CREANDO` → no llama a `cliente.crearOrden`.

## A5 · Media: «V139 no aplicada a la base habitual» no está garantizado

**Evidencia**
- `application.yml:14` tiene `ddl-auto: update` y `:21` tiene `flyway.enabled: true`.
- Si el backend local se reinicia con el código nuevo:
  - con Flyway activo, V139 se aplica sola;
  - con migraciones deshabilitadas (como en las sesiones anteriores), Hibernate crea `pagos_previstos_json`, `monto_recibido` y `cambio` sin registrarlo en `flyway_schema_history`, y la siguiente ejecución de Flyway fallará con «Duplicate column».
- La propia prueba contempla que las columnas ya existan en el origen y las elimina en la copia (`MigracionPointDivididoMysqlTest.java:19`).

**Validación (solo lectura)**
- Consultar en la base habitual `information_schema.COLUMNS` para esas tres columnas y la versión máxima de `flyway_schema_history`, y registrar el resultado en `respuestas/010.md`.
- Si las columnas existen, **no corregir sin autorización del usuario**: documentarlo y proponer opciones.
- Hasta cerrar 010, levantar el backend habitual con `ddl-auto: validate` o `none`.

---

## Bajas
- **B1.** `requiereReferencia` solo se aplica en la UI (`caja-cobro.component.ts:53`). `PlanPagosVenta.java:39` no lo exige. Debe validarse en `resolver`, según el PROTOCOLO.
- **B2.** `mapear(orden)` usa la lectura estricta de `previstos.leer` (`:279`). Un JSON inválido rompe `listarOrdenesCaja` de toda la sucursal. Para mostrar conviene una lectura tolerante, que devuelva `efectivoPrevisto=null` y una marca; la confirmación sigue siendo estricta.
- **B3.** `ManejadorConcurrenciaCaja` está limitado a `caja.api`. Si T1 de Point no obtiene un bloqueo, responde 500. Hay que extenderlo al POST de creación Point, no al webhook.
- **B4.** `cerrarSesion` (`ServicioCaja.java:171`) usa RR. Hoy funciona porque la primera lectura es la de bloqueo, pero es frágil. Conviene anotarla con READ_COMMITTED. Las ventas y pagos sin Point no bloquean la sesión: es una carrera previa con el corte; documentarla.
- **B5.** Reutilizar una `Idempotency-Key` con otro cuerpo devuelve en silencio la orden anterior (`:133-136`). Conviene comparar la venta o el importe y responder 409.

## Verificado sin hallazgos
- **D1:** `updatable=false` y `columnDefinition=json` (`OrdenPointCajaEntidad.java:22`). Formato `v:1` con importes como cadenas y como máximo 2 partes (`PagosPrevistosPoint.java:19-27`). Referencia de 120 caracteres o menos (`PlanPagosVenta.java:40`).
- **D2:** `orden.importe` más las partes previstas, y si no cuadra se registra solo Point (`ServicioVentasCaja.java:273-282`).
- **D3:** bloqueo de la sesión en T1 (`:169`, `:408`) y 409 en el cierre.
- **D4:** recibido ≥ monto y cambio calculado (`PlanPagosVenta.java:35-37`).
- **D5/D6:** V139 solo agrega columnas.
- **B1 (de 010-migracion):** error tipado; solo cancela un 4xx definitivo en el primer envío (`:175`).
- **Límite de reenvío:** `Clock` UTC (`ConfiguracionReloj.java:11`) con sesión JDBC en UTC; el cálculo es consistente.
- **Orden de bloqueo:** terminal → (venta Staff) → sesión → existencias → citas. El webhook de checkout usa existencias → prestadores → citas. `confirmarPoint` usa orden → venta → citas y `cancelarPoint` orden → venta → existencias. No hay ciclos.
- **Bot:** `ControladorWhatsappTwilio` no es transaccional, así que capturar el 409 de `crearCitaBot` no deja la transacción marcada para revertir.
- **Historial de cita:** se agrupa por detalle, con `MIXTO` (`PagoVentaRepositorio.java:32-38`).

## Antes de cerrar
Corregir A1–A4, registrar la verificación de A5 y luego correr las suites completas: backend con MySQL sin omitir (incluida `MigracionPointDivididoMysqlTest`) y Angular. Después, `git diff --check` y la revisión visual del estado `REQUIERE_REVISION` en los tres tamaños.
