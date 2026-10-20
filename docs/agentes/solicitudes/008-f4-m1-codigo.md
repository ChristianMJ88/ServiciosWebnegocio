# 008 F4 M1 — revisión de código antes de la medición

Autor: Codex. Revisar solo lectura según PROTOCOLO y arquitectura008-f4-m0. No modificar archivos ni ampliar negocio. No iniciar M2 aún. Código sin commit posterior a checkpoints011798137b/008e642571; cambios ajenos conservados.

Implementado M1: angular.json solo configmarketing estilos, nuevas hojasmarketing/tenant/base/tokens/generadas; scripts/css/marketing-subset.mjs con PostCSS8.5.6 directo/mismaversión, package scripts y lock acotado; servicio/TOKEN no-op por defecto, provider solo main.marketing, resolver junto a todos los resolvers tenant, AppHeader/footer/overlays esperanCSS. Verificar precedencia real del link, cargas/race/error/reintento, aislamiento application/hostpropio, subconjuntos incluidas clases TS/animaciones. Hash del CSS de application idénticoM0 y src/styles.css intacto.

**Desvío razonado:** ready se evalúa **dentro** del @defer Header. Se baja el chunk en paralelo con CSS y se monta solo al ready. Evaluarlo fuera retrasaría el inicio de su descarga hasta después del link, empeorando P2. Prueba completa antes/después del ready. Suite262/79exit0; ambosbuildsexit0; marketing212554dd89383177 inicial569.89kB/147.07kB, styles42.47kB/8.95kB, tenant321.82kB/35.98kB lazy.

M0 corregido antes de códigoM1: precompresión/warmup aparte (74);51/85/83/82/87med83,100A/BP/SEO; unaCLS.014787 se declara. Script ahora acepta compare-root para alternar5porvariante en misma sesión, valida tresGET realesHome, registra bytes/huella recursiva/dirty/chunks y elimina preview enfinally. La medición M1 será después de esta revisión y las capturas. No corrí suites/builds durante M0.

Revisar tooling ajustado y M1 por defectos/omisiones. No dar equivalencia visual ni85 por adelantado. Capturas iniciales tenant están en /private/tmp/fluora-008-m0-ui; M1home tiene mismas fuentes/colores pero rects distintos por32px/animaciones, falta estabilizar/completarcomparación. Assets/checkgeneración y deriva pasan. No se tocó firebase.json, no deploy; D1cache sigue requisito antes de desplegar.

Responder prioridades, archivos, riesgo y prueba concreta, con alcance acotado. Incluye cualquier incompatibilidad que veas para el siguiente M2 application builder, sin implementarlo.
