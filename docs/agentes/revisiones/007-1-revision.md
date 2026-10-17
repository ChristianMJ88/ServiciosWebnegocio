# Revisión 007-1: implementación (Claude)

Revisé el código en el working tree: el módulo `politicas`, V137/V138, el checkout (crear orden, pagar, aprobar), las guardas de activación, folios, correos, el bot, la página pública y el checkout en Angular, y las pruebas nuevas de política, aceptación y MySQL. No pude ejecutar `git diff` (solo tengo Read/Glob/Grep), así que leí los archivos directamente. Tampoco pude guardar esto en `revisiones/007-1.md`: Codex tendría que registrarlo.

**Lo que ya cumple el diseño:**
- **Política:** la entidad es inmutable y la publicación hace lock de empresa, compara `versionBase`, no crea versión si el contenido es igual y deja auditoría.
- **Endpoint público:** da el mismo 404 en los tres casos, no expone ids y tiene caché de 60 s.
- **Página pública:** muestra el texto por interpolación, sin HTML.
- **Admin:** la empresa sale del token.
- **Guardas:** `habilitado` y el paso de `requiereAnticipo` a true las tienen en sus únicos dos puntos de escritura, y la compuerta efectiva está en `obtenerPublica`, `puedeCobrarOnline` y `puedeCobrarProductosOnline`.
- **Folios:** se generan en los dos únicos puntos donde se crea una cita, con `@PrePersist` de respaldo.
- **Bot:** busca el folio solo entre las citas del teléfono.
- **V138:** se puede repetir, porque solo toca filas con `folio IS NULL`.

No encontré ningún bypass de la casilla ni de la política en el código. Las suites y la revisión visual siguen en curso, así que esto no cierra la entrega.

## Alta: condiciona el despliegue, no el commit

**A1. El texto publicado promete funciones que 007-1 no tiene.**
- `ServicioPoliticaEmpresa.java:123-132` dice: «puedes solicitar reagendar dentro de N días… el crédito se aplica…», y habla de crédito o reembolso si cancela el negocio.
- Custodia, reagendado con crédito y la elección P7 son de 007-2/3. Si 007-1 sale sola con checkout activo, un no-show con `CREDITO_REAGENDAR` se trataría como hoy, contra lo que el cliente aceptó. Esa aceptación es justo la evidencia ante un contracargo.
- **Recomendación:** no desplegar 007-1 sin 007-2/3. Si se quiere antes, el texto debe describir solo lo que el sistema hace. **Decide el usuario.**
- La redacción de la plantilla `ANTICIPOS_V1` sigue pendiente de su aprobación (R3) y de su asesor.

## Media

**M1. Una escritura vieja puede borrar la aceptación de una cita.**
- `CitaEntidad.java:19-20` deja las dos columnas actualizables y la entidad no tiene `@Version` ni `@DynamicUpdate`. Hibernate reescribe todas las columnas en cada guardado.
- Si un proceso cargó la cita sin bloqueo antes de la aprobación y la guarda después, deja `NULL/NULL`. El CHECK lo permite porque los dos campos van juntos.
- Es el mismo riesgo que ya existe con `estado`, pero aquí borra evidencia.
- **Recomendación:**
  - marcar ambas columnas `updatable=false`;
  - escribirlas con un UPDATE nativo: `SET politica_version_id=?, politica_aceptada_en=? WHERE id=? AND empresa_id=? AND politica_version_id IS NULL`;
  - registrar el aviso cuando afecte 0 filas.
  
  Así «no reescribir» queda garantizado por la base de datos.
- **Prueba:** una cita cargada antes y guardada después de `acreditarAceptacion` conserva la aceptación.

**M2. Cambió el orden de bloqueos en la aprobación (riesgo de deadlock).**
- En `ServicioEstadoPagoCheckout.java:58-59`, `acreditarAceptacion` bloquea prestadores y citas **antes** de `inventario.confirmarSiDisponible`, que bloquea la existencia del producto.
- Las órdenes sin aceptación (legadas) siguen el orden anterior: existencia → prestador → cita. Dos webhooks con el mismo producto y el mismo prestador pueden bloquearse entre sí: MySQL revierte uno y queda reintentar.
- **Recomendación:** mover la llamada justo después de `confirmarSiDisponible` y antes de `citas.confirmar`, sin depender de `stock`. Se sigue copiando aunque haya incidencia y se recupera el orden original: orden → intento → existencias → prestadores → citas.

**M3. Saldar una cita legada le pone una aceptación que no cubre su anticipo. Decide el usuario.**
- Una cita con anticipo pagado antes de esta entrega, sin aceptación, que después se salda online, recibe la aceptación de esa segunda orden (`ServicioAceptacionPoliticaCheckout.java:64-66`).
- 007-2 aplicaría entonces custodia o retención a un anticipo que se pagó sin política.
- **Opciones:**
  - (a) copiarla solo si la cita no tiene pagos aprobados anteriores; la aceptación queda en la orden;
  - (b) copiarla siempre.
