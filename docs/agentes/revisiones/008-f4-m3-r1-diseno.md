# 008 F4 · M3 R1: revisión del diseño (separar el shell CSR)

Autor: Claude Code, 2026-10-19. Solo hice lectura: no ejecuté builds ni suites y no escribí código. Revisé `firebase.json`, `package.json`, `angular.json`, `functions/index.js`, los renderers de `functions/` y sus pruebas, `functions/scripts/prepare-template.mjs`, `scripts/check-public-builds.cjs`, `scripts/performance/public-preview.mjs` con su test, y `scripts/lighthouse-home.mjs`.

**Veredicto:** el diseño no se puede implementar como está. El punto 3 apunta a un archivo que producción no lee (H1). Hay que corregirlo antes de escribir código. Además, R1 queda más simple si se deja fuera `application` (H3).

## H1 · Alta: las Functions no usan `functions/template/index.html`
- **Evidencia:** `functions/index.js:25-38`. `loadCurrentTemplate()` descarga en cada petición `${marketingOrigin}/index.html`, y la única validación es `includes('<app-root')`. La usan:
  - `tenantPage` (`:94`);
  - `landingPage` (`:187`);
  - `directoryPage` (`:212`). El diseño no menciona este caso.
- **`prepare-template.mjs` no tiene consumidores:**
  - su salida está en `functions/.gitignore:2`;
  - el grep no encuentra ninguna referencia en el código;
  - ya se había visto que la plantilla estaba obsoleta (`008-f4-assets-locales.md:35`).
- **Corrijo mi diseño anterior:** el punto 3.1 de `008-f4-m3-diseno.md` decía que se usaba esa plantilla, y era incorrecto.
- **Riesgo:** si R1 solo cambia `prepare-template`, después de M3 las Functions inyectarían el tenant, la vertical o el directorio sobre el home prerenderizado.
  - `renderTenantHtml` solo cambia el `<head>` (`seo-renderer.js:133`). Por eso, el contenido del home quedaría visible dentro del `<app-root>` en `/e/**`.
- **Corrección (Codex):**
  1. Sacar la carga a un módulo CommonJS dentro de `functions/`, por ejemplo `functions/csr-shell.js`, que se pueda probar sin `firebase-functions`. Debe exportar:
     - `assertCsrShell(html)`;
     - `loadCsrTemplate(origin, fetchImpl)`.
  2. `loadCsrTemplate` pide **`/index.csr.html`**:
     - sin fallback a `/index.html`;
     - si recibe un 404, el HTML está contaminado o se agota el tiempo, lanza un error y se usan los caminos de 503 que ya existen.
  3. Las tres Functions usan `loadCsrTemplate`.
  4. Validador único. Las Functions se despliegan solo desde `functions/`, así que el validador vive ahí y los scripts de la raíz lo importan con `require`, como ya hace `check-public-builds.cjs:23`.
  5. Sobre `prepare-template.mjs`, mi recomendación es **eliminarlo** (el usuario decide). La alternativa es dejarlo como está y marcarlo obsoleto en ESTADO. No conviene invertir en él, porque crea una falsa garantía.
- **Orden de despliegue (dejarlo en ESTADO; hoy no se despliega):**
  - Las Functions nuevas necesitan que el Hosting ya tenga `index.csr.html`.
  - Orden: primero `hosting:marketing`, después `functions`. No hay que contar con el orden interno de un `firebase deploy` combinado.
  - **Rollback:** volver el Hosting a una versión anterior a R1 mientras siguen activas las Functions de R1 deja `/e/**`, las verticales y `/negocios` en 503. Las dos cosas se revierten juntas.
- **Pruebas (`node --test`, con un `fetch` inyectado):**
  - la URL que se pide termina en `/index.csr.html`;
  - shell válido → se devuelve;
  - home con contenido en `<app-root>` → se rechaza;
  - 404 → se rechaza y no hay ninguna segunda petición a `/index.html`;
  - marcadores de hidratación → se rechaza.

## H2 · Alta: guarda real antes de desplegar
- **Problema:** «el guard lo detecta antes de despliegue» no está conectado a nada.
- **Riesgo:** si alguien hace `ng build` directo, se salta `postbuild:marketing` y no se genera `index.csr.html`. Con `** → /index.csr.html`, Firebase respondería 404 en `/acceso`, `/registro`, `/privacidad`, etc. Por H1, también daría 503 en las Functions.
- **Corrección:**
  - agregar `"predeploy": ["node scripts/check-public-builds.cjs"]` en el target `marketing` de `firebase.json`;
  - esa guarda solo lee, no construye ni corrige nada;
  - es un cambio de configuración local, no un despliegue.
- **Prueba:** ejecutar el check contra una carpeta temporal sin `index.csr.html` y comprobar que termina con código distinto de 0.

## H3 · Media: dejar `application` fuera de R1 (alternativa más simple)
- **Evidencia:** el prerender queda solo en la configuración `marketing` (`008-f4-m3-diseno.md` §3.3: «`application` no cambia»).
- Mi recomendación anterior («en los **dos** sitios») era incorrecta. En `app`, `** → /index.csr.html` no aporta nada y añade el mismo modo de fallo 404 en todos los enlaces profundos del panel.
- **Corrección:**
  - el target `app` conserva `** → /index.html`;
  - no hay `postbuild:app`;
  - `check-public-builds` comprueba que el `index.html` de application tiene `<app-root>` vacío. Así, si alguien activa el prerender ahí por error, se detecta.

