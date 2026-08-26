import type { EventInput } from '@fullcalendar/core';
import { AgendaAppointmentVm, AgendaViewMode } from './agenda.types';

export type AgendaCalendarView = Exclude<AgendaViewMode, 'staff'>;
export type FullCalendarAgendaView = 'dayGridMonth' | 'timeGridWeek' | 'timeGridDay';

interface AgendaEventPalette {
  background: string;
  border: string;
  text: string;
}

export function toFullCalendarView(view: AgendaCalendarView): FullCalendarAgendaView {
  switch (view) {
    case 'month':
      return 'dayGridMonth';
    case 'day':
      return 'timeGridDay';
    default:
      return 'timeGridWeek';
  }
}

export function toFullCalendarEvent(appointment: AgendaAppointmentVm): EventInput {
  const palette = getAgendaEventPalette(appointment.status);
  const primaryLabel = appointment.compactLabel || appointment.title;
  const title = primaryLabel === appointment.title
    ? primaryLabel
    : `${primaryLabel} · ${appointment.title}`;
  return {
    id: String(appointment.id),
    title,
    start: appointment.start,
    end: appointment.end,
    backgroundColor: palette.background,
    borderColor: palette.border,
    textColor: palette.text,
    extendedProps: {
      service: appointment.title,
      collaborator: appointment.collaboratorLabel,
      statusLabel: appointment.statusLabel
    }
  };
}

export function getAgendaEventPalette(status: string): AgendaEventPalette {
  switch (status.toUpperCase()) {
    case 'CONFIRMADA':
      return { background: '#dcfce7', border: '#22c55e', text: '#14532d' };
    case 'FINALIZADA':
      return { background: '#dbeafe', border: '#3b82f6', text: '#1e3a8a' };
    case 'CANCELADA':
    case 'NO_ASISTIO':
      return { background: '#fee2e2', border: '#ef4444', text: '#7f1d1d' };
    default:
      return { background: '#fef3c7', border: '#f59e0b', text: '#78350f' };
  }
}
