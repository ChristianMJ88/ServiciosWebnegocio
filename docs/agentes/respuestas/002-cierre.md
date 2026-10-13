# 002 — Correcciones de cierre (Codex)

Revisión completa leída. Usuario aprobó H1/H2, M1/M3/M4/M5 y L1–L3; D6 sí (rechazo conserva apartado/reintento), H1 sin horario libre se trata como incidencia, sin reembolso automático. Sin commit.

## Migraciones coordinadas antes de crear

2026-10-13: `git fetch --all` terminado; todas las referencias locales/remotas y el directorio de migraciones revisados. V135 y V136 libres. Reservadas: V135 para incidencia de pago en orden; V136 para tope de procesamiento por tenant. No editar V132–V134 aplicadas. No cubre ramas no publicadas de otros equipos.

## Correcciones implementadas

- **H1:** un pago aprobado se conserva como APROBADO y se concilia aunque el apartado ya haya sido liberado. Se comprueba disponibilidad bajo bloqueo antes de recuperar el grupo y el inventario. Si no es posible, la orden queda PAGADA_CON_INCIDENCIA, con motivo y señal de conciliación; no se confirma al cliente ni se reembolsa automáticamente. La incidencia queda consultable por tenant en Pagos y se genera el aviso al negocio para reservas.
- **H2:** los procesos obtienen primero identificadores/referencias, bloquean el grupo y refrescan sus entidades antes de decidir. El liberador reevalúa estado y vencimiento bajo el bloqueo; no decide usando la entidad previamente cargada. Los prestadores también se bloquean para coordinar recuperación y creación de reservas.
- **M1:** tope de procesamiento configurable por negocio, con valor inicial definido por configuración del backend. Los nuevos intentos sin vencimiento del proveedor reciben un plazo persistido, acotado por el inicio de la cita. El recuperador consulta al proveedor antes de aplicar el vencimiento y vuelve a validar la orden y el último intento bajo bloqueo. No inventa un rechazo del proveedor.
- **M2 / D6 aprobado:** el rechazo conserva el apartado mientras siga vigente y permite otro intento en la misma orden. Cancelación o vencimiento liberan el apartado; una notificación tardía no degrada un pago aprobado.
- **M3:** evento y mensaje propios para apartado vencido, sin presentarlo como cancelación del negocio ni emitir ese aviso comercial.
- **M4:** consulta de días con reglas, excepciones y citas cargadas por rango; configuración obtenida una vez y búsqueda interrumpida al encontrar disponibilidad. Límite y ventana del endpoint definidos en backend; se conserva el cálculo de franjas existente.
- **M5:** creación de citas persistida explícitamente en UTC, conversión de persistencia y configuración JDBC de zona. Se agregó verificación SQL con sesión de base de datos en otra zona.
- **L1:** los tres endpoints públicos reconstruyen la solicitud con el tenant resuelto por el slug, incluso cuando el cuerpo omite empresaId; se mantienen las comprobaciones de pertenencia.
- **L2:** selección de citas pendientes sin cobro limitada por sucursal y rango de fecha local, evitando recorrer todo el histórico pendiente del tenant.
- **L3:** Clock inyectado en política, grupos, órdenes y trabajos relacionados, con pruebas de tiempo deterministas.

V135 añade la marca/motivo de incidencia en la orden; V136 añade el tope de procesamiento por negocio y el índice correspondiente. No se modificaron migraciones aplicadas.

## Verificación

- Suite completa backend: `mvn -q test`, salida 0; **332 pruebas, 331 aprobadas, 1 omitida, 0 fallos y 0 errores**.
- Suite completa Angular: `npx ng test --watch=false`; **152 pruebas aprobadas en 56 archivos**, sin fallos.
- `git diff --check`: sin errores.
- Casos añadidos: aprobación tardía con horario disponible y ocupado; recuperación y falta de inventario; rechazo/reintento/cancelación; notificación tardía; refresco después del bloqueo; tope configurable; vencimiento con mensaje propio; consultas por rango; tenant omitido del cuerpo público; persistencia UTC leída desde SQL; presentación de incidencia y reintento según respuesta del backend.
- Se corrigió el mock previo de `invitarPrestador` en la prueba del facade, conservando el comportamiento de invitación del código de aplicación.

