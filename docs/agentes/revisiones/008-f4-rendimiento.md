# Revisión 008 F4: optimización tras la medición real

**Veredicto:** el alcance y el orden de rutas están bien. Hay dos riesgos que conviene medir o corregir antes de cerrar: el eager de Home también se descarga en los minisitios de tenant, y un preconnect está mal configurado. No pude correr el build ni la suite porque Bash no está disponible en esta sesión, así que no verifiqué el tamaño de los bundles.

Durante la revisión el código cambió: `main.marketing.ts` pasó a usar `withInitialMarketingHome`, y revisé esa versión.

## Lo que está bien
- **Orden de rutas:** la Home del tenant (`app.routes.marketing.ts:20-23`, con `canMatch: [tenantHostMatch]`) sigue antes de `fluoraHomeRoute` (`:57`), que no tiene `canMatch`. En un host de tenant gana la Home del tenant y en el host de plataforma cae a Fluora, igual que antes.
- **Aislamiento:** `withInitialMarketingHome` (`app.routes.marketing.ts:114-118`) devuelve un array nuevo y conserva la identidad de todas las rutas salvo una. `fluoraHomeRoute` no se modifica. Los demás entries, `app.routes.ts` vía `publicContentRoutes` y `application`, siguen en lazy. `loadComponent: undefined` junto con `component` pasa la validación del router.
- **Prueba** (`app.routes.separation.spec.ts:21-29`): cubre la identidad, la posición, que `loadComponent` se conserva en la ruta compartida y que la Home del tenant va antes. Es suficiente.
- **Fuentes:** los alias `Fluora Marketing *` (`marketing-tokens.css:2-3`) no tocan las familias que eligen los tenants. Los preloads coinciden en URL, tipo y `crossorigin`. Animate y Google Fonts se conservan, y `index.app.html` no cambia.
- No hay cambios de backend, tenant, permisos ni precios.

## Hallazgos

**P1. El eager de Home entra también al bundle inicial de los minisitios de tenant.**
- **Evidencia:** el build `marketing` (`angular.json:75-80`) sirve `publicContentRoutes`, que incluye los tenants por host y `e/:slug`. `app.routes.application` excluye `e/:slug`. Entonces `FluoraHomeComponent`, junto con hero, pillars, workflow y los servicios de marketing (`fluora-home.component.ts:1-20`), ahora lo descarga y parsea todo visitante de un minisitio, aunque nunca lo renderice. El viaje de red que se ahorra en refluora.com se paga en LCP/TBT de los tenants.
- **Validación:** medir el delta del JS inicial (`main-*.js`, en KB transferidos) antes y después. Además, correr Lighthouse con la misma configuración sobre una Home de tenant, por host y por `/e/:slug`.
- **Si el tenant empeora:** en lugar del import estático, iniciar el chunk en paralelo desde `main.marketing.ts` solo cuando el host sea de plataforma. Por ejemplo, ejecutar `import('./app/pages/fluora-home/fluora-home.component')` antes de `bootstrapApplication` y dejar la ruta en lazy. El router reutiliza la promesa del módulo, se adelanta el fetch y los tenants no cargan nada extra.

**P2. El preconnect a cdnjs con `crossorigin` no se usa** (`src/index.html:28`).
- **Evidencia:** `animate.min.css` (`:32`) se pide sin CORS, y Chrome usa pools de conexión separados para peticiones con y sin CORS. Por eso esa conexión precalentada se desperdicia. Lo mismo aplica a `fonts.googleapis.com` (`:26`): ese preconnect sí está bien configurado, pero el stylesheet se descubre en la línea 30, casi al mismo tiempo, así que aporta muy poco. Lighthouse además advierte cuando hay más de 2 preconnects, y aquí hay 3.
- **Recomendación:** dejar solo `fonts.gstatic.com crossorigin` (`:27`), que es el único origen que se descubre tarde. Si se conserva cdnjs, quitarle el `crossorigin`.
- **Validación:** en el waterfall, que no aparezca una conexión duplicada a cdnjs y que desaparezca la advertencia de `uses-rel-preconnect`.

**P2. Los preloads de las fuentes de marketing también se aplican a los tenants** (`index.html:24-25`).
- **Evidencia:** el mismo `index.html` del build marketing sirve los minisitios, que no usan esas fuentes. Son dos WOFF2 de prioridad alta compitiendo con el LCP del tenant, y aparecerá el warning de consola "preloaded but not used". Es un warning, no un error, así que no afecta el puntaje de BP.
- **Recomendación:** documentar el costo con el tamaño de los dos archivos y medirlo en la misma corrida de tenant que el P1. Si resulta relevante, se resuelve junto con la carga condicional por host.

**P3. Metodología de medición.**
- **Evidencia:** la dispersión entre 65 y 44 en incógnito es demasiado alta para decidir con una sola corrida.
- **Recomendación:** reportar la mediana y el rango de al menos 5 corridas por URL (plataforma y tenant), con el mismo perfil y el equipo en reposo, junto con LCP, TBT y JS inicial. Hacerlo para el antes y el después. Me parece correcto dejar registrados los resultados bajos y la meta de Perf 85 como no cumplida.

**P3. Estilo del spec.** El nuevo `it` (`spec.ts:21-29`) junta varias sentencias por línea, a diferencia del resto del archivo. Es cosmético.

## Para cerrar F4
1. El build marketing sin errores de budget (`angular.json:36-41`) y el delta de JS inicial reportado.
2. La suite completa en verde, incluido `app.routes.separation.spec.ts`.
3. La medición sobre una Home de tenant que muestre que P1 y los preloads no la empeoran; si la empeoran, aplicar la alternativa de P1.
4. Corregir o justificar el preconnect a cdnjs.
