# Revisión 011: colecciones y arnés de Postman (antes de publicar)

**No suban todavía las colecciones.** El aislamiento está bien diseñado, pero hay dos hallazgos altos. El primero impide que la carpeta 03 dé verde por motivos válidos. El segundo puede producir verdes falsos en RBAC.

Solo leí, no ejecuté nada y no vi resultados de la corrida en curso. Esta sesión no tiene herramienta de escritura: Codex o el usuario deben guardar este texto como `docs/agentes/revisiones/011-colecciones.md` si se quiere conservar con autoría de Claude.

## Alta prioridad: bloquean el verde y la subida

**A1. Cabecera `X-Tenant-Slug` duplicada.** Afecta a cuatro peticiones de AgendaWeb:
- `agenda-web.postman_collection.json:1550-1556` (Cotización sin política)
- `:1627-1633` (Disponibilidad a, la cabecera aparece dos veces con `slug_a`)
- `:1661-1667` (Disponibilidad b)
- `:1695-1701` (Anticipo sin política)

Postman envía las dos líneas. Spring une los valores de un `@RequestHeader String` con coma, así que el backend recibe `"slugA,slugB"`. Con ese valor, `ResolutorTenantPublico.java:28-29` devuelve 404.

Al fallar "Disponibilidad a", las variables `inicio_*` quedan vacías y la carpeta 03 falla en cascada. Las carpetas 04 y 05 dependen de esas reservas.

- **Corrección:** dejar una sola cabecera, del tenant que corresponde a la sucursal del cuerpo. No cambiar los códigos esperados.
- **Validación:** añadir a `test-postman-guards.cjs` una prueba estática: cada petición lleva como máximo un `X-Tenant-Slug`, y su sufijo `_a`/`_b` coincide con `sucursal_*` y `servicio_*`.

**A2. Asignación de rol que puede fallar en silencio.** En `ApiAisladaApplication.java:98`, el `INSERT … SELECT … FROM rol WHERE codigo=?` inserta 0 filas si el código del rol no existe, y no avisa.

STAFF solo tiene pruebas negativas (`:912`, `:987`, `:1062`), así que sin rol pasarían igual. Las pruebas de login solo comprueban que `permisos` sea un arreglo.

- **Corrección:** comprobar que `update` devuelva 1 en la asignación de rol y en `usuario_interno_sucursal`.
- **Validación:** en cada login, comprobar el rol esperado y al menos un permiso conocido. Añadir una petición positiva para staff (por ejemplo, su agenda propia).

## Prioridad media

**M1. La guarda exime peticiones solo por nombre** (`agenda-web…json:3597`). Cualquier GET llamado "Salud backend" pasa sin guarda, aunque apunte a producción.
- Restringir a la ruta `/actuator/health` en `127.0.0.1` o `localhost`, puertos 8080 y 18080.
- Añadir la prueba negativa: "Salud backend" contra `https://api.refluora.com/actuator/health` debe saltarse.

**M2. Las cinco pruebas negativas no prueban el caso asíncrono.** El `sendRequest` simulado responde de forma síncrona (`test-postman-guards.cjs:6`). No demuestra que Postman CLI respete `skipRequest()` cuando se llama desde un callback, que es lo que hacen `agenda-web…json:3602-3604` y `mercado-pago…json:8191-8193`.
- **Validación:** una corrida real del CLI contra un servidor de prueba en 18080 que devuelva otra base. Sus registros deben mostrar solo el marcador y nunca la ruta protegida.
- **Por confirmar:** con `postman collection run --help` y el estado de sesión del CLI, que `--no-report-events` existe y evita que se suban resultados. No lo pude comprobar.

**M3. "Refresh después de logout" no prueba la revocación en el servidor** (`:817-843`). El logout borra la cookie (`ControladorAutenticacion.java:117`), así que el 401 llega solo porque no se envía cookie.
- Capturar la cookie antes del logout y reenviarla de forma explícita después.
- Además, los tokens de acceso se reutilizan después de su logout (`token_admin_a` en las carpetas 01–02). Hay que documentar el contrato real ("logout solo revoca el refresh") con una prueba explícita.

**M4. Las negativas no fijan el `codigo` del error.** Comprueban que exista `codigo`, pero no su valor. Un 409 o 400 por otra causa pasaría como bueno. `:2002` acepta incluso `[409, 400]`.
- Fijar el código esperado en `:1453`, `:2002`, `:2089`, `:2611`, `:2655`, `:2699`, `:2743`, `:2830` y `:3182`. "Anticipo sin política" ya lo hace bien.

