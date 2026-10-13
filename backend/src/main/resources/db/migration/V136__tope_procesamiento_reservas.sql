ALTER TABLE empresa_configuracion_reservas
  ADD COLUMN horas_tope_procesamiento_pago INT NULL;
CREATE INDEX idx_intento_estado_actualizado ON intento_pago_checkout (estado, actualizado_en);
