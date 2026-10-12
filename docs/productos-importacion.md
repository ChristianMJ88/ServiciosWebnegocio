# Productos: paginación e importación

El panel solicita `/api/v1/admin/productos/catalogo?pagina=0&busqueda=...`. El backend aplica la búsqueda y `Pageable` sobre la empresa autenticada y devuelve página, tamaño y totales. El tamaño predeterminado es 25, con máximo operativo de 100. Los precios regionales se consultan sólo para los productos de la página.

La importación utiliza los permisos y el módulo existentes de Productos:

- `GET /api/v1/admin/productos/importacion/plantilla`: archivo XLSX con encabezados, instrucciones y referencias de categorías/proveedores del tenant.
- `POST /api/v1/admin/productos/importacion/revision`: multipart `archivo`, revisión sin escritura.
- `POST /api/v1/admin/productos/importacion`: revalida el archivo y crea el lote en una transacción; cualquier fallo revierte el lote.

Sólo se crean productos nuevos. Los SKU existentes no se sobrescriben. Se validan los DTO, relaciones del tenant, reglas compartidas del catálogo y el límite `PRODUCTOS_MAX` del plan. Se rechazan fórmulas. Las imágenes, medidas y precios por zona se editan después; las existencias iniciales se registran en Inventario.

Los límites operativos del lector se configuran por `PRODUCTOS_IMPORTACION_MAX_FILAS` (predeterminado 500) y `PRODUCTOS_IMPORTACION_MAX_BYTES` (predeterminado 5 MiB). No sustituyen los límites comerciales del plan ni los límites multipart del servidor.

La revisión no reserva capacidad ni bloquea cambios concurrentes: la confirmación vuelve a validar y puede rechazar el lote si cambió el catálogo o la capacidad disponible.
