# Admin dashboard

El directorio raíz contiene únicamente el contenedor que compone el panel, su configuración de navegación y el loader inicial.

Las funcionalidades se agrupan por dominio:

- `access`: usuarios internos, roles y permisos.
- `agenda`: cálculos y tipos de la agenda administrativa.
- `availability`: reglas, excepciones y formularios de disponibilidad.
- `catalog`: sucursales, grupos, subgrupos y servicios.
- `contacts`: solicitudes recibidas desde el sitio público.
- `email`: configuración y migración de correo.
- `overview`: resumen operativo.
- `providers`: prestadores y asignación de servicios.
- `site`: configuración del sitio público.
- `whatsapp`: configuración, onboarding y bandeja de WhatsApp.

Cada dominio mantiene juntos sus componentes (`.ts`, `.html`, `.css`), contratos, formularios, fachadas y pruebas. Las reglas de autorización y los catálogos de negocio continúan siendo responsabilidad del backend.
