# Directorio multitenant: primera entrega

- Ruta pública `/negocios`, buscador por nombre comercial, servicios públicos y dirección del sitio o sucursal activa.
- Participación voluntaria desde `/admin/sitio`, apartado Publicación. No se inscriben empresas automáticamente ni se crean datos ficticios.
- El endpoint de administración obtiene empresa de la identidad autenticada y reutiliza los permisos de configuración del sitio. No admite empresa en el cuerpo.
- Backend exige Marketplace al activar; permite retirarse aunque el plan deje de incluirlo. El directorio vuelve a comprobar capacidades usando el servicio y la política de vigencia existentes.
- Publicación requiere sitio publicado, empresa activa, participación y suscripción con módulo. No hay decisiones por nombre de plan en frontend.
- Consultas parametrizadas; comodines de búsqueda escapados; cursor y tamaño fijo acotan candidatos por solicitud. Cada candidato consulta las capacidades existentes; antes de escalar conviene añadir una operación masiva al servicio de capacidades, manteniendo la misma política.
- Las tarjetas usan datos públicos existentes y llevan al catálogo y agenda del negocio. No muestran horarios ni reseñas que no se hayan obtenido de sus fuentes reales.
- Por ahora la nueva ruta tiene `noindex, follow`: faltan renderizado HTML para buscadores, sitemap y publicación en hosting. No se despliega a producción en esta entrega.

## Segunda entrega

- Filtros independientes por zona/dirección y grupo de servicios. Los grupos son nombres reales de cada catálogo; no se mantiene una lista fija global. Las sugerencias del formulario provienen de los servicios ya consultados, no representan todo el catálogo.
- La ubicación consulta direcciones públicas, no infiere ciudades normalizadas ni distancias. Sigue pendiente un modelo de ubicación que permita páginas de ciudad y búsquedas geográficas fiables.
- Tarjetas con portada configurada, identidad, contacto y hasta tres servicios públicos: precio, moneda y duración del catálogo. Una sola consulta por lote obtiene los servicios de los negocios elegibles, priorizando coincidencias con la búsqueda y grupo elegido.
- Los servicios deben tener una asignación activa en una sucursal activa de la misma empresa. La paginación conserva los filtros enviados aunque el usuario cambie campos sin aplicarlos.
- Activación de Nail Art autorizada por el usuario exclusivamente en desarrollo, desde el portal real.

Pendientes de aquella etapa resueltos abajo: ubicación estructurada, horario comercial y renderizado SEO. Sigue pendiente la prueba de desactivación/republicación y la publicación en producción.

## Ubicación, horarios y enlaces públicos

V119 agrega municipio/ciudad, estado/región, país ISO y código postal opcionales a cada sucursal. El backend normaliza espacios y Unicode; no deduce estos campos del texto antiguo. El autocompletado existente de Google Maps puede completarlos cuando la configuración de direcciones del backend lo habilite. Captura manual no equivale a geocodificación verificada.

El horario publicado procede de las reglas de disponibilidad de la sucursal, por día ISO (1=lunes, 7=domingo), respetando vigencia y zona horaria. No existe otro horario editable en Sucursales: enlaza a `/admin/reglas`. El directorio muestra horario habitual, no un indicador de abierto ahora ni excepciones por fecha. La columna JSON de V119 ya aplicada queda reservada, sin uso por la aplicación.

## Editor semanal

`/admin/reglas` permite elegir varios días y aplicar apertura, cierre, intervalo y vigencia comunes a una sucursal o prestador. El endpoint `/api/v1/admin/disponibilidad/reglas/semana` valida el sujeto dentro de la empresa autenticada y guarda todo en una transacción. Omite duplicados exactos y rechaza cruces antes de modificar datos. Reemplazar es explícito y afecta solo días seleccionados con la misma vigencia; conserva otras vigencias y excepciones. La agenda sigue calculando la intersección de sucursal y prestador y aplica las excepciones existentes. Los endpoints individuales permanecen compatibles.

El directorio carga sucursales activas en una consulta por página, solo para empresas autorizadas por capacidades. La búsqueda incorpora ubicación estructurada sin retirar el texto de dirección existente. No hay catálogo inventado de ciudades ni distancias.

`/negocios` tiene función de renderizado HTML desde la API pública, canonical, CollectionPage/ItemList y entrada en sitemap. Las variantes con query son noindex/follow. Las fallas de backend devuelven 503 sin caché. Producción requiere desplegar funciones y hosting; estos cambios no confirman indexación en Google.

Inicio enlaza el directorio y las landing pages publicadas mediante `/publico/marketing/landings/navegacion`. Etiquetas, descripciones y slugs provienen del backend. El componente público de descubrimiento es independiente del hero y de los planes. Flujo: Inicio → Directorio → sitio del tenant → Servicios / Agendar. Las landing pages de Fluora llevan al registro del SaaS.

Staff utiliza el mismo servicio semanal mediante `/api/v1/staff/disponibilidad/reglas/semana`. El backend impone el prestador y la empresa autenticados, ignorando cualquier sujeto enviado en el cuerpo. Admin y Staff comparten metadatos de días e intervalo mínimo del backend; no existen horarios iniciales fijos en el formulario Staff.
