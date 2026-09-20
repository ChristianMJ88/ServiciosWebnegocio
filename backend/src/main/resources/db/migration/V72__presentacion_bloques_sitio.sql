ALTER TABLE empresa_sitio_bloque
    ADD COLUMN superficie VARCHAR(20) NOT NULL DEFAULT 'BASE' AFTER orden,
    ADD COLUMN presentacion VARCHAR(20) NOT NULL DEFAULT 'ABIERTA' AFTER superficie,
    ADD COLUMN ancho_contenido VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER presentacion,
    ADD COLUMN alineacion VARCHAR(20) NOT NULL DEFAULT 'IZQUIERDA' AFTER ancho_contenido,
    ADD COLUMN separador VARCHAR(20) NOT NULL DEFAULT 'NINGUNO' AFTER alineacion,
    ADD COLUMN espaciado VARCHAR(20) NOT NULL DEFAULT 'NORMAL' AFTER separador,
    ADD COLUMN etiqueta VARCHAR(80) NULL AFTER espaciado,
    ADD COLUMN titulo VARCHAR(160) NULL AFTER etiqueta,
    ADD COLUMN descripcion_visual VARCHAR(400) NULL AFTER titulo;

UPDATE empresa_sitio_bloque
SET superficie = 'SUAVE', presentacion = 'PANEL', ancho_contenido = 'AMPLIO', separador = 'DEGRADADO'
WHERE tipo = 'SERVICIOS_DESTACADOS';

UPDATE empresa_sitio_bloque
SET superficie = 'BASE', presentacion = 'PANEL', ancho_contenido = 'AMPLIO', separador = 'LINEA', etiqueta = 'Galería'
WHERE tipo = 'GALERIA';

UPDATE empresa_sitio_bloque
SET superficie = 'SUAVE', presentacion = 'BANDA', ancho_contenido = 'AMPLIO', separador = 'DEGRADADO',
    etiqueta = 'Promociones', titulo = 'Una oportunidad especial para reservar',
    descripcion_visual = 'Ofertas vigentes seleccionadas para nuestros clientes.'
WHERE tipo = 'PROMOCIONES';

UPDATE empresa_sitio_bloque
SET superficie = 'SUAVE', presentacion = 'PANEL', ancho_contenido = 'NORMAL', separador = 'LINEA'
WHERE tipo = 'CONTACTO';
