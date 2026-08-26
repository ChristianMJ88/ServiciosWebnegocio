
import { Component, computed, effect, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
import { AgendaAppointmentVm } from '../../../components/agenda/agenda.types';
import {
  ReportePrestadorAdmin,
  ReporteServicioAdmin,
  ResumenAdmin
} from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-summary-section',
  standalone: true,
  imports: [DatePipe, MatCardModule, MatDividerModule, MoneyDisplayPipe],
  templateUrl: './admin-summary-section.component.html',
  styleUrl: './admin-summary-section.component.css'
})
export class AdminSummarySectionComponent {
  readonly resumen = input<ResumenAdmin | null>(null);
  readonly appointments = input<AgendaAppointmentVm[]>([]);
  readonly dateDisplay = input('');
  readonly topPrestadoresPorIngreso = input<ReportePrestadorAdmin[]>([]);
  readonly topPrestadoresPorCitas = input<ReportePrestadorAdmin[]>([]);
  readonly reporteServicios = input<ReporteServicioAdmin[]>([]);

  readonly openAgenda = output<void>();
  readonly appointmentSelected = output<number>();

  readonly paginaCitas = signal(0);
  readonly citasPorPagina = 5;
  readonly citasPrioritarias = computed(() =>
    [...this.appointments()]
      .filter((cita) => !['CANCELADA', 'FINALIZADA', 'NO_ASISTIO'].includes(cita.status.toUpperCase()))
      .sort((a, b) => a.start.localeCompare(b.start))
  );
  readonly totalPaginasCitas = computed(() =>
    Math.max(1, Math.ceil(this.citasPrioritarias().length / this.citasPorPagina))
  );
  readonly citasPaginaActual = computed(() => {
    const inicio = this.paginaCitas() * this.citasPorPagina;
    return this.citasPrioritarias().slice(inicio, inicio + this.citasPorPagina);
  });
  readonly inicioPaginaCitas = computed(() => this.paginaCitas() * this.citasPorPagina + 1);
  readonly finPaginaCitas = computed(() =>
    Math.min((this.paginaCitas() + 1) * this.citasPorPagina, this.citasPrioritarias().length)
  );

  constructor() {
    effect(() => {
      this.dateDisplay();
      this.paginaCitas.set(0);
    });

    effect(() => {
      const ultimaPagina = this.totalPaginasCitas() - 1;
      if (this.paginaCitas() > ultimaPagina) {
        this.paginaCitas.set(ultimaPagina);
      }
    });
  }

  cambiarPaginaCitas(desplazamiento: number): void {
    const siguiente = Math.min(
      this.totalPaginasCitas() - 1,
      Math.max(0, this.paginaCitas() + desplazamiento)
    );
    this.paginaCitas.set(siguiente);
  }

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
