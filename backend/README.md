# Backend de Agenda

Base inicial del backend Spring Boot para evolucionar la plataforma de agenda de servicios.

## Stack

- Java 21
- Spring Boot
- Spring Security con JWT
- Spring Data JPA
- Flyway
- MySQL
- Docker

## Estructura

```text
backend/
  src/main/java/com/techprotech/agenda
    compartido/
    seguridad/
    modulos/
      autenticacion/
      sucursales/
      servicios/
      disponibilidad/
      citas/
  src/main/resources/
    application.yml
    db/migration/
```

## Arranque local

1. Levanta MySQL:

```bash
docker compose up -d mysql
```

MySQL queda expuesto localmente en `localhost:3302` para no chocar con instalaciones que ya usan `3306`.

La imagen local recomendada es `mysql:8.0` para evitar el warning de compatibilidad que Flyway muestra con `8.4`.

2. Ejecuta el backend:

```bash
mvn spring-boot:run
```

Spring Boot usa configuracion en:

- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`

No hay `application.properties` porque en este backend se esta usando formato YAML.

Si ya habias levantado Docker con una version distinta de MySQL, puede que necesites recrear el volumen:

```bash
docker compose down -v
docker compose up -d mysql
```

Si MySQL deja de arrancar con errores tipo `Invalid pid in unix socket lock file` o `mysqlx.sock.lock`, normalmente basta con recrear solo el contenedor y conservar el volumen:

```bash
docker compose rm -sf mysql
docker compose up -d mysql
```

## Endpoints iniciales

- `GET /actuator/health`
- `GET /api/v1/publico/sucursales`
- `GET /api/v1/publico/servicios`
- `GET /api/v1/publico/disponibilidad/franjas`
- `POST /api/v1/publico/citas`
- `POST /api/v1/publico/whatsapp/twilio/webhook`
- `POST /api/v1/auth/iniciar-sesion`
- `POST /api/v1/auth/registrar-cliente`
- `POST /api/v1/auth/refrescar-token`
- `POST /api/v1/auth/cerrar-sesion`

## Produccion

1. Copia el ejemplo de variables:

```bash
cp .env.example .env
```

2. Ajusta secretos, base de datos y origenes CORS.

3. Levanta el backend:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Notas:

- En `prod`, Hibernate queda en `validate`.
- El frontend Angular ya esta preparado para trabajar en modo `backend-first`.
- Si necesitas una transicion temporal, puedes activar `allowLegacyFallback` en `src/environments`.

## Correo de confirmacion

El backend ya procesa confirmaciones por correo de forma asincrona usando la tabla `bandeja_salida_notificacion`.

Variables globales recomendadas:

- `CORREO_HABILITADO=true`
- `CORREO_REMITENTE_POR_DEFECTO=no-reply@tu-dominio.com`
- `CORREO_NOMBRE_REMITENTE_POR_DEFECTO=Agenda Web`
- `CORREO_RESPONDER_A_POR_DEFECTO=soporte@tu-dominio.com`
- `CORREO_LLAVE_CIFRADO_SECRETOS=una-llave-larga-por-entorno`
- `CORREO_LOTE_MAXIMO_OUTBOX=20`
- `CORREO_REINTENTO_SEGUNDOS=60`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_SMTP_AUTH`
- `MAIL_SMTP_STARTTLS`

Multitenancy:

- La identidad del remitente y el proveedor de envio (`SMTP` o `GRAPH`) pueden configurarse por empresa en `configuracion_correo_empresa`.
- Si una empresa no tiene configuracion propia, se usa el fallback global.
- La cita ya no espera a que el correo se envie en el request HTTP; solo deja el evento en outbox.
- `smtp_password` y `graph_client_secret` ya soportan formato cifrado con prefijo `enc:v1:`. Si detectan texto plano, siguen funcionando temporalmente para facilitar migracion.
- El modulo admin expone:
- `GET /api/v1/admin/configuracion-correo`
- `PATCH /api/v1/admin/configuracion-correo`
- `POST /api/v1/admin/configuracion-correo/migrar-secretos`

Ejemplo SMTP por tenant:

```sql
INSERT INTO configuracion_correo_empresa (
  empresa_id,
  habilitado,
  remitente,
  nombre_remitente,
  responder_a,
  smtp_host,
  smtp_port,
  smtp_username,
  smtp_password,
  smtp_auth,
  smtp_starttls
)
VALUES (
  1,
  TRUE,
  'citas@tenant.com',
  'Tenant Uno',
  'soporte@tenant.com',
  'smtp.tenant.com',
  587,
  'tenant-user',
  'tenant-pass',
  TRUE,
  TRUE
);
```

Recomendacion:

- Migra `smtp_password` a formato cifrado antes de usar credenciales reales de produccion.
- Usa una `CORREO_LLAVE_CIFRADO_SECRETOS` distinta por ambiente.
- Para Microsoft Graph configura una app en Microsoft Entra con permiso de aplicacion `Mail.Send`, consentimiento de administrador y un mailbox valido en `graph_user_id`.

Ejemplo Graph por tenant:

