# Validación de suscripciones Stripe

## Alcance verificado

Las pruebas automatizadas ejecutan el manejador de webhooks con firmas HMAC de prueba y repositorios simulados. No cobran dinero ni modifican suscripciones de Stripe. Las decisiones de acceso se comprueban en `ServicioCapacidadesEmpresa`, sin reglas comerciales nuevas en el frontend.

| Caso | Resultado esperado | Evidencia |
| --- | --- | --- |
| Checkout completado | Vincular cliente y suscripción; continuar pendiente hasta la confirmación de pago | Prueba automatizada |
| Factura pagada | Activar el plan | Prueba automatizada |
| Renovación con formato actual `parent.subscription_details` | Resolver la suscripción local y guardar la factura | Prueba automatizada |
| Pago fallido | Aplicar los días de gracia configurados en el plan; no ampliar por reintentos | Prueba automatizada |
| Recuperación de pago | Reactivar y eliminar la gracia | Prueba automatizada |
| Cancelación al final del período | Mantener acceso antes del vencimiento y retirarlo después, incluso si el webhook se demora | Prueba automatizada |
| Cancelación definitiva | Registrar la cancelación; una factura posterior no reactiva | Prueba automatizada |
| Notificación repetida | No procesar de nuevo el evento registrado | Prueba automatizada |
| Evento anterior | No revertir un estado posterior | Prueba automatizada |
| Cambio Inicial → Profesional desde el portal | Factura de prueba pagada y capacidades actualizadas por backend | Validado en navegador y webhooks de Stripe en modo prueba |

Suite dirigida: **29 pruebas aprobadas**, sin fallos ni pruebas omitidas.

## Validación externa restante

La renovación mensual, el rechazo del ciclo siguiente, la recuperación automática y la cancelación programada se comprobaron contra Stripe con un reloj y un negocio independiente (véase la ejecución con reloj más abajo). Restan la finalización de una contratación nueva en la interfaz alojada de Checkout y la comprobación externa del corte de acceso con el reloj del backend ante un webhook demorado. No se ha cancelado ni sustituido la suscripción Profesional utilizada en la validación del portal.

Para esa ejecución, utilizar un tenant de prueba sin suscripción vigente y verificar:

1. Inicio de Checkout desde Plan Fluora con el precio configurado en Plataforma.
2. Finalización con un medio de pago de prueba y recepción de los webhooks firmados.
3. Plan, estado, período y capacidades del tenant después de la confirmación.
4. Renovación y fallo de pago con la ventana de gracia definida por el catálogo.
5. Recuperación y cancelación programada; comprobar el acceso antes y después de la fecha de corte.

No avanzar el reloj ni cancelar la suscripción del tenant compartido para simular estos escenarios. Un túnel temporal requiere actualizar la URL del webhook si cambia su dominio.

## Ejecución local

### Verificación del 8 de octubre de 2026

#### Cobro y rechazo contra Stripe de pruebas

Se creó un negocio local independiente (empresa 6, suscripción 3), sin cambiar Nail Art Studio. El precio Inicial se obtuvo del catálogo. La suscripción `sub_1UOCdtAzYrdtuY8aeJNuvjz1` utilizó métodos de pago virtuales oficiales de Stripe.

- Se reinició el ciclo sin prorrateo para generar una factura de $199.00: `in_1UOCggAzYrdtuY8a7u3LZexd` / `CCKX2TZW-0002`, pagada en Stripe. El aviso 257 quedó ENVIADA en Fluora.
- Con el método de prueba que rechaza cobros, el siguiente reinicio generó `in_1UOERpAzYrdtuY8aaQ0Ajp0q` / `CCKX2TZW-0003`: abierta, importe pagado cero, suscripción Stripe `past_due`.
- El túnel temporal perdió resolución DNS. Se restableció y se actualizó el destino existente, conservando su secreto y sus seis eventos. El evento real `evt_1UOERtAzYrdtuY8an7GjnXeV` se reenvió desde Stripe y respondió 200. Fluora pasó a GRACIA; aviso 258 ENVIADA, un intento y sin error. Reenviar el mismo evento no duplicó el aviso ni extendió la gracia.
- Se canceló la suscripción aislada al terminar para detener futuros cobros de prueba. La suscripción Profesional del demo se preservó.

Esta primera ejecución comprobó cobros por reinicio de ciclo (`billing_reason=subscription_update`); no equivalía a avanzar un mes mediante un reloj de Stripe. En ese momento la clave restringida no permitía crear relojes y el token de onboarding expiró. La ejecución posterior con reloj resolvió ambas limitaciones.

La factura inicial expuso un orden posible de webhooks: `invoice.paid` llegó antes de vincular la suscripción. Se agregó resolución por metadata de la factura únicamente para registros PENDIENTE_PAGO sin vínculo, verificando empresa y cliente Stripe. Tres regresiones prueban el cobro anticipado y rechazan empresa o cliente distintos. Suite dirigida: 34 pruebas aprobadas.

