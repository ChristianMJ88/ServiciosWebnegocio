import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { SolicitudContactoAdmin } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-contacts-section',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './admin-contacts-section.component.html'
})
export class AdminContactsSectionComponent {
  readonly resumen = input.required<{ total: number; nuevos: number; enProceso: number; atendidos: number }>();
  readonly contactos = input<SolicitudContactoAdmin[]>([]);
  readonly estadosContactoDisponibles = input<string[]>([]);
  readonly actualizandoContactoId = input<number | null>(null);
  readonly claseEstadoContacto = input.required<(estado: string) => string>();
  readonly formatearEstadoContacto = input.required<(estado: string) => string>();

  readonly updateStatus = output<{ contacto: SolicitudContactoAdmin; estado: string }>();
}
