# Veredicto de la revisión 008 F4 · M3 R1 (código)

**Apruebo R1 para que lo revise el usuario.** No encontré fallos funcionales que bloqueen. H1–H7 están implementados como pedía el diseño corregido. Hay un riesgo de despliegue que se tiene que comprobar antes de publicar (A1).

No pude guardar `docs/agentes/revisiones/008-f4-m3-r1-codigo.md` porque esta sesión solo tiene Read, Glob y Grep. Lo que sigue es el contenido de ese archivo.

Solo leí código: no ejecuté builds ni suites. Los resultados de pruebas que cito (31/31 en Node 20) vienen de Codex.

## Revisé y está conforme
- **H1:** `functions/csr-shell.js:49-55` pide solo `/index.csr.html`. No hay segunda petición y el error termina en 503 con `no-store`. Las tres Functions usan este loader (`functions/index.js:79,172,197`). `csr-functions.test.js` ejecuta los handlers reales y comprueba 503, `no-store`, una sola petición CSR y ninguna a `/index.html`.
- **H2/H3:** `firebase.json:9` tiene la guarda predeploy, `:120` el CSR en marketing y `:180` deja `app` en `/index.html`. `check-public-builds.cjs:12` valida que el `index.html` de application esté vacío.
- **H4:** el validador rechaza sin limpiar nada (`csr-shell.js:5-13`). La igualdad byte a byte solo se exige mientras `index.html` es un shell (`:44-46`).
- **H5:** `prepare-csr-shell.mjs` cubre copia, existente, contaminado, stale, falta de index e idempotencia. Solo marketing tiene `postbuild`.
- **H6:** `/` se mapea explícitamente (`public-preview.mjs:64`). El modo se valida antes de escuchar ningún puerto. Hay pruebas de traversal, asset 404 y modo inválido. `--spa-shell` se registra en los dos resúmenes.
- **Orden de despliegue:** publicar primero el Hosting de R1 es compatible con las Functions anteriores, porque `/index.html` sigue siendo un shell vacío. El orden Hosting → Functions es seguro y está documentado (`README.md:95-100`).

## Hallazgos

**A1 · Media-alta (verificar antes de desplegar): `redirect: 'error'` cambia el comportamiento en producción**
- **Evidencia:** `csr-shell.js:51`. El loader anterior seguía las redirecciones de `fetch` (comportamiento por defecto). El diseño no pedía esto: solo pedía «sin fallback a `/index.html`».
- **Riesgo:** si el host de `FLUORA_MARKETING_ORIGIN` redirige (apex → www, o al revés) o pasa a hacerlo, `/e/**`, las verticales y `/negocios` responderían 503 en producción. Los tests no lo detectarían porque usan un `fetch` simulado.
- **Aviso:** al buscar con grep entró por error una línea de `functions/.env.fir-serviciosweb-b2901` con el origen de marketing. Es una URL, no un secreto. No leí nada más de ese archivo.
- **Validación:** confirmar con la foto de dominios (`customDomains`, campo de redirección) que ese host sirve directamente, sin redirect. La otra opción es pasar a `redirect: 'manual'` y aceptar solo 200. Ojo: así tampoco se seguiría una redirección, así que solo es más explícito, no cambia el riesgo. La decisión de mantener `'error'` es razonable si se deja anotada como precondición del despliegue.

**A2 · Baja: la guarda de predeploy exige los dos builds**
- `check-public-builds.cjs:8` recorre marketing y application. Desplegar solo `hosting:marketing` falla si no existe `dist/application`.
- Es aceptable y coherente con «exige ambos builds», pero hay que anotarlo en ESTADO para que no parezca un fallo.
- **Supuesto sin verificar:** el CLI ejecuta el hook con el directorio del proyecto como cwd. El script resuelve sus rutas con `__dirname`, así que solo afecta a la ruta relativa del comando.

**A3 · Baja: la comprobación de `firebase.json` no revisa la posición de la regla `**`**
- `check-public-builds.cjs:43,45` usa `find` sobre `source === '**'`. Si alguien pusiera un `**` antes que `/e/**`, ocultaría las Functions sin que el check fallara.
- **Validación propuesta:** comprobar que `**` es la última regla de `rewrites` en los dos sitios.

**A4 · Baja: falta el camino de éxito de las Functions**
- `csr-functions.test.js` solo cubre el 503. Faltan:
  - un caso 200 por handler con el CSR válido, que confirme que `template` llega al renderer;
  - un caso de CSR contaminado (hoy solo se prueba el 404).
- No bloquea, porque `check-public-builds.cjs:29-38` ya prueba los tres renderers sobre el CSR real.

**A5 · Informativa (anterior a R1, sin cambios)**
- En `tenantPage` (`index.js:74`), `Promise.all` hace que un CSR caído convierta también un slug inexistente (que sería 404) en 503.
- Es aceptable porque responde con `no-store` y `Retry-After`. Lo dejo como límite, no como corrección.

## Límites
- No ejecuté `npm run build` ni las suites. Faltan la evidencia de Node 22, de Angular en configuración normal y de los builds finales. Antes de cerrar hay que confirmar que `dist/marketing/browser/index.csr.html` es idéntico a `index.html`.
- El preview no ejecuta Functions (está documentado).
- La foto de dominios es puntual.
- `prepare-template.mjs` sigue en el repo marcado como obsoleto y sin consumidores (lo confirmé con grep). Eliminarlo lo decide el usuario.
- Antes de desplegar el prerender o conectar dominios de clientes sigue haciendo falta el sitio `tenant-domains`.

**Antes de R4:** resolver A1 (cerrar la verificación o anotar la precondición) y registrar A2 en ESTADO. A3 y A4 son opcionales y caben dentro de R1. Sin commit ni despliegue, como pediste.
