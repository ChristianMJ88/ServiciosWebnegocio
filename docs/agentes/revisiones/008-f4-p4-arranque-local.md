La corrección mínima del `.run` es correcta y no veo bloqueantes. Sí hacen falta cuatro ajustes antes de arrancar P4; dos de ellos cambian el comando que tenías previsto. Solo hice lecturas, y del `.run` leí nombres de claves, nunca la línea 8 con los valores.

## Lo comprobado

- **`.run/Backend Spring Boot (dev).run.xml`:** `goals` = `spring-boot:run` (l.11), `workingDirPath` = `$PROJECT_DIR$/backend` (l.24), `pomFileName` = `pom.xml` (l.25). `myRunnerSettings` está vacío y hereda del IDE (l.5). La l.8 contiene `profiles`, `jvmArguments`, `DB_HOST/PORT/NAME/USER/PASSWORD` y `JWT_SECRETO`.
- **`backend/pom.xml:153`:** `<jvmArguments>-Duser.timezone=UTC</jvmArguments>`. Surefire también fija UTC (l.160). La devtools está en el classpath (l.74).
- **`application-dev.yml:3`:** carga `optional:file:.env[.properties]`, ruta relativa al directorio de trabajo, así que el arranque tiene que hacerse desde `backend/`.
- **`application.yml`:** los `DB_*` tienen valores por defecto (l.7-9) y el JWT también (l.202). El interruptor de tareas es `TAREAS_PROGRAMADAS_HABILITADAS`, que vale `true` por defecto (l.70). Lo usa `ConfiguracionTareasProgramadas.java:10`, que es la clase que activa los `@Scheduled`. La verificación automática de dominios ya viene en `false` (l.60).
- **Maven:** `backend/.mvn` y `mvnw` no inyectan `DB_*`, `JWT` ni `jvmArguments`.

## Ajustes necesarios

**P1. Si falta `backend/.env`, el backend arranca igual.** Como la importación es `optional:`, sin el fichero se usan en silencio los valores por defecto (usuario y JWT de desarrollo), y el error queda oculto. La guarda debe exigir que `backend/.env` exista y que estén las claves `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y `JWT_SECRETO`. Debe comprobarlo con `grep -c '^CLAVE='` e informar solo de presente o ausente.

**P1. Las variables exportadas en la shell pisan al `.env`.** En Spring, las variables del sistema operativo tienen prioridad sobre los ficheros importados. Antes de arrancar hay que comprobar por nombre (`env | cut -d= -f1 | grep -E '^(DB_|JWT_|TAREAS_PROGRAMADAS|SPRING_)'`) que no hay ninguna exportada. Si aparece alguna, arranca con `env -u`.

**P1. Desactivar la devtools obliga a repetir UTC.** El reinicio de la devtools solo se apaga con una propiedad del sistema de la JVM. Un `--argumento` de aplicación no sirve. La única forma de pasarla con `spring-boot:run` es `-Dspring-boot.run.jvmArguments`, y eso sustituye el valor del pom. Por eso el comando de medida tiene que repetir UTC explícitamente:

```
cd backend && ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.jvmArguments="-Duser.timezone=UTC -Dspring.devtools.restart.enabled=false" \
  -Dspring-boot.run.arguments=--aplicacion.tareas-programadas.habilitadas=false
```

Esto aplica solo a la CLI de P4. El `.run` se queda únicamente con `-Dspring-boot.run.profiles=dev`. Hay que documentar en ESTADO que el override de la medida es intencionado y que incluye UTC.

**P2. Puede que el `.run` esté versionado con los valores dentro.** El `.gitignore` excluye `.idea/` y `backend/.env`, pero no `.run/`, así que el fichero probablemente está en git. Hay que confirmarlo con `git ls-files --error-unmatch ".run/Backend Spring Boot (dev).run.xml"`. Si lo está, quitar los literales no los saca del historial. Comprueba sin mostrar valores (`git log --oneline -S 'JWT_SECRETO=' -- .run/`) y, si el JWT literal se usó fuera de local, pide al usuario que lo rote. Antes de editar, `git diff --stat` sobre ese fichero debe salir vacío (es el "estado inicial sin cambios ajenos").

## Guarda estática

- Parsear el XML, no usar regex sobre el texto.
- Los tokens de `cmdOptions` deben ser exactamente `["-Dspring-boot.run.profiles=dev"]`.
- `goals` debe ser exactamente `["spring-boot:run"]`, con `workingDirPath` y `pomFileName` como arriba.
- `myRunnerSettings` sin opciones de VM ni de entorno.
- Ningún texto `-DDB_`, `-DJWT` ni `jvmArguments` en el fichero.
- `pom.xml` debe conservar `-Duser.timezone=UTC` en `jvmArguments`.
- Más los dos controles de P1 sobre `.env` y la shell.
- Al fallar, informar solo del nombre de la clave, nunca del valor.
- IntelliJ puede reescribir o reordenar el XML al abrirlo, así que la guarda debe comparar opciones, no el texto literal.

## Flyway (solo lectura, antes del arranque)

- Consulta `SELECT version, success FROM flyway_schema_history ORDER BY installed_rank` mediante `docker exec agenda-mysql`. Usa `MYSQL_PWD` dentro del contenedor para no pasar la contraseña como argumento ni mostrarla.
- **Criterios:** ninguna fila con `success=0`; la versión máxima es `142`, la última que hay en `db/migration` (`V142__planificacion_y_lease_dominios.sql`); y el conjunto de versiones aplicadas coincide con el de los ficheros, sin huecos ni versiones de más.
- Los checksums los valida el propio arranque (`validateOnMigrate`). Si falla, se diagnostica, sin `repair`.

## Verificación una vez arrancado

- **Devtools:** el log debe decir "Restart disabled due to System property…".
- **UTC:** `jcmd <pid> VM.system_properties | grep user.timezone` debe dar `UTC`.
- **Tareas programadas:** en `jcmd <pid> Thread.print` no debe aparecer ningún hilo `scheduling-*`.
- **Puertos:** el 8080 responde y el 3302 sigue apuntando al contenedor con el volumen `backend_agenda_mysql_data`.

La licencia de IntelliJ no bloquea nada: la configuración Maven y la CLI con Java 21 no dependen de Ultimate.
