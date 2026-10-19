# Propuesta: rediseño del home de Fluora y navegación responsiva

- Autor: Claude Code (arquitectura y revisión). Solo propuesta: no se modificó código, estilos, rutas ni datos.
- Fecha: 2026-10-16. Rama observada: `codex/refactor-roles-permisos`.
- Alcance: home de marketing (`FluoraHomeComponent`, ruta `''` sin tenant), `HeaderComponent` y `FooterComponent` en modo Fluora, y `index.html`.
- Fuera de alcance: minisitios de tenant (`/e/:slug`, hosts propios), paneles internos, landings verticales (solo se enlazan) y backend.
- Prototipo visual (móvil interactivo, página móvil completa, tablet con menú lateral y escritorio): https://claude.ai/artifact/9MCLnAF2HACfZFFE8cygPY

## 1. Enfoque del negocio que no debe perderse

Fluora vende una agenda de citas para negocios de servicios. Incluye un minisitio con reservas, el equipo coordinado sin cruces y recepción, caja y CRM conectados. La página tiene que llevar a la persona a **registrarse con el plan recomendado** (`recommendedRegistrationHref()`) y, después, a **entrar a la app** o **encontrar un negocio** (directorio). Todo lo demás está para sostener esas tres acciones.

Contratos que se conservan sin cambios: planes vía `PublicPlansService` (nombre, precio, días de prueba y de gracia, módulos y límites vienen del backend; el front no calcula nada), `registroPlanHref(plan.id)`, `platformHost.appUrl/marketingUrl`, `MarketingTrackingService` (`signup_click` en landings), SEO (`SeoService.applyMarketing`, title/meta, JSON-LD de `index.html`), la redirección a `/acceso` en el host de app, `PublicDiscoveryComponent` y los enlaces legales.

## 2. Diagnóstico (con evidencia)

| # | Prioridad | Hallazgo | Evidencia |
|---|---|---|---|
| H1 | Alta | La navegación de escritorio no cabe: a 1440 px, «Cómo funciona» e «Iniciar sesión» se parten en dos líneas. Son 6 enlaces, más el acceso y el CTA. | Captura 1440×900. `header.component.html:79-96`, enlace de acceso en `:127-134`, CTA en `:153-172` |
| H2 | Alta | El menú móvil de Fluora es un desplegable sin `max-height` ni scroll interno. Son 8 elementos apilados y, en horizontal o en pantallas bajas, se sale de la pantalla. El body no se bloquea mientras está abierto. El menú del tenant sí tiene `max-height: calc(100dvh …)`. | `header.component.css:307-325` (Fluora) frente a `:777-781` (tenant) |
| H3 | Alta | Página excesivamente larga y repetitiva: 13 bloques. «Cómo funciona» aparece dos veces (journey y workflow). Lo que hace el producto se cuenta cuatro veces: painPoints, pillars, capabilities y featureGroups. En móvil, el hero solo ocupa unas 3 pantallas (4 signals + 3 highlights + la animación). | `fluora-home.component.html:17-37, 110-287`; `.refluora-hero__signals` pasa a una columna en `fluora-home.component.css:155` |
| H4 | Alta | La CSS del home tiene 4 capas que se pisan: clara (l.1), oscura (l.165), clara otra vez (l.290-560) y un parche `:host` oscuro (l.651+). El resultado depende del orden y de la especificidad. Es la causa más probable de regresiones visuales. | `.refluora-page` definido en l.1 y l.165; `.refluora-hero` en l.195 y l.290; `:host .section-head h2 {color:#fff}` en l.664 para corregir `color:#18242d` de l.483 |
| H5 | Media | Texto ilegible: tamaños de 0.42–0.5 rem (6.7–8 px) en la demo del hero, el chat y el sync-rail. Incumple las pautas de legibilidad y de zoom en móvil. | `fluora-home.component.css:130, 136, 865, 875, 882, 1032` |
| H6 | Media | Tipografía inconsistente. `index.html` carga Playfair y Poppins, pero la app usa Inter y Plus Jakarta Sans, que no se cargan (se ve una fuente de sistema). Animate.css se carga en todo el sitio y bloquea el render. | `index.html:25, 27`; `app.ts` en `resolverFuenteCss` |
| H7 | Media | Las anclas no coinciden: «Funciones» del header apunta a `#beneficios` («Para cada etapa»), pero el producto está en `#producto`. El footer sí usa `#producto`. | `header.component.ts:59`, `header.component.html:89`, `footer.component.ts:29` |
| H8 | Media | Errores de copy: el encabezado dice «Cuatro áreas…» pero se muestran 3 grupos. «Empieza… Crece sin cambiar de sistema» se repite en la franja de conversión y en Planes. | `fluora-home.component.html:271, 78, 293` |
| H9 | Media | Landmarks: hay un `<main>` del home dentro del `<main>` de `app.html`, o sea, dos `main` anidados. | `fluora-home.component.html:1`, `app.html:6` |
| H10 | Baja | Falta `viewport-fit=cover` y `theme-color`, que son necesarios para la sensación de app nativa (safe areas, color de la barra del navegador). | `index.html:20` |
| H11 | Baja | `@HostListener('window:scroll')` dispara detección de cambios en cada evento de scroll. Además, el header comparte una sola plantilla y unas 850 líneas de CSS entre Fluora y el tenant, con muchos modificadores (`--over-hero`, `--hero-integrated`, `--detached`, `--menu-detached`, `--solid`). | `header.component.ts:85-88`, `header.component.html:1-9` |

