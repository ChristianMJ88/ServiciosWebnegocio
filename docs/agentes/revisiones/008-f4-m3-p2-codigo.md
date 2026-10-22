**Dictamen: P2 aprobado, puede pasar a P3.** No hay bloqueantes. Hay dos ajustes medios que conviene cerrar antes de dar P por terminado; Codex puede hacerlos en paralelo con P3. Además dejo tres notas para 012. Solo leí archivos: no ejecuté comandos ni cambié nada.

## Lo que se cumple

| Punto | Evidencia |
|---|---|
| **Separación de HTML** | `prepare-clientes-site.mjs:24-26` escribe el shell en `index.html` e `index.csr.html`. `check-clientes-site.mjs:10-12` exige que los dos sean idénticos al `index.csr.html` de marketing y del mismo build (`assertCsrContract`). El prerender de marketing no se toca. |
| **Rechazo del home** | `check-clientes-site.mjs:17-20` recorre todos los `.html` y aplica `assertCsrShell`: `app-root` vacío y sin `ngh`, `ng-state` ni `ng-server-context`. Además rechaza `id="home-title"` y el texto del h1 en cualquier parte del archivo. El noscript genérico (`src/index.html:28-33`) queda fuera de `app-root` y su h1 («Fluora Agenda: agenda de citas…») no coincide con esa expresión. Se cumple lo acordado: se permite ese noscript y nunca el h1 del prerender. |
| **robots** | Solo se quita la línea `Sitemap:` (`prepare…:28`). El resultado es `User-agent: *` / `Allow: /`, y la guarda lo comprueba (`check…:16`). |
| **Marcador** | `fluora-host.txt` solo se genera en clientes. Se exige que no exista en marketing (`check…:15`) y tiene `no-store` (`firebase.json:234-242`). Si aparece un symlink, falla (`prepare…:11`, `check…:18`). |
| **Rewrites** | `firebase.json:244-256` tiene solo `/api/** → apiProxy` y `** → /index.html`. La guarda exige exactamente 2 rewrites, en ese orden y con ese `functionId` (`check…:23-25`). Por la regla de longitud, cualquier `landingPage`, `directoryPage`, `sitemap` o `tenantPage` añadido hace fallar la guarda. |
| **Headers y predeploy** | Los headers son los de marketing más el del marcador. El predeploy está conectado y la guarda verifica ese wiring (`check…:22`). |
| **Sitio no creado** | `.firebaserc:14-16` solo asigna el id propuesto `fluora-clientes`. No hay nada creado ni desplegado. |
| **Rutas de entrada y salida** | El script exige que origen y destino sean carpetas distintas y que ninguna esté dentro de la otra (`prepare…:18`). Hay una prueba negativa (`test:26`). |

## Ajustes antes de cerrar P

**M1 · Medio. El predeploy de `clientes` no aplica las guardas de producción de P1.**
- `firebase.json:188-190` solo ejecuta `check-clientes-site`. Esa guarda no valida que el build sea `mode: production`, que no haya `.map` ni que `runtime-config` coincida con la fuente revisada. Hoy esas comprobaciones están en `check-hydration-probe.mjs:13-14,56`, y las llama `check-public-builds.cjs:17`.
- Si alguien despliega solo `hosting:clientes`, se publicaría lo que haya en `dist/marketing/browser` sin esa validación. Es el mismo criterio que se corrigió en P1.
- **Propuesta:** que el predeploy de clientes sea `node scripts/check-public-builds.cjs && node scripts/check-clientes-site.mjs`, o que `checkClientesSite` llame a `checkHydrationProbe(..., {production:true})` sobre marketing. Además, rechazar `*.map` en el recorrido de `check…:17-20`.
- **Prueba:** un fixture con marketing en modo development, o con un `.map`, debe hacer fallar `checkClientesSite`.

**M2 · Medio-bajo. La guarda no comprueba que el resto de los archivos sean de marketing.**
- `stale-assets` (`test:22`) solo cubre referencias dentro de `index.html`. Un JS alterado, un `runtime-config.js` distinto o un archivo extra en clientes (por ejemplo un `sitemap.xml` estático o una carpeta `e/`) pasaría la guarda.
- Hoy no ocurre porque `prepare` borra el destino (`:22`), pero la guarda debe funcionar sin depender de `prepare`.
- **Propuesta:** el conjunto de archivos de clientes debe ser igual al de marketing más `fluora-host.txt`, con bytes idénticos en todos menos `index.html`, `index.csr.html` y `robots.txt`.
- **Prueba:** dos casos negativos, uno con un archivo extra y otro con `main-A.js` modificado.

**Menores (opcionales):**
- `hostingBuildId` ordena con `localeCompare` (`prepare…:10`). Si el build y el predeploy corren con locales distintos, la huella puede cambiar. Falla del lado seguro, pero conviene comparar por código (`a.name < b.name ? -1 : …`). También conviene separar la ruta del contenido al calcular el hash (por ejemplo, con `\0` o con la longitud).
- **Cobertura del noscript:** el fixture (`test:8`) no lleva noscript. Faltan dos casos:
  - positivo: un shell con el noscript real;
  - negativo: `<noscript><h1 id="home-title">…` fuera de `app-root`.

  Hoy `extra-html` falla por `assertCsrShell` y nunca llega a probar la expresión regular por sí sola.

## Notas para 012 (no bloquean P)

1. **Marcador.** La huella cambia con cada build de marketing. La activación de 012 §4.4 debe aceptar el formato `^fluora-clientes:[0-9a-f]{64}\n?$` y no comparar un hash fijo. Si compara uno fijo, cada despliegue rompe la verificación.
2. **SEO en el shell.** El shell que sirve clientes lleva `canonical` y `og:url` a `https://refluora.com/`, el JSON-LD de Fluora y `robots index,follow` (`src/index.html:5-17`). Si un dominio llega a `ACTIVO` con los pasos 1–5 pero sin §6, los buscadores lo tratarían como duplicado de refluora.com.
   - **Recomendación:** no conectar dominios reales hasta desplegar §6 (`customDomainPage`), o hacer que `prepare` quite canonical, `og:url` y JSON-LD y ponga `noindex` en el shell de clientes.
3. **Despliegue.** Mientras `fluora-clientes` no exista, un `firebase deploy --only hosting` sin especificar target incluye `clientes` y falla. Hay que dejar escrito en la respuesta P que, hasta autorizar la creación del sitio, se usa `--only hosting:marketing,hosting:app`.

## Validación propuesta en P3
- Además de lo que pide §4 del diseño, comprobar con `public-preview` sobre clientes:
  - `/fluora-host.txt` → 200 con el formato correcto;
  - `/sitemap.xml` → shell CSR, no XML de Fluora;
  - `/e/nail-art` → shell CSR, sin `tenantPage`.
- Registrar en la respuesta los casos Node nuevos de M1 y M2.
