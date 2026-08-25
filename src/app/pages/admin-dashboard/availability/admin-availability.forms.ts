import {
  ExcepcionDisponibilidadAdmin,
  GuardarExcepcionDisponibilidadPayload,
  GuardarReglaDisponibilidadPayload,
  MetadatosDisponibilidadAdmin,
  PrestadorAdmin,
  ReglaDisponibilidadAdmin,
  SucursalAdmin
} from '../../../core/admin/admin.service';
import { normalizarTextoOpcional } from '../catalog/admin-catalog.helpers';
import { SujetoDisponibilidadOption } from './admin-availability.types';

export const crearFormularioRegla = (
  sujetoId = 0,
  regla?: ReglaDisponibilidadAdmin
): GuardarReglaDisponibilidadPayload => ({
  tipoSujeto: regla?.tipoSujeto ?? '',
  sujetoId: regla?.sujetoId ?? sujetoId,
  diaSemana: regla?.diaSemana ?? 0,
  horaInicio: regla?.horaInicio ?? '',
  horaFin: regla?.horaFin ?? '',
  intervaloMinutos: regla?.intervaloMinutos ?? 0,
  vigenteDesde: regla?.vigenteDesde ?? null,
  vigenteHasta: regla?.vigenteHasta ?? null
});

export const construirPayloadRegla = (
  formulario: GuardarReglaDisponibilidadPayload
): GuardarReglaDisponibilidadPayload => ({
  ...formulario,
  vigenteDesde: normalizarTextoOpcional(formulario.vigenteDesde),
  vigenteHasta: normalizarTextoOpcional(formulario.vigenteHasta)
});

export const crearFormularioExcepcion = (
  sujetoId = 0,
  excepcion?: ExcepcionDisponibilidadAdmin
): GuardarExcepcionDisponibilidadPayload => ({
  tipoSujeto: excepcion?.tipoSujeto ?? '',
  sujetoId: excepcion?.sujetoId ?? sujetoId,
  fechaExcepcion: excepcion?.fechaExcepcion ?? '',
  horaInicio: excepcion?.horaInicio ?? null,
  horaFin: excepcion?.horaFin ?? null,
  tipoBloqueo: excepcion?.tipoBloqueo ?? '',
  motivo: excepcion?.motivo ?? null
});

export const construirPayloadExcepcion = (
  formulario: GuardarExcepcionDisponibilidadPayload
): GuardarExcepcionDisponibilidadPayload => ({
  ...formulario,
  horaInicio: normalizarTextoOpcional(formulario.horaInicio),
  horaFin: normalizarTextoOpcional(formulario.horaFin),
  motivo: normalizarTextoOpcional(formulario.motivo)
});

export function construirSujetosDisponibilidad(
  tipoSujeto: string,
  sucursales: SucursalAdmin[],
  prestadores: PrestadorAdmin[]
): SujetoDisponibilidadOption[] {
  return tipoSujeto === 'PRESTADOR'
    ? prestadores.map(prestador => ({
        id: prestador.usuarioId,
        nombre: `${prestador.nombreMostrar} · ${prestador.sucursalNombre}`
      }))
    : sucursales.map(sucursal => ({ id: sucursal.id, nombre: sucursal.nombre }));
}

export function sincronizarSujetoSeleccionado(
  sujetoId: number,
  opciones: SujetoDisponibilidadOption[]
): number {
  return opciones.some(opcion => opcion.id === sujetoId) ? sujetoId : opciones[0]?.id ?? 0;
}

export function completarFormularioReglaConMetadatos(
  formulario: GuardarReglaDisponibilidadPayload,
  metadatos: MetadatosDisponibilidadAdmin
): GuardarReglaDisponibilidadPayload {
  return {
    ...formulario,
    tipoSujeto: formulario.tipoSujeto || metadatos.tiposSujeto[0]?.valor || '',
    diaSemana: formulario.diaSemana || metadatos.diasSemana[0]?.valor || 0,
    intervaloMinutos: formulario.intervaloMinutos || metadatos.intervaloMinimoMinutos
  };
}

export function completarFormularioExcepcionConMetadatos(
  formulario: GuardarExcepcionDisponibilidadPayload,
  metadatos: MetadatosDisponibilidadAdmin
): GuardarExcepcionDisponibilidadPayload {
  return {
    ...formulario,
    tipoSujeto: formulario.tipoSujeto || metadatos.tiposSujeto[0]?.valor || '',
    tipoBloqueo: formulario.tipoBloqueo || metadatos.tiposBloqueo[0]?.valor || ''
  };
}
