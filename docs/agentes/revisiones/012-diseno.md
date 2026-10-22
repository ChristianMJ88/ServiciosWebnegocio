# 012: Dominios propios para negocios (fase 1, conectar un dominio que el negocio ya tiene)

Autor: Claude Code, 2026-10-20. El usuario aprobó ofrecer dominios propios («si adelante con todo»). Alcance de **esta** tarea: el **caso B**, en el que el negocio ya tiene un dominio y lo conecta. La **venta de dominios** desde Fluora (caso A: API de un registrador, cobro y renovaciones) queda **fuera**: es una decisión comercial con proveedor y contrato, y se diseña aparte cuando el usuario lo pida. Depende del sitio de Hosting `clientes` de `revisiones/008-f4-m3-p-diseno.md` §3.

**No autorizado sin una autorización explícita del usuario:**
- crear el sitio de Hosting o conectar dominios en Firebase;
- desplegar;
- ejecutar consultas en la base de producción;
- enviar correos.

## 1. Lo que existe hoy (verificado)
- `empresa_sitio_config.dominio_principal`: V33, índice único.
- Escritura desde el panel del negocio: `ServicioAdminCitas.java:559-565`. Normaliza el host (`:2655`) y solo valida que **ningún otro tenant** lo tenga (`:2627-2637`).
- Lectura pública: `GET /publico/sitio/host/{hostname}` (`ControladorSitioPublico.java:31`) → `findByDominioPrincipalIgnoreCase` (`ServicioSitioPublico.java:62`).
- El frontend resuelve el negocio por host con `tenantHostResolver`, cuando el host no es de marketing ni de la app.
- Las capacidades por plan ya existen (`ServicioCapacidadesEmpresa`, `InterceptorModuloContratado`).
- Hoy no hay ningún dominio de cliente conectado en Firebase (verificado en R2).

## 2. Hallazgos que motivan el diseño
| # | Prioridad | Evidencia | Problema |
|---|---|---|---|
| H1 | **Alta** | `ServicioAdminCitas.java:2627-2637` | **No se verifica la propiedad.** Un negocio puede registrar el dominio de otro, por ejemplo el de la competencia, y apartarlo por el índice único. El dueño real no podría usarlo |
| H2 | Alta | `ServicioSitioPublico.java:62` | El sitio público resuelve **cualquier** valor guardado, aunque el dominio no esté verificado ni conectado |
| H3 | Media | `admin-site-section.component.html:198` | Es un campo de texto suelto: el negocio no sabe qué registros DNS poner ni en qué estado está |
| H4 | Media | `functions/index.js:62-124` | `tenantPage` solo entiende `/e/:slug`. En un dominio propio, el HTML inicial lleva el título genérico del shell |

## 3. Modelo
**Tabla nueva `empresa_dominio`.** Codex reserva la versión en ESTADO antes de crearla; la siguiente libre parece **V141**, pero hay que confirmarlo con `git fetch` y las demás ramas.
- `id`, `empresa_id` (FK) y `hostname`: único, en minúsculas, en punycode y sin puerto ni ruta.
- `estado`: `PENDIENTE_VERIFICACION` → `VERIFICADO` → `CONECTANDO` → `ACTIVO`, más `ERROR` y `RETIRADO`.
- `token_verificacion`: aleatorio de 32 bytes, en base64url. No se escribe en los logs.
- `registros_hosting`: JSON con los registros que pide Firebase y que captura la plataforma.
- Tiempos: `verificado_en`, `activado_en`, `ultimo_chequeo_en`. Además `ultimo_error` (texto corto sin datos sensibles), `creado_por` y `version` para el bloqueo optimista.
- La cantidad de dominios por empresa **no se fija en el código**: sale del límite del plan `DOMINIOS_PROPIOS_MAX` (§7). Se valida bajo bloqueo con `exigirLimite`.

**Migración de datos:** los `dominio_principal` que ya existan entran como `PENDIENTE_VERIFICACION`, **no** como `ACTIVO`. Como hoy no hay ningún dominio conectado en Hosting, ningún sitio visible se rompe.
- Codex solo cuenta los existentes en la base local o de pruebas.
- Para saber cuántos hay en producción, el usuario tiene que autorizar una consulta de solo lectura.

`dominio_principal` se mantiene como valor derivado del dominio `ACTIVO`, para no romper los DTO ni el SEO. Se puede retirar después, en una limpieza aparte.

## 4. Flujo
1. **Solicitud (panel del negocio).** Requiere el permiso del sitio que ya usa la configuración y la capacidad de plan `DOMINIO_PROPIO` (D1).
   - Se valida el host. Se rechazan:
     - `refluora.com` y sus subdominios, y el host de la app;
     - `*.web.app` y `*.firebaseapp.com`;
     - `localhost`, las IP y los sufijos públicos, como `com.mx` (con la Public Suffix List);
     - más de 253 caracteres.
   - Se crea en `PENDIENTE_VERIFICACION` y se muestra el registro **TXT** `_fluora.<host>` = `fluora-verificacion=<token>`.
