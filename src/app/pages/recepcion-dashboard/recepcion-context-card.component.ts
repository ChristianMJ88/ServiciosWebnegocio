import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { SucursalRecepcionCatalogo } from '../../core/recepcion/recepcion.service';

@Component({
  selector: 'app-recepcion-context-card',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './recepcion-context-card.component.html'
})
export class RecepcionContextCardComponent {
  @Input({ required: true }) sucursales!: SucursalRecepcionCatalogo[];
  @Input({ required: true }) sucursalActivaId!: number | null;
  @Input({ required: true }) sucursalActivaNombre!: string;
  @Input({ required: true }) fechaAgenda!: string;

  @Output() branchChange = new EventEmitter<number | null>();
  @Output() dateChange = new EventEmitter<string>();
}
