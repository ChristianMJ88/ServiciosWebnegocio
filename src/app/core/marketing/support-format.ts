import { PublicSupport } from './platform-contact.service';
export function supportDayName(day: number): string {
  return new Intl.DateTimeFormat('es-MX', {weekday: 'long', timeZone: 'UTC'})
    .format(new Date(Date.UTC(2024, 0, day))); // ISO Monday as a formatting reference, not a business schedule.
}
export function supportSchedule(support: PublicSupport): string {
  const days = [...new Set(support.dias)].sort((a,b) => a-b);
  const ranges: string[] = [];
  for (let index = 0; index < days.length;) {
    const first = days[index]; let last = first;
    while (index + 1 < days.length && days[index + 1] === last + 1) last = days[++index];
    ranges.push(first === last ? supportDayName(first) : `de ${supportDayName(first)} a ${supportDayName(last)}`);
    index++;
  }
  return `${new Intl.ListFormat('es-MX', {type:'conjunction'}).format(ranges)}, ${support.horaInicio.slice(0,5)}–${support.horaFin.slice(0,5)} (${support.zonaHoraria})`;
}
