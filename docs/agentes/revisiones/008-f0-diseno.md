## Revisión 008-F0: coordinación de V140 y contrato

**Veredicto: el diseño está aprobado con cambios.** V140 sigue libre en este working tree: la migración más alta es `V139__partes_previstas_point_y_cambio_pago_venta.sql`. Codex puede crear V140 cuando la línea base 011 (M4) quede en verde y después de repetir `git fetch --all --prune`. Hay dos cambios que hacer antes de implementar: A1 y A2. También corrijo un criterio de aceptación que yo mismo escribí (M1).

Solo tengo herramientas de lectura, así que no guardé esta revisión. Si se quiere en el canal, Codex o el usuario pueden pegarla en `docs/agentes/revisiones/008-f0-diseno.md`.

### Lo que comprobé en el código
- La seguridad de rutas está en `ConfiguracionSeguridad.java:40-42`. `/api/v1/publico/**` es `permitAll` y `/api/v1/plataforma/**` exige `AMBITO_PLATAFORMA`. Las rutas propuestas quedan bien protegidas sin tocar nada aquí.
- El permiso `PLATAFORMA_MARKETING_GESTIONAR` ya existe y solo lo tiene `ADMINISTRADOR_PLATAFORMA` (`V115__landings_marketing.sql:8-12`). El patrón a copiar está en `ControladorLandingPlataforma.java:10-16`: `@PreAuthorize` a nivel de clase y `usuario.usuarioId()`.
- `ServicioAuditoriaPlataforma.registrar(usuarioId, accion, recurso, recursoId, Map)` usa el `save` del repositorio. Dentro de un servicio `@Transactional` corre en la misma transacción. La tabla `plataforma_auditoria.usuario_id` tiene FK a `plataforma_usuario(id)` (`V107:44-55`).
- Para la caché pública ya hay un precedente: `ControladorPoliticaPublica.java:16` usa `CacheControl.maxAge(60s).cachePublic()`.
- No hay SSR: no existen `server.ts` ni `app.config.server.ts`. El riesgo de SEO y CLS de la sección 7 es menor.

### Hallazgos

**A1 (alta). Una carrera en el control optimista acaba en 500, no en 409.** Ningún manejador captura `ObjectOptimisticLockingFailureException`, así que termina en `ManejadorGlobalExcepciones.java:110`, que responde 500 y además devuelve `ex.getMessage()`. La comparación explícita de versión, como en `ServicioLandingMarketingImpl.java:32`, solo cubre una versión vieja leída antes. Dos PUT simultáneos con la misma versión llegan al `@Version` de Hibernate.
- *Corrección:* en el servicio, envolver `saveAndFlush` y traducir esa excepción, y también `DataIntegrityViolationException` si se crea la fila, a `ResponseStatusException(CONFLICT, "La configuración de soporte cambió. Actualiza antes de guardar")`. Debe hacerse antes de llamar a la auditoría.
- *Prueba:* una prueba de servicio en MySQL con dos transacciones y la misma versión. Una devuelve 200 y la otra 409, con una sola auditoría.
- La landing tiene el mismo problema latente. No entra en F0; lo dejo anotado como bajo.

**A2 (alta). El soporte de Fluora puede aparecer en minisitios de tenant.** `FooterComponent.tenantActivo()` es `Boolean(tenant())` (`tenant-context.service.ts:11`). En un host propio o en `/e/:slug`, mientras el tenant no se ha resuelto, vale `false`, y la plantilla actual ya cae a la rama de Fluora (`footer.component.html:97-100`).
- *Corrección:* mostrar el bloque y pedir datos a la API solo si se cumplen las tres condiciones: `!tenantActivo()`, `!platformHost.isCustomTenantHost()` y la ruta no empieza por `/e/`. El servicio compartido no se debe llamar en ningún otro caso.
- Además, la condición de la línea 97 debe ocultar el `<li>` de correo en modo Fluora cuando no hay configuración.
- *Prueba:* con host propio y `tenant=null`, y con `/e/x` y `tenant=null`, no sale ninguna petición HTTP (`HttpTestingController.expectNone`) y el bloque no se muestra.

**M1 (media). Mi criterio de búsqueda en el repo era demasiado amplio** (`008-decisiones.md:88`). Hoy `contacto@refluora.com` aparece en dos sitios que no son soporte:
- `legal-page.component.html:16,34,69,80` es el contacto de privacidad/ARCO, dentro de textos legales que el usuario todavía tiene que aportar;
- `application.yml:170,227` son valores por defecto de variables de entorno para el remitente y el responder-a del correo saliente.

El criterio queda así: **sin coincidencias en el footer, el home/FAQ ni en `backend/src/main/java`; las páginas legales y `application.yml` no entran en F0.** Codex no debe tocarlos y los anota como residuales. Los fixtures de prueba que contienen `2213688033` no son de soporte.

