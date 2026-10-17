import { CitaCobroComponent } from '../../../shared/components/cita-cobro.component';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CitaRecepcion, OpcionRecepcion } from '../../../core/recepcion/recepcion.service';

@Component({
  selector: 'app-recepcion-agenda-section',
  standalone: true,
  imports: [CommonModule, DatePipe, CitaCobroComponent],
  templateUrl: './recepcion-agenda-section.component.html',
  styleUrls: ['./recepcion-agenda-section.component.css']
})
export class RecepcionAgendaSectionComponent {
  @Input({ required: true }) citas!: CitaRecepcion[];
  @Input({ required: true }) guardando!: boolean;
  @Input({ required: true }) puedeGestionarCitas!: boolean;
  @Input({ required: true }) puedeRegistrarCheckIn!: boolean;
  @Input({ required: true }) estadosCita!: OpcionRecepcion[];
  @Input({ required: true }) estadoPendiente!: string;

  @Output() checkIn = new EventEmitter<number>();
  @Output() confirm = new EventEmitter<number>();
  @Output() finish = new EventEmitter<number>();
  @Output() noShow = new EventEmitter<number>();
  @Output() cancel = new EventEmitter<number>();

  seccion: 'ACTIVA' | 'FINALIZADA' | 'CANCELADA' = 'ACTIVA';
  readonly secciones = [{codigo:'ACTIVA',etiqueta:'Activas'},{codigo:'FINALIZADA',etiqueta:'Finalizadas'},{codigo:'CANCELADA',etiqueta:'Canceladas'}] as const;
  get citasVisibles() { return this.citas.filter(c => c.seccionAgenda === this.seccion); }
  contar(seccion: string) { return this.citas.filter(c => c.seccionAgenda === seccion).length; }
  get titulo() { return this.secciones.find(s => s.codigo === this.seccion)!.etiqueta; }
  etiquetaEstado(codigo: string): string {
    return this.estadosCita.find(estado => estado.codigo === codigo)?.etiqueta ?? codigo;
  }
}
