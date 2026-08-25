import {
  GuardarGrupoServicioPayload,
  GuardarServicioPayload,
  GuardarSubgrupoServicioPayload,
  GuardarSucursalPayload,
  GrupoServicioAdmin,
  ServicioAdmin,
  SubgrupoServicioAdmin,
  SucursalAdmin
} from '../../core/admin/admin.service';

export const normalizarTextoOpcional = (valor: string | null | undefined): string | null => {
  const limpio = valor?.trim();
  return limpio || null;
};

export const crearFormularioSucursal = (sucursal?: SucursalAdmin): GuardarSucursalPayload => ({
  nombre: sucursal?.nombre ?? '',
  direccion: sucursal?.direccion ?? '',
  telefono: sucursal?.telefono ?? '',
  zonaHoraria: sucursal?.zonaHoraria ?? 'America/Mexico_City',
  activa: sucursal?.activa ?? true
});

export const construirPayloadSucursal = (formulario: GuardarSucursalPayload): GuardarSucursalPayload => ({
  ...formulario,
  direccion: normalizarTextoOpcional(formulario.direccion),
  telefono: normalizarTextoOpcional(formulario.telefono)
});

export const crearFormularioGrupoServicio = (grupo?: GrupoServicioAdmin): GuardarGrupoServicioPayload => ({
  nombre: grupo?.nombre ?? '',
  slug: grupo?.slug ?? '',
  descripcion: grupo?.descripcion ?? '',
  imagenUrl: grupo?.imagenUrl ?? '',
  icono: grupo?.icono ?? '',
  ordenPublico: grupo?.ordenPublico ?? 10,
  activo: grupo?.activo ?? true
});

export const construirPayloadGrupoServicio = (
  origen: GuardarGrupoServicioPayload | GrupoServicioAdmin
): GuardarGrupoServicioPayload => ({
  nombre: origen.nombre,
  slug: normalizarTextoOpcional(origen.slug),
  descripcion: normalizarTextoOpcional(origen.descripcion),
  imagenUrl: normalizarTextoOpcional(origen.imagenUrl),
  icono: normalizarTextoOpcional(origen.icono),
  ordenPublico: origen.ordenPublico,
  activo: origen.activo
});

export const crearFormularioSubgrupoServicio = (
  grupoId: number | null,
  subgrupo?: SubgrupoServicioAdmin
): GuardarSubgrupoServicioPayload => ({
  grupoId: subgrupo?.grupoId ?? grupoId,
  nombre: subgrupo?.nombre ?? '',
  slug: subgrupo?.slug ?? '',
  descripcion: subgrupo?.descripcion ?? '',
  ordenPublico: subgrupo?.ordenPublico ?? 10,
  activo: subgrupo?.activo ?? true
});

export const construirPayloadSubgrupoServicio = (
  origen: GuardarSubgrupoServicioPayload | SubgrupoServicioAdmin
): GuardarSubgrupoServicioPayload => ({
  grupoId: origen.grupoId,
  nombre: origen.nombre,
  slug: normalizarTextoOpcional(origen.slug),
  descripcion: normalizarTextoOpcional(origen.descripcion),
  ordenPublico: origen.ordenPublico,
  activo: origen.activo
});

export const crearFormularioServicio = (
  sucursalId = 0,
  servicio?: ServicioAdmin
): GuardarServicioPayload => ({
  sucursalId: servicio?.sucursalId ?? sucursalId,
  sucursalIds: [...(servicio?.sucursalIds ?? (servicio ? [servicio.sucursalId] : sucursalId ? [sucursalId] : []))],
  grupoId: servicio?.grupoId ?? null,
  subgrupoId: servicio?.subgrupoId ?? null,
  nombre: servicio?.nombre ?? '',
  slug: servicio?.slug ?? '',
  descripcion: servicio?.descripcion ?? '',
  imagenUrl: servicio?.imagenUrl ?? '',
  duracionMinutos: servicio?.duracionMinutos ?? 60,
  bufferAntesMinutos: servicio?.bufferAntesMinutos ?? 0,
  bufferDespuesMinutos: servicio?.bufferDespuesMinutos ?? 0,
  precio: servicio?.precio ?? 0,
  moneda: servicio?.moneda ?? 'MXN',
  ordenPublico: servicio?.ordenPublico ?? 10,
  visiblePublico: servicio?.visiblePublico ?? true,
  requiereAnticipo: servicio?.requiereAnticipo ?? false,
  anticipoTipo: servicio?.anticipoTipo ?? '',
  anticipoValor: servicio?.anticipoValor ?? null,
  activo: servicio?.activo ?? true
});

export const construirPayloadServicio = (
  origen: GuardarServicioPayload | ServicioAdmin
): GuardarServicioPayload => {
  const sucursalIds = [...(origen.sucursalIds ?? [origen.sucursalId])];
  return {
    ...origen,
    sucursalId: origen.sucursalId || sucursalIds[0],
    sucursalIds,
    slug: normalizarTextoOpcional(origen.slug),
    descripcion: normalizarTextoOpcional(origen.descripcion),
    imagenUrl: normalizarTextoOpcional(origen.imagenUrl),
    moneda: (origen.moneda || 'MXN').toUpperCase(),
    anticipoTipo: origen.requiereAnticipo ? normalizarTextoOpcional(origen.anticipoTipo) : null,
    anticipoValor: origen.requiereAnticipo ? origen.anticipoValor : null
  };
};

export function mergeCatalogoById<T extends { id: number }>(actuales: T[], cambios: T[]): T[] {
  const cambiosPorId = new Map(cambios.map(item => [item.id, item]));
  return actuales.map(item => cambiosPorId.get(item.id) ?? item);
}

export function ordenarCatalogo<T extends { ordenPublico: number; nombre: string }>(a: T, b: T): number {
  return a.ordenPublico - b.ordenPublico || a.nombre.localeCompare(b.nombre, 'es-MX');
}
