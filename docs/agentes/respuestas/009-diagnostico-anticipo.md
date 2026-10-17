# Prueba real local del anticipo — 2026-10-17

## Alcance solicitado y resultado

El usuario solicitó reproducir una reserva pública con un servicio que exige anticipo, usando los datos de cliente que proporcionó. Se ejecutó el flujo real del navegador en `localhost:4200`, contra el backend habitual y MySQL local. No se usaron respuestas simuladas. Se leyeron PROTOCOLO, ESTADO, `revisiones/007-1-cierre-final.md` y `revisiones/009.md`; esta prueba confirma P1 de 009. No se implementaron P1–P3 ni C1–C4 en este encargo de diagnóstico.

- Servicio seleccionado desde el catálogo publicado del tenant: Diseño de ceja.
- Precio: $280.00 MXN. Anticipo: $140.00 MXN, `ANTICIPO_PORCENTAJE`.
- Sucursal y profesional elegidos por los datos reales del catálogo/disponibilidad; sin constantes de negocio nuevas.
- Reserva efectivamente creada: **FL-2SEVGJN4**, 20 de octubre de 2026, 10:00–10:40, hora de la sucursal.
- Persistencia comprobada en MySQL: `PENDIENTE`, precio 280.00, importe requerido 140.00, `pago_expira_en=NULL`, **0 pagos_cita y 0 líneas de orden de checkout**.
- El navegador no abrió checkout. Antes de enviar, decía «El anticipo de $140.00 se paga en el negocio» y mostraba «Reservar cita».
- La reserva de prueba permanece registrada; no se canceló, eliminó, confirmó ni cobró. No se incluyeron los datos personales del cliente en esta documentación.

## Causa comprobada

1. La configuración persistida de este tenant tiene habilitados checkout, tarjeta, servicios y productos. Su conexión Mercado Pago está `CONECTADO` en `PRUEBA`, con llave pública presente (solo se consultó su presencia, nunca su contenido).
2. Hay **0 políticas publicadas**. La compuerta incorporada en 007-1 lo requiere: `ServicioPoliticaReservaPago.puedeCobrarOnline` devuelve false y `ServicioConfiguracionCheckout.obtenerPublica` devuelve `habilitado=false`, `politica=null`. No se intentó llamar al proveedor de pagos.
3. `ServicioPoliticaReservaPago.plan` combina el importe requerido con esa disponibilidad: si el cobro no está disponible, devuelve `requierePago=false`. `aplicar` guarda `PENDIENTE` y no crea plazo de pago, aunque mantiene el importe requerido y la política de anticipo.
4. `ServicioCotizacionCitas` conserva los $140.00 pero devuelve `requierePago=false`. `booking.component.html` interpreta esa combinación como anticipo en el negocio. El submit y su respuesta permiten reservar; `booking.component.ts` solo abre checkout cuando `requierePago=true`.

La reserva sin anticipo cobrado es un fallo de degradación silenciosa; la falta de política no debería convertir la condición de cobro del servicio en pago presencial. Coincide con la causa y la corrección propuesta en P1 de 009: decisión backend explícita de no reservable, rechazo transaccional 409 antes de crear cliente/cita y presentación del motivo en la UI. No se publicó automáticamente una política para ocultar el problema.

## Conteo de impacto local

Consulta de solo lectura por slug del tenant, desde `flyway_schema_history.installed_on` de V137 (2026-10-17 07:19:59 según la sesión MySQL), con `politica_cobro <> SIN_COBRO`, `importe_requerido > 0`, estado `PENDIENTE`, sin `pago_cita` positivo y sin línea de servicio de venta `PAGADA` con total positivo: **2 candidatas**, incluida esta prueba.

Ese predicado identifica candidatas, no acredita por sí solo el origen público de ambas: la tabla de citas no registra explícitamente ese canal. La nueva reserva sí se comprobó por el flujo público del navegador. No se corrigieron registros históricos.

## Evidencia y límites

- Captura local del mensaje reproducido: `/private/tmp/fluora-prueba-anticipo-sin-cobro.png`. Se recargó la selección después de crear la reserva para capturar el aviso; su fecha visible corresponde a esa nueva selección, no a la cita ya persistida del día 20.
- Lecturas de configuración pública y SQL local; comprobación real de creación y ausencia de orden/pagos.
- No se modificó código de aplicación, configuración comercial, credenciales ni políticas; no se ejecutaron suites por tratarse de diagnóstico sin cambios de código.
- No se realizó pago ni despliegue, no se consultó producción y no se hizo commit. No se certificó la entrega de notificaciones externas de la reserva.
- La corrección de 009/P1 y el cierre 007-1/C1–C4 siguen pendientes; este resultado no los declara implementados.
