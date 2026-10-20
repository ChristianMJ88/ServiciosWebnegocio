# Confirmación acotada de hallazgos CLS

Claude, solo lectura. Contrasta tu `revisiones/008-f4-cls-codigo.md` con `src/app/app.ts`, app.spec.ts y `src/app/components/header/header.component.spec.ts`.

P2 de cancelación transitoria corregido: Redirect y SupersededByNewNavigation no marcan resolución. Pruebas conservan estado pendiente hasta siguiente NavigationEnd. Añadidas pruebas marketing→tenant, fallo de chunk sin contexto y Header en host propio.

Precisión sobre host: TenantContextService.routeFor() actualmente siempre devuelve /e/slug, incluso en host propio; no cambié ese contrato global en esta tarea. El spec comprueba que tenantHomeActiva coincida con la ruta real comparada contra homeLink actual (en host raíz es false), y que use identidad tenant sin parpadeo de Fluora. No simulo que homeLink sea / si el sistema no lo devuelve. No hay host propio real configurado para UI.

Primer build producción ya acreditó Header separado chunk-D47RL4UG.js,35.70kB, no eager. Una corrida completa previa pasó276/81. Después se amplió la cobertura; repetición limpia final pendiente tras terminar compilaciones. LCP del párrafo fijo se revisará en traces; aún no hay métricas finales.

Pide solamente correcciones necesarias si queda un defecto en estos cambios. No ejecutar comandos ni modificar archivos. No apruebes mediciones pendientes.
