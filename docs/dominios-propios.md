# Dominios propios: operación de 012

Implementación local, pendiente de autorización de publicación. No se crearon sitios, no se conectaron dominios reales y no se modificaron credenciales. La compra/renovación de dominios y el alta automática en Firebase quedan fuera de esta fase.

## Configuración y permisos

La plataforma asigna `DOMINIO_PROPIO` y el límite `DOMINIOS_PROPIOS_MAX` desde el editor de planes. El catálogo no asigna planes ni presupone sus nombres u orden. Un dominio raíz y su `www` son dos solicitudes; el límite ausente conserva la semántica previa de ilimitado, por lo que conviene fijarlo explícitamente al habilitar el módulo.

El negocio necesita `CONFIGURACION_EMPRESA_GESTIONAR`; la empresa se obtiene del principal autenticado. La cola de plataforma exige `PLATAFORMA_DOMINIOS_GESTIONAR`. El backend valida capacidades, versiones, estado y límites. Retirar sigue disponible si vence la suscripción. Los registros se conservan; bajar el límite bloquea agregar y muestra el excedente, sin desactivar dominios existentes.

1. Negocio solicita un dominio que ya controla. El panel muestra su TXT aleatorio.
2. DNS se comprueba con timeout, fuera de transacciones y sin registrar tokens.
3. Plataforma prepara manualmente el dominio en el sitio **clientes**, cuando se autorice esa operación, y captura los registros de Firebase.
4. Negocio agrega esos registros. HTTPS válido y la marca del sitio confirmado permiten activar.
5. Retiro o pérdida de capacidad corta la resolución pública. Recuperar la capacidad restaura el acceso sin volver a pedir TXT. El estado de propiedad no se borra.

Antes de preparar conexiones reales: confirmar el ID del sitio propuesto en `.firebaserc`, crear/configurar el sitio con autorización y definir `DOMINIOS_SITIO_HOSTING` en backend. El job está desactivado por defecto. Su activación inicia DNS/HTTPS reales y necesita la autorización correspondiente. Revisar además el modo SQL de producción antes de migrar: V142 usa el escape normal de cadenas MySQL; no debe ejecutarse con `NO_BACKSLASH_ESCAPES` sin una revisión previa. Nunca modificar una migración aplicada ni usar repair para ocultar un cambio.

## Plantilla, navegación y canonical

`FLUORA_CLIENTES_ORIGIN` es un parámetro HTTPS fijo de Functions, correspondiente al origen del sitio clientes confirmado. No es el hostname aportado por el visitante. `customDomainPage` descarga `/index.csr.html` de ese origen; plantilla y assets pertenecen al mismo despliegue. Una publicación independiente de marketing no cambia esa plantilla. El runbook exige actualizar clientes con el workspace y comprobar cada sitio después de un despliegue parcial.

No existe `index.html` en el artefacto clientes: Hosting lo serviría antes de los rewrites y saltaría el SEO de la portada. La guarda exige su ausencia. Se conserva el CSR vacío con `X-Robots-Tag: noindex`. `/api/**` usa el proxy y el resto llega a `customDomainPage`; solo navegaciones HTML GET/HEAD de rutas públicas del negocio producen HTML. Assets ausentes, directorio, landings y sitemap de Fluora no producen páginas del SaaS en ese host. `/e/<slug>` y los enlaces legales compartidos del footer redirigen al origen de marketing conservando query; también funcionan al abrirlos directamente en otra pestaña. El frontend aplica la misma separación en navegación SPA.

El hostname reenviado solo selecciona el endpoint público; el backend decide el tenant, su publicación y la capacidad vigente. El canonical y las URLs de productos salen de `dominioPrincipal` filtrado por backend, tanto en Function como en navegador. Cuando hay varios aliases activos, comparten el dominio canónico derivado vigente. El header del visitante nunca decide el canonical ni un destino de fetch. Si falta ese dominio vigente, se falla cerrado.

El bootstrap del servidor se consume una sola vez para el host que lo recibió. Navegaciones siguientes vuelven a consultar backend. Una falla termina en la pantalla no disponible con `noindex`, sin redirigir de nuevo a la raíz y golpear la API en bucle. Todas las respuestas dinámicas del dominio propio son `no-store`.

## Estimación por navegación antes de desplegar

No es una factura ni una medición del entorno productivo. Una navegación HTML nueva usa **1 invocación de customDomainPage**, **1 GET al backend para resolver el host** y **1 GET al CSR estático de clientes**. Productos añade un GET del catálogo/detalle. El bootstrap evita otra resolución del host al arrancar Angular. Los assets estáticos no invocan la Function; cada cambio de ruta SPA posterior vuelve a consultar `/publico/sitio/host/…` mediante el proxy (un GET de resolución por navegación), además de sus APIs funcionales. Las consultas funcionales de horarios, catálogo, carrito y pagos son adicionales y dependen de la página. Cada POST público con Origin dinámico añade la comprobación de dominio/capacidad del CORS; no se cachea una decisión vencida.

Para estimar, separar `N` cargas HTML, `S` consultas SPA al proxy, `P` cargas HTML de productos, duración media efectiva `t` y bytes transferidos. Invocaciones aproximadas: `N + S`; lecturas de sitio servidor: `N`; lecturas servidor de productos: `P`; lecturas CSR: `N`. Contabilizar aparte tiempo de backend/SQL, arranques fríos, transferencia, almacenamiento, logs y APIs de terceros. El máximo de instancias limita concurrencia, no garantiza un tope monetario.

La referencia de precios consultada el 21-oct-2026 es [Cloud Run, facturación por solicitudes](https://cloud.google.com/run/pricing): en us-central1, antes de cuotas/descuentos, USD0.40/millón de solicitudes, USD0.000024/vCPU-s y USD0.0000025/GiB-s. Como ejemplo **supuesto**, con 1vCPU, 0.25GiB y 0.5s efectivos sin concurrencia, una invocación sería aproximadamente USD0.0000127125; 10,000 cargas HTML serían USD0.127125 de esa Function, excluyendo los demás conceptos. La duración no fue medida en producción; concurrencia, arranques y configuración efectiva cambian esa cifra. No se descuenta la cuota gratuita compartida ni se promete costo cero.

[Hosting](https://firebase.google.com/docs/hosting/usage-quotas-pricing) cobra almacenamiento y transferencia; la referencia indica 10GB/mes sin cargo y USD0.15 por GB adicional en Blaze. La transferencia real debe medirse, incluyendo HTML, CSR y assets. Antes de publicar, calcular con tráfico y latencias observados, confirmar tarifas/región/configuración efectiva y revisar alertas de presupuesto existentes. Ningún ajuste de facturación fue realizado aquí.
