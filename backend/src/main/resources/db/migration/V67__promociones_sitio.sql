CREATE TABLE empresa_sitio_promocion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descripcion VARCHAR(500) NULL,
    imagen_url VARCHAR(500) NULL,
    fecha_inicio DATE NULL,
    fecha_fin DATE NULL,
    tipo_descuento VARCHAR(20) NOT NULL,
    valor_descuento DECIMAL(10,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL DEFAULT 'MXN',
    texto_boton VARCHAR(40) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_sitio_promocion_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    INDEX idx_sitio_promocion_empresa_orden (empresa_id, orden)
);

CREATE TABLE empresa_sitio_promocion_servicio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    promocion_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_promocion_servicio_promocion FOREIGN KEY (promocion_id) REFERENCES empresa_sitio_promocion(id) ON DELETE CASCADE,
    CONSTRAINT fk_promocion_servicio_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id),
    CONSTRAINT uq_promocion_servicio UNIQUE (promocion_id, servicio_id),
    INDEX idx_promocion_servicio_empresa (empresa_id)
);

CREATE TABLE empresa_sitio_promocion_sucursal (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    promocion_id BIGINT NOT NULL,
    sucursal_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_promocion_sucursal_promocion FOREIGN KEY (promocion_id) REFERENCES empresa_sitio_promocion(id) ON DELETE CASCADE,
    CONSTRAINT fk_promocion_sucursal_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursal(id),
    CONSTRAINT uq_promocion_sucursal UNIQUE (promocion_id, sucursal_id),
    INDEX idx_promocion_sucursal_empresa (empresa_id)
);
