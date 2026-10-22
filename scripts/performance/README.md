# Medición fija de Home (008 M0)

Compilar antes de medir; no ejecutar builds ni suites durante la serie. Mac con
corriente y las mismas condiciones de carga en cada paso. El script inicia un
preview comprimido de producción solo para cinco corridas y lo retira en finally.
Precomprime todos los recursos antes de escuchar, calienta las GET reales y
registra una corrida fija de calentamiento separada de las cinco de la mediana.
No usa4200 para medir un build de desarrollo, ni inicia/detiene el backend8080.
Proxy limitado a GET/HEAD de la API pública local; sin auth, cookies o escrituras.

Instalar Lighthouse13.4.1 fuera del proyecto y usar Node≥22.19 **solo para esa
herramienta**. No cambia la versión Node20 del proyecto ni sus dependencias.

```sh
npm install --prefix /ruta/temporal/lighthouse --ignore-scripts --no-audit --no-fund --save-exact lighthouse@13.4.1
LIGHTHOUSE_CLI=/ruta/temporal/lighthouse/node_modules/lighthouse/cli/index.js \
LIGHTHOUSE_NODE=/ruta/a/node22 \
node scripts/lighthouse-home.mjs --spa-shell csr --label m0 --output tmp/lighthouse/m0
```

`--root`, `--api`, `--port`, `--path`, `--width` y `--height` son configurables.
M0/M1/M2 de Home usan375×812, mobile y throttling simulate de la versión fijada,
cinco navegaciones con perfil Chrome nuevo por corrida/caché fría. Para la matriz
de tenant se varía únicamente ruta/dimensiones y se conservan CPU/red.
La matriz con caché caliente requiere otro ejercicio; estas corridas no la cubren.
Nunca se alteran preferencias del Chrome personal.

P2 usa el mismo perfil de red/CPU y Lighthouse CLI, con un Chrome temporal por
tamaño. La ruta del negocio es un argumento, nunca una regla incrustada:

```sh
LIGHTHOUSE_CLI=/ruta/temporal/lighthouse/node_modules/lighthouse/cli/index.js \
LIGHTHOUSE_NODE=/ruta/a/node22 \
node scripts/lighthouse-tenant-cache.mjs --path /e/slug-del-negocio
```

Descarta una navegación para calentar las GET reales antes de cada pareja.
Después mide frío (resetea caché/almacenamiento) y caliente (misma instancia,
`--disable-storage-reset`). Verifica tenant CSS/API200, frío sin caché y todos
los JS/CSS calientes desde caché salvo runtime no-store y CSS tenant revalidado
con ETag/304. Esa última política **simula D1 localmente**, no cambia ni certifica
el hosting. Los demás assets del preview usan max-age604800. Las latencias de
disco calientes son estimaciones de Lantern, no mediciones físicas.

Una muestra por tamaño/estado, 390×844,820×1180,1440×900; **1440 conserva
throttling móvil**, no es una auditoría de rendimiento desktop. Guarda todas
las métricas, recursos y calentamientos separados, procedencia y huella común
con Home. Una petición pendiente pertenece a la corrida que la inició, para
que una respuesta tardía no invalide otra pareja. Los errores reales abortan
la serie y dejan diagnóstico separado. No usar series incompletas para cerrar
P2; no ejecutar builds, suites ni otra revisión durante ninguna medición.

Para investigar CLS, `--traces` guarda las trazas oficiales con `--save-assets`.
`--baseline` solo admite un artefacto sin `tenant-public.css`; omite las guardas
específicas de esa hoja/304, conservando Header, API200 y comprobaciones de caché.
El resumen lo etiqueta expresamente, sin atribuir fuentes actuales al artefacto.
Para Agendar con servicios precargados puede usarse `--allow-quote`: la única
excepción a GET/HEAD es el POST de **lectura** `/api/v1/publico/citas/cotizar` que
ya admite el preview. No permite reservas, órdenes, pagos ni autenticación.
La opción queda registrada como `allowReadOnlyQuote` y está apagada por defecto.

Para comparar artefactos archivados en la misma sesión, añadir
`--root tmp/builds/m0 --label m0 --compare-root dist/marketing/browser --compare-label m1`.
Se precomprimen ambas variantes antes de medir, se calienta cada una una vez y
se alterna el orden de las cinco parejas AB/BA/AB/BA/AB (A primero3veces/B2veces).
Cada resumen conserva su propia huella; el commit/dirty y hashes de scripts
identifican el orquestador, no atribuyen fuentes actuales al artefacto archivado.

Se registran versión, huella SHA256 recursiva de todos los recursos, commit y
estado sucio del código, navegador, settings, cinco cifras, mediana, bytes por
tipo y si se solicitó Header/tenant-public.css. Exige un único chunk de Header.
Reportes bajo `tmp/`
(ignorado), sin subirlos a un proveedor. Detiene la serie ante API caída/error de
auditoría. No comparar con las series previas de DevTools.

## Shell de navegación (M3 R1)

