import { describe, expect, it } from 'vitest';
import { construirPayloadSitio, crearFormularioSitio } from './admin-site.forms';

describe('admin site forms', () => {
  it('crea un formulario neutral sin imponer tema visual', () => {
    expect(crearFormularioSitio(null, 'Empresa', 'empresa')).toEqual(expect.objectContaining({
      slug: 'empresa',
      nombreComercial: 'Empresa',
      colorPrimario: null,
      fuenteTitulos: null,
      heroImagenUrl: null,
      tema: null
    }));
  });

  it('normaliza textos antes de guardar', () => {
    const payload = construirPayloadSitio({
      ...crearFormularioSitio(),
      slug: ' negocio ',
      nombreComercial: ' Mi negocio ',
      dominioPrincipal: ' negocio.mx ',
      descripcionCorta: '  '
    });
    expect(payload).toEqual(expect.objectContaining({
      slug: 'negocio',
      nombreComercial: 'Mi negocio',
      dominioPrincipal: 'negocio.mx',
      descripcionCorta: null
    }));
  });
});
