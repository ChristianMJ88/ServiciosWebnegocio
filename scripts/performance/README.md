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
node scripts/lighthouse-home.mjs --label m0 --output tmp/lighthouse/m0
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

Fuentes: [Lighthouse CLI](https://github.com/GoogleChrome/lighthouse),
[simulación de throttling](https://github.com/GoogleChrome/lighthouse/blob/main/docs/throttling.md).
