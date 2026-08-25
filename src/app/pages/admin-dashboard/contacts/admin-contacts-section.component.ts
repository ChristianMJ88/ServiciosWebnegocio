import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { EstadoContactoAdmin, SolicitudContactoAdmin } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-contacts-section',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './admin-contacts-section.component.html',
  styleUrls: ['./admin-contacts-section.component.css']
})
export class AdminContactsSectionComponent {
  readonly resumen = input.required<{ total: number; nuevos: number; enProceso: number; atendidos: number }>();
  readonly contactos = input<SolicitudContactoAdmin[]>([]);
  readonly estadosContactoDisponibles = input<EstadoContactoAdmin[]>([]);
  readonly actualizandoContactoId = input<number | null>(null);

  readonly updateStatus = output<{ contacto: SolicitudContactoAdmin; estado: string }>();

  claseEstado(estado: string): string {
    return `contacto-estado-${estado.toLowerCase()}`;
  }

  etiquetaEstado(codigo: string): string {
    return this.estadosContactoDisponibles().find(estado => estado.codigo === codigo)?.etiqueta ?? codigo;
  }
}
