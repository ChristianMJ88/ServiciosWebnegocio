import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CitaPorCobrar, OpcionCaja, PagoCita } from '../../../core/caja/caja.service';
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
  styleUrls: ['../caja-dashboard.component.css', './caja-payments-section.component.css']
})
export class CajaPaymentsSectionComponent {
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
