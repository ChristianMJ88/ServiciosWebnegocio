# 002 — Revisión de cierre

Autor: Claude Code (lectura). Responde a `solicitudes/002-cierre.md` y `respuestas/002.md`. Revisión estática del diff y de los archivos nuevos; no se ejecutaron pruebas.

## Veredicto

**No se aprueba todavía. Hay un hallazgo bloqueante (H1)** porque implica dinero cobrado sin registro. H2 debe corregirse junto con H1, ya que afecta el mismo flujo. Lo demás está bien encaminado. El aislamiento por tenant, la configuración por empresa, el corte horario y el selector de profesional cumplen lo acordado.

## Verificado correctamente

| Área | Evidencia |
|---|---|
| Tenant por slug | `ResolutorTenantPublico` rechaza con 404 un `empresaId` distinto al del sitio publicado y valida que la sucursal, el servicio y la asignación pertenezcan a la empresa. Se aplica en citas, disponibilidad, servicios, prestadores y sucursales. |
| Prestadores | `AsignacionServicioPrestadorRepositorio.findActivasPorEmpresaYServicio` filtra por empresa tanto en el servicio como en el usuario. En disponibilidad, `resolverPrestador` exige que el prestador esté asignado al servicio y a la sucursal, no solo que sea de la empresa. |
| `1L` | Se retiraron los valores por defecto de `ServicioCitas` y `ServicioCatalogoServicios`. Si falta la empresa o la sucursal, responde 400. |
| Configuración por tenant | V132 con FK a empresa. El DTO valida rangos (`@Min/@Max`). El endpoint admin usa `empresaId` del JWT y el RBAC existente. Si la empresa no tiene configuración, se usa el respaldo de plataforma. |
| Corte horario | `ServicioConsultaDisponibilidad:109,192`: `Clock` UTC, zona de la sucursal, anticipación y ventana del tenant. Solo se ocultan las franjas anteriores al corte. |
| Ocupación | `ESTADOS_OCUPADOS` y el conflicto de `crearCita` incluyen `PENDIENTE_PAGO`. |
| Avisos diferidos | `ServicioCitas`: con `requierePago`, no se programan el correo de "registrada" ni el WhatsApp de confirmación. `confirmar` los programa al aprobarse el pago. `notificarEnAgenda` excluye `PENDIENTE_PAGO`. |
| Grupo y política | `politica_cobro` e `importe_requerido` quedan congelados en la cita. El checkout expande el grupo y omite las citas `SIN_COBRO`. La orden vence con el apartado más próximo del grupo. |
| Manual | `ServicioAdminCitas` bloquea los cambios sobre `PENDIENTE_PAGO`. Recepción, Staff y cliente no lo incluyen en sus listas de estados operables. |
| Migraciones | V132–V134 son aditivas. V130/V131 no se tocaron. La coordinación quedó registrada. |

## Hallazgos

### H1 — Alta, bloqueante: un pago aprobado después de liberar el grupo se pierde
**Dónde:**
- `ServicioConfirmacionCitasCheckout.java:33`: lanza `IllegalStateException` si la cita ya no está `PENDIENTE` o `PENDIENTE_PAGO`.
- Se invoca desde `ServicioEstadoPagoCheckout.aplicar` (`@Transactional`, línea 31, y la llamada en 51).

**Efecto:** la excepción revierte toda la transacción.
- El intento no queda `APROBADO`, la orden no queda `PAGADA` y `conciliacion.registrarCheckout` no se guarda.
- En el webhook (`ProcesadorEventosPagoCheckout:76`), el evento se reintenta 5 veces y queda en `ERROR`.
- En el pago con tarjeta (`ServicioPagoCheckout:118`), el cliente recibe un error aunque el cargo sí se hizo.

**Escenario realista:**
1. La orden vence junto con el apartado (`ServicioOrdenCheckout:156-158`).
2. El cliente paga en el último minuto.
3. El liberador (cada 60 s) libera el grupo antes de que llegue la aprobación.
4. También ocurre con un `PROCESANDO` cuyo vencimiento informado por el proveedor pasa antes de la aprobación.

