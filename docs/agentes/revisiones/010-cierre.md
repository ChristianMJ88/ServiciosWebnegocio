# Revisión de cierre 010

Revisé el código actual en modo de solo lectura. **A1–A5 y B1–B5 están corregidos en lo esencial.** Encontré dos hallazgos medios y cinco bajos. M1 debe corregirse antes de cerrar 010. Los demás pueden cerrarse en esta entrega o quedar como seguimiento explícito en `respuestas/010.md`. No ejecuté pruebas ni revisé el diff. La evidencia de las suites es la que reporta Codex.

## Medios

### M1 · El reembolso de una orden aprobada tarde revierte una venta que pagó otra orden
**Evidencia**
- La corrección de A3 funciona: una orden que se aprueba tarde, o que ya fue sustituida, queda en `APROBADO` sin registrar el pago (`ServicioVentasCaja.java:274-277`).
- Pero si después llega `REEMBOLSADO` y la orden no tiene partes previstas, `aplicarEstado` llama a `marcarReembolsadaPoint` sin más comprobaciones (`ServicioMercadoPagoPoint.java:286,295-296`).
- `marcarReembolsadaPoint` (`ServicioVentasCaja.java:333-343`) no revisa el estado de la venta, la orden vigente ni que exista un pago de esa orden.

**Impacto**
- **Staff (orden A cancelada, B cobrada, A aprobada tarde):** el negocio corrige el doble cargo reembolsando A. Entonces la venta que pagó B pasa a `REEMBOLSADA`, la cita vuelve a deber, el pago de B sale del corte y se reintegra inventario.
- **Venta de Caja ya cancelada:** el inventario se reintegra por segunda vez, porque `cancelarPoint` ya lo había hecho.

**Corrección**
- Con `REEMBOLSADO`, llamar a `marcarReembolsadaPoint` solo si la venta está `PAGADA` y existe un `pago_venta` `MERCADO_PAGO_POINT` con la referencia de esta orden. En cualquier otro caso, solo marcar revisión (`revisarReembolsoPoint` y `detalleEstado` `REVISAR:`).
- Pasar la orden al método, como ya se hace en `confirmarPoint`.

**Pruebas (MySQL)**
- Staff A tardía, B pagada y reembolso de A: la venta sigue `PAGADA`, saldo 0, el corte conserva B y el inventario no cambia.
- Venta de Caja cancelada, aprobación tardía y reembolso: el inventario no cambia.

### M2 · Las órdenes atascadas pueden desaparecer de la UI aunque sigan bloqueando
**Evidencia**
- `listarOrdenesCaja` toma las 50 más recientes de **toda la empresa** y luego filtra por sucursal (`ServicioMercadoPagoPoint.java:97-98`).
- Actividad muestra solo 5 órdenes (`caja-sales-section.component.html:62`).
- Cobros no tiene forma de retomar una orden. Su bloqueo envía a Actividad (`caja-payments-section.component.ts:59`).
- El 409 del cierre de caja no dice qué orden lo bloquea (`ServicioCaja.java:180-181`).

**Impacto**
- Una orden en `CREANDO` o `REQUIERE_REVISION` que salga de esas ventanas sigue bloqueando la terminal, las citas y el corte, pero ya no se puede «Continuar» ni resolver desde la UI.
- Es decir, A1 vuelve a ocurrir a nivel de interfaz.

**Corrección**
- El backend devuelve siempre todas las órdenes no finales de la sucursal (filtrando por la terminal), seguidas de las recientes.
- La UI muestra primero las órdenes en curso, sin recortar.
- El 409 del cierre incluye el folio o folios.

**Prueba:** con 6 órdenes finales más nuevas que una en `REQUIERE_REVISION`, Actividad sigue mostrando la orden en revisión y su acción.

## Bajos

- **B1n · La moneda remota se lee de un solo campo.** `ClienteMercadoPagoPoint.java:95` lee solo `currency` en la raíz. `MapeadorRespuestaMercadoPago.java:42-43` además prueba `currency_id` y la moneda del pago. Si la respuesta real de Point no trae `currency` en la raíz, «Sí se cobró» responde siempre 409. Conviene alinear con el mapeador y validarlo con una respuesta del sandbox convertida en fixture, sin cobrar. No verifiqué la documentación de Mercado Pago.
- **B2n · Un reparto que ya no pasa las reglas no tiene salida.** Al confirmar, `PagosPrevistosPoint.leer` vuelve a aplicar las reglas actuales de `PlanPagosVenta.resolver` (catálogo y `requiereReferencia`, `:43`). Si el catálogo cambia, una orden aprobada lanza excepción en T2 en cada reintento del webhook. Como `ServicioResolucionPoint` solo atiende `REQUIERE_REVISION` sin ID remoto, no hay forma de resolverla. Corrección: si falla la lectura al confirmar, registrar solo Point y marcar revisión, igual que D2 «no cuadra», sin propagar la excepción.
- **B3n · Historial con varios métodos.**
  - El HQL (`PagoVentaRepositorio.java:34-42`) es correcto: una fila por detalle, el importe es el del detalle y el desempate es por id.
  - Su prueba (`PagosConciliadosMomentoTest.java:41-47`) usa H2 y un solo pago `TARJETA`, así que no prueba `MIXTO`, el desempate ni una venta con dos citas. Hay que agregar esos tres casos, idealmente en la suite MySQL.
  - Hay una inconsistencia: `mapearPagos` usa `size()>1` para `MIXTO` (`ServicioVentasCaja.java:516`) y el HQL usa `count(distinct metodo)`. `PlanPagosVenta` acepta dos líneas del mismo método. Conviene rechazar métodos repetidos o usar el mismo criterio en los dos lugares.
