-- Version139 reservada en ESTADO y coordinada en revision010-migracion.
-- Solo columnas nuevas; no modifica importes ni registros históricos.
ALTER TABLE orden_point_caja ADD COLUMN pagos_previstos_json JSON NULL;
ALTER TABLE pago_venta ADD COLUMN monto_recibido DECIMAL(12,2) NULL, ADD COLUMN cambio DECIMAL(12,2) NULL;
