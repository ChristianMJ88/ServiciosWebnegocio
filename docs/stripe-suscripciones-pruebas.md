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

## Validación externa pendiente

La contratación nueva desde Checkout, la renovación, los reintentos de cobro y la cancelación aún requieren una ejecución completa en Stripe con un negocio de prueba independiente. No se ha cancelado ni sustituido la suscripción Profesional utilizada en la validación del portal.

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

Esta ejecución comprueba cobros por reinicio de ciclo (`billing_reason=subscription_update`); no equivale a avanzar un mes mediante un reloj de Stripe. La clave restringida permite consultar relojes, pero no crearlos. Queda pendiente esa simulación temporal y la validación de capacidades en una sesión renovada: el token de onboarding expiró y la consulta protegida respondió 401.

La factura inicial expuso un orden posible de webhooks: `invoice.paid` llegó antes de vincular la suscripción. Se agregó resolución por metadata de la factura únicamente para registros PENDIENTE_PAGO sin vínculo, verificando empresa y cliente Stripe. Tres regresiones prueban el cobro anticipado y rechazan empresa o cliente distintos. Suite dirigida: 34 pruebas aprobadas.

- Cambio temporal Profesional → Expansión y restauración a Profesional mediante Stripe en modo prueba, sin prorrateo ni nuevos cargos. Ambos webhooks fueron procesados; los avisos de cambio 255 y 256 quedaron ENVIADA, con un intento y sin error. La suscripción del negocio quedó ACTIVA y con renovación habilitada.
- Fallo de renovación, recuperación y vencimiento se verificaron con pruebas automatizadas del backend; esta ejecución no simula un rechazo real de tarjeta ni adelanta el reloj del tenant compartido.
- Se comprueba que los reintentos de una factura fallida conservan el plazo de gracia y la referencia de deduplicación del aviso. El procesador de vencimientos no modifica la política de acceso; si falla la preparación del aviso, no marca al negocio como notificado y permite reintentarlo.
- Los precios y planes de la prueba se resolvieron desde el catálogo configurado, sin modificar las reglas del frontend.

Desde `backend`, con JDK 21:

```sh
mvn -q -Dtest=ServicioCapacidadesEmpresaTest,ServicioStripePlataformaTest,ServicioStripeWebhookTest,ServicioPlanSuscripcionStripeTest test
```
