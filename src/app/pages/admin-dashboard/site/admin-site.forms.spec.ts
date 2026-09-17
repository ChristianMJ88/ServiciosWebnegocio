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
      footerTexto: '  Atención personalizada  '
    });
    expect(payload).toEqual(expect.objectContaining({
      slug: 'negocio',
      nombreComercial: 'Mi negocio',
      dominioPrincipal: 'negocio.mx',
      descripcionCorta: null,
      seoTitulo: 'Agenda en Puebla',
      footerTexto: 'Atención personalizada'
    }));
  });
});