```sql
INSERT INTO configuracion_correo_empresa (
  empresa_id,
  habilitado,
  proveedor,
  remitente,
  nombre_remitente,
  responder_a,
  graph_tenant_id,
  graph_client_id,
  graph_client_secret,
  graph_user_id
)
VALUES (
  1,
  TRUE,
  'GRAPH',
  'no-reply@notificaciones.tecprotech.com.mx',
  'TecProTech',
  'soporte@tecprotech.com.mx',
  'tu-tenant-id',
  'tu-client-id',
  'tu-client-secret',
  'no-reply@notificaciones.tecprotech.com.mx'
);
```

Fallback global opcional en `application.yml`:

- `CORREO_PROVEEDOR_POR_DEFECTO=GRAPH`
- `CORREO_GRAPH_TENANT_ID=...`
- `CORREO_GRAPH_CLIENT_ID=...`
- `CORREO_GRAPH_CLIENT_SECRET=...`
- `CORREO_GRAPH_USER_ID=no-reply@notificaciones.tecprotech.com.mx`

Para pruebas locales puedes usar un SMTP de desarrollo como MailHog o Mailpit.

## WhatsApp con Twilio

El backend ya puede atender un webhook entrante de Twilio para operaciones de citas por WhatsApp y tambien enviar confirmaciones y recordatorios por outbox.

Variables recomendadas:

- `WHATSAPP_HABILITADO=true`
- `WHATSAPP_EMPRESA_ID=1`
- `TWILIO_ACCOUNT_SID=...`
- `TWILIO_AUTH_TOKEN=...`
- `TWILIO_WHATSAPP_FROM=whatsapp:+14155238886`
- `TWILIO_MESSAGING_SERVICE_SID=MG...`
- `TWILIO_WHATSAPP_STATUS_CALLBACK_URL=https://tu-dominio/api/v1/publico/whatsapp/twilio/status`
- `TWILIO_WHATSAPP_TEMPLATE_SOLICITUD_CONFIRMACION_SID=HX...`
- `TWILIO_WHATSAPP_TEMPLATE_REPROGRAMADA_PENDIENTE_SID=HX...`
- `TWILIO_WHATSAPP_TEMPLATE_RECORDATORIO_CONFIRMACION_SID=HX...`
- `TWILIO_WHATSAPP_TEMPLATE_CITA_CONFIRMADA_SID=HX...`
- `TWILIO_WHATSAPP_TEMPLATE_RECORDATORIO_SID=HX...`
- `TWILIO_WHATSAPP_TEMPLATE_CANCELACION_SID=HX...`
- `WHATSAPP_LOTE_MAXIMO_OUTBOX=20`
- `WHATSAPP_REINTENTO_SEGUNDOS=60`
- `WHATSAPP_RECORDATORIO_HORAS_ANTES=24`
- `WHATSAPP_RECORDATORIO_VENTANA_MINUTOS=60`

Pruebas:

- Si todavia no tienes un remitente de WhatsApp habilitado, usa el Sandbox de Twilio con `TWILIO_WHATSAPP_FROM=whatsapp:+14155238886`.
- En sandbox, el numero destino debe unirse primero al sandbox antes de poder recibir mensajes.

Webhook a configurar en Twilio:

- `POST /api/v1/publico/whatsapp/twilio/webhook`
- `POST /api/v1/publico/whatsapp/twilio/status` para callbacks de entrega si no envias `TWILIO_WHATSAPP_STATUS_CALLBACK_URL` por mensaje

Comandos soportados por WhatsApp:

- `AYUDA`
- `SUCURSALES`
- `SERVICIOS <sucursalId>`
- `HORARIOS <sucursalId> <servicioId> <AAAA-MM-DD>`
- `AGENDAR|<sucursalId>|<servicioId>|AAAA-MM-DD HH:mm|Nombre completo|correo@dominio.com|Notas opcionales`
- `MIS CITAS`
- `CONFIRMAR <folio>`
- `CANCELAR <folio>`
- `REAGENDAR|<folio>|AAAA-MM-DD HH:mm`

Notas:

- Las confirmaciones de cita creadas desde el backend se encolan en `bandeja_salida_notificacion` con canal `WHATSAPP`.
- La fila de outbox ahora guarda `proveedor_mensaje_id`, `estado_entrega`, `codigo_error_proveedor` y `detalle_error_proveedor` para rastrear el ciclo de entrega de Twilio.
- Cuando una cita se registra, el backend puede usar una plantilla distinta para solicitar que el cliente confirme su asistencia. Si esa plantilla no esta configurada, hace fallback a texto libre con `CONFIRMAR <folio>`.
- Cuando una cita se reprograma y vuelve a `PENDIENTE`, el backend puede usar una plantilla especifica para pedir confirmacion nuevamente. Si no esta configurada, hace fallback a texto libre.
- Cuando una cita sigue `PENDIENTE` y se acerca la fecha, el backend puede usar una plantilla distinta para recordar al cliente que aun falta confirmar.
- Cuando una cita pase a estado `CONFIRMADA`, el backend puede usar una plantilla de Twilio por `ContentSid` con variables `1 nombre`, `2 fecha`, `3 hora`.
- Cuando la cita se cancela desde administracion, el backend puede usar una plantilla de cancelacion iniciada por el negocio.
- Los recordatorios se programan automaticamente para citas `PENDIENTE` o `CONFIRMADA` dentro de la ventana configurada.
- El cliente se identifica por numero telefonico y debe tener `acepta_whatsapp = true`.
