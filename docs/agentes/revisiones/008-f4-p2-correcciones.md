Revisé el medidor de P2: los tres hallazgos medios de `008-f4-m2-codigo` están cerrados y no encontré bloqueantes, así que se puede ejecutar P2. Antes conviene hacer dos ajustes baratos que afectan a cómo se lee el resultado.

No pude guardar la revisión porque esta sesión no tiene herramienta de escritura. El texto de abajo está listo para `docs/agentes/revisiones/008-f4-p2-correcciones.md`, con autoría de Claude.

---

# 008 F4 P2 — revisión de correcciones del medidor

**Alcance:** solo leí `scripts/lighthouse-tenant-cache.mjs`, `lighthouse-home.mjs`, `performance/public-preview.mjs`, `public-preview.test.mjs`, `build-fingerprint.mjs`, `performance/README.md`, `app.routes.marketing.ts` y `core/styles/tenant-public-styles.ts`. No ejecuté builds, suites, `node:test` ni Lighthouse; las cifras (2/2, 266/80, 11/11) son las que declara la solicitud. Comprobé que `tmp/lighthouse/tenant-cache*` no existe, así que P2 todavía no se ha ejecutado.

## Medios cerrados

| Hallazgo de M2 | Estado | Evidencia |
|---|---|---|
| La caché del CSS del tenant se sobrestimaba | Cerrado | `public-preview.mjs:62-73` sirve `no-cache`+`ETag` y responde 304 solo con `tenantCssRevalidate`. Por defecto sigue en `max-age=604800` y sin ETag, lo que cubre la prueba `revalidate=false`. `lighthouse-home.mjs:34` no activa la opción, así que Home no cambia. `lighthouse-tenant-cache.mjs:71` exige un 304 real en el log del servidor y `:91` deja constancia de que es una simulación de D1, no el hosting desplegado. |
| Las guardas no probaban frío/caliente/tenant | Cerrado | En frío (`:64`) no se acepta ningún recurso propio con `cache !== 'none'`; ese campo existe en los informes 13.4.1, donde vi `"none"` en m0/m1. En ambas corridas (`:65-66`) se exige `/tenant-public.css` más una GET pública 200 que no sea de `plataforma/`. Esto basta para descartar que se cuele la home de marketing: el CSS solo lo carga `tenantPublicStylesResolver` (`app.routes.marketing.ts:9,73`), que la ruta de `FluoraHome` (`:55`) no usa. En caliente (`:69-70`) todos los `.js`/`.css` propios vienen de caché, salvo `runtime-config.js` y el CSS del tenant, y la lista no puede estar vacía. |
| Faltaban métricas | Cerrado | `:78-80` guarda FCP, LCP, TBT, Speed Index y `resource-summary`; `:75` rechaza un score `null`; `:92` incluye el aviso de Lantern. |

## Bajos cerrados

- **Procedencia:** la huella vive en `build-fingerprint.mjs` y la usan los dos medidores. El bloque `measurementCode` (`:93-96`) es el mismo que en Home e incluye los hashes del helper y del preview.
- **Informes:** se nombran `${width}x${height}-${cache}.json` (`:46`), y `viewportProfile` (`:90`) aclara que 1440 se mide con throttling móvil.

## Hallazgos nuevos

| Pri. | Ubicación | Hallazgo | Corrección y validación |
|---|---|---|---|
| Bajo | `build-fingerprint.mjs:5-24` | **No puedo confirmar que sea "el algoritmo exacto anterior".** `lighthouse-home.mjs` no está versionado, así que no hay un diff con la versión que generó M0/M1. Si el algoritmo cambió, P2 no se puede atar a la huella de M2. | Sin cambiar código, recalcular la huella de `tmp/builds/m1` (y de M2 si está archivado) y compararla con el `buildFingerprint` de los resúmenes ya guardados. Deben coincidir byte a byte, y el resultado se anota en la respuesta. |
| Bajo | `lighthouse-tenant-cache.mjs:40` | **El backend llega frío a la primera corrida.** Solo se comprueba `plataforma/soporte`; las GET del tenant llegan a 8080 por primera vez en la corrida fría de 390×844. Lantern usa el tiempo de respuesta observado del servidor, así que esa primera corrida puede inflar la diferencia frío/caliente, y solo hay una muestra por tamaño. | Antes del bucle, calentar sin registrar las GET públicas del tenant que use la ruta, igual que hace `lighthouse-home.mjs:55-57`, o hacer una corrida descartada con su propio Chrome. Validación: el `summary` lo indica. |
| Info | `:66` | La guarda del tenant también aceptaría `marketing/landings/navegacion`. No es un problema porque además se exige el CSS del tenant, pero un comentario de una línea lo dejaría claro. | Opcional. |

## No son hallazgos

- `tenantCssRevalidated: cache === 'warm'` (`:81`) es una constante, pero solo se llega ahí si `:71` no lanzó error, así que es correcta.
- La misma ETag para gzip y para identity es aceptable en un preview que ya envía `Vary: Accept-Encoding`.
- El 405 a POST de autenticación queda cubierto por la prueba, y `/api/` fuera de `publico` sigue devolviendo 403 (`:33`).
- Un Chrome por tamaño y su cierre en `finally` (`:42,86,99`) son correctos.

## Antes de ejecutar P2

1. Recalcular la huella de M1/M2 y comparar el SHA (primer bajo).
2. Si se acepta, añadir el calentamiento de las GET del tenant (segundo bajo).
3. Serie sin builds, suites ni revisiones simultáneas; no tocar 4200/8080; puerto 4173 libre.
4. Etiquetar el resultado como una muestra por tamaño y estado de caché, con D1 simulado. No se debe presentar como latencia de producción.

## Sin cambios

- La equivalencia visual exacta sigue sin probarse.
- D1 sigue pendiente de aprobación del usuario y no se toca el hosting.
- M3 queda fuera del alcance.
- Se conserva el trabajo de otros agentes en el árbol: sin commit, push ni despliegue.
