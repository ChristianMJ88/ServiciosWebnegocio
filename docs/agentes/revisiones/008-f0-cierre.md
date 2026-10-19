# Revisión 008-F0: correcciones y cierre del código

**Veredicto: apruebo el cierre del código de F0, con las limitaciones que indico abajo.** A1 queda corregido con su prueba. M2, M3, B1, B2, B3 y B5 se ven en el código. M1 lo acepto con la evidencia SQL de Codex. No ejecuté ninguna suite. Solo leí archivos (Read, Glob y Grep) y no modifiqué nada, así que esta revisión no está guardada. Puede copiarse en `revisiones/008-f0-cierre.md`.

## Correcciones confirmadas

- **A1:** el pie empieza en `null` mientras `router.navigated` es falso, y `null` no es elegible (`footer.component.ts:21-22`). La spec cubre el caso que pedía: `url='/'` sin navegación no hace la petición y, después de `NavigationEnd`, la hace (`footer.component.spec.ts:22`). También cubre `/e/x`, el host propio y el contacto del tenant (`:24-26`).
  - Revisé un caso que podía quedar suelto: que el pie se cree con una navegación en curso. No ocurre, porque `app.html:10` solo crea el pie cuando cambia `mostrarChromePublico`, y eso pasa en `NavigationEnd` (`app.ts:47-48`).
- **M2:** `ContactoSeguridadTest.java` usa `ConfiguracionSeguridad`, el filtro JWT real, tokens firmados y la cadena `springSecurityFilterChain` como filtro. Comprueba:
  - sin token → 401;
  - tenant con `PLATAFORMA_MARKETING_GESTIONAR` → 403;
  - plataforma sin el permiso → 403, sin llegar al servicio (`verifyNoInteractions`);
  - plataforma con el permiso → 200;
  - contacto legal con y sin token de tenant, con `X-Tenant-Id: 999` → mismo cuerpo, con comparación estricta.

  La spec del home cubre 200, 404 y host propio, y comprueba que no se pide `contacto-legal`.
- **M3:** se añadieron tres peticiones: dos GET públicos en «00 Humo» (`agenda-web…json:693-734`) y el 403 del tenant en «01 Seguridad» (`:1302-1327`). Las tres están dentro de las carpetas que corre el runner.
- **B1:** el `%` ya no está en la expresión regular (`ServicioSoportePlataformaImpl.java:60`), y el caso `a%3fbcc%3dx@…` se prueba en los dos campos (`ServicioSoportePlataformaTest.java:35`).
- **B2:** `ManejadorSolicitudSoporte` solo aplica a `ControladorSoportePlataforma`. Devuelve 400 con un mensaje fijo y sin `ex.getMessage()`. La prueba está en `ControladorContactoTest.java:42-48`.
- **B3:** la prueba MySQL usa:
  - el `ServicioAuditoriaPlataforma` real y sus repositorios JPA reales;
  - una FK hacia `plataforma_usuario`;
  - `hbm2ddl=validate` sobre las dos entidades;
  - una barrera después de `findById` que obliga a que las dos lecturas ocurran antes de escribir.

  Resultado esperado: un 200 y un 409, una sola fila de auditoría y `version=1`. El 409 sale de `ObjectOptimisticLockingFailureException`, gracias al interceptor de traducción.
- **B5:** el aviso ya está en el editor (`support-editor.component.html:14`).

## Configuración del runner (solo copia `fluora_test_011_*`)

El aislamiento es correcto:
- La URL de origen tiene que ser local y apuntar a una base `fluora_test_*`.
- Se comprueban las concesiones (solo sobre `` `fluora\_test\_%`.* ``, sin GRANT OPTION ni roles).
- La base se crea con un nombre aleatorio `fluora_test_011_<hex>`.
- `ApiAisladaApplication.main` vuelve a llamar a `MysqlAislado.comprobar` antes de que arranque Spring y, por lo tanto, Flyway.
- Del entorno solo se pasan `PATH`, `HOME`, etc. No se hereda ninguna variable `SPRING_FLYWAY_*` y el perfil `api-aislado` no carga la `.env` (solo `application-dev.yml:3` la importa).
- La base se borra en `finally` con una comprobación de nombre y luego se verifica que ya no existe.