- Cambio temporal Profesional → Expansión y restauración a Profesional mediante Stripe en modo prueba, sin prorrateo ni nuevos cargos. Ambos webhooks fueron procesados; los avisos de cambio 255 y 256 quedaron ENVIADA, con un intento y sin error. La suscripción del negocio quedó ACTIVA y con renovación habilitada.
- Recuperación y vencimiento se verificaron con pruebas automatizadas del backend; la recuperación también se comprobó externamente en la ejecución posterior con reloj. Los rechazos descritos corresponden a tarjetas virtuales de Stripe, sin dinero real ni avance del reloj del tenant compartido.
- Se comprueba que los reintentos de una factura fallida conservan el plazo de gracia y la referencia de deduplicación del aviso. El procesador de vencimientos no modifica la política de acceso; si falla la preparación del aviso, no marca al negocio como notificado y permite reintentarlo.
- Los precios y planes de la prueba se resolvieron desde el catálogo configurado, sin modificar las reglas del frontend.

#### Renovación mensual con reloj de Stripe

Después de la autorización y verificación de identidad del propietario, se habilitó únicamente Test Clocks → Escritura en la clave restringida de pruebas. Stripe confirmó el permiso mediante una creación exitosa por API.

Se registró el negocio aislado empresa 7 y se inició Checkout por el endpoint normal de Fluora, con el plan y precio obtenidos del catálogo activo. La suscripción `sub_1UOVDvAzYrdtuY8aBeiIy947` se creó por la API de pruebas sobre el cliente preparado por Fluora; la sesión alojada de Checkout se expiró para evitar una segunda contratación. Esto valida el ciclo de Billing y los webhooks, pero no la finalización de la interfaz alojada.

Reloj: `clock_1UOVDUAzYrdtuY8ah4qosjIo`. Se avanzó solo ese reloj hasta dos horas después del siguiente límite mensual, sin reiniciar el ciclo de facturación.

| Etapa | Stripe | Fluora y correo |
| --- | --- | --- |
| Cobro inicial | Factura `in_1UOVDvAzYrdtuY8a9zsxoOM5`, $199.00 pagados | Suscripción 4 ACTIVA; aviso 260 ENVIADA, un intento |
| Siguiente ciclo mensual | Factura `in_1UOVEYAzYrdtuY8a5IXYe3bg`, $199.00 pagados; evento `evt_1UOVEfAzYrdtuY8a29aUW9Ab` | ACTIVA; período actualizado; aviso 261 ENVIADA, un intento |
| Ciclo posterior con tarjeta virtual de rechazo | Factura `in_1UOVGVAzYrdtuY8akmJvdvq6`; Stripe `past_due`; evento `evt_1UOVGbAzYrdtuY8afNreLbEu` | GRACIA; aviso 262 ENVIADA, un intento |
| Recuperación automática | Se restauró la tarjeta virtual aprobada y se avanzó tres días hasta después del reintento programado; la misma factura quedó pagada por $199.00 y Stripe volvió a `active` | ACTIVA; gracia eliminada; aviso 263 ENVIADA, un intento |
| Cancelación programada | `cancel_at_period_end=true`; al avanzar al cierre del período se recibió `evt_1UOVNzAzYrdtuY8au4CBjTa3` (`customer.subscription.deleted`) | Acceso conservado antes del cierre; suscripción CANCELADA y capacidades SIN_SUSCRIPCION, `suscripcionVigente=false`; avisos 264 y 265 ENVIADA, un intento cada uno |

Los eventos quedaron PROCESADO, sin error. La consulta autenticada de capacidades devolvió ACTIVA tras el pago y GRACIA tras el rechazo, conservando acceso durante la ventana configurada. El plazo de gracia se calcula con el reloj del backend: adelantar Stripe no adelanta el servidor local. Se verificó una ventana de tres días, hasta `2026-10-12 10:18:45.332773` UTC.

Los avisos se enviaron al destinatario de pruebas autorizado y no registraron error SMTP. ENVIADA confirma transporte; la recepción en la bandeja del destinatario debe confirmarse por separado. Nail Art Studio mantuvo su suscripción 2 ACTIVA, plan Profesional y renovación habilitada.

La suscripción aislada se dejó cancelada para detener futuros cobros de prueba. El precio, estado, gracia y capacidades se resolvieron en backend; no se modificaron reglas comerciales del frontend ni credenciales productivas.

Desde `backend`, con JDK 21:

```sh
mvn -q -Dtest=ServicioCapacidadesEmpresaTest,ServicioStripePlataformaTest,ServicioStripeWebhookTest,ServicioPlanSuscripcionStripeTest test
```
