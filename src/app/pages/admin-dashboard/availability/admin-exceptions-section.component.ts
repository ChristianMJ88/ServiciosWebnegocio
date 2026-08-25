
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ExcepcionDisponibilidadAdmin, GuardarExcepcionDisponibilidadPayload, OpcionTextoDisponibilidadAdmin } from '../../../core/admin/admin.service';
import { SujetoDisponibilidadOption } from './admin-availability.types';

@Component({
  selector: 'app-admin-exceptions-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
],
  templateUrl: './admin-exceptions-section.component.html',
  styleUrls: ['./admin-exceptions-section.component.css']
})
export class AdminExceptionsSectionComponent {
  @Input({ required: true }) excepcionEditandoId!: number | null;
  @Input({ required: true }) formularioExcepcion!: GuardarExcepcionDisponibilidadPayload;
  @Input({ required: true }) guardandoExcepcion!: boolean;
  @Input({ required: true }) sujetosExcepcion!: SujetoDisponibilidadOption[];
  @Input({ required: true }) tiposSujeto!: OpcionTextoDisponibilidadAdmin[];
  @Input({ required: true }) tiposBloqueo!: OpcionTextoDisponibilidadAdmin[];
  @Input({ required: true }) excepcionesDisponibilidad!: ExcepcionDisponibilidadAdmin[];

  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() typeChange = new EventEmitter<string>();
  @Output() editException = new EventEmitter<ExcepcionDisponibilidadAdmin>();
}
