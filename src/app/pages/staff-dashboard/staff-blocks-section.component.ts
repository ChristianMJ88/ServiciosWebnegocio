
import { Component, input, output, signal, effect, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  ExcepcionDisponibilidadStaff,
  GuardarExcepcionDisponibilidadStaffPayload
} from '../../core/staff/staff.service';

@Component({
  selector: 'app-staff-blocks-section',
  standalone: true,
  imports: [DatePipe, FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './staff-blocks-section.component.html',
  styleUrls: ['./staff-blocks-section.component.css']
})
export class StaffBlocksSectionComponent {
  readonly excepcionEditandoId = input<number | null>(null);
  readonly guardandoExcepcion = input(false);
  readonly formularioExcepcion = input.required<GuardarExcepcionDisponibilidadStaffPayload>();
  readonly tiposBloqueo = input<{valor: string; etiqueta: string}[]>([]);
  readonly excepciones = input<ExcepcionDisponibilidadStaff[]>([]);

  readonly vista = signal<'crear' | 'lista'>('crear');
  readonly mostrarMotivo = signal(false);
  readonly todoElDia = signal(true);
  readonly busqueda = signal('');
  readonly grupos = computed(() => {
    const consulta = this.busqueda().trim().toLocaleLowerCase();
    const fechas = new Map<string, ExcepcionDisponibilidadStaff[]>();
    for (const bloqueo of this.excepciones()) {
      const texto = `${bloqueo.fechaExcepcion} ${this.etiquetaTipo(bloqueo.tipoBloqueo)} ${bloqueo.motivo ?? ''}`.toLocaleLowerCase();
      if (consulta && !texto.includes(consulta)) continue;
      const grupo = fechas.get(bloqueo.fechaExcepcion) ?? [];
      grupo.push(bloqueo);
      fechas.set(bloqueo.fechaExcepcion, grupo);
    }
    return [...fechas.entries()].sort(([a], [b]) => b.localeCompare(a)).map(([fecha, bloques]) => ({fecha, bloques}));
  });
  constructor() {
    effect(() => {
      const formulario = this.formularioExcepcion();
      this.todoElDia.set(!formulario.horaInicio && !formulario.horaFin);
      this.mostrarMotivo.set(!!formulario.motivo);
      this.vista.set('crear');
    });
  }
  etiquetaTipo(valor: string) { return this.tiposBloqueo().find(tipo => tipo.valor === valor)?.etiqueta ?? valor; }
  limpiar(form: NgForm) {
    form.resetForm();
    this.cancelEdit.emit();
  }
  guardar() {
    if (this.todoElDia()) {
      this.formularioExcepcion().horaInicio = null;
      this.formularioExcepcion().horaFin = null;
    }
    this.save.emit();
  }
  readonly save = output<void>();
  readonly cancelEdit = output<void>();
  readonly editBlock = output<ExcepcionDisponibilidadStaff>();
}
