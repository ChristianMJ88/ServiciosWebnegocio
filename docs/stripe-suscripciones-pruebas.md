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

- Cambio temporal Profesional → Expansión y restauración a Profesional mediante Stripe en modo prueba, sin prorrateo ni nuevos cargos. Ambos webhooks fueron procesados; los avisos de cambio 255 y 256 quedaron ENVIADA, con un intento y sin error. La suscripción del negocio quedó ACTIVA y con renovación habilitada.
- Fallo de renovación, recuperación y vencimiento se verificaron con pruebas automatizadas del backend; esta ejecución no simula un rechazo real de tarjeta ni adelanta el reloj del tenant compartido.
- Se comprueba que los reintentos de una factura fallida conservan el plazo de gracia y la referencia de deduplicación del aviso. El procesador de vencimientos no modifica la política de acceso; si falla la preparación del aviso, no marca al negocio como notificado y permite reintentarlo.
- Los precios y planes de la prueba se resolvieron desde el catálogo configurado, sin modificar las reglas del frontend.

Desde `backend`, con JDK 21:

```sh
mvn -q -Dtest=ServicioCapacidadesEmpresaTest,ServicioStripePlataformaTest,ServicioStripeWebhookTest,ServicioPlanSuscripcionStripeTest test
```
