# CLS tenant — dictamen acotado de arquitectura

Claude: solo lectura, NO leer traces/JSON grandes ni todo docs/ESTADO. La primera llamada de diseño se interrumpió tras13min sin respuesta. Leer SOLO: src/app/app.ts, app.html, app.css, app.spec.ts, components/header/header.component.ts/html y core/styles/tenant-public-styles.ts. Puedes buscar líneas específicas de header.component.css. Entregar un diseño concreto y pruebas en menos de1200palabras. No ejecutar ni modificar aplicación.

Usuario autorizó corregir CLS tenant primero; Codex implementa tras dictamen. Mantener branding/diseño/backend/RBAC/multitenant, Header diferido y ausente deHomeMarketing, outlet vivo para resolver. SinM3/hosting/commit. No alturas de placeholder adivinadas.

Evidencia YA extraída: M0antesM1/M2 también fallaCLS: frío/caliente390 .08526/0;820 .061335/.061449;1440 .220519/.220617. M2: .081198/.081668,.060761/.061449,.220519/.220519. Trace main y0→72/74; filmstrip primeroHeaderFluora y después tenant. No hace falta releer medios/JSON/imagenes. backend4200/8080 activos conservados.

Fuente: App inicializa publicPath desdeLocation.path pero sincronizarTenantConRuta(this.router.url) usa'/' antes de primera navegación y puede limpiar contexto precargado. Header inicializa rutaActual desdeRouter.url'/' y puede calcularmodoFluoraHomeantesdeNavigationEnd. App montaHeader con styles.ready sin exigir tenantactivo; main se pinta antesdelHeader conmin-vh100.

Propuesta a evaluar (no obligatoria): corregir URL inicial y preservar contexto de slug; gate visual para contenido/footer/overlays hasta tenant+CSS+Header montado. Mantener router-outlet vivo mediantevisibility:hidden, NO@if destruyendooutlet. Query de Header por ref localSTRING/readElementRef evita referencia de clase que rompa defer. Fallback accesible en@error para que gate no deje invisibleelcontenido. Estado derivado/no flags de negocio/no sizing supuesto. Validar qué ciclo usar para revelar: queryready/afterNextRender y si hay caso de timeout necesario. Si otra solución más segura/minimal, concretarla.

Exigir pruebas: CSS/tenant/Header retrasados, error de chunk sinbloqueo, inicialRouter.url'/' conLocationreal slug/host, contextoprecargado, HomeMarketingningúnHeader/deferCSS, auth/panel sinchrome, navegaciónentretenantconservaHeaderinstancia ysalidaamarketing. MedirCLSfrío/caliente390/820/1440después, UIhome/servicios/booking/checkout. Dictamen técnico; no aprobar métricas anticipadamente.
