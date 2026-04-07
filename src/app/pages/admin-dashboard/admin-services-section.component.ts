import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GuardarServicioPayload, ServicioAdmin, SucursalAdmin } from '../../core/admin/admin.service';
import { MoneyDisplayPipe } from '../../shared/pipes/money-display.pipe';

@Component({
  selector: 'app-admin-services-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MoneyDisplayPipe,
    MatSelectModule,
    MatSlideToggleModule
  ],
  templateUrl: './admin-services-section.component.html'
})
export class AdminServicesSectionComponent {
  @Input({ required: true }) servicioEditandoId!: number | null;
  @Input({ required: true }) formularioServicio!: GuardarServicioPayload;
  @Input({ required: true }) sucursales!: SucursalAdmin[];
  @Input({ required: true }) guardandoServicio!: boolean;
  @Input({ required: true }) servicios!: ServicioAdmin[];

  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() editService = new EventEmitter<ServicioAdmin>();
}
