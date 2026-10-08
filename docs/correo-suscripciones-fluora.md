# Correos de suscripción de Fluora

Los avisos SaaS se envían con la identidad de Fluora, independientemente del proveedor de correo configurado por cada negocio para sus clientes. El destinatario pertenece a la suscripción del tenant y puede modificarse desde **Plan Fluora → Avisos de facturación**. El backend valida y guarda la dirección.

## Eventos

- Inicio del período de prueba al registrar el negocio o al confirmarlo Stripe.
- Factura pagada: importe realmente pagado y referencia de Stripe.
- Cambio de plan confirmado por Stripe.
- Factura fallida y fecha de gracia, cuando corresponde.
- Cancelación programada y cancelación definitiva.
- Vencimiento de prueba, gracia o acceso programado: proceso periódico por lotes, sin depender de que llegue otro webhook.

Los eventos se guardan en la bandeja de salida dentro de la transacción de la suscripción. El trabajador existente envía el correo y aplica sus reintentos. La clave única evita encolar de nuevo la misma factura o aviso. No hay llamadas SMTP dentro de la transacción de pago.

El estado `ENVIADA` representa aceptación del envío por el proveedor, no confirmación de entrega en la bandeja del destinatario. Los reintentos después de un corte de conexión SMTP tienen la limitación habitual de entrega al menos una vez: el servidor puede haber aceptado un mensaje antes de que el cliente reciba la respuesta.

## Configuración

La migración V114 agrega el contacto de facturación, el marcador de vencimiento y la clave de deduplicación. Los registros anteriores conservan sus datos y requieren configurar su correo de facturación si aún no lo tienen.

Para SMTP, establecer en el entorno:

```dotenv
SUSCRIPCIONES_CORREO_PROVEEDOR=SMTP
SUSCRIPCIONES_CORREO_REMITENTE=
SUSCRIPCIONES_CORREO_SMTP_HOST=
SUSCRIPCIONES_CORREO_SMTP_PORT=465
SUSCRIPCIONES_CORREO_SMTP_USERNAME=
SUSCRIPCIONES_CORREO_SMTP_PASSWORD=
SUSCRIPCIONES_CORREO_SMTP_SSL=true
SUSCRIPCIONES_CORREO_SMTP_START_TLS=false
```

El alias de salida y la cuenta que autentica SMTP pueden ser diferentes, siempre que el proveedor autorice ese alias. Con puerto 587, configurar STARTTLS según los datos del proveedor. No se desactiva la validación de certificados.

Para SendGrid, usar `SUSCRIPCIONES_CORREO_PROVEEDOR=SENDGRID` y la configuración de correo de plataforma existente. Si se define un remitente específico de suscripciones, debe estar autorizado en el proveedor.

Las fechas del correo se convierten desde UTC a la zona horaria guardada para el negocio. Los precios, planes, destinatarios y fechas provienen del backend.

## Envío externo explícito de ejemplos

`CorreoSuscripcionesEnvioRealTest` permanece desactivada salvo que exista `FLUORA_CORREO_PRUEBA_DESTINATARIO`. Envía siete ejemplos claramente marcados como pruebas. No modifica suscripciones ni registra pagos y no debe activarse en CI.

Las pruebas normales de plantillas, contacto, aislamiento SMTP y deduplicación no envían correos externos.
