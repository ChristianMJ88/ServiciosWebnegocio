import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { finalize, switchMap, takeWhile, timer } from 'rxjs';
import { CajaService, CitaPorCobrar, OpcionCaja, OrdenPointCaja, PagoCita, TerminalPointCaja } from '../../../core/caja/caja.service';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
import { FormularioPagoCaja } from '../forms/caja.forms';

@Component({
  selector: 'app-caja-payments-section',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    MoneyDisplayPipe,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './caja-payments-section.component.html',
  styleUrls: ['./caja-payments-section.component.css']
})
export class CajaPaymentsSectionComponent implements OnChanges {
  private readonly caja = inject(CajaService);
  private readonly destroyRef = inject(DestroyRef);
  @Input({ required: true }) sucursalId!: number | null;
  @Input({ required: true }) citas!: CitaPorCobrar[];
  @Input({ required: true }) citaSeleccionadaId!: number | null;
  @Input({ required: true }) citaSeleccionada!: CitaPorCobrar | null;
  @Input({ required: true }) formularioPago!: FormularioPagoCaja;
  @Input({ required: true }) metodosPago!: OpcionCaja[];
  @Input({ required: true }) metodoPagoEfectivo!: string;
  @Input({ required: true }) pagos!: PagoCita[];
  @Input({ required: true }) guardando!: boolean;
  @Input({ required: true }) cajaAbierta!: boolean;
  @Input({ required: true }) mensajeBloqueo!: string;

  @Output() selectAppointment = new EventEmitter<number>();
  @Output() savePayment = new EventEmitter<void>();
  @Output() printReceipt = new EventEmitter<void>();
  @Output() pointPaymentCompleted = new EventEmitter<void>();

  readonly terminalesPoint = signal<TerminalPointCaja[]>([]);
  readonly ordenPoint = signal<OrdenPointCaja | null>(null);
  readonly procesandoPoint = signal(false);
  readonly simulandoPoint = signal(false);
  readonly errorPoint = signal('');
  terminalPointId: number | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sucursalId']) this.cargarTerminalesPoint();
    if (changes['citaSeleccionada']) {
      this.ordenPoint.set(null);
      this.errorPoint.set('');
    }
  }

  cobrarConPoint(): void {
    const cita = this.citaSeleccionada;
    if (!cita || !this.sucursalId || !this.terminalPointId || this.procesandoPoint() || !this.cajaAbierta) return;
    this.procesandoPoint.set(true);
    this.errorPoint.set('');
    this.ordenPoint.set(null);
    this.caja.crearOrdenPoint(this.terminalPointId, {
      sucursalId: this.sucursalId,
      citaId: cita.citaId,
      clienteId: null,
      productos: [],
      metodoPago: 'MERCADO_PAGO_POINT',
      referencia: null,
      observaciones: this.formularioPago.observaciones.trim() || null
    }, crypto.randomUUID()).subscribe({
      next: orden => {
        this.ordenPoint.set(orden);
        if (this.esEstadoFinalPoint(orden.estado)) this.finalizarPoint(orden);
        else this.vigilarPoint(orden.id);
      },
      error: error => {
        this.procesandoPoint.set(false);
        this.errorPoint.set(error?.error?.detail || error?.error?.message || 'No fue posible enviar el cobro a la terminal.');
      }
    });
  }

  cancelarPoint(): void {
    const orden = this.ordenPoint();
    if (!orden || this.esEstadoFinalPoint(orden.estado)) return;
    this.caja.cancelarOrdenPoint(orden.id, crypto.randomUUID()).subscribe({
      next: actualizada => this.finalizarPoint(actualizada),
      error: error => this.errorPoint.set(error?.error?.detail || 'No fue posible cancelar la orden Point.')
    });
  }

  simularAprobacionPoint(): void {
    const orden = this.ordenPoint();
    if (!orden?.simulacionDisponible || this.esEstadoFinalPoint(orden.estado) || this.simulandoPoint()) return;
    this.simulandoPoint.set(true); this.errorPoint.set('');
    this.caja.simularAprobacionPoint(orden.id).pipe(finalize(() => this.simulandoPoint.set(false))).subscribe({
      next: actualizada => this.ordenPoint.set(actualizada),
      error: error => this.errorPoint.set(error?.error?.detail || 'No fue posible simular la aprobación de Point.')
    });
  }

  private cargarTerminalesPoint(): void {
    this.terminalesPoint.set([]);
    this.terminalPointId = null;
    if (!this.sucursalId) return;
    this.caja.listarTerminalesPoint(this.sucursalId).subscribe({
      next: terminales => {
        this.terminalesPoint.set(terminales);
        this.terminalPointId = terminales[0]?.id ?? null;
      },
      error: () => this.terminalesPoint.set([])
    });
  }

  private vigilarPoint(id: string): void {
    timer(2000, 3000).pipe(
      switchMap(() => this.caja.consultarOrdenPoint(id)),
      takeWhile(orden => !this.esEstadoFinalPoint(orden.estado), true),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.procesandoPoint.set(false))
    ).subscribe({
      next: orden => {
        this.ordenPoint.set(orden);
        if (this.esEstadoFinalPoint(orden.estado)) this.finalizarPoint(orden);
      },
      error: error => this.errorPoint.set(error?.error?.detail || 'No fue posible consultar la terminal.')
    });
  }

  private finalizarPoint(orden: OrdenPointCaja): void {
    this.ordenPoint.set(orden);
    this.procesandoPoint.set(false);
    if (orden.estado === 'APROBADO') this.pointPaymentCompleted.emit();
    else if (orden.estado !== 'CANCELADO') this.errorPoint.set(`El cobro terminó con estado ${orden.estado.toLowerCase()}.`);
  }

  private esEstadoFinalPoint(estado: string): boolean {
    return ['APROBADO', 'CANCELADO', 'RECHAZADO', 'EXPIRADO', 'REEMBOLSADO'].includes(estado);
  }

  calcularCambio(): number {
    if (this.formularioPago.metodoPago !== this.metodoPagoEfectivo) {
      return 0;
    }
    const monto = Number(this.formularioPago.monto || 0);
    const recibido = Number(this.formularioPago.montoRecibido || 0);
    return Number.isFinite(monto) && Number.isFinite(recibido) && recibido > monto
      ? recibido - monto
      : 0;
  }
}
