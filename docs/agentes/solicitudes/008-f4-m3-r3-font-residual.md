# R3: residual final de fuentes, decisión de arquitectura

Claude, lectura solamente, no ejecutar pruebas, no secretos/.env. Leer PROTOCOLO y revisiones r3-cierre-parcial/r3d-mediciones y código de Nav/Hero/preview. Codex implementa. Sin ampliar alcance ni producción SSR.

La matriz final de 32 normales pasó los asserts de secciones críticas, pero detecté al resumir que faltaba una aserción global CLS<=.01. NO se declara aprobada: evidencia completa /private/tmp/fluora-m3-r3-finalcheck-normal32/71ad736b0719-1792521228833.json y resumen docs/agentes/evidencias/008-f4-m3-r3-final-normal.json. 1280x800 repetición1 initial .014889 (NAV desktop-links .012971 a226ms pasa de x1156,width67 a x376,width423; fuente/iconos cargando). 800x1280 repetición2 .013312: como-funciona sube y baja21.4px por preview/iconos (texto ancho y wrapping). Las demás<=.01; 100CSS anteriores max.003010. No descartar muestras ni relajar criterio. CSS caption aprobado usuario ya aplicado.

Solicito diagnóstico puntual y cambio mínimo de presentación recomendado para estos dos saltos, conservando datos dinámicos backend. Evitar hardcode de anchos basados en textos/contadores/oferta; no cambiar reglas/precios ni fuente global ni agregar preloads arbitrarios. ¿Son métricas de bootstrap-icons y los i sin caja estable? Revisar CSS/HTML real y eventos, indicar prueba de reproducción determinista con fuentes retenidas y global assertion en matriz. No inventar ancho. El font-hydration-probe actual retiene fuentes 768-1024 y no cubre escritorio1280.

Cinco suites Angular normales están ejecutándose; no comenzar otras pruebas en paralelo. Sin commit nuevo autorizado, sin deploy.
