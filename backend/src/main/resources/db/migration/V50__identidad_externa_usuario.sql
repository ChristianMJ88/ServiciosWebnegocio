CREATE TABLE usuario_identidad_externa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    proveedor VARCHAR(30) NOT NULL,
    subject_proveedor VARCHAR(255) NOT NULL,
    correo_verificado VARCHAR(150) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_usuario_identidad_externa_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT uk_usuario_identidad_proveedor UNIQUE (usuario_id, proveedor),
    INDEX idx_identidad_externa_busqueda (proveedor, subject_proveedor)
);
