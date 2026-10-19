# 008 F4 — siguiente reducción mínima del inicial

Autor: Codex. Claude: arquitectura/lectura únicamente; no editar aplicación ni revisiones anteriores.

Usuario pidió continuar los pendientes de008. P1 recursos locales implementado/aprobado: 252/78 Angular, ambos builds correctos, Home Lighthouse cinco corridas 78/80/83/81/82 (mediana81), A/BP/SEO100, CLS0–.001. No commit/deploy. No cambiar backend/negocio/permisos, builder ni separar CSS global sin inventario. Datos y tenants conservados.

Diagnóstico A realizado: `dist/marketing/stats.json` ignorado fuera de browser. Inicial928.86kB, estimado186.43kB. Módulos originales de Header~52,969bytes, Footer~35,927, MarketingNav~32,659, CDK~42,364; no son tamaños minificados ni tiempoCPU y no se presentan como tal. App importa Header y Footer de forma eager aunque Home nunca muestra Header y su footer está al final. App decide el chrome con señales existentes de ruta/host/contexto; no duplicar esas decisiones.

Evalúa y define el cambio mínimo para reducir el inicial:

1. Header original solo dentro de un `@defer (on immediate)` en la rama pública que ya NO usa MarketingNav. Angular lo separaría sin descargarlo en Home; en tenant comenzaría al mostrar la rama, conservando componente/contratos/estilos. Un placeholder de presentación evitaría saltos mientras llega el chunk. Hay que considerar cuánto espacio necesita el header según sus reglas originales y no inventar una altura que cambie el layout del negocio. Tests App deben comprobar aislamiento y montaje tras cargar bloque, no asumir síncrono un componente lazy.
2. Footer: evaluar diferir todas sus referencias para que realmente salga del inicial: viewport en marketing y `when tenantContext.activo()` en tenant, siempre dentro de la compuerta existente. No diferir solo una referencia porque Angular mantendría eager la otra. Evitar flashes de marca ajena, CLS o empujar anclas; valorar si es mejor dejar Footer intacto por ahora.

Recomienda una opción concreta con archivos, condiciones de carga, placeholders y regresiones de UI (móvil/tablet/escritorio, fragmentos, host propio, navegación entre marketing/tenant/paneles). No proponer ocultar contenido, debilitar Lighthouse, cambiar decisiones de backend ni cargar Home a tenants. Si no consideras seguro ese cambio, indica una alternativa pequeña basada en el bundle; separa cambios de arquitectura mayores que necesiten otro alcance.

Mientras llega esta revisión Codex completa pruebas visuales de fuentes y animaciones. No nuevas migraciones ni datos de prueba de negocios.
