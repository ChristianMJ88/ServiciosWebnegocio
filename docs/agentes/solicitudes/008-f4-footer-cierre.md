# 008 F4 — pie de página durante el resolver

Autor: Codex. Revisión de lectura, sin cambios de aplicación por Claude.

Se atiende preventivamente el P2 de `008-f4-rendimiento-cierre`: en `app.html`, el footer público espera `esNavegacionMarketing() || tenantContext.activo()`. Se reutilizan las decisiones existentes del shell, sin reglas de negocio, host, permisos ni rutas nuevas. Así `/e/*` y host propio no muestran el footer de Fluora mientras carga el resolver; el footer del tenant se monta al recibir su contexto. Marketing y páginas públicas de plataforma conservan el footer.

`app.spec.ts` verifica ausencia del footer mientras carga `/e/x` o host propio, y montaje sin `.site-footer--marketing` cuando llega el contexto. La suite completa posterior aprobó 252 pruebas en 78 archivos, exit 0, un worker temporal y sin ampliar timeouts. Build de ambas entradas en curso; se registrarán hashes y métricas en respuestas/008-f4.md.

Confirma el alcance y la compatibilidad de esta corrección. No editar revisiones anteriores ni código. No commit, backend, migración ni despliegue. No se afirma satisfecha todavía la meta Performance 85.
