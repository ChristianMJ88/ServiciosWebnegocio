CREATE TABLE empresa_sitio_beneficio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    descripcion VARCHAR(400) NOT NULL,
    icono VARCHAR(30) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_sitio_beneficio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    INDEX idx_sitio_beneficio_empresa_orden (empresa_id, orden)
);

UPDATE empresa_sitio_bloque
SET superficie = 'MARCA', presentacion = 'ABIERTA', ancho_contenido = 'AMPLIO', alineacion = 'CENTRO',
    separador = 'DEGRADADO', espaciado = 'NORMAL', etiqueta = 'Por qué elegirnos',
    titulo = 'Una experiencia pensada para ti'
WHERE tipo = 'BENEFICIOS';
