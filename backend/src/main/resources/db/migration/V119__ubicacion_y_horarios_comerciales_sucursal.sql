ALTER TABLE sucursal
 ADD COLUMN municipio VARCHAR(100) NULL,
 ADD COLUMN estado_region VARCHAR(100) NULL,
 ADD COLUMN pais_codigo CHAR(2) NULL,
 ADD COLUMN codigo_postal VARCHAR(12) NULL,
 ADD COLUMN horarios_comerciales JSON NULL;
CREATE INDEX idx_sucursal_ubicacion ON sucursal (empresa_id, activa, pais_codigo, estado_region, municipio);
