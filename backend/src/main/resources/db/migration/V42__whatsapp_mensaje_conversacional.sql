CREATE TABLE whatsapp_mensaje (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    telefono_normalizado VARCHAR(30) NOT NULL,
    direccion VARCHAR(12) NOT NULL,
    cuerpo TEXT NULL,
    content_sid VARCHAR(80) NULL,
    proveedor_mensaje_id VARCHAR(80) NULL,
    estado VARCHAR(30) NULL,
    codigo_error_proveedor VARCHAR(32) NULL,
    detalle_error_proveedor VARCHAR(500) NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_whatsapp_mensaje_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    INDEX idx_whatsapp_mensaje_empresa_fecha (empresa_id, creado_en),
    INDEX idx_whatsapp_mensaje_empresa_telefono (empresa_id, telefono_normalizado, creado_en)
);
