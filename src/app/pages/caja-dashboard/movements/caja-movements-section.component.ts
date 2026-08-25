import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MovimientoCaja, OpcionCaja, ResumenCaja } from '../../../core/caja/caja.service';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
import { FormularioMovimientoCaja } from '../forms/caja.forms';

@Component({
  selector: 'app-caja-movements-section',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, MoneyDisplayPipe, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './caja-movements-section.component.html',
  styleUrls: ['./caja-movements-section.component.css']
})
export class CajaMovementsSectionComponent {
  @Input({ required: true }) puedeGestionar!: boolean;
  @Input({ required: true }) cajaAbierta!: boolean;
  @Input({ required: true }) guardando!: boolean;
  @Input({ required: true }) formulario!: FormularioMovimientoCaja;
  @Input({ required: true }) tiposMovimiento!: OpcionCaja[];
  @Input({ required: true }) metodosPago!: OpcionCaja[];
  @Input({ required: true }) estadosSesion!: OpcionCaja[];
  @Input({ required: true }) estadoSesion!: string;
  @Input({ required: true }) movimientos!: MovimientoCaja[];
  @Input({ required: true }) ultimoMovimiento!: MovimientoCaja | null;
  @Input({ required: true }) resumen!: ResumenCaja | null;

  @Output() saveMovement = new EventEmitter<void>();

  etiquetaTipo(codigo: string): string {
    return this.tiposMovimiento.find(tipo => tipo.codigo === codigo)?.etiqueta ?? codigo;
  }

  etiquetaMetodo(codigo: string | null): string {
    if (!codigo) {
      return '';
    }
    return this.metodosPago.find(metodo => metodo.codigo === codigo)?.etiqueta ?? codigo;
  }

  etiquetaEstadoSesion(): string {
    return this.estadosSesion.find(estado => estado.codigo === this.estadoSesion)?.etiqueta ?? this.estadoSesion;
  }
}
