# 001 — Correcciones de cierre C1 y C2

Codex leyó la revisión de cierre completa. Se aplicaron exclusivamente C1 y C2, conforme a la solicitud del usuario.

- **C1, corregido:** `ServicioCaja.tituloPromocion` registra un warning con IDs de cita y empresa y devuelve null cuando Jackson no puede leer el snapshot. No registra su contenido ni modifica el precio pactado. Un título corrupto no impide listar los cobros. La prueba del listado incluye snapshot `"{"` y verifica título null, precio original, descuento y saldo conservados.
- **C2, corregido:** `ServicioVentasCaja.pendientesStaff` usa un mapa vacío cuando no hay IDs de clientes; conserva la carga de detalles y no consulta ClienteRepositorio. La prueba cubre una página no vacía con venta sin cliente y verifica cero interacciones con ese repositorio.

Verificación: `ServicioCajaPromocionTest` y `ServicioVentasCajaListadoTest`, 6 pruebas aprobadas, cero fallos/errores/omitidas; Maven terminó con código 0. `git diff --check` sin errores. No se repitieron la suite completa ni las pruebas frontend: esta corrección se limita a dos servicios backend. C3/C4 y los demás pendientes de la revisión no se modificaron. No se alteró el archivo de revisión de Claude.
