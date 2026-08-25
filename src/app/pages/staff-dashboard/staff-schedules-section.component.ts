
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  GuardarReglaDisponibilidadStaffPayload,
  ReglaDisponibilidadStaff
} from '../../core/staff/staff.service';

@Component({
  selector: 'app-staff-schedules-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './staff-schedules-section.component.html'
})
export class StaffSchedulesSectionComponent {
  readonly reglaEditandoId = input<number | null>(null);
  readonly guardandoRegla = input(false);
  readonly formularioRegla = input.required<GuardarReglaDisponibilidadStaffPayload>();
  readonly diasSemana = input<{ value: number; label: string }[]>([]);
  readonly reglas = input<ReglaDisponibilidadStaff[]>([]);
  readonly etiquetaDiaSemana = input.required<(dia: number) => string>();

  readonly save = output<void>();
  readonly cancelEdit = output<void>();
  readonly editRule = output<ReglaDisponibilidadStaff>();
}
