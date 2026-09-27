INSERT INTO permiso (codigo, nombre, descripcion)
VALUES ('ENVIOS_CONFIGURAR', 'Configurar envíos', 'Permite configurar proveedores, orígenes y reglas de envío de la empresa')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), descripcion = VALUES(descripcion);

INSERT IGNORE INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo = 'ENVIOS_CONFIGURAR' WHERE r.codigo = 'ADMIN';

INSERT IGNORE INTO rol_empresa_permiso (rol_empresa_id, permiso_id)
SELECT re.id, p.id FROM rol_empresa re JOIN permiso p ON p.codigo = 'ENVIOS_CONFIGURAR' WHERE re.codigo = 'ADMIN';

CREATE TABLE empresa_envio_config (
    empresa_id BIGINT NOT NULL,
    proveedor VARCHAR(40) NOT NULL DEFAULT 'ENVIA',
    habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    ambiente VARCHAR(20) NOT NULL DEFAULT 'PRUEBA',
    token_cifrado MEDIUMTEXT NULL,
    generar_guia_automaticamente BOOLEAN NOT NULL DEFAULT FALSE,
    permitir_seleccion_paquetera BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_recargo VARCHAR(20) NOT NULL DEFAULT 'NINGUNO',
    valor_recargo DECIMAL(12,4) NOT NULL DEFAULT 0,
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (empresa_id),
    CONSTRAINT fk_empresa_envio_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE sucursal_envio_origen (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    nombre_contacto VARCHAR(180) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    correo VARCHAR(180) NULL,
    calle VARCHAR(180) NOT NULL,
    numero_exterior VARCHAR(30) NOT NULL,
    numero_interior VARCHAR(30) NULL,
    colonia VARCHAR(150) NOT NULL,
    municipio VARCHAR(150) NOT NULL,
    estado VARCHAR(120) NOT NULL,
    codigo_postal VARCHAR(12) NOT NULL,
    codigo_pais VARCHAR(2) NOT NULL DEFAULT 'MX',
    referencias VARCHAR(500) NULL,
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_sucursal_envio_origen_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_sucursal_envio_origen_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id),
    CONSTRAINT uk_sucursal_envio_origen UNIQUE (empresa_id, sucursal_id),
    INDEX idx_sucursal_envio_origen_activo (empresa_id, activo)
);
