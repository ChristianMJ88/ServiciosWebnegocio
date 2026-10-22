-- V141 ya aplicada en desarrollo: se conserva su checksum 1601200476.
-- Versión coordinada en ESTADO tras un segundo fetch de ramas.
ALTER TABLE empresa_dominio
 ADD COLUMN chequeo_en_curso BOOLEAN NOT NULL DEFAULT FALSE AFTER ultimo_chequeo_en,
 ADD COLUMN proximo_chequeo_en DATETIME(6) NULL AFTER chequeo_en_curso,
 ADD COLUMN intentos_chequeo INT NOT NULL DEFAULT 0 AFTER proximo_chequeo_en,
 ADD INDEX idx_empresa_dominio_proximo(estado,proximo_chequeo_en);

UPDATE empresa_dominio SET estado='RETIRADO',ultimo_error='DOMINIO_LEGADO_INVALIDO',version=version+1
 WHERE estado='PENDIENTE_VERIFICACION' AND (CHAR_LENGTH(hostname)>253 OR NOT
 REGEXP_LIKE(hostname,'^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?([.][a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+\\z','c'));
