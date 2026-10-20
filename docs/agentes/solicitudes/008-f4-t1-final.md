# T1: solución final por función pura (opción a aprobada)

La opción b con template mínimo falló aún bajo carga: normal1, 301 aprobadas/1 timeout Admin, 82/83 archivos, 41.27s. Legal pasó y no hubo error TestBed entre archivos. Evidencia /private/tmp/fluora-t1-normal-1.log. No subir tiempos ni reducir workers.

Aplico opción a de tu diseño: `seleccionarModuloActivo` exportada de admin-dashboard.config.ts, usada realmente por el computed en AdminDashboardComponent. Tres pruebas puras en config.spec.ts: lista sin capacidades/permisos→null, sección ausente→primero permitido, sección válida→mismo objeto. El spec pesado no llegó a versionarse y se retira. No se conservan las dos aserciones DOM de la opción b porque repetían template; el template real mantiene ?. y strictTemplates/build validan el tipo nullable. Regla, filtrado RBAC y backend intactos; solo extraer el mismo selector de presentación.

Se conserva isolate:true permanente sin cambios de workers/timeouts. P3 Booking/Header como revisión anterior. Solicito revisión de código de este cambio pequeño; no ejecutar pruebas/builds mientras Codex realiza tres corridas normales. Revisa también si el requisito de aligerar toda prueba >2.5s es necesario: ese umbral nuevo no es criterio original de T1, podría ampliar el alcance a todos los specs UI existentes; se documentarán 3 más lentas y márgenes reales respecto a5s, sin ocultar fallos.

No escribir código. Resultado en revisiones/008-f4-t1-final.md.
