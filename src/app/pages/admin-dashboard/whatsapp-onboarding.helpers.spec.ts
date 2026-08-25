import { describe, expect, it } from 'vitest';
import { ConfiguracionWhatsappAdmin } from '../../core/admin/admin.service';
import { buildWhatsappOnboardingChecklist, getWhatsappOnboardingStats } from './whatsapp-onboarding.helpers';

describe('WhatsApp onboarding helpers', () => {
  it('identifica el primer paso pendiente sin configuración', () => {
    const checklist = buildWhatsappOnboardingChecklist(null, 0);
    const stats = getWhatsappOnboardingStats(checklist);

    expect(checklist).toHaveLength(8);
    expect(stats.completados).toBe(2);
    expect(stats.porcentaje).toBe(25);
    expect(stats.siguiente?.key).toBe('credenciales');
  });

  it('considera las plantillas locales cuando Twilio no devuelve resultados', () => {
    const config = {
      tipoCuentaTwilio: 'PLATAFORMA',
      plantillaRecordatorioSid: 'HX_RECORDATORIO'
    } as ConfiguracionWhatsappAdmin;

    const plantillas = buildWhatsappOnboardingChecklist(config, 0)
      .find(item => item.key === 'plantillas');

    expect(plantillas?.done).toBe(true);
    expect(plantillas?.detail).toContain('1 plantilla(s)');
  });
});
