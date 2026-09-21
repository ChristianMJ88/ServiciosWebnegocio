CREATE TABLE empresa_sitio_pregunta_frecuente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    pregunta VARCHAR(180) NOT NULL,
    respuesta VARCHAR(1200) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    orden INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_sitio_pregunta_frecuente_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    INDEX idx_sitio_pregunta_frecuente_empresa_orden (empresa_id, orden)
);

UPDATE empresa_sitio_bloque
SET superficie = 'BASE', presentacion = 'PANEL', ancho_contenido = 'NORMAL', alineacion = 'IZQUIERDA',
    separador = 'LINEA', espaciado = 'AMPLIO', etiqueta = 'Antes de reservar',
    titulo = 'Preguntas frecuentes',
    descripcion_visual = 'Respuestas claras para ayudarte a preparar tu visita'
WHERE tipo = 'PREGUNTAS_FRECUENTES';
