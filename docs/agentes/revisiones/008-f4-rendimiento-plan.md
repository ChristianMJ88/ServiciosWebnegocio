# 008 F4: plan para cerrar la meta de rendimiento del home

Autor: Claude Code, 2026-10-19. Revisión de solo lectura: leí `angular.json`, `src/styles.css`, `dist/marketing/stats.json` y `dist/marketing/browser/index.html`.

## Qué hizo Codex desde `008-f4-header-cierre`
- **No hubo cambios de código** después de esa revisión: ningún archivo de `src/` ni de `backend/src` es más nuevo que ella. Solo hubo mediciones, revisiones de UI y limpieza, todo registrado en `respuestas/008-f4.md`.
- Se cumplió lo que se pidió:
  - el build application tiene exit 0, hash `6f4b4f6b883ec6f1` y 905.70 kB / 186.35 kB;
  - se probó la navegación del catálogo en 390, 820 y 1440, con menú, Escape, foco y resize;
  - se devolvieron a su estado original la preferencia de Chrome y la emulación de reduced-motion.
- Siguen abiertos, y la respuesta lo reconoce: **Performance en Home tiene mediana 62 (64, 62, 66, 57, 50), frente a la meta de 85**, y el P2 (CLS del tenant con caché fría y caliente) no está cerrado.
- **El preview de 4300 sigue activo.** Hay que detenerlo cuando el usuario ya no use esa ventana.

## Diagnóstico: el problema no es el Header
Los ajustes de F4 ahorraron unos 5 kB estimados. Lo que pesa de verdad son estas dos cosas:

1. **El build de producción usa el builder antiguo de webpack.**
   - `angular.json:18` declara `@angular-devkit/build-angular:browser` en Angular 21.0.6. La entrada de pruebas (`:109`) ya usa `@angular/build:application` (esbuild), así que el código compila con el builder nuevo.
   - Lo que pierde el builder antiguo: los chunks ESM con `modulepreload` y el mejor tree-shaking del builder nuevo. Hoy `main` incluye en el inicial unos 950 kB **sin minificar** de `@angular/core`, 228 kB de `common` y 212 kB de `router` (según `stats.json`).
2. **La hoja global de estilos es la del panel de administración.**
   - `src/styles.css` importa el tema de Angular Material (`azure-blue`), Bootstrap completo, Bootstrap Icons, `admin-site-section`, `admin-shell` y `operation-workspace`. El resultado es `styles.*.css` con **362 kB** que también carga el home de marketing.
   - `inlineCritical` lo vuelve asíncrono, pero en un móvil lento el navegador igual tiene que descargar, analizar y aplicar 362 kB de CSS. Eso eleva el TBT (660 ms de mediana) y el LCP (4.2 s).
3. **El ruido de medición tapa los cambios.** En un mismo build, el rango va de 50 a 66, con previews de 4300/4400, Chrome con otras pestañas y el ahorro de energía alternando. Con tanta varianza, un cambio de 5 kB no se puede medir.

## Plan, en orden (cada paso se mide antes de seguir)
**M0 · Medición estable (antes de cambiar nada).**
- Lighthouse CLI con un perfil fijo: móvil, `--throttling-method=simulate` (el predeterminado, que reduce la varianza de la máquina), 5 corridas y la mediana.
- Contra el build de producción servido con compresión. La Mac conectada a la corriente, sin otros servidores de preview ni pestañas pesadas.
- Se guarda un script, por ejemplo `scripts/lighthouse-home.sh`, que registra la versión de Lighthouse, el hash del build y las 5 cifras.
- Esa medición es la nueva línea base. **No se comparan series de condiciones distintas.**

**M1 · Hoja de estilos propia para marketing. Impacto esperado: alto. Riesgo: medio.**
- La configuración `marketing` usa `styles.marketing.css`, con solo lo que usan el home, las verticales, legal y el directorio: tokens de marketing, fuentes y las utilidades de Bootstrap realmente usadas. **Sin** tema de Material, sin admin ni operation-workspace, y con Bootstrap Icons solo si el marketing los usa.
- `application` mantiene `styles.css` sin cambios.
- **Cuidado:** las rutas de tenant (`/e/:slug`) también se sirven desde marketing. Hay que inventariar qué estilos globales necesitan el header del tenant, booking, checkout y servicios. Si los necesitan, se cargan con `styleUrl` en sus componentes o se pasan a una hoja de tenant **diferida**, y nunca se quitan sin ese inventario.
- **Validación:** capturas de antes y después del tenant (home, servicios, booking, checkout) en 375, 820 y 1440, sin diferencias. Pasan las specs y la matriz de 16 tamaños del home.

