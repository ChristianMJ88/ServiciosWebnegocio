import { describe, expect, it } from 'vitest';
import { construirPayloadSitio, crearFormularioSitio } from './admin-site.forms';

describe('admin site forms', () => {
  it('crea un formulario con valores compatibles para la personalizacion', () => {
    expect(crearFormularioSitio(null, 'Empresa', 'empresa')).toEqual(expect.objectContaining({
      slug: 'empresa',
      nombreComercial: 'Empresa',
      colorPrimario: null,
      fuenteTitulos: null,
      heroImagenUrl: null,
      heroTipoMedio: 'IMAGEN',
      heroAltura: 'PANTALLA',
      heroAccionSecundaria: 'WHATSAPP',
      heroMostrarSecundario: true,
      tema: 'BASE',
      menuEstilo: 'FLOTANTE',
      heroAlineacion: 'IZQUIERDA',
      inicioOrdenSecciones: 'CONTACTO_SERVICIOS',
      inicioTextoReservar: 'Reservar',
      footerMostrarContacto: true
    }));
  });

  it('normaliza textos antes de guardar', () => {
    const payload = construirPayloadSitio({
      ...crearFormularioSitio(),
      slug: ' negocio ',
      nombreComercial: ' Mi negocio ',
      dominioPrincipal: ' negocio.mx ',
      descripcionCorta: '  ',
      seoTitulo: '  Agenda en Puebla  ',
      heroVideoUrl: '  https://cdn.negocio.mx/portada.mp4  ',
      heroEnlaceCta: '  https://negocio.mx/promocion  ',
      footerTexto: '  Atención personalizada  '
    });
    expect(payload).not.toHaveProperty('dominioPrincipal');
    expect(payload).toEqual(expect.objectContaining({
      slug: 'negocio',
      nombreComercial: 'Mi negocio',
      descripcionCorta: null,
      seoTitulo: 'Agenda en Puebla',
      heroVideoUrl: 'https://cdn.negocio.mx/portada.mp4',
      heroEnlaceCta: 'https://negocio.mx/promocion',
      footerTexto: 'Atención personalizada'
    }));
  });
});
