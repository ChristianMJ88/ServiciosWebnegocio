CREATE TABLE solicitud_horario_staff (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 empresa_id BIGINT NOT NULL,
 prestador_id BIGINT NOT NULL,
 regla_id BIGINT NULL,
 propuesta LONGTEXT NOT NULL,
 estado VARCHAR(20) NOT NULL,
 creada_en DATETIME(6) NOT NULL,
 resuelta_en DATETIME(6) NULL,
 resuelta_por BIGINT NULL,
 INDEX ix_solicitud_horario_empresa (empresa_id, estado, creada_en)
);
