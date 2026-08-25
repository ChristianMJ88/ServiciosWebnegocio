export type SeccionAdmin =
  | 'resumen'
  | 'sitio'
  | 'correo'
  | 'whatsapp'
  | 'mensajes'
  | 'contactos'
  | 'sucursales'
  | 'servicios'
  | 'usuarios'
  | 'prestadores'
  | 'reglas'
  | 'excepciones'
  | 'citas';

export type ModuloAdminDef = {
  id: SeccionAdmin;
  titulo: string;
  descripcion: string;
  abreviatura: string;
  icono: string;
  capacidad?: CapacidadAdmin;
};

export type CapacidadAdmin =
  | 'puedeGestionarConfiguracionEmpresa'
  | 'puedeGestionarWhatsapp'
  | 'puedeVerContactosAdmin'
  | 'puedeGestionarSucursales'
  | 'puedeGestionarServicios'
  | 'puedeGestionarUsuariosInternos'
  | 'puedeGestionarPrestadores'
  | 'puedeGestionarCitasAdmin';

export type GrupoSidebarAdmin = {
  id: string;
  titulo: string;
  icono: string;
  modulos: SeccionAdmin[];
};

export const MODULOS_ADMIN: readonly ModuloAdminDef[] = [
  { id: 'resumen', titulo: 'Dashboard', descripcion: 'Indicadores clave, agenda e ingresos del negocio.', abreviatura: 'DB', icono: 'resumen' },
  { id: 'sitio', titulo: 'Sitio web', descripcion: 'Slug, branding, dominio y publicación del tenant público.', abreviatura: 'SW', icono: 'sitio', capacidad: 'puedeGestionarConfiguracionEmpresa' },
  { id: 'correo', titulo: 'Correo transaccional', descripcion: 'Graph o SMTP por tenant, con cifrado y migración de secretos.', abreviatura: 'CO', icono: 'correo', capacidad: 'puedeGestionarConfiguracionEmpresa' },
  { id: 'whatsapp', titulo: 'WhatsApp y Twilio', descripcion: 'Sender, plantillas, pruebas y trazabilidad por tenant.', abreviatura: 'WA', icono: 'whatsapp', capacidad: 'puedeGestionarWhatsapp' },
  { id: 'mensajes', titulo: 'Mensajes', descripcion: 'Inbox de conversaciones WhatsApp con clientes.', abreviatura: 'MS', icono: 'contactos', capacidad: 'puedeGestionarWhatsapp' },
  { id: 'contactos', titulo: 'Contactos', descripcion: 'Mensajes recibidos desde el formulario web y seguimiento comercial.', abreviatura: 'CN', icono: 'contactos', capacidad: 'puedeVerContactosAdmin' },
  { id: 'sucursales', titulo: 'Sucursales', descripcion: 'Alta y mantenimiento de sedes operativas.', abreviatura: 'SU', icono: 'sucursal', capacidad: 'puedeGestionarSucursales' },
  { id: 'servicios', titulo: 'Servicios', descripcion: 'Catálogo, duración, buffers y precio.', abreviatura: 'SV', icono: 'servicio', capacidad: 'puedeGestionarServicios' },
  { id: 'usuarios', titulo: 'Usuarios internos', descripcion: 'Recepción, caja y administradores internos.', abreviatura: 'UI', icono: 'prestador', capacidad: 'puedeGestionarUsuariosInternos' },
  { id: 'prestadores', titulo: 'Prestadores', descripcion: 'Usuarios staff y asignaciones de servicio.', abreviatura: 'PR', icono: 'prestador', capacidad: 'puedeGestionarPrestadores' },
  { id: 'reglas', titulo: 'Horarios base', descripcion: 'Reglas semanales por sucursal o prestador.', abreviatura: 'HB', icono: 'horario', capacidad: 'puedeGestionarPrestadores' },
  { id: 'excepciones', titulo: 'Bloqueos', descripcion: 'Vacaciones, descansos y cierres puntuales.', abreviatura: 'BL', icono: 'bloqueo', capacidad: 'puedeGestionarPrestadores' },
  { id: 'citas', titulo: 'Agenda', descripcion: 'Seguimiento operativo y gestión detallada de reservas.', abreviatura: 'AG', icono: 'agenda', capacidad: 'puedeGestionarCitasAdmin' }
];

export const GRUPOS_SIDEBAR_ADMIN: readonly GrupoSidebarAdmin[] = [
  { id: 'operacion', titulo: 'Operación', icono: 'operacion', modulos: ['resumen', 'citas', 'contactos'] },
  { id: 'canales', titulo: 'Canales', icono: 'canales', modulos: ['mensajes', 'whatsapp', 'correo'] },
  { id: 'catalogo', titulo: 'Catálogo', icono: 'catalogo', modulos: ['servicios', 'sucursales'] },
  { id: 'equipo', titulo: 'Equipo', icono: 'equipo', modulos: ['prestadores', 'usuarios'] },
  { id: 'configuracion', titulo: 'Disponibilidad', icono: 'configuracion', modulos: ['reglas', 'excepciones', 'sitio'] }
];
