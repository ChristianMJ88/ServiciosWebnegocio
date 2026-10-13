# 002 — Solicitud de revisión de implementación

Autor: Codex. Alcance: revisión 002 completa y Adenda, D1–D5 aprobadas por usuario. Sin commit.

Leer `respuestas/002.md` y `ESTADO.md` para evidencia y límites. Revisar el diff y los archivos nuevos; no modificar código de aplicación.

## Decisión adicional autorizada

Una cita sin cobro creada dentro del margen de liberación puede confirmarse hasta el inicio. `PlazoConfirmacionCita` decide desde inicio en zona de sucursal y creación UTC; lo consumen confirmación y liberación. Prueba del límite justo antes/al inicio incluida.

## Áreas de revisión

1. Slug como ámbito de endpoints públicos, validación de sucursales/servicios/prestadores y queries de asignaciones por empresa.
2. Configuración por tenant, migraciones coordinadas V132/V133/V134; no modificar V130/V131 ni migraciones ya creadas/aplicadas.
3. Corte UTC/local y días del backend, navegación/selector por profesional en Booking, sin reglas de disponibilidad duplicadas.
4. Política por servicio, cotización mixta, congelación de importes y grupo de reserva; ninguna notificación de confirmación antes de pago.
5. Aprobación, rechazo, vencimiento y procesamiento sin fecha del proveedor; inventario y citas agrupadas. Examinar especialmente pago tardío después de liberación y estados recibidos fuera de orden.
6. Confirmación de cliente y modificaciones manuales de citas pendientes de pago; etiquetas/notificaciones en paneles.

## Evidencia pendiente

Verificación de Codex terminada: backend limpio 315 pruebas (314 aprobadas, 1 omitida); después, 13 pruebas de 4 suites afectadas por las últimas correcciones de pagos, todas aprobadas. Frontend Booking/borrador 17 aprobadas. Flyway validó 134 migraciones y esquema V134. Interfaz pública revisada en móvil 375×812, tablet 820×1180 y escritorio 1440×900: sin desbordamiento horizontal; profesional, fecha automática, anticipo/saldo y CTA comprobados con catálogo real. No se creó cita ni se efectuó cobro; los avisos de pruebas son mocks. El detalle de incidencias intermedias y evidencia está en la respuesta.

El working tree conserva cambios previos y la revisión 002 de Claude. No hacer commit, desplegar, modificar credenciales, crear citas/cobros ni enviar notificaciones reales. Registrar hallazgos en `revisiones/002-cierre.md`; Codex actualizará resultados finales en la respuesta/estado.
