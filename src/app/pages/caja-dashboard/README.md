# Caja dashboard

- `data`: coordinación de consultas y operaciones HTTP.
- `forms`: contratos, creación, normalización y payloads de formularios.
- `payments`: cobros e historial de pagos por cita.
- `session`: apertura, resumen y cierre de sesión.
- `movements`: movimientos manuales e historial del turno.
- `receipt`: DTO, transformación, presentación e impresión del comprobante.
- La raíz conserva la coordinación del tablero y publica una sola vez los estilos prefijados de Caja.

Los catálogos y reglas operativas deben provenir del backend.
