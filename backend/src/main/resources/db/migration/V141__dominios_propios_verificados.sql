-- Reservada en ESTADO tras actualizar e inspeccionar todas las ramas (2026-10-20).
-- Los pendientes no reservan un hostname: solo su dueño comprobado lo puede reservar.
CREATE TABLE empresa_dominio (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 empresa_id BIGINT NOT NULL,
 hostname VARCHAR(253) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 estado VARCHAR(30) NOT NULL,
 token_verificacion VARCHAR(43) CHARACTER SET ascii NOT NULL,
 registros_hosting JSON NULL,
 identificador_build VARCHAR(64) NULL,
 verificado_en DATETIME(6) NULL,
 activado_en DATETIME(6) NULL,
 ultimo_chequeo_en DATETIME(6) NULL,
 ultimo_error VARCHAR(180) NULL,
 creado_en DATETIME(6) NOT NULL,
 creado_por BIGINT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 hostname_reservado VARCHAR(253) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS
   (CASE WHEN estado IN ('VERIFICADO','CONECTANDO','ACTIVO','ERROR') THEN hostname END) STORED,
 clave_vigente VARCHAR(280) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS
   (CASE WHEN estado <> 'RETIRADO' THEN CONCAT(empresa_id,':',hostname) END) STORED,
 CONSTRAINT fk_empresa_dominio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
 UNIQUE KEY uk_empresa_dominio_hostname_reservado(hostname_reservado),
 UNIQUE KEY uk_empresa_dominio_clave_vigente(clave_vigente),
 INDEX idx_empresa_dominio_hostname(hostname),
 INDEX idx_empresa_dominio_empresa_estado(empresa_id,estado),
 INDEX idx_empresa_dominio_chequeo(estado,ultimo_chequeo_en)
);
INSERT INTO empresa_dominio(empresa_id,hostname,estado,token_verificacion,creado_en)
 SELECT empresa_id,LOWER(dominio_principal),'PENDIENTE_VERIFICACION',
 REPLACE(REPLACE(REPLACE(TO_BASE64(RANDOM_BYTES(32)),'+','-'),'/','_'),'=',''),UTC_TIMESTAMP(6)
 FROM empresa_sitio_config WHERE dominio_principal IS NOT NULL AND TRIM(dominio_principal)<>'';
UPDATE empresa_sitio_config SET dominio_principal=NULL WHERE dominio_principal IS NOT NULL;

INSERT INTO plataforma_modulo(codigo,nombre,descripcion,orden) VALUES
 ('DOMINIO_PROPIO','Dominio propio','Conecta un dominio del negocio con propiedad verificada',100)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre),descripcion=VALUES(descripcion),orden=VALUES(orden);
INSERT INTO plataforma_limite(codigo,nombre,descripcion,unidad,orden) VALUES
 ('DOMINIOS_PROPIOS_MAX','Dominios propios','Solicitudes vigentes, incluidos dominios y subdominios','dominios',140)
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre),descripcion=VALUES(descripcion),unidad=VALUES(unidad),orden=VALUES(orden);
INSERT INTO plataforma_permiso(codigo,nombre,descripcion) VALUES
 ('PLATAFORMA_DOMINIOS_GESTIONAR','Gestionar dominios propios','Preparación de Hosting, seguimiento y retiro de dominios')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre),descripcion=VALUES(descripcion);
INSERT IGNORE INTO plataforma_rol_permiso(rol_id,permiso_id)
 SELECT r.id,p.id FROM plataforma_rol r CROSS JOIN plataforma_permiso p
 WHERE r.codigo='ADMINISTRADOR_PLATAFORMA' AND p.codigo='PLATAFORMA_DOMINIOS_GESTIONAR';
-- No asigna módulos ni límites a ningún plan.
