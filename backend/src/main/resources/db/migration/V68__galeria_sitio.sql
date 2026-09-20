CREATE TABLE empresa_sitio_galeria_config (
    empresa_id BIGINT NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    vista VARCHAR(20) NOT NULL DEFAULT 'CUADRICULA',
    ampliacion_habilitada BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (empresa_id),
    CONSTRAINT fk_sitio_galeria_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE empresa_sitio_galeria_imagen (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    imagen_url VARCHAR(500) NOT NULL,
    texto_alternativo VARCHAR(180) NOT NULL,
    titulo VARCHAR(120) NULL,
    descripcion VARCHAR(500) NULL,
    servicio_id BIGINT NULL,
    sucursal_id BIGINT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_sitio_galeria_imagen_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_sitio_galeria_imagen_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id) ON DELETE SET NULL,
    CONSTRAINT fk_sitio_galeria_imagen_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id) ON DELETE SET NULL,
    INDEX idx_sitio_galeria_imagen_empresa_orden (empresa_id, orden)
);
