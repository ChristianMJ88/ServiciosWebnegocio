import {
  ConfiguracionCorreoAdmin,
  GuardarConfiguracionCorreoPayload
} from '../../../core/admin/admin.service';
import { normalizarTextoOpcional } from '../catalog/admin-catalog.helpers';

export function crearFormularioCorreo(
  configuracion?: ConfiguracionCorreoAdmin | null
): GuardarConfiguracionCorreoPayload {
  return {
    habilitado: configuracion?.habilitado ?? false,
    proveedor: configuracion?.proveedor ?? null,
    remitente: configuracion?.remitente ?? '',
    nombreRemitente: configuracion?.nombreRemitente ?? '',
    responderA: configuracion?.responderA ?? '',
    smtpHost: configuracion?.smtpHost ?? '',
    smtpPort: configuracion?.smtpPort ?? null,
    smtpUsername: configuracion?.smtpUsername ?? '',
    smtpPassword: '',
    smtpAuth: configuracion?.smtpAuth ?? null,
    smtpStartTls: configuracion?.smtpStartTls ?? null,
    graphTenantId: configuracion?.graphTenantId ?? '',
    graphClientId: configuracion?.graphClientId ?? '',
    graphClientSecret: '',
    graphUserId: configuracion?.graphUserId ?? '',
    graphCertificateThumbprint: configuracion?.graphCertificateThumbprint ?? '',
    graphPrivateKeyPem: ''
  };
}

export function construirPayloadCorreo(
  formulario: GuardarConfiguracionCorreoPayload
): GuardarConfiguracionCorreoPayload {
  return {
    ...formulario,
    remitente: normalizarTextoOpcional(formulario.remitente),
    nombreRemitente: normalizarTextoOpcional(formulario.nombreRemitente),
    responderA: normalizarTextoOpcional(formulario.responderA),
    smtpHost: normalizarTextoOpcional(formulario.smtpHost),
    smtpPort: formulario.smtpPort ? Number(formulario.smtpPort) : null,
    smtpUsername: normalizarTextoOpcional(formulario.smtpUsername),
    smtpPassword: normalizarTextoOpcional(formulario.smtpPassword),
    graphTenantId: normalizarTextoOpcional(formulario.graphTenantId),
    graphClientId: normalizarTextoOpcional(formulario.graphClientId),
    graphClientSecret: normalizarTextoOpcional(formulario.graphClientSecret),
    graphUserId: normalizarTextoOpcional(formulario.graphUserId),
    graphCertificateThumbprint: normalizarTextoOpcional(formulario.graphCertificateThumbprint),
    graphPrivateKeyPem: normalizarTextoOpcional(formulario.graphPrivateKeyPem)
  };
}
