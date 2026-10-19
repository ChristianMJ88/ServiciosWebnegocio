# 008 F4 — Header diferido implementado

Autor: Codex. Revisión solo lectura, sin editar aplicación ni otras revisiones.

Se implementó P1 de `008-f4-carga-shell`: `app.html` conserva la decisión existente y monta Header dentro de `@defer(on immediate)` en la rama no marketing. Placeholder div vacío aria-hidden, sin altura, loading, mínimo ni prefetch. HeaderComponent, sus estilos y contratos, App.ts, Footer y decisiones de host/contexto no cambian. No backend, permisos, precio, migración ni datos de negocio.

Tests App con DeferBlockBehavior.Manual: Home no tiene bloque ni Header; `/e/x` y host propio no muestran marketing/footer ajeno mientras esperan, y el bloque completo monta Header; auth/panel sin chrome. Se mantiene montaje del footer con contexto. Suite completa254/78, exit0, configuración temporal de un worker, sin ampliar timeouts.

Build marketing876238215e39f020 exit0: inicial892.05kB / estimado181.59kB (antes928.86/186.43), nuevo chunk Header37.23kB/6.31kB. App build en curso; métricas y navegación real en curso, no se afirma Performance85. Stats ignorado trasladado fuera de browser. La prueba Playfair en editor dev confirmó familia y aspecto en sus vistas previas de390/820/1440; se restauró Inter y recargó sin guardar. Booking fadeIn/fadeInUp encapsulados 1s/both, sin overflow real375/820/1440; no se crearon reservas ni se llenaron datos.

Confirma aislamiento real del chunk, ciclo de vida/compuertas y pruebas. Advierte riesgos antes de dar por cerrada F4; no aprobar meta85 por anticipado.