- Recomiendo (a), porque coincide con «no inventar aceptación».

**M4. V138 bajo Flyway: el camino de falla no está probado.**
- `MigracionesPoliticaMysqlTest` corre los scripts con `ScriptUtils` (autocommit), no con Flyway, y solo prueba el éxito.
- V138 abre y cierra su propia transacción (`START TRANSACTION`/`COMMIT`) dentro de la que maneja Flyway.
- **Recomendación:**
  - una prueba con Flyway sobre MySQL temporal que fuerce una guarda fallida (por ejemplo, una huella alterada) y confirme que `folio` sigue NULL y la migración queda fallida;
  - que el ensayo A1 sea sobre un clon con los mismos flags de Cloud SQL: GTID activado, `binlog_format=ROW` (lo exige `RANDOM_BYTES`) y temporales dentro de la transacción.
- V137 no tiene rollback, así que una falla requiere `flyway repair`. Conviene dejarlo en el plan de despliegue.

**M5. En el frontend, un 409 al pagar deja al cliente atascado.**
- `checkout.component.ts:452-455` solo muestra el error. La orden sigue cargada y la casilla no se puede volver a marcar.
- Solo `confirmOrder` (`:163-166`) genera una clave nueva y recarga la configuración.
- **Recomendación:** ante un 409 de `pay` en una orden con citas, borrar `order`, generar nuevo `orderKey` y `paymentKey`, desmarcar la casilla y recargar la configuración.
- **Prueba:** un componente que recibe 409 al pagar puede crear una orden nueva.

## Baja

- **B1. Reintento rechazado por error.** `validarReintento` (`:40-43`) decide por `request.citaIds()` y no por las líneas de la orden. Si todas las citas son SIN_COBRO y hay productos, la orden se crea sin aceptación y el reintento con la misma clave da 409, porque el frontend envía la versión cuando `hasAppointments()`. Conviene decidir por si la orden tiene líneas `SERVICIO`.
- **B2. Enlace a una política inexistente.** Los correos (`ServicioCorreoCitas.java:67-68`, `ServicioCorreoEventosCita.java:206,232`) enlazan la política aunque la empresa no tenga versión publicada, y el cliente llega a «Política no disponible». Incluir el enlace solo si existe una versión vigente.
- **B3. Validación y texto de la política.**
  - `validar` no pone límites superiores a los días ni a `reagendadosMax`. Sugiero, por ejemplo, aclaración ≤ 365 y un tope razonable de reagendados, sin fijarlo en 1 (decisión P8).
  - En `:124` el texto dice «vez/veces»; conviene pluralizar.
- **B4. Respuesta 500 en publicaciones simultáneas.** Si fallara el lock, el UNIQUE devolvería un 500. Convertir `DataIntegrityViolationException` en 409.
- **B5. Folio sin prefijo en el bot.** `resolverCitaPorOpcion` exige escribir «FL-». El diseño proponía añadirlo si falta, con cuidado: un folio de solo dígitos se confunde con un id.
- **B6. Pruebas MySQL que faltan:**
  - CHECKs de `politica_empresa` por modo;
  - CHECKs de `ajuste_saldo_cita`: tipo, destino, reversión única y FK compuesta entre empresas;
  - FK compuesta de `checkout_orden` con otra empresa;
  - una integración de `aplicar` con producto + cita que fije el orden de bloqueo de M2.
- **B7. Empresa sin sitio configurado.** `obtener` da 404 si la empresa no tiene sitio, y `publicar` se revierte (`:53,104`). Tiene sentido porque el checkout exige slug, pero conviene un mensaje claro en el editor.
- **Previo a esta entrega, no lo introdujo 007-1:** `bloquearPrestadores` lanza 409 si falta el prestador. Pasa también en `avisarIncidencia` y `confirmar`, y en la rama APROBADO eso revierte el registro del pago aprobado (el mismo patrón que H1 de 002). Queda anotado.

## Plantillas de WhatsApp sin folio (te toca decidir)

Las plantillas no llevan id ni folio en sus variables; `cita.getId()` en `ServicioMensajesWhatsappCitas.java:101-245` solo se usa en logs. Los mensajes libres y el bot ya usan el folio.

**Mi recomendación es dejarlo pendiente de configuración, sin bloquear 007-1.** Habría que resolverlo antes de 007-3, cuando el folio sirva para reagendar. Dar de alta plantillas nuevas en el proveedor es una acción externa que nadie ha autorizado.

## Validación antes del cierre

- Suite backend completa con MySQL temporal y suite Angular, después de corregir M1, M2 y M5.
- La prueba de falla de V138 con Flyway (M4).
- Revisión visual del editor, la página pública y el checkout con la casilla.

## Decisiones pendientes del usuario

- A1: cuándo desplegar y la redacción del texto de la política.
- M3: si se copia la aceptación al saldar citas legadas.
- Plantillas de WhatsApp: bloqueante o pendiente de configuración.