**Corrección propuesta:**
- La aprobación nunca debe lanzar una excepción por el estado de la cita. Siempre se persisten el intento `APROBADO` y la conciliación.
- Si el grupo está `LIBERADA_SIN_PAGO`:
  - Si todos los horarios siguen libres, confirmar de nuevo con `buscarConflictos` bajo bloqueo.
  - Si no, marcar la orden como pagada con incidencia (un estado o bandera, p. ej. `requiere_conciliacion`), avisar al negocio para reprogramar o reembolsar, y no enviar al cliente un "confirmada".
- El liberador puede dejar un margen de gracia antes de liberar cuando existe un intento reciente en curso.

**Pruebas:**
1. Grupo `LIBERADA_SIN_PAGO` + `APROBADO` → no hay excepción, el intento queda `APROBADO`, la conciliación se registra y la incidencia queda marcada.
2. El mismo caso con los horarios libres → las citas quedan `CONFIRMADA` y se envían los avisos.

### H2 — Alta, plausible: carrera entre el liberador y la aprobación por lectura sin bloqueo
**Dónde:**
- `liberarCitasVencidas` (`:49-52`) lee las citas con `findTop100…` sin bloqueo y después llama a `grupo(cita)`, que sí bloquea (`CitaRepositorio`, `@Lock PESSIMISTIC_WRITE`).
- `grupoDeOrden` (`:68`) hace lo mismo con `findById`.

**Riesgo:** si una entidad ya está en el contexto de persistencia, la consulta con bloqueo no refresca necesariamente su estado. Hibernate devuelve la instancia gestionada.
- Ejemplo: el liberador lee la cita en `PENDIENTE_PAGO`, la aprobación confirma y hace commit, y el liberador obtiene el bloqueo con datos viejos. Resultado: libera una cita pagada.
- La tabla `cita` no tiene `@Version` que detecte el conflicto.

**Corrección:** obtener el bloqueo antes de cualquier lectura del grupo. Opciones:
- Una consulta con bloqueo por `reserva_grupo_id` como primera lectura.
- `entityManager.refresh(cita, PESSIMISTIC_WRITE)`.
- Añadir `@Version` a `CitaEntidad`, con su migración coordinada.

Después del bloqueo, volver a evaluar el estado.

**Prueba:** una de integración con dos transacciones. Si no es viable, al menos una unitaria que verifique que el estado se lee después del bloqueo o se refresca.

### M1 — Media: un `PROCESANDO` sin vencimiento bloquea el horario indefinidamente
`prorrogar` (`:43`) deja `pago_expira_en = null`, y la consulta del liberador ignora los `null`. El inventario tampoco se libera (`LiberadorReservasCheckout`). Si el estado final no llega (webhook perdido o evento en `ERROR`), el horario queda apartado para siempre.

**Corrección:**
- Un tope configurable por tenant, por ejemplo el menor entre N horas e inicio − margen.
- Un job que consulte `consultarCobro` en los intentos `PROCESANDO` antiguos.

**Prueba:** un intento `PROCESANDO` sin fecha y con más antigüedad que el tope → se consulta al proveedor y luego se libera.

### M2 — Media, decisión de negocio: un rechazo libera el grupo de inmediato
`ServicioEstadoPagoCheckout:54` libera el grupo con `RECHAZADO`, y la orden queda `RECHAZADA` sin poder reutilizarse. Un rechazo común, como fondos insuficientes, hace perder el horario aunque al cliente le queden minutos de apartado. La Adenda preveía conservar el carrito para reintentar.

**Propuesta (D6):** con `RECHAZADO`, mantener el apartado hasta `pago_expira_en` y permitir un nuevo intento sobre la misma orden. Liberar solo al vencer o con `CANCELADO`.

### M3 — Media: aviso de "cancelación" por un apartado que el cliente nunca recibió
`liberarGrupo` (`:59`) programa `programarCanceladaCliente` y el WhatsApp `programarCancelacionNegocio` al cliente. Como los avisos de creación se difieren, el primer mensaje que recibe el cliente es "cita cancelada por el negocio".

**Corrección:** usar un mensaje propio, "Tu apartado venció sin pago; el horario quedó libre", o no enviar nada. No avisar al negocio.

