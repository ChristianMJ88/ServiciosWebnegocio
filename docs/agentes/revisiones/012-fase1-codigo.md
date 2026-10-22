# Revisión 012, pasos 1–5: implementación backend y frontend

Solo leí archivos. No ejecuté comandos ni pruebas, y no consulté DNS, Git ni datos. Los resultados de pruebas que menciona la solicitud (MySQL efímero, las 59 focales y el cliente HTTP simulado) son de Codex; no los comprobé. Como solo tengo herramientas de lectura, esta revisión no quedó guardada en `revisiones/`. Si la quieres en el canal, Codex puede copiarla a `revisiones/012-pasos-1-5-codigo.md`.

## Lo que se pidió verificar y está bien

- **Conteo bajo bloqueo:** `solicitar` bloquea la empresa, exige el módulo y llama a `exigirLimite` con las filas que no están `RETIRADO`, incluidas las pendientes (`ServicioDominios.java:40`).
- **Unicidad generada:** V141 tiene `hostname_reservado` y `clave_vigente` como columnas STORED con índice único, más un índice por `hostname`. No asigna nada a ningún plan (`V141:19-28,48`).
- **Red fuera de los bloqueos:**
  - La fase 1 cierra la transacción antes de consultar DNS o HTTPS (`:51-60`).
  - La fase 2 bloquea las filas del host y vuelve a comprobar versión, estado y token con `isEqual` (`:62-63`).
  - Los TXT válidos de las dos empresas terminan en `VERIFICADO` para una y `PENDIENTE` con `HOST_EN_USO` para la otra (`:67`).
- **Lectura pública con capacidad vigente:**
  - `empresaActiva` exige `ACTIVO`, suscripción vigente y el módulo.
  - `dominioPublico` oculta el dominio en la lectura por slug.
  - Las dos respuestas llevan `no-store` (`ServicioConsultaDominios.java:16-26`, `ControladorSitioPublico.java:28,34`).
- **Retiro tras el vencimiento:**
  - El interceptor no exige suscripción en `/admin/dominios` (`InterceptorModuloContratado.java:32-34`).
  - `retirar` no exige el módulo.
  - Las mutaciones de pago sí lo exigen en el servicio.
- **Auditoría sin tokens:** solo se guardan host y estado (`ServicioAuditoriaDominios.java:13-14`). El DTO de plataforma usa `vista(d,false,…)`, que no lleva token. Los errores son códigos fijos y ninguna excepción externa llega a `ultimo_error`.
- **H1 cerrado:** fuera de `ServicioDominios.java:93` ya no hay ningún `setDominioPrincipal`.
- **Frontend:** muestra `acciones`, `incluido` y `puedeAgregar` tal como los envía el backend. El enlace del menú depende de `PLATAFORMA_DOMINIOS_GESTIONAR` y el backend responde 403 si falta el permiso.
- **Defensas SSRF ya presentes:**
  - El host del resolver se fija y se comprueban **todas** las IP antes de conectar, usando la misma respuesta DNS.
  - Sin proxy del sistema, ya que no se usa `useSystemProperties`.
  - Sin redirects ni reintentos automáticos.
  - Ruta fija y puerto 443.
  - Límite total de 5 s y lectura de 1025 bytes como máximo.

## Hallazgos

