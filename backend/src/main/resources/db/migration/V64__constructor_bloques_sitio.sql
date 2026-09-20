CREATE TABLE empresa_sitio_bloque (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    orden INT NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_empresa_sitio_bloque_tipo UNIQUE (empresa_id, tipo),
    CONSTRAINT uk_empresa_sitio_bloque_orden UNIQUE (empresa_id, orden),
    CONSTRAINT fk_empresa_sitio_bloque_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    INDEX idx_empresa_sitio_bloque_publico (empresa_id, habilitado, orden)
);

INSERT INTO empresa_sitio_bloque (empresa_id, tipo, habilitado, orden)
SELECT empresa_id, 'PORTADA', TRUE, 0 FROM empresa_sitio_config;

INSERT INTO empresa_sitio_bloque (empresa_id, tipo, habilitado, orden)
SELECT empresa_id, 'CONTACTO', inicio_mostrar_contacto,
       CASE WHEN inicio_orden_secciones = 'CONTACTO_SERVICIOS' THEN 10 ELSE 20 END
FROM empresa_sitio_config;

INSERT INTO empresa_sitio_bloque (empresa_id, tipo, habilitado, orden)
SELECT empresa_id, 'SERVICIOS_DESTACADOS', inicio_mostrar_servicios,
       CASE WHEN inicio_orden_secciones = 'SERVICIOS_CONTACTO' THEN 10 ELSE 20 END
FROM empresa_sitio_config;

INSERT INTO empresa_sitio_bloque (empresa_id, tipo, habilitado, orden)
SELECT empresa_id, tipos.tipo, FALSE, tipos.orden
FROM empresa_sitio_config
CROSS JOIN (
    SELECT 'BENEFICIOS' tipo, 30 orden UNION ALL
    SELECT 'EQUIPO', 40 UNION ALL
    SELECT 'GALERIA', 50 UNION ALL
    SELECT 'PROMOCIONES', 60 UNION ALL
    SELECT 'RESENAS', 70 UNION ALL
    SELECT 'PREGUNTAS_FRECUENTES', 80 UNION ALL
    SELECT 'MAPA', 90
) tipos;
