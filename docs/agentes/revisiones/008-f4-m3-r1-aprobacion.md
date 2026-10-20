# 008 F4 M3: R1 verificado. Recomendación de aprobar y pasar a R4

Autor: Claude Code, 2026-10-19. Leí el diff de R1 y **ejecuté las verificaciones yo mismo** (no me baso solo en lo que reporta Codex). Sin cambios en el código ni despliegues.

## Verificado ejecutando
- `npm test` con la configuración normal (Node 20.19, `TZ=America/Mexico_City`, sin worker temporal), **dos corridas seguidas: 82 archivos / 304 pruebas en verde las dos veces.** T1 queda resuelto (`ac7781d`).
- `npm run build`:
  - marketing: 476.17 / 130.28 kB, con «Marketing CSR shell validated»;
  - application: 824.99 / 167.70 kB.
- `check-public-builds.cjs`: exit 0 («empty CSR, asset contracts, SEO renderers and hosting fallback verified»).
- `index.csr.html` es **idéntico byte a byte** a `index.html` (es lo esperado antes del prerender), y application **no** tiene CSR, que también es lo esperado.
- Node 22.21.1: `functions` da **25/25**, y el preview más `prepare-csr-shell` dan **12/12**. Son las mismas 37 que reporta Codex.

## Revisión del código
- `firebase.json`:
  - el fallback de marketing pasa a `** → /index.csr.html` y queda **al final** de los rewrites;
  - `predeploy` ejecuta la guarda de builds;
  - el sitio `app` no cambia.
- `functions/index.js`: `tenantPage`, `landingPage` y `directoryPage` ahora usan `loadCsrTemplate`, que descarga `/index.csr.html` con timeout de 5 s y `redirect: 'error'` y **rechaza cualquier plantilla que no sea un shell vacío** (un `<app-root>` vacío, sin estado de hidratación). Con eso, el contenido del home nunca va a parar dentro de un tenant o una landing.
- `assertCsrContract` exige que el CSR y el index sean del mismo build (main, runtime, estilos y base).
- `prepare-template.mjs` queda marcado como obsoleto y ya no lo usa el flujo de producción. Se puede borrar después, en una limpieza aparte.
- R2 está confirmado en Firebase real (solo lectura): solo están conectados `refluora.com`, `www` y `app`. **0 dominios de clientes.**

## Condiciones que siguen vigentes (no bloquean R4)
1. **Orden de despliegue, cuando se autorice:** primero Hosting marketing, para que `/index.csr.html` exista en el origen, y después Functions. Si se revierte R1, se revierten los dos juntos. Antes de desplegar Functions, comprobar con un GET que `/index.csr.html` responde 200.
2. **El sitio de hosting separado para dominios de clientes** es obligatorio antes de desplegar el prerender o de conectar cualquier dominio de cliente. No se crea ahora.
3. **Carpeta `postman/` en la raíz del repo (sin versionar).** Es el workspace local de la app de Postman: `collections`, `environments`, `globals`, `mocks` y `.postman`. `globals` y `environments` pueden tener secretos. **No debe entrar en ningún commit:** hay que agregar `postman/` a `.gitignore`, igual que `.postman/`. No revisé su contenido para no leer posibles secretos. Las colecciones versionadas siguen en `backend/postman/`.

## Recomendación
**Apruebo R1** y que se continúe con R4 (preparar el código para que funcione en el servidor, según `008-f4-m3-diseno.md` §3.2-R4). También recomiendo un **commit de R1 como punto de control** antes de R4, con `.gitignore` actualizado para `postman/` y sin despliegue. Ese commit lo autoriza el usuario.
