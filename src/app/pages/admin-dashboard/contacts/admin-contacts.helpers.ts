import { MetadatosContactosAdmin, SolicitudContactoAdmin } from '../../../core/admin/admin.service';

export function resumirContactos(contactos: SolicitudContactoAdmin[], metadatos: MetadatosContactosAdmin | null) {
  return {
    total: contactos.length,
    nuevos: contarEstado(contactos, metadatos?.estadoNuevo),
    enProceso: contarEstado(contactos, metadatos?.estadoEnProceso),
    atendidos: contarEstado(contactos, metadatos?.estadoAtendido)
  };
}

export function reemplazarContacto(
  contactos: SolicitudContactoAdmin[],
  actualizado: SolicitudContactoAdmin
): SolicitudContactoAdmin[] {
  return contactos.map(contacto => contacto.id === actualizado.id ? actualizado : contacto);
}

function contarEstado(contactos: SolicitudContactoAdmin[], estado?: string): number {
  return estado ? contactos.filter(contacto => contacto.estado === estado).length : 0;
}
