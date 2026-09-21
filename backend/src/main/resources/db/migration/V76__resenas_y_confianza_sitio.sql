CREATE TABLE empresa_sitio_resena_config (
    empresa_id BIGINT NOT NULL,
    google_maps_url VARCHAR(500) NULL,
    PRIMARY KEY (empresa_id),
    CONSTRAINT fk_sitio_resena_config_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE TABLE empresa_sitio_resena (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    calificacion TINYINT NOT NULL,
    comentario VARCHAR(600) NOT NULL,
    destacada BOOLEAN NOT NULL DEFAULT FALSE,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_sitio_resena_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT chk_sitio_resena_calificacion CHECK (calificacion BETWEEN 1 AND 5),
    INDEX idx_sitio_resena_empresa_orden (empresa_id, orden)
);

CREATE TABLE empresa_sitio_indicador_confianza (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    valor VARCHAR(40) NOT NULL,
    etiqueta VARCHAR(80) NOT NULL,
    icono VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_sitio_indicador_confianza_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    INDEX idx_sitio_indicador_confianza_empresa_orden (empresa_id, orden)
);

INSERT INTO empresa_sitio_resena_config (empresa_id)
SELECT empresa_id FROM empresa_sitio_config;

UPDATE empresa_sitio_bloque
SET superficie = 'SUAVE', presentacion = 'PANEL', ancho_contenido = 'NORMAL', alineacion = 'CENTRO',
    separador = 'LINEA', espaciado = 'AMPLIO', etiqueta = 'Experiencias reales',
    titulo = 'Lo que dicen nuestros clientes',
    descripcion_visual = 'Opiniones que ayudan a reservar con confianza'
WHERE tipo = 'RESENAS';