Observaciones:
- **M-a (media, defensa adicional):** la condición «solo `fluora_test_011_*`» la garantiza el script, no el backend. `MysqlAislado` acepta cualquier `fluora_test_*`, incluida la plantilla `fluora_test_base`. Ahora que Flyway escribe, si alguien arranca `ApiAisladaApplication` a mano con la URL de la plantilla, le aplicaría V140 y dejaría de estar vacía.
  - *Propuesta:* en `ApiAisladaApplication.main`, exigir que `DATABASE()` cumpla `fluora_test_011_[a-z0-9]+` antes de `SpringApplication.run`.
  - *Prueba:* un test unitario de esa guarda que rechace `fluora_test_base`.
- **B-a (baja):** el runner no comprueba qué migraciones quedan pendientes en la copia antes de migrar. Propongo consultar `MAX(version)` en `flyway_schema_history` y abortar si no es 139. Así queda demostrado que la corrida solo aplica V140.
- **Nota:** `--spring.flyway.enabled=true` ya era el valor por defecto (`application.yml:21`, y `api-aislado` no tiene archivo de perfil). Pasarlo es explícito, no cambia el comportamiento. El texto de la solicitud («Flyway true únicamente después de…») no debe leerse como que antes estaba apagado.

## Contratos GET de Postman

- **Contacto legal:** comprueba que las claves sean exactamente `['correo']`. Correcto.
- **Soporte público:** compara el conjunto exacto de claves, así que fallaría si aparecieran `version`, `visible`, `actualizado*` o `correoLegal`. Correcto. Da 200 porque la semilla de V140 tiene `visible=TRUE` y la copia sale de una base sin V140. Si en el futuro el origen local tiene V140 con `visible=false`, este caso fallará por los datos, no por un error.
- **403 del tenant:** `token_admin_a` probablemente no tiene el permiso de plataforma, así que esta petición no prueba el caso «incluso con el mismo permiso». Ese caso sigue cubierto solo por Java. Además, las dos aserciones son iguales.
- **B-b (baja):** propongo cuatro mejoras:
  - comprobar `Cache-Control: max-age=60, public` en los dos GET públicos;
  - repetir una petición con `X-Tenant-Slug: {{slug_a}}` y comparar el cuerpo;
  - en el 403, comprobar que el cuerpo no trae `correoLegal` ni `version`;
  - quitar la aserción duplicada.

## Pendientes y limitaciones

- **B4 sigue abierto:** la página de privacidad todavía tiene varios `role="status"` (`legal-page.component.html:16,34`). Es menor y se puede aceptar.
- **B3:** la tabla de auditoría está escrita a mano en la prueba, no viene de la migración real. Además, el `ObjectMapper` de la prueba no es el de Spring Boot: este escribe las fechas como ISO y el de la prueba probablemente como arrays. La prueba demuestra que la transacción, la FK y el conflicto funcionan, pero no el formato exacto del JSON de auditoría en producción.
- **M2:** el «sin token» del público manda `Authorization` vacío en vez de omitir la cabecera. Para el filtro da lo mismo, pero no es el caso literal.
- **M1:** solo puedo leer archivos, así que no verifiqué el estado de Flyway en la base habitual. Me baso en la consulta que reporta Codex (V139 con success=1, sin V140). Antes de volver a arrancar el backend habitual hace falta que el usuario autorice explícitamente aplicar V140 en local, y dejarlo anotado en ESTADO.
- **Cifras de las suites:** las de backend (621/620/1 omitida, 157 suites), Angular (210/70), las guardas (8/8) y el build no las ejecuté. Las tomo de lo que reporta Codex.
- **No están cerrados:** la revisión visual y la corrida completa de Postman siguen en curso, así que F0 no está completo. El cierre del código no incluye la corrida de Postman. Tampoco cubre commits, push, despliegue ni el PUT a las colecciones remotas, que según el README necesita una corrida verde, mi revisión y la comparación con el respaldo.

**Para el cierre total de F0:** el resultado de la corrida completa de Postman, la revisión visual y el registro de M1 en ESTADO. M-a y B-a son recomendables antes de la siguiente corrida con Flyway, pero no bloquean el cierre.
