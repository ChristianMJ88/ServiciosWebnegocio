import { ConfiguracionWhatsappAdmin } from '../../core/admin/admin.service';

export type WhatsappOnboardingItem = {
  key: string;
  done: boolean;
  title: string;
  detail: string;
};

const obtenerPlantillasConfiguradas = (config: ConfiguracionWhatsappAdmin | null): unknown[] => [
  config?.plantillaSolicitudConfirmacionSid,
  config?.plantillaReprogramadaPendienteSid,
  config?.plantillaRecordatorioConfirmacionSid,
  config?.plantillaCitaConfirmadaSid,
  config?.plantillaRecordatorioSid,
  config?.plantillaCancelacionSid,
  config?.plantillaLiberadaSinConfirmacionSid,
  config?.plantillaGraciasVisitaSid,
  config?.plantillaRecordatorioRegresoSid,
  config?.plantillaEspacioDisponibleWalkinSid,
  config?.plantillaMenuBienvenidaSid
].filter(Boolean);

export function buildWhatsappOnboardingChecklist(
  config: ConfiguracionWhatsappAdmin | null,
  plantillasDetectadas: number
): WhatsappOnboardingItem[] {
  const tipoCuenta = config?.tipoCuentaTwilio ?? 'PLATAFORMA';
  const tieneCredenciales = !!config?.accountSid && !!config?.authTokenConfigurado;
  const totalPlantillas = plantillasDetectadas || obtenerPlantillasConfiguradas(config).length;

  return [
    {
      key: 'modelo',
      done: !!tipoCuenta,
      title: 'Modelo de cuenta definido',
      detail: tipoCuenta === 'SUBCUENTA'
        ? 'El tenant operará en una subcuenta Twilio aislada.'
        : tipoCuenta === 'CUENTA_PROPIA'
          ? 'El tenant usará sus propias credenciales de Twilio.'
          : 'El tenant opera con la cuenta plataforma.'
    },
    {
      key: 'credenciales',
      done: tieneCredenciales,
      title: 'Credenciales operativas listas',
      detail: tieneCredenciales
        ? 'El tenant ya tiene Account SID y Auth Token disponibles.'
        : 'Falta guardar credenciales válidas de Twilio para operar.'
    },
    {
      key: 'subcuenta',
      done: tipoCuenta !== 'SUBCUENTA' || !!config?.subaccountSid,
      title: 'Subcuenta provisionada',
      detail: tipoCuenta === 'SUBCUENTA'
        ? (config?.subaccountSid
          ? `Subcuenta registrada: ${config.subaccountSid}.`
          : 'Aún falta crear o capturar la subcuenta del tenant.')
        : 'No aplica para este modelo de cuenta.'
    },
    {
      key: 'remitente',
      done: !!config?.numeroRemitente,
      title: 'Número remitente capturado',
      detail: config?.numeroRemitente
        ? `Remitente configurado: ${config.numeroRemitente}.`
        : 'Falta capturar el número remitente de WhatsApp.'
    },
    {
      key: 'messaging',
      done: !!config?.messagingServiceSid,
      title: 'Messaging Service listo',
      detail: config?.messagingServiceSid
        ? `Messaging Service activo: ${config.messagingServiceSid}.`
        : 'Aún no se ha creado o capturado el Messaging Service SID.'
    },
    {
      key: 'sender',
      done: !!config?.channelSenderSid,
      title: 'Sender asociado al servicio',
      detail: config?.channelSenderSid
        ? `Channel Sender detectado: ${config.channelSenderSid}.`
        : 'Falta detectar o asociar el Channel Sender SID (XE...).'
    },
    {
      key: 'plantillas',
      done: totalPlantillas > 0,
      title: 'Plantillas disponibles',
      detail: totalPlantillas > 0
        ? `${totalPlantillas} plantilla(s) visibles para el tenant.`
        : 'Aún no hay plantillas detectadas o configuradas.'
    },
    {
      key: 'canal',
      done: !!config?.habilitado,
      title: 'Canal habilitado',
      detail: config?.habilitado
        ? 'WhatsApp ya está habilitado para este tenant.'
        : 'El canal sigue deshabilitado aunque la configuración exista.'
    }
  ];
}

export function getWhatsappOnboardingStats(checklist: WhatsappOnboardingItem[]) {
  const completados = checklist.filter(item => item.done).length;
  const total = checklist.length;

  return {
    completados,
    total,
    porcentaje: total ? Math.round((completados / total) * 100) : 0,
    siguiente: checklist.find(item => !item.done) ?? null
  };
}