- **B4n · Mensajes de error en Punto de venta.**
  - Cancelar, reembolsar, simular y el sondeo leen solo `error.detail` (`caja-sales-section.component.ts:165,185,197,214`), pero `ErrorApi` usa `mensaje`. Por eso se pierde, por ejemplo, el 409 explicativo del reembolso dividido.
  - Además, «Reembolsar» aparece en órdenes divididas que el backend siempre rechaza. Hay que ocultarlo con `efectivoPrevisto>0` o con un indicador del backend.
- **B5n · Formulario de resolución.**
  - Los campos de resolución, incluida la casilla «Verifiqué», no se reinician al cambiar de orden (`caja-cobro.component.ts:27,34`). Conviene reiniciarlos cuando cambia `orden.id`.
  - La auditoría guarda `cobrado:true` aunque el estado remoto haya quedado en `EN_TERMINAL` o `CANCELADO`. Conviene guardar también el estado remoto.

## Incidente de catálogo V139 (límite que hay que cerrar)
- **La comparación por hash no cubre las columnas que se borraron.** Las huellas excluyen `pagos_previstos_json`, `monto_recibido` y `cambio` (`MigracionPointDivididoMysqlTest.java:41`). Por eso no demuestran que no se perdieran datos en esas tres columnas mientras estuvieron borradas.
  - **Validación (solo lectura):** contar las filas de `orden_point_caja` con `creado_en` y de `pago_venta` con `registrado_en` posteriores al `installed_on` de V139.
  - Si hay alguna, registrar sus ids en `respuestas/010.md`. Una orden no final que haya perdido su reparto se confirmaría solo como Point, con revisión.
- **La guarda del origen detecta, pero no previene.** Hoy el DDL va calificado y la conexión efímera se verifica con `getCatalog`, y eso está bien. Aun así, conviene que `FLUORA_REGRESION_MYSQL_URL` apunte a un esquema o usuario desechable sin privilegios sobre la base habitual.
- **Configuración global:** el valor por defecto de `ddl-auto` cambió a `validate` (`application.yml:14`). Es lo que se pidió en A5, pero afecta a todos los entornos locales y debe quedar registrado.
- **Límite de A2 que se conserva hasta 007-2:** el corte sigue contando como cobrados los 400 de Point aunque se hayan reembolsado, porque la venta sigue `PAGADA`. Debe quedar en `respuestas/010.md`.

## Verificado sin hallazgos nuevos
- **A1:**
  - `ServicioResolucionPoint` exige `PAGOS_CONFIGURAR` en el controlador y en el servicio, y rechaza a usuarios de plataforma.
  - Solo atiende `REQUIERE_REVISION` sin ID remoto y vuelve a validarlo dentro de la transacción con bloqueo.
  - El motivo debe tener entre 10 y 500 caracteres.
  - La auditoría guarda actor, motivo y momento UTC desde `Clock`.
  - Antes de bloquear, valida ID remoto, `external_reference`, importe, moneda y que la orden remota no esté vinculada a otro cobro.
  - En la UI, la vista de resolución sustituye a la de espera y el sondeo se detiene en `REQUIERE_REVISION`.
- **A3:** `esOrdenPointVigente` y la idempotencia por referencia del pago están bien. Las pruebas MySQL cubren la cuenta cancelada y la orden Staff sustituida.
- **A4:** el GET ya no reenvía. `POST /reintentar` exige `CAJA_COBRAR` y alcance. `buscarConAlcance` se aplica en consultar, cancelar, reembolsar, simular, reintentar y resolver, y está probado con otra sucursal.
- **A5:** `validate` por defecto, con Flyway activo.
- **B1:** referencia obligatoria en el backend (`PlanPagosVenta.java:41`).
- **B2:** `mapear` es tolerante y devuelve `planInvalido`.
- **B3:** `PessimisticLockingFailureException` responde 409 `COBRO_CONCURRENTE` en el controlador Point.
- **B4:** `cerrarSesion` usa READ_COMMITTED.
- **B5:** la huella de la solicitud bajo bloqueo de terminal responde 409 si cambia la cuenta.

## Límites
- No ejecuté suites, `git diff --check` ni la revisión visual, que sigue en curso. Angular final está pendiente de repetirse.
- No verifiqué en la documentación de Mercado Pago la búsqueda por `external_reference` ni el campo `currency`.
- Revisé las specs del frontend solo de forma superficial. No amplié el alcance a 011.

## Para cerrar 010
1. Corregir M1 con sus pruebas MySQL.
2. Corregir M2, o registrarlo explícitamente como seguimiento.
3. Hacer la validación de solo lectura del incidente de V139.
4. Repetir las suites completas (backend con MySQL sin omitir y Angular) y `git diff --check`.
5. Terminar la revisión visual de `REQUIERE_REVISION`, resolución y aprobación con revisión en los tres tamaños.
