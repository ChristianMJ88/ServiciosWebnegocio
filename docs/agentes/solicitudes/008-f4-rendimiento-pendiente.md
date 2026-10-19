# 008 F4 — diagnóstico del criterio de rendimiento pendiente

Autor: Codex. Arquitectura y revisión de lectura; no modificar aplicación, revisiones anteriores ni ejecutar cobros, envíos, credenciales o despliegues.

El build final marketing `2f46854344cca5b3` (927.61 kB inicial / 186.22 kB transferencia estimada) y application `c09b39f307bb75ed` terminaron con exit 0. Angular final: 252 aprobadas en 78 archivos, un worker temporal, sin ampliar timeouts. El pie de página durante el resolver ya fue aprobado en `008-f4-footer-cierre`.

Lighthouse 13.4.1 mediante UI nativa de Chrome incógnito, navegación móvil Moto G Power / Slow 4G, preview local 4300 con gzip y API GET pública local. Tres corridas del mismo build, sin suites, builds ni matriz UI simultáneos:

| Corrida | Performance | Accesibilidad | Buenas prácticas | SEO | FCP | LCP | TBT | CLS | SI |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 55 | 100 | 100 | 100 | 3.4 s | 4.5 s | 800 ms | .002 | 5.2 s |
| 2 | 62 | 100 | 100 | 100 | 3.4 s | 4.5 s | 510 ms | .001 | 4.6 s |
| 3 | 66 | 100 | 100 | 100 | 3.9 s | 4.2 s | 420 ms | 0 | 3.9 s |

Mediana Performance 62, rango 55–66: NO cumple 85. No se atribuye el incumplimiento exclusivamente al host ni se descartan puntuaciones bajas. Las corridas anteriores con extensión Adobe o trabajo concurrente están separadas de este conjunto final. Chrome identifica CSS de Google Fonts/Animate heredadas que bloquean render, y ejecución JS/TBT como trabajo pendiente. No se eliminaron porque siguen usadas por tenants/Booking.

Propón el siguiente cambio mínimo de frontend que permita avanzar hacia 85 sin alterar visualmente tenants, sin nueva decisión de host duplicada, sin precargar Home para todos, sin cambiar contratos/precios/permisos y sin introducir SSR/backend. Evalúa si conviene alojar localmente los mismos recursos heredados con sus licencias, o diferir dependencias concretas verificables. No recomendar ocultar contenido/CTA ni cambiar la configuración de Lighthouse para inflar la nota. Indica alcance, archivos y regresiones necesarias. Si requiere ampliar arquitectura, sepáralo expresamente.

F4 queda abierta por rendimiento; no se solicita una aprobación ficticia del criterio. La respuesta con evidencia está en `respuestas/008-f4.md` y se actualizará al terminar las comprobaciones.
