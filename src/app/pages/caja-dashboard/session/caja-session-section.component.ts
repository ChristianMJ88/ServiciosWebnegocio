import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CajaSesion, ResumenCaja } from '../../../core/caja/caja.service';
import { MoneyDisplayPipe } from '../../../shared/pipes/money-display.pipe';
import { FormularioAperturaCaja, FormularioCierreCaja } from '../forms/caja.forms';

@Component({
  selector: 'app-caja-session-section',
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
    MatInputModule
  ],
  templateUrl: './caja-session-section.component.html',
  styleUrls: ['../caja-dashboard.component.css', './caja-session-section.component.css']
})
export class CajaSessionSectionComponent {
  @Input({ required: true }) cajaAbierta!: boolean;
  @Input({ required: true }) sesion!: CajaSesion | null;
  @Input({ required: true }) resumen!: ResumenCaja | null;
  @Input({ required: true }) puedeGestionar!: boolean;
  @Input({ required: true }) formularioApertura!: FormularioAperturaCaja;
  @Input({ required: true }) formularioCierre!: FormularioCierreCaja;
  @Input({ required: true }) guardandoApertura!: boolean;
  @Input({ required: true }) guardandoCierre!: boolean;

  @Output() openSession = new EventEmitter<void>();
  @Output() closeSession = new EventEmitter<void>();
}
