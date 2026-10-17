# Revisión 009 y cierre final de 007-1: implementación

Autor: Claude Code, revisión de solo lectura (Read/Glob/Grep), 2026-10-17. No ejecuté compilación ni pruebas, y las suites completas siguen en curso. El working tree trae todo 007-1 y revisiones de otras tareas; aquí solo atribuyo a 009/C1–C4 lo que aparece en `solicitudes/009.md`.

**Resultado: no aprobada todavía.** Hay un hallazgo alto de concurrencia y uno medio en C4. El resto de P1, P2, P3 y C1–C3 cumple el diseño.

---

## A1 · Alta: las lecturas `FOR UPDATE` de saldo pueden provocar interbloqueos entre cobros de citas distintas

**Dónde:**
- `PagoCitaRepositorio.java:14` (`pagosActuales`).
- `VentaDetalleRepositorio.java:18` (`serviciosPagadosActuales`).
- `CitaRepositorio.java:21-26`, que corresponde a C2.
- Se usan en `ServicioCaja.java:288` (pago directo) y en `ServicioVentasCaja.java:197`.

**Por qué:**
- MySQL corre en REPEATABLE READ. No encontré otra configuración de aislamiento, y la prueba fija `hibernate.connection.isolation=4`.
- En ese modo, un `SELECT … FOR UPDATE` sobre `empresa_id=? AND cita_id IN (…)` bloquea también los huecos (gap/next-key) del índice `idx_pago_cita_empresa_cita (empresa_id, cita_id, registrado_en)` (V20:63). Con `venta_detalle` pasa lo mismo, por `idx_venta_detalle_cita` (V85:51).
- Una cita recién atendida casi nunca tiene pagos, así que su id cae en el último hueco del tenant.
- **El caso concreto:** dos cajeros cobran a la vez dos citas distintas del mismo tenant. Ambos toman el bloqueo de hueco, que es compartible. Luego cada uno inserta su `pago_cita` y espera al otro. MySQL detecta el interbloqueo y aborta uno de los dos. Eso termina en `CannotAcquireLockException` y un 500 genérico (`ManejadorGlobalExcepciones.java:110`).
- **Es una regresión.** Antes, el pago directo solo bloqueaba la fila de `cita`, y como era la primera sentencia de la transacción, la lectura normal ya veía datos actuales.
- Puede pasar algo parecido entre Caja y el webhook de checkout. El webhook toma huecos de `pago_cita` y `venta_detalle` en `acreditarAceptacion`, y Caja inserta después en esos mismos huecos.

**Por qué las pruebas no lo detectan:** `CobrosConcurrentesMysqlTest` hace competir solo dos cobros sobre **la misma** cita, que ya quedan en serie por el bloqueo de esa cita. Además, el esquema lo genera Hibernate (`:47`, `create-drop`), sin los índices de Flyway, así que los bloqueos no se comportan como en producción.

**Recomendación:**
1. Que la frescura de la lectura dependa del bloqueo de la cita, no de bloquear las filas de pagos. Declarar `@Transactional(isolation = Isolation.READ_COMMITTED)` en `ServicioCaja.registrarPago`, `ServicioVentasCaja.registrar` y `prepararPoint`, y que `calcularBajoBloqueo` vuelva a lecturas normales sin `FOR UPDATE`.
   - En READ COMMITTED, cada SELECT ve lo último confirmado y no se toman bloqueos de hueco.
   - Esto requiere que todo escritor de `pago_cita` y de líneas `SERVICIO` en ventas `PAGADA` bloquee antes la cita. Hay que documentarlo como inventario (ver O2: `confirmarPoint` no la bloquea).
2. En C2, el webhook no cambia de aislamiento en esta entrega. Puede conservar sus lecturas con bloqueo: si Caja ya no tiene huecos tomados, el ciclo desaparece y Caja solo espera.
3. Si se prefiere mantener `FOR UPDATE`, hace falta reintentar ante interbloqueo en esas tres entradas, con prueba. Es una opción menos recomendable.

**Prueba esperada (MySQL):**
- Crear explícitamente los índices de V20:63 y V85:51, o usar un esquema con Flyway.
- Dos citas del mismo tenant sin pagos. Dos hilos con barrera entre la lectura del saldo y el insert:
  - pago directo contra pago directo;
  - venta contra pago directo.
- Resultado esperado: ambos 200, sin `CannotAcquireLockException`.
- Conservar la prueba actual con la misma cita, que debe dar 200 y 409.

## M1 · Media: C4 prueba un contrato de error que el backend no usa

**Dónde:**
- `admin-policy.component.ts:25` lee `e?.error?.message || e?.error?.detail`.
- El backend responde siempre `ErrorApi{codigo, mensaje, fechaHora}` (`ErrorApi.java:5-8`, `ManejadorGlobalExcepciones.java:78-85`), sin campo `message`.
- La prueba simula `{error:{message:…}}` (`admin-policy.component.spec.ts:8`). Pasa, pero un 409 real muestra el texto genérico.

**Corrección:** mi revisión de cierre de 007-1 (C4: «no hace falta cambiar código») estaba equivocada.

**Recomendación:** leer primero `e?.error?.mensaje`, como ya hacen `appointment.service.ts:209-214` y `caja-sales-section.component.ts:174`, y dejar `message`/`detail` como respaldo. La prueba debe usar la forma real: `{status:409, error:{codigo:'ERROR_NEGOCIO', mensaje:'…'}}`.

## B1 · Baja: Point en Cobros con varias citas marcadas

