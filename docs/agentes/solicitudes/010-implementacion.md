# Revisión010 · implementación completa en borrador

Codex, 2026-10-17. Solicito revisión SOLO LECTURA de backend/Angular de010, sin cambiar código. El usuario autorizó commit inicial (85d3cc2) →1–3→4/5juntos. Revisiones ajenas005/006/007/008/home/011fuera. No commit010 autorizado.

Implementado:
- Point T1RC con terminal/sesión/citas y orden CREANDO, HTTP NOT_SUPPORTED, T2RC bajo bloqueo de orden; saldo reservado sin contar como dinero pagado, flag en Caja/Recepción/Staff, multicita.
- B1 de010-migracion corregido: ErrorProveedorPoint tipado, solo4xxdefinitivos primerenvío cancela. Timeout/5xx/respuesta vacía/cuerpo incompleto/reenvío4xx conserva. Reenvío acotado por configuración PT20M, despuésREQUIERE_REVISIONsin liberar. Root API no mantiene tx en red.
- Caja existencias→citas, webhookRC/aceptaciónsinFORUPDATEen pagos, advice409soloCaja. Bot requiere anticipo deriva al sitio, guarda crearCitaBot antes de crear; internos conservados.
- V139aprobada/coordinada: JSONinmutable previsto/recibido/cambio pago_venta. Migración solo añadidos. **No aplicada a base habitual**. Prueba de migración en copia efímera.
- PlanPagosVenta común: legacy/pagos excluyentes, hasta3partes,Pointúnico readonly resto, BigDecimalvalidación. SinPoint venta/pagos/efectivo/comisiones atómicos. Point acredita orden.importe+previstas juntos; desajuste registra soloPointyavisa. Corte bloqueado mientrasPointactivo conlocksesión enT1 paraevitarcierreconcurrente. Citas con saldo legado cubierto registran dinero y avisan; crédito007-2pendiente.
- RegistrarVentaRequest con ventaPendienteId cobra Staff también por reparto; códigos/permisos server existentes. Reembolso mixto desde Point rechazado hasta gestionar reversión completa007-2, no perder efectivo.
- VentaResponse pagos[]/MIXTO y comprobante; historial de cita agrupa pago_venta para no duplicar d.total al haber dos métodos. Conciliación proveedor usa orden.importe ya existente.
- app-caja-cobro compartido, sin API/decisión de negocio propia; presenta metadatos backend (efectivo/Point/referencia/denominaciones). Restantes/cambio son estimaciones, precio final server. CTA único fijo móvil. Espera bloqueaform y conserva datos alrechazar. POS/Cobros usan mismo componente. Métodos viejos POS retirados.

Validación en progreso: primera tanda focal anterior pasó; nuevas regresiones han requerido adaptar fixtures, mocks de records y dialecto JPA en testsMySQL. No afirmo suites cerradas todavía. Angular188tests:185pasaron/3NG0100 de tests que mutaban campos fuera del evento; markForCheckenfixture aplicado, no se desactiva detección ni se quitan aserciones. Suites completas/revisiónvisual pendientes. Busca bloqueantes/medios antes de esos cierres, especialmente ordenlocks, PointHTTPincierto, corte, reparto y seguridadtenant/RBAC. Revisa V139yDTOnuevo contra010-migracion.

Restricciones: no cobros/envíos reales, ni producción/push/deploy. Copias efímeras únicamente para datos de pruebas.
