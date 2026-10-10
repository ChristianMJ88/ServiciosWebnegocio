# Atribución comercial — fase 5

Implementada tras aprobar la propuesta. No requiere contratar campañas ni activar Google Ads. La medición externa sigue desactivada.

## Responsabilidades

- `RegistrarEmpresaRequest` acepta un DTO opcional `atribucion`. Las solicitudes anteriores sin ese campo conservan su contrato.
- `ServicioOnboardingEmpresaImpl` entrega al servicio de adquisición el ID de la empresa recién creada. El cliente no puede elegir la empresa, la fecha ni la ruta persistida.
- `ServicioAdquisicionEmpresa` y su implementación validan los identificadores y comprueban la vertical contra las landing pages existentes en backend. No contienen una lista fija de industrias ni reglas de planes, precios o permisos.
- `AdquisicionEmpresaEntidad` y su repositorio guardan una atribución por empresa en `empresa_adquisicion`. La migración V117 agrega tabla, FK e índice único; no cambia registros históricos.
- La persistencia exige participar en la transacción de onboarding: si el alta falla, la atribución no debe quedar aislada. Sin datos válidos no se crea un registro vacío.
- El frontend conserva y envía el contexto opcional al endpoint real de registro. No decide estados comerciales ni interpreta esa atribución como autorización.

Los UTM son datos declarados por el visitante, no prueba de que hubo una campaña pagada o una conversión certificada por Google. Deben usar identificadores sin información personal. No conceden beneficios ni modifican la suscripción.

## Verificación

- Once pruebas dirigidas de backend aprobadas: servicio de adquisición, validación y compatibilidad del DTO, landing pages y transporte de correo de onboarding.
- Cuatro pruebas de frontend aprobadas y compilaciones de marketing y aplicación exitosas.
- Migración aplicada únicamente en desarrollo; backend inició correctamente con V117. La nueva tabla quedó con cero registros, sin historial inventado.
- Endpoint local rechazó una atribución inválida con HTTP 400 antes de crear un negocio.

No se creó un tenant artificial para simular un alta completa ni se probó una campaña pagada. Producción no se modificó.

## Pendiente

Mostrar atribución en informes de plataforma con permisos backend y registrar conversiones de prueba/suscripción a partir de estados confirmados y deduplicados del servidor. Configurar analítica real y consentimiento cuando se decida activarla. No hay cargos por campañas generados por esta implementación.
