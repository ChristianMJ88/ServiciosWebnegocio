import { CommonModule, DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { AgendaActionVm, AgendaAppointmentVm, AgendaThemeMode } from './agenda.types';

@Component({
  selector: 'app-agenda-detail-panel',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './agenda-detail-panel.component.html',
  styleUrl: './agenda-detail-panel.component.css'
})
export class AgendaDetailPanelComponent {
  readonly theme = input<AgendaThemeMode>('dark');
  readonly appointment = input<AgendaAppointmentVm | null>(null);
  readonly emptyTitle = input('Selecciona una cita');
  readonly emptyMessage = input('Elige un bloque en el timeline para ver el contexto operativo y las acciones rápidas.');

  readonly actionTriggered = output<{ actionId: string; appointmentId: number }>();

  triggerAction(action: AgendaActionVm, appointmentId: number): void {
    if (action.disabled || action.externalUrl) {
      return;
    }

    this.actionTriggered.emit({ actionId: action.id, appointmentId });
  }
}
