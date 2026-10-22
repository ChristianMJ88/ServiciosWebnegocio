# Revisión de código: retorno a CSR decidido por usuario

Lee PROTOCOLO y revisión 008-f4-m3-p-retorno-csr. Codex implementó únicamente fragmentos de config/build/guardas/pruebas; 012 se conserva. Revisa sin modificar ni ejecutar nada.

Archivos: angular.json, package.json, scripts/build-marketing.mjs, build-marketing-hydration-probe.mjs, check-marketing-csr.mjs, check-public-builds.cjs/.test.mjs, marketing-hydration-wiring.test.mjs, marketing-origins.test.mjs, prepare-clientes-site.test.mjs, performance/csr-hydration-probe.mjs y README.

Desviación comprobada respecto al diseño de reversión: Angular 21 emite prerendered-routes.json {routes:{}} aun sin server/outputMode; primer build20 falló correctamente. Guarda nueva acepta ausencia o exactamente {routes:{}}, rechaza cualquier ruta/otra estructura. No se borra ese archivo para fingir CSR. Server/ sigue rechazado. Shell manifiesto explícito csr. Diagnóstico no puede contener, estar dentro ni coincidir con dist/marketing. 39 focales previas verdes; agregado caso CSR con metadata vacía; build20 completo en curso, luego Node/Angular y browser CSR. No solicitar una serie Lighthouse nueva: el usuario eligió CSR.

Comprueba seguridad de guardas/coherencia con012 y que no haya regresiones introducidas; señala bloqueantes reales y pruebas faltantes. No revisar cambios ajenos AGENTS/revisiones005/006/007/plan. No commit, despliegue ni sitios.
