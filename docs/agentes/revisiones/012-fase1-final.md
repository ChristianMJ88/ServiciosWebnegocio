# Revisión 012 fase 1: cierre antes de §6

Solo leí archivos: no ejecuté comandos, pruebas ni consultas. Los resultados de Java 21 con MySQL efímero y de Angular los reporta Codex y no los comprobé. Tampoco puedo escribir en el canal; si la quieres en `revisiones/012-fase1-final.md`, Codex tendría que copiarla.

**Conclusión: nada bloquea §6.** Las correcciones C1, C2, H1, H2, H3 y H4 están bien implementadas. El CORS dinámico tiene el alcance correcto. Dejo dos endurecimientos bajos y dos cosas para verificar en §6. La tarea no está terminada: siguen pendientes P3, P4 y las validaciones visuales.

## Correcciones que verifiqué

- **C1** (`PrepararLegadoDominiosFlyway.java:13-14`): agrega `LENGTH<>CHAR_LENGTH`, `REGEXP_LIKE(…,?,'c')` y el patrón parametrizado que termina en `\z`. El `\z` llega a MySQL sin escapes de cadena SQL. Los datos de prueba incluyen `ſ.mx`, `x.mx\n` y `" x.mx"` (`DominiosMysqlTest.java:46`).
- **C2**:
  - El log solo registra el número de filas (`:15`).
  - `ApplicationContextRunner` con `FlywayAutoConfiguration` confirma que el bean está en `getCallbacks()` y que V141 no falla con datos no ASCII (`:47-51`). Ahora la ejecución real pasa por Spring.
  - La prueba separada migra hasta 141 sin el callback y luego a 142. Comprueba que `x.mx.` y `x.mx:443` quedan `RETIRADO`/`DOMINIO_LEGADO_INVALIDO` (`:107-110`).
- **C3**: según Codex, el historial local solo tiene V141 (checksum 1601200476) y V142 no está aplicada. Por eso es válido corregir el ancla de V142 (`V142…sql:11`). V141 no se tocó. A V142 no le hace falta la comprobación de bytes porque `hostname` es `ascii_bin`.
- **H1**: los intentos y `proximo` vuelven a cero al pasar a `VERIFICADO` (`ServicioDominios.java:71`) y a `CONECTANDO` (`:79`).
- **H2**: `porFrecuencia` llama a `descontarIntento`, que nunca baja de cero (`EmpresaDominioRepositorio.java:22`). Queda un detalle menor: `proximo_chequeo_en` sigue usando la espera calculada con el intento antes de descontarlo. Es aceptable.
- **H3**: se captura cada fila y el log solo incluye el tipo de error (`TareaComprobacionDominios.java:19`).
- **H4**:
  - La rama muerta ya no está.
  - El error ahora es un código, `LIBERADO_POR_OTRA_CUENTA` (`:70`).
  - `domains.service.ts:22` traduce los códigos.

## CORS dinámico (`ConfiguracionCors.java:30-50`)

**Lo que está bien:**
- La excepción dinámica solo se aplica a `/api/v1/publico/**` y a la ruta exacta `registrar-cliente`. Admin, plataforma, login, refresh y onboarding no reciben origen dinámico.
- Solo acepta `https://<host>` exacto: sin puerto, ruta ni mayúsculas distintas.
- Exige un dominio `ACTIVO` y que la capacidad esté vigente (`ServicioConsultaDominios.java:16-21`).
- Si hay error o el servicio no está disponible, usa la configuración base.

**Riesgo con credenciales: bajo.** La configuración dinámica hereda `allowCredentials(true)`. Un tenant con su dominio activo podría hacer peticiones con credenciales directo al host de la API. Hoy no sirve de nada:
- `registrar-cliente` responde 204 sin poner cookie (`ControladorAutenticacion.java:91-94`).
- `fluora_refresh` está limitada a `/api/v1/auth` y ningún endpoint público la lee.

| # | Prioridad | Ubicación | Problema | Corrección y prueba |
|---|---|---|---|---|
| F1 | Baja | `ConfiguracionCors.java:44-45` | El origen dinámico hereda `allowCredentials(true)` y `exposedHeaders(Authorization)`. A través del proxy la petición es del mismo origen, así que el navegador no usa esas cabeceras. Solo dejan abierta una superficie que no se necesita. | `permitida.setAllowCredentials(false)` y quitar los headers expuestos en esa copia. **Prueba:** en `CorsDominiosTest`, `getAllowCredentials()` es `false` para `https://marca.mx` y sigue `true` para el origen base. |
| F2 | Baja | `CorsDominiosTest.java:10-16` | Faltan casos negativos. La línea 15 se llama "platform" pero usa `/api/v1/admin/clientes`. | Agregar `/api/v1/auth/iniciar-sesion`, `/api/v1/auth/refrescar-token` y `/api/v1/plataforma/…` con origen de dominio activo, que deben dar `null` sin llamar a `consulta`. Agregar también `http://marca.mx` y `https://marca.mx:443`, que deben dar `null`. |
| F3 | Baja (verificar) | `V142…sql:11` | Si el servidor tiene `NO_BACKSLASH_ESCAPES` en `sql_mode`, `\\z` deja de ser el ancla `\z`. Entonces ningún host coincide y V142 retiraría **todos** los `PENDIENTE_VERIFICACION`. El modo por defecto de MySQL 8 y Cloud SQL no lo incluye. | Antes de desplegar, confirmar que el `sql_mode` de producción no lo tiene. No editar V142 si ya está aplicada en algún entorno. |

## Para §6, sin ampliar alcance

- **Límite de solicitudes detrás del proxy.** `FiltroLimiteSolicitudes.java:72` usa `getRemoteAddr()` con `forward-headers-strategy: framework` (`application-prod.yml:23`). Hay que confirmar que `apiProxy` reenvía `X-Forwarded-For` con la IP del cliente. Si no, todas las peticiones desde dominios propios compartirán una sola IP y quedarán limitadas a 5 registros por hora entre todos los visitantes.
- **Costo por petición.** Cada petición pública con un `Origin` que no está en la lista base consulta la base de datos (`findByHostnameAndEstado` y `capacidades.obtener`) y no hay caché. Tiene el mismo orden de costo que el endpoint público, así que no bloquea. Encaja con el costo y el canonical que ya están previstos en §6.

## Orden sugerido

F1 y F2 pueden ir junto con §6, y F3 se revisa antes de desplegar. No se tocó producción, el historial de Flyway, DNS, planes, correos ni el despliegue.
