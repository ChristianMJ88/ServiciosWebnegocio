# R3: ancla FAQ queda detrás de la medición del rail

Claude, arquitectura y revisión solo en lectura; no ejecución, .env/postman ni escritura de app. Revisar PROTOCOLO/ESTADO, FluoraHome afterEveryRender, HomePlans ViewChild/ResizeObserver, specs y MarketingSectionsDirective.

La prueba real posterior `navigation-hydration-probe` reproduce en375×812/#faq: todas las secciones cargadas y red estable, 0consola/NG05xx, pero FAQtop99.625 conbarra60 yscroll-margin78; aria-current siguePlanes. Log `/private/tmp/fluora-m3-r3-post-navigation-production.log`; diagnósticoJSON en carpeta homónima. /#planes sí alinea78.71875 y seleccionaPlanes. Antes de cambiar aplicación quiero diseño acotado.

Hipótesis de código: afterEveryRender delHome ve refs/dataReady y hace scrollIntoView, borra pendingAnchor. El ResizeObserver delrail dePlanes determina canScroll después; al crear los controles reduce el ancho delheading y su descripción gana otra línea (~21.6px), desplazando FAQ. No requiere ni debe tocar reglas/precios/capacidades/backend.

Propuesta para evaluar:
- HomePlans expone un computed de presentación layoutReady: !offer.loading() && (offer.failed() || !offer.plans().length || firstRailMeasured()).
- firstRailMeasured signal falso, se resetea al setter de rail y se pone true en el update real del ResizeObserver junto con canScroll; fallbackqueueMicrotask existente. No timers, sin adivinar dimensiones.
- Home lee viewChild<HomePlansComponent>('plansReady') en vez deElementRef (solo comprueba existencia actualmente), espera layoutReady antes de scroll y conserva cancelaciones porwheel/touch/teclado ylimpiezas. Un solo scroll cuando elrail ya tuvo su primer render con controles.
- Unitaria significativa que separa render inicial sinmedición y callbackRO, verificael mismoancla despuésdecontrols, cubreplaneserror/vacíos/fallbacksinRO; real/#faq antes/después móvil+desktop+resize.

Pedir corrección si esta propuesta no garantiza que el DOM con controles ya se renderizó al activar ready. No soluciones con sleeps/timeout o scroll repetido que secuestre al usuario. No amplíes a refactor general de rail o reglas comerciales. Si se necesita otro enfoque, detallar secuencia concreta y pruebas.

Estado:100CSSbaseline+100posterioresconmismashuellasAPI, initialnormalmax.0030109; finaltrasprogramscrollmax.02618 informativo. CSS usuarioaprobada aplicada; Node79/79/builds/guardasgreen; CSRprod7green, devfixturederivadoenverificación/realNG0913registrados. No cierreR3 ni commit/deploy; 4200/8080intactos.
