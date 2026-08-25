import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ClienteRecepcion,
  FranjaRecepcionDisponible,
  OpcionRecepcion,
  ServicioRecepcionCatalogo,
  SolicitudEsperaRecepcion,
  SucursalRecepcionCatalogo
} from '../../core/recepcion/recepcion.service';

type FormularioCitaRecepcion = {
  sucursalId: number | null;
  servicioId: number | null;
  clienteId: number | null;
  prestadorId: number | null;
  nombreCliente: string;
  correoCliente: string;
  telefonoCliente: string;
  fechaWalkIn: string;
  inicio: string;
  notas: string;
  avisarWhatsapp: boolean;
};

@Component({
  selector: 'app-recepcion-side-panel',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule
  ],
  templateUrl: './recepcion-side-panel.component.html'
})
export class RecepcionSidePanelComponent {
  @Input({ required: true }) puedeBuscarClientes!: boolean;
  @Input({ required: true }) puedeGestionarCitas!: boolean;
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
  @Input({ required: true }) solicitudesEspera!: SolicitudEsperaRecepcion[];
  @Input({ required: true }) estadosEspera!: OpcionRecepcion[];
  @Input({ required: true }) estadoEsperaPendiente!: string;
  @Input({ required: true }) estadoEsperaNotificada!: string;

  @Output() searchChange = new EventEmitter<string>();
  @Output() clientSelected = new EventEmitter<ClienteRecepcion>();
  @Output() branchChange = new EventEmitter<number | null>();
  @Output() serviceChange = new EventEmitter<number | null>();
  @Output() walkInDateChange = new EventEmitter<string>();
  @Output() loadSlots = new EventEmitter<void>();
  @Output() slotSelected = new EventEmitter<FranjaRecepcionDisponible>();
  @Output() registerWaitlist = new EventEmitter<void>();
  @Output() saveAppointment = new EventEmitter<void>();
  @Output() notifyWaitlist = new EventEmitter<number>();

  etiquetaEstadoEspera(codigo: string): string {
    return this.estadosEspera.find(estado => estado.codigo === codigo)?.etiqueta ?? codigo;
  }
}
