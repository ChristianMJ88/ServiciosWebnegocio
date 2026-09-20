UPDATE empresa_sitio_bloque
SET superficie = 'BASE',
    presentacion = 'PANEL',
    ancho_contenido = 'AMPLIO',
    alineacion = 'CENTRO',
    separador = 'LINEA',
    espaciado = 'NORMAL',
    etiqueta = COALESCE(etiqueta, 'Nuestro equipo'),
    titulo = COALESCE(titulo, 'Conoce a quienes te atienden'),
    descripcion_visual = COALESCE(
        descripcion_visual,
        'Profesionales del negocio disponibles para acompañarte en tu próxima cita.'
    )
WHERE tipo = 'EQUIPO';
