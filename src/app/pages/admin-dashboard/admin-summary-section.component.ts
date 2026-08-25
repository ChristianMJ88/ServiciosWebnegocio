
import { Component, computed, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { AgendaOperationsSectionComponent } from '../../components/agenda/agenda-operations-section.component';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';
import {
  AgendaAppointmentVm,
  AgendaCollaboratorVm,
  AgendaViewMode
} from '../../components/agenda/agenda.types';
import {
  ReportePrestadorAdmin,
  ReporteServicioAdmin,
  ResumenAdmin
} from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-summary-section',
  standalone: true,
  imports: [MatCardModule, MatDividerModule, AgendaOperationsSectionComponent, MoneyDisplayPipe],
  templateUrl: './admin-summary-section.component.html'
})
export class AdminSummarySectionComponent {
  readonly resumen = input<ResumenAdmin | null>(null);
  readonly viewMode = input<AgendaViewMode>('day');
  readonly dateValue = input('');
  readonly dateDisplay = input('');
  readonly hourLabels = input<string[]>([]);
  readonly collaborators = input<AgendaCollaboratorVm[]>([]);
  readonly appointments = input<AgendaAppointmentVm[]>([]);
  readonly selectedAppointment = input<AgendaAppointmentVm | null>(null);
  readonly selectedAppointmentId = input<number | null>(null);
  readonly timelineWidthPx = input(960);
  readonly topPrestadoresPorIngreso = input<ReportePrestadorAdmin[]>([]);
  readonly topPrestadoresPorCitas = input<ReportePrestadorAdmin[]>([]);
  readonly reporteServicios = input<ReporteServicioAdmin[]>([]);

  readonly previousDay = output<void>();
  readonly nextDay = output<void>();
  readonly goToToday = output<void>();
  readonly dateValueChange = output<string>();
  readonly appointmentSelected = output<number>();
  readonly appointmentAction = output<{ actionId: string; appointmentId: number }>();
  readonly appointmentClosed = output<void>();
  readonly viewModeChange = output<AgendaViewMode>();

  private readonly maxIngresos = computed(() =>
    Math.max(...this.topPrestadoresPorIngreso().map((item) => item.ingresosFinalizados), 0)
  );
  private readonly maxCitas = computed(() =>
    Math.max(...this.topPrestadoresPorCitas().map((item) => item.totalCitas), 0)
  );

  ratioPrestadorIngresos(valor: number): number {
    const max = this.maxIngresos();
    return max ? Math.max(12, Math.round((valor / max) * 100)) : 12;
  }

  ratioPrestadorCitas(valor: number): number {
    const max = this.maxCitas();
    return max ? Math.max(12, Math.round((valor / max) * 100)) : 12;
  }
}
