**Dictamen: no veo bloqueantes de código en el camino P, las guardas ni el wiring de build y predeploy.** Hay un riesgo alto en la herramienta de medición que debe corregirse antes de completar P3 tenant y P4. Lo demás son ajustes medios o bajos.

Solo leí archivos: no ejecuté comandos ni verifiqué ninguna cifra. No tengo herramienta de escritura, así que el texto queda listo para que Codex lo guarde en `docs/agentes/revisiones/008-f4-m3-p3-codigo.md`.

## Lo que se cumple

| Punto | Evidencia |
|---|---|
| **Un solo camino de build** | `angular.json:75-90`: `marketing` lleva `server` y `outputMode: static`, y ya no existe la configuración del probe. `build-marketing.mjs:13` rechaza un build que no sea de producción en `dist/marketing`. El wrapper del probe escribe en otra carpeta (`build-marketing-hydration-probe.mjs:5`). |
| **Cadena de guardas** | `package.json:7,10,19`: el build ejecuta marketing (CSR, guarda de hidratación con `--production`), luego app y luego clientes (prepare y check). El predeploy de marketing (`firebase.json:10`) y el de clientes (`:189`) llaman `check-public-builds`, que exige `production:true` (`check-public-builds.cjs:17`). |
| **Artefacto de marketing** | `check-hydration-probe.mjs`: solo la ruta `/` (`:19`), h1, lead y CTA (`:27-33`), estado de hidratación sin datos de negocio (`:40-44`), orígenes (`:45-51`), sin `.map` en producción (`:56`) y solo los HTML `index`/`index.csr` (`:60`). |
| **Clientes tras 012 §6** | Sin `index.html` (`prepare-clientes-site.mjs:32`, `check-clientes-site.mjs:9`). El CSR es idéntico al de marketing (`:11-12`). Los archivos son los de marketing más la marca, con bytes iguales (`:28-29`). Ningún `.map` (`:22`). Hay `X-Robots-Tag` en el CSR (`:32`) y exactamente 2 rewrites, con `apiProxy` primero y `customDomainPage` al final (`:33-35`). |
| **Marca** | El formato del build (`prepare…:37`) coincide con el patrón del backend (`ComprobadorHostingHttps.java:40`), que no compara el hash. La nota 1 de P2 queda resuelta. |
| **Assets que faltan en clientes** | `/missing.js` da 404 sin consultar el backend (`custom-domain-page.js:36-37`, `test.js:41`). `runtime-config.js` usa `/api/v1` relativo, así que en un dominio propio pasa por el `apiProxy` de clientes. |
| **Tabla de rewrites** | `hosting-routes.test.mjs:14-22` cubre la precedencia estática y el despacho de marketing y clientes, incluida `/fluora-host.txt`. |

## Hallazgos

**A1 · Alta (bloquea certificar P3 tenant y P4, no el código P). Los orquestadores sirven el home prerenderizado en las rutas profundas.**
- `lighthouse-tenant-cache.mjs:23` y `lighthouse-home.mjs:21` usan `spa-shell` con valor por defecto `index`. Con ese modo, `public-preview.mjs:79` responde `/e/<slug>/…` con `index.html`. Ese archivo ya es el prerender con `ngh`/`ng-state` del home, así que el cliente hidrataría el DOM del home en una ruta de tenant. Puede dar NG05xx, CLS o errores que no ocurren en producción, donde `tenantPage` usa el CSR.
- Antes de P no pasaba porque `index` y `csr` eran iguales. Además, `README.md:80` todavía dice «El prerender todavía no está habilitado».
- **Propuesta:** en `publicPreview`, si `spaShell === 'index'` e `index.html` no pasa `assertCsrShell`, fallar antes de abrir el listener. Otra opción es que el valor por defecto sea `csr`. Hay que actualizar el README.
- **Prueba:** fixture con un `index.html` prerenderizado y `spaShell: 'index'`, que debe rechazarse. El caso equivalente con `csr` debe pasar.
- **Antes de seguir:** confirmar que las 30 muestras de tenant y la serie P de P4 se corren con `--spa-shell csr` y que el resumen lo registra (`lighthouse-tenant-cache.mjs:111`). Si alguna muestra ya se tomó con `index` sobre el artefacto `da5f…`, no es válida.

**M1 · Media. El criterio de P3 «sin `platform-server` en los JS del navegador» solo se prueba en el código fuente.**
- `marketing-hydration-wiring.test.mjs:20` revisa `main.marketing.ts` y `app.config.marketing.ts`. Ninguna guarda revisa `dist/marketing/browser/*.js`. El aumento de +0.81 kB es un indicio a favor, no una prueba.
- **Propuesta:** en `checkHydrationProbe`, recorrer los `.js` del navegador y rechazar marcadores de servidor que sobrevivan a la minificación. Por ejemplo, textos de error propios de `platform-server`/`@angular/ssr`, o `ng-server-context` usado como escritura. Si no hay un marcador estable, registrar la evidencia con un build `--stats-json` en la respuesta.

**B1 · Baja.** `check-clientes-site.mjs:13` vuelve a leer el mismo archivo que leyó en `:10` y lo compara consigo mismo, así que la aserción no comprueba nada. Conviene quitarla o reemplazarla por algo útil.

**B2 · Baja.** En `firebase.json:226-233` el header `/index.html` de clientes ya no tiene efecto, porque ese archivo no existe. La regla `/` sí aplica a la Function y coincide con su `no-store`. Se puede quitar o dejar comentado en la respuesta.

**B3 · Baja.** En `hosting-routes.test.mjs:21-22`, la rama de clientes no prueba nada nuevo: `calls.at(-1)` ya quedó comprobado en `:19`. Conviene sustituirla por `/missing.js` con `Accept: */*`, que debe despachar a `customDomainPage`.

**B4 · Baja, preexistente.** `public/NailArt_logo.jpeg` es el logo de un tenant real que se publica en marketing y en todos los dominios de clientes. No es nuevo de P, pero ahora llega a dominios ajenos. Lo dejo para que el usuario decida; puede quedar fuera de P.

**B5 · Baja.** El `/index.csr.html` de marketing no lleva `X-Robots-Tag` (clientes sí lo tiene). Su canonical apunta a `/`, así que el riesgo es menor.

## Sobre las cifras reportadas
- El build, el backend 697, Angular 387/92 ×3 y Node 157/157 son coherentes con el wiring, pero no los verifiqué. Las correcciones del spec de orígenes y de los helpers del emulador deben quedar en la respuesta con el antes y el después de cada aserción, para acreditar que no se relajaron.
- El máximo de la matriz de 32 (0.0066625…) coincide con el de R3. Para cerrar P3 todavía faltan la hidratación, el replay y la navegación sobre `da5f…` y las 30 muestras de tenant, estas últimas después de A1.
- La parte de 012 §6 sigue bajo `012-seo-cierre`, que no tiene bloqueantes de código.

## Siguiente paso
1. Corregir A1, con su prueba, antes de tomar las 30 muestras de tenant.
2. Corregir M1 y los bajos B1–B3, opcionalmente en paralelo.
3. Cerrar P3 con su evidencia y después empezar P4, con `--spa-shell csr` en la serie P.

Sin sitios, DNS, Firebase, producción, correos, commit ni despliegue.
