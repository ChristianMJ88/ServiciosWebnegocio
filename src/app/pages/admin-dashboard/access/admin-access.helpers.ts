import {
  GuardarRolInternoPayload,
  GuardarUsuarioInternoPayload,
  PlantillaRolInternoAdmin,
  RolInternoAdmin,
  UsuarioInternoAdmin
} from '../../../core/admin/admin.service';
import { normalizarTextoOpcional } from '../catalog/admin-catalog.helpers';

const ordenarPermisos = (permisos: Iterable<string>): string[] =>
  Array.from(new Set(permisos)).sort((a, b) => a.localeCompare(b, 'es-MX'));

export function crearFormularioUsuarioInterno(
  sucursalId: number | null,
  rolEmpresaId: number | null,
  usuario?: UsuarioInternoAdmin
): GuardarUsuarioInternoPayload {
  return {
    sucursalId: usuario?.sucursalId ?? sucursalId,
    sucursalIds: [...(usuario?.sucursalIds ?? [])],
    correo: usuario?.correo ?? '',
    contrasenaTemporal: '',
    nombreCompleto: usuario?.nombreCompleto ?? '',
    telefono: usuario?.telefono ?? '',
    puesto: usuario?.puesto ?? '',
    rolEmpresaId: usuario?.rolEmpresaId ?? rolEmpresaId,
    permisosDirectos: [...(usuario?.permisosDirectos ?? [])],
    activo: usuario?.activo ?? true,
    notas: usuario?.notas ?? ''
  };
}

export function construirPayloadUsuarioInterno(
  formulario: GuardarUsuarioInternoPayload
): GuardarUsuarioInternoPayload {
  return {
    ...formulario,
    sucursalIds: [...formulario.sucursalIds],
    correo: formulario.correo.trim().toLowerCase(),
    contrasenaTemporal: normalizarTextoOpcional(formulario.contrasenaTemporal),
    telefono: normalizarTextoOpcional(formulario.telefono),
    puesto: normalizarTextoOpcional(formulario.puesto),
    notas: normalizarTextoOpcional(formulario.notas),
    nombreCompleto: formulario.nombreCompleto.trim(),
    permisosDirectos: ordenarPermisos(formulario.permisosDirectos)
  };
}

export const crearFormularioRolInterno = (rol?: RolInternoAdmin): GuardarRolInternoPayload => ({
  codigo: rol?.codigo ?? '',
  nombre: rol?.nombre ?? '',
  descripcion: rol?.descripcion ?? '',
  activo: rol?.activo ?? true,
  permisos: [...(rol?.permisos ?? [])]
});

export function construirPayloadRolInterno(formulario: GuardarRolInternoPayload): GuardarRolInternoPayload {
  return {
    codigo: formulario.codigo.trim().toUpperCase(),
    nombre: formulario.nombre.trim(),
    descripcion: normalizarTextoOpcional(formulario.descripcion),
    activo: formulario.activo,
    permisos: ordenarPermisos(formulario.permisos)
  };
}

export function cambiarPermiso(permisos: string[], codigo: string, seleccionado: boolean): string[] {
  const resultado = new Set(permisos);
  seleccionado ? resultado.add(codigo) : resultado.delete(codigo);
  return ordenarPermisos(resultado);
}

export function quitarPermisosHeredados(permisosDirectos: string[], rol: RolInternoAdmin | null): string[] {
  const heredados = new Set(rol?.permisos ?? []);
  return ordenarPermisos(permisosDirectos.filter(permiso => !heredados.has(permiso)));
}

function generarValorUnico(base: string, existentes: string[], separador: string): string {
  let valor = base;
  let consecutivo = 2;
  const ocupados = new Set(existentes.map(item => item.toLowerCase()));
  while (ocupados.has(valor.toLowerCase())) {
    valor = `${base}${separador}${consecutivo++}`;
  }
  return valor;
}

export function crearFormularioRolClonado(rol: RolInternoAdmin, existentes: RolInternoAdmin[]) {
  return {
    ...crearFormularioRolInterno(rol),
    codigo: generarValorUnico(`${rol.codigo}_COPIA`, existentes.map(item => item.codigo), '_'),
    nombre: generarValorUnico(`${rol.nombre} copia`, existentes.map(item => item.nombre), ' ')
  };
}

export function crearFormularioDesdePlantilla(
  plantilla: PlantillaRolInternoAdmin,
  existentes: RolInternoAdmin[]
): GuardarRolInternoPayload {
  return {
    codigo: generarValorUnico(plantilla.codigoSugerido.trim().toUpperCase(), existentes.map(item => item.codigo), '_'),
    nombre: generarValorUnico(plantilla.nombreSugerido.trim(), existentes.map(item => item.nombre), ' '),
    descripcion: plantilla.descripcion,
    activo: true,
    permisos: [...plantilla.permisos]
  };
}