| # | Prioridad | Ubicación | Problema | Corrección y prueba propuestas |
|---|---|---|---|---|
| 1 | **Alta** (bloquea activar el job) | `TareaComprobacionDominios.java:16-17`, `ServicioDominios.java:51-54`, `EmpresaDominioRepositorio.java:18` | **El job deja otras filas sin comprobar.** Cuando una fila falla con 402 (sin módulo o suscripción vencida) o con 409, `ultimo_chequeo_en` no se actualiza porque la transacción se revierte. Como los candidatos se ordenan por esa fecha de forma ascendente (con los NULL primero), si hay al menos `lote` filas así, el job nunca llega a las demás. V141 crea un pendiente por cada `dominio_principal` heredado, muchos de planes sin el módulo, así que el caso es realista. Además, los pendientes abandonados se consultan cada 5 min para siempre. | Elegir los candidatos solo entre empresas con capacidad, o agregar en V141 `proximo_chequeo_en` con espera creciente (aún no está aplicada fuera del MySQL efímero), que se actualice en una transacción propia también ante 402/409. **Prueba:** 60 filas sin módulo y 1 con módulo, con `lote=50`; la fila válida se comprueba en 2 ejecuciones como máximo. |
| 2 | **Media** | `ServicioDominios.java:54,74-77`, sin manejador en `dominios` | **Un bloqueo optimista termina en 500.** No hay manejador para `ObjectOptimisticLockingFailureException` (solo existe en otros módulos, como `ServicioSoportePlataformaImpl.java:49`). Ejemplos:<br>- La fase 1 de B lee su fila con versión v; entre tanto, la fase 2 de A, que no bloquea la empresa B, retira esa pendiente; el `UPDATE … version=v` de B falla.<br>- `retirar` y `conectar` concurrentes con una fase 2.<br>- Igual con `PessimisticLockingFailureException` o un deadlock.<br>El contrato pedía «nunca un 500». | Convertir esas excepciones en 409 dentro del servicio, o con un `@RestControllerAdvice` limitado a `dominios`. **Prueba MySQL:** `retirar` de B mientras A verifica y libera la pendiente de B → 409 o éxito, nunca 500. |
| 3 | **Media** | `V141:30-33` | **La migración con datos heredados puede romper el arranque.** Solo aplica `LOWER`. Con un valor no ASCII en una columna `ascii` y modo estricto, Flyway falla. Con un punto final, puerto, ruta o más de 253 caracteres, se insertan hosts que nunca van a verificar. No sé qué aceptaba el normalizador anterior, porque ya se quitó. | Filtrar con un `REGEXP` de hostname en ASCII de 253 caracteres como máximo. Los demás valores solo se ponen en NULL; hoy no se sirven. **Prueba:** fixtures con `Marca.MX`, `ñandú.mx`, `x.mx.` y `x.mx:443`. Contar los casos reales en producción necesita autorización del usuario. |
| 4 | **Media** | `ServicioDominios.java:59,76`, `ComprobadorHostingHttps.java:40` | **La marca depende del build de cada fila.** El operador captura el hash del build en cada fila. Después de cada despliegue de `clientes`, todas las filas `CONECTANDO` pasan a `ERROR` hasta que se vuelva a capturar fila por fila. | Usar una marca estable del sitio `clientes`, por configuración, y no el build por fila. Hay que alinear el contenido de `fluora-host.txt` con P. |
| 5 | Media | `ServicioDominios.java:71` | **La auditoría crece sin control.** Cada comprobación, incluidas las fallidas del job cada 5 min, deja un registro `DOMINIO_COMPROBADO`. | Auditar solo cuando cambian el estado o `ultimo_error`. |
| 6 | Baja | `ServicioDominios.java:53,68` | **La reserva depende de la frecuencia.** La reserva técnica dura `frecuenciaSegundos`. Si la frecuencia se configura por debajo de unos 10 s (DNS de ~3 s y HTTPS de 5 s), la reserva vence a mitad de la comprobación y el perdedor recibe un 409 en vez de `HOST_EN_USO`. | Fijar una duración propia: `max(frecuencia, timeouts + margen)`. |
| 7 | Baja | `ServicioDominios.java:65-67` | **El mensaje no refleja que el host está en uso.** Si la pendiente de B se salvó por la reserva pero su TXT no coincide, queda con `TXT_NO_ENCONTRADO` aunque el host ya lo reservó otra empresa. | Si hay otra fila que reserva el host, marcar `HOST_EN_USO` aunque la comprobación falle. |
| 8 | Baja (defensa extra) | `ComprobadorHostingHttps.java:48-50` | **Rangos de red que faltan excluir.** `2000::/3` incluye `2002::/16` (6to4) y `2001::/32` (Teredo), que encapsulan IPv4 privadas. En IPv4 falta `192.88.99.0/24`. Explotarlo requiere una ruta 6to4, algo improbable en Cloud Run. | Excluir esos rangos y probarlos en una tabla en `ProteccionHostingTest` (no revisé ese archivo). |
| 9 | Baja | `ComprobadorHostingHttps.java:26`, `ResolverTxtDns.java:11` | **Los timeouts no se respetan del todo.** `getAllByName` no se interrumpe, así que el hilo virtual puede seguir vivo después de los 5 s. El TXT no tiene un timeout total: son 1 s más 2 s por cada servidor DNS. Las dos consultas corren fuera de los bloqueos, así que no afectan la concurrencia. | Documentarlo. Si se quiere, agregar `disableContentCompression()` para comparar los bytes exactos. |
| 10 | Baja | `admin-site-section.component.html:195,205` | **Enter guarda el sitio en vez de solicitar el dominio.** `<app-site-domains>` está dentro del `<form>` del sitio, que tiene botón de envío. Al pulsar Enter en el campo del dominio se guarda la configuración del sitio. | Darle al componente su propio `<form (ngSubmit)>` o evitar el Enter. Agregar una prueba de componente. |
| 11 | Baja | `admin-site.forms.ts:91`, `admin-site-section.component.ts:108` | **Restos del campo anterior.** El formulario sigue enviando `dominioPrincipal`, que el backend ignora. La vista previa muestra `https://<derivado>` aunque el plan no tenga la capacidad. | Quitar el campo del payload y la vista previa. |
| 12 | Baja | `ServicioDominios.java:80` | **El aviso de retiro nunca se cierra.** `pendienteRetiroHosting` sigue en verdadero para siempre y no hay forma de marcarlo como hecho. La cola además hace unas 3 consultas por fila. | Aceptable en la fase 1; dejarlo anotado. |

