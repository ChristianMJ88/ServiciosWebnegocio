import {
  AsociarChannelSenderWhatsappPayload,
  ConfiguracionWhatsappAdmin,
  GuardarConfiguracionWhatsappPayload,
  GuardarPlantillaWhatsappEmpresaPayload,
  PlantillaWhatsappEmpresaAdmin,
  ProbarPlantillaWhatsappPayload,
  ProvisionarMessagingServiceWhatsappPayload,
  ProvisionarSubcuentaWhatsappPayload
} from '../../../core/admin/admin.service';
import { normalizarTextoOpcional } from '../catalog/admin-catalog.helpers';

export const crearFormularioWhatsapp = (
  config?: ConfiguracionWhatsappAdmin | null
): GuardarConfiguracionWhatsappPayload => ({
  habilitado: config?.habilitado ?? false,
  accountSid: config?.accountSid ?? '',
  authToken: '',
  tipoCuentaTwilio: config?.tipoCuentaTwilio ?? 'PLATAFORMA',
  subaccountSid: config?.subaccountSid ?? '',
  numeroRemitente: config?.numeroRemitente ?? '',
  messagingServiceSid: config?.messagingServiceSid ?? '',
  channelSenderSid: config?.channelSenderSid ?? '',
  statusCallbackUrl: config?.statusCallbackUrl ?? '',
  plantillaSolicitudConfirmacionSid: config?.plantillaSolicitudConfirmacionSid ?? '',
  plantillaReprogramadaPendienteSid: config?.plantillaReprogramadaPendienteSid ?? '',
  plantillaRecordatorioConfirmacionSid: config?.plantillaRecordatorioConfirmacionSid ?? '',
  plantillaCitaConfirmadaSid: config?.plantillaCitaConfirmadaSid ?? '',
  plantillaRecordatorioSid: config?.plantillaRecordatorioSid ?? '',
  plantillaCancelacionSid: config?.plantillaCancelacionSid ?? '',
  plantillaLiberadaSinConfirmacionSid: config?.plantillaLiberadaSinConfirmacionSid ?? '',
  plantillaGraciasVisitaSid: config?.plantillaGraciasVisitaSid ?? '',
  plantillaRecordatorioRegresoSid: config?.plantillaRecordatorioRegresoSid ?? '',
  plantillaEspacioDisponibleWalkinSid: config?.plantillaEspacioDisponibleWalkinSid ?? '',
  plantillaMenuBienvenidaSid: config?.plantillaMenuBienvenidaSid ?? '',
  plantillasListPickerSids: config?.plantillasListPickerSids ?? '',
  senderDisplayName: config?.senderDisplayName ?? '',
  senderPhoneNumber: config?.senderPhoneNumber ?? '',
  senderStatus: config?.senderStatus ?? '',
  qualityRating: config?.qualityRating ?? '',
  throughputMps: config?.throughputMps ?? null,
  wabaId: config?.wabaId ?? '',
  metaBusinessManagerId: config?.metaBusinessManagerId ?? ''
});

export function construirPayloadWhatsapp(formulario: GuardarConfiguracionWhatsappPayload): GuardarConfiguracionWhatsappPayload {
  const normalizados = Object.fromEntries(
    Object.entries(formulario).map(([campo, valor]) => [
      campo,
      typeof valor === 'string' ? normalizarTextoOpcional(valor) : valor
    ])
  ) as unknown as GuardarConfiguracionWhatsappPayload;
  return {
    ...normalizados,
    habilitado: formulario.habilitado,
    throughputMps: formulario.throughputMps ? Number(formulario.throughputMps) : null
  };
}

export const crearFormularioPruebaWhatsapp = (
  actual?: ProbarPlantillaWhatsappPayload,
  config?: ConfiguracionWhatsappAdmin | null
): ProbarPlantillaWhatsappPayload => ({
  telefonoDestino: actual?.telefonoDestino ?? '',
  nombreCliente: actual?.nombreCliente ?? '',
  fecha: actual?.fecha ?? '',
  hora: actual?.hora ?? '',
  plantillaSid: config?.plantillaSolicitudConfirmacionSid ?? config?.plantillaCitaConfirmadaSid ?? null
});

export const construirPayloadPruebaWhatsapp = (
  formulario: ProbarPlantillaWhatsappPayload
): ProbarPlantillaWhatsappPayload => ({
  telefonoDestino: formulario.telefonoDestino.trim(),
  nombreCliente: formulario.nombreCliente.trim(),
  fecha: formulario.fecha.trim(),
  hora: formulario.hora.trim(),
  plantillaSid: normalizarTextoOpcional(formulario.plantillaSid)
});

export const crearFormularioPlantillaWhatsapp = (
  plantilla?: PlantillaWhatsappEmpresaAdmin
): GuardarPlantillaWhatsappEmpresaPayload => ({
  nombre: plantilla?.nombre ?? '',
  uso: plantilla?.uso ?? '',
  contentSid: plantilla?.contentSid ?? '',
  tipoContenido: plantilla?.tipoContenido ?? '',
  categoria: plantilla?.categoria ?? '',
  estado: plantilla?.estado ?? '',
  activa: plantilla?.activa ?? true
});

export const construirPayloadPlantillaWhatsapp = (
  formulario: GuardarPlantillaWhatsappEmpresaPayload
): GuardarPlantillaWhatsappEmpresaPayload => ({
  nombre: normalizarTextoOpcional(formulario.nombre) ?? '',
  uso: normalizarTextoOpcional(formulario.uso) ?? '',
  contentSid: normalizarTextoOpcional(formulario.contentSid) ?? '',
  tipoContenido: normalizarTextoOpcional(formulario.tipoContenido),
  categoria: normalizarTextoOpcional(formulario.categoria),
  estado: normalizarTextoOpcional(formulario.estado),
  activa: formulario.activa
});

export const crearFormularioProvisionSubcuenta = (
  actual?: ProvisionarSubcuentaWhatsappPayload
): ProvisionarSubcuentaWhatsappPayload => ({ friendlyName: actual?.friendlyName ?? '' });

export const construirPayloadProvisionSubcuenta = (
  formulario: ProvisionarSubcuentaWhatsappPayload
): ProvisionarSubcuentaWhatsappPayload => ({ friendlyName: normalizarTextoOpcional(formulario.friendlyName) });

export const crearFormularioProvisionMessagingService = (
  actual?: ProvisionarMessagingServiceWhatsappPayload
): ProvisionarMessagingServiceWhatsappPayload => ({
  friendlyName: actual?.friendlyName ?? '',
  inboundRequestUrl: actual?.inboundRequestUrl ?? ''
});

export const construirPayloadProvisionMessagingService = (
  formulario: ProvisionarMessagingServiceWhatsappPayload
): ProvisionarMessagingServiceWhatsappPayload => ({
  friendlyName: normalizarTextoOpcional(formulario.friendlyName),
  inboundRequestUrl: normalizarTextoOpcional(formulario.inboundRequestUrl)
});

export const crearFormularioAsociacionSender = (
  config?: ConfiguracionWhatsappAdmin | null,
  actual?: AsociarChannelSenderWhatsappPayload
): AsociarChannelSenderWhatsappPayload => ({
  channelSenderSid: config?.channelSenderSid ?? actual?.channelSenderSid ?? ''
});

export const construirPayloadAsociacionSender = (
  formulario: AsociarChannelSenderWhatsappPayload
): AsociarChannelSenderWhatsappPayload => ({
  channelSenderSid: formulario.channelSenderSid.trim()
});
