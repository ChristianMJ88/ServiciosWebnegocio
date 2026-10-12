
import { Component, input, output, effect, signal } from '@angular/core';
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
  templateUrl: './staff-schedules-section.component.html',
  styleUrls: ['./staff-schedules-section.component.css']
})
export class StaffSchedulesSectionComponent {
  readonly reglaEditandoId = input<number | null>(null);
  readonly guardandoRegla = input(false);
  readonly formularioRegla = input.required<GuardarReglaDisponibilidadStaffPayload>();
  readonly diasSemana = input<{ value: number; label: string }[]>([]);
  readonly reglas = input<ReglaDisponibilidadStaff[]>([]);
  readonly etiquetaDiaSemana = input.required<(dia: number) => string>();

  readonly intervaloMinimoMinutos = input.required<number>();
  readonly diasSeleccionados = signal<number[]>([]);
  reemplazar = false;

  constructor() {
    effect(() => {
      const dia = this.formularioRegla().diaSemana;
      this.diasSeleccionados.set(dia ? [dia] : []);
      this.reemplazar = false;
    });
  }

  seleccionarDia(dia: number) {
    this.diasSeleccionados.update(dias => dias.includes(dia) ? dias.filter(valor => valor !== dia) : [...dias, dia]);
  }
  seleccionarTodos() { this.diasSeleccionados.set(this.diasSemana().map(dia => dia.value)); }
  reglasDelDia(dia: number) { return this.reglas().filter(regla => regla.diaSemana === dia); }
  guardar() { this.save.emit({diasSemana: this.diasSeleccionados(), reemplazar: this.reemplazar}); }
  readonly save = output<{diasSemana: number[]; reemplazar: boolean}>();
  readonly cancelEdit = output<void>();
  readonly editRule = output<ReglaDisponibilidadStaff>();
}
