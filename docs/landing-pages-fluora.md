# Landing pages de Fluora — fase 2

La primera ruta es `/barberias`. Reutiliza el registro y el catálogo público de planes: no decide precios, módulos ni elegibilidad de prueba.

## Responsabilidades

- El módulo backend `marketing` conserva contenido, publicación y versión en `plataforma_landing`. Es configuración de Fluora, separada de los sitios de cada tenant.
- La API pública entrega únicamente páginas publicadas. La edición requiere ámbito de plataforma y el permiso `PLATAFORMA_MARKETING_GESTIONAR`; cada guardado genera auditoría y comprueba la versión.
- `/plataforma/marketing` permite editar portada, imagen HTTPS, beneficios, funcionalidades, pasos, preguntas y metadatos. El componente público es reutilizable para las siguientes industrias.
- La ruta de marketing se habilita únicamente en hosts de plataforma conocidos. No modifica los dominios de los negocios ni el registro existente.
- Firebase `landingPage` prepara HTML inicial con contenido semántico y metadatos desde el backend. Angular carga luego la experiencia interactiva. El sitemap incluye las landing pages publicadas.

## Publicación

1. Desplegar primero el backend con la migración aditiva V115. Se aplicó solamente en desarrollo durante esta implementación.
2. Configurar el parámetro no secreto `FLUORA_APP_ORIGIN` con el origen HTTPS del portal de registro, además de los parámetros existentes de las funciones.
3. Desplegar Functions y el hosting de marketing. La reescritura inicial corresponde a `/barberias`.
4. Comprobar HTML inicial, canonical y sitemap en el dominio publicado. Los cachés públicos pueden tardar hasta cinco minutos en reflejar una edición.

No se realizó despliegue productivo ni se afirma indexación en Google.

## Validación

- Siete pruebas dirigidas de backend, cinco de SEO frontend y nueve de Functions aprobadas.
- Compilaciones de producción de marketing y aplicación aprobadas.
- API pública: página publicada 200 y página inexistente 404. Editor rechazado sin sesión de plataforma, incluso con sesión de tenant.
- Navegación al registro existente con el identificador real del plan comprobada.
- Revisión del sitio público en 390×844, 820×1180, 1024×1366 y 1440×900, sin desbordamiento horizontal. Se corrigieron la separación del encabezado móvil y la navegación local del botón secundario.
- Guardado mediante la interfaz comprobado con una sesión autorizada de plataforma: confirmación visible y versión 1 recuperada desde la API pública. Se conservó el contenido vigente; no se cambiaron credenciales.
- HTML inicial comprobado usando la plantilla compilada de producción y el contenido real de la API local: un H1, canonical, registro y sitemap correctos. Se elimina el contenido general de inicio del `noscript` de la plantilla, conservando su respaldo de estilos.

## Fases posteriores

Agregar otras industrias reutilizando la plantilla y sus rutas; después abordar atribución de campañas y medición. No se añadieron testimonios, cifras de rendimiento ni registros de adquisición ficticios.