## 3. Arquitectura recomendada

1. **Separar la navegación de marketing de la del tenant** (prioridad máxima para no romper nada):
   - Nuevo `MarketingNavComponent` (standalone, `OnPush`) que solo se usa cuando `!tenantActivo()`. `HeaderComponent` queda intacto para el tenant.
   - En `app.html`: `@if (tenantContext.activo()) { <app-header> } @else { <app-marketing-nav> }`. Es el mismo criterio que hoy usa `site-header--tenant`.
   - Las URLs se reutilizan tal cual (`platformHost.marketingUrl/appUrl`, `registerHref`, tracking).
2. **Dividir el home en componentes por sección** dentro de `pages/fluora-home/sections/`: `hero`, `verticals-strip`, `how-it-works`, `product-tabs`, `pricing`, `faq`, `final-cta`. El contenido (arrays de copy) va a un `fluora-home.content.ts`. `FluoraHomeComponent` queda como orquestador de datos (planes, SEO).
3. **Una sola hoja de tokens** para el marketing (`shared/styles/marketing-tokens.css` o `:host` del home) con variables `--fl-bg`, `--fl-surface`, `--fl-border`, `--fl-text`, `--fl-muted`, `--fl-action`, `--fl-radius-*` y `--fl-space-*`. Se eliminan las capas clara y `:host` de `fluora-home.component.css`. Meta: que el CSS del home baje de unas 1180 líneas a unas 400, repartidas por sección.
4. **Rendimiento**: `@defer (on viewport)` para product-tabs, pricing, faq y `app-public-discovery`. GSAP solo en el hero, con import dinámico dentro de `afterNextRender`, y nada en móvil o con `prefers-reduced-motion`. `NgOptimizedImage` con `priority` solo en la imagen del hero, si la hay. Quitar Animate.css del `index.html` global, después de comprobar con grep que ningún componente lo usa.

## 4. Nueva arquitectura de información (de 13 a 7 bloques)

| Bloque | Reúne | Id de ancla |
|---|---|---|
| 1. Hero | Hero, franja de conversión y highlights. Un titular, un párrafo, 2 CTA, una línea de confianza y una sola tarjeta de agenda animada | `#inicio` |
| 2. Giros | Chips hacia `/barberias`, `/salones`, `/esteticas`, `/unas`, `/spa` (ya existen) | — |
| 3. Cómo funciona | Journey y workflow en 3 pasos | `#como-funciona` |
| 4. Producto (pestañas) | Demo del minisitio, pillars, featureGroups, WhatsApp y painPoints. Pestañas: Minisitio · Agenda · WhatsApp · Caja/CRM | `#producto` |
| 5. Planes | Igual que hoy (datos del backend, prueba y nota de consumos) | `#planes` |
| 6. Preguntas | Acordeón (`<details>/<summary>` nativo o botones con `aria-expanded`) | `#faq` |
| 7. CTA final + directorio | Final CTA y la entrada a `PublicDiscovery` | — |

Mantener `#beneficios` y `#demostracion` como anclas alias (un `<span id>` vacío) para no romper enlaces externos o compartidos.

## 5. Navegación por dispositivo

| Ancho | Patrón |
|---|---|
| ≥1200 px | Barra fija (72 px): logo · Producto · Cómo funciona · Planes · Negocios · Preguntas · «Iniciar sesión» (texto) · CTA. Se quita «Inicio», porque el logo ya lleva al inicio. `white-space: nowrap`. Scrollspy con `IntersectionObserver` que marca `aria-current`. |
| 768–1199 px (tablet/iPad) | Logo + CTA + botón de menú, que abre un **drawer lateral derecho** (380 px, `100dvh`, scroll interno, foco atrapado, Esc y backdrop para cerrar, body bloqueado). Dentro: secciones con icono, chips de giro, «Iniciar sesión» y CTA. |
| <768 px (móvil, tipo app nativa) | **App bar superior** de 56–60 px: logo y «Entrar». **Barra inferior de pestañas** fija: Inicio · Producto · **Probar** (central, destacado) · Planes · Más. «Más» abre una **hoja inferior** (bottom sheet) con Cómo funciona, Directorio, Preguntas, giros, «Iniciar sesión» y CTA. Usa `env(safe-area-inset-bottom)`, objetivos de 48 px y `scroll-margin-top` en las anclas. |

