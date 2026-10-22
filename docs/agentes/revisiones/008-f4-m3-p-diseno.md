# 008 F4 M3 · P: prerender productivo del home y sitio de Hosting para dominios de clientes

Autor: Claude Code, 2026-10-20. El usuario aprobó continuar con el prerender productivo y con los dominios propios («si adelante con todo»). Este documento cubre **el prerender y el sitio separado**. Los dominios propios como producto están en `revisiones/012-diseno.md`. Base: commit `4e93dd1` (R3 local).

**Autorizado:** implementación y verificación local.
**No autorizado** (requiere una autorización explícita del usuario en su momento): `firebase hosting:sites:create`, `firebase deploy`, conectar dominios, push.

## 1. Estado verificado (solo lectura)
- `angular.json:89-94`: la configuración `marketing-hydration-probe` (server, `outputMode: 'static'`) solo se usa desde `scripts/build-marketing-hydration-probe.mjs`. `npm run build:marketing` sigue en CSR.
- `src/app/app.routes.marketing.server.ts`: `''` → Prerender y `**` → Client. Es el alcance correcto (R5).
- `scripts/lib/marketing-origins.cjs`: lee `public/runtime-config.js` y exige HTTPS, que los orígenes sean distintos y que no sea `build.invalid`. Con la configuración actual da `https://refluora.com` y `https://app.refluora.com`.
- `firebase.json`: marketing `** → /index.csr.html` (R1). Las Functions usan `loadCsrTemplate`. `.firebaserc` tiene solo los targets `marketing` y `app`.
- `isMarketingHostname` (`platform-host.service.ts:57-64`) acepta el dominio y `www.`. Con el prerender, `www.refluora.com/` también hidrata.
- Hosting sirve el archivo estático `index.html` para `/` **antes** que cualquier rewrite. Por eso un sitio que no deba recibir el home necesita **otra carpeta pública** cuyo `index.html` sea el shell CSR. No basta con un rewrite.

## 2. P1 · El build de marketing de producción pasa a prerender
1. **Un solo camino de build.** `build:marketing` usa el builder con `server` y `outputMode: 'static'`, con las mismas `--define` de orígenes del script del probe.
   - La configuración `marketing-hydration-probe` se **renombra** (por ejemplo, `marketing-prerender`) o se integra en `marketing`. No deben quedar dos caminos que puedan divergir.
   - El script del probe se puede conservar como envoltorio de desarrollo, siempre que use la misma configuración.
2. **Guardas del artefacto productivo.** Se reutiliza `check-hydration-probe.mjs` contra `dist/marketing/browser`. Deja de ser exclusiva del probe.
   - `index.html` lleva el h1 «La agenda en línea para salones, barberías y spas.», el lead, los CTA y los chips. Tiene estado de hidratación y event replay solo para la raíz.
   - No lleva HttpTransferCache, contenido de negocio, precios, planes, datos de soporte, `localhost` ni orígenes ajenos.
   - Solo existe la ruta `/`: ningún HTML extra.
   - `index.csr.html` es un shell vacío y sin estado.
3. **`check-public-builds.cjs`.** Hoy exige `assertCsrContract(shell, index)` y, en la práctica, que los dos archivos sean iguales.
   - Pasa a exigir: `index.csr.html` vacío y del **mismo build** (main, runtime y estilos iguales), e `index.html` prerenderizado, validado con la guarda del punto 2.
   - Los renderers de SEO siguen probándose con `index.csr.html`.
4. **`prepare-csr-shell.mjs`.** Con el prerender, el builder ya genera `index.csr.html`. La rama que lo copia desde `index.html` solo debe aceptarse si `index.html` está vacío. Si no, debe fallar, para no copiar el home como shell.
5. **Sin cambios** en `application`, en el backend, en las Functions ni en las rutas que no son `/`.

## 3. P2 · Sitio de Hosting `clientes` (R2), solo configurado
- **`firebase.json`:** un tercer sitio con `"target": "clientes"` y `"public": "dist/clientes/browser"`.
  - Rewrites: **solo** `/api/** → apiProxy` y `** → /index.html`.
  - No incluye `landingPage`, `directoryPage`, `sitemap` ni `/e/**`. En un dominio de cliente no deben aparecer las landings de Fluora, el directorio, el sitemap de Fluora ni otros negocios.
  - Headers iguales a los de marketing: `/`, `**/*.html` y `runtime-config.js` con `no-store`.
  - `predeploy` con su propia guarda.
