import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { AgendaAppointmentVm, AgendaCollaboratorVm, AgendaThemeMode } from './agenda.types';

@Component({
  selector: 'app-agenda-timeline',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './agenda-timeline.component.html',
  styleUrl: './agenda-timeline.component.css'
})
export class AgendaTimelineComponent {
  readonly theme = input<AgendaThemeMode>('dark');
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

  isSelected(appointmentId: number): boolean {
    return this.selectedAppointmentId() === appointmentId;
  }

  selectAppointment(appointmentId: number): void {
    this.appointmentSelected.emit(appointmentId);
  }
}
