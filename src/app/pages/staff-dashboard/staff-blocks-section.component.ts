
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
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
  imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './staff-blocks-section.component.html'
})
export class StaffBlocksSectionComponent {
  readonly excepcionEditandoId = input<number | null>(null);
  readonly guardandoExcepcion = input(false);
  readonly formularioExcepcion = input.required<GuardarExcepcionDisponibilidadStaffPayload>();
  readonly tiposBloqueo = input<string[]>([]);
  readonly excepciones = input<ExcepcionDisponibilidadStaff[]>([]);

  readonly save = output<void>();
  readonly cancelEdit = output<void>();
  readonly editBlock = output<ExcepcionDisponibilidadStaff>();
}
