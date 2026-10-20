# Revisión 008 F4: M1 medido y paso a M2

No pude guardar el archivo porque esta sesión no tiene herramienta de escritura. El texto queda listo para guardarlo como `docs/agentes/revisiones/008-f4-m1-medicion.md`, con autoría de Claude.

Solo leí archivos. No ejecuté builds, suites ni Lighthouse, ni comprobé las cifras de `tmp/lighthouse/m1-paired/`. Las tomo de la solicitud.

**Veredicto: no hay ningún problema de código que impida empezar M2.** La propuesta manual es correcta. Abajo van los puntos que hay que vigilar durante M2 y un hallazgo Bajo en la prueba de NG8107.

## Lo que quedó cerrado
- **Procedencia de Lighthouse:**
  - `scripts/lighthouse-home.mjs:132` ya incluye `scripts` en el pathspec de `sourceDirty`.
  - `:133` agrega el sha256 de `lighthouse-home.mjs` y `performance/public-preview.mjs`.
- **Hoja global como último hijo:** `tenant-public-styles.ts:49` usa `global ? global.nextSibling : componentStyle`, así que el link queda al final de `<head>`.
- **NG8107:**
  - `admin-dashboard.component.ts:374` termina en `.at(0) ?? null`.
  - La plantilla (`:185-186`) mantiene `?.`.
  - La prueba está en `admin-dashboard.component.spec.ts`.
- **Medición de M1:** el resultado es neutro, sin regresión. Las medianas son 83 en M0 y 84 en M1, y los rangos se solapan por completo (M1 va de 79 a 85). Se cumplen A/BP/SEO 100, CLS ≤ 0.01 y el Header ausente. **La meta de 85 no está alcanzada.** Mantener esa formulación y mencionar que A fue primero en 3 de las 5 corridas.

## Hallazgos
| Pri. | Ubicación | Hallazgo | Corrección y prueba |
|---|---|---|---|
| Baja | `admin-dashboard.component.spec.ts:13-18` | **La prueba no renderiza la plantilla.** Sin `detectChanges()`, solo prueba el `computed` y no las líneas `:185-186` que causaban NG8107. No es una «prueba real del componente sin módulos». | Llamar a `fixture.detectChanges()`, absorber las peticiones con `HttpTestingController` sin responderlas y comprobar que el `h1` está vacío y que no hubo excepción. Confirmar además que NG8107 ya no aparece en la salida del build. |

## Lo que hay que vigilar en M2
Revisé `node_modules/@angular/build` 21.0.6. Estos puntos ya están comprobados en el código:

**1. `tenant-public.css` sigue sin hash.**
- `global-styles.js:43-46` pone otros nombres de salida a los estilos con `inject:false`, así que la URL fija de `tenant-public-styles.ts:27` sigue sirviendo.
- `check-public-builds.cjs:14` lo vigila.

**2. Las URL `/fonts/...` se dejan tal cual.** `css-resource-plugin.js:48` marca como externas las rutas que empiezan con `/`, así que `tenant-fonts.css` no cambia.

**3. Los nombres nuevos (`styles-HASH.css`, `main-HASH.js`, `chunk-*`) ya son compatibles.**
- `check-public-builds.cjs:10` acepta `styles[.-]`.
- `tenant-public-styles.ts:46` busca `href^="styles"`.
- `lighthouse-home.mjs:45` encuentra el Header por su contenido, no por el nombre.
- Ya no habrá `runtime` ni `polyfills`, y no hay ninguna comprobación que los espere.

**4. Plantilla SEO.**
- `functions/scripts/prepare-template.mjs:7` lee `dist/marketing/browser/index.html`, y esa ruta se conserva.
- Los renderizadores (`seo-renderer.js:26-29`, `landing-renderer.js:26-27`, `directory-renderer.js:27-28`) solo tocan title, meta, canonical, ld+json, noscript y app-root, así que no les afecta `modulepreload`.
- **Prueba:** correr `prepare-template` con el build nuevo y las pruebas de `functions`. Comprobar que en el HTML renderizado de un tenant `/runtime-config.js` sigue siendo un script clásico en `<head>`, antes de los `type="module"`, y que `__FLUORA_TENANT__` va antes que `main`.

**5. Media: el criterio de «CSS idéntico byte a byte» deja de servir.** esbuild minifica distinto que webpack, así que el CSS de application y de marketing va a cambiar sin que cambie nada en `src`.
- No hay que reportarlo como fallo ni forzarlo.
- Para M2, sustituirlo por la cobertura de clases DOM de 15 rutas, `check:marketing-css` y la comparación de reglas y selectores, no de bytes.
- Fijar una línea base nueva después de M2.

**6. Media: guardar el build de M1 antes de compilar M2.**
- Con un `outputPath` de objeto, el builder nuevo borra la carpeta base `dist/marketing`, no solo `browser/`.
- Copiar `dist/marketing/browser` de M1 a `tmp/` antes de compilar, para la comparación pareada M1/M2 con los mismos scripts.
- Además, `3rdpartylicenses.txt` pasa a la carpeta base y ya no se publica. Es solo informativo.

**7. Media: el servidor del usuario en 4200.**
- El proceso que está corriendo sigue con webpack. **Cuando el usuario lo reinicie, pasará a Vite.** Hay que avisarle.
- Vite rechaza los hostnames desconocidos. localhost, las IP de LAN y `*.localhost` funcionan; un túnel o un dominio puesto en `/etc/hosts` necesitaría `allowedHosts`. Esto no está en el código, hay que preguntárselo al usuario.
- El proxy `/api/v1` es compatible. Vite ignora `logLevel`.
- **Prueba:** en un servidor de desarrollo temporal propio, en otro puerto, `GET /api/v1/publico/plataforma/soporte` por el proxy responde 200 y DEP0060 ya no aparece.

**8. Esquema de `angular.json`.**
- Hay que quitar `main` de `options` (`:22`) y de `marketing` (`:76`) y de `application` (`:90`). Si queda alguno, el builder falla de forma visible.
- `index` en forma de objeto (`:91-94`), `inject:false`, `inlineCritical`, `fonts:false` y los budgets están soportados.
- Los avisos de CommonJS que aparezcan se reportan. No se silencian con `allowedCommonJsDependencies` sin revisarlos.

**9. Sin cambios de dependencias.** `@angular-devkit/build-angular` queda sin uso después de M2. Quitarlo es otro paso y necesita la aprobación del usuario.

## Sin cambios
- La equivalencia visual exacta sigue sin probarse, por las capturas con distinto scroll y estado.
- D1 (`Cache-Control: no-cache` para `/tenant-public.css`) sigue siendo requisito antes de cualquier despliegue y necesita la aprobación del usuario.
- Después de M2: medición pareada M1/M2 y P2. Si la mediana sigue por debajo de 85, el usuario decide entre M3 o aceptar el valor.
- Las revisiones ajenas del árbol se conservan. Sin push ni despliegue.