**M2 (media). La validación de zona es demasiado permisiva.** `ZoneId.of` acepta `Z`, `UTC+3` y `+05:00`, y eso no es una zona IANA. Hay que exigir `ZoneId.getAvailableZoneIds().contains(valor)`. *Prueba:* `+05:00` y `UTC+3` devuelven 400, y `America/Mexico_City` devuelve 200.

**M3 (media). El rango del teléfono está ambiguo.** Hay que fijarlo:
- primero, quitar espacios, `()`, `-` y `.`;
- después, validar con `^\+[1-9]\d{7,14}$`, es decir, de 8 a 15 dígitos en total con el código de país, según E.164;
- sin `+` el valor se rechaza con un mensaje que pida el código internacional.

En F0 no se formatea a número nacional ni se agrega libphonenumber. El enlace `tel:` lleva el valor E.164 y en pantalla se muestra el E.164 tal cual. Formatear como «221 368 8033» exige reglas por país, así que queda como posible seguimiento si el usuario lo pide.

### Ajustes al diseño propuesto
1. **Nombre de la tabla:** `plataforma_soporte`, con el prefijo `plataforma_` que usan V107–V115, en lugar de `marketing_soporte_plataforma`.
2. **Una sola fila:** `id TINYINT PRIMARY KEY` con `CHECK (id = 1)`.
3. **Columnas:**
   - `correo VARCHAR(150) NOT NULL`;
   - `telefono VARCHAR(16) NOT NULL`;
   - `dias JSON NOT NULL`, un arreglo de enteros ISO del 1 al 7. Con una sola fila no hace falta tabla de detalle. Java valida, quita duplicados y ordena;
   - `hora_inicio TIME` y `hora_fin TIME`;
   - `zona_horaria VARCHAR(64)`;
   - `visible BOOLEAN`;
   - `actualizado_en DATETIME(6)` en UTC, tomado del `Clock` inyectado (aplica la regla del proyecto contra `now()` sin argumentos);
   - `actualizado_por BIGINT NULL` con FK a `plataforma_usuario(id)`. Es NULL en la fila sembrada;
   - `version BIGINT NOT NULL DEFAULT 0`.
4. **Siembra:** solo en V140, con los valores de `008-decisiones.md:31-36`, el teléfono `+522213688033` y `visible=TRUE`. Nada de esto va en Java ni en HTML.
5. **Mapeo JPA:** la entidad usa `columnDefinition="json"` como `LandingMarketingEntidad`. Hay que comprobarlo con Hibernate `validate`.
6. **Si falta la fila:** el GET de administración devuelve 404 y el PUT con `version=null` la crea con `id=1`, como hace la landing. Una creación concurrente devuelve 409 (ver A1). No hay endpoint DELETE.
7. **Validaciones adicionales:**
   - horas sin segundos y `fin > inicio`, sin rangos que crucen la medianoche;
   - correo con `trim`, en minúsculas, `@Email` y longitud máxima 150.
8. **Auditoría:** acción `SOPORTE_ACTUALIZAR`, recurso `SOPORTE`, con `anterior` y `nuevo`. Usar `LinkedHashMap`, porque `Map.of` falla con valores nulos (el `actualizadoPor` de la fila sembrada es NULL).
9. **DTO público:** solo `correo`, `telefono`, `dias`, `horaInicio`, `horaFin` y `zonaHoraria`. Sin `version`, `visible` ni `actualizado*`.
   - El 200 lleva `max-age=60, public`. El 404 no lleva caché pública, para que activar la visibilidad se note enseguida.
   - El editor avisa que el cambio público puede tardar hasta 60 s.
10. **Formateador en el frontend:**
    - los nombres de los días salen de `Intl.DateTimeFormat('es-MX', {weekday:'long'})`;
    - los días consecutivos se agrupan como «de lunes a viernes» y los demás se unen con `Intl.ListFormat`;
    - la hora se muestra tal como está configurada, sin convertir a la zona de quien visita;
    - la zona se muestra con su nombre IANA o con el nombre de `Intl`, sin cadenas fijas.
11. **Pruebas extra:**
    - token de tenant contra la ruta de plataforma → 403;
    - GET público sin autenticar → 200, y el JSON no contiene campos privados;
    - prueba MySQL de V140 en una BD efímera con el usuario `fluora_tests`: el `CHECK` rechaza `id=2`, la fila sembrada existe y Hibernate `validate` pasa;
    - formateador con `[1,3,5]`, `[6,7]` y `[1..7]`.

Todo lo demás de la solicitud queda confirmado sin cambios:
- el editor standalone en `platform-marketing` muestra el `mensaje` del backend;
- el FAQ se oculta cuando no hay datos;
- el tenant queda intacto;
- las suites completas se corren al final de F0;
- no se editan migraciones ya aplicadas;
- no hay commit, producción ni despliegue de 008.
