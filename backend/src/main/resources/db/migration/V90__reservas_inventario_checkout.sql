ALTER TABLE checkout_orden
    ADD COLUMN inventario_estado VARCHAR(20) NOT NULL DEFAULT 'NO_APLICA' AFTER estado;

ALTER TABLE producto_movimiento_inventario
    MODIFY COLUMN creado_por_usuario_id BIGINT NULL;
