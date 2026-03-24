CREATE TABLE solicitud_contacto (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) NULL,
    correo VARCHAR(150) NOT NULL,
    asunto VARCHAR(180) NOT NULL,
    mensaje TEXT NOT NULL,
    canal VARCHAR(30) NOT NULL DEFAULT 'WEB',
    estado VARCHAR(20) NOT NULL DEFAULT 'NUEVO',
    notificacion_correo_programada BOOLEAN NOT NULL DEFAULT FALSE,
    notificada_en DATETIME NULL,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_solicitud_contacto_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE INDEX idx_solicitud_contacto_empresa_estado_creada
    ON solicitud_contacto (empresa_id, estado, creada_en);
