# Caja dashboard

- `data`: coordinación de consultas y operaciones HTTP.
- `forms`: contratos, creación, normalización y payloads de formularios.
- `payments`: cobros e historial de pagos por cita.
- `session`: apertura, resumen y cierre de sesión.
- `movements`: movimientos manuales e historial del turno.
- `receipt`: DTO, transformación, presentación e impresión del comprobante.
- `header`: toolbar, alertas, navegación superior y perfil.
- `overview`: contexto operativo de sucursal, estado y métricas.
- `navigation`: selector de vista y contexto del cobro activo.
- `models`: contratos de presentación del dashboard.
- `state`: store con señales privadas, lectura pública y estado derivado probado.
- La raíz conserva la coordinación del tablero y publica una sola vez los estilos prefijados de Caja.

Los catálogos y reglas operativas deben provenir del backend.

Cada dominio operativo contiene su propia fachada; `data` queda reservado para cargar y refrescar el tablero agregado.
El coordinador de `data` administra el ciclo de carga, errores y aplicación de respuestas al store.
