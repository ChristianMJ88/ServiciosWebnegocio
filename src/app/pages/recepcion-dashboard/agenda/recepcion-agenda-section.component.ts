import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CitaRecepcion, OpcionRecepcion } from '../../../core/recepcion/recepcion.service';

@Component({
  selector: 'app-recepcion-agenda-section',
  standalone: true,
  imports: [CommonModule, DatePipe],
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
  @Input({ required: true }) estadosFinalizables!: string[];
  @Input({ required: true }) estadosCancelables!: string[];

  @Output() checkIn = new EventEmitter<number>();
  @Output() confirm = new EventEmitter<number>();
  @Output() finish = new EventEmitter<number>();
  @Output() cancel = new EventEmitter<number>();

  etiquetaEstado(codigo: string): string {
    return this.estadosCita.find(estado => estado.codigo === codigo)?.etiqueta ?? codigo;
  }
}