2. **Verificación de propiedad (resuelve H1).** Un job periódico y el botón «Verificar ahora», con límite de frecuencia por empresa, consultan el TXT por DNS.
   - Se usa un resolver con timeout de 5 s, por ejemplo `dnsjava`. Se comparan en tiempo constante.
   - Si coincide, pasa a `VERIFICADO`. Si después de 7 días (configurable) no coincide, sigue pendiente con un aviso; no se borra.
   - **Solo quien controla el DNS puede pasar este paso.** Si otra empresa verifica el mismo host, el registro pendiente anterior se libera.
3. **Conexión en Hosting (manual en la fase 1).** Una vista de plataforma con un permiso nuevo, `PLATAFORMA_DOMINIOS_GESTIONAR`, muestra la cola de dominios `VERIFICADO`.
   - El operador agrega el dominio en Firebase (sitio `clientes`) y captura en Fluora los registros que indica Firebase. Pasa a `CONECTANDO`.
   - El panel del negocio muestra esos registros.
4. **Activación (resuelve H2).** El job comprueba `GET https://<host>/fluora-host.txt`. Es un archivo estático que solo existe en el sitio `clientes`, con un identificador del build.
   - Con HTTPS válido, `redirect: 'error'`, timeout de 5 s y lectura de 1 KB como máximo, pasa a `ACTIVO`.
   - **Protección SSRF:** solo `https`, solo el host verificado, una ruta fija y el rechazo de cualquier resolución a IP privada, loopback o link-local.
5. **Lectura pública:** `obtenerPorHostname` solo resuelve dominios `ACTIVO` y responde 404 para los demás.
6. **Retiro:** lo hace el negocio o la plataforma → `RETIRADO`, y la lectura pública se corta de inmediato. La plataforma ve una tarea «quitar de Firebase». No se borra el historial.

Todas las transiciones validan el tenant, el permiso y la versión en el backend, y quedan en la auditoría existente. El frontend solo muestra el estado que decide el backend.

## 5. Frontend
- **Panel del negocio:** el campo suelto cambia por una tarjeta «Dominio propio»:
  - estado;
  - registros por copiar, con botón de copiar;
  - «Verificar ahora»;
  - guía corta para GoDaddy, Namecheap, Cloudflare y Google o Squarespace (texto, sin capturas de terceros).
- **Vista de plataforma:** la cola por estado, la captura de registros y el retiro.
- **En un dominio propio**, `/e/<slug>` redirige al origen de marketing (pendiente de P §3).

## 6. SEO en dominios propios (H4), segunda parte de 012
- El sitio `clientes` cambia `**` por una Function, `customDomainPage`, **solo para navegaciones HTML** (los assets estáticos se sirven antes que los rewrites). La Function:
  - resuelve el negocio por host con `x-forwarded-host` (hay que comprobar en el emulador qué header llega);
  - reutiliza `renderTenantHtml` y `loadCsrTemplate`, con un canonical al dominio propio.
- Se hace después de que 1–5 estén verdes. Antes de desplegarla hay que estimar su costo por visita.

## 7. Decisiones del usuario (2026-10-20)
- **D1: del plan Profesional en adelante.** Condición del usuario: **«los planes pueden cambiar, por eso no debe quedar nada hardcodeado»**.
- **D2: se aceptan subdominios y dominios raíz.**
- **D3: el DNS lo pone el negocio**, con la guía del panel.

### 7.1 Cómo se cumple D1 sin hardcodear (obligatorio)
Hoy los planes son datos que se editan desde la plataforma: `plataforma_plan`, `plataforma_plan_modulo` y `plataforma_plan_limite`, con `POST`/`PUT /suscripciones/planes` y `PLATAFORMA_SUSCRIPCIONES_GESTIONAR` (`ControladorPlataforma.java:119-130`). Ninguna migración siembra planes. «Profesional» existe solo como dato.
- **Catálogo, no asignación.** La migración solo agrega:
  - el módulo `DOMINIO_PROPIO` a `plataforma_modulo`, con el mismo patrón `ON DUPLICATE KEY UPDATE` de V108 y V109;
  - el límite `DOMINIOS_PROPIOS_MAX` a `plataforma_limite`.
  - **No** inserta filas en `plataforma_plan_modulo` ni en `plataforma_plan_limite`, y **no** menciona ningún código de plan.
- **La asignación la hace el usuario** en el editor de planes: activar `DOMINIO_PROPIO` y fijar el límite, por ejemplo 1, en Profesional y en los planes superiores. Si mañana los planes cambian, se edita ahí, sin desplegar.
  - Codex deja el paso escrito en la respuesta. No lo ejecuta en producción.
