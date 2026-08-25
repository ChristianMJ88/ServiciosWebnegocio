import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { OpcionRecepcion, SolicitudEsperaRecepcion } from '../../../core/recepcion/recepcion.service';

@Component({
  selector: 'app-recepcion-waitlist',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './recepcion-waitlist.component.html',
  styleUrls: ['./recepcion-waitlist.component.css']
})
export class RecepcionWaitlistComponent {
  @Input({ required: true }) solicitudes!: SolicitudEsperaRecepcion[];
  @Input({ required: true }) estados!: OpcionRecepcion[];
  @Input({ required: true }) estadoPendiente!: string;
  @Input({ required: true }) estadoNotificada!: string;
  @Input({ required: true }) guardando!: boolean;

  @Output() notify = new EventEmitter<number>();

  etiquetaEstado(codigo: string): string {
    return this.estados.find(estado => estado.codigo === codigo)?.etiqueta ?? codigo;
  }
}
