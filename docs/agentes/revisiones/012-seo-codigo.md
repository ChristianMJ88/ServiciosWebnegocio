# Revisión 012 §6: SEO y enrutamiento del dominio propio

**Resultado:** encontré dos bloqueos antes de desplegar. Uno impide que el canonical funcione en las páginas internas y el otro puede dejar en blanco los dominios propios. No encontré ningún problema de seguridad que bloquee. Revisé el código en modo de solo lectura y no ejecuté ningún comando.

**Pruebas:** no las corrí. La solicitud guardada dice 58/58 pruebas Node verdes y el mensaje de esta sesión menciona "77?". No pude confirmar ninguna de las dos cifras, así que hay que tomar el conteo del log.

## Lo que está bien
- **Host:** `forwardedHostname` (`functions/custom-domain-page.js:7-12`) rechaza listas, puertos, esquemas, rutas y espacios. No vuelve a `Host` cuando `x-forwarded-host` viene presente pero no es válido.
- **Destinos de fetch:** solo se piden URLs de la API configurada y del origen de marketing. Ninguna sale del header del visitante. Las llamadas usan `redirect:'error'` y timeout.
- **Redirección `/e`:** queda limitada al origen de marketing (`:29-33`) y conserva la query.
- **Respuestas:**
  - Todas las respuestas, también las de error, son `no-store`.
  - `405` para métodos que no son GET/HEAD.
  - `404` sin llamar a la API para landings, `/negocios`, `sitemap.xml` y assets desconocidos.
  - HEAD valida la activación y no envía cuerpo.
- **Backend:** `ServicioConsultaDominios.java:16-26` exige ACTIVO y `incluido(empresaId)`. `dominioPublico` solo devuelve el canonical derivado si está ACTIVO y pertenece a la misma empresa.
- **Inyección en el HTML:** `renderTenantHtml` escapa `<` y U+2028/2029 en `__FLUORA_TENANT__` y en el JSON-LD. No hay cookies ni sesión en el HTML.
- **Guarda de clientes:**
  - `prepare-clientes-site.mjs:32` borra `index.html`.
  - `check-clientes-site.mjs:9` exige ENOENT, que los assets sean idénticos byte a byte, robots sin `Sitemap` y que `customDomainPage` sea el último rewrite.
  - Las pruebas cubren `polluted`, `fallback-order` y `stale-assets`.
- **Canonical en el servidor:** usa `dominioPrincipal` también en las URLs de ItemList (`seo-renderer.js:72-98`). `SeoService.tenantUrl` (`seo.service.ts:130-138`) aplica la misma regla.

## Hallazgos

### P1-A · Alta · Bloqueante: el canonical del cliente reemplaza al del servidor en todas las páginas internas
- **Dónde:** `src/app/core/tenant/tenant-host.resolver.ts:24`, `seo.applyTenant(config, '/')`.
- **Evidencia:**
  - El resolver corre en `''`, `servicios`, `agendar`, `contacto`, `politicas`, `registro`, `checkout` y `productos*` (`app.routes.marketing.ts:16-50`).
  - `setCanonical` cambia el `href` del `<link>` que ya inyectó la Function (`seo.service.ts:146-154`).
  - En el HTML de `/servicios` el canonical es `https://marca.mx/servicios`, pero después de renderizar pasa a `https://marca.mx/`. Google usa el DOM renderizado, así que las páginas internas se consolidan en la home.
  - Solo productos lo corrige después, en `products.component.ts:155`.
  - En las rutas `/e/:slug` esto no pasa, porque `tenant.resolver.ts:18` usa `state.url`.
- **Corrección:** usar la firma `(route, state)` y llamar `seo.applyTenant(config, state.url)`. `tenantUrl` ya maneja rutas locales sin el prefijo `/e/slug`.
- **Prueba esperada:** spec del resolver con host propio. Para `/servicios?utm=x` el canonical debe ser `https://marca.mx/servicios`. Para `/politicas` debe terminar en `/politicas`. Para `/` debe ser `https://marca.mx/`.

### P1-B · Alta · Bloqueante antes de desplegar: la plantilla sale de marketing y los assets de clientes
- **Dónde:**
  - `functions/custom-domain-page.js:54` llama `templateLoader(marketingOrigin)`, que hace fetch de `https://<marketing>/index.csr.html` (`csr-shell.js:50`).
  - El shell referencia `main-*.js` y `styles-*.css` con rutas relativas (`<base href="/">`). Por eso se piden al dominio propio, es decir, al sitio `clientes`.
- **Escenario:** el runbook despliega `firebase deploy --only hosting:marketing,hosting:app` (`docs/fluora-produccion-runbook.md:139`) sin `clientes`.
  - Después de un despliegue de marketing, la plantilla apunta a `main-NUEVO.js`. Ese archivo no existe en `clientes`.
  - La petición cae en `customDomainPage`, que devuelve 404 en HTML (el `Accept */*` de los scripts pasa el filtro).
  - Resultado: una página 200, indexable y con canonical, pero en blanco en todos los dominios propios.
