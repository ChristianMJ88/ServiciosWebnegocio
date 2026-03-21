CREATE TABLE configuracion_correo_empresa (
    empresa_id BIGINT PRIMARY KEY,
    habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    remitente VARCHAR(150) NULL,
    nombre_remitente VARCHAR(150) NULL,
    responder_a VARCHAR(150) NULL,
    actualizada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_configuracion_correo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);
