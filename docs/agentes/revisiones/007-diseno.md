No pude guardar el diseño en `revisiones/` porque esta sesión solo tiene herramientas de lectura. Lo dejo aquí completo para que Codex lo registre. La base es el código actual y la versión consolidada de 007.

# Diseño 007-1

## Hay que decidir antes de implementar

- **R1. Política y productos.** La versión consolidada dice literalmente «no se puede activar el anticipo o cobro online sin una política publicada». Leído así, el checkout de solo productos también queda bloqueado, aunque la política no dice nada de devoluciones de productos. Propongo seguir la lectura literal: ningún checkout se activa sin política, y la casilla de aceptación solo se pide cuando la orden lleva citas. No hay que redactar política de productos.
- **R2. Número de reagendados.** P4(a) aprueba «uno», pero P8 lo pone entre los parámetros configurables (valor 1). Propongo guardar la columna en cada versión y que la validación solo acepte 1 hasta que el usuario decida.
- **R3. Redacción del texto generado.** El texto público sale de una plantilla y es lo que el cliente acepta. Codex no debe publicarlo como texto aprobado: queda como borrador hasta que el usuario apruebe la redacción. Es distinto de los términos de Fluora (A5), pero igual es visible al cliente.
- **R4. Modo RETENER.** No está definido si conserva la aclaración. Propongo que sí: el reembolso manual de P3 y el plazo de corrección de S2 dependen de esos días.

## 1. Política versionada y valores predeterminados

**Dónde guardar los predeterminados:** en la configuración de la aplicación (`@ConfigurationProperties("fluora.politica.predeterminada")`, validada al arrancar), no en `parametro_sistema_definicion`.
- Ese catálogo es clave‑valor editable por empresa y tiene «restablecer» (`ServicioParametrosSistemaImpl.java:29-43`). Guardar ahí las cifras crearía una segunda fuente de decisión, editable de una en una y sin versión.
- Los predeterminados solo rellenan el formulario. **Nunca se aplican a una cita**: sin versión publicada no hay política.

**V137, tabla `politica_empresa`.** Las filas son inmutables: entidad con `@Immutable` y `updatable=false`, sin ningún método de actualización.
```
id BIGINT PK AI, empresa_id BIGINT NOT NULL FK empresa,
version INT NOT NULL,                    -- UNIQUE(empresa_id, version)
modo_no_show VARCHAR(20) NOT NULL CHECK IN ('CREDITO_REAGENDAR','RETENER'),
dias_reagendar SMALLINT NULL, reagendados_max TINYINT NULL,
recordatorio_1_dias SMALLINT NULL, recordatorio_2_dias SMALLINT NULL,
dias_aclaracion SMALLINT NOT NULL,
texto_adicional VARCHAR(2000) NULL,      -- texto plano, se escapa al mostrarlo
contacto_reclamos_correo VARCHAR(255) NULL, contacto_reclamos_telefono VARCHAR(30) NULL,
plantilla_texto VARCHAR(20) NOT NULL,    -- id de la plantilla usada
texto_generado TEXT NOT NULL, texto_sha256 CHAR(64) NOT NULL,
publicada_en DATETIME(6) NOT NULL (UTC), publicada_por_usuario_id BIGINT NOT NULL,
UNIQUE KEY uk_politica_empresa_id (empresa_id, id)   -- destino de las FK compuestas
CHECKs: correo o teléfono obligatorio;
  CREDITO_REAGENDAR ⇒ los cuatro campos de reagendado obligatorios,
                      1 ≤ dias_reagendar ≤ dias_aclaracion ≤ 365,
                      dias_reagendar > recordatorio_1 > recordatorio_2 ≥ 1, reagendados_max = 1 (R2);
  RETENER ⇒ esos cuatro campos NULL.
```
- No pongo `permite_reembolso_noshow`: P3 ya lo fijó y sería una cifra de decisión que nadie usa.
- `texto_generado` es solo presentación. Se guarda tal como se publicó para que, si la plantilla cambia, siga existiendo prueba de lo aceptado. Las decisiones se leen solo de las columnas.
- **Versión vigente = la de `version` más alta.** No hay bandera de vigencia que se pueda desincronizar.

