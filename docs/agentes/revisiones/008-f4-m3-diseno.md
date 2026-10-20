# 008 F4: estado verificado, corrección de la suite y diseño de M3 (prerender)

Autor: Claude Code, 2026-10-19. El usuario decidió (1) **diseñar M3** y (2) autorizar el **commit de M1, M2 y CLS después de dejar verde la suite de Angular con la configuración normal**. M3 no se despliega. Su implementación necesita que el usuario apruebe este diseño.

## 1. Verificación propia (no solo lo que reporta Codex)
- **Commits ya hechos:** `798137b` (011) y `e642571` (008 F0–F4 aprobado). Quedan sin commit 22 archivos de M1, M2 y CLS, que ya están revisados en `008-f4-m1-*`, `m2-*` y `cls-*`.
- **`npm run build`** (marketing y application), que ejecuté yo: los dos terminan bien.
  - Marketing: **476.17 kB inicial / 130.28 kB estimados**, frente a 928.86 / 186.43 antes de M1.
  - Application: 824.99 / 167.69.
  - Sin NG8107.
- **NG8107:** está bien corregido. `admin-dashboard.component.ts:374` termina en `?? this.modulosAdmin().at(0) ?? null` y conserva `?.`.
- **Rendimiento del home** según los JSON de Codex, revisados en `008-f4-m2-cierre`: M0 83, M1 84 y M2 82 de mediana. Las tres están dentro del ruido (±3) y ninguna llega a 85. **Con M1 y M2 se agotó lo que se puede ganar con tamaño de paquete.** Lo que falta es el tiempo hasta el primer pintado, porque el home sigue siendo una SPA: `<app-root></app-root>` está vacío hasta que el JS arranca.

## 2. T1 · Media (requisito para el commit): `npm test` no pasa con la configuración normal
- **Evidencia:** corrí dos veces la suite completa, con Node 20.19, `TZ=America/Mexico_City` y los workers predeterminados (8 CPU).
  - 1.ª corrida: falla 1 de 301 pruebas.
  - 2.ª corrida: fallan 2 de 301, y no son las mismas.
  - Las dos veces falla `admin-dashboard.component.spec.ts` («admite que el backend no conceda ningún módulo…»), por **timeout de 5 s**. Sola, pasa en **2.1 s**, porque monta el `AdminDashboardComponent` entero con todos sus imports.
  - En la 2.ª corrida, `legal-page.component.spec.ts` falla con «Cannot configure the test module when the test module has already been instantiated». Sola, pasa. Es un **efecto en cadena**: el `TestBed` queda instanciado después del timeout dentro del mismo worker.
- Codex siempre corrió la suite con **un worker temporal** (respuestas 005, 010 y 008-f4). Es decir, el comando del repositorio, `npm test`, no está verde. Esta es la tercera vez que aparece la deuda de timeouts.

**Corrección (Codex):**
1. **No subir el timeout ni quitar aserciones.**
2. La prueba de NG8107 se vuelve ligera:
   - opción a: extraer la selección del módulo activo a una función pura, por ejemplo `seleccionarModuloActivo(modulos, seccion)`, usarla en el `computed` y probarla sin `TestBed` (lista vacía → `null`, sección inexistente → el primero, sección válida → ese módulo);
   - opción b: si se mantiene la prueba del componente, usar `overrideComponent` con un template mínimo.
3. Aislamiento: comprobar por qué un timeout deja el `TestBed` instanciado para el siguiente archivo. Revisar la opción `isolate` del builder `unit-test` y si hay estado global compartido. Que un timeout ya no arrastre otros archivos. Si queda algún archivo más lento que el límite con carga, se aligera de la misma forma, sin tocar el timeout.
4. **Criterio:** `npm test` (configuración normal, sin worker temporal) en verde **3 veces seguidas**, con el conteo de cada corrida en `respuestas/008-f4-rendimiento-plan.md`. Después, el commit de M1, M2 y CLS, más esta corrección, en commits separados si se pueden separar limpiamente. Sin push ni despliegue.

## 3. M3 · Diseño de prerender del home

### 3.1 Cómo se sirve hoy (verificado en `firebase.json` y `functions/`)
- La **misma página de Hosting** (`target marketing`, `dist/marketing/browser`) sirve:
  - `/`, que es el `index.html` estático;
  - las verticales, a través de la función `landingPage`;
  - `/e/**`, a través de `tenantPage`;
  - `/negocios`, a través de `directoryPage`;
  - `**`, que cae en `/index.html`.
- Las funciones `landingPage` y `tenantPage` toman como **plantilla** `functions/template/index.html`, que es una copia de `dist/marketing/browser/index.html` (`functions/scripts/prepare-template.mjs`). Inyectan HTML de SEO dentro de `<app-root>`. Angular **no hidrata**: borra ese contenido y vuelve a renderizar.
- El frontend ya soporta **dominio propio** (`PlatformHostService`: un host que no es marketing ni app se trata como tenant). **En ese caso, el `/` de un dominio propio que apunte a este mismo sitio recibe el mismo `index.html`.**

### 3.2 Decisión de arquitectura
Se prerenderiza solo **`/` en el host de marketing**, con hidratación. Para que funcione, se requieren las cinco condiciones siguientes. Si falta cualquiera, M3 no se implementa:

**R1 · El fallback SPA y la plantilla de Functions no pueden ser el home.**
- Con `outputMode: 'static'`, `index.html` pasa a ser el home prerenderizado, y aparece un `index.csr.html` como shell vacío.
- El rewrite `** → /index.html` cambia a `** → /index.csr.html` en los **dos** sitios de hosting (marketing y app). Así, `/acceso`, `/privacidad`, etc. no reciben HTML del home.
- `prepare-template.mjs` copia **`index.csr.html`**. Si no, `tenantPage` y `landingPage` inyectarían su contenido dentro del HTML del home.
- `check:public-builds` y las pruebas de `functions/` verifican las dos cosas.