`npm run build:marketing` prepara `index.csr.html` mediante su hook postbuild.
El usuario reactivó P: index.html prerenderiza la raíz e index.csr.html es
un shell vacío del mismo build. Application conserva index.html y no usa ese
hook. El wrapper diagnóstico usa exactamente la configuración de marketing.
No hay despliegue autorizado.

Los dos orquestadores admiten `--spa-shell index|csr` y registran el modo en su
resumen. `index` mantiene compatibles las referencias M0/M2 archivadas. Para
M3 usar `--spa-shell csr`: `/` y `/index.html` sirven el inicio; los enlaces
profundos HTML usan el CSR validado antes de abrir el listener. No hay fallback
silencioso si falta el CSR o contiene datos de hidratación. Los archivos reales
y las restricciones del proxy se conservan. Este preview **no ejecuta** las
Functions de tenant, landing ni directorio; sus renderers se verifican aparte.

`npm run check:public-builds` comprueba el CSR, su contrato de assets y la salida
vacía de application. Es también la guarda de predeploy del target marketing:
solo valida, nunca construye ni repara. Un `ng build` directo omite el hook; antes
de cualquier despliegue debe usarse el comando npm completo y pasar la guarda.
La guarda exige ambos builds incluso si se prepara solo el target marketing.

R1 necesita publicar Hosting marketing **antes** de Functions, porque la nueva
carga remota exige `/index.csr.html`. Volver Hosting a una versión anterior sin
revertir también Functions dejaría rutas públicas en503. No usar un despliegue
combinado suponiendo orden. El sitio separado para futuros dominios de tenants
será obligatorio antes de desplegar el prerender o conectar esos dominios; no
se crea en R1, cuyo inicio todavía es CSR. Nada de esto autoriza un despliegue.
El loader no sigue redirecciones: antes de publicar Functions debe comprobarse
que FLUORA_MARKETING_ORIGIN sirve directamente el CSR publicado, con200, y no
redirige a otro host. La comprobación de index.html del19/oct fue200 sinLocation;
no sustituye esa validación posterior del nuevo CSR en Hosting.

