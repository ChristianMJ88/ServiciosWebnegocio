# 004 / P3 — Inventario de contratos temporales

El alcance prioritario implementado en P3 es Caja, Recepción y solicitudes de horario. Los siete DTO indicados en la revisión exponen **OffsetDateTime UTC**, incluidos cierre de sesión, notificación y cierre de espera; no cambian los tipos de persistencia ni los campos de agenda. El mapeo `MomentoUtc.de` añade el offset UTC a un DATETIME que ya guarda UTC; nunca reinterpreta ese valor en la zona de la JVM.

El repositorio de pagos conciliados devuelve ahora una proyección de persistencia con LocalDateTime; el servicio la convierte a la respuesta de API. Así se evita que Hibernate intente construir un DTO OffsetDateTime a partir de una columna DATETIME. Se conserva el filtro de tenant, los importes y el orden de pagos directos/conciliados.

## Clasificación del resto de respuestas con LocalDateTime

Inventario de código, no cambios de contratos de estos módulos en este paso. Permite continuar por módulo con sus consumidores y pruebas, sin aplicar un serializador global que también convertiría horas de agenda. No implica que toda la API haya migrado a offset en P3.

| Respuesta (ruta desde com/techprotech/agenda) | Campos pendientes o conservados | Clasificación |
|---|---|---|
| `modulos/admin/api/dto/AuditoriaConfiguracionAdminResponse.java` | `creadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/admin/api/dto/AuditoriaRolInternoAdminResponse.java` | `creadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/admin/api/dto/DetalleInvitacionUsuarioResponse.java` | `expiraEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/admin/api/dto/InvitacionUsuarioResponse.java` | `expiraEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/admin/api/dto/LogMensajeWhatsappAdminResponse.java` | `enviadaEn`, `estadoEntregaActualizadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/admin/api/dto/MensajeWhatsappAdminResponse.java` | `creadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/admin/api/dto/PlantillaWhatsappEmpresaAdminResponse.java` | `creadaEn`, `actualizadaEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/caja/api/dto/CitaPorCobrarResponse.java` | `inicio` | A: agenda; conservar LocalDateTime |
| `modulos/clientes/api/dto/ClienteResponse.java` | `ultimaCita`, `ultimaVisita` | A: agenda; conservar LocalDateTime |
| `modulos/envios/api/dto/CotizacionEnvioPublicaResponse.java` | `expiraEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/envios/api/dto/EnvioEventoResponse.java` | `creadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/envios/api/dto/EnvioPedidoResponse.java` | `creadoEn`, `actualizadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/pagos/api/dto/ConciliacionPagoResponse.java` | `liquidadoEn`, `actualizadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/pagos/api/dto/ConexionProveedorPagoResponse.java` | `conectadoEn`, `tokenExpiraEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/pagos/api/dto/IncidenciaCheckoutResponse.java` | `creadaEn`, `inicio` | creadaEn: B pendiente; Cita.inicio: A, conservar |
| `modulos/pagos/api/dto/OrdenCheckoutResponse.java` | `expiraEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/pagos/api/dto/OrdenPointResponse.java` | `creadoEn`, `actualizadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/pagos/api/dto/ReporteLiberacionResponse.java` | `fechaDesde`, `fechaHasta`, `solicitadoEn`, `procesadoEn` | solicitadoEn/procesadoEn: B pendientes; rango fechaDesde/fechaHasta: revisar con proveedor/formulario |
| `modulos/parametros/api/dto/ParametroSistemaResponse.java` | `actualizadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/plataforma/api/dto/AvisoAccesoSuscripcionResponse.java` | `graciaHasta` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/plataforma/api/dto/CapacidadesEmpresaResponse.java` | `pruebaHasta`, `periodoActualHasta` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/plataforma/api/dto/EstadoSuscripcionEmpresaResponse.java` | `iniciaEn`, `pruebaHasta`, `periodoActualHasta` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/plataforma/api/dto/IntegracionPagoTenantResponse.java` | `conectadoEn`, `ultimaSincronizacionEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/plataforma/api/dto/PlanPlataformaResponse.java` | `vigenteDesde`, `vigenteHasta` | Vigencias con hora: revisar junto al formulario de edición y su request antes de cambiar el contrato |
| `modulos/plataforma/api/dto/ReglaComisionPlataformaResponse.java` | `vigenteDesde`, `vigenteHasta` | Vigencias con hora: revisar junto al formulario de edición y su request antes de cambiar el contrato |
| `modulos/plataforma/api/dto/SuscripcionPlataformaResponse.java` | `iniciaEn`, `terminaEn`, `pruebaHasta` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/plataforma/api/dto/TenantPlataformaResponse.java` | `creadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/productos/api/dto/ExistenciaProductoResponse.java` | `actualizadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/productos/api/dto/MovimientoInventarioResponse.java` | `creadoEn` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/ventasstaff/api/VentaStaffResponse.java` | `fecha` | B: momentos; migración de contrato pendiente en su módulo |
| `modulos/whatsapp/onboarding/api/OnboardingWhatsappResponse.java` | `actualizadoEn` | B: momentos; migración de contrato pendiente en su módulo |

## Frontend de este paso

- DatePipe presenta los momentos según la zona del navegador. No se fija una zona de negocio ni se resta una cantidad de horas en el frontend. La agenda conserva su contrato actual.
- Comprobante: conserva la cadena ISO del backend con su offset hasta renderizarla.
- Recepción: muestra la hora de llegada, el registro y la notificación de espera usando el mismo DatePipe.
- Caja/Recepción no ordenan ni comparan esos momentos como texto; el backend conserva la ordenación. El historial combinado de pagos usa Comparator sobre OffsetDateTime.
- Las regresiones de render se ejecutan con navegador de pruebas en America/Mexico_City (`TZ` del proceso), y comprueban además dos offsets de visualización y el cambio de día.

Los datos locales anteriores (A2) siguen pendientes. Un dato histórico que se convirtió a hora local en 003 necesita primero su restitución documentada; el offset correcto no corrige una fecha almacenada con otra convención.
