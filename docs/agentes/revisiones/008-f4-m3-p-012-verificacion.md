# 008 P (retorno a CSR) + 012: verificación ejecutada por Claude

Autor: Claude Code, 2026-10-21. Las revisiones intermedias (`p1`…`p3`, `retorno-csr*`, `012-fase1*`, `012-seo*`) fueron de solo lectura. Aquí **ejecuté** las verificaciones sobre el working tree actual. No cambié código ni desplegué. No hice consultas reales de DNS ni de Firebase, ni leí `.env`, `postman/` o credenciales.

## Resultados
| Verificación | Resultado | Frente a Codex |
|---|---|---|
| Node: `functions` y `scripts` | 55 + 120 = **175/175** | igual |
| `npm run build` (marketing, app y clientes) | exit 0. Marketing **503.93/138.69 kB**, app **827.15/168.27 kB** | igual |
| Marca de clientes | `e33862baea08…`, reproducida | igual |
| `check-public-builds`, `check-marketing-csr dist/marketing` y `check-clientes-site` | verdes. `index.html` es igual byte a byte a `index.csr.html` (9930 B); `prerendered-routes.json` = `{routes:{}}`; clientes sin `index.html` | igual |
| `npm test` normal (Node 20.19, `TZ=America/Mexico_City`) | **3 corridas seguidas 387/387, 92/92** (49.9, 54.8 y 63.3 s), sin ENOSPC ni timeouts | **cubre el faltante** de Codex |
| Backend (`mvn -o test`, Java 21) | 693 ejecutadas, **0 fallos, 0 errores**, 23 omitidas | ver límite 1 |
| `git diff --check` | limpio | — |

**Límites:**
1. Mi entorno no tiene las credenciales del MySQL de pruebas, así que se omitieron las 8 clases `*MysqlTest`, entre ellas `DominiosMysqlTest` (2) y las de migraciones. **No verifiqué V141/V142 contra MySQL.** Eso descansa en el 697/0/0/1 de Codex. La diferencia de conteo (693 frente a 697) no la puedo explicar desde aquí.
2. No repetí las sondas en navegador, el emulador ni Lighthouse.

## Revisión del código de 012 (complementa las revisiones previas)
Lo que confirmé leyendo el código:
- **D1 sin hardcodear:**
  - V141 solo agrega el catálogo (módulo `DOMINIO_PROPIO`, límite `DOMINIOS_PROPIOS_MAX` y el permiso de plataforma), con el comentario «No asigna módulos ni límites a ningún plan».
  - `check-domain-plan-policy.test.mjs` falla si aparecen nombres de plan o inserciones en `plataforma_plan_*`, y exige `exigirModulo` y `exigirLimite`.
- **Propiedad:** `hostname_reservado` es una columna generada y única, solo para los estados con propiedad comprobada. Los pendientes no reservan el host.
- **Lectura pública:** solo resuelve dominios `ACTIVO` con la capacidad vigente (`ServicioConsultaDominios.empresaActiva`), sin caché.
- **SSRF** (`ComprobadorHostingHttps`):
  - el resolver DNS es el mismo que valida que la IP sea pública y el que usa la conexión;
  - sin redirecciones, 5 s, 1 KB como máximo y cuerpo exacto `fluora-host:<sitio>:<sha256>`;
  - IPv6 solo global unicast.
- **CORS dinámico:** solo para `/api/v1/publico/**` y el registro de cliente, `https` exacto, sin credenciales y solo si el dominio está `ACTIVO`.
- **El job DNS y HTTPS** queda **desactivado por defecto** (`DOMINIOS_VERIFICACION_AUTOMATICA:false`).

## Hallazgos nuevos
| # | Prioridad | Ubicación | Problema | Propuesta |
|---|---|---|---|---|
| N1 | **Media (operativa)** | `.firebaserc` (target `clientes` → `fluora-clientes`), runbook `firebase deploy --only hosting:marketing,hosting:app,hosting:clientes` | El target apunta a un sitio **que no existe**. Cualquier despliegue de Hosting que incluya `clientes`, o un `firebase deploy` sin `--only`, falla o queda **a medias** (el runbook ya dice que no es atómico). | En el runbook: «hasta crear el sitio, desplegar solo `--only hosting:marketing` o `hosting:app`». Validar con un `firebase target` de solo lectura antes de desplegar. |
| N2 | **Media (operativa)** | `functions/index.js`: `defineString('FLUORA_CLIENTES_ORIGIN')` sin valor por defecto | El **próximo despliegue de Functions, por cualquier motivo** (por ejemplo, una corrección de `tenantPage`), va a pedir ese parámetro o va a fallar, aunque el sitio clientes no exista. Además, `customDomainPage` se desplegaría sin uso. | Documentar en el runbook que el valor se fija **solo** cuando exista el sitio. Mientras tanto, desplegar Functions con `--only functions:apiProxy,functions:tenantPage,...` sin `customDomainPage`, o dar un default vacío que ya falla cerrado (503). Codex elige y lo prueba. |
| N3 | Baja | `check-domain-plan-policy.test.mjs:3` | Las raíces revisadas no incluyen `site-domains.component.*`, `functions/custom-domain-page.js` ni `docs/dominios-propios.md`. Hoy no hay nombres de plan ahí (lo comprobé con grep), pero la guarda no los protege. | Agregar esas rutas a `roots`. |
| N4 | Baja | `ConfiguracionCors.java` | Cada petición pública con un `Origin` https ajeno hace una consulta indexada a la base, y una más de capacidades si está activo. Es un amplificador pequeño. | Opcional: una caché negativa y positiva de 60 s como máximo, que respeta la regla de ≤60 s. |
| N5 | Info | `PrepararLegadoDominiosFlyway` | Los `dominio_principal` del legado con formato inválido se ponen en `NULL` antes de V141 y solo se registra el conteo, no el valor. Es aceptable porque no están verificados. | Antes del despliegue, con autorización del usuario, una consulta de solo lectura en producción: cuántos `dominio_principal` existen y cuántos son inválidos. |

## Entorno
- **El disco está al 98 %** (≈5 GB libres). `.angular/` ocupa **9.3 GB**: es la caché de build de Angular y se regenera sola. Fue la causa probable del ENOSPC de Codex. Recomiendo que el usuario autorice borrar `.angular/cache`. No lo borré.
- El backend 8080 cambió de PID (ahora 17725) por el reinicio que hizo el usuario. 4200 sigue igual.

## Dictamen
- **P y el retorno a CSR, más 012, quedan verificados en local.** El faltante de las tres corridas de Angular queda cubierto. No hay bloqueos de código. N1 y N2 son condiciones **antes de cualquier despliegue**; no bloquean un commit.
- **Commit (lo autoriza el usuario):** recomiendo dos commits si se pueden separar limpiamente: (1) 008 P con el retorno a CSR y la configuración del sitio clientes; (2) 012. Fuera: `AGENTS.md` y las revisiones 005, 006, 007 y el plan de rendimiento. Antes: búsqueda de secretos y `git diff --check`.
- **Para el usuario, sobre F4:** en la misma serie alternada, el prerender dio **82** y el CSR **71** (+11). Los valores absolutos se midieron con la batería al 9 % descargándose, lo que les resta confianza, aunque no a la comparación entre los dos. Volver a CSR es una decisión válida y ya está tomada. F4 sigue abierta, por debajo de 85.