### M4 — Media: costo de `/publico/disponibilidad/dias`
`obtenerDias` (`:130-146`) calcula todas las franjas de cada día × servicio, y sin prestador elegido, de cada prestador. Cada cálculo hace unas 6 consultas, más `configuracion.obtener` en cada llamada.
- Con 31 días, 3 servicios y 5 prestadores, son unas 2 800 consultas por petición pública sin autenticar. Es lento y además es un vector de abuso.

**Corrección:**
- Precargar una sola vez para el rango las reglas, las excepciones y las citas ocupadas.
- Resolver la configuración una vez por petición.
- Cortar en la primera franja encontrada por día.
- Considerar límite de tasa.

**Prueba:** el número de consultas, o un tiempo acotado con datos de prueba.

### M5 — Baja (descartado en local y producción): `creada_en` no se escribe en UTC
`CitaEntidad.java:74` mapea `creada_en` con `insertable = false`, así que el `setCreadaEn(UTC)` de `ServicioCitas` se ignora al insertar y se usa el `DEFAULT CURRENT_TIMESTAMP` de MySQL. Al releer, el valor depende del `time_zone` de la sesión. `serverTimezone=UTC` en la URL no lo fuerza sin `forceConnectionTimeZoneToSession`.

Si el servidor MySQL no está en UTC, `PlazoConfirmacionCita` interpreta mal la creación. Una cita creada dentro del margen se liberaría en el siguiente ciclo, lo que contradice la decisión del usuario de permitir confirmar hasta el inicio.

**Verificar:** el usuario puede ejecutar `SELECT @@global.time_zone, @@session.time_zone` en producción (solo lectura).
**Resultado (2026-10-13):** `SYSTEM, SYSTEM`. No es concluyente: MySQL usa la zona del sistema operativo del servidor. Falta `SELECT @@system_time_zone, TIMEDIFF(NOW(), UTC_TIMESTAMP());`.
**Local:** la consulta fue sobre la base local. El contenedor `agenda-mysql` (mysql:8.0) tiene el sistema en UTC (`date` dentro del contenedor = UTC, sin `TZ`), así que en local M5 no se manifiesta. El equipo anfitrión está en CST. **Producción (2026-10-13):** `@@system_time_zone = UTC`, `TIMEDIFF(NOW(), UTC_TIMESTAMP()) = 00:00:00`. El defecto no se manifiesta hoy. Baja a prioridad baja como endurecimiento: escribir `creada_en` en UTC desde la aplicación (o quitar el `setCreadaEn` que se ignora) para no depender de la zona del host.
**Corrección:** que `creada_en` sea insertable y se escriba explícitamente en UTC, o una columna UTC propia para el plazo.
**Prueba:** una de repositorio con base real que compruebe que el valor releído coincide con el UTC escrito.

### Bajos
- **L1:** `ControladorCitasPublico:44,52,60` pasan `request` tal cual, y los servicios usan `request.empresaId()`. Si el cliente omite `empresaId`, el resolutor lo acepta pero el servicio responde 400. Conviene pasar el `empresaId` resuelto desde el slug.
- **L2:** `ProgramadorRecordatoriosWhatsapp:153` carga en cada ciclo todas las citas `PENDIENTE` de cada empresa, sin rango de fechas. Conviene acotar por `inicio`, entre ahora − 1 día y ahora + horas de liberación.
- **L3:** `ServicioPoliticaReservaPago:30`, `ServicioConfirmacionCitasCheckout` y `ServicioOrdenCheckout:156` usan `LocalDateTime.now(UTC)` en lugar del `Clock` inyectado. Es inconsistente con disponibilidad y deja sin poder probar los vencimientos con reloj fijo.
- **L4:** el estilo dificulta la revisión: nombres totalmente calificados, sangría rota y varias sentencias por línea en `ServicioCitas`, los controladores, `ServicioOrdenCheckout` y `ServicioPoliticaReservaPago`. Formatear solo las líneas tocadas, sin reformatear masivamente.
- **L5:** cambio de contrato. Los endpoints públicos ahora exigen `X-Tenant-Slug`, y `/publico/servicios` exige `sucursalId`. Conviene confirmar que no existen consumidores fuera de la SPA (widget, app móvil).
- **L6:** `/dias` evalúa cada servicio por separado. Un día puede marcarse disponible aunque no exista una secuencia que encaje todos los servicios. Booking lo detecta después en las franjas. Es aceptable, pero conviene anotarlo.

