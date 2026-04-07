import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { GuardarReglaDisponibilidadPayload, ReglaDisponibilidadAdmin } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-rules-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './admin-rules-section.component.html'
})
export class AdminRulesSectionComponent {
  @Input({ required: true }) reglaEditandoId!: number | null;
  @Input({ required: true }) formularioRegla!: GuardarReglaDisponibilidadPayload;
  @Input({ required: true }) guardandoRegla!: boolean;
  @Input({ required: true }) sujetosRegla!: Array<{ id: number; nombre: string }>;
  @Input({ required: true }) diasSemana!: Array<{ value: number; label: string }>;
  @Input({ required: true }) reglasDisponibilidad!: ReglaDisponibilidadAdmin[];
  @Input({ required: true }) diasSemanaTexto!: Record<number, string>;

  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() typeChange = new EventEmitter<string>();
  @Output() editRule = new EventEmitter<ReglaDisponibilidadAdmin>();
}