**Publicación:**
- `GET /api/v1/admin/politica` devuelve `{vigente|null, borrador}`. `POST /api/v1/admin/politica/versiones` recibe el formulario completo y `versionBase`.
- Los dos con `PARAMETROS_SISTEMA_GESTIONAR`. La empresa sale del token, nunca del cuerpo.
- Pasos dentro de la transacción:
  1. Bloquear la fila de `empresa` (`PESSIMISTIC_WRITE`).
  2. Si `versionBase` no es la vigente, responder 409 (dos administradores a la vez).
  3. Si el contenido es igual a la vigente, devolverla sin crear versión.
  4. Si no, insertar `max+1` con `publicada_en = reloj.instant()`.
- El UNIQUE es la última defensa si falla el bloqueo.
- **Cambios en `/admin/parametros`:** se añade una sección propia, con su propio endpoint y un botón «Publicar versión», separada de la lista de parámetros genéricos.

**Cita (V137):** `politica_version_id BIGINT NULL` y `politica_aceptada_en DATETIME(6) NULL` (UTC, mismo converter que `creada_en`), con `CHECK ((politica_version_id IS NULL) = (politica_aceptada_en IS NULL))`.
- FK compuesta `(empresa_id, politica_version_id) → politica_empresa(empresa_id, id)`: impide por esquema ligar una política de otro tenant.
- Las citas legadas quedan NULL/NULL.

## 2. `ajuste_saldo_cita` (V137, solo el esquema)
```
id BIGINT PK AI, empresa_id BIGINT NOT NULL,
cita_origen_id BIGINT NOT NULL, cita_destino_id BIGINT NULL,
tipo VARCHAR(30) NOT NULL CHECK IN ('CUSTODIA','RETENCION','TRANSFERENCIA','VENCIMIENTO',
     'REEMBOLSO_FUERA_SISTEMA','CONDONACION','REVERSION'),
monto DECIMAL(10,2) NOT NULL CHECK (monto > 0), moneda VARCHAR(10) NOT NULL,
politica_version_id BIGINT NULL,
revierte_ajuste_id BIGINT NULL UNIQUE,   -- FK a la propia tabla; una sola reversión por ajuste
origen VARCHAR(20) NOT NULL,             -- SISTEMA|ADMIN|CLIENTE|TAREA|CORRECCION
actor_usuario_id BIGINT NULL, motivo VARCHAR(500) NOT NULL,
efectivo_en DATETIME(6) NOT NULL,        -- fecha contable (p. ej. la del vencimiento)
registrado_en DATETIME(6) NOT NULL,      -- cuándo se escribió
clave_idempotencia VARCHAR(120) NOT NULL, UNIQUE(empresa_id, clave_idempotencia)
FK (empresa_id, cita_origen_id) → cita(empresa_id, id)   -- requiere UNIQUE(empresa_id,id) en cita
FK (empresa_id, cita_destino_id) → cita(empresa_id, id)
FK (empresa_id, politica_version_id) → politica_empresa(empresa_id, id)
CHECK (tipo='TRANSFERENCIA') = (cita_destino_id IS NOT NULL); CHECK cita_destino_id <> cita_origen_id
CHECK (tipo='REVERSION') = (revierte_ajuste_id IS NOT NULL)
INDEX (empresa_id, cita_origen_id), INDEX (empresa_id, cita_destino_id), INDEX (empresa_id, tipo, efectivo_en)
```
- **El monto siempre es positivo y el tipo define el efecto.** Las reglas de efecto las fija 007-2.
- **La reversión del S2 es otra fila** (`REVERSION` + `revierte_ajuste_id`), nunca un UPDATE ni un DELETE. Que no se pueda revertir una reversión ni una transferencia ya usada lo valida la aplicación en 007-2.
- **Para no fingir saldos:**
  - En 007-1 solo se crea el DDL y una prueba MySQL de las restricciones. Sin entidad ni repositorio en producción.
  - `ServicioSaldoCitas`, el predicado de Caja y los reportes no se tocan. «Custodia» sigue marcada como no disponible.
- **Para 007-2/4 queda pendiente:**
  - Los plazos se calculan desde la versión de política y la fecha de la cita.
  - El plazo propio de P7 y las extensiones de aclaración no tienen columna. Si 007-2 las necesita, hay que reservar otra versión de migración.
  - Citas legadas con anticipo y sin política aceptada: lo decide el usuario en 007-2, sin aplicarles la versión vigente.

## 3. Folio

