
import { Component, input, output } from '@angular/core';
import { AgendaOperationsSectionComponent } from '../../components/agenda/agenda-operations-section.component';
import {
  AgendaAppointmentVm,
  AgendaCollaboratorVm,
  AgendaThemeMode,
  AgendaViewMode
} from '../../components/agenda/agenda.types';

@Component({
  selector: 'app-staff-agenda-section',
  standalone: true,
  imports: [AgendaOperationsSectionComponent],
  templateUrl: './staff-agenda-section.component.html'
})
export class StaffAgendaSectionComponent {
  readonly totalAgenda = input(0);
  readonly totalPendientes = input(0);
  readonly totalConfirmadas = input(0);
  readonly totalFinalizadas = input(0);
  readonly theme = input<AgendaThemeMode>('light');
  readonly viewMode = input<AgendaViewMode>('day');
  readonly dateValue = input('');
  readonly dateDisplay = input('');
  readonly hourLabels = input<string[]>([]);
  readonly collaborators = input<AgendaCollaboratorVm[]>([]);
  readonly appointments = input<AgendaAppointmentVm[]>([]);
  readonly selectedAppointment = input<AgendaAppointmentVm | null>(null);
  readonly selectedAppointmentId = input<number | null>(null);
  readonly timelineWidthPx = input(920);
  readonly hourSlotHeight = input(84);
  readonly emptyMessage = input('No hay citas programadas en la fecha activa.');

  readonly previousDay = output<void>();
  readonly nextDay = output<void>();
  readonly goToToday = output<void>();
  readonly dateValueChange = output<string>();
  readonly appointmentSelected = output<number>();
  readonly appointmentAction = output<{ actionId: string; appointmentId: number }>();
  readonly appointmentClosed = output<void>();
  readonly viewModeChange = output<AgendaViewMode>();
}
