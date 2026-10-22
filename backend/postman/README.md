# Regresión de API de Fluora

Se reutilizan las colecciones existentes del usuario: **AgendaWeb** y **Mercado
Pago Developers**. Sus identificadores se conservan. Los respaldos completos y
los entornos privados quedan fuera del repositorio; los archivos versionables no
incluyen contraseñas, tokens, cookies ni ejemplos con datos personales.

## Ejecutar Fluora en aislamiento

Requisitos: Java21, Maven, MySQL local, cliente mysql, Docker (solo para exportar
la base local), Postman CLI autenticado y un usuario MySQL limitado a
`fluora\_test\_%`.*, sin privilegios globales ni roles. Nunca usar root para
importar la copia, arrancar el backend o correr las pruebas. Las pruebas Java
verifican la denegación de lectura/escritura a `agenda_db`.

Las variables `FLUORA_REGRESION_MYSQL_URL`, `FLUORA_REGRESION_MYSQL_USER` y
`FLUORA_REGRESION_MYSQL_PASSWORD` se suministran en un JSON privado fuera del repo,
con permisos0600. La URL selecciona una plantilla vacía `fluora_test_base`.
Las cuatro pruebas MySQL de Java requieren una plantilla de cuatro tablas vacías
(`cita`, `checkout_orden`, `orden_point_caja`, `pago_venta`) del esquema vigente.
La guarda comprueba base, concesiones y rol antes de cualquier DDL.

```sh
python3 scripts/test-api.py --mysql-env /ruta/privada/mysql.json --copy-local
node --test scripts/test-postman-guards.cjs
node scripts/test-postman-guards-cli.cjs
```

Para verificar el DDL real de Flyway, sin heredar un esquema que Hibernate haya
modificado antes, usar `--fresh-schema --mysql-collation utf8mb4_0900_ai_ci` en
lugar de `--copy-local`. El arnés comprueba que el esquema efímero esté vacío;
Flyway y sus callbacks crean el esquema antes de `validate` y de las fixtures.
La collation se pasa como parámetro del ensayo; no cambia la base habitual.

`--copy-local` **solo lee** `agenda_db` del contenedor local `agenda-mysql` con
mysqldump. La importación usa exclusivamente el usuario restringido en una base
nueva `fluora_test_011_*`. Como alternativa, `--source-dump /ruta/privada/dump.sql`
acepta una exportación sin `CREATE DATABASE` ni `USE`.

El arnés (solo en `src/test`, nunca en el artefacto desplegable) crea tenants,
usuarios, roles por empresa y productos/citas de prueba. Los permisos proceden
del catálogo de roles y no se relajan para las pruebas. Los precios de fixtures
son datos de prueba, no una configuración del producto.

El backend aislado escucha solo en127.0.0.1:18080, rechaza otro esquema y no carga
la `.env` del desarrollador. Tareas y envíos deshabilitados; Point simulado en
memoria; HTTP(S) de la JVM dirigido a un proxy local sin servicio. No se llaman
pagos, reembolsos, correos ni WhatsApp externos. El runner retira su proceso y su
base en `finally` y comprueba que el puerto queda libre. Ante un cierre abrupto,
revisar los esquemas `fluora_test_011_*` antes de otra corrida.

El entorno efímero se genera con escritura atómica y permisos0600. Credenciales,
IDs e instantes se encadenan dentro de la corrida. No usar el archivo example
para ejecutar las escrituras: no contiene el nonce ni las fixtures reales.

Se ejecutan solo00–07 desde **el archivo local**, con `--no-report-events`,
reporters CLI/JUnit y resultados en `backend/target/postman/` (ignorado por Git).
Cada petición exige además un marcador test-only que confirma DATABASE() y el
nonce de esa instancia; no basta con elegir el puerto. La prueba del CLI verifica
que un marcador inválido impide la escritura desde el callback asíncrono.
Solo `GET /actuator/health` enlocalhost:8080/18080 está exento de esa guarda.

Las tres peticiones de **Legado** apuntaban a Apps Script y se conservan sin
correrlas. Los casos de tarjeta/webhook aprobado y BOT quedan en las pruebas de
integración de Java: esta colección no envía una tarjeta ni una firma de Twilio.
El contrato de logout revoca refresh; el acceso JWT sigue vigente hasta expirar.
La colección prueba ambos comportamientos y el rechazo de un JWT firmado vencido.

## Colección directa de Mercado Pago

Se conserva, pero **no se ejecuta** en la regresión anterior. Requiere
`mp_ambiente=PRUEBA`, `mp_access_token` y `mp_cuenta_prueba_esperada` en un entorno
privado. La guarda consulta `/users/me` antes de permitir la petición. Para
`APP_USR`, además exige `test_user`; un token `TEST-` corresponde al modo de
prueba de la cuenta verificada del vendedor. Nunca emplear credenciales productivas.

Cada corrida con escrituras requiere autorización explícita del usuario.
Únicamente para esa corrida se pasa por `--env-var` el valor
`mp_escritura_autorizada_hasta` (epoch en milisegundos, máximo120segundos en el
futuro). No guardarlo en un entorno: caduca y no debe convertirse en autorización
permanente. No hay autorizaciones de escritura en estos archivos. Tokens OAuth y
referencias de scripts se guardan solo en variables transitorias de la corrida.

## Actualizar las colecciones remotas

Primero, revisión de Claude del saneado y corrida verde. Antes de cada PUT,
comparar la colección remota actual con su respaldo; si cambió, integrar el
cambio o consultar al usuario. Actualizar las mismas colecciones con las versiones
sin secretos del repo. No sustituir los entornos privados ni publicar resultados
de corridas. Un PUT no constituye autorización para ejecutar las peticiones.

F0 de008 agrega lecturas de soporte/contacto legal y rechazo del token tenant en administración de plataforma. El runner aplica Flyway únicamente en la copia `fluora_test_011_*` verificada, antes de Hibernate validate. No actualiza el origen.
