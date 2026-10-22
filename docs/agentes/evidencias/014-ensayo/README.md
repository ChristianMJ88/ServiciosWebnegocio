# Ensayo local 014

`Ensayo014.java` es un arnés de verificación, fuera del artefacto de aplicación. Solo admite MySQL loopback con cuenta limitada a `fluora\_test\_%`.*. No usa datos reales ni claves de proveedores. La contraseña de sus fixtures se genera en memoria.

Ejecutar después de `mvn -q test` en verde, sin otra compilación/suite concurrente:

```sh
python3 scripts/ensayo-014.py --check-guards
python3 scripts/ensayo-014.py --mysql-env /ruta/privada/mysql.json --collation utf8mb4_0900_ai_ci --output /ruta/nueva/resultado.json
```

El lanzador construye un entorno vacío salvo PATH, HOME, TMPDIR, LANG, LC_ALL y JAVA_HOME (equivalente a `env -i`), compila las clases de prueba, excluye DevTools y verifica las huellas de todas las migraciones fuente/compiladas. Después incorpora únicamente las tres variables MySQL del JSON privado y las opciones del ensayo. Java vuelve a validar entorno, DevTools, host, privilegios y nombre del esquema antes del DDL. Se toleran `JAVA_MAIN_CLASS_<pid>` y `__CF_USER_TEXT_ENCODING`, añadidas por macOS; no controlan Spring ni proveedores.

Tres negativas: perfil `dev` heredado, JDBC remoto y DevTools presente, todas deben abortar antes de conectar MySQL. Se rechaza Python con optimizaciones (`-O`). Usar una ruta de resultado nueva; no sobrescribir los fallos históricos.

El perfil es explícitamente `prod`, el puerto es efímero y loopback, todas las tareas y proveedores externos están deshabilitados. El proxy HTTP(S) externo apunta al puerto local 9 sin servicio; las llamadas de humo internas usan loopback. El único tenant configurado usa una llave pública sintética sin token de acceso: solo se consulta su capacidad pública, nunca se crea ni cobra una orden allí. Los planes y sus módulos se obtienen del catálogo y se crean/asignan por API únicamente en las fixtures.

El `finally` intenta retirar independientemente los dos esquemas y comprueba su ausencia. Ante kill/cierre abrupto, revisar manualmente nombres `fluora_test_014_%` con la cuenta restringida antes de otra corrida; no borrar por patrones ni tocar bases habituales. El JSON registra resultados, metadatos de esquema, hashes y nombres de variables; no registra tokens ni contraseñas.

`resultado.json` conserva el primer fallo V58→V142; no representa el estado final. Los archivos con sufijo V144 son ejecuciones nuevas. Las duraciones de DDL con filas sintéticas no estiman bloqueos ni duración en producción. La tabla de reseñas nace después de V58 y está vacía en este ensayo: su preservación se verifica en `CalificacionResenaMysqlTest` con valores 1 y 5 y rechazo CHECK de 0 y 6.