**Sobre SSRF:** solo se puede llegar al cliente HTTPS después de dos pasos: demostrar la propiedad por TXT y que un operador haga `conectar`. El resultado es un booleano y solo se conecta a IP públicas en el puerto 443. No encontré ninguna forma de saltarse la validación de IP: el host es fijo, no hay redirects, no hay una segunda resolución DNS y los literales de IP ya se rechazan en el validador.

## Pruebas del contrato que no encontré

Verificar o agregar:
- **#5:** la misma empresa pide el mismo host dos veces a la vez, en MySQL.
- **#6:** N+1 solicitudes simultáneas contra el límite, en MySQL. La prueba actual simula `ServicioCapacidadesEmpresa`, así que `exigirLimite` no hace nada.
- **#8:** los logs capturados no contienen tokens ni valores TXT.
- **#11:** un `PUT` con `dominioPrincipal` no lo cambia.
- **La guarda contra el hardcodeo de §8:** que falle si el código menciona códigos o nombres de plan.
- **El retiro con la suscripción vencida**, pasando por el interceptor real.
- **Los hallazgos 1 y 2.**

## Riesgos que hay que resolver antes de §6 (SEO)

1. **Llamadas desde el dominio propio (bloqueante, verificar).** `CORS_ORIGENES` es una lista fija (`application.yml:197`). Un sitio servido en `marca.mx` que llame directamente a la API fallará por CORS. Hay que confirmar que `clientes` envía `/api` a la API a través del proxy en el mismo origen (`functions/index.js`). Aplica lo mismo a reCAPTCHA, a los dominios autorizados de Auth y a las `back_urls` de pagos si el flujo público las usa.
2. **No confiar en `x-forwarded-host`.** La Function siempre debe resolver con `/publico/sitio/host/{h}`, que ya aplica `ACTIVO` y la capacidad. El canonical y el og:url se arman con el `dominioPrincipal` que devuelve el backend, escapado, nunca con el header.
3. **Caché del CDN.** Las respuestas 200 deben ir con `no-store` o con 60 s como máximo (§7.1). Hay que confirmar que la clave de caché de Hosting incluye el host. Un 404 guardado 60 s retrasa la activación como mucho un minuto, y es aceptable.
4. **El rewrite `**` también atrapa los assets que faltan.** Una ruta con extensión (`.js`, `.css`, `.png`) debe dar 404 y no devolver HTML. `fluora-host.txt` debe seguir siendo un archivo estático. `robots.txt` y el sitemap por dominio no pueden ser un archivo estático compartido. El sitemap global no lista dominios propios mientras tenga `s-maxage=3600`.
5. **Contenido duplicado.** Hay que decidir hacia dónde apunta el canonical: si `/e/slug` en marketing apunta al dominio propio cuando está publicado, o si se redirige. Cuando se pierde la capacidad, el canonical debe volver a marketing en el siguiente request, sin caché.
6. **Dominios retirados que siguen en Firebase.** Mientras el operador no los quite, la Function debe responder 404 con `noindex`, sin redirigir al tenant. El backend ya responde 404.
7. **Costo.** Cada navegación invoca la Function y hace una llamada al backend. Un 404 rápido para hosts desconocidos y la estimación de costo por visita deben estar listos antes de desplegar (§6).

**Prioridad para Codex:** primero los hallazgos 1 y 2 con sus pruebas MySQL, luego 3 y 4 (4 se coordina con P), después las pruebas que faltan y por último las bajas. §6 debería empezar con el punto 1 de la lista anterior verificado.
