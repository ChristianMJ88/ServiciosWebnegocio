import {
  EncuadreFotoColaborador,
  GuardarPrestadorPayload,
  PrestadorAdmin,
  ServicioAdmin
} from '../../../core/admin/admin.service';
import { normalizarTextoOpcional } from '../catalog/admin-catalog.helpers';

export function crearFormularioPrestador(
  sucursalId = 0,
  prestador?: PrestadorAdmin
): GuardarPrestadorPayload {
  return {
    sucursalId: prestador?.sucursalId ?? sucursalId,
    correo: prestador?.correo ?? '',
    nombreUsuario: prestador?.nombreUsuario ?? null,
    contrasenaTemporal: '',
    nombreMostrar: prestador?.nombreMostrar ?? '',
    biografia: prestador?.biografia ?? '',
    fotoUrl: prestador?.fotoUrl ?? null,
    fotoEncuadre: prestador?.fotoEncuadre ?? 'ROSTRO',
    fotoZoom: prestador?.fotoZoom ?? 1,
    colorAgenda: prestador?.colorAgenda ?? null,
    activo: prestador?.activo ?? true,
    servicioIds: [...(prestador?.servicioIds ?? [])]
  };
}

export function construirPayloadPrestador(formulario: GuardarPrestadorPayload): GuardarPrestadorPayload {
  return {
    ...formulario,
    correo: formulario.correo.trim().toLowerCase(),
    nombreUsuario: normalizarTextoOpcional(formulario.nombreUsuario),
    contrasenaTemporal: normalizarTextoOpcional(formulario.contrasenaTemporal),
    biografia: normalizarTextoOpcional(formulario.biografia),
    fotoUrl: normalizarTextoOpcional(formulario.fotoUrl),
    fotoEncuadre: formulario.fotoEncuadre,
    fotoZoom: Math.min(2, Math.max(1, Number(formulario.fotoZoom) || 1)),
    colorAgenda: normalizarTextoOpcional(formulario.colorAgenda)
  };
}

const GRAVEDAD_CLOUDINARY: Record<EncuadreFotoColaborador, string> = {
  ROSTRO: 'face',
  AUTOMATICO: 'auto',
  CENTRO: 'center',
  SUPERIOR: 'north'
};

export function construirUrlRetrato(
  fotoUrl: string | null,
  encuadre: EncuadreFotoColaborador,
  zoom: number,
  tamano = 360
): string | null {
  const url = fotoUrl?.trim();
  if (!url || !url.includes('res.cloudinary.com') || !url.includes('/upload/')) return url || null;

  const zoomSeguro = Math.min(2, Math.max(1, Number(zoom) || 1)).toFixed(2);
  const gravedad = GRAVEDAD_CLOUDINARY[encuadre];
  const modoRecorte = encuadre === 'ROSTRO' ? 'thumb' : 'fill';
  const transformacion = `c_${modoRecorte},g_${gravedad},w_${tamano},h_${tamano},z_${zoomSeguro},q_auto,f_auto`;
  return url.replace('/upload/', `/upload/${transformacion}/`);
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