## H4 · Media: definición única de «shell vacío» y de «mismo runtime»
- **`assertCsrShell(html)`:**
  - exactamente un `<app-root …>…</app-root>`;
  - su contenido, quitando espacios y comentarios, queda vacío;
  - ni en el documento ni en `<app-root>` aparecen `ng-server-context`, `ngh=`, `<script id="ng-state"` ni `ng-version`;
  - **no se limpia nada, solo se rechaza**.
- **Contrato entre `index.csr.html` e `index.html`** (también detecta un CSR viejo o ajeno):
  - el mismo `<base href>`;
  - `runtime-config.js` como script clásico antes del módulo;
  - el mismo `src` de `main` y de `polyfills`;
  - el mismo `href` de la hoja de estilos global;
  - el mismo `<noscript>` de estilos.
- **Igualdad byte a byte:**
  - hay que exigirla mientras `index.html` siga siendo shell (fase R1);
  - **no** después del prerender, porque `inlineCritical: true` (`angular.json:58`) y los modulepreload van a cambiar el `<head>` del home.
- **R4/prerender:** comprobar con el primer `index.csr.html` nativo que pasa el validador. Si no pasa, se ajusta con evidencia; no se relaja por adelantado.

## H5 · Media: `scripts/prepare-csr-shell.mjs` (solo marketing)
Los casos del diseño están bien:

| `index.html` | `index.csr.html` | Resultado |
|---|---|---|
| vacío | no existe | se copia |
| cualquiera | ya existe | valida shell y contrato; no sobrescribe |
| prerenderizado | no existe | falla |

- Agregar el caso **«`index.html` no existe» → falla**.
- Exportar `prepareCsrShell(root)` y usar una CLI con `--root` obligatorio.
- `package.json` usa `postbuild:marketing`. Hoy hay `prebuild:marketing` y npm ejecuta los dos hooks con `npm run build`.
- **Pruebas** en `mkdtemp` para los cuatro casos, más una segunda ejecución sobre su propia salida, que debe ser idempotente.

## H6 · Media: preview (`public-preview.mjs`)
- **Raíz explícita:** hoy `/` llega al fallback porque `stat(root)` es un directorio (`:61-64`). Hay que mapear `/` y `/index.html` a `index.html` en los dos modos.
- **Opción `spaShell`:**
  - valores `'index' | 'csr'`; cualquier otro valor se rechaza;
  - `'index'` es el predeterminado, para los artefactos de M0 y M2;
  - con `'csr'`, `assertCsrShell` se aplica **antes de devolver el servidor**, y si falla no se escucha ningún puerto.
- **El fallback sigue limitado a `Accept: text/html`.**
- **Scripts de medición:**
  - `lighthouse-home.mjs` y `lighthouse-tenant-cache.mjs` reciben `--spa-shell`;
  - el valor se registra en el resumen JSON (`lighthouse-home.mjs:113-121`), para que una medición de M3 no use el modo baseline sin dejar constancia.
- **Pruebas:**

| Petición | Resultado esperado |
|---|---|
| `/` y `/index.html` | `index.html` |
| `/registro`, `/privacidad`, ruta HTML inexistente | shell elegido |
| asset inexistente sin Accept HTML | 404 |
| `/tenant-public.css` | el archivo |
| `/e/slug/servicios` | shell elegido; documentado como **no equivalente** a Functions |
| ruta con traversal | 403 |
| `/api` | el proxy y las pruebas actuales sin cambios |
| `csr` sin el archivo | la creación falla |

## H7 · Baja: cabeceras y documentación
- **Cabeceras:** `**/*.html` (`firebase.json:26`) ya pone `no-store` a `index.csr.html`, así que no hace falta otra regla.
- **Documentación:**
  - actualizar `scripts/performance/README.md`;
  - registrar en ESTADO los bloqueos de despliegue:
    1. orden Hosting → Functions (H1);
    2. el sitio `tenant-domains` es requisito antes de desplegar el **prerender** y antes de conectar dominios de clientes. R1 por sí solo no lo requiere, porque `/` sigue siendo CSR.
- Sin cambios en `.firebaserc`, sin target ficticio y sin crear sitios.

## Alcance corregido de R1
1. `prepare-csr-shell.mjs`, solo para marketing, con `postbuild:marketing` (H5).
2. `firebase.json`: en `marketing`, `** → /index.csr.html` más el `predeploy` del check. `app` no cambia (H2, H3).
3. `functions/csr-shell.js`, con `loadCurrentTemplate` sustituido en las tres Functions (H1).
4. `check-public-builds.cjs`:
   - el validador y el contrato de H4;
   - `renderTenantHtml`, `renderLandingHtml` y `renderDirectoryHtml` probados sobre el CSR;
   - en application, `index.html` vacío.
5. Preview con `spaShell` (H6).
6. `prepare-template.mjs` eliminado u obsoleto, según decida el usuario.

## Validación esperada
- `npm run build` (debe crear `dist/marketing/browser/index.csr.html`, idéntico a `index.html`);
- `npm run check:public-builds`;
- `cd functions && npm test`;
- `node --test scripts/performance/` y las pruebas nuevas de `prepare-csr-shell`;
- `npm test` con la configuración normal.

Evidencia en `respuestas/008-f4-m3-r1*.md` y en ESTADO. Sin deploy, sin DNS y sin sitio nuevo. Después de R1, parar para que el usuario revise antes de R4.

---
Nota: esta sesión solo tiene herramientas de lectura. No guardé `docs/agentes/revisiones/008-f4-m3-r1-diseno.md`: este texto es su contenido.
