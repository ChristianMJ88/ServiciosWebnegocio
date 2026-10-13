# Promociones en reservas, checkout y Caja

El backend calcula los importes. El frontend envía identificadores de tenant,
sucursal, servicios, promoción y fecha/horario; no envía precios ni descuentos.

## Reglas y persistencia

- `PORCENTAJE`: neto = lista × (1 − valor/100).
- `PRECIO`: valor es el precio final de **cada servicio incluido**, limitado entre
  cero y el precio de lista. No representa un descuento fijo ni un precio de paquete.
- Solo descuentan los servicios asociados. Una lista de sucursales vacía permite
  todas las sucursales; una lista de servicios vacía no descuenta reservas.
- La promoción debe pertenecer al tenant, estar activa, no eliminada, coincidir
  en moneda y estar vigente tanto hoy como el día de la cita. Las fechas se evalúan
  en la zona horaria configurada de la sucursal.
- La cotización devuelve precios normales y el motivo cuando la oferta no aplica.
  La creación vuelve a cotizar dentro de la transacción: una promoción enviada que
  no aplica a ninguna línea responde `409`, antes de crear/actualizar el cliente.
- Se congelan `precio_lista`, `descuento`, `precio` neto, `promocion_id` y el snapshot
  `{id,titulo,tipoDescuento,valorDescuento,moneda}`. Reprogramar conserva estos valores.
- El administrador actualiza promociones por ID. Las retiradas se marcan con
  `eliminada_en`, sin eliminar la referencia de citas existentes. IDs ajenos,
  duplicados o ya eliminados se rechazan. Se conserva la auditoría antes/después.

## Anticipos y reconocimiento del descuento

Un calculador compartido se usa en cotización y checkout. El anticipo se calcula
sobre el neto, con la política del servicio por encima de la política general de
checkout. La suma inicial y el saldo quedan limitados al importe pendiente.

Crear una cita cotiza exclusivamente su precio pactado, sin depender del anticipo.
La vista previa calcula anticipos solo si checkout está habilitado y permite
servicios. Si el anticipo está mal configurado, muestra el neto completo; el
checkout conserva la validación estricta y no cobra con esa configuración inválida.

Si la cita ya tiene pagos, el checkout cobra el saldo pendiente completo; el
anticipo solo se calcula en el primer cobro. El backend identifica el concepto
como «Anticipo», «Pago total» o «Liquidar saldo» en el nombre persistido del detalle,
que presenta el checkout público. No existen parcialidades configurables.

Se reconoce **todo el descuento en el primer cobro**, incluso si es anticipo.
Los cobros siguientes contienen únicamente el saldo neto. El precio de lista de
una línea de cobro es el importe cobrado más el descuento reconocido en esa línea;
no es un prorrateo del descuento por cada pago. El precio original completo sigue
congelado en la cita. Esto mantiene `subtotal − descuento = total` en las ventas
y evita duplicar el descuento al cobrar el saldo.

Ejemplo ilustrativo, no configuración del sistema:

| Concepto | Lista del cobro | Descuento reconocido | Cobro neto |
|---|---:|---:|---:|
| Cita de lista $250, neto $200; anticipo 50% | $150.00 | $50.00 | $100.00 |
| Saldo posterior | $100.00 | $0.00 | $100.00 |
| Total | $250.00 | $50.00 | $200.00 |

Los pagos directos de citas existentes cuentan como pagos previos para esta regla;
no se reconstruyen ni modifican movimientos históricos. Las citas con neto cero
no se envían al checkout desde la reserva; no se crea un cobro sin saldo.

Los pagos directos de citas (`registrarPago`) no generan líneas de descuento; la
fuente de verdad del descuento de servicios es la cita (`precio_lista`,
`descuento`, `promocion_id`). Caja muestra estos importes y el título del snapshot
al seleccionar una cita con descuento. No se añadió una columna a `pago_cita`.
Un futuro reporte usará la cita como fuente, sin sumar descuentos de líneas de
servicio de `venta_detalle`, para cubrir todos los canales y evitar doble conteo.

## Cotización preliminar y comprobación

`POST /api/v1/publico/citas/cotizar` exige suscripción, como la creación de citas.
Antes de seleccionar horario, `inicio` puede ser nulo y el item puede incluir
`fecha` (día local deseado). Sin ambos se cotiza para hoy en la sucursal. Cuando hay
un `inicio`, ese instante manda sobre la fecha preliminar. La creación siempre usa
el horario real validado por disponibilidad, nunca la fecha preliminar.