- **Prohibido en el código y en las pruebas de producción:** comparar códigos o nombres de plan (`PROFESIONAL`, etc.), suponer un orden o jerarquía de planes («de X en adelante»), o fijar un número de dominios.
  - El backend solo pregunta `exigirModulo(empresaId,"DOMINIO_PROPIO")` y `exigirLimite(empresaId,"DOMINIOS_PROPIOS_MAX",uso)`.
  - El frontend solo muestra lo que llega en `capacidades`.
  - En las pruebas se usan planes de fixture con nombres neutros.
- **Si el plan pierde el módulo** (por un cambio de plan, al editar el plan o por suscripción vencida):
  - la lectura pública también exige la capacidad vigente, de modo que el dominio deja de servir el sitio, con caché corta de 60 s como máximo;
  - el estado del dominio **no** se borra ni se vuelve a verificar. Si se recupera la capacidad, vuelve a funcionar;
  - el panel muestra «Tu plan actual no incluye dominio propio».
  - Lo mismo aplica si el límite baja por debajo del uso: se conservan los dominios existentes, se bloquea agregar más y la plataforma ve el excedente. No se desactiva ninguno automáticamente sin una decisión del usuario.
- **Copy de la oferta:** el home y la página de planes muestran la característica solo si el plan la incluye, leyendo los módulos del endpoint público de planes. **No** se escribe «desde Profesional» como texto fijo.

### 7.2 D2 · Dominio raíz y subdominio
- **El dominio raíz** (`marca.mx`) usa los registros A que indique Firebase. El panel avisa: «Si ya tienes una página web en este dominio, dejará de mostrarse».
- **El subdominio** (`reservas.marca.mx`) usa el registro que indique Firebase para subdominios.
- **El `www.` de un dominio raíz no se agrega solo.** Si el negocio lo quiere, es un segundo dominio y cuenta para `DOMINIOS_PROPIOS_MAX`.
- **Los dos llevan el mismo TXT de verificación** (`_fluora.<host>`).

### 7.3 D3 · El negocio pone el DNS
- La guía del panel es el camino principal: registros por copiar, «Verificar ahora» y mensajes claros por estado («Aún no vemos el registro TXT; la propagación puede tardar hasta 48 h»).
- **Límite de la fase 1:** el paso de Firebase (§4.3) lo sigue haciendo la plataforma. El negocio ve «En preparación por Fluora» y después recibe los registros finales. La automatización de ese paso es la fase 2.

## 8. Pruebas y criterio de cierre
- **Backend:**
  - validación de host (todos los rechazos de §4.1);
  - transiciones válidas e inválidas;
  - verificación TXT con un resolver simulado;
  - activación con un cliente HTTP simulado (redirect, IP privada, timeout y tamaño);
  - concurrencia: dos empresas con el mismo host, y la que verifica gana;
  - aislamiento por tenant y RBAC;
  - la lectura pública solo devuelve dominios `ACTIVO`;
  - la migración con datos existentes en MySQL efímero;
  - **capacidades (D1):**
    - un plan de fixture sin `DOMINIO_PROPIO` → 402 al solicitar;
    - con el módulo y un límite de N → se acepta hasta N y el siguiente da 402;
    - al quitar el módulo del plan, la lectura pública deja de resolver el dominio y, al devolverlo, vuelve a resolver sin otra verificación;
    - la migración no inserta nada en `plataforma_plan_modulo` ni en `plataforma_plan_limite`.
- **Guarda contra el hardcodeo:** una búsqueda (una prueba o un script de revisión) que falle si el código de 012 menciona códigos o nombres de plan.
- **Frontend:** la tarjeta en todos los estados y la vista de plataforma, en 375, 820 y 1440, sin desbordamiento.
- **Functions** (parte 2): resolución por host, 404 y canonical.
- Las suites normales de backend, Angular y Node en verde.
- **Sin** pruebas contra un DNS o Firebase reales sin autorización.

## 9. Fase 2 (fuera de alcance, para recordar)
- Automatizar el alta en Firebase con su API, o pasar a Cloudflare for SaaS cuando el número de dominios lo pida. Firebase limita los dominios por sitio; hay que confirmar el límite vigente antes de decidir.
- Venta de dominios (caso A).
- Domain Connect para configurar el DNS con un clic.

## Orden
1. P (prerender y sitio `clientes`) según `008-f4-m3-p-diseno.md`.
2. 012 pasos 1–5. D1–D3 ya están decididos (§7). Puede ir en paralelo con P, porque no comparten archivos, salvo que 012 necesita el sitio `clientes` para probar la activación.
3. 012 §6.

Cada paso tiene su respuesta en `respuestas/012.md` y su revisión de Claude. Commit solo con autorización. Sin push ni despliegue.
