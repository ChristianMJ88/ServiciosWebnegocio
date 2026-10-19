# Revisión 008-F0: implementación

**Veredicto: todavía no la apruebo.** Hay 1 hallazgo alto (A2 se cumple solo en parte) y 3 medios. El backend se ajusta al diseño y a la adenda. No ejecuté ninguna suite, así que no doy por buena ninguna prueba sin ver la evidencia final de Codex. No modifiqué archivos. Si quieren guardar esta revisión en el canal, se puede pegar en `revisiones/008-f0-implementacion.md`.

## Lo que se cumple

- **V140** usa la tabla `plataforma_contacto`, `CHECK (id=1)` y la FK `actualizado_por`. Todas las columnas tienen `NOT NULL`, excepto el actor. La siembra solo está en la migración y no usa un `CHECK` de minúsculas.
- **Control optimista (A1):** `ServicioSoportePlataformaImpl.java:43` compara la versión de forma explícita y la línea `:49` convierte `ObjectOptimisticLockingFailureException` y `DataIntegrityViolationException` en 409 antes de auditar (`:51`). La auditoría usa `LinkedHashMap` y la hora sale del `Clock` en UTC.
- **Validaciones:**
  - zona horaria: solo nombres IANA (`:41`);
  - teléfono E.164 según M3 (`:37-38`);
  - horas sin segundos y con fin posterior al inicio;
  - días del 1 al 7, sin duplicados y ordenados;
  - correo con la expresión de L2 en los dos campos (`:58-61`).
- **DTO público y caché:** el DTO público no trae `version`, `visible`, `actualizado*` ni `correoLegal`. El contacto legal no depende de `visible`. Los 200 llevan `max-age=60, public` y el 404 no lleva caché pública.
- **Seguridad:** solo se usa `@PreAuthorize` con `PLATAFORMA_MARKETING_GESTIONAR`, en la ruta `/plataforma/**` ya existente, y el actor es `usuarioId()`.
- **Páginas legales (L1):** los cuatro enlaces y el asunto de eliminación están bien. Mientras carga se ve «Cargando contacto…» y el aviso de no disponible solo sale después de un error. La frase de la línea 95 está dentro de un `@if`.
- **Búsqueda en el repo:** `contacto@refluora.com` solo aparece en V140, `application.yml:170,227`, `backend/README.md` y los fixtures. No hay coincidencias en `src/` ni en `backend/src/main/java`.

## Hallazgos

**A1 (alta): el pie de página pide soporte antes de la primera navegación (A2 en parte).**
- `footer.component.ts:21` empieza la URL con `startWith(this.router.url)`. Mientras no se completa la primera navegación, `router.url` vale `'/'`. `provideRouter` no usa `withEnabledBlockingInitialNavigation` (`app.config.base.ts:12`), y `App.mostrarChromePublico` también vale `true` con `'/'` (`app.ts:27`).
- Por eso, si alguien abre directamente `/e/slug` en el host de la plataforma, `fluoraContact()` vale `true` en la primera detección de cambios. La petición a `/publico/plataforma/soporte` sale, y el bloque de Fluora puede verse un momento si la respuesta llega antes de `NavigationEnd`.
- La prueba `footer.component.spec.ts:13,22` no detecta el caso, porque simula `router.url = '/e/x'` desde el principio.
- *Corrección:* no considerar elegible el pie hasta el primer `NavigationEnd`. Por ejemplo, usar un valor inicial `null` y tratar `null` como no elegible. Otra opción es usar la ruta real del navegador (`Location.path()`).
- *Prueba:* simular `router.url = '/'` con la ruta del navegador en `/e/x` y sin emitir `NavigationEnd` → `expectNone(endpoint)`. Después, emitir `NavigationEnd` en `/` → `expectOne`.

**M1 (media): reiniciar el backend habitual puede aplicar V140 o hacer fallar `validate`.**
V140 ya está en `db/migration` y la entidad nueva está mapeada. Si DevTools recarga la aplicación habitual:
- con Flyway activo, aplica V140, como pasó con V139 (ESTADO 2026-10-17);
- con Flyway desactivado y `ddl validate`, el arranque falla porque falta la tabla.

*Validación:* antes de que corra otra recarga, Codex debe comprobar el estado de Flyway en el proceso habitual y dejar registrado en ESTADO que V140 todavía no está aplicada.

**M2 (media): la cobertura de ámbito y del FAQ es menor que lo declarado.**
- `ControladorContactoTest.java:25-34` comprueba `@PreAuthorize` con `ROLE_ADMIN`, pero no hace pasar un token de tenant por `ConfiguracionSeguridad` (`AMBITO_PLATAFORMA`), que es lo que pedía el diseño (§11).
- La ruta pública con cabeceras de tenant usa `standaloneSetup`, sin filtros.
- No hay ninguna spec en `pages/fluora-home/`, así que nada prueba que el FAQ se oculte cuando no hay datos ni que no llame en un host propio (`fluora-home.component.ts:183`).
- *Prueba:* añadir un 403 con el token de tenant usando el contexto de seguridad, y una spec del FAQ con 200, 404 y host propio.

**M3 (media): falta el GET de contacto legal en Postman.** La adenda lo pedía como petición de solo lectura. No encontré `contacto-legal` ni `marketing/soporte` en `backend/postman/`, `.postman/` ni `scripts/`.

**Bajos**
- **B1:** la expresión del correo acepta `%`, así que `a%3fbcc%3dx@dominio.mx` pasa la validación. Según RFC 6068 se decodifica como parte de la dirección, pero algunos clientes de correo lo interpretan de forma distinta. Recomiendo quitar `%` de la clase de caracteres. Solo un administrador de plataforma puede capturar el valor.
- **B2:** un cuerpo mal formado, por ejemplo `"horaInicio":"25:00"`, provoca `HttpMessageNotReadableException`, que termina en 500 con `ex.getMessage()` (`ManejadorGlobalExcepciones.java:110`). El problema ya existía y es general; lo anoto porque afecta al editor nuevo.
- **B3:** la prueba MySQL usa un stub de la auditoría (`SoportePlataformaMysqlTest.java:65-69`) y crea la tabla de auditoría sin la FK. La serialización real de `LocalTime` y `OffsetDateTime` solo queda cubierta por el `ObjectMapper` de Spring en ejecución, no por esa prueba.
- **B4:** la página de privacidad tiene dos `role="status"` que se anuncian a la vez. Es un detalle de accesibilidad.
- **B5:** después de un 409, «Actualizar» descarta los cambios que no se guardaron. Encaja con el diseño, pero conviene decirlo en el mensaje.

## Límites de esta revisión

- No ejecuté Maven, Angular ni MySQL. Las cifras de las suites quedan pendientes de la evidencia de Codex.
- No pude ver el diff porque solo tengo lectura de archivos; revisé los archivos nuevos y los modificados que se indican.
- No atribuyo a F0 ningún cambio previo de 011, ni de `application.yml` o `AGENTS.md`. Siguen como residuales, sin cambios: `application.yml:170` (`correo-bienvenida.soporte` = `contacto@`) y `revisiones/007.md:164`.

Para aprobar necesito A1 corregido con su prueba, M1 verificado y anotado, y la evidencia final de las suites. M2 y M3 pueden cerrarse en esta misma ronda.
