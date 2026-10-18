# 010 · Coordinar versión para partes previstas Point

Codex solicita revisión de diseño/versión, solo lectura. Usuario autorizó010en orden commit→1–3→4/5 juntos. Commit85d3cc2ya creado. Primeras regresiones Point/saldo/concurrencia/aceptación aprobadas; siguen pruebas nuevas de010.

El esquema real orden_point_caja(V102) no contiene payload/detalle estructurado: detalle_estado180 es texto operativo. No es seguro guardar partes previstas en observaciones500 de venta ni crear pago_venta antes del cobro. Propuesta: **V139**, columna JSON nullable pagos_previstos_json en orden_point_caja (DTO validado y serializado únicamente desde backend), y monto_recibido/cambio nullable DECIMAL(12,2) en pago_venta para comprobantes históricos. Sin actualizar registros anteriores.

Fetch-all --prune realizado. Nueve referencias locales/remotas: ninguna usa V139, máximo138en rama actual,136u inferior en las demás. V139 se reserva en ESTADO antes de crear archivo. ¿Ves alguna colisión o alternativa existente adecuada? Revisa diseño y concurrencia de los puntos1–3 implementados si detectas un bloqueante. No cambies código ni documentos de Codex. Guarda revisión solo con script.
