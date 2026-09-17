import { ConfiguracionSitioAdmin, GuardarConfiguracionSitioPayload } from '../../../core/admin/admin.service';
import { normalizarTextoOpcional } from '../catalog/admin-catalog.helpers';

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
    heroEtiqueta: configuracion?.heroEtiqueta ?? '',
    inicioTituloServicios: configuracion?.inicioTituloServicios ?? '',
    inicioDescripcionServicios: configuracion?.inicioDescripcionServicios ?? '',
    inicioTituloContacto: configuracion?.inicioTituloContacto ?? '',
    inicioDescripcionContacto: configuracion?.inicioDescripcionContacto ?? '',
    whatsapp: configuracion?.whatsapp ?? '',
    telefono: configuracion?.telefono ?? '',
    correo: configuracion?.correo ?? '',
    direccion: configuracion?.direccion ?? '',
    instagramUrl: configuracion?.instagramUrl ?? '',
    facebookUrl: configuracion?.facebookUrl ?? '',
    xUrl: configuracion?.xUrl ?? '',
    tiktokUrl: configuracion?.tiktokUrl ?? '',
    whatsappHabilitado: configuracion?.whatsappHabilitado ?? true,
    instagramHabilitado: configuracion?.instagramHabilitado ?? true,
    facebookHabilitado: configuracion?.facebookHabilitado ?? true,
    xHabilitado: configuracion?.xHabilitado ?? true,
    tiktokHabilitado: configuracion?.tiktokHabilitado ?? true,
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
    heroEtiqueta: normalizarTextoOpcional(formulario.heroEtiqueta),
    inicioTituloServicios: normalizarTextoOpcional(formulario.inicioTituloServicios),
    inicioDescripcionServicios: normalizarTextoOpcional(formulario.inicioDescripcionServicios),
    inicioTituloContacto: normalizarTextoOpcional(formulario.inicioTituloContacto),
    inicioDescripcionContacto: normalizarTextoOpcional(formulario.inicioDescripcionContacto),
    whatsapp: normalizarTextoOpcional(formulario.whatsapp),
    telefono: normalizarTextoOpcional(formulario.telefono),
    correo: normalizarTextoOpcional(formulario.correo),
    direccion: normalizarTextoOpcional(formulario.direccion),
    instagramUrl: normalizarTextoOpcional(formulario.instagramUrl),
    facebookUrl: normalizarTextoOpcional(formulario.facebookUrl),
    xUrl: normalizarTextoOpcional(formulario.xUrl),
    tiktokUrl: normalizarTextoOpcional(formulario.tiktokUrl),
    tema: normalizarTextoOpcional(formulario.tema)
  };
}
