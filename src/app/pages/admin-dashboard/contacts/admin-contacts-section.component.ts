import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { EstadoContactoAdmin, SolicitudContactoAdmin } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-contacts-section',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatCardModule],
  templateUrl: './admin-contacts-section.component.html',
  styleUrls: ['./admin-contacts-section.component.css']
})
export class AdminContactsSectionComponent {
  readonly resumen = input.required<{ total: number; nuevos: number; enProceso: number; atendidos: number }>();
  readonly contactos = input<SolicitudContactoAdmin[]>([]);
  readonly estadosContactoDisponibles = input<EstadoContactoAdmin[]>([]);
  readonly actualizandoContactoId = input<number | null>(null);
  readonly pagina = input(0);
  readonly totalPaginas = input(0);

  readonly updateStatus = output<{ contacto: SolicitudContactoAdmin; estado: string }>();
  readonly pageChange = output<number>();
  filtroTexto = '';
  filtroEstado = 'TODOS';

  contactosFiltrados(): SolicitudContactoAdmin[] {
    const texto = this.normalizar(this.filtroTexto);
    return this.contactos().filter(contacto => {
      const coincideEstado = this.filtroEstado === 'TODOS' || contacto.estado === this.filtroEstado;
      const contenido = this.normalizar(`${contacto.nombreCompleto} ${contacto.correo} ${contacto.telefono || ''} ${contacto.asunto} ${contacto.mensaje}`);
      return coincideEstado && (!texto || contenido.includes(texto));
    });
  }

  claseEstado(estado: string): string {
    return `contacto-estado-${estado.toLowerCase()}`;
  }

  etiquetaEstado(codigo: string): string {
    return this.estadosContactoDisponibles().find(estado => estado.codigo === codigo)?.etiqueta ?? codigo;
  }

  private normalizar(valor: string): string {
    return valor.trim().toLowerCase().normalize('NFD').replace(/\p{M}+/gu, '');
  }
}
