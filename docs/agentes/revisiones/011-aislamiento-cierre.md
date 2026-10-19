Ya terminé de leer los cuatro archivos y los informes de surefire. Este es el veredicto. No edité ningún archivo, así que tampoco escribí un archivo de revisión.

# O-H1: veredicto de cierre M1/M2

**Doy O-H1 por cerrado. No hay hallazgos altos ni medios.** Quedan dos puntos bajos, opcionales y que no bloquean el cierre.

## M1: guarda de privilegios (cerrado)

`MysqlAislado.comprobar` (`backend/src/test/java/com/techprotech/agenda/soporte/MysqlAislado.java:9-21`) hace tres comprobaciones en este orden:
1. `DATABASE()` debe seguir el patrón `fluora_test_*`.
2. `CURRENT_ROLE()` debe ser `NONE`.
3. Cada línea de `SHOW GRANTS` pasa por `comprobarConcesion`.

`comprobarConcesion` (`:31-40`) solo acepta dos cosas:
- **Global:** `GRANT USAGE ON *.*`, tal cual.
- **Por base:** privilegios `[A-Z_, ]+` sobre `` `fluora\_test\_%`.* ``, tal cual.

Rechaza `WITH GRANT OPTION` en cualquier posición. Comprobé por qué se rechaza cada caso límite:
- **Roles concedidos aunque estén inactivos:** la línea `` GRANT `r`@`%` TO `` no lleva ` ON `.
- **Privilegios dinámicos u otros privilegios sobre `*.*`:** el destino no coincide.
- **`PROXY ON ''@''`:** el destino no coincide.
- **Permisos de columna:** el paréntesis no encaja en el patrón.
- **Permisos de tabla:** el destino no coincide.
- **Líneas `REVOKE` con revocaciones parciales:** no empiezan por `GRANT `.

La guarda está en el sitio correcto. Se llama antes del primer DDL en `CobrosConcurrentesMysqlTest.java:44`, y también en `MigracionesPoliticaMysqlTest:19`, `MigracionPointDivididoMysqlTest:11`, `HorariosZonaJvmMysqlTest:32` y `ApiAisladaApplication:33`.

El informe confirma que las pruebas no corrieron como root. `TEST-…MysqlPrivilegiosTest.xml` da 1 prueba, 0 omitidas y 0 fallos, y esa prueba llama a `comprobar` con la cuenta del entorno. Root tiene `ALL ON *.* WITH GRANT OPTION`, así que habría fallado.

## M2: URL hija comprobada (cerrado)

`urlHija` (`:22-30`) hace lo siguiente:
- Exige `jdbc:mysql://host/fluora_test_…`.
- Rechaza `database`, `databaseName` y `catalog` sin distinguir mayúsculas.
- Rechaza que la base hija sea igual a la base de origen.

Revisé el `replaceFirst`: solo sustituye el segmento de ruta. No toca el host aunque tenga credenciales o varios nodos, ni una `/` dentro de la cadena de consulta.

La barrera que de verdad importa está en ejecución, en `CobrosConcurrentesMysqlTest.java:47-50`. Abre una conexión con la misma URL, el mismo usuario y la misma contraseña que recibe Hibernate, llama a `comprobar(destino)` y exige `getCatalog()` igual a `db`. Todo eso ocurre antes de `buildSessionFactory()` con `create-drop` (`:51-55`). Gracias a esto, cualquier propiedad de Connector/J que se le escape a la expresión regular (por ejemplo, una variante de `dbname`) también queda cubierta.

## Pruebas unitarias e informes

- `MysqlAisladoTest.xml` da 18 casos, 0 omitidos y 0 fallos. Coincide con el archivo actual (6+2+4+4+2) e incluye los permisos globales, los roles, el permiso sobre otra base, `GRANT OPTION` y las URL sin base o sin ruta.
- Ningún caso termina sin aserción: todos usan `assertThatThrownBy` o `assertThatCode`.
- `CobrosConcurrentesMysqlTest.xml` da 19 casos, 0 omitidos y 0 fallos.

## Puntos bajos (opcionales, para Codex)

**B1: puede quedar una base huérfana.** Está en `CobrosConcurrentesMysqlTest.java:45-59`.
- **Problema:** el `CREATE DATABASE` (`:45`) se ejecuta antes de validar con `urlHija` (`:46`). El `DROP` solo lo protege el `finally` de `:59`. Si fallan `urlHija`, la comprobación previa (`:47-50`) o `StandardServiceRegistryBuilder.build()`, la base `fluora_test_009_*` queda sin borrar.
- **Por qué es bajo:** es un tema de limpieza, no de seguridad, porque solo afecta a bases `fluora_test_*`.
- **Propuesta:** calcular `hija` antes del `CREATE` (así el nombre también se valida antes del DDL) y envolver todo lo que viene después del `CREATE` en `try/finally DROP`.
- **Validación:** una ejecución con la comprobación previa forzada a fallar no debería dejar ninguna base `fluora_test_009_%` en `SHOW DATABASES`.

**B2: faltan casos unitarios de `urlHija`.** Está en `MysqlAisladoTest.java:11-14`. Propongo añadir tres:
- `jdbc:mysql://localhost/fluora_test_a?catalog=agenda_db`, que debe rechazarse. Ahora mismo `catalog` no tiene caso propio.
- Un caso positivo: `jdbc:mysql://h:3306/fluora_test_a?useSSL=false` debe dar `jdbc:mysql://h:3306/fluora_test_hija?useSSL=false`.
- El caso en que la base hija es igual a la base de origen, que debe rechazarse.

**Observación:** `MysqlPrivilegiosTest.java:21-26` repite el recorrido de `SHOW GRANTS` que ya hace `comprobar` en `:15`. Es redundante pero inofensivo.

## Límites de la revisión

- Revisé solo los cuatro archivos pedidos y las cabeceras de los XML de surefire. De las otras pruebas MySQL solo verifiqué que llamen a la guarda.
- No leí `.env`, credenciales ni las propiedades de los informes.
- No cambié revisiones, código de aplicación ni datos.
- El arnés Postman queda fuera de esta revisión y sigue abierto.
