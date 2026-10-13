ALTER TABLE cita
  ADD COLUMN pago_expira_en DATETIME NULL,
  ADD COLUMN reserva_grupo_id VARCHAR(36) NULL,
  ADD COLUMN politica_cobro VARCHAR(30) NULL,
  ADD COLUMN importe_requerido DECIMAL(10,2) NULL,
  ADD INDEX idx_cita_pago_expira (estado, pago_expira_en),
  ADD INDEX idx_cita_reserva_grupo (empresa_id, reserva_grupo_id);
