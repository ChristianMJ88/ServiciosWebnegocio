# Directorio multitenant: primera entrega

- Ruta pública `/negocios`, buscador por nombre comercial, servicios públicos y dirección del sitio o sucursal activa.
- Participación voluntaria desde `/admin/sitio`, apartado Publicación. No se inscriben empresas automáticamente ni se crean datos ficticios.
- El endpoint de administración obtiene empresa de la identidad autenticada y reutiliza los permisos de configuración del sitio. No admite empresa en el cuerpo.
- Backend exige Marketplace al activar; permite retirarse aunque el plan deje de incluirlo. El directorio vuelve a comprobar capacidades usando el servicio y la política de vigencia existentes.
- Publicación requiere sitio publicado, empresa activa, participación y suscripción con módulo. No hay decisiones por nombre de plan en frontend.
- Consultas parametrizadas; comodines de búsqueda escapados; cursor y tamaño fijo acotan candidatos por solicitud. Cada candidato consulta las capacidades existentes; antes de escalar conviene añadir una operación masiva al servicio de capacidades, manteniendo la misma política.
- Las tarjetas usan datos públicos existentes y llevan al catálogo y agenda del negocio. No muestran horarios ni reseñas que no se hayan obtenido de sus fuentes reales.
- Por ahora la nueva ruta tiene `noindex, follow`: faltan renderizado HTML para buscadores, sitemap y publicación en hosting. No se despliega a producción en esta entrega.

Pendiente: filtros estructurados del catálogo, ubicación normalizada, horarios reales, renderizado SEO, prueba de tarjetas con un negocio participante autorizado y pruebas de activación/desactivación desde el portal con sesión real.
