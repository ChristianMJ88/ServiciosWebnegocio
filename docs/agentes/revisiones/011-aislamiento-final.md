# Revisión 011 · O-H1: MySQL aislado

**Resultado: apruebo O-H1 sin hallazgos altos.** Hay dos hallazgos medios que conviene corregir antes de la línea base con Postman y dos bajos. La guarda de nombre, las bases hijas y la regresión de denegación están bien planteadas. Además, `MysqlPrivilegiosTest` detecta si alguien vuelve a ejecutar las pruebas con una cuenta amplia y la URL apuntando a `fluora_test_base`: en ese caso fallaría el SELECT contra `agenda_db`.

**Límites:** solo tengo Read/Glob/Grep, así que no pude ejecutar `git diff`. Revisé el contenido actual de los 7 archivos, no el diff línea por línea. Tampoco pude guardar esto en `revisiones/011.md`; Codex puede copiarlo ahí. No leí el archivo de credenciales ni ningún `.env`.

## Medios

**M1 — La comprobación de `SHOW GRANTS` usa una lista negra que deja pasar permisos globales**
`soporte/MysqlPrivilegiosTest.java:21-25`. Solo rechaza el texto exacto `GRANT ALL PRIVILEGES ON *.*`, `WITH GRANT OPTION` y `agenda_db`. Estos casos pasarían la prueba:
- `GRANT SELECT, SUPER, FILE, PROCESS ON *.*` u otros permisos dinámicos (`SYSTEM_VARIABLES_ADMIN`…). El SELECT denegado no los detecta.
- `GRANT … ON \`agenda\_db\`.*`: el texto escapado no contiene `agenda_db`. Esto sí lo cubre la denegación de SELECT, pero no esta comprobación.
- Roles concedidos (`GRANT \`rol\`@\`%\` TO …`): `SHOW GRANTS` sin `USING` no muestra sus permisos.

**Cambio propuesto:** usar una lista permitida. Cada línea debe ser `GRANT USAGE ON *.* TO …` o `GRANT <privs> ON \`fluora\_test\_%\`.* TO …`, sin `WITH GRANT OPTION`. Además, `SELECT CURRENT_ROLE()` debe devolver `NONE`.
**Validación:** un caso unitario con líneas de ejemplo (aceptadas y rechazadas) que separe el analizador de la conexión. Así se prueba sin cambiar permisos reales.

**M2 — `CobrosConcurrentesMysqlTest` ejecuta DDL antes de comprobar la base de destino**
`CobrosConcurrentesMysqlTest.java:47-51`. Con `hbm2ddl.auto=create-drop`, Hibernate borra y crea tablas al construir la `SessionFactory`, en la línea 50. La comprobación `SELECT DATABASE()=db` llega después, en la línea 51. La URL de destino sale de `replaceFirst("/[^/?]+(?=\\?|$)", …)`. Si ese reemplazo no coincide, la URL sigue apuntando a `fluora_test_base`. Por ejemplo, con una URL que indique la base por parámetro o con otra forma de host. En ese caso el borrado y la creación ocurrirían en la base compartida y eliminarían las tablas `LIKE` que usan las demás suites.
Los permisos limitan el daño a `fluora_*`, pero esto contradice la regla de "guarda antes de DDL". `MigracionesPoliticaMysqlTest:25` y `MigracionPointDivididoMysqlTest:18` sí comprueban antes.

**Cambio propuesto:** antes de construir el registro, abrir una conexión a la URL derivada y comprobar con `MysqlAislado.comprobar` que `DATABASE()` sea igual a `db`. También comprobar que la URL derivada no sea igual a la base.
**Validación:** un caso unitario con una URL sin base en la ruta que demuestre el rechazo antes de crear la `SessionFactory`.

## Bajos

**B1 — Si falta la variable de entorno, las pruebas MySQL se omiten sin fallar**
Las cinco suites usan `@EnabledIfEnvironmentVariable`. Si la variable no está definida, `MysqlPrivilegiosTest` también se omite. Al registrar la suite completa, conviene anotar los casos MySQL ejecutados y sin omitir, como en la 010 ("19 casos sin omitir"), y nombrar `MysqlPrivilegiosTest` de forma explícita.

**B2 — Pueden quedar bases huérfanas `fluora_test_0xx_*`**
Si la JVM termina de forma abrupta, el `finally` no corre. Propongo comprobar después de la suite que `SHOW DATABASES LIKE 'fluora\_test\_0%'` y `'fluora\_test\_permisos\_%'` no devuelvan resultados. Las bases que queden se pueden borrar con la misma cuenta restringida. Es el mismo criterio de "0 esquemas restantes" usado en la 009/010.

## Lo que revisé y está bien
- `MysqlAislado.java:16`: `String.matches` exige que coincida el nombre completo. El patrón solo admite minúsculas, dígitos y `_`, así que no admite comillas invertidas, `;` ni saltos de línea. Es más estricto que el patrón del GRANT (`fluora\_test\_%`), y así debe ser. `MysqlAisladoTest` cubre los valores nulo y vacío, `agenda_db`, el caso con `_` sin escapar (`fluoraXtest_base`) y una inyección.
- La guarda está antes de cualquier DDL en las conexiones base de las cuatro suites. En `HorariosZonaJvmMysqlTest:32` la comprobación usa la misma conexión que los `TEMPORARY` porque el `SingleConnectionDataSource` está creado con `suppressClose=true`.
- `MigracionPointDivididoMysqlTest`: todo el DDL se escribe sobre `` `db`.tabla ``. `estadoOrigen` ahora protege `fluora_test_base`. La línea 32, `setCatalog(original)`, no hace nada pero tampoco causa daño.
- `MysqlPrivilegiosTest`: los códigos 1044/1142 son correctos para una cuenta sin acceso, incluso si la tabla no existe. Con `WHERE 1=0` la prueba no cambia filas aunque falle la configuración. El `DROP` está en un `finally`.
- Se conservó el trabajo ajeno: no toqué el working tree.

**Siguiente paso:** con M1 y M2 corregidos y la suite completa registrada sin omitir los casos MySQL, O-H1 queda cerrada y se puede pasar a la línea base 011 con Postman.
