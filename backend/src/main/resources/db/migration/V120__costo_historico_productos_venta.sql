-- Nullable: no se reconstruye el costo de ventas u órdenes antiguas.
ALTER TABLE venta_detalle ADD COLUMN costo_unitario_historico DECIMAL(12,2) NULL;
ALTER TABLE checkout_orden_detalle ADD COLUMN costo_unitario_historico DECIMAL(12,2) NULL;
