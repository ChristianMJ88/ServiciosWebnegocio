ALTER TABLE checkout_orden
  ADD COLUMN requiere_conciliacion BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN motivo_incidencia VARCHAR(500) NULL;