## Verificación pendiente antes del commit

- Suite backend completa después de las correcciones. La corrida de 315 pruebas es anterior a los últimos cambios de pagos.
- Suite completa de Angular, no solo las 2 suites de Booking. La falla conocida de `admin-providers.facade.spec.ts` es previa.
- Pruebas nuevas de H1, H2 y M1 (y de M2, si se aprueba D6).

## Decisión para el usuario

- **D6 (M2):** ante un rechazo, ¿mantener el apartado hasta que venza y permitir reintentar? Se recomienda que sí.
- **H1 sin horario libre:** ¿reembolso automático, o incidencia para que el negocio decida? Se recomienda la incidencia; el reembolso automático queda fuera de este alcance.

---

## Segunda revisión: correcciones de Codex (respuestas/002-cierre.md), 2026-10-13

Revisión estática. No se repitieron las suites. Codex reporta 332 pruebas backend (331 aprobadas, 1 omitida) y 152 de Angular aprobadas.

### Verificado

| Hallazgo | Estado | Evidencia |
|---|---|---|
| H1 | Corregido | `ServicioEstadoPagoCheckout.aplicar` ya no lanza excepción por el estado de la cita: `confirmar` devuelve `false` y la orden queda `PAGADA_CON_INCIDENCIA` con motivo; el intento y la conciliación se guardan siempre. Las citas `LIBERADA_SIN_PAGO` se confirman de nuevo si el horario sigue libre (`buscarConflictosBloqueando` excluyendo el grupo y comprobando que el inicio sea futuro). |
| H2 | Corregido | `grupoPorId` lee solo la referencia, bloquea y hace `refresh(PESSIMISTIC_WRITE)`. `liberarCitasVencidas` trabaja con IDs y vuelve a evaluar bajo el bloqueo. `aplicar` bloquea y refresca la orden y el intento. |
| M1 | Corregido | `prorrogar` sin fecha usa el tope del tenant (V136), acotado por el inicio de la cita. `ServicioRecuperacionPagosCheckout` consulta al proveedor antes de vencer, y `expirarEnProceso` revalida el último intento bajo bloqueo. |
| M2/D6 | Corregido | Con `RECHAZADO` dentro del plazo, la orden vuelve a `PENDIENTE_PAGO` y conserva el inventario y las citas. `validarOrden` impide un intento nuevo mientras hay uno `PAGO_EN_PROCESO`. |
| M3 | Corregido | Evento `CITA_APARTADO_VENCIDO_EMAIL` con texto propio. Ya no se envía WhatsApp de cancelación del negocio. |
| M4 | Corregido | `obtenerDias` precarga reglas, excepciones y citas del rango (`ContextoRango`), resuelve la configuración una vez, corta en el primer prestador con franja y limita el rango a hoy–ventana. |
| M5 | Corregido | `creada_en` es insertable y usa `FechaUtcPersistencia`; `forceConnectionTimeZoneToSession=true` y `hibernate.jdbc.time_zone=UTC`. La única ruta que crea citas (`ServicioCitas`, 2 sitios) asigna `creadaEn`, así que no hay riesgo de insertar `NULL`. |
| L1–L3 | Corregidos | Se usa el tenant resuelto, la consulta de pendientes está acotada y hay un `Clock` inyectado (`ConfiguracionReloj`). |
| Avisos | Correcto | El outbox deduplica por cita, evento y estado, así que un webhook `APROBADO` repetido no duplica el aviso de incidencia. |

### Hallazgos nuevos

#### N1 — Alta, antes de cerrar: un pago con incidencia no llega a Caja
`ServicioVentasCheckout.java:48` y `ProyectorVentasCheckout.java:22` solo proyectan órdenes en estado `"PAGADA"`, así que una orden `PAGADA_CON_INCIDENCIA` no genera venta.

**Efecto:**
- El dinero cobrado en línea no aparece en Caja ni en los reportes.
- Si el negocio reprograma la cita y la cobra en Caja, `sumarServiciosPagadosPorCita` no encuentra el anticipo. Caja pide el precio completo y **se le cobra dos veces al cliente**.

