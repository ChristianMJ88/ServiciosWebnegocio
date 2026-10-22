Ya terminé de leer el código. Te dejo la revisión técnica de 012.

# 012: concurrencia, propiedad del dominio y lectura pública

## Recomendación: una sola tabla con un índice único parcial (tu alternativa)

**Hay que aclarar el diseño.** El §3 dice que `hostname` es único, pero el §4.2 dice que «si otra empresa verifica el mismo host, el registro pendiente anterior se libera». Para que eso pase, dos filas con el mismo host tienen que existir a la vez. Lo que se buscaba con «único» era que **un host tenga un solo dueño con propiedad comprobada**, no que haya una sola fila por host. Como autor del diseño, la aclaración es esta: la unicidad solo aplica a los estados que reservan el host. Esto no toca D1–D3.

**Por qué esta opción y no la de dos tablas:** con una tabla hay una sola máquina de estados, una sola versión y una sola proyección para el panel. Con dos tablas, los estados quedan duplicados entre la reclamación y la vinculación, y hay que sincronizarlos. El árbitro de la carrera es el mismo en los dos casos: un índice único que decide al hacer commit.

### Columnas de `empresa_dominio` en V141 (además de las del §3)

Las dos columnas nuevas son generadas. Con `ddl-auto: validate` (`application.yml:14`) no hace falta mapearlas en la entidad, o se mapean con `insertable=false, updatable=false`. La base es MySQL 8.0 (`backend/docker-compose.yml:3`), que permite índices únicos sobre columnas generadas.

- **`hostname_reservado`**
  - Valor: `CASE WHEN estado IN ('VERIFICADO','CONECTANDO','ACTIVO','ERROR') THEN hostname END` (generada, STORED).
  - Índice: `UNIQUE uk_empresa_dominio_hostname_reservado`.
  - Se puede repetir NULL, así que los pendientes y los retirados no apartan el host.
  - `ERROR` reserva porque solo se llega ahí después de comprobar la propiedad.
- **`clave_vigente`**
  - Valor: `CASE WHEN estado <> 'RETIRADO' THEN CONCAT(empresa_id,':',hostname) END`.
  - Índice: `UNIQUE`.
  - Así cada empresa tiene como máximo una fila vigente por host, y los retiros se quedan como historial (§4.6). Si el negocio vuelve a pedir el host, se crea una fila nueva con un token nuevo.
- **Índice normal en `(hostname)`**, para bloquear y consultar por host.

### Qué pasa cuando otra empresa (B) verifica un host

| Fila actual de A | Resultado para B | Resultado para A |
|---|---|---|
| no existe, o está `RETIRADO` | `VERIFICADO` | sin cambio |
| `PENDIENTE_VERIFICACION` | `VERIFICADO` | pasa a `RETIRADO`, con `ultimo_error` genérico: «Otra cuenta comprobó la propiedad de este dominio» (así se cumple el «se libera» del §4.2) |
| `VERIFICADO`, `CONECTANDO`, `ACTIVO` o `ERROR` | **se queda en `PENDIENTE`**, con el código `HOST_EN_USO` en `ultimo_error` | **sin cambio** (misma `version`) |

En el último caso nunca se quita automáticamente un dominio con propiedad ya comprobada. La plataforma ve el conflicto en su cola, filtrando por `HOST_EN_USO`. La «nueva decisión» es que la plataforma retire la fila de A; después B vuelve a verificar con normalidad. No hace falta agregar estados.

**Al crear la solicitud:** si el host ya está reservado por otra empresa, se responde 409 con un mensaje genérico, sin decir quién lo tiene. Es el mismo criterio de hoy (`ServicioAdminCitas.java:2635`). El caso `HOST_EN_USO` en la verificación queda solo para las carreras.

### Orden de la verificación (importante)

1. **La consulta DNS se hace fuera de cualquier transacción.** Hay que leer la fila, el token y la versión; resolver el TXT con un timeout de 5 s y comparar con `MessageDigest.isEqual`. No se debe tener bloqueada ninguna fila durante esos 5 s.
2. **Después, en una transacción corta:**
   - bloquear las filas del host con `PESSIMISTIC_WRITE`, consultando por hostname y siguiendo el patrón de `CitaRepositorio.java:68`;
   - volver a leer la fila de B y confirmar que sigue en `PENDIENTE` con el mismo token y la misma versión. Si cambió, no se hace nada;
   - pasar las pendientes de otras empresas a `RETIRADO` y la de B a `VERIFICADO`.
3. **Si el commit falla por el índice único** (`DataIntegrityViolationException` sobre `uk_..._hostname_reservado`), se marca `HOST_EN_USO` en una transacción nueva y se responde 409. Nunca debe terminar en un 500.

**Varios valores TXT:** `_fluora.<host>` puede tener varios valores a la vez. Si el dueño del DNS pone los tokens de A y de B, los dos pasan la verificación. El índice decide quién gana, y es legítimo, porque el dueño del DNS autorizó a ambos.

**Límite del plan:** el uso que se pasa a `exigirLimite` es el número de filas de la empresa que no están `RETIRADO`, contando las pendientes. Así un negocio no puede acumular pendientes sin límite. Se calcula con la fila de la empresa bloqueada (por ejemplo `empresa_sitio_config` con `PESSIMISTIC_WRITE`), para que dos solicitudes simultáneas no pasen las dos.

- **Observación (Media):** si el plan no tiene la fila del límite, hoy eso significa «sin límite» (`ServicioCapacidadesEmpresa.java:71-78`). No hay que cambiarlo. Codex lo escribe en el paso del editor de planes de su respuesta, para que el usuario fije `DOMINIOS_PROPIOS_MAX` al activar `DOMINIO_PROPIO`.

