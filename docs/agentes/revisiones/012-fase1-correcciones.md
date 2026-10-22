# Revisión 012 fase 1: correcciones y migración inmutable

Solo leí archivos: no ejecuté comandos, pruebas ni consultas. Los resultados de las suites son de Codex y no los comprobé. Tampoco pude guardar este texto en el canal; si lo quieres ahí, Codex puede copiarlo a `revisiones/012-fase1-correcciones.md`.

**Conclusión:** el callback es seguro y Spring lo registra bien. Ninguna corrección bloquea seguir. Hay que ajustar dos cosas del callback (C1, C2) y la planificación al hacer `conectar` (H1) antes de activar el job.

## Callback `PrepararLegadoDominiosFlyway`

**Registro: correcto, pero ninguna prueba lo demuestra.**
- Con Spring Boot 3.4.3 (`pom.xml:10`), la configuración automática de Flyway recibe todos los beans `Callback`. La clase es `@Component` dentro del paquete que escanea `AgendaApplication`.
- No hay otro `Flyway`, `FlywayMigrationStrategy`, `FlywayConfigurationCustomizer` ni plugin Maven. `application-prod.yml` y `application-dev.yml` no cambian `spring.flyway`.
- El runner de Postman arranca con el contexto de `AgendaApplication`, así que también lo incluye.
- Pero `DominiosMysqlTest.java:45` pasa el callback a mano con `.callbacks(new …)`. Eso prueba el SQL, no el registro.

**Seguridad: correcta.**
- `supports` solo responde a `BEFORE_EACH_MIGRATE` de la versión 141. Donde V141 ya está aplicada no se ejecuta nunca.
- Usa la conexión de la migración y no tiene entradas externas.
- Es idempotente. Como `CREATE TABLE` hace commit implícito en MySQL, el `UPDATE` queda aplicado aunque V141 falle después. Es aceptable, porque V141 de todos modos borra los `dominio_principal`.
- No modifica la migración aplicada. Hay un guard del checksum en `scripts/check-domain-plan-policy.test.mjs:12`.

## Hallazgos

| # | Prioridad | Ubicación | Problema | Corrección y prueba |
|---|---|---|---|---|
| C1 | **Media** | `PrepararLegadoDominiosFlyway.java:13` | **El regex no bloquea todo lo no ASCII, que es justo la falla que debía evitar.** `empresa_sitio_config` usa la intercalación por defecto (`_ci`, creada en V32). En MySQL 8, `REGEXP` con intercalación `_ci` aplica el cierre de mayúsculas de ICU, así que `[a-zA-Z]` acepta `ſ` (U+017F) y `K` (U+212A). `LOWER('ſ')` sigue siendo `ſ`, y el `INSERT` de V141 en la columna `ascii` falla en modo estricto. Además, en ICU `$` también coincide antes de un salto de línea final, así que `x.mx\n` pasa el filtro. | Agregar `OR LENGTH(dominio_principal)<>CHAR_LENGTH(dominio_principal)`, que no depende del regex. Usar `REGEXP_LIKE(…, '…', 'c')` y terminar el patrón en `\z` en lugar de `$`. Agregar `ſ.mx`, `x.mx\n` y `" x.mx"` a los datos de `DominiosMysqlTest`. |
| C2 | Media | `PrepararLegadoDominiosFlyway.java:12-14`, `DominiosMysqlTest.java:45` | **Faltan trazabilidad y dos pruebas.** El callback borra valores de producción sin dejar rastro, mientras V142 sí deja la fila `RETIRADO` con `DOMINIO_LEGADO_INVALIDO`. Ninguna prueba demuestra el registro en Spring. La rama de V142 tampoco se prueba, porque el callback ya limpió esos casos. | Registrar en el log solo el número de filas de `executeUpdate`, sin valores. **Pruebas:** (a) `ApplicationContextRunner` con `FlywayAutoConfiguration` y el bean, verificando que `flyway.getConfiguration().getCallbacks()` lo contiene; (b) migrar hasta 141 sin el callback, con `x.mx.` y `x.mx:443`, y luego a 142: esas filas quedan `RETIRADO` con `DOMINIO_LEGADO_INVALIDO`. |
| C3 | Baja (verificar) | `V142` | **V142 puede estar aplicada ya.** DevTools ya aplicó V141 sola. Si V142 también llegó a `agenda_db` local, corregir el `$` en su regex cambiaría el checksum. | Revisar el historial local antes de tocar V142. Si ya está aplicada, no editarla. Como `hostname` es `ascii_bin`, el único riesgo es `\n`, y basta con dejarlo anotado. |
| H1 | **Media** | `ServicioDominios.java:80`, `PlanificadorChequeosDominio.java:12` | **El backoff acumulado retrasa la conexión.** `intentos_chequeo` y `proximo_chequeo_en` nunca vuelven a cero. Una fila que pasó unas 8 rondas en `PENDIENTE` llega a `CONECTANDO` con `proximo_chequeo_en` hasta 24 h en el futuro, y el job no la comprueba ese día. | Poner `intentos=0` y `proximo=null` al pasar a `VERIFICADO` o `CONECTANDO`. **Prueba:** una fila con 10 intentos, luego `conectar`; queda como candidata en la siguiente ejecución. |
| H2 | Baja | `TareaComprobacionDominios.java:17`, `ServicioDominios.java:53` | **El 429 por frecuencia infla el backoff.** El límite de frecuencia es por empresa. En un mismo lote, la segunda y tercera fila de una empresa reciben 429 y suben `intentos` sin haberse comprobado. | Que el job omita la frecuencia por empresa, o que `aplazar` no incremente los intentos ante un 429. |
| H3 | Baja | `TareaComprobacionDominios.java:17` | **Una excepción inesperada corta el lote.** Cualquier `RuntimeException` que no sea `ResponseStatusException` ni `DataAccessException` (por ejemplo, el `IllegalStateException` de `vista`) detiene el resto del lote. La cola no se bloquea, porque la fila ya se aplazó, pero esa ejecución deja filas sin revisar. | Capturar `RuntimeException` en cada fila y registrar solo el tipo de error, sin el mensaje. |
| H4 | Baja | `ServicioDominios.java:70`, `:71`; `site-domains.component.html:11` | **Restos y textos sin traducir.** La rama de la línea 70 ya no se puede ejecutar porque la línea 67 la cubre. `ultimo_error` mezcla códigos con una frase en español (línea 71). `DOMINIO_LEGADO_INVALIDO` aparece tal cual al tenant. | Quitar la rama muerta. Usar un código como `LIBERADO_POR_OTRA_CUENTA` y traducir ese código y `DOMINIO_LEGADO_INVALIDO` en el frontend. |
| H5 | Baja | `ConflictosHttpDominiosTest.java:12` | **La prueba no cubre el alcance del advice.** `standaloneSetup` registra el advice explícitamente, así que no prueba `basePackageClasses` ni su precedencia frente a `ManejadorGlobalExcepciones`. El cuerpo `{mensaje}` es compatible con `fail()` del frontend. | Opcional: un `@WebMvcTest` con los dos advices. |

