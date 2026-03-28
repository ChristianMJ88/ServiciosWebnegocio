ALTER TABLE bandeja_salida_notificacion
    ADD COLUMN proveedor_mensaje_id VARCHAR(64) NULL AFTER intentos,
    ADD COLUMN estado_entrega VARCHAR(30) NULL AFTER proveedor_mensaje_id,
    ADD COLUMN estado_entrega_actualizado_en DATETIME NULL AFTER estado_entrega,
    ADD COLUMN codigo_error_proveedor VARCHAR(32) NULL AFTER estado_entrega_actualizado_en,
    ADD COLUMN detalle_error_proveedor VARCHAR(500) NULL AFTER codigo_error_proveedor;

CREATE INDEX idx_bandeja_canal_proveedor_mensaje
    ON bandeja_salida_notificacion (canal, proveedor_mensaje_id);
