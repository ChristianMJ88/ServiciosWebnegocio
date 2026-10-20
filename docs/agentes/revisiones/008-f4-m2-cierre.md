# Revisión 008 F4 M2: cierre de evidencia

**Dictamen:** estoy de acuerdo con la respuesta. M1 y M2 están verificados como implementación, y F4 sigue abierta por tres motivos: Performance 85, el CLS del tenant y la equivalencia visual. No hay bloqueantes en el código nuevo. La respuesta no exagera las cifras: las comparé con los JSON y coinciden. Solo faltan cuatro precisiones de lectura (puntos 2 a 5).

Esta sesión no tiene herramienta de escritura. El texto está listo para que Codex lo guarde en `docs/agentes/revisiones/008-f4-m2-cierre.md` con autoría de Claude.

**Qué revisé:** la respuesta 008-f4-rendimiento-plan, la revisión p2-correcciones, `public-preview.mjs` y su test, `lighthouse-tenant-cache.mjs`, los resúmenes de `m2-paired`, `m2-services-cache` y `m2-tenant-cache-scoped`, el `failed-run.json` del diagnóstico, el informe `1440x900-cold.json` y `m02-final-summary.json`. No ejecuté builds, suites ni Lighthouse. Las cifras 266/80, 11/11 y 3/3 son las que declara la solicitud.

## 1. Cifras comprobadas
- **Home:** M1 da 79/84/84/80/87 (mediana 84) y M2 da 82/84/84/80/80 (mediana 82). A, BP y SEO dan 100 y el CLS es ≤ .000707. Ni el Header ni el CSS del tenant se piden en la Home.
- **Huellas:** la de M2 (`41ddf9…`) es la misma en Home y en P2.
- **P2 Servicios:** el CLS y el Performance de las seis muestras coinciden con la tabla, y A es 96.
- **UI:** 11 de 15 pares sin diferencias. Los cuatro restantes son Booking en los tres tamaños y Servicios en escritorio.

## 2. Precisiones de lectura (no bloquean, conviene anotarlas)

| Pri. | Evidencia | Precisión |
|---|---|---|
| Medio | 820 caliente: TBT 1810 ms y SI 3678 frente a 98 / 25.5 ms en los otros tamaños calientes | El 63 en caliente frente al 66 en frío es una muestra única y anómala. No sirve para decir que la caché no ayuda en 820. |
| Medio | El CLS no depende de la caché. En 1440 es .220519 en la ejecución de calentamiento, en frío y en caliente. En 390 y 820, el valor en caliente coincide con el del calentamiento. | El CLS es un problema de layout, no de red. D1 no lo corrige, así que conviene tratarlos como asuntos separados. |
| Medio | En frío, 11 de las 13 imágenes son de terceros y pesan unos 3.45 MB. El LCP en frío va de 6.2 a 7.4 s. | Es el coste principal del catálogo y la respuesta no lo menciona. M3 (prerender de la Home) no afecta a esta ruta, y el usuario debería saberlo antes de decidir. No implica causalidad. |
| Bajo | "30 muestras" en la línea 48 | Son 15 pares, con 11–26 nodos muestreados por página. Booking móvil no tiene diferencia de scroll ([0,0]): tiene `content:1`, `style:41` y block-size +266 px. Atribuirlo a la cotización es plausible pero no está demostrado. |
| Info | El preview cambió de hash entre la Home M1/M2 (`a5c8c…`) y P2 (`9d706f…`). | El cambio solo afecta al registro de peticiones, no a los bytes servidos. Conviene una línea que lo diga, para que "mismo orquestador" no se lea como el mismo hash. |

## 3. Código nuevo: `public-preview.mjs:38-52`
La atribución al inicio es correcta y el test `public-preview.test.mjs:41-72` prueba el solapamiento real. Tiene dos huecos menores:
- **Bajo:** una observación que sigue en `status:0` cuando se comprueba la auditoría y falla después nunca se marca como error, porque `lighthouse-tenant-cache.mjs:63` filtra `>=400` en ese momento. **Corrección:** registrar en el resumen cuántas peticiones quedaban pendientes al cerrar cada auditoría, o rechazar la corrida si queda alguna.
- **Bajo:** no hay test del camino de error (502 / `clientClosed`). Es opcional.

## 4. P2: siguiente revisión de diseño, no un placeholder
`layout-shifts` en 1440 frío registra un único desplazamiento, del 0.2205. El elemento es `main.min-vh-100`, con top 78 al final.

En 390 y 820, el CLS equivale a unos 68–72 px de desplazamiento (CLS × lado mayor del viewport, suponiendo que `main` llena la pantalla). Eso es compatible con que el Header diferido de M1 se inserte encima de `main` después de pintarse.

Es una **hipótesis, no una conclusión**. Los 0.22 de 1440 no se explican solo con 78 px, y M1 es un candidato más probable que M2.

**Recomendación:** antes de diseñar, medir P2 sobre `tmp/builds/m0` (`15db…`) para tener la línea base que falta. Ahora el medidor exige `/tenant-public.css` y el 304 en `:72` y `:77`, y M0 no tiene ese archivo. Hace falta un modo de línea base que desactive esas dos guardas y lo indique en el resumen. Con eso y con el trace de los desplazamientos, se propone una corrección en una revisión aparte. No apruebo alturas adivinadas.

## 5. Sin cambios
- No se afirma la meta 85 ni una mejora causal.
- M3 no está implementado y lo decide el usuario: diseñar y autorizar el prerender, o aceptar el valor actual. No más microajustes de la Home.
- D1 sigue pendiente y no se toca el hosting.
- No hay equivalencia exacta total ni prueba en iOS físico.
- Sin commit, push ni despliegue. Se conservan los procesos 4200/8080 y el trabajo de otros agentes.
