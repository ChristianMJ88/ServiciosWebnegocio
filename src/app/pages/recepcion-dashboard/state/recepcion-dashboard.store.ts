import { Injectable, signal } from '@angular/core';
import {
  CitaRecepcion,
  ClienteRecepcion,
  FranjaRecepcionDisponible,
  OpcionRecepcion,
  ServicioRecepcionCatalogo,
  SolicitudEsperaRecepcion,
  SucursalRecepcionCatalogo
} from '../../../core/recepcion/recepcion.service';

@Injectable({ providedIn: 'root' })
export class RecepcionDashboardStore {
  private readonly loadingState = signal(false);
  private readonly loadingBusquedaState = signal(false);
  private readonly guardandoState = signal(false);
  private readonly errorState = signal('');
  private readonly mensajeState = signal('');
  private readonly fechaAgendaState = signal(this.fechaLocalActual());
  private readonly sucursalActivaIdState = signal<number | null>(null);
  private readonly citasState = signal<CitaRecepcion[]>([]);
  private readonly clientesEncontradosState = signal<ClienteRecepcion[]>([]);
  private readonly sucursalesState = signal<SucursalRecepcionCatalogo[]>([]);
  private readonly serviciosState = signal<ServicioRecepcionCatalogo[]>([]);
  private readonly solicitudesEsperaState = signal<SolicitudEsperaRecepcion[]>([]);
  private readonly terminoBusquedaClienteState = signal('');
  private readonly loadingFranjasState = signal(false);
  private readonly franjasDisponiblesState = signal<FranjaRecepcionDisponible[]>([]);
  private readonly mensajeWalkInState = signal('');
  private readonly estadosCitaState = signal<OpcionRecepcion[]>([]);
  private readonly estadoCitaPendienteState = signal('');
  private readonly estadoCitaConfirmadaState = signal('');
  private readonly estadosCitaFinalizablesState = signal<string[]>([]);
  private readonly estadosCitaCancelablesState = signal<string[]>([]);
  private readonly estadosEsperaState = signal<OpcionRecepcion[]>([]);
  private readonly estadoEsperaPendienteState = signal('');
  private readonly estadoEsperaNotificadaState = signal('');

  readonly loading = this.loadingState.asReadonly();
  readonly loadingBusqueda = this.loadingBusquedaState.asReadonly();
  readonly guardando = this.guardandoState.asReadonly();
  readonly error = this.errorState.asReadonly();
  readonly mensaje = this.mensajeState.asReadonly();
  readonly fechaAgenda = this.fechaAgendaState.asReadonly();
  readonly sucursalActivaId = this.sucursalActivaIdState.asReadonly();
  readonly citas = this.citasState.asReadonly();
  readonly clientesEncontrados = this.clientesEncontradosState.asReadonly();
  readonly sucursales = this.sucursalesState.asReadonly();
  readonly servicios = this.serviciosState.asReadonly();
  readonly solicitudesEspera = this.solicitudesEsperaState.asReadonly();
  readonly terminoBusquedaCliente = this.terminoBusquedaClienteState.asReadonly();
  readonly loadingFranjas = this.loadingFranjasState.asReadonly();
  readonly franjasDisponibles = this.franjasDisponiblesState.asReadonly();
  readonly mensajeWalkIn = this.mensajeWalkInState.asReadonly();
  readonly estadosCita = this.estadosCitaState.asReadonly();
  readonly estadoCitaPendiente = this.estadoCitaPendienteState.asReadonly();
  readonly estadoCitaConfirmada = this.estadoCitaConfirmadaState.asReadonly();
  readonly estadosCitaFinalizables = this.estadosCitaFinalizablesState.asReadonly();
  readonly estadosCitaCancelables = this.estadosCitaCancelablesState.asReadonly();
  readonly estadosEspera = this.estadosEsperaState.asReadonly();
  readonly estadoEsperaPendiente = this.estadoEsperaPendienteState.asReadonly();
  readonly estadoEsperaNotificada = this.estadoEsperaNotificadaState.asReadonly();

  setLoading(value: boolean): void { this.loadingState.set(value); }
  setLoadingBusqueda(value: boolean): void { this.loadingBusquedaState.set(value); }
  setGuardando(value: boolean): void { this.guardandoState.set(value); }
  setError(value: string): void { this.errorState.set(value); }
  setMensaje(value: string): void { this.mensajeState.set(value); }
  setFechaAgenda(value: string): void { this.fechaAgendaState.set(value); }
  setSucursalActivaId(value: number | null): void { this.sucursalActivaIdState.set(value); }
  setCitas(value: CitaRecepcion[]): void { this.citasState.set(value); }
  setClientesEncontrados(value: ClienteRecepcion[]): void { this.clientesEncontradosState.set(value); }
  setSucursales(value: SucursalRecepcionCatalogo[]): void { this.sucursalesState.set(value); }
  setServicios(value: ServicioRecepcionCatalogo[]): void { this.serviciosState.set(value); }
  setSolicitudesEspera(value: SolicitudEsperaRecepcion[]): void { this.solicitudesEsperaState.set(value); }
  updateSolicitudesEspera(update: (value: SolicitudEsperaRecepcion[]) => SolicitudEsperaRecepcion[]): void {
    this.solicitudesEsperaState.update(update);
  }
  setTerminoBusquedaCliente(value: string): void { this.terminoBusquedaClienteState.set(value); }
  setLoadingFranjas(value: boolean): void { this.loadingFranjasState.set(value); }
  setFranjasDisponibles(value: FranjaRecepcionDisponible[]): void { this.franjasDisponiblesState.set(value); }
  setMensajeWalkIn(value: string): void { this.mensajeWalkInState.set(value); }
  setEstadosCita(value: OpcionRecepcion[]): void { this.estadosCitaState.set(value); }
  setEstadoCitaPendiente(value: string): void { this.estadoCitaPendienteState.set(value); }
  setEstadoCitaConfirmada(value: string): void { this.estadoCitaConfirmadaState.set(value); }
  setEstadosCitaFinalizables(value: string[]): void { this.estadosCitaFinalizablesState.set(value); }
  setEstadosCitaCancelables(value: string[]): void { this.estadosCitaCancelablesState.set(value); }
  setEstadosEspera(value: OpcionRecepcion[]): void { this.estadosEsperaState.set(value); }
  setEstadoEsperaPendiente(value: string): void { this.estadoEsperaPendienteState.set(value); }
  setEstadoEsperaNotificada(value: string): void { this.estadoEsperaNotificadaState.set(value); }

  private fechaLocalActual(): string {
    const ahora = new Date();
    const offset = ahora.getTimezoneOffset() * 60_000;
    return new Date(ahora.getTime() - offset).toISOString().slice(0, 10);
  }
}