**M5. El runner hereda el entorno del shell y solo Point está simulado.**
- `test-api.py:21` copia `os.environ` completo. Si el shell tiene `SPRING_PROFILES_ACTIVE=dev`, `application-dev.yml:3` importa `backend/.env` (el proceso corre con cwd en `backend`, `:63`), y entran credenciales de proveedores. La base de datos sigue sobrescrita.
- **Corrección:** construir el entorno desde una lista permitida, forzar un perfil sin `.env` y quitar `MYSQL_PWD` del entorno de Postman y Java.
- La afirmación "sin red" solo está garantizada para Point. Quedan sin simular `AdaptadorMercadoPagoOrders`, `ClienteReportesMercadoPago`, el OAuth de Mercado Pago y Envía.
- **Propuesta:** arrancar la JVM con un proxy agujero negro (`-Dhttp(s).proxyHost=127.0.0.1 -Dhttp(s).proxyPort=9`).

**M6. Guarda de Mercado Pago.**
- La autorización de escritura (`:8186`) son dos variables iguales. Si ambas quedan guardadas en un entorno, la autorización es permanente. Exigir que una llegue solo por `--env-var` y que incluya una caducidad, por ejemplo una marca horaria comparada con la hora actual.
- Los tokens `TEST-` se saltan la verificación de identidad (`:8189`). Son credenciales de prueba de la cuenta real del vendedor, así que conviene documentarlo o exigir también `/users/me`.
- `:8044-8047` guarda los tokens OAuth del vendedor (acceso y refresh) en el entorno con `postman.setEnvironmentVariable`, y `:833` y `:2405` hacen lo mismo con otros datos. Cambiar a `pm.variables.set`, que no persiste fuera de la corrida.

## Prioridad baja y decisiones del usuario

- **B1.** Login admin_b (`:304`) y "Recepción mantiene reserva" (`:1929-1935`) envían `slug_a` con un usuario o token de B. Hoy no causa verdes falsos porque `app-login` no lee esa cabecera, pero confunde. Usar `slug_b`.
- **B2 (decisión).** "Check-in interno" se hace sobre una cita dentro de dos días. El backend lo acepta porque no valida la fecha (`ServicioRecepcion.java:478-487`). Si el check-in debe ser solo del mismo día, la prueba está dando por bueno un hueco del producto. Lo debe decidir el usuario; no corregirlo en la prueba.
- **B3.** El archivo de entorno se crea vacío y se escribe después (`ApiAisladaApplication.java:49-51`). El runner lo lee en cuanto existe (`test-api.py:66-70`), así que puede leerlo vacío. Escribirlo en un temporal y moverlo de forma atómica.
- **B4.** El classpath se saca del XML de surefire de `CobrosConcurrentesMysqlTest` (`test-api.py:51`), que puede estar desactualizado o no existir. Usar `mvn dependency:build-classpath -Dmdep.includeScope=test`.
- **B5.** "Cita ajena" guarda el mensaje (`:1122`) pero nunca lo compara con un 404 de un id inexistente del propio tenant. Añadir esa comparación de mensaje y código.
- **B6 (sugerencia).** Añadir escrituras cruzadas entre tenants: Caja de A vendiendo `cita_b`, y Recepción consultando la agenda de `otra_sucursal`.
- **B7. Saneado:** sin valores sensibles literales. Las variables de autenticación y datos personales están vacías, no hay `console.*`, y los `@/path/to/file` son marcadores de ejemplo del original. Se conservan `lastUpdatedBy` y `uid` con el ID de usuario de Postman, lo cual es aceptable para actualizar la misma colección. Antes de subir, hay que hacer la comparación con la versión remota, porque un PUT reemplaza la colección completa.

## Lo que está bien

- Guarda de privilegios antes de Spring y nueva comprobación del marcador con el DataSource real.
- Nonce y nombre de base verificados por el runner con valor exacto.
- Point `@Primary` simulado con idempotencia y rechazo por la clave del cliente (`ServicioMercadoPagoPoint.java:147`).
- `DROP` con validación del nombre y comprobación del puerto en el `finally`.
- La carpeta Legado queda bloqueada por la guarda.
- `target/` está ignorado (`backend/.gitignore:1`).

## Orden propuesto

1. Corregir A1 y A2 y repetir la corrida, sin cambiar los códigos esperados.
2. Corregir M1–M5.
3. Hacer la prueba real asíncrona de M2.
4. Revisión de cierre y comparación con la versión remota antes de cualquier subida.