### Cómo mantener la propiedad sin exponer los tokens de otro negocio

- **Endpoints del panel:** siempre filtran por el `empresaId` de la sesión. El token se devuelve solo para las filas propias en `PENDIENTE_VERIFICACION`; después de verificar ya no se necesita.
- **DTO de la plataforma:** no tiene campo de token. Muestra el host, el estado, la empresa y `ultimo_error`.
- **Mensajes al negocio:** los de conflicto no dicen qué empresa tiene el host.
- **Logs y `ultimo_error`:** nunca guardan los valores TXT que se leyeron. **Ojo: esos valores pueden incluir el token de otro negocio.** Tampoco se guarda el token propio.

### Huecos que hay que cerrar en el mismo paso

- **H1 sigue abierto por otro camino (Alta).** El `PUT` de configuración del sitio sigue escribiendo `dominioPrincipal` (`ServicioAdminCitas.java:559-565`). Tiene que dejar de hacerlo: ignorar el campo o responder 400 si viene distinto. Si no, se puede saltar la verificación.
- **Migración:**
  - copiar cada `dominio_principal` que exista a `empresa_dominio` como `PENDIENTE_VERIFICACION`;
  - generar el token en SQL: `REPLACE(REPLACE(REPLACE(TO_BASE64(RANDOM_BYTES(32)),'+','-'),'/','_'),'=','')`;
  - después, poner `dominio_principal = NULL`, porque solo se deriva de un dominio `ACTIVO` y todavía no hay ninguno.
  - El índice V33 sigue sirviendo, porque como mucho hay un `ACTIVO` por host.

## Lectura pública por slug: sí hay que ocultar el dominio

Hoy `obtenerPorSlug` devuelve `sitio.getDominioPrincipal()` sin ninguna condición (`ServicioSitioPublico.java:78`). Ahora el canonical usa el origen de marketing (`seo.service.ts:16`, `seo-renderer.js:90`), pero §6 va a usar este valor. Propuesta:

- **El valor guardado y el valor público van separados:**
  - `dominio_principal` se mantiene como derivado del **estado**: se pone al pasar a `ACTIVO` y se limpia al retirar. **No cambia** cuando se pierde la capacidad del plan.
  - Lo que se publica es `ACTIVO ∧ capacidad DOMINIO_PROPIO vigente`, calculado al leer.
- **Por slug:** si no hay capacidad, `dominioPrincipal = null` en la respuesta y el sitio se sigue sirviendo en `/e/slug`.
- **Por host:** si no hay capacidad, 404.
- **Al recuperar la capacidad:** vuelve a funcionar sin otra verificación, porque la fila nunca dejó de estar `ACTIVO`.
- **Caché:**
  - `ServicioCapacidadesEmpresa.obtener` lee de la base en cada llamada; no hay caché en la aplicación.
  - Lo que hay que limitar es la caché HTTP. El backend debe responder `/publico/sitio/{slug}` y `/host/{h}` con `Cache-Control` de `max-age` 60 como máximo, o con `no-store`. Hoy no fija ninguno, y el proxy copia lo que llegue (`functions/index.js:52`).
  - `tenantPage` ya usa `no-store` en las respuestas 200 y 60 s en las 404 (`index.js:84,114`).
  - **Restricción para §6:** el sitemap no puede listar URLs de dominios propios mientras tenga `s-maxage=3600` (`index.js:150`).

## Pruebas (MySQL efímero, resolver y reloj simulados)

1. **Carrera entre dos empresas** con ambos TXT presentes (dos hilos y un `CountDownLatch`): exactamente una queda `VERIFICADO`; la otra queda `PENDIENTE` con `HOST_EN_USO`, y no hay ningún 500.
2. **A pendiente y B verifica:** A pasa a `RETIRADO` con el mensaje genérico, B a `VERIFICADO`, y el panel de A no muestra ningún dato de B.
3. **Takeover bloqueado:** con A en cada uno de `VERIFICADO`, `CONECTANDO`, `ACTIVO` y `ERROR`, B tiene un TXT válido y aun así no avanza; A queda con la misma versión. Si B solicita el host, recibe 409.
4. **A retirado y B verifica:** funciona; la fila retirada de A se conserva.
5. **Misma empresa, dos solicitudes simultáneas del mismo host:** una sola fila vigente. Si vuelve a pedirlo después de retirarlo, se crea una fila nueva y la anterior se queda como historial.
6. **Límite N con N+1 solicitudes simultáneas:** se aceptan exactamente N; las pendientes cuentan.
7. **Verificación desactualizada:** si el token rota o la fila se retira entre la consulta DNS y el commit, no hay transición.
8. **Aislamiento:** el panel de B nunca incluye filas ni tokens de A; el DTO de la plataforma no tiene token; los logs capturados (`OutputCaptureExtension`) no contienen ni tokens ni valores TXT.
9. **Capacidad:**
   - al quitar el módulo, el host da 404, el slug devuelve `dominioPrincipal` null y la fila sigue `ACTIVO`;
   - al devolverlo, el host vuelve a resolver y el resolver no se llama (`verifyNoInteractions`);
   - las cabeceras de caché tienen 60 s como máximo.
10. **Migración:** lo existente pasa a `PENDIENTE` con tokens únicos de 43 caracteres, `dominio_principal` queda en NULL y no se inserta nada en `plataforma_plan_*`.
11. **PUT del sitio:** un `PUT` con `dominioPrincipal` ya no lo cambia.

Esto se revisó solo leyendo archivos. No ejecuté nada ni guardé esta revisión en `revisiones/`; si la quieres en el canal, Codex la copia.
