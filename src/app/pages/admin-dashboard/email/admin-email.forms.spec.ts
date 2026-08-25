import { describe, expect, it } from 'vitest';
import { construirPayloadCorreo, crearFormularioCorreo } from './admin-email.forms';

describe('admin email forms', () => {
  it('crea un formulario neutral sin imponer proveedor ni puerto', () => {
    expect(crearFormularioCorreo()).toEqual(expect.objectContaining({
      habilitado: false,
      proveedor: null,
      smtpPort: null,
      smtpAuth: null,
      smtpStartTls: null
    }));
  });

  it('sincroniza la configuración sin exponer secretos', () => {
    const formulario = crearFormularioCorreo({
      habilitado: true,
      proveedor: 'GRAPH',
      remitente: 'correo@example.com',
      nombreRemitente: null,
      responderA: null,
      smtpHost: null,
      smtpPort: null,
      smtpUsername: null,
      smtpPasswordConfigurada: false,
      smtpPasswordCifrada: false,
      requiereMigracionSecretos: false,
      smtpAuth: null,
      smtpStartTls: null,
      graphTenantId: 'tenant',
      graphClientId: 'client',
      graphUserId: 'user',
      graphClientSecretConfigurado: true,
      graphClientSecretCifrado: true,
      graphCertificateThumbprint: null,
      graphPrivateKeyConfigurada: false,
      graphPrivateKeyCifrada: false
    });

    expect(formulario).toEqual(expect.objectContaining({
      proveedor: 'GRAPH',
      graphTenantId: 'tenant',
      graphClientSecret: '',
      graphPrivateKeyPem: ''
    }));
  });

  it('normaliza textos y números antes de guardar', () => {
    const payload = construirPayloadCorreo({
      ...crearFormularioCorreo(),
      remitente: ' correo@example.com ',
      nombreRemitente: '  ',
      smtpPort: 587,
      smtpPassword: ' secreto '
    });

    expect(payload).toEqual(expect.objectContaining({
      remitente: 'correo@example.com',
      nombreRemitente: null,
      smtpPort: 587,
      smtpPassword: 'secreto'
    }));
  });
});