**Formato:** `FL-` + **8 caracteres** de un alfabeto de 32 (`23456789ABCDEFGHJKLMNPQRSTUVWXYZ`). Así `SecureRandom.nextInt(32)` y `byte & 31` en SQL no tienen sesgo.
- Con 6 caracteres hay unos 1,07·10⁹ combinaciones. Con 100 mil citas por tenant habría colisiones reales y un intento a ciegas acierta 1 de cada 10⁴.
- Con 8 hay unos 1,1·10¹² y un intento acierta 1 de cada 10⁷; se sigue dictando bien. 10 ya no aporta.
- El ejemplo de la versión consolidada tiene 6. Pasar a 8 es un detalle de formato: si el usuario prefiere 6, también vale, porque 007-3 no aceptará el folio solo.

**Sufijo:**
- Reserva de grupo con más de un miembro: `FL-XXXXXXXX-1…n`, en orden de creación (id). Reserva de un solo miembro: sin sufijo.
- Ojo: `ServicioPoliticaReservaPago.java:38` asigna un `reserva_grupo_id` incluso a las citas individuales. Las anteriores a V133 lo tienen NULL y cada una es su propia reserva.

**Esquema:**
- V137: `cita.folio VARCHAR(16) NULL` y `UNIQUE uk_cita_empresa_folio (empresa_id, folio)`.
- V138: rellenar y después `MODIFY ... NOT NULL`.
- Opcional, si el MySQL de prueba lo acepta: `CHECK (REGEXP_LIKE(folio,'^FL-[2-9A-HJ-NP-Z]{8}(-[1-9][0-9]?)?$','c'))`.
- No hace falta columna aparte para la base: la comprobación usa `folio = base OR folio LIKE 'base-%'` sobre el índice único.

**Aplicación:**
- `GeneradorFolioCita` con `SecureRandom`.
- En `ServicioCitas.java:188` y `:337`: generar la base, comprobar que no exista y asignar el folio con sufijo antes de `save`.
- **No reintentar capturando la violación de UNIQUE**: deja la transacción JPA marcada solo para rollback. Si choca, 409 reintentable.
- En `CitaEntidad`, un `@PrePersist` que solo asigna una base sin sufijo si `folio == null`, con `updatable=false`. Es la red de seguridad, y además la necesitan las pruebas H2 `create-drop`.
- Reprogramar actualiza la misma fila, así que conserva el folio.

**Relleno V138 (MySQL 8):** todo es DML y tablas temporales, y el `ALTER` va al final. `CREATE TEMPORARY TABLE` no hace commit implícito, así que si falla una guarda se revierte todo lo anterior.
```sql
SET @a := '23456789ABCDEFGHJKLMNPQRSTUVWXYZ';
CREATE TEMPORARY TABLE tmp_folio_reserva (empresa_id BIGINT NOT NULL, clave VARCHAR(64) NOT NULL,
  miembros INT NOT NULL, base CHAR(8) NULL, PRIMARY KEY (empresa_id, clave), UNIQUE KEY (empresa_id, base));
INSERT INTO tmp_folio_reserva (empresa_id, clave, miembros)
  SELECT empresa_id, COALESCE(reserva_grupo_id, CONCAT('CITA:', id)), COUNT(*)
  FROM cita WHERE folio IS NULL GROUP BY 1, 2;
CREATE TEMPORARY TABLE tmp_folio_cita (cita_id BIGINT PRIMARY KEY, empresa_id BIGINT, clave VARCHAR(64), n INT);
INSERT INTO tmp_folio_cita SELECT id, empresa_id, COALESCE(reserva_grupo_id, CONCAT('CITA:', id)),
  ROW_NUMBER() OVER (PARTITION BY empresa_id, COALESCE(reserva_grupo_id, CONCAT('CITA:', id)) ORDER BY id)
  FROM cita WHERE folio IS NULL;
-- 5 rondas; UPDATE IGNORE deja en NULL las filas que chocan con el UNIQUE
UPDATE IGNORE tmp_folio_reserva SET base = CONCAT(
  SUBSTRING(@a,(ASCII(RANDOM_BYTES(1)) & 31)+1,1), /* … ×8 */ ) WHERE base IS NULL;   -- ×5
CREATE TEMPORARY TABLE tmp_guardia (pendientes INT NOT NULL CHECK (pendientes = 0));
INSERT INTO tmp_guardia SELECT COUNT(*) FROM tmp_folio_reserva WHERE base IS NULL;
-- huella previa de las columnas que no se deben tocar
CREATE TEMPORARY TABLE tmp_huella AS SELECT id, SHA2(CONCAT_WS('|', estado, inicio, fin, creada_en,
  cancelada_en, check_in_en, pago_expira_en, precio, precio_lista, descuento, importe_requerido,
  politica_cobro, reserva_grupo_id, politica_version_id, politica_aceptada_en), 256) h FROM cita;
UPDATE cita c JOIN tmp_folio_cita o ON o.cita_id = c.id
  JOIN tmp_folio_reserva r ON r.empresa_id = o.empresa_id AND r.clave = o.clave
  SET c.folio = IF(r.miembros > 1, CONCAT('FL-', r.base, '-', o.n), CONCAT('FL-', r.base))
  WHERE c.folio IS NULL;
-- guardas con el mismo truco CHECK: 0 citas sin folio; 0 diferencias de huella
--   (comparar contra una 2.ª temporal, porque MySQL no deja reabrir la misma temporal en una consulta)
ALTER TABLE cita MODIFY folio VARCHAR(16) NOT NULL;
```
- No se asignan fechas, montos ni aceptaciones, ni se usa `NOW()`. La tabla `cita` no tiene columnas `ON UPDATE`.
- `RANDOM_BYTES` exige binlog en formato ROW (es el predeterminado en MySQL 8 y Cloud SQL).

