export const PERMISOS = {
  panelAdmin: 'PANEL_ADMIN_ACCESO',
  configuracionEmpresaGestionar: 'CONFIGURACION_EMPRESA_GESTIONAR',
  sucursalesGestionar: 'SUCURSALES_GESTIONAR',
  serviciosGestionar: 'SERVICIOS_GESTIONAR',
  prestadoresGestionar: 'PRESTADORES_GESTIONAR',
  usuariosInternosGestionar: 'USUARIOS_INTERNOS_GESTIONAR',
  whatsappConfigurar: 'WHATSAPP_CONFIGURAR',
  contactosAdminVer: 'CONTACTOS_ADMIN_VER',
  reportesAdminVer: 'REPORTES_ADMIN_VER',
  citasAdminGestionar: 'CITAS_ADMIN_GESTIONAR',
  recepcionAcceso: 'RECEPCION_ACCESO',
  recepcionClientesVer: 'RECEPCION_CLIENTES_VER',
  recepcionCitasGestionar: 'RECEPCION_CITAS_GESTIONAR',
  recepcionCheckin: 'RECEPCION_CHECKIN',
  cajaAcceso: 'CAJA_ACCESO',
  cajaCobrar: 'CAJA_COBRAR',
  cajaSesionGestionar: 'CAJA_SESION_GESTIONAR',
  cajaMovimientosGestionar: 'CAJA_MOVIMIENTOS_GESTIONAR',
  staffPanel: 'STAFF_PANEL_ACCESO',
  staffAgendaVer: 'STAFF_AGENDA_VER',
  staffCitasGestionar: 'STAFF_CITAS_GESTIONAR',
  staffDisponibilidadGestionar: 'STAFF_DISPONIBILIDAD_GESTIONAR',
  clientePanel: 'CLIENTE_PANEL_ACCESO'
} as const;

export type PermisoCodigo = typeof PERMISOS[keyof typeof PERMISOS];