- **La guarda no lo cubre:** `check-clientes-site` compara archivos al momento del build, pero no evita desplegar los dos sitios por separado.
- **Corrección recomendada:**
  1. Cargar la plantilla desde el origen fijo del sitio `clientes`, con un parámetro nuevo (por ejemplo `FLUORA_CLIENTES_ORIGIN=https://<siteId>.web.app`) validado como `https:`. Sigue siendo configuración, no un host que elige el visitante. Así plantilla y assets salen siempre del mismo despliegue.
  2. Actualizar el runbook: desplegar `hosting:clientes` cada vez que se despliega marketing.
- **Prueba esperada:** en el handler, que el loader se llame con el origen de `clientes` y nunca con `marketingOrigin` ni con el host del visitante. Que la Function falle cerrada (503) si el parámetro falta o no es `https`.

### P2-C · Media · Canonical: rutas sin normalizar
- **Dónde:** `custom-domain-page.js:34` usa la bandera `/i` y acepta `/?` al final. `renderTenantHtml` recibe `path` tal cual.
- **Efecto:**
  - `/SERVICIOS` y `/servicios/` devuelven 200 con canonical `https://marca.mx/SERVICIOS` o con barra final.
  - El router de Angular distingue mayúsculas, así que `/SERVICIOS` cae en `**` y redirige a marketing (`app.routes.marketing.ts:51`). Queda un HTML indexable que no corresponde a lo que ve el usuario.
  - Con productos pasa lo mismo: el fetch va en minúsculas (`:48`) pero el canonical conserva las mayúsculas.
- **Corrección:** responder 301 hacia la ruta en minúsculas y sin barra final, conservando la query. Otra opción es quitar `/i` y devolver 404. En ambos casos, renderizar solo con la ruta normalizada.
- **Prueba esperada:** `/SERVICIOS` responde 301 a `/servicios`. `/servicios/` responde 301 o canonical sin barra final. `/productos/ARTICULO` no produce un canonical en mayúsculas.

### P2-D · Media · Robustez y coste: el resolver de host puede entrar en bucle y duplica la consulta
- **Dónde:** `tenant-host.resolver.ts:21-30`.
- **Bucle:** si falla `/publico/sitio/host/...`, el resolver redirige a `['/']`. En un host propio, `''` vuelve a pasar por `tenantHostMatch` y por este mismo resolver, así que la navegación se repite mientras dure el fallo. Esto pasa si se retira el dominio después de servir el HTML, si cambia la capacidad o si hay un error de CORS o de API. Cada vuelta golpea la API.
- **Doble consulta:** el resolver no reutiliza `window.__FLUORA_TENANT__`, que ya inyecta la Function. Cada carga hace dos consultas al host (Function y navegador), y cada navegación interna una más.
- **Corrección:**
  - Si falla, mostrar un estado terminal (página "no disponible" con `noindex`, o `location.assign` a marketing) y no volver a `/`.
  - En la primera navegación, usar el tenant inyectado si existe.
- **Prueba esperada:** spec con la API fallando: una sola llamada, sin redirección a `/`. Con tenant inyectado: cero llamadas HTTP en la primera navegación.
- **Coste:** incluir esto en el documento de coste por navegación que se mencionó.

### P3 · Baja (no bloquean)
1. **Accept vacío:** `custom-domain-page.js:19` devuelve 404 cuando falta `Accept`. Algunos bots de vista previa y monitores de uptime lo omiten. Conviene tratar el `Accept` vacío como HTML. Prueba: sin `Accept` → 200.
2. **`/index.csr.html` público:** en `clientes` se sirve de forma estática, sin pasar por la activación y sin `noindex`. Añadir `X-Robots-Tag: noindex` para esa ruta en el target `clientes`, y que `check-clientes-site.mjs:35` lo exija.
3. **`/checkout` y `/registro`:** el servidor los marca `index, follow`. Para páginas transaccionales sugiero `noindex`. Es opcional y afecta igual a `/e/:slug`.
4. **Alias activo con canonical derivado no ACTIVO:** el backend devuelve `dominioPrincipal: null` y la Function responde 404 (la prueba de `:30` lo confirma). Es un fallo cerrado correcto, pero hay que confirmar con la planificación de V142 que, mientras exista un alias ACTIVO, el canonical derivado siempre está ACTIVO. Si no, un alias pagado mostraría 404.
5. **`apiProxy` en `clientes`:**
   - Reenvía todas las rutas, `Cookie` y `Set-Cookie` (`functions/index.js:37-40`, `proxy-headers.js:16-21`). Restringirlo a endpoints públicos reduciría la superficie de ataque. Es una decisión de producto y no la amplío aquí.
   - Además reenvía el `X-Forwarded-For` que manda el visitante. En la prueba con emuladores hay que confirmar que el backend toma el salto confiable de la derecha y no el primero.

## Antes de cerrar
- Corregir P1-A y P1-B y añadir sus pruebas.
- Volver a correr las suites focales de Functions y de la guarda de clientes, y el spec del resolver. Registrar los conteos con su log.
- La prueba con emuladores debe incluir una petición a un asset con hash después de "desplegar" un build de marketing distinto. Así se verifica P1-B.
