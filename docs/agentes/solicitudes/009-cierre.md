# Revisión de correcciones de 009

Lee solicitudes/009.md y revisiones/009-implementacion.md. El usuario autorizó 009 y C1–C4 de 007-1. Codex corrigió A1, M1, B1–B3, sin migraciones ni cambios de permisos/datos comerciales:

- A1: pagos directos/registrar venta/prepararPoint y la transacción EXTERIOR ServicioMercadoPagoPoint.crearOrden declaran READ_COMMITTED. CalcularBajoBloqueo reutiliza los agregados normales del cálculo común; se eliminaron pagosActuales y serviciosPagadosActuales con bloqueos de rango. Sigue el bloqueo de citas ascendente. C2 mantiene lecturas actuales con FOR UPDATE después del grupo, en webhook RR.
- MySQL: se crean expresamente índices idx_pago_cita_empresa_cita(empresa_id,cita_id,registrado_en) e idx_venta_detalle_cita(empresa_id,cita_id), iguales a V20/V85, en base efímera. Prueba de misma cita conserva200/409. Dos citas diferentes: dos pagos directos y venta/pago directo, con barrera DESPUÉS de ambas lecturas de saldo, ambos200. Hilos conREAD_COMMITTED y prueba de anotaciones de todas las entradas incluida exterior Point. Aceptación C2 cambia conexión de esa prueba a REPEATABLE_READ, establece snapshot vacío, confirma anticipo en otra conexión y verifica NULL vs aceptación de orden actual.
- M1: editor usa error.mensaje primero; prueba con ErrorApi real.
- B1: Point multicita desactivado/notificado en Cobros y guard en método, lee mensaje backend. POS conserva rechazo backend explícito permitido; no se envía una instrucción externa antes de validar.
- B2: etiqueta Pagada solo estadoCobro=PAGADA; resto Atendida · por cerrar. La clasificación por check-in/saldo0 conserva la decisión ya aprobada (no se cambia la regla por ser gratuito).
- B3: prueba reserva mixta con TRES servicios: anticipo50% de250, fijo40 y sin anticipo; ServicioPoliticaReservaPago REAL con política/checkout conectados: todas PENDIENTE_PAGO, expiración y total165; sin mensajes.

Inventario de escritores y límites: pago_cita solo ServicioCaja; venta directa ahora bloqueo; proyección checkout ordinaria sigue confirmar/grupo y aceptación antes de proyección venta; confirmarPoint no bloquea cita y Point pendiente no reserva saldo, pendiente O2 preexistente explícito con Point multi-cita; bot usa publico=false O1 pendiente. No se amplían esos flujos en 009. Revisar si esto deja riesgo introducido por los cambios RC dentro del alcance.

Último conteo XML completo observado:539 pruebas,0 fallos/errores,1omitida,143suites; MySQLconcurrencia8tests sin omitir. Angular181/64 sinfallos, antes del ajuste visual de tabs para usar tokens operation y evitar recorte en móvil; repetición final pendiente. Revisiones UI en curso con datos existentes sin cobrar. En ejecución externa apareció política versión1 publicada, Codex no la publicó ni alteró; sin envíos ni nuevas reservas propias en este encargo. Sin commit/despliegue/producción.

Revisa código/pruebas en solo lectura y devuelve hallazgos o aprobación condicionada al cierre de evidencia visual/suites. Guardar en revisiones/009-cierre.md (archivo nuevo), no tocar aplicación.
