CREATE TABLE whatsapp_plantilla_empresa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    uso VARCHAR(80) NOT NULL,
    content_sid VARCHAR(80) NOT NULL,
    tipo_contenido VARCHAR(80) NULL,
    categoria VARCHAR(40) NULL,
    estado VARCHAR(40) NULL,
    activa BIT(1) NOT NULL DEFAULT b'1',
    creada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_whatsapp_plantilla_empresa_uso_sid UNIQUE (empresa_id, uso, content_sid),
    INDEX idx_whatsapp_plantilla_empresa_uso (empresa_id, uso, activa)
);
