No tengo herramienta de escritura en esta sesión (solo Read, Glob y Grep), así que **no creé `revisiones/009-cierre.md`**. Abajo está el contenido completo para ese archivo, listo para guardarlo con `scripts/claude-review.sh` o a mano. No toqué la aplicación ni ejecuté pruebas.

---

```markdown
# Revisión de cierre 009: correcciones A1, M1, B1–B3

Autor: Claude Code, revisión de solo lectura (Read/Glob/Grep), 2026-10-17. No compilé ni ejecuté suites. Solo atribuyo a 009 y a C1–C4 lo que figura en solicitudes/009.md y 009-cierre.md.

**Resultado: aprobada con condiciones.** No hay hallazgos altos ni medios. Las condiciones son cerrar la evidencia de suites y la revisión visual (ver «Condiciones»). B4–B6 son bajos y no bloquean el cierre: se pueden corregir o registrar como seguimiento en respuestas/009.md.

## Verificado

**A1**
- `@Transactional(isolation=READ_COMMITTED)` está en:
  - `ServicioCaja.registrarPago` (`ServicioCaja.java:274`);
  - `ServicioVentasCaja.registrar` y `prepararPoint` (`ServicioVentasCaja.java:133`, `:142`);
  - la transacción exterior `ServicioMercadoPagoPoint.crearOrden` (`ServicioMercadoPagoPoint.java:113`). Esto es necesario porque `prepararPoint` se une a esa transacción y su aislamiento propio no se aplicaría.
- Los únicos llamadores son controladores, con `open-in-view: false` (`application.yml:12`), así que el aislamiento se aplica al iniciar la transacción. No encontré ninguna entrada que se una a una transacción con aislamiento por defecto.
- `calcularBajoBloqueo` delega en el cálculo común (`ServicioSaldoCitas.java:45-47`). Ya no existen `pagosActuales` ni `serviciosPagadosActuales`, y no queda ningún `FOR UPDATE` sobre `pago_cita` ni `venta_detalle` en Caja.
- Se conserva el orden de bloqueo: primero la cita (ascendente en ventas, `:180-185`), después el saldo y al final las escrituras. Con READ COMMITTED, las lecturas que siguen al bloqueo ven lo último confirmado y no toman bloqueos de hueco.
- Prueba MySQL (`CobrosConcurrentesMysqlTest`):
  - crea los índices reales (`:51`) y usa aislamiento 2 en las conexiones;
  - en citas distintas, la barrera queda después de que ambos hilos leen el saldo y antes de insertar (`:72`). Con el `FOR UPDATE` anterior habría interbloqueo y el `future.get` fallaría, así que la prueba sí detecta la regresión;
  - se conservan 200/409 con la misma cita;
  - una prueba reflexiva cubre las cuatro anotaciones (`:139-144`).
- Aceptación C2 (`:87-107`): demuestra que la transacción RR conserva su instantánea (`count=0` en `:95`). Aun así, después de `bloquearGrupoOrden` ve el anticipo directo de la cita `uno` (resultado NULL) y excluye la venta de la orden actual en la cita `dos` (resultado 91).

**M1**
- `admin-policy.component.ts:25` lee primero `error.mensaje`.
- La prueba usa la forma real de `ErrorApi` (`spec.ts:8`).

**B1**
- El botón queda desactivado con `cobroMultiple` y aparece la nota (`caja-payments-section.component.html:93`).
- Hay una guarda en el método (`.ts:112`), y los manejadores leen `mensaje` primero (`:134`, `:65`).
- La prueba confirma que no se llama a `crearOrdenPoint`.
- Sigue el rechazo del backend (`ServicioVentasCaja.java:145`) antes de cualquier llamada externa.

**B2**
- La etiqueta depende de `estadoCobro==='PAGADA'`; en otro caso muestra «Atendida · por cerrar» (`recepcion-agenda-section.component.html:35`).
- La regla de sección no cambia, como ya se había decidido.

**B3**
- `ServicioCitasPromocionTest.java:124-139` usa `ServicioPoliticaReservaPago` real, con política y checkout.
- Cubre tres servicios: 50 % de 250, un fijo de 40 y uno sin anticipo. Todos quedan `PENDIENTE_PAGO` con expiración, el total requerido es 165 y no se envían mensajes ni outbox.
- Que el servicio sin anticipo quede apartado coincide con la regla vigente para grupos mixtos.

**Inventario de escritores**
- Coincide con lo que encontré: `pago_cita` solo lo escribe `ServicioCaja`, siempre bajo bloqueo de cita.
- La venta directa ahora bloquea las citas.
- El webhook bloquea el grupo antes de las lecturas C2 y de la proyección.
- `confirmarPoint` (`:248-267`) no bloquea la cita: es O2, ya existía y queda en seguimiento.
- El bot sigue con `publico=false`: es O1, ya existía.
- El cambio a READ COMMITTED no agrega riesgos de doble cobro respecto al estado anterior a 009. Lo que antes protegía la lectura con bloqueo inicial lo cubre ahora una lectura actual tras el bloqueo.

## Hallazgos bajos

### B4 · Baja: el webhook en RR con `FOR UPDATE` puede bloquear a Caja, con un ciclo posible pero raro
- **Dónde:** `CitaRepositorio.java:21-27`, `ServicioEstadoPagoCheckout.java:58-60`.
- **Qué pasa normalmente:** Caja (RC) ya no toma huecos. Si inserta en el hueco que el webhook bloqueó, solo espera; no hay ciclo.
- **Cuándo sí hay ciclo:** se tienen que cumplir dos condiciones a la vez:
  - el webhook bloquea existencias (`confirmarSiDisponible`) → grupo de citas → next-key en `venta_detalle`, mientras Caja inserta `venta_detalle` de la cita B → bloquea la existencia del mismo producto;
  - la cita del checkout es anterior a B, de modo que la fila no confirmada de B es el siguiente registro del índice.
- **Efecto:** MySQL aborta una de las dos transacciones. Si cae el webhook, el proveedor lo reintenta. Si cae Caja, el cajero recibe un 500 genérico.
- **Recomendación (seguimiento, fuera de 009):** pasar `ServicioEstadoPagoCheckout.aplicar` a READ_COMMITTED y que C2 use lecturas normales tras `bloquearGrupoOrden`, con su propia prueba MySQL. Otra opción es documentarlo junto con un reintento ante interbloqueo. Registrarlo en respuestas/009.md.

### B5 · Baja: Point mantiene el bloqueo de la cita durante la llamada HTTP, sin timeouts
- **Dónde:** `ServicioMercadoPagoPoint.java:125-132`.
- **Qué pasa:** desde P3, `prepararPoint` bloquea la cita, y la llamada a `cliente.crearOrden` ocurre dentro de la misma transacción.
- **Por qué importa:** `ClienteMercadoPagoPoint.java:31` usa `builder.build()` sin timeouts, y no encontré `read-timeout`/`connect-timeout` en `application.yml` ni un customizer compartido. Si Mercado Pago tarda, un pago directo o una venta de esa cita espera hasta `innodb_lock_wait_timeout` y termina en 500.
- **Recomendación:** configurar timeouts de conexión y lectura en el cliente Point (como `ClienteCorreoSendgrid.java:25-27`). Si no entra en 009, hacerlo en el seguimiento de Point multicita, junto con O2.

### B6 · Baja: pruebas incompletas
- `CobrosConcurrentesMysqlTest.java:83`: en el caso de citas distintas, solo se comprueba el saldo de `id`. Falta afirmar que `otro` también quedó pagado en 500 con pendiente 0.
- `recepcion-agenda-section.component.spec.ts`: no hay un caso `SIN_COSTO`/`PARCIAL` que verifique «Atendida · por cerrar».
- `calcularBajoBloqueo` usa `@Transactional` REQUIRED. Si alguien lo llama fuera de una transacción, abriría una en RR sin bloqueo previo. Opcional: `propagation = MANDATORY`.

## Condiciones de cierre
1. Repetir la suite backend completa después del último cambio y dar el conteo XML: total, fallos/errores = 0, omitidas, suites, y `CobrosConcurrentesMysqlTest` con 8 pruebas y `skipped=0`. El conteo de 539/143 es anterior a la última ronda.
2. Repetir Angular completo después del ajuste de las pestañas (el 181/64 es anterior) y pasar `git diff --check`.
3. Revisión visual de Recepción (etiquetas Pagada/Atendida), Caja Cobros (multiselección y Point desactivado) y POS en móvil 375×812, tablet 820×1180 y escritorio 1440×900, sin desbordamiento ni recorte en las pestañas, usando datos existentes y sin cobrar.
4. respuestas/009.md con la disposición de B4–B6 y de O1/O2.
5. **Antes de desplegar:** comprobar que el MySQL de destino tenga `binlog_format=ROW` (o MIXED). Con `STATEMENT`, las transacciones READ COMMITTED que escriben fallan.

## Límites
- Point real no se probó; solo están cubiertos el rechazo multicita y la guarda del frontend.
- Sobre la política versión 1 publicada en una ejecución externa: no la atribuyo a Codex ni evalué su contenido.
- Sin commit, publicación, cobros, envíos ni producción.
```

---

**En resumen:** la apruebo con condiciones. Las correcciones A1, M1 y B1–B3 están bien aplicadas, y el paso a READ COMMITTED no deja un riesgo de doble cobro nuevo dentro del alcance de 009.

Los tres puntos bajos:
- **B4:** el webhook todavía puede bloquear a Caja, y en un caso raro se interbloquean.
- **B5:** Point llama a Mercado Pago sin timeouts mientras mantiene bloqueada la cita.
- **B6:** faltan algunas comprobaciones en las pruebas.

Para cerrar faltan:
- repetir las suites backend y Angular;
- la revisión visual;
- `respuestas/009.md`;
- y, antes de desplegar, comprobar que el MySQL use `binlog_format=ROW`.
