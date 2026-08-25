import { describe, expect, it } from 'vitest';
import { ConfiguracionWhatsappAdmin } from '../../core/admin/admin.service';
import {
  construirPayloadPlantillaWhatsapp,
  construirPayloadWhatsapp,
  crearFormularioPruebaWhatsapp,
  crearFormularioWhatsapp
} from './admin-whatsapp.forms';

describe('admin WhatsApp forms', () => {
  it('limpia secretos al sincronizar una configuración recibida', () => {
    const formulario = crearFormularioWhatsapp({
      habilitado: true,
      accountSid: 'AC123',
      authTokenConfigurado: true,
      tipoCuentaTwilio: 'SUBCUENTA'
    } as ConfiguracionWhatsappAdmin);

    expect(formulario.accountSid).toBe('AC123');
    expect(formulario.authToken).toBe('');
    expect(formulario.tipoCuentaTwilio).toBe('SUBCUENTA');
  });

  it('normaliza campos antes de enviarlos al backend', () => {
    const payload = construirPayloadWhatsapp({
      ...crearFormularioWhatsapp(),
      accountSid: ' AC123 ',
      numeroRemitente: ' ',
      throughputMps: 4
    });

    expect(payload.accountSid).toBe('AC123');
    expect(payload.numeroRemitente).toBeNull();
    expect(payload.throughputMps).toBe(4);
  });

  it('no impone valores de negocio a una plantilla nueva', () => {
    const payload = construirPayloadPlantillaWhatsapp({
      nombre: ' Confirmación ',
      uso: ' ',
      contentSid: ' HX1 ',
      tipoContenido: '',
      categoria: '',
      estado: '',
      activa: true
    });

    expect(payload.nombre).toBe('Confirmación');
    expect(payload.uso).toBe('');
    expect(payload.contentSid).toBe('HX1');
    expect(payload.categoria).toBeNull();
  });

  it('preserva los datos de prueba y toma la plantilla desde el backend', () => {
    const actual = crearFormularioPruebaWhatsapp();
    actual.telefonoDestino = '+525500000000';
    const formulario = crearFormularioPruebaWhatsapp(actual, {
      plantillaSolicitudConfirmacionSid: 'HX_CONFIG'
    } as ConfiguracionWhatsappAdmin);

    expect(formulario.telefonoDestino).toBe('+525500000000');
    expect(formulario.plantillaSid).toBe('HX_CONFIG');
  });
});
