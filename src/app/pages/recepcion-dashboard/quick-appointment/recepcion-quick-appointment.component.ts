import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClienteRecepcion, FranjaRecepcionDisponible, ServicioRecepcionCatalogo, SucursalRecepcionCatalogo } from '../../../core/recepcion/recepcion.service';
import { FormularioCitaRecepcion } from '../forms/recepcion.forms';

@Component({
  selector: 'app-recepcion-quick-appointment',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './recepcion-quick-appointment.component.html',
  styleUrls: ['./recepcion-quick-appointment.component.css']
})
export class RecepcionQuickAppointmentComponent {
  @Input({ required: true }) puedeBuscarClientes!: boolean;
  @Input({ required: true }) loadingBusqueda!: boolean;
  @Input({ required: true }) terminoBusquedaCliente!: string;
  @Input({ required: true }) clientesEncontrados!: ClienteRecepcion[];
  @Input({ required: true }) sucursales!: SucursalRecepcionCatalogo[];
  @Input({ required: true }) sucursalActivaNombre!: string;
  @Input({ required: true }) servicios!: ServicioRecepcionCatalogo[];
  @Input({ required: true }) formularioCita!: FormularioCitaRecepcion;
  @Input({ required: true }) loadingFranjas!: boolean;
  @Input({ required: true }) franjasDisponibles!: FranjaRecepcionDisponible[];
  @Input({ required: true }) mensajeWalkIn!: string;
  @Input({ required: true }) guardando!: boolean;

  @Output() searchChange = new EventEmitter<string>();
  @Output() clientSelected = new EventEmitter<ClienteRecepcion>();
  @Output() branchChange = new EventEmitter<number | null>();
  @Output() serviceChange = new EventEmitter<number | null>();
  @Output() walkInDateChange = new EventEmitter<string>();
  @Output() loadSlots = new EventEmitter<void>();
  @Output() slotSelected = new EventEmitter<FranjaRecepcionDisponible>();
  @Output() registerWaitlist = new EventEmitter<void>();
  @Output() saveAppointment = new EventEmitter<void>();
}
