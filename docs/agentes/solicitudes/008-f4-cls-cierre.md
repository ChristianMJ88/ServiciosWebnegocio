# Cierre acotado CLS tenant

Claude, revisión solo lectura; no ejecutar comandos ni modificar código. Leer `docs/agentes/respuestas/008-f4-cls.md` completo, `revisiones/008-f4-cls-correcciones.md` y `docs/agentes/ESTADO.md` (entrada actual de CLS). Revisar únicamente las evidencias de esta entrega; no ampliar a M3/hosting ni cambios de negocio. Application en App/Header y sus specs, medidor tenant y modo baseline/traces son el alcance.

Comprueba que las cifras se atribuyan a artefactos y perfiles correctos, que se declaren caché/D1 solo simulado y número de muestras, que LCP final no sea el mensaje de espera, y que no se afirme cerrado HomeMarketing85 ni se confunda con Inicio del negocio. Verifica límites de UI/host propio y regresiones, Header lazy y que no haya una promesa de rendimiento basada en una sola pareja.

Los datos finales ya están en la respuesta, sección «Cierre de mediciones del artefacto aeab9d». También son alcance Booking/Calendar y sus pruebas: lee `revisiones/008-f4-cls-precarga-confirmacion.md`, que ya cierra el código de A/B y calendario. No hay medición en curso. Treinta muestras delSHAaeab9d…:24sin parámetros<.001 y6precargadas<.01 (máximo.005125707 en detalle de carrito desktop). No decir que las30quedan<.001, ni CLS0 absoluto. Angular301/83, tresbuilds y Functions11/preview3 verdes. Fuente compacta de las cifras: `/private/tmp/fluora-cls/verified-analysis.log`; no necesitas leer traces grandes. Puedes cotejar campos puntuales de `tmp/lighthouse/cls-verified-*/summary.json` con Grep sin cargar archivos enteros.

Hay un límite explícito de captura nativa IAB blanca tras resize; se descartó y se usó la captura cruda de Lighthouse, inspeccionada por Codex. No afirmar certificación física/iOS ni equivalencia total visual.

No apruebes lo que no tenga evidencia y señala cualquier discrepancia concreta. No hace falta volver a leer archivos ajenos ni credenciales. Devuelve veredicto, hallazgos necesarios y pendientes reales, de forma breve. No convertir P3 opcionales en cambios nuevos sin un defecto concreto, ni abrirM3/hosting/backend/commit.
