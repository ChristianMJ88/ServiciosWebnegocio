# Revisión de 006 fase 1

Lee AGENTS.md, docs/agentes/PROTOCOLO.md, revisiones/006.md completo y el diff/new files de esta implementación. Eres Claude: arquitectura y revisión solo lectura; no modificar código ni ejecutar comandos, proveedores ni envíos. Devuelve hallazgos concretos con prioridad/ruta/línea/prueba esperada, o aprobación con límites.

Usuario autoriza empezar 006 y commit separado. Solo fase 1, antes de 007; polling/token de fase 2, custodia/condonación 007 y SSE excluidos. Sin migraciones ni contratos API nuevos.

Implementación: records con DatosEventoCita (tenant, sucursal, cita, cliente, origen/actor, Instant UTC Clock, inicio/fin de agenda). Publicador Spring; listener @EventListener síncrono + MANDATORY, sin AFTER_COMMIT/Async, conserva outbox real/dedup/consentimiento. Cancelación del negocio ahora guarda estado/historial y publica desde ServicioCancelacionCita, conserva mensajes y validaciones. Cierre por rutas Admin/Recepción/Staff; confirmación por Admin/Recepción/check-in/Cliente/checkout; reprogramación cliente y Recepción (actor real); cobros directos, venta mixta, Point y proyección online, con guardas idempotentes originales. Se añadieron dos hechos auxiliares para trasladar avisos existentes del checkout: CitaApartadoVencido y PagoCitaConIncidencia. Recordatorios permanecen intactos.

Registro inicial no se convierte en confirmación: conserva un adaptador síncrono NotificacionesRegistroCita que retorna el booleano real del outbox del contrato público y mantiene agrupación WhatsApp/overloads. No se usa un evento mutable ni se simula ese acuse. Solo listener y adaptador llaman al outbox en las rutas migradas; prueba de arquitectura lo verifica. Onboarding/Contactos y recordatorios fuera del refactor.

Pruebas agregadas: transacción JPA/H2 real con bus/listener de Spring, rollback de estado/historial/outbox y rechazo fuera de transacción; escenarios de hechos por origen, reintentos, venta mixta por cita, Point virtual y aislamiento; guarda de records/sin envío asincrónico. Las aserciones anteriores se conservan; los cambios en fixtures conectan el bus/listener real. Se añaden comprobaciones de eventos a pruebas existentes sin sustituir sus comprobaciones de outbox.

Pruebas específicas y suites completas están en ejecución por Codex; no certificar resultados aún. Revisar especialmente omisiones de productores/efectos, compatibilidad de canales/contratos, atomicidad/idempotencia y cualquier consulta innecesaria por cita. Detecta si la resolución administrativa de incidencias necesita origin/actor distinto al default WEBHOOK del confirmador compartido. No ampliar la tarea a reglas de negocio de 007.

Working tree: modificaciones ajenas en revisiones/005.md y archivo 007.md preexistentes: no pertenecen al commit. Leer sin alterarlos si aportan contexto. Revisiones/006.md también es de Claude, no editar/sobrescribir. Esta solicitud es de Codex.