En `caja-payments-section.component.html:93`, «Enviar a Point» sigue habilitado cuando `cobroMultiple` es verdadero.

- El backend lo rechaza bien: 400 con mensaje claro (`ServicioVentasCaja.java:145`).
- Pero el manejador (`.ts:133`) lee `detail`/`message`, así que el usuario solo ve «No fue posible enviar el cobro a la terminal».

**Recomendación:** desactivar el botón cuando `cobroMultiple` sea verdadero, con la nota «Point admite una sola cita», y leer `mensaje`. Añadir una prueba de componente.

## B2 · Baja: «Pagada · por cerrar» aparece en servicios sin costo

`SeccionAgendaCita.java:11` clasifica como FINALIZADA cualquier check-in con pendiente 0, lo que incluye `SIN_COSTO`. Eso coincide con la decisión del usuario. La etiqueta, en cambio (`recepcion-agenda-section.component.html:35`), dice «Pagada» también cuando el precio es 0.

**Recomendación:** condicionarla además a `cita.estadoCobro === 'PAGADA'`, que es un valor del backend, y usar otra etiqueta, como «Atendida · por cerrar», para `SIN_COSTO`. También conviene confirmar con el usuario que un servicio gratuito pase a Finalizadas en cuanto se hace el check-in.

## B3 · Baja: pruebas que faltan

- P1: no encontré la prueba de regresión de una reserva **mixta** con política publicada y checkout conectado, que debe dar `PENDIENTE_PAGO` con la suma de los anticipos (`revisiones/009.md:41`). La de una sola cita sí existe (`ServicioCitasPromocionTest.java:90-102`).
- P3: el esquema DDL de `CobrosConcurrentesMysqlTest` no reproduce los índices reales (ver A1).

---

## Verificado sin hallazgos

**P1**
- `plan()` lanza `AnticipoNoDisponibleException` antes de `resolverOCrearCliente` en las dos rutas (`ServicioCitas.java:180`, `:325`), después de los bloqueos y dentro de la transacción.
- La rama `publico=false` no llama a `puedeCobrarOnline`.
- El manejador específico da 409 `ANTICIPO_NO_DISPONIBLE` y tiene prioridad sobre el de `ResponseStatusException`.
- La cotización calcula `reservableEnLinea`/`motivo` solo con `calcularAnticipo`, así que la creación no depende del checkout.
- Booking deshabilita el envío según el indicador del backend (`booking.component.html:589`, `.ts:719`).
- El aviso de administración se calcula por tenant (`ServicioPoliticaEmpresa.java:64-66`). Por diseño, solo cubre la falta de política.
- La configuración pública expone `productosOnlineDisponibles`/motivo, y el carrito los presenta.
- Las pruebas de rechazo verifican que no se crean cliente, cita, historial ni mensajes.

**P2**
- Hay una sola regla en un enum compartido, que recibe el saldo de `cobros.consultar`. El frontend solo filtra por `seccionAgenda`, y la prueba cubre las reglas.

**P3**
- `citasNormalizadas()`: rechaza que lleguen los dos campos (400), quita duplicados y ordena de forma ascendente.
- Bloquea las citas por tenant en orden ascendente y valida sucursal, cobrabilidad, cliente, moneda y saldo antes de cualquier escritura.
- `venta.citaId` solo se llena con una cita, y cada detalle lleva su `citaId`.
- Publica un evento por cita con su pendiente. `PublicadorPagosCitas.venta` agrupa por línea.
- El inventario de quién lee `venta.cita_id` coincide con lo que encontré: no hay otros lectores en el backend, y el frontend no lee `venta.citaId`.

**C1–C3**
- C1: `dias(n)` funciona, con prueba de «1 día natural».
- C2: la consulta va después de `bloquearGrupoOrden`, con prueba InOrder y prueba MySQL de instantánea (ver A1 sobre los bloqueos).
- C3: hay pruebas de `paymentKey` y del mensaje cuando falla la recarga.

## Fuera de alcance: para que el usuario decida (no atribuir a 009)

- **O1:** el bot de WhatsApp crea citas con `publico=false` (`ServicioWhatsappCitas.java:989`, `:2163`). Un servicio con anticipo reservado por el cliente desde el bot queda `PENDIENTE` sin cobro: es el mismo síntoma de P1, pero viene de antes.
- **O2:** una venta Point en `PENDIENTE_PAGO` no cuenta en el saldo (`v.estado='PAGADA'`), y `confirmarPoint` (`ServicioVentasCaja.java:249-265`) no bloquea la cita. Un pago directo hecho mientras Point está pendiente puede cobrar dos veces. Conviene agregarlo al seguimiento de Point multi-cita.

## Límites

- No compilé ni ejecuté suites. El cierre exige el conteo en los XML de surefire (con MySQL y `skipped=0` en `CobrosConcurrentesMysqlTest`), Angular completo y `git diff --check`.
- No hay evidencia de la revisión visual de Recepción, Cobros ni Punto de venta en móvil, tablet y escritorio.
- Point real no se probó; solo existe el rechazo explícito cuando hay más de una cita.
- `respuestas/009.md` todavía no existe.

## Validación para volver a revisar

1. A1 corregido, con la prueba MySQL de citas distintas sobre índices reales.
2. M1 corregido, con una prueba que use la forma de `ErrorApi`.
3. B1–B3, o el motivo para descartarlos registrado en `respuestas/009.md`.
4. Suites completas contadas y `git diff --check`.

Sin commit, publicación, cobros, envíos ni producción.