**M2 · Migrar el build al builder `@angular/build:application`. Impacto esperado: alto. Riesgo: medio.**
- `ng update` (las migraciones `use-application-builder`) o el cambio manual de las tres configuraciones (marketing, application y la predeterminada), manteniendo `outputPath`, `index` y `fileReplacements`.
- Hay que revisar `outputPath` (el builder nuevo agrega `browser/`), la plantilla SEO de Functions y su script, `check:public-assets`, el `ng serve` habitual y los budgets.
- No se despliega. Primero se comparan los artefactos (index, rutas de assets y headers esperados).

**M3 (solo si con M0–M2 no se llega a 85) · Prerender (SSG) de las rutas de marketing estáticas** (`/`, verticales, legal), con `@angular/ssr` en modo de solo prerender. No se usa SSR en tiempo real.
- Es el cambio que más mueve FCP y LCP, pero **toca el hosting y Functions**, así que requiere un diseño propio y la aprobación del usuario antes de implementarse.

## Criterio de cierre de F4
- Mediana de 5 corridas con M0: **≥ 85**, A/BP/SEO 100, CLS ≤ 0.01, y sin el chunk de Header en la red del home.
- P2: CLS del tenant con caché fría y caliente en 390, 820 y 1440, medido con el mismo perfil de M0.
- Si después de M1 y M2 la mediana se queda por debajo de 85, Codex reporta las cifras y **el usuario decide** entre M3 o aceptar el valor alcanzado. No se pasan más horas en microajustes.

## Adenda 2026-10-19: commit autorizado y continuación
El usuario **autorizó el commit** de lo aprobado y pidió seguir con el plan. Codex hace el commit **antes de M0**.

**Contenido:** 011 y 008 (F0–F3, más el código de F4 ya revisado), en commits separados por entrega si se pueden separar limpiamente.
- **011:**
  - `backend/postman/*` y su README;
  - los cambios de pruebas MySQL y aislamiento;
  - `package.json` (scripts);
  - `respuestas/011*` y `revisiones/011*`.
- **008:**
  - el código de marketing, footer, header, legal y directorio;
  - V140 y el módulo de soporte, si forman parte de F0;
  - `respuestas/008-*` y `revisiones/008-*`.

**Fuera del commit:**
- **`.postman/`**: es la configuración local de la herramienta (id del workspace y caché de skills). No es parte del producto. Se agrega a `.gitignore`, o se deja sin versionar si el usuario prefiere.
- Las revisiones ajenas, como hasta ahora.
- Archivos de `dist/` y logs.

**Antes del commit, Codex:**
- busca secretos en lo que entra. Claude ya buscó en `backend/postman` y `.postman` patrones de tokens de Mercado Pago, claves de Postman, Bearer, contraseñas y access tokens literales, sin coincidencias;
- confirma que el diff no incluye `.env` ni credenciales;
- pasa `git diff --check`.

Sin push ni despliegue.

**Después del commit:**
- se sigue con M0 → M1 → M2, cada paso con su medición y su revisión de Claude;
- M3 solo con la aprobación del usuario.

**El preview de 4300** se detiene cuando el usuario confirme que ya no usa esa ventana. Codex se lo pregunta; no lo cierra por su cuenta.

## Pendientes conocidos, sin cambios
- No hay `@error` para el chunk de Header (P3).
- Falta el primer salto SPA de auth o panel al tenant.
- Siguen los timeouts intermitentes de la suite Angular con varios workers, como deuda de pruebas.
- **No hay commit de 011 ni de 008.** Recomiendo commitear ahora lo ya aprobado (011 y 008 F0–F3, más el código de F4 revisado) **antes** de M1/M2, que son cambios de build con más riesgo. Así queda un punto al que se puede volver. Lo autoriza el usuario.
