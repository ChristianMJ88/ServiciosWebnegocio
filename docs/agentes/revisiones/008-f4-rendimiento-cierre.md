## Revisión 008 F4: la recomendación es aprobar. El aislamiento, el bootstrap y el alcance se cumplen

Solo leí archivos: no edité, no hice commits ni toqué el backend. **No hay hallazgos P0 ni P1.** Queda un punto P2 para verificar en la auditoría de Chrome.

### Aislamiento
- **Archivos compartidos sin cambios:**
  - `src/main.ts` y `src/main.application.ts` no tienen el initializer.
  - El único `import()` de Home sigue siendo el `loadComponent` de `app.routes.marketing.ts:52`.
  - No hay un import directo de Home ni un helper que reemplace rutas.
  - Las rutas con `tenantHostMatch`, los resolvers y `/e/:slug` siguen como estaban.
- **Una sola decisión de host:** `marketing-bootstrap.ts:8` usa `isCustomTenantHost()` y `isAppHost()` de `PlatformHostService`. No repite ninguna comprobación de host. Con eso quedan fuera de la precarga:
  - el dominio propio de un tenant, que en `/` coincide con el `''` de `tenantHostMatch`;
  - el host de la app;
  - `/e/*` y las rutas verticales, porque su `pathname` no es `/`.
- **El build de tenants no lo incluye:** `build:app` usa `index.app.html` y `main.application.ts`, así que no recibe nada de esto. `build:marketing` es el único que lleva el initializer.
- **Fuentes:** `index.html:24` solo conserva el `preconnect` a `fonts.gstatic.com` con `crossorigin`. Los dos `<link>` de precarga se crean únicamente en `marketing-bootstrap.ts:9-13`.

### Bootstrap
- `provideEnvironmentInitializer` se ejecuta al crear el injector raíz, antes de la navegación inicial. El import dinámico apunta al mismo módulo que la ruta, así que el bundler lo resuelve al mismo chunk y el router lo reutiliza.
- `.catch(()=>{})` (`:15`) ignora un fallo de la precarga sin afectar el manejo de carga y errores del router.
- **Las URL de la precarga coinciden con las del CSS:** `marketing-tokens.css:2-3` usa `/fonts/marketing/{manrope,plusjakartasans}.woff2` sin hash, y lo comprobé en `dist/marketing/browser/styles.*.css`. Como además lleva `as=font`, `type=font/woff2` y `crossorigin=anonymous`, la precarga cumple los requisitos para no descargar las fuentes dos veces. Ese `dist` puede ser anterior al build que está corriendo; conviene comprobarlo otra vez cuando termine.

### Pruebas (`marketing-bootstrap.spec.ts`)
- Cubren Home más 4 exclusiones (`/e/…`, dominio propio, host de app, `/barberias`). Inyectan `DOCUMENT` y `PlatformHostService`, sin mocks de permisos ni de HTTP.
- La segunda llamada a `run()` comprueba que no se duplican los `<link>`.
- Hueco menor, no bloquea: no se verifica `load` en esa segunda llamada. En la práctica no importa, porque el initializer de entorno se ejecuta una sola vez.

### P2: verificar en Chrome incógnito, no lo introduce F4
- **Footer en `/e/:slug`:** el footer aplica `site-footer--marketing` mientras `!tenantActivo()` (`footer.component.html:1`), y esa clase usa las fuentes de marketing. El footer forma parte del shell y se pinta antes de que termine el resolver. Si al cargar `/e/:slug` el tenant todavía no está activo, el navegador podría pedir `manrope` y `plusjakartasans` aunque F4 no las precargue.
  - **Cómo validarlo:** en la pestaña Network, en un `/e/<slug>` y en un dominio propio, comprobar que no aparece ningún `fonts/marketing/*.woff2` ni el chunk de `fluora-home`.
  - Si aparecen, es un caso aparte del footer, no un fallo de F4.
- **Para la meta Perf 85 (fuera del alcance de F4):** `index.html:26` y `:28` siguen cargando la hoja de Google Fonts y animate.css desde un CDN. Ambas bloquean el renderizado en el build de marketing, también en el `/e/*` servido desde ahí. Coincido en no dar la meta por cumplida mientras sigan esas dos hojas.

### Validación propuesta para Codex
1. Esperar a que terminen la suite y `npm run build`. Luego comprobar que en `dist/application/browser` no aparece `fonts/marketing` y que en `dist/marketing/browser/index.html` no hay `rel="preload"` para fuentes.
2. En Chrome incógnito, revisar Network en tres casos:
   - `/` del host de plataforma: 2 precargas de fuentes y el chunk de Home desde el inicio;
   - `/e/<slug>`: ni las fuentes ni Home;
   - un dominio propio en `/`: ni las fuentes ni Home.
3. Guardar los resultados de Lighthouse aunque salgan bajos, como indicaste.
