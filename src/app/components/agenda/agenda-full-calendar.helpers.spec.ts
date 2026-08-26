import { describe, expect, it } from 'vitest';
import { AgendaAppointmentVm } from './agenda.types';
import { getAgendaEventPalette, toFullCalendarEvent, toFullCalendarView } from './agenda-full-calendar.helpers';

const appointment: AgendaAppointmentVm = {
  id: 42,
  status: 'CONFIRMADA',
  statusLabel: 'Confirmada',
  title: 'Corte de cabello',
  subtitle: 'Ana López',
  compactLabel: 'Ana López',
  collaboratorId: 7,
  collaboratorLabel: 'María',
  avatarLabel: 'AL',
  top: 0,
  height: 0,
  leftPct: 0,
  widthPct: 100,
  start: '2026-08-25T10:00:00',
  end: '2026-08-25T11:00:00',
  detailTitle: 'Ana López',
  metaFields: [],
  actions: []
};

describe('agenda full calendar helpers', () => {
  it('mapea las vistas gratuitas de FullCalendar', () => {
    expect(toFullCalendarView('month')).toBe('dayGridMonth');
    expect(toFullCalendarView('week')).toBe('timeGridWeek');
    expect(toFullCalendarView('day')).toBe('timeGridDay');
  });

  it('adapta una cita sin perder colaborador, servicio ni estado', () => {
    const event = toFullCalendarEvent(appointment);
    expect(event).toMatchObject({
      id: '42',
      title: 'Ana López · Corte de cabello',
      start: appointment.start,
      end: appointment.end,
      extendedProps: {
        service: 'Corte de cabello',
        collaborator: 'María',
        statusLabel: 'Confirmada'
      }
    });
  });

  it('usa una paleta visual consistente por estado', () => {
    expect(getAgendaEventPalette('CONFIRMADA').border).toBe('#22c55e');
    expect(getAgendaEventPalette('CANCELADA').border).toBe('#ef4444');
  });
});
