
import { Component, computed, input, output } from '@angular/core';
import { AgendaAppointmentVm, AgendaCollaboratorVm, AgendaThemeMode, AgendaViewMode } from './agenda.types';

@Component({
  selector: 'app-agenda-timeline',
  standalone: true,
  imports: [],
  templateUrl: './agenda-timeline.component.html',
  styleUrl: './agenda-timeline.component.css'
})
export class AgendaTimelineComponent {
  readonly theme = input<AgendaThemeMode>('light');
  readonly viewMode = input<AgendaViewMode>('day');
  readonly anchorDate = input('');
  readonly collaborators = input<AgendaCollaboratorVm[]>([]);
  readonly appointments = input<AgendaAppointmentVm[]>([]);
  readonly hourLabels = input<string[]>([]);
  readonly selectedAppointmentId = input<number | null>(null);
  readonly timelineWidthPx = input(960);
  readonly hourSlotHeight = input(84);
  readonly emptyMessage = input('No hay movimientos registrados para la fecha seleccionada.');

  readonly appointmentSelected = output<number>();

  readonly gridTemplateColumns = computed(
    () => `repeat(${Math.max(this.collaborators().length, 1)}, minmax(260px, 1fr))`
  );
  readonly weekCollaborators = computed(() => {
    if (this.collaborators().length) {
      return this.collaborators().map(collaborator => ({
        id: collaborator.id,
        name: collaborator.name,
        shortLabel: collaborator.avatar,
        accentColor: collaborator.accentColor ?? null
      }));
    }

    const map = new Map<string | number, { id: string | number; name: string; shortLabel: string; accentColor?: string | null }>();
    for (const appointment of this.appointments()) {
      const collaboratorId = appointment.collaboratorId ?? 'general';
      const collaboratorLabel = appointment.collaboratorLabel ?? appointment.supportingText ?? 'Agenda';
      if (!map.has(collaboratorId)) {
        map.set(collaboratorId, {
          id: collaboratorId,
          name: collaboratorLabel,
          shortLabel: appointment.avatarLabel || collaboratorLabel.slice(0, 2).toUpperCase(),
          accentColor: appointment.collaboratorAccentColor ?? null
        });
      }
    }

    return map.size
      ? Array.from(map.values())
      : [{ id: 'general', name: 'Agenda', shortLabel: 'AG', accentColor: null }];
  });
  readonly showWeekCollaboratorTracks = computed(() => this.weekCollaborators().length > 1);
  readonly weekHeaderGridTemplate = computed(() => {
    const minDayWidth = Math.max(180, this.weekCollaborators().length * 168);
    return `repeat(7, minmax(${minDayWidth}px, 1fr))`;
  });
  readonly weekTrackGridTemplate = computed(
    () => `repeat(${Math.max(this.weekCollaborators().length, 1)}, minmax(0, 1fr))`
  );
  readonly weekDays = computed(() => {
    const anchor = this.parseAnchorDate();
    const start = this.startOfWeek(anchor);

    return Array.from({ length: 7 }, (_, index) => {
      const day = new Date(start);
      day.setDate(start.getDate() + index);
      const iso = this.toIso(day);
      const isToday = iso === this.toIso(new Date());

      return {
        iso,
        weekday: day.toLocaleDateString('es-MX', { weekday: 'short' }).replace('.', ''),
        dayLabel: day.toLocaleDateString('es-MX', { day: '2-digit', month: 'short' }),
        isToday
      };
    });
  });
  readonly weekAppointments = computed(() =>
    this.weekDays().map(day => ({
      ...day,
      appointments: this.appointments()
        .filter(appointment => appointment.start.slice(0, 10) === day.iso)
        .sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime())
    }))
  );
  readonly agendaStartHour = computed(() => {
    const firstLabel = this.hourLabels()[0] ?? '08:00';
    const parsed = Number.parseInt(firstLabel.split(':')[0] ?? '8', 10);
    return Number.isNaN(parsed) ? 8 : parsed;
  });
  readonly weekAppointmentsPositioned = computed(() =>
    this.weekAppointments().map(day => {
      const tracks = this.weekCollaborators();
      const trackWidth = 100 / Math.max(tracks.length, 1);
      const appointmentsByTrack = new Map<string | number, AgendaAppointmentVm[]>();

      for (const appointment of day.appointments) {
        const collaboratorId = appointment.collaboratorId ?? tracks[0]?.id ?? 'general';
        const existing = appointmentsByTrack.get(collaboratorId) ?? [];
        existing.push(appointment);
        appointmentsByTrack.set(collaboratorId, existing);
      }

      const appointments = tracks.flatMap((track, trackIndex) =>
        this.distributeAppointments(appointmentsByTrack.get(track.id) ?? []).map(appointment => {
          const start = new Date(appointment.start);
          const end = new Date(appointment.end);
          const minutesFromStart = (start.getHours() - this.agendaStartHour()) * 60 + start.getMinutes();
          const durationMinutes = Math.max(30, Math.round((end.getTime() - start.getTime()) / 60000));
          const horizontalGap = Math.min(1.2, trackWidth * 0.08);

          return {
            ...appointment,
            collaboratorId: appointment.collaboratorId ?? track.id,
            collaboratorLabel: appointment.collaboratorLabel ?? track.name,
            collaboratorAccentColor: appointment.collaboratorAccentColor ?? track.accentColor ?? null,
            top: (minutesFromStart / 60) * this.hourSlotHeight(),
            height: Math.max(68, (durationMinutes / 60) * this.hourSlotHeight() - 8),
            leftPct: trackIndex * trackWidth + appointment.leftPct * (trackWidth / 100) + horizontalGap / 2,
            widthPct: Math.max(8, appointment.widthPct * (trackWidth / 100) - horizontalGap)
          };
        })
      );

      return {
        ...day,
        tracks,
        appointments
      };
    })
  );

  isSelected(appointmentId: number): boolean {
    return this.selectedAppointmentId() === appointmentId;
  }

  selectAppointment(appointmentId: number): void {
    this.appointmentSelected.emit(appointmentId);
  }

  appointmentHoverText(appointment: AgendaAppointmentVm): string {
    return [
      `${appointment.title}`,
      appointment.subtitle,
      appointment.collaboratorLabel,
      appointment.supportingText,
      `${this.formatTimeRange(appointment.start, appointment.end)}`,
      appointment.statusLabel,
      appointment.priceLabel
    ]
      .filter(Boolean)
      .join(' · ');
  }

  visibleAppointmentLabel(appointment: AgendaAppointmentVm): string {
    return appointment.compactLabel || appointment.title;
  }

  formatTimeRange(start: string, end: string): string {
    const startDate = new Date(start);
    const endDate = new Date(end);
    const timeOptions: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit' };
    return `${startDate.toLocaleTimeString('es-MX', timeOptions)} - ${endDate.toLocaleTimeString('es-MX', timeOptions)}`;
  }

  private distributeAppointments<T extends AgendaAppointmentVm>(appointments: T[]): Array<T & { leftPct: number; widthPct: number }> {
    const ordered = [...appointments].sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());
    const laneEnds: number[] = [];
    const placement = new Map<number, { lane: number; laneCount: number }>();

    for (const appointment of ordered) {
      const start = new Date(appointment.start).getTime();
      const end = new Date(appointment.end).getTime();
      let lane = laneEnds.findIndex(value => value <= start);

      if (lane === -1) {
        lane = laneEnds.length;
        laneEnds.push(end);
      } else {
        laneEnds[lane] = end;
      }

      const laneCount = Math.max(
        1,
        ordered.filter(candidate => this.overlaps(appointment.start, appointment.end, candidate.start, candidate.end)).length
      );

      placement.set(appointment.id, { lane, laneCount });
    }

    return ordered.map(appointment => {
      const data = placement.get(appointment.id) ?? { lane: 0, laneCount: 1 };
      const widthPct = 100 / data.laneCount;

      return {
        ...appointment,
        leftPct: data.lane * widthPct,
        widthPct
      };
    });
  }

  private overlaps(startA: string, endA: string, startB: string, endB: string): boolean {
    const fromA = new Date(startA).getTime();
    const toA = new Date(endA).getTime();
    const fromB = new Date(startB).getTime();
    const toB = new Date(endB).getTime();

    return fromA < toB && toA > fromB;
  }

  private parseAnchorDate(): Date {
    const value = this.anchorDate();
    if (!value) {
      return new Date();
    }

    return new Date(`${value}T12:00:00`);
  }

  private startOfWeek(date: Date): Date {
    const result = new Date(date);
    const currentDay = result.getDay();
    const diff = currentDay === 0 ? -6 : 1 - currentDay;
    result.setDate(result.getDate() + diff);
    return result;
  }

  private toIso(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
