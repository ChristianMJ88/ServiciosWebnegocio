# 012 — precisión técnica previa de concurrencia

Lee PROTOCOLO y `revisiones/012-diseno.md` completo, incluida §7, aprobado por el usuario. Solo Read/Glob/Grep, sin comandos, código, datos o credenciales. Devuelve propuesta técnica de implementación; no amplíes negocio.

Hay una contradicción que necesito resolver antes de crear V141 (ya reservada en ESTADO tras fetch de todas las ramas): hostname único en empresa_dominio, pero un tenant sin propiedad no puede apartarlo y otra empresa que verifica debe ganar. Para ofrecer el TXT a la segunda empresa hace falta persistir su desafío independiente.

Propongo empresa_dominio con hostname único para la vinculación, y empresa_dominio_reclamacion para solicitudes por empresa+host, token aleatorio y tiempos/version. Las solicitudes pendientes no reservan dominio. Al verificar, bajo bloqueo de la fila de host y del tenant, se asigna el dominio al ganador solo si la vinculación previa sigue pendiente/retirada; nunca quitar uno verificado/activo sin nueva decisión. El panel proyecta cada reclamación con su estado. Alternativa: pendientes duplicados y un índice único generado solo para estados que reservan hostname; contradice la unicidad literal pero simplifica el modelo. Indica la opción mínima coherente con el diseño y pruebas de carrera/takeover. Aclara qué hacer con los estados verificado/conectando al verificar otra empresa, y cómo mantener propiedad sin exponer tokens de otro tenant. No cambies D1–D3 ni configures planes.

Revisa también si lectura pública por slug debe ocultar el dominio derivado cuando pierde capacidad para no producir canonical hacia un host inhabilitado. Debe conservarse el registro y recuperarse sin verificar de nuevo, sin caché mayor a 60s.