## Límites y entrega a revisión

La carrera H2 se verifica con prueba de orden bloqueo/refresco y reevaluación; no se ejecutó una prueba de dos transacciones concurrentes contra MySQL. La comprobación SQL de UTC utiliza H2; no certifica la configuración de un servidor productivo. El caso de recuperación comprueba la consulta/aplicación y delegación al servicio de expiración; no usa una cuenta real del proveedor. Los intentos antiguos sin plazo se procesan en lotes de 100; queda por revisar el comportamiento de acumulaciones grandes de intentos legacy aún no vencidos.

No se efectuaron pagos externos, envíos reales de correo ni una revisión visual adicional en navegador en este cierre. L4–L6 no forman parte de las correcciones solicitadas. Los cambios están sin commit y quedan disponibles para la nueva revisión de Claude; esta respuesta no sustituye su aprobación.

## Segunda revisión — N1, N2 y N3

Se leyó completa la sección «Segunda revisión». Alcance aprobado: N1–N3, sin commit. N4–N6 siguen como seguimiento de Claude y no se mezclaron con estas correcciones.

### N1 — Venta y anticipo de pagos con incidencia

`ServicioVentasCheckout` admite PAGADA y PAGADA_CON_INCIDENCIA; ambas generan venta PAGADA de canal ONLINE, vinculada a la orden y con detalles que conservan cita e importe efectivamente pagado. La observación de venta identifica la incidencia. Así la suma existente de pagos por cita en Caja acredita el anticipo sin duplicar una regla de precios. El proyector selecciona únicamente órdenes pagadas todavía sin venta (incluye las incidencias), evitando que las primeras 100 ya proyectadas oculten las siguientes. La proyección sigue siendo idempotente bajo bloqueo de orden. Los envíos conservan su selección de PAGADA y no procesan incidencias abiertas.

Prueba de integración con repositorios JPA/H2: orden con incidencia → una sola venta ONLINE; anticipo de 100 acreditado en la consulta real de detalles y en la respuesta de Caja; precio de cita 500, pendiente 400. Otra empresa no encuentra ese anticipo.

### N2 — Resolver incidencia con permiso y auditoría

Nuevo POST `/api/v1/admin/pagos/conciliaciones/incidencias/{ordenId}/resolver`, bajo el permiso existente PAGOS_CONFIGURAR y el módulo PAGOS_ONLINE. El tenant proviene de la sesión; se comprueba también el alcance de sucursal, se bloquea/refresca la orden y se impide resolver dos veces.

- REPROGRAMADA: exige todos los miembros de la reserva, mantiene prestador y duración, valida nuevas franjas con el servicio de disponibilidad existente, impide superposiciones y confirma el grupo con el pago existente. Incluye los miembros sin cobro que siguen pendientes; sus horarios anteriores no bloquean la nueva reserva. Los nuevos horarios se guardan antes del refresco de confirmación. Solo al completar esto se quita la incidencia y la orden pasa a PAGADA.
- REEMBOLSADA_FUERA_DEL_SISTEMA: registra una devolución que el negocio ya realizó; no llama a una API para devolver dinero. Orden y venta quedan REEMBOLSADA; el anticipo deja de contarse como dinero disponible para cobrar esa cita.
- OTRA: requiere motivo, quita la marca de conciliación pendiente y conserva el estado financiero de pago con incidencia. No inventa confirmación, recuperación de stock ni habilita envíos automáticos.

Se proyecta el pago antes de resolver si el trabajo programado aún no lo había hecho. La auditoría existente guarda tenant, actor, correo, orden, estado/motivo previos, horarios previos y resolución con los horarios solicitados. Un webhook aprobado repetido no reabre la incidencia ni revierte su resolución; conserva la actualización de conciliación financiera del proveedor. El formulario en Pagos presenta las opciones del backend, pide motivo y nuevos horarios cuando corresponde, conserva los datos al recibir un rechazo y actualiza el listado después del éxito.