Detalles de implementación:
- Se puede usar `@angular/cdk` (Overlay/FocusTrap/BlockScrollStrategy), que ya viene con Angular Material en `styles.css`. Si no, `<dialog>` nativo con `showModal()` da foco atrapado y Esc sin dependencias.
- La barra inferior solo se muestra en el home y las landings de Fluora, nunca en el tenant. Hay que comprobar que no choque con el botón de WhatsApp ni con el chatbot: esos solo existen con tenant activo (`app.html:16, 24`), así que hoy no hay conflicto.
- Ocultar la barra inferior mientras el teclado está abierto, aunque en este home no hay inputs.
- Scroll: listener `passive` fuera de la zona de Angular que actualiza un signal **solo al cruzar el umbral** (sustituye a H11).

## 6. Sistema visual

- Se conserva la identidad actual: lienzo oscuro `#0A0F1A`, superficies `#121A2B`/`#182238` y un solo color de acción, `#00F5A0` con texto `#04130E`. El acento azul `#5B8CFF` queda solo para los estados de agenda.
- Tipografía: **Plus Jakarta Sans** (títulos, 700–800) y **Manrope** (texto, 400–700). Ambas existen ya en `resolverFuenteCss`. Se cargan con `<link rel="preconnect">` y `display=swap`, y se quitan Playfair y Poppins del `index.html` si nada más las usa.
- Escala de texto: mínimo 12 px en etiquetas y 14–16 px en el cuerpo en móvil. `h1` con `clamp(2.4rem, 4.6vw, 4.25rem)` y `h2` con `clamp(1.75rem, 3.4vw, 3rem)`.
- Espaciado vertical por sección: `clamp(40px, 8vw, 96px)`. Contenedor `max-width: 1200px`. Gutter de 20 px en móvil.
- Contraste: el texto secundario `#A7B3C6` sobre `#0A0F1A` pasa AA. Hay que evitar `#64748B` en texto que no sea decorativo.

## 7. Plan por fases (para Codex, cada una con su propia revisión)

1. **F1 Fundaciones, sin cambio visual grande**: tokens, fuentes, `viewport-fit=cover`, `theme-color`, corregir `<main>` anidado (H9), anclas (H7) y copy (H8).
2. **F2 Navegación**: `MarketingNavComponent` con tres variantes (escritorio, drawer y barra inferior con hoja). `HeaderComponent` del tenant sin cambios.
3. **F3 Home 7 bloques**: componentes por sección, eliminación de capas CSS (H4), tamaños mínimos (H5) y `@defer`.
4. **F4 Pulido**: scrollspy, microanimaciones solo en el hero y medición de rendimiento.

## 8. Criterios de aceptación y validación propuesta

- Sin desbordamiento horizontal ni texto cortado en 360×740, 375×812, 390×844, 768×1024, 820×1180, 1024×1366 (iPad Pro), 1280×800 y 1440×900, en vertical y horizontal (`document.documentElement.scrollWidth === innerWidth`).
- Ningún enlace del header de escritorio se parte en dos líneas a 1200 px o más.
- En móvil, el hero (titular y CTA principal) se ve completo en la primera pantalla de 375×667. Altura total de la página en móvil: ≤ 7 pantallas de 812 px (hoy son unas 20 o más).
- El menú lateral y la hoja inferior atrapan el foco, cierran con Esc y backdrop, bloquean el scroll del body y devuelven el foco al disparador. Los objetivos táctiles miden 44 px o más.
- Ningún `font-size` menor de 12 px en el home (comprobar con grep `font-size: \.[0-6][0-9]*rem`).
- Los tenants no cambian: snapshot visual de `/e/:slug` y de un host propio antes y después. `app.routes.separation.spec.ts` y las specs del header y del home siguen pasando.
- Lighthouse móvil: Performance ≥ 85, Accessibility ≥ 95, CLS < 0.1. Sin errores de consola.
- Los CTA conservan `href` y tracking: registro con `?plan=<id>` del plan destacado, `signup_click` en landings y acceso a `appUrl('/acceso')`.
- Pruebas: `ng test` completo; nuevas specs para `MarketingNavComponent` (abrir/cerrar, Esc, `aria-expanded`, enlaces generados por `PlatformHostService`) y para que las pestañas de producto sean accesibles (`role=tab`, `aria-selected`).

## 9. Límites de esta revisión

- Las capturas se hicieron con Chrome headless contra `localhost:4200`. La vista a 1440×900 es fiable. Las vistas móviles no lo son porque Chrome headless no baja de unos 500 px de ancho, así que el diagnóstico móvil se basa en la lectura de la CSS. Codex debe validar con DevTools en modo dispositivo.
- Los precios y nombres de los planes del prototipo son marcadores (`[precio]`, `[Plan…]`), porque en producción vienen del backend.
