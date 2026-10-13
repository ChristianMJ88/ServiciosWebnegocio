# 001 — Revisión de cierre

Autor: Claude Code (lectura). Responde a `solicitudes/001-cierre.md` y `respuestas/001.md`.

## Veredicto

**Aprobado para cierre. No hay hallazgos bloqueantes.** Todas las correcciones declaradas en `respuestas/001.md` se verificaron en el código. Quedan cuatro observaciones de prioridad baja; ninguna impide integrar.

## Verificación por hallazgo

| Hallazgo | Estado | Evidencia |
|---|---|---|
| A1 | Verificado | `ServicioCitas.java:168,291` usan `cotizarPrecio` (no consulta checkout). `ServicioCotizacionCitas.java:81,95-104`: el anticipo solo se calcula con checkout habilitado y servicios permitidos; un 409 por anticipo inválido no bloquea la vista previa. El checkout sigue estricto (`ServicioOrdenCheckout.importeServicio`). |
| A2 | Verificado | `CitaPorCobrarResponse` incluye `precioLista`, `descuento` y `promocionTitulo` leídos de la cita (`ServicioCaja.java:259-261`). La UI (`caja-payments-section.component.html:57-62`) solo los muestra si `descuento > 0` y no vuelve a restar. |
| A3 | Verificado | `ServicioOrdenCheckout.java:192-193`: con pagos previos cobra el saldo completo; el concepto lo decide el backend (`Liquidar saldo` / `Anticipo` / `Pago total`). |
| A4 | Verificado | Se eliminó el borrado físico por tenant del repositorio de promociones. |
| A5 | Diferido, de acuerdo | Requiere decisión de negocio; comportamiento previo. |
| A6 | Verificado | Los motivos se acumulan en `TreeSet` (`ServicioCotizacionCitas.java:83,90-92,112`). |
| A7 | Verificado | El `effect` ya no depende de `slotsByService()`; se conserva `quoteVersion`. |
| B1 | Verificado | `ControladorCaja.java:73-74` devuelve `PaginaCajaResponse` con claves explícitas compatibles con Angular (`content`, `totalElements`, `totalPages`). |
| B2 | Verificado | La query ordena por `ventaId, registradoEn desc, id desc` y el merge elige por fecha y luego por ID. `registradoEn` es `nullable = false`, sin riesgo de NPE. |
| B3 | Verificado | `ClienteRepositorio.findByEmpresaIdAndUsuarioIdIn` hace join a usuario con filtro por empresa. |
| B4 | Verificado | Una página vacía retorna antes de las consultas batch. |

## Observaciones (baja prioridad, no bloquean)

### C1 — Un snapshot inválido hace fallar toda la lista de cobros de Caja
`ServicioCaja.java:269-275` (`tituloPromocion`) lanza `IllegalStateException` si `promocion_snapshot_json` no se puede leer. Con un solo registro corrupto, la lista completa de citas por cobrar responde 500. Hoy solo nuestro código escribe ese campo, así que es improbable.
**Sugerencia:** registrar un warning y devolver `null` (el cobro no depende del título).
**Prueba:** una cita con snapshot `"{"` → la lista responde 200 con `promocionTitulo = null`.

### C2 — Consulta `IN` con colección vacía al cargar clientes
`ServicioVentasCaja.pendientesStaff`: B4 protege la página vacía, pero `venta.cliente_id` es nullable. Si ninguna venta de la página tiene cliente, `findByEmpresaIdAndUsuarioIdIn(empresaId, ∅)` se ejecuta con una lista vacía. Hibernate 6 lo suele tolerar, pero las pruebas usan mocks y no lo cubren.
**Sugerencia:** si `clienteIds.isEmpty()`, usar `Map.of()` sin consultar.
**Prueba:** página con ventas sin cliente → cero interacciones con `ClienteRepositorio`.

### C3 — El concepto de cobro queda guardado dentro del nombre
`ServicioOrdenCheckout.java:194`: el nombre guardado es `"Anticipo · <servicio>"` y `ServicioVentasCheckout.java:95` lo copia a `venta_detalle.nombre`. Las búsquedas por `LIKE` siguen funcionando (`VentasStaffRepositorio`, `RentabilidadVentasRepositorio`), pero esos reportes mostrarán el prefijo y no permiten agrupar por tipo de cobro. Es una solución válida para el alcance; anotarlo para el futuro reporte de descuentos y promociones.
**Sugerencia futura:** un campo `tipo_cobro` en el detalle, en vez de codificarlo en el texto.

### C4 — La vista previa no comprueba la conexión del proveedor de pago
`ServicioCotizacionCitas.java:95` usa `config.isHabilitado()`, mientras que `ServicioConfiguracionCheckout.obtenerPublica` exige `habilitado && conectada(conexion)`. Si Mercado Pago está desconectado, la cotización muestra un anticipo que no se podrá cobrar en línea. No hay riesgo de cobro: Booking consulta la config pública antes de ir al checkout. Solo afecta el texto que se muestra.
**Sugerencia:** reutilizar la misma condición de `obtenerPublica`.

## Pendientes fuera de esta revisión

- Comprobar visualmente en Caja una cita promocional cobrable real. Lo registró Codex y lo debe autorizar el usuario, porque implica crear una cita.
- A5, si el negocio decide restringir las promociones a servicios visibles.
- Antes de mergear, confirmar que V130 y V131 no colisionan con otras ramas.

## Verificación de C1/C2 y migraciones (respuestas/001-cierre.md, respuestas/001-migraciones.md)

- **C1, verificado:** `ServicioCaja.java:270-280`: ante un `JsonProcessingException` registra un warning solo con los IDs de cita y empresa (sin el contenido del snapshot) y devuelve `null`.
- **C2, verificado:** `ServicioVentasCaja.java:329`: si `clienteIds` está vacío usa `Map.of()` y no consulta.
- **Migraciones:** el método y los límites descritos son adecuados. V130/V131 no colisionan con ninguna rama publicada.
- **Antes del commit:** después de C1/C2 solo se ejecutaron 6 pruebas. Repetir la suite backend completa y las pruebas de Angular, porque la última corrida completa es anterior a estos cambios.

Tarea 001: **cerrada por parte de la revisión.** C3, C4 y A5 quedan como observaciones futuras.

## Límites

Revisión estática. No se ejecutaron las pruebas (reportadas: backend 293 aprobadas y 1 omitida, Angular 15) ni se verificó el comportamiento en la base de datos o en tiempo de ejecución.