**Corrección:** proyectar la venta también para `PAGADA_CON_INCIDENCIA`, marcada como incidencia, para que el anticipo quede acreditado en la cita. Los envíos siguen excluidos, lo cual es correcto.
**Prueba:** orden `PAGADA_CON_INCIDENCIA` → existe una venta `ONLINE`, y la cita muestra `pagado` igual al anticipo en Caja.

#### N2 — Media: la incidencia no se puede resolver
Solo existe `GET /incidencias` (`ControladorConciliacionPagos:26`). `requiere_conciliacion` nunca vuelve a `false` y el listado (top 100) solo crece.

**Corrección:** una acción "resolver incidencia" con RBAC de pagos y auditoría. Debe incluir motivo (reprogramada / reembolsada fuera del sistema / otra) y, si se reprograma, confirmar la nueva cita con el pago existente.

#### N3 — Media-baja: el orden de los bloqueos es inconsistente
- `confirmar` bloquea primero las citas del grupo (`grupoPorId`) y después los prestadores (`:57`).
- `crearCita` bloquea el prestador y después las citas (`ServicioCitas:171-173`).

Si coinciden sobre el mismo prestador y horario, puede haber un deadlock. MySQL lo resuelve abortando una de las dos transacciones. Si cae la de `aplicar` en el pago con tarjeta sincrónico, el cliente ve un error aunque el cargo se hizo; el webhook lo recupera después.

**Corrección:** bloquear primero los prestadores, cuyos IDs se pueden obtener sin bloqueo a través de la referencia, y después las citas, igual que en la creación.

#### N4 — Baja-media: inanición en la recuperación de intentos antiguos
`buscarIdsParaRecuperacion` incluye los intentos con `pago_expira_en IS NULL`, ordenados por id, con límite de 100. Los que todavía no llegan al tope se saltan con `continue`. Si hay 100 o más intentos antiguos vigentes, nunca se revisan los siguientes. Codex lo señaló como límite.

**Corrección:** al leerlos, asignar `pago_expira_en = creado_en + tope` y persistirlo, o excluir los `NULL` después de ese relleno.

#### N5 — Baja: casos en que el aviso de incidencia no llega al negocio
- `ServicioCorreoEventosCita:97` sale si el cliente no tiene correo, antes de llegar a la rama de incidencia (`:108`).
- `avisarIncidencia` no avisa cuando la orden es solo de productos (incidencia por inventario), porque usa la primera cita.
- Tampoco avisa si el tenant tiene el correo deshabilitado.

En esos casos solo queda la lista de Pagos. **Corrección:** mover la rama de incidencia antes de esa comprobación y avisar también en órdenes sin citas.

#### N6 — Baja
- `ServicioRecuperacionPagosCheckout` registra el warning sin la excepción. Conviene añadir `ex`.
- La configuración JDBC de zona vive en la URL por defecto de `application.yml`. Si producción define `SPRING_DATASOURCE_URL` completa, el parámetro `forceConnectionTimeZoneToSession` no se aplica. Hoy no afecta, porque producción está en UTC. La prueba de UTC usa H2, no MySQL.

### Veredicto de la segunda revisión

Los bloqueantes H1/H2 y M1–M5 están **corregidos y verificados en el código**. **Para cerrar la 002 falta N1**, porque puede causar doble cobro al reprogramar. Se recomienda corregir N2 y N3 en la misma pasada. N4–N6 pueden quedar como seguimiento.

---

## Tercera revisión: N1–N3 (respuestas/002-cierre.md, sección «Segunda revisión»), 2026-10-13

Revisión estática. No se repitieron las suites. Codex reporta 344 pruebas backend (343 aprobadas, 1 omitida) y 155 de Angular aprobadas.

### Verificado
- **N1, corregido:** `ServicioVentasCheckout.java:48` proyecta `PAGADA` y `PAGADA_CON_INCIDENCIA` como venta `PAGADA`, así que `sumarServiciosPagadosPorCita` (`v.estado = 'PAGADA'`) acredita el anticipo en Caja. Los envíos siguen excluidos.
- **N2, parcial:**
  - Hay RBAC (`PAGOS_CONFIGURAR`), tenant de sesión, alcance de sucursal, bloqueo y refresco de la orden, protección contra doble resolución y auditoría.
  - `REEMBOLSADA_FUERA_DEL_SISTEMA` deja la venta `REEMBOLSADA`, que ya no suma como pagada.
  - Un error a mitad de la reprogramación revierte la transacción completa (`ResponseStatusException`).
