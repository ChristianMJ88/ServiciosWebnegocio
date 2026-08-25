import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { SucursalCaja } from '../../../core/caja/caja.service';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
import { CajaMetricViewModel } from '../models/caja-dashboard.models';

@Component({
  selector: 'app-caja-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, MoneyDisplayPipe, MatCardModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './caja-overview.component.html',
  styleUrls: ['./caja-overview.component.css']
})
export class CajaOverviewComponent {
  @Input({ required: true }) totalNotificaciones!: number;
  @Input({ required: true }) estadoSesionEtiqueta!: string;
  @Input({ required: true }) cajaAbierta!: boolean;
  @Input({ required: true }) sucursalActivaId!: number | null;
  @Input({ required: true }) sucursalActivaNombre!: string;
  @Input({ required: true }) sucursalActivaDireccion!: string;
  @Input({ required: true }) sucursalActivaResuelta!: boolean;
  @Input({ required: true }) sucursales!: SucursalCaja[];
  @Input({ required: true }) citasPorCobrar!: number;
  @Input({ required: true }) metricas!: CajaMetricViewModel[];
  @Input({ required: true }) error!: string;
  @Input({ required: true }) mensaje!: string;

  @Output() branchSelected = new EventEmitter<number | null>();
}