## Correcciones que verifiqué

- **Planificación separada:** `aplazar` corre en `REQUIRES_NEW` antes de `verificar`. El `UPDATE` masivo no sube `@Version`. `candidatos` filtra por `proximo_chequeo_en`. La prueba 60+1 con lote de 50 refleja la semántica (`PlanificacionDominiosTest.java`).
- **Advice:** está limitado al paquete `dominios.api`, que incluye el controlador de plataforma, con precedencia máxima. Cubre `ObjectOptimisticLocking…`, deadlocks y esperas de bloqueo vencidas, que son subclases de `PessimisticLockingFailureException`.
- **Auditoría:** solo registra cuando cambian el estado o el error (`:74`).
- **Reserva técnica:** dura `max(15, frecuencia)` (`:71`). Si la fase 2 se revierte, `chequeo_en_curso` queda en verdadero, pero vence con el tiempo.
- **`HOST_EN_USO`:** se asigna antes de evaluar el TXT o un error de red (`:67`).
- **Rangos de red:** `2002::/16`, `2001::/32` y `192.88.99.0/24` quedan excluidos (`ComprobadorHostingHttps.java:48-50`). `192.0.x.x` se excluye completo, lo cual es inofensivo.
- **Marca:** `fluora-host:<sitio>:<hash 64>`. El sitio sale de la configuración del backend con `Pattern.quote`, y el hash no decide la activación. `conectar` responde 503 si falta `DOMINIOS_SITIO_HOSTING`. Del lado del build, el identificador se toma de `.firebaserc` (`targets.…clientes`, exactamente uno, validado por `prepare-clientes-site.mjs:16-19`). La marca no es secreta, pero eso no afecta la seguridad: la propiedad ya se demostró por TXT y conectar lo hace un operador.
- **Frontend:**
  - Enter llama a `preventDefault()` y `request()` no hace nada si hay una operación en curso; hay una prueba.
  - El payload omite `dominioPrincipal` (`admin-site.forms.ts:86`) y su prueba lo confirma.
- **Prueba MySQL:** `ServicioCapacidadesEmpresa` es real, con N+1 → 200/402 y el mismo host → 200/409 (`DominiosMysqlTest.java:58,72,74`).

## Orden sugerido

C1 y C2, revisando antes C3, luego H1, y después las bajas. Costo, canonical, CORS y emulador siguen para §6, como acordamos. No se tocó producción ni el historial de Flyway.
