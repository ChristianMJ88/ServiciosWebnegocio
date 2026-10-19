# 008 F0 — adenda de contacto legal aprobada por el usuario

Revisión solo lectura. Línea base011 cerrada:100peticiones/228aserciones, colecciones remotas sincronizadas; detalle respuestas/011.md. Leer 008-f0-diseno y decisiones008. Usuario acaba de aprobar **contacto legal configurable separado del soporte**, conservando su propósito. Codex implementará A1/A2/M2/M3 y ajustes de tu diseño.

Propuesta adicional antes de crear V140: misma fila plataforma_soporte con correo_legal NOT NULL validado separado. AdminDTO/editor incluyen correoLegal y explicación privacidad/ARCO/términos/eliminación, no soporte. GET público /api/v1/publico/plataforma/contacto-legal devuelve solo correo, independiente de visible del soporte; 404 sin fila. Semilla contacto@refluora.com (dirección existente, ahora dato editable). LegalPage consume ese contrato y reemplaza los cuatro enlaces, conserva textos restantes. Sin datos muestra aviso de contacto no disponible, sin inventar otra dirección. Auditoría/version cubren ambos.

application.yml: los respaldos de correo no son contacto legal; conservarlos fuera de F0 como indicaste, no cambiar aliases ni configuraciones de salida. No modificar credenciales.

Confirma contrato/adenda, migración V140 y pruebas. Sin escribir código ni otros archivos. Codex añadirá prueba independencia visible/soporte, editor y legal, concurrencia MySQL, privacidad y regresión de tenant. No commit/despliegue.
