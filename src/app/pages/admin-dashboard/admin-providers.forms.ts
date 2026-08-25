import {
  GuardarPrestadorPayload,
  PrestadorAdmin,
  ServicioAdmin
} from '../../core/admin/admin.service';
import { normalizarTextoOpcional } from './admin-catalog.helpers';

export function crearFormularioPrestador(
  sucursalId = 0,
  prestador?: PrestadorAdmin
): GuardarPrestadorPayload {
  return {
    sucursalId: prestador?.sucursalId ?? sucursalId,
    correo: prestador?.correo ?? '',
    contrasenaTemporal: '',
    nombreMostrar: prestador?.nombreMostrar ?? '',
    biografia: prestador?.biografia ?? '',
    colorAgenda: prestador?.colorAgenda ?? null,
    activo: prestador?.activo ?? true,
    servicioIds: [...(prestador?.servicioIds ?? [])]
  };
}

export function construirPayloadPrestador(formulario: GuardarPrestadorPayload): GuardarPrestadorPayload {
  return {
    ...formulario,
    correo: formulario.correo.trim().toLowerCase(),
    contrasenaTemporal: normalizarTextoOpcional(formulario.contrasenaTemporal),
    biografia: normalizarTextoOpcional(formulario.biografia),
    colorAgenda: normalizarTextoOpcional(formulario.colorAgenda)
  };
}

export function filtrarServiciosPrestador(servicios: ServicioAdmin[], sucursalId: number): ServicioAdmin[] {
  return servicios.filter(servicio =>
    (servicio.sucursalIds ?? [servicio.sucursalId]).includes(sucursalId)
  );
}

export function sincronizarServiciosPrestador(servicioIds: number[], disponibles: ServicioAdmin[]): number[] {
  const idsDisponibles = new Set(disponibles.map(servicio => servicio.id));
  return servicioIds.filter(id => idsDisponibles.has(id));
}

export function alternarServicioPrestador(servicioIds: number[], servicioId: number, marcado: boolean): number[] {
  const seleccionados = new Set(servicioIds);
  marcado ? seleccionados.add(servicioId) : seleccionados.delete(servicioId);
  return Array.from(seleccionados);
}