- **N3, corregido según la descripción:** se bloquean primero los prestadores y después las citas, con el mismo orden en confirmación, prórroga, liberación y resolución.

### Hallazgos

#### R1 — Alta: reprogramar una incidencia falla siempre con datos reales
`ServicioResolucionIncidenciasCheckout` compara las franjas con `LocalDateTime.parse(f.inicio())` y `LocalDateTime.parse(f.fin())`. Pero `ServicioConsultaDisponibilidad.java:247-251` genera `inicio` y `fin` con `OffsetDateTime.toString()` (p. ej. `2026-10-14T10:00-06:00`), y `LocalDateTime.parse` lanza `DateTimeParseException` cuando el texto trae offset.

**Efecto:** la opción `REPROGRAMADA` responde 500 en cualquier caso real. La prueba (`ServicioResolucionIncidenciasCheckoutTest.java:70`) no lo detecta porque arma las franjas con `LocalDateTime.toString()`, sin offset, un formato que el backend nunca produce.

**Corrección:** `OffsetDateTime.parse(f.inicio()).toLocalDateTime()`, igual que hace `ServicioCitas` al crear la cita.
**Prueba:** usar en el fixture el formato real de la franja (`inicio.atZone(zona).toOffsetDateTime().toString()`). Mejor aún, una prueba que use el `ServicioConsultaDisponibilidad` real o lo construya desde su salida.

#### R2 — Baja: la reprogramación administrativa aplica las reglas públicas
La validación usa `obtenerFranjasDisponibles`, que aplica la anticipación mínima y la ventana de reserva del tenant. Por eso el negocio no puede reprogramar una incidencia para dentro de 30 minutos si la anticipación mínima es mayor. Es aceptable; solo conviene confirmarlo como regla.

#### R3 — Baja: comisiones al reembolsar
Si la venta proyectada ya devengó comisión (`ServicioComisionesDevengadas`) y luego se marca `REEMBOLSADA`, la comisión no se revierte. Hay que verificar si el flujo actual de devolución en Caja la revierte y aplicar lo mismo.

### Veredicto de la tercera revisión
N1 y N3 están cerrados. **R1 debe corregirse antes del commit**, porque la reprogramación desde Pagos, que es la salida principal de una incidencia, no funciona con datos reales. Corregir R1 es un cambio de dos líneas más el ajuste del fixture. Con eso, la 002 queda aprobada por esta revisión. R2, R3 y N4–N6 quedan como seguimiento.

---

## Cuarta revisión: R1, 2026-10-13

- **R1, verificado:** `ServicioResolucionIncidenciasCheckout.java:78-79` usa `OffsetDateTime.parse(...).toLocalDateTime()` para el inicio y el fin. El fixture (`ServicioResolucionIncidenciasCheckoutTest.java:71-74`) genera las franjas con `atZone(America/Mexico_City).toOffsetDateTime().toString()`, el mismo formato que produce `ServicioConsultaDisponibilidad`. La prueba parametrizada cubre las citas `LIBERADA_SIN_PAGO` y las `PENDIENTE` sin cobro.
- **Pruebas reportadas:** backend 344 (343 aprobadas, 1 omitida) y Angular 155 aprobadas. Los timeouts de la ejecución simultánea con Maven no se repitieron al correr Angular sola; parecen deberse a carga del equipo y no a un defecto.

### Veredicto final

**Tarea 002 aprobada por la revisión.** Puede hacerse commit cuando el usuario lo autorice.

Quedan como seguimiento, sin bloquear:
- R2: confirmar la regla de anticipación al reprogramar desde Pagos.
- R3: revertir comisiones al reembolsar.
- N4: relleno de `pago_expira_en` en intentos antiguos.
- N5: casos en que no se envía el aviso de incidencia.
- N6: log sin la excepción y parámetro JDBC sujeto a la URL de producción.
- L4–L6 de la primera revisión.

**Antes de desplegar:**
- V130–V136 están sin commit. Revalidar que no colisionen si se publican ramas nuevas.
- Verificar que producción no reemplace la URL JDBC completa (N6).