## 4. Checkout

**Contrato:**
- La configuración pública añade `politica: {version, vigenteDesde, url}` o `null`.
- `CrearOrdenCheckoutRequest` añade `politicaVersion` (número de versión **por empresa**, no el id global, que es secuencial y da pistas entre tenants) y `aceptaPolitica` (Boolean).

**V137, `checkout_orden`:** `politica_version_id NULL` y `politica_aceptada_en NULL`, con el mismo CHECK de los dos NULL juntos y FK compuesta.

**`ServicioOrdenCheckout.crear` (`ServicioOrdenCheckout.java:95-165`):**
- **Línea 101.** Hoy, si la clave de idempotencia ya existe, devuelve la orden sin comparar nada. Si `politicaVersion` difiere de la de esa orden, incluido el caso NULL frente a valor, debe responder 409. Así un reintento no acepta otra política en silencio.
- **Después de calcular las líneas:**
  - Si hay una línea `SERVICIO`, exigir `Boolean.TRUE.equals(aceptaPolitica)` y que `politicaVersion` sea la vigente; si no, 409 `POLITICA_REEMPLAZADA` o 400.
  - Guardar en la orden `politica_aceptada_en = reloj.instant()`. La hora del navegador no se usa nunca.
  - Si la orden es solo de productos, no se guarda aceptación (R1).

**`ServicioPagoCheckout.pagarConTarjeta` (`:65-121`), antes de `inventario.reservar`:**
- Orden con líneas `SERVICIO` y sin aceptación (órdenes de antes de esta entrega) → 409 «vuelve a confirmar tu reserva».
- Versión aceptada que ya no es la vigente → 409. Es el momento en que se mueve el dinero, y la nueva orden puede reutilizar el apartado de la cita.
- `pagarConMetodoAlternativo` solo admite productos (`:139`), así que no cambia.

**Grabar la aceptación en la cita:**
- Se hace al aprobarse el pago, no al crear la orden. Una orden sin pagar no compromete la cita y es normal tener varias órdenes por cita.
- En `ServicioEstadoPagoCheckout.java:54`, rama `APROBADO`, **antes** de `citas.confirmar(...)` y aunque luego haya incidencia: el pago existió. Las citas del grupo se bloquean (`grupoDeOrden`) y a cada cita con línea `SERVICIO` en la orden se le copian los dos valores de la orden **solo si su política es NULL**.
- Si ya tiene otra, no se reescribe: se registra un aviso. Una «liquidación de saldo» posterior no cambia la política de la cita.
- Un webhook de un intento empezado antes de esta entrega deja la cita en NULL. No se afirma una aceptación que no hubo.
- El aviso de reemplazo no se repite en la rama ya pagada (`:39-43`).

**Bloqueo de activación:**
- `ServicioConfiguracionCheckout.actualizar` (`:61-85`): `habilitado=true` sin política publicada → 409.
- Además, compuerta efectiva:
  - `obtenerPublica` (`:96`): `habilitado` exige también política publicada.
  - `ServicioPoliticaReservaPago.puedeCobrarOnline` (`:20-23`): la misma condición, para que una configuración antigua no siga cobrando.
