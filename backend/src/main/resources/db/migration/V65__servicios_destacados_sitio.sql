CREATE TABLE empresa_sitio_servicios_destacados_config (
    empresa_id BIGINT NOT NULL,
    vista VARCHAR(20) NOT NULL DEFAULT 'TARJETAS',
    mostrar_precio BOOLEAN NOT NULL DEFAULT TRUE,
    mostrar_duracion BOOLEAN NOT NULL DEFAULT TRUE,
    mostrar_descripcion BOOLEAN NOT NULL DEFAULT TRUE,
    limite INT NOT NULL DEFAULT 3,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (empresa_id),
    CONSTRAINT fk_sitio_destacados_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE empresa_sitio_servicio_destacado_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    referencia_id BIGINT NOT NULL,
    etiqueta VARCHAR(40) NULL,
    orden INT NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_sitio_destacado_referencia UNIQUE (empresa_id, tipo, referencia_id),
    CONSTRAINT uk_sitio_destacado_orden UNIQUE (empresa_id, orden),
    CONSTRAINT fk_sitio_destacado_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    INDEX idx_sitio_destacado_empresa_orden (empresa_id, orden)
);

INSERT INTO empresa_sitio_servicios_destacados_config (
    empresa_id, vista, mostrar_precio, mostrar_duracion, mostrar_descripcion, limite
)
SELECT empresa_id, 'TARJETAS', TRUE, TRUE, TRUE, 3
FROM empresa_sitio_config;
