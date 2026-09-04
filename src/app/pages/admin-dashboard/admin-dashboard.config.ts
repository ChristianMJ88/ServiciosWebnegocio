import { PERMISOS, PermisoCodigo } from '../../core/auth/permissions';

export type SeccionAdmin =
  | 'resumen'
  | 'sitio'
  | 'correo'
  | 'whatsapp'
  | 'mensajes'
  | 'contactos'
  | 'clientes'
  | 'sucursales'
  | 'servicios'
  | 'usuarios'
  | 'permisos'
  | 'prestadores'
  | 'reglas'
  | 'excepciones'
  | 'citas'
  | 'parametros';

export type ModuloAdminDef = {
  id: SeccionAdmin;
  titulo: string;
  descripcion: string;
  abreviatura: string;
  iconClass: string;
  permiso?: PermisoCodigo;
};

export type GrupoSidebarAdmin = {
  id: string;
  titulo: string;
  iconClass: string;
  modulos: SeccionAdmin[];
};

export type GrupoSidebarAdminView = GrupoSidebarAdmin & {
  modulosVisibles: readonly ModuloAdminDef[];
};

export const MODULOS_ADMIN: readonly ModuloAdminDef[] = [
  { id: 'resumen', titulo: 'Dashboard', descripcion: 'Indicadores clave, agenda e ingresos del negocio.', abreviatura: 'DB', iconClass: 'bi bi-speedometer2' },
  { id: 'sitio', titulo: 'Sitio web', descripcion: 'Identidad, dominio y publicación de tu página pública.', abreviatura: 'SW', iconClass: 'bi bi-window-sidebar', permiso: PERMISOS.configuracionEmpresaGestionar },
  { id: 'correo', titulo: 'Correo', descripcion: 'Configura el envío de confirmaciones y avisos de tu empresa.', abreviatura: 'CO', iconClass: 'bi bi-envelope-paper', permiso: PERMISOS.configuracionEmpresaGestionar },
  { id: 'whatsapp', titulo: 'WhatsApp', descripcion: 'Conecta el canal, administra plantillas y verifica su funcionamiento.', abreviatura: 'WA', iconClass: 'bi bi-whatsapp', permiso: PERMISOS.whatsappConfigurar },
  { id: 'mensajes', titulo: 'Mensajes', descripcion: 'Inbox de conversaciones WhatsApp con clientes.', abreviatura: 'MS', iconClass: 'bi bi-chat-left-text', permiso: PERMISOS.whatsappConfigurar },
  { id: 'contactos', titulo: 'Contactos', descripcion: 'Mensajes recibidos desde el formulario web y seguimiento comercial.', abreviatura: 'CN', iconClass: 'bi bi-chat-left-text', permiso: PERMISOS.contactosAdminVer },
  { id: 'clientes', titulo: 'Clientes', descripcion: 'Directorio, datos de contacto e historial de citas por cliente.', abreviatura: 'CL', iconClass: 'bi bi-person-vcard', permiso: PERMISOS.clientesGestionar },
  { id: 'sucursales', titulo: 'Sucursales', descripcion: 'Alta y mantenimiento de sedes operativas.', abreviatura: 'SU', iconClass: 'bi bi-buildings', permiso: PERMISOS.sucursalesGestionar },
  { id: 'servicios', titulo: 'Servicios', descripcion: 'Catálogo, duración, buffers y precio.', abreviatura: 'SV', iconClass: 'bi bi-list-check', permiso: PERMISOS.serviciosGestionar },
  { id: 'usuarios', titulo: 'Usuarios y accesos', descripcion: 'Administra identidades, invitaciones y acceso al sistema.', abreviatura: 'UI', iconClass: 'bi bi-person-badge', permiso: PERMISOS.usuariosInternosGestionar },
  { id: 'permisos', titulo: 'Permisos', descripcion: 'Define roles y capacidades de acceso por negocio.', abreviatura: 'PE', iconClass: 'bi bi-shield-lock', permiso: PERMISOS.usuariosInternosGestionar },
  { id: 'prestadores', titulo: 'Prestadores', descripcion: 'Perfiles operativos que atienden servicios y aparecen en la agenda.', abreviatura: 'PR', iconClass: 'bi bi-person-workspace', permiso: PERMISOS.prestadoresGestionar },
  { id: 'reglas', titulo: 'Horarios base', descripcion: 'Reglas semanales por sucursal o prestador.', abreviatura: 'HB', iconClass: 'bi bi-calendar3', permiso: PERMISOS.prestadoresGestionar },
  { id: 'excepciones', titulo: 'Bloqueos', descripcion: 'Vacaciones, descansos y cierres puntuales.', abreviatura: 'BL', iconClass: 'bi bi-lock', permiso: PERMISOS.prestadoresGestionar },
  { id: 'citas', titulo: 'Agenda', descripcion: 'Seguimiento operativo y gestión detallada de reservas.', abreviatura: 'AG', iconClass: 'bi bi-calendar-week', permiso: PERMISOS.citasAdminGestionar },
  { id: 'parametros', titulo: 'Parámetros', descripcion: 'Valores operativos configurables para este negocio.', abreviatura: 'PA', iconClass: 'bi bi-sliders', permiso: PERMISOS.parametrosSistemaGestionar }
];

const SECCIONES_ADMIN = new Set<string>(MODULOS_ADMIN.map(modulo => modulo.id));

export function esSeccionAdmin(valor: string | null | undefined): valor is SeccionAdmin {
  return typeof valor === 'string' && SECCIONES_ADMIN.has(valor);
}

export const GRUPOS_SIDEBAR_ADMIN: readonly GrupoSidebarAdmin[] = [
  { id: 'operacion', titulo: 'Operación', iconClass: 'bi bi-grid-1x2-fill', modulos: ['resumen', 'citas', 'clientes', 'contactos'] },
  { id: 'canales', titulo: 'Canales', iconClass: 'bi bi-broadcast-pin', modulos: ['mensajes', 'whatsapp', 'correo'] },
  { id: 'catalogo', titulo: 'Catálogo', iconClass: 'bi bi-folder2-open', modulos: ['servicios', 'sucursales'] },
  { id: 'equipo', titulo: 'Equipo', iconClass: 'bi bi-people-fill', modulos: ['prestadores', 'usuarios', 'permisos'] },
  { id: 'disponibilidad', titulo: 'Disponibilidad', iconClass: 'bi bi-calendar2-check', modulos: ['reglas', 'excepciones'] },
  { id: 'personalizacion', titulo: 'Personalización', iconClass: 'bi bi-palette2', modulos: ['sitio', 'parametros'] }
];

export function filtrarModulosAdmin(permisosBackend: readonly string[]): ModuloAdminDef[] {
  const permisos = new Set(permisosBackend);
  return MODULOS_ADMIN.filter(modulo => !modulo.permiso || permisos.has(modulo.permiso));
}

export function construirGruposSidebarAdmin(modulos: readonly ModuloAdminDef[]): GrupoSidebarAdminView[] {
  const disponibles = new Map(modulos.map(modulo => [modulo.id, modulo]));
  return GRUPOS_SIDEBAR_ADMIN
    .map(grupo => ({
      ...grupo,
      modulosVisibles: grupo.modulos
        .map(moduloId => disponibles.get(moduloId))
        .filter((modulo): modulo is ModuloAdminDef => !!modulo)
    }))
    .filter(grupo => grupo.modulosVisibles.length > 0);
}
