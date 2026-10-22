# 008 F4 · P reactivado y P4 repetida: verificación ejecutada por Claude

Autor: Claude Code, 2026-10-21. Ejecuté las verificaciones sobre el working tree actual. No cambié código, no desplegué y no leí `.env` ni valores de credenciales. Las revisiones `008-f4-m3-p-reactivar`, `008-f4-p4-arranque-local` y `008-f4-m3-p4-repeticion-cierre` fueron de solo lectura; esta las complementa con ejecución.

## Resultados
| Verificación | Resultado | Frente a Codex |
|---|---|---|
| P4, recalculada desde los 10 informes crudos (`/private/tmp/fluora-p-p4-repeat-vMKCD0FI/*-{1..5}.json`) | P 75/81/85/87/88 → mediana **85**. CSR 82/82/82/83/83 → **82**. A/BP/SEO 100 en las 10. CLS de P ≤ .009774. LCP `h1#home-title` en las 10. Lighthouse 13.4.1, 41/44 peticiones constantes | igual |
| Orden de la serie (`run.log`) | warmups en 1 y 2; luego AB/BA/AB/BA/AB | igual |
| `npm run build` (marketing, app, clientes) y sus guardas | exit 0. Marketing **503.93/138.71 kB**, app 827.15/168.27 kB. `index.html` con el h1; `index.csr.html` de marketing y de clientes sin h1; `prerendered-routes.json` = `{"/":{}}` | — |
| Huella del build de hoy (`build-fingerprint.mjs`) | **`da5fbb28f393…`**, la misma que se midió | reproducida |
| Node: `functions` y `scripts` | 55 + 135 = **190/190** | igual |
| Angular normal (Node 20.19, `TZ=America/Mexico_City`) | **3 corridas seguidas 387/387, 92/92** (38, 44 y 61 s) | igual |
| `git diff --check` y búsqueda de secretos en diff y no versionados | limpio; la única coincidencia es la línea **eliminada** del `.run` | — |
| Servicios | 4200 (PID 82535, estado `S`) y 8080 (PID 49912) escuchando | igual |

No repetí el backend (`*MysqlTest` siguen sin credenciales en mi entorno), la sonda de hidratación ni Lighthouse.

## Lectura de la medición
- **La batería explicaba buena parte del 71.** El mismo CSR (`86a0c011…`) pasó de 71 (rango 64–82) a 82 (82–83) conectado a la corriente. La serie anterior no servía como valor absoluto.
- **La ventaja de P ahora es +3 de mediana, no +11.** P tiene un FCP bimodal: 1.9–2.1 s en 2 corridas y 2.9–3.4 s en 3. CSR queda estable en ≈3.1 s. Cumple la regla, pero sin holgura: con la corrida 2 un punto más baja, la mediana sería 84.
- **El CLS de P es casi constante, .009774, a .000226 del umbral.** Cualquier cambio en el hero o en las fuentes puede pasarlo. Se debe vigilar en cada cambio del home (la guarda de layout de R3 sirve para eso).
- **Alcance:** la meta queda acreditada en el preview local, no en Hosting.

## Hallazgo nuevo
| # | Prio. | Evidencia | Problema | Propuesta |
|---|---|---|---|---|
| S1 | **Alta (seguridad, decisión del usuario)** | `.run/Backend Spring Boot (dev).run.xml` está versionado desde `2aa9911` (2026-03-20) y ese commit está en `origin/main`, `origin/codex/refactor-roles-permisos` y `origin/codex/operacion-whatsapp-recepcion-caja`. En `HEAD` contiene `-DDB_USER`, `-DDB_PASSWORD` y **`-DJWT_SECRETO`** con valores literales (los conté, no los mostré). `DB_HOST` es `localhost`. | La corrección de Codex quita los literales del working tree, pero **siguen en el historial publicado en GitHub**. Si el repositorio es público, o si ese `JWT_SECRETO` o esa contraseña se usan fuera de la máquina local, cualquiera puede firmar tokens o entrar a esa base. | 1. El usuario confirma si el repo es privado y si el JWT o la contraseña de producción coinciden con los del `.run`. **2. Si coinciden, o si hay duda: rotar `JWT_SECRETO` en producción**, lo que invalida las sesiones, y la contraseña de la base afectada. Requiere autorización. 3. Limpiar el historial (`git filter-repo`) es opcional y no sustituye la rotación. 4. Hacer un commit propio para el cambio del `.run`, separado de P y de 012. |

### S1 · Respuesta del usuario (2026-10-21)
- El repositorio es **privado**. La exposición queda limitada a quien tiene acceso al repo, y la prioridad baja a **media**.
- No recuerda si el `JWT_SECRETO` de producción coincide con el del `.run`. Como no se puede descartar, **la recomendación es rotarlo** en el próximo despliegue del backend, o antes: un valor nuevo de al menos 32 bytes aleatorios (`openssl rand -base64 48`), guardado solo en la configuración de producción. Efecto: las sesiones abiertas se cierran y cada usuario vuelve a iniciar sesión. La rotación la hace el usuario o se autoriza explícitamente; Claude no consulta ni toca producción.
- Al rotarlo, comprobar también que producción **define** `JWT_SECRETO`. `application.yml` tiene un valor por defecto versionado, y si producción no define la variable, usaría ese valor.
- La contraseña de la base del `.run` corresponde a `localhost`. Rotarla solo hace falta si se reutiliza en otra base.

## Dictamen
- **P reactivado y P4 quedan verificados en local. F4 se cumple en preview: 85 ≥ 85**, con todos los criterios por corrida. No hay bloqueos de código.
- **Commits (los autoriza el usuario):** tres, sin cambios ajenos (`AGENTS.md`, revisiones 005, 006, 007 y el plan de rendimiento quedan fuera):
  1. 008 P: prerender productivo, sitio clientes configurado y evidencias resumidas, sin los JSON crudos;
  2. 012;
  3. `.run` sin credenciales.
- **Antes de cualquier despliegue:** S1 resuelto, N1/N2 de 012 y crear el sitio `clientes` antes de conectar dominios de clientes. Marketing con prerender puede desplegarse antes, porque no hay dominios de clientes conectados. Nada de esto está autorizado todavía.