**R2 · Dominios propios fuera del home prerenderizado.** Firebase Hosting no puede elegir un archivo estático según el host. **Recomendación:** un **sitio de Hosting separado para los dominios propios** (por ejemplo, el target `tenant-domains`), con el mismo build de marketing **sin** el `/` prerenderizado (su `/` es `index.csr.html`) y las mismas funciones `tenantPage`, `apiProxy`, etc.
- **Condición previa:** comprobar, solo leyendo la consola o la CLI de Firebase y **sin cambiar nada**, que hoy **no hay dominios propios conectados** al sitio de marketing. Codex reportó que «host propio físico no configurado».
- Si ya hubiera alguno, M3 se detiene y se le pregunta al usuario.
- Crear el sitio nuevo y conectar dominios es un cambio de hosting: **se diseña y se deja configurado en `firebase.json`/`.firebaserc`, pero no se ejecuta `firebase deploy` ni `hosting:sites:create` sin autorización.**

**R3 · Hidratación sin parpadeo ni datos congelados.**
- `provideClientHydration()` en `main.marketing.ts`, con `withEventReplay()`.
- Las secciones con datos de la API (planes, soporte configurable, directorio, precios) **no se prerenderizan con datos**. Siguen en `@defer (on viewport)`, que en el prerender produce su placeholder. El texto fijo del home (hero, pilares, cómo funciona, pestañas, preguntas sin datos) sí se prerenderiza.
- **No se hornean precios ni datos de soporte en el build.** Cambiarían sin redesplegar y quedarían desactualizados.
- Los placeholders de `@defer` reservan su espacio con **las medidas reales** de las secciones, tomadas en la matriz de 16 tamaños de F3/F4. No se inventa una altura. El criterio de CLS lo verifica.

**R4 · El código tiene que funcionar en el servidor.**
- Se inventaría todo acceso a `window`, `document`, `localStorage`, `IntersectionObserver`, GSAP, `matchMedia` y `location` en lo que carga el home: `PlatformHostService`, `prepareMarketingHome`, MarketingNav, el scrollspy, el hero y el loader de fuentes.
- Cada uno se protege con `isPlatformBrowser` o se mueve a `afterNextRender`, sin cambiar su comportamiento en el navegador.
- **El prerender debe dar el mismo HTML que el primer render del cliente.** Hay que vigilar `PlatformHostService` durante el prerender: el host del build no existe, así que se fuerza el **modo marketing** solo para la ruta `/` del build de marketing. Nunca debe quedar en modo tenant.
- En consola: cero errores `NG05xx` de hidratación (mismatch).

**R5 · El alcance no sale de `/`.**
- Las verticales y `/negocios` siguen con sus funciones. Llevan datos editables desde la plataforma.
- Las rutas legales se pueden agregar después si `/` cumple. No en este paso.

### 3.3 Herramientas
- `@angular/ssr` en la **misma versión de Angular (21.0.6)**, solo para prerender en build (`outputMode: 'static'`, `prerender` con `routes: ['/']`), **sin servidor SSR ni Express en producción**.
- Solo en la configuración `marketing`. `application` no cambia.
- `ng add @angular/ssr` modifica varios archivos. Codex aplica solo lo necesario para el prerender estático y documenta cada archivo tocado.

### 3.4 Pruebas y criterio de cierre
- **Build:**
  - `dist/marketing/browser/index.html` tiene el h1 «La agenda en línea para salones, barberías y spas.», el lead, los CTA y los chips **en el HTML**;
  - `index.csr.html` tiene un `<app-root>` vacío;
  - `functions/template/index.html` es igual a `index.csr.html`;
  - `check:public-builds` extendido con todo lo anterior.
- **Hosting simulado** (el preview de `scripts/performance/public-preview.mjs`, extendido con la misma tabla de rewrites):
  - `/` → home prerenderizado;
  - `/acceso`, `/privacidad` y una ruta inexistente → CSR;
  - `/e/nail-art/servicios` → igual que antes de M3.
- **Hidratación:** 0 errores de hidratación o `NG05xx` en consola en `/` con 375, 820 y 1440, y GSAP y el scrollspy funcionando como en F4.
- **Rendimiento:** el mismo orquestador de M0 y el mismo perfil, con una serie alternada M2 frente a M3 de 5 + 5.
  - Meta: **mediana de M3 ≥ 85**, A/BP/SEO 100 y CLS ≤ 0.01 en todas las muestras.
  - Con el home prerenderizado, el **LCP debe ser el h1 o el lead**.
- **Regresión:** se repiten la matriz de 16 tamaños del home y el P2 del tenant (las 30 muestras del artefacto anterior) **sin empeorar**.
- **Suite:** `npm test` en verde con la configuración normal (T1), más las pruebas de `functions` (11) y del preview.
- Si con M3 la mediana se queda por debajo de 85, se reportan las cifras y **el usuario decide** si se acepta el valor. No se hacen más microajustes.

## 4. Orden
1. T1: suite verde 3 veces con la configuración normal.
2. Commit de M1, M2 y CLS, más T1 (autorizado).
3. Comprobación de solo lectura de R2: que no haya dominios propios conectados. Si los hay, se detiene y se pregunta.
4. M3 en este orden: R1 → R4 → R3 → prerender → medición. Cada paso con su respuesta y su revisión de Claude.
5. Sin despliegue, sin `firebase deploy` y sin crear sitios de Hosting sin autorización explícita.
