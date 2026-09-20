CREATE TABLE empresa_archivo_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    proveedor VARCHAR(30) NOT NULL,
    asset_id VARCHAR(150) NULL,
    public_id VARCHAR(500) NOT NULL,
    secure_url VARCHAR(1000) NOT NULL,
    formato VARCHAR(30) NULL,
    ancho INT NULL,
    alto INT NULL,
    bytes BIGINT NOT NULL,
    nombre_original VARCHAR(255) NULL,
    content_type VARCHAR(120) NULL,
    creado_por_usuario_id BIGINT NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_archivo_media_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_archivo_media_usuario FOREIGN KEY (creado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_archivo_media_proveedor_public_id UNIQUE (proveedor, public_id),
    INDEX idx_archivo_media_empresa_categoria (empresa_id, categoria, creado_en)
);

ALTER TABLE empresa_sitio_galeria_imagen
    ADD COLUMN media_id BIGINT NULL AFTER empresa_id,
    ADD CONSTRAINT fk_sitio_galeria_imagen_media FOREIGN KEY (media_id) REFERENCES empresa_archivo_media(id) ON DELETE SET NULL,
    ADD INDEX idx_sitio_galeria_imagen_media (media_id);