- **`.firebaserc`:** el target `clientes` apunta a un id de sitio propuesto, por ejemplo `fluora-clientes`. El id definitivo lo confirma el usuario al crear el sitio. **Codex no crea el sitio.**
- **Build `dist/clientes/browser`:** un script, por ejemplo `scripts/prepare-clientes-site.mjs`, que se ejecuta después de `build:marketing`:
  - copia `dist/marketing/browser`;
  - reemplaza `index.html` por `index.csr.html` (shell vacío);
  - elimina `index.csr.html` de la copia, o lo deja idéntico al nuevo `index.html`;
  - reemplaza `robots.txt` por uno sin el `Sitemap:` de Fluora.
- **Guarda `clientes`:**
  - `index.html` es un shell CSR vacío y del mismo build que marketing;
  - no hay ningún HTML con el h1 del home;
  - en `firebase.json`, el sitio `clientes` no tiene rewrites a `landingPage`, `directoryPage`, `sitemap` ni `tenantPage`, y `**` es el último rewrite.
- **El frontend no cambia:** en un host que no es de Fluora, `tenantHostResolver` ya resuelve el negocio por backend.
- **Pendiente para 012, no en P:** en un dominio de cliente, la ruta `/e/<otro-slug>` del router mostraría otro negocio. En 012 se redirige al origen de marketing.

## 4. P3 · Verificación local
| Prueba | Criterio |
|---|---|
| `npm run build` (marketing, application y clientes) | exit 0. El tamaño del inicial de marketing se reporta frente a 503.12 kB. No entra `platform-server` en los JS del navegador |
| `check-public-builds` y las guardas de hidratación y de clientes | verdes |
| Preview simulado (`public-preview.mjs`, con la tabla de rewrites de cada sitio) | Marketing: `/` → prerender; `/acceso`, `/privacidad` y una ruta inexistente → CSR; `/e/nail-art/servicios` y las landings sin cambios. Clientes: `/` → CSR vacío; `/barberias` y `/negocios` → CSR, sin Function |
| Hidratación del artefacto productivo | 375, 820 y 1440: DOM reutilizado, 0 NG05xx y 0 errores de consola; event replay en móvil; anclas y nav como en R3 |
| Matriz de 16 tamaños y P2 del tenant (30 muestras) | Sin empeorar frente a las evidencias de R3 y F4 |
| `npm test` normal | verde en 3 corridas seguidas |
| Node (`functions` y `scripts`) | verde, con las pruebas nuevas de las guardas y del script de clientes |

## 5. P4 · Medición de la meta F4
- El mismo orquestador y el mismo perfil de M0. Serie **alternada de 5 + 5**: M2 (el build CSR de `4e93dd1`) frente a P (prerender), servidos por el preview con compresión.
- **Meta:** mediana de P ≥ 85; A, BP y SEO en 100; CLS ≤ 0.01 en las 10 muestras; el LCP de P es el h1 o el lead.
- Las cifras, el hash del build y las versiones se registran. Si queda por debajo de 85, **decide el usuario**. No hay más microajustes.
- La evidencia que entra al repo es solo un **resumen**. Los JSON completos se quedan en `/private/tmp` (las de R3 sumaron 2.3 MB).

## 6. Despliegue (diseñado, no autorizado)
Cuando el usuario lo autorice, este es el orden:
1. `firebase hosting:sites:create <id-clientes>` y `firebase target:apply hosting clientes <id-clientes>`.
2. Desplegar `hosting:clientes`. Comprobar que `https://<id>.web.app/` sirve el shell vacío y que `/api/v1/...` responde por `apiProxy`.
3. Desplegar `hosting:marketing`. Comprobar `/` (el h1 viene en el HTML), `/index.csr.html` (200 y vacío), `/acceso` (CSR) y `/e/<slug>` (tenantPage). Las Functions no cambian en P.
4. **Reversión:** se vuelve a desplegar `hosting:marketing` desde `4e93dd1`. El sitio de clientes no afecta a marketing.

Los dominios de clientes se conectan al sitio `clientes` **solo** con el flujo de 012. **Ningún** dominio de cliente se conecta al sitio de marketing.

## 7. Orden y entrega
P1 → P2 → P3 → P4. Cada paso se registra en `respuestas/008-f4-m3-p.md` y espera la revisión de Claude. Commit solo con la autorización del usuario. Sin push ni despliegue.
