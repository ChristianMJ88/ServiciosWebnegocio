import { MensajeWhatsappAdmin } from '../../core/admin/admin.service';

export interface WhatsappChecklistItem {
  key: string;
  done: boolean;
  title: string;
  detail: string;
}

export interface WhatsappOnboardingStats {
  completados: number;
  total: number;
  porcentaje: number;
  siguiente: WhatsappChecklistItem | null;
}

export interface ConversacionWhatsappVm {
  telefono: string;
  iniciales: string;
  ultimoMensaje: MensajeWhatsappAdmin;
  mensajes: MensajeWhatsappAdmin[];
  entrantes: number;
  salientes: number;
  errores: number;
}
