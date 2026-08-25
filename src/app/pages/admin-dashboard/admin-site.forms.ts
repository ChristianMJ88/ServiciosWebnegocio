import { ConfiguracionSitioAdmin, GuardarConfiguracionSitioPayload } from '../../core/admin/admin.service';
import { normalizarTextoOpcional } from './admin-catalog.helpers';

export function crearFormularioSitio(
  configuracion?: ConfiguracionSitioAdmin | null,
  nombreEmpresa = '',
  slugActual = ''
): GuardarConfiguracionSitioPayload {
  return {
    slug: configuracion?.slug ?? slugActual,
    nombreComercial: configuracion?.nombreComercial ?? nombreEmpresa,
    dominioPrincipal: configuracion?.dominioPrincipal ?? '',
    logoUrl: configuracion?.logoUrl ?? '',
    descripcionCorta: configuracion?.descripcionCorta ?? '',
    colorPrimario: configuracion?.colorPrimario ?? null,
    colorSecundario: configuracion?.colorSecundario ?? null,
    fuenteTitulos: configuracion?.fuenteTitulos ?? null,
    fuenteCuerpo: configuracion?.fuenteCuerpo ?? null,
    heroTitulo: configuracion?.heroTitulo ?? '',
    heroSubtitulo: configuracion?.heroSubtitulo ?? '',
    heroImagenUrl: configuracion?.heroImagenUrl ?? null,
    whatsapp: configuracion?.whatsapp ?? '',
    telefono: configuracion?.telefono ?? '',
    correo: configuracion?.correo ?? '',
    direccion: configuracion?.direccion ?? '',
    instagramUrl: configuracion?.instagramUrl ?? '',
    facebookUrl: configuracion?.facebookUrl ?? '',
    tema: configuracion?.tema ?? null,
    publicado: configuracion?.publicado ?? false
  };
}

export function construirPayloadSitio(
  formulario: GuardarConfiguracionSitioPayload
): GuardarConfiguracionSitioPayload {
  return {
    ...formulario,
    slug: formulario.slug.trim(),
    nombreComercial: formulario.nombreComercial.trim(),
    dominioPrincipal: normalizarTextoOpcional(formulario.dominioPrincipal),
    logoUrl: normalizarTextoOpcional(formulario.logoUrl),
    descripcionCorta: normalizarTextoOpcional(formulario.descripcionCorta),
    colorPrimario: normalizarTextoOpcional(formulario.colorPrimario),
    colorSecundario: normalizarTextoOpcional(formulario.colorSecundario),
    fuenteTitulos: normalizarTextoOpcional(formulario.fuenteTitulos),
    fuenteCuerpo: normalizarTextoOpcional(formulario.fuenteCuerpo),
    heroTitulo: normalizarTextoOpcional(formulario.heroTitulo),
    heroSubtitulo: normalizarTextoOpcional(formulario.heroSubtitulo),
    heroImagenUrl: normalizarTextoOpcional(formulario.heroImagenUrl),
    whatsapp: normalizarTextoOpcional(formulario.whatsapp),
    telefono: normalizarTextoOpcional(formulario.telefono),
    correo: normalizarTextoOpcional(formulario.correo),
    direccion: normalizarTextoOpcional(formulario.direccion),
    instagramUrl: normalizarTextoOpcional(formulario.instagramUrl),
    facebookUrl: normalizarTextoOpcional(formulario.facebookUrl),
    tema: normalizarTextoOpcional(formulario.tema)
  };
}
