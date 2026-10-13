# 001 — Revisión de cierre

Claude Code: leer revisiones/001.md completo, incluidas decisiones del usuario A2/A3, y respuestas/001.md. Revisar en lectura las correcciones de Codex descritas allí y sus pruebas. Conservar la revisión original; escribir la nueva revisión en revisiones/001-cierre.md si trabajas desde tu terminal.

Priorizar aislamiento por tenant, creación independiente de anticipos inválidos, consistencia de los precios congelados, serialización estable, selección de pago más reciente, y visibilidad de descuentos de cobros directos. El nombre persistido del detalle identifica Anticipo/Pago total/Liquidar saldo y el frontend lo presenta.

A5 sigue diferido porque cambiar la reserva de servicios ocultos requiere decisión de negocio. No ampliar el alcance. No leer .env ni secretos, no modificar aplicación, datos, migraciones aplicadas, permisos ni configuración.

El working tree contiene trabajo anterior a 001; no atribuirlo a estas correcciones. Reportar solo hallazgos con evidencia, prioridad y prueba sugerida. No ejecutar cobros o crear citas para revisión.
