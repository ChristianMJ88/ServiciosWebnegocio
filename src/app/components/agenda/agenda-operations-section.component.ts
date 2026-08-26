
import { Component, computed, input, output } from '@angular/core';
import { AgendaDetailPanelComponent } from './agenda-detail-panel.component';
import { AgendaFullCalendarComponent } from './agenda-full-calendar.component';
import { AgendaCalendarView } from './agenda-full-calendar.helpers';
import { AgendaTimelineComponent } from './agenda-timeline.component';
import {
  AgendaAppointmentVm,
  AgendaCollaboratorVm,
  AgendaOccupancyVm,
  AgendaStatCardVm,
  AgendaThemeMode,
  AgendaViewMode
} from './agenda.types';

@Component({
  selector: 'app-agenda-operations-section',
  standalone: true,
  imports: [AgendaTimelineComponent, AgendaFullCalendarComponent, AgendaDetailPanelComponent],
  templateUrl: './agenda-operations-section.component.html',
  styleUrl: './agenda-operations-section.component.css'
})
export class AgendaOperationsSectionComponent {
  readonly theme = input<AgendaThemeMode>('light');
  readonly viewMode = input<AgendaViewMode>('day');
  readonly availableViews = input<AgendaViewMode[]>(['month', 'week', 'day', 'staff']);
  readonly eyebrow = input('Operación diaria');
  readonly title = input('Centro operativo de agenda');
  readonly dateLabel = input('Fecha seleccionada');
  readonly dateValue = input('');
  readonly dateDisplay = input('');
  readonly summaryCards = input<AgendaStatCardVm[]>([]);
  readonly occupancy = input<AgendaOccupancyVm | null>(null);
  readonly hourLabels = input<string[]>([]);
  readonly collaborators = input<AgendaCollaboratorVm[]>([]);
  readonly appointments = input<AgendaAppointmentVm[]>([]);
  readonly selectedAppointment = input<AgendaAppointmentVm | null>(null);
  readonly selectedAppointmentId = input<number | null>(null);
  readonly timelineWidthPx = input(960);
  readonly hourSlotHeight = input(84);
  readonly emptyMessage = input('No hay citas registradas para la fecha seleccionada.');

  readonly previousDay = output<void>();
  readonly nextDay = output<void>();
  readonly goToToday = output<void>();
  readonly dateValueChange = output<string>();
  readonly appointmentSelected = output<number>();
  readonly appointmentAction = output<{ actionId: string; appointmentId: number }>();
  readonly appointmentClosed = output<void>();
  readonly viewModeChange = output<AgendaViewMode>();

  readonly occupancyPercent = computed(() => Math.max(0, Math.min(100, this.occupancy()?.percent ?? 0)));
  readonly calendarViewMode = computed<AgendaCalendarView>(() => {
    const view = this.viewMode();
    return view === 'staff' ? 'day' : view;
  });
  readonly viewOptions = computed(() =>
    this.availableViews().map(view => ({
      id: view,
      label: this.viewLabel(view)
    }))
  );

  onDateChange(value: string): void {
    this.dateValueChange.emit(value);
  }

  onViewModeChange(view: AgendaViewMode): void {
    this.viewModeChange.emit(view);
  }

  closeAppointment(): void {
    this.appointmentClosed.emit();
  }

  private viewLabel(view: AgendaViewMode): string {
    const labels: Record<AgendaViewMode, string> = {
      month: 'Mes',
      week: 'Semana',
      day: 'Día',
      staff: 'Equipo'
    };
    return labels[view];
  }
}