- `requiereAnticipo` del servicio (`ServicioAdminCitas.java:2965`): bloquear solo el paso de false a true, para que se puedan seguir editando los servicios que ya lo tienen.
- Impacto: producción está en V58, sin tablas de checkout, así que la compuerta solo afecta a datos locales y a fixtures de pruebas.

## 5. WhatsApp y página pública

**Plantillas:**
- Tienen variables fijas y numeradas (`ServicioMensajesWhatsappCitas.java:94-257`). Añadir una variable 6 o 7 exige nueva aprobación del proveedor, y registrar plantillas es una acción externa que no está autorizada.
- En 007-1 **no se cambian ni el SID ni las variables**.
- Si más adelante se aprueban plantillas con folio, se dan de alta como un uso nuevo en `ServicioPlantillasWhatsappEmpresa`, con las actuales como respaldo. Queda registrado como límite.

**Mensajes libres y bot (aquí sí se cambia):**
- Los mensajes libres de `:259-363` y la respuesta de `ServicioWhatsappCitas.java:2175` hoy muestran `cita.getId()` como «Folio» y en `CONFIRMAR/CANCELAR/REAGENDAR`. Pasan a mostrar el folio.
- **`resolverCitaPorOpcion` (`:2324-2340`) tiene que aceptar el folio:** si no, los clientes que respondan `CONFIRMAR FL-…` reciben un error de parseo.
  - Orden de resolución: número de opción → folio normalizado (mayúsculas, se añade `FL-` si falta) → id numérico, temporalmente y solo para mensajes ya enviados.
  - Todo filtrado a las citas del teléfono.
- Actualizar `ayuda()` (`:2201-2215`).
- Los correos que hoy dicen «Folio #id» también cambian: `ServicioCorreoCitas.java:80,142,273` y `ServicioCorreoEventosCita.java:196,223`.

**Página pública:**
- `GET /api/v1/publico/empresas/{slug}/politicas`, solo la versión vigente.
- Sitio no publicado, slug inexistente y empresa sin política dan el mismo 404, con el mismo mensaje.
- DTO: `nombreComercial, version, vigenteDesde (Z), modoNoShow, diasReagendar, diasAclaracion, textoGenerado, textoAdicional, contactoReclamos{correo,telefono}`. Sin ids, usuarios, recordatorios ni datos de pago.
- `Cache-Control: public, max-age=60`. **No se implementa `?version`** hasta tener la verificación de 007-3.

## Alertas
1. **Despliegue (A1):** si una revisión vieja sigue escribiendo citas sin folio durante V137–V138, el `MODIFY NOT NULL` falla. Hay que migrar sin tráfico de escritura y ensayarlo sobre una copia. El DDL de V137 no tiene rollback.
2. **Pruebas H2:** usan `create-drop` desde las entidades, así que los CHECK, las FK compuestas, `RANDOM_BYTES` y V138 solo se prueban en el MySQL temporal. Hace falta una prueba que corra V137+V138 sobre datos sembrados: grupo, cita individual, cita legada con grupo NULL, dos tenants y huella sin cambios.
3. **El folio no es una credencial:** ningún endpoint público de 007-1 lo acepta solo. Los ids numéricos de cita siguen en los contratos autenticados y en `citaIds` del checkout; quitarlos queda fuera de esta entrega.
4. **Caja y Recepción:** la búsqueda por folio normaliza la entrada y se limita a empresa y sucursal con el RBAC actual. Booking, el panel del cliente, Recepción y Caja muestran el folio que viene en los DTO; el frontend no lo calcula.

## Pruebas mínimas
- Publicación:
  - versión `max+1` con bloqueo y 409 si `versionBase` está desactualizada;
  - contenido igual no crea versión;
  - la versión anterior no cambia;
  - permiso y tenant;
  - CHECK por modo.
- Activación:
  - 409 sin política;
  - la compuerta efectiva bloquea una configuración antigua.
- Checkout:
  - falta de aceptación, `false` o versión reemplazada → 409, tanto al crear la orden como al pagar;
  - misma clave con otra versión → 409;
  - orden legada con servicio → 409 al pagar;
  - aprobación con reloj fijo: escribe la cita solo si estaba en NULL, no reescribe y también lo hace con incidencia.
- Folio:
  - formato y alfabeto;
  - sufijos del grupo;
  - unicidad entre tenants (el mismo folio en dos empresas es válido);
  - `@PrePersist`;
  - bot con folio, opción e id legado;
  - V138 en MySQL temporal.
- Endpoint público: 404 idéntico en los tres casos, sin campos internos y sin datos de otras empresas.