El borrador conserva solo IDs, incluido el de promoción. Después de reservar,
checkout usa los IDs de las citas y sus precios pactados. Las respuestas de precio
obsoletas se ignoran y se bloquea la reserva mientras se cotiza o hay un error.

Pruebas: calculador puro y redondeo, alcance y vigencia con zona horaria, reservas
simples/múltiples, reprogramación, IDs y borrado lógico, anticipos/saldo, venta mixta
y proyección online. Angular verifica ID de promoción, precios del servidor frente
al catálogo desactualizado, payload de reserva, rechazo de respuestas obsoletas,
precio normal de una oferta inválida y ausencia de checkout para citas gratuitas.

## Archivos de esta integración

Backend (bajo `backend/src/main/java/com/techprotech/agenda/`):

- `modulos/sitio/api/dto/PromocionSitioRequest.java`
- `modulos/sitio/aplicacion/ServicioPromocionesSitio.java`
- `modulos/sitio/infraestructura/entidad/EmpresaSitioPromocionEntidad.java`
- `modulos/sitio/infraestructura/repositorio/EmpresaSitioPromocionRepositorio.java`
- `modulos/sitio/infraestructura/repositorio/EmpresaSitioPromocionServicioRepositorio.java`
- `modulos/sitio/infraestructura/repositorio/EmpresaSitioPromocionSucursalRepositorio.java`
- `modulos/citas/api/ControladorCitasPublico.java`
- `modulos/citas/api/dto/CrearCitaRequest.java`
- `modulos/citas/api/dto/CrearCitasMultiplesRequest.java`
- `modulos/citas/api/dto/CitaCreadaResponse.java`
- `modulos/citas/api/dto/CotizarCitasRequest.java`
- `modulos/citas/api/dto/ResultadoCotizacionCitas.java`
- `modulos/citas/aplicacion/ServicioCitas.java`
- `modulos/citas/aplicacion/ServicioCotizacionCitas.java`
- `modulos/citas/aplicacion/CalculadorPromocionServicios.java`
- `modulos/citas/aplicacion/CalculadorAnticipoServicio.java`
- `modulos/citas/aplicacion/DesglosePrecioCita.java`
- `modulos/citas/infraestructura/entidad/CitaEntidad.java`
- `modulos/pagos/aplicacion/ServicioOrdenCheckout.java`
- `modulos/caja/aplicacion/ServicioVentasCaja.java`
- `modulos/caja/aplicacion/ServicioVentasCheckout.java`

Migración: `backend/src/main/resources/db/migration/V131__promociones_precio_pactado_citas.sql`.

Frontend:

- `src/app/core/admin/admin.service.ts`
- `src/app/pages/admin-dashboard/site/site-promotions.component.ts` y `.html`
- `src/app/pages/home/home.component.ts`
- `src/app/pages/booking/booking.component.ts`, `.html`, `booking-experience.css`
- `src/app/services/appointment.service.ts`
- `src/app/services/reservation-cart.service.ts`
- `src/app/shared/components/reservation-cart/reservation-cart.component.ts`, `.html`, `.css`

Pruebas nuevas/modificadas:

- `backend/src/test/java/com/techprotech/agenda/modulos/citas/aplicacion/CalculadorPromocionServiciosTest.java`
- `backend/src/test/java/com/techprotech/agenda/modulos/citas/aplicacion/ServicioCotizacionCitasTest.java`
- `backend/src/test/java/com/techprotech/agenda/modulos/citas/aplicacion/ServicioCitasPromocionTest.java`
- `backend/src/test/java/com/techprotech/agenda/modulos/citas/aplicacion/ServicioReprogramacionPrecioPactadoTest.java`
- `backend/src/test/java/com/techprotech/agenda/modulos/sitio/aplicacion/ServicioPromocionesSitioTest.java`
- `backend/src/test/java/com/techprotech/agenda/modulos/pagos/aplicacion/ServicioOrdenCheckoutTest.java`
- `backend/src/test/java/com/techprotech/agenda/modulos/caja/aplicacion/ServicioVentasCajaTest.java`
- `backend/src/test/java/com/techprotech/agenda/modulos/caja/aplicacion/ServicioVentasCheckoutTest.java`
- `src/app/pages/booking/booking.component.spec.ts`
- `src/app/pages/admin-dashboard/site/site-promotions.component.spec.ts`
