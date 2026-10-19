import { supportSchedule } from './support-format';
import { PublicSupport } from './platform-contact.service';
const base: PublicSupport = {correo:'help@example.test',telefono:'+12025550189',dias:[],horaInicio:'10:00:00',horaFin:'16:00:00',zonaHoraria:'Europe/Madrid'};
describe('horario público de soporte', () => {
  it('compone días alternados sin asumir lunes a viernes',()=>expect(supportSchedule({...base,dias:[5,1,3]})).toBe('lunes, miércoles y viernes, 10:00–16:00 (Europe/Madrid)'));
  it('agrupa el fin de semana',()=>expect(supportSchedule({...base,dias:[6,7]})).toContain('de sábado a domingo'));
  it('agrupa toda la semana sin depender de la zona del visitante',()=>expect(supportSchedule({...base,dias:[1,2,3,4,5,6,7]})).toBe('de lunes a domingo, 10:00–16:00 (Europe/Madrid)'));
});