Fuentes: [Lighthouse CLI](https://github.com/GoogleChrome/lighthouse),
[simulación de throttling](https://github.com/GoogleChrome/lighthouse/blob/main/docs/throttling.md).

## Probe local de hidratación (M3 R3)

El build de producción sigue siendo CSR. Este probe usa una configuración aparte,
no participa en `npm run build`, Firebase ni predeploy. Compilar con Node20 y
ejecutar las herramientas CDP con Node22 (WebSocket nativo):

```sh
npm run build:marketing:hydration-probe
npm run build:marketing:hydration-probe -- --mode development
/ruta/a/node22 scripts/check-hydration-probe.mjs dist/marketing-hydration-probe/production
/ruta/a/node22 scripts/performance/hydration-probe.mjs
/ruta/a/node22 scripts/performance/hydration-probe.mjs --root dist/marketing-hydration-probe/development
```

Los orígenes se leen de `public/runtime-config.js`, con validación HTTPS y hosts
distintos, y se pasan como argumentos `define` al builder21.0.6. No se lee `.env`
ni se duplican dominios en angular.json. El servidor sin contexto falla cerrado.
Las únicas rutas de servidor son raíz prerenderizada y comodín CSR. La guarda
exige una sola ruta emitida, estado exclusivamente de hidratación, sin datos de
API, y un CSR vacío compatible. Solo usar artefactos de un build con exit0.

Cada muestra abre Chrome headless en un perfil temporal independiente, sin
conectar al Chrome personal ni cambiar sus preferencias. El preview escucha en
un puerto efímero, nunca4200/8080/4300, y solo reenvía GET/HEAD públicos al backend
local indicado por `--api`. No inicia ni detiene el backend. Se cierra el grupo
propio de procesos y el servidor en limpieza independiente, conservando el error
original si también falla la limpieza.

Otros ejercicios, ejecutados secuencialmente y sin builds/suites simultáneos:

```sh
/ruta/a/node22 scripts/performance/hydration-probe.mjs --sizes 375x812 --replay --scenario apiSlow
/ruta/a/node22 scripts/performance/navigation-hydration-probe.mjs
/ruta/a/node22 scripts/performance/font-hydration-probe.mjs
/ruta/a/node22 scripts/performance/loading-plans-measure.mjs
/ruta/a/node22 scripts/performance/home-sections-measure.mjs
/ruta/a/node22 scripts/performance/csr-hydration-probe.mjs --tenant slug-publico-real
```

`apiSlow` retiene las GET1500ms antes de enviarlas; `netSlow` emula150ms,
200000B/s de descarga,93750B/s de subida yCPU4. Son diagnósticos locales, no SLA.
Las marcas del SDK se leen de `mark_feature_usage.detail.feature`; que exista la
marca no acredita por sí solo que terminó la hidratación. Se espera a la limpieza
del event replay, red y chunks en calma, hashes de respuestas y dos frames.

La matriz registra CLS por ventanas desde la navegación hasta estabilidad
(`initial`) y por sección tras scroll (`deltaCls`), además del acumulado (`final`).
Este último depende del ritmo de scroll del script: no es Lighthouse ni CrUX.
Las reservas CSS son mínimos medidos, no alturas fijas ni garantías para textos
configurables futuros. Los fallos503 son inyectados por el probe; todos los demás
datos provienen de las GET reales, con huellas para detectar cambios entre fases.

El probe CSR llama los renderers existentes sobre index.csr.html y verifica que
sus hijos iniciales se retiran sin hidratar. `/acceso` es una redirección al
origen de la app: se comprueba el documento anterior y se pausa el destino sin
seguirlo. Para ello el runtime interceptado usa dos orígenes locales distintos,
declarados en cada resultado. Exige una sola navegación y una ventana de calma.

Si los datos reales generan NG0913, la consola estricta hace fallar esa corrida:
no se suprime el aviso. La variante `--logo null` es un fixture **derivado**:
clona SiteConfig con logoUrl=null y las respuestas del directorio con
negocios[*].imagenUrl=null, dejando el resto de campos y GET intactos. Registra
esas sustituciones; no modifica la API ni el negocio. Usar outputs distintos y
reportar el fallo real junto al resultado derivado, sin presentarlos como una
misma prueba exitosa.

Los resultados/capturas se guardan por defecto en `/private/tmp`, con permisos
privados. Las tablas saneadas de evidencia en docs/agentes no contienen cuerpos
de API, URLs de imágenes, contactos ni credenciales. El cierre de R3 no autoriza
prerender productivo, Hosting, Functions, dominios o despliegue.

### Parser pauses and separate font arrival (R3 residuals)

`layout-residual-probe.mjs --phase before|after --kind all|nav|icons`
keeps every sample and returns nonzero for final violations. It uses the same
isolated Chrome lifecycle and local read-only APIs. `--phase before` records the
reproduction without treating known pre-fix layout shifts as success criteria.
The nav fixture splits only the root HTML into two uncompressed chunks; every
other route and the default preview remain unchanged. Its hero cut is explicitly
diagnostic. Font samples release Jakarta, then Manrope/icons in both orders;
geometry is captured between releases. No API payload, font file or business
configuration is modified. Output defaults to a private temporary directory.

The normal section matrix records every initial CLS violation before failing the
run. A successful process exit from an older harness without this global check
must not be reported as satisfying the initial CLS limit.
The revised residual classifier preserves parser, font and hydration events by
ancestor region, including text nodes. Only parser/hydration shifts fail the nav
check; font shifts remain visible and count toward total initial CLS. Icon-only
transitions compare workflow position relative to pillars; Manrope transitions
are reported separately as `textResidual`. Empty geometry is an error.
`--phase before --kind nav --widths 1280 --cuts details --baseline-presentation`
is an explicitly derived negative control: it overrides only the new visibility
rule in an isolated page, to verify the observer on an incomplete header. It is
not evidence for an earlier build SHA and never edits source or business data.

`nav-cta-probe.mjs` records a public-build fingerprint with its 12 explicitly
derived trial/error fixtures. `beforeFontState` distinguishes fallback from loaded
fonts; `before`, `after`, and `afterFonts` carry Range and neighbour geometry.
The former duplicate `fallback` field was removed. CTA content reclassification
requires the original SSR icon node, stable boxes, a changed label and the measured
centering delta; it never subtracts the residual from total CLS.

## P: previsualización y evidencia de módulos del navegador

El artefacto productivo de marketing vuelve a P por decisión del usuario.
check-hydration-probe exige raíz prerenderizada, shell vacío coherente,
manifiesto production/prerender, runtime revisado y ausencia de datos de negocio,
HTML adicional y sourcemaps. La configuración marketing es única, tanto para
producción como para el wrapper diagnóstico. Se rechazan salidas anidadas o que
contengan la carpeta productiva; desarrollo no puede escribir en ella. El
modo `index` se rechaza antes de escuchar si el archivo lleva contenido/hidratación.
No usar cifras de tenant obtenidas con el home prerenderizado como fallback.
`node scripts/build-marketing.mjs --stats` permite comprobar los inputs de los
outputs JS del navegador en `stats.json`; es diagnóstico y comparte el builder.
La evidencia resume los inputs, las huellas y los resultados; no se versiona el
metafile completo. Las Functions de SEO siguen reutilizando CSR. El sitio de
clientes se configura localmente sin crearlo ni desplegarlo.

Para verificar las rutas CSR del artefacto P:
`node scripts/performance/csr-hydration-probe.mjs --root dist/marketing --tenant <slug-existente>`. El modo por defecto valida primero el prerender. La opción `--rendering csr` y check-marketing-csr quedan para artefactos CSR archivados, no como guardas productivas.

P4 repetida: conservar la serie previa y escribir otro resumen; alternar AB/BA/AB/BA/AB, cinco corridas por build y dos warmups separados. Condiciones y regla en revisiones/008-f4-m3-p-reactivar.md: Header ausente en P; cualquier incumplimiento requiere decisión del usuario, sin microajustes. Una serie abortada se conserva y se repite entera, sin mezclar corridas.
