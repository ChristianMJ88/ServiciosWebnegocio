# Verificación final 006 fase 1 tras revisión

Claude, solo lectura con Read/Glob/Grep. Lee revisiones/006-cierre.md y comprueba las correcciones actuales de M1, B1 y B2. No ejecutar suites ni escribir aplicación. Emite dictamen acotado para commit; no certifiques pruebas.

- M1: sobrecarga de confirmar acepta origen/actor; ruta estándar WEBHOOK/null, resolutor ADMIN/usuario. Prueba del resolutor verifica la llamada con actor 9; prueba de confirmador publica dos CitaConfirmada con ADMIN/9 y mismos avisos.
- B1: historial de reprogramación usa actorId y motivo de Recepción cuando corresponde; prueba de ambos orígenes lo verifica.
- B2: arquitectura ahora recorre todo src/main y restringe tipos ServicioOutboxCorreoCitas/WhatsappCitas a la lista cerrada de listener, adaptador de registro, programador y servicios de outbox.
- M2: se elige tu recomendación dentro del alcance autorizado «sin cambio funcional»: no publicar todavía la confirmación de Staff ni añadir avisos/banderas. Se agrega regresión que conserva el historial y verifica ausencia de eventos/outbox de esa confirmación. No requiere decisión comercial ahora porque no se habilita ninguna conducta nueva. Cierres Staff sí publican y notifican. Queda documentado para futura autorización comercial, sin bloquear el resto de fase 1.
- B3: enum interno documenta ADMIN como operación del personal/Caja, no rol/permisos. WEBHOOK representa hecho confirmado por proveedor; iniciador Point permanece en venta enlazada. No se amplía enum aprobado ni se cambia RBAC.
- B4: git HEAD previo tenía consulta findByClienteIdAndEmpresaId dentro del for del grupo, misma N que ahora; no añadimos consultas. PublicadorPagosCitas usa IN por venta; no cambia precios/saldos.
- Resolución de incidencias mueve horarios y conserva solo confirmación/notificaciones existentes; no se añadió evento reprogramada que generaría otros avisos. Reembolsos/custodia fuera de fase 1.

Sigue prohibido implementar 007, polling, SSE, migración o cambio de API. Suites completas en ejecución por Codex. Cambios de revisiones/005.md y 007.md son ajenos y se conservarán fuera del commit 006. No sobrescribir ninguna revisión anterior.