No se requiere migración nueva: se reutilizan los campos de V135, la venta/detalle existentes y `auditoria_configuracion_empresa`. V132–V136 no se modificaron.

Pruebas: autorización real con Method Security (sin permiso denegado, con PAGOS_CONFIGURAR permitido); aislamiento de empresa y alcance de sucursal; auditoría y resolución repetida; reprogramación con validación/confirmación y rechazo de horario ocupado; registro de devolución externa; webhook duplicado; formulario Angular con éxito, error y envío de horarios solamente para reprogramación.

### N3 — Orden de bloqueos

Se obtienen referencias y IDs de prestadores mediante proyecciones escalares antes de cargar citas gestionadas. Para la orden completa se bloquean primero todos los prestadores, ordenados y sin repetidos; después se bloquean/refrescan los grupos de citas. Se usa el mismo orden en confirmación, prórroga, liberación y resolución. Las pruebas verifican que el bloqueo del prestador precede a la lectura bloqueada de citas y al refresco; se conserva la reevaluación de H2.

### Verificación final de esta pasada

- Suite completa backend final: `mvn -q test`, salida 0; **344 pruebas, 343 aprobadas, 1 omitida, cero fallos/errores**.
- Suite completa Angular final: `npx ng test --watch=false`, salida 0; **155 pruebas aprobadas en 57 archivos**, cero fallos.
- `git diff --check`: sin errores.
- Las corridas iniciales detectaron fixtures nuevos incompletos (estado/nombre de sucursal en Caja y acceso TypeScript a una clave de diccionario); se corrigieron y se repitieron las suites completas. No se cambió la lógica de aplicación para acomodar esos fixtures.
- La comprobación del anticipo usa persistencia y consultas reales JPA/H2 más el servicio real de Caja, con adaptadores externos simulados. El orden de bloqueos se verifica mediante pruebas unitarias; no se ejecutaron dos transacciones concurrentes en MySQL.
- No hubo pagos externos, reembolsos reales, envío de correos reales ni revisión visual adicional en navegador. Sin migración nueva, sin commit y sin despliegue. Cambios disponibles para la siguiente revisión de Claude.

## Tercera revisión — R1

Se leyó completa la sección «Tercera revisión». Se corrigió únicamente R1: `ServicioResolucionIncidenciasCheckout` interpreta inicio y fin de las franjas mediante `OffsetDateTime.parse(...).toLocalDateTime()`, conservando la hora local que se utiliza para las citas de la sucursal.

El fixture de reprogramación genera ambos extremos con `inicio.atZone(zona).toOffsetDateTime().toString()`, igual que el servicio de disponibilidad real. La prueba parametrizada cubre tanto citas liberadas como miembros sin cobro pendientes usando el formato con offset, en lugar del formato local anterior que ocultaba el error.

No se requiere migración ni cambios de reglas de disponibilidad. R2, R3 y N4–N6 permanecen como seguimiento fuera de este alcance. Sin commit.

Verificación final de R1:

- Suite completa backend: `mvn -q test`, salida 0; **344 pruebas, 343 aprobadas, 1 omitida, cero fallos/errores**. Incluye ambos casos de reprogramación con inicio y fin en formato real con offset.
- Suite completa Angular: `npx ng test --watch=false`, salida 0; **155 pruebas aprobadas en 57 archivos**, cero fallos.
- La primera ejecución simultánea de Angular y Maven tuvo tres timeouts de 5000 ms en pruebas existentes de App, Staff y Caja. Se repitió la suite completa de Angular sin Maven ejecutándose a la vez; todas pasaron, sin modificar pruebas ni sus límites de tiempo.
- `git diff --check`: sin errores. Sin migración nueva, sin commit ni despliegue. No se hicieron pagos externos ni revisión visual adicional. R1 queda corregido y verificado por Codex; la tercera revisión de Claude condiciona la aprobación de 002 a esta corrección.
