
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GuardarPrestadorPayload, PrestadorAdmin, ServicioAdmin, SucursalAdmin } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-providers-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule
],
  templateUrl: './admin-providers-section.component.html'
})
export class AdminProvidersSectionComponent {
  @Input({ required: true }) prestadorEditandoId!: number | null;
  @Input({ required: true }) formularioPrestador!: GuardarPrestadorPayload;
  @Input({ required: true }) sucursales!: SucursalAdmin[];
  @Input({ required: true }) guardandoPrestador!: boolean;
  @Input({ required: true }) serviciosPrestadorDisponibles!: ServicioAdmin[];
  @Input({ required: true }) prestadores!: PrestadorAdmin[];

  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() branchChange = new EventEmitter<number>();
  @Output() serviceToggle = new EventEmitter<{ servicioId: number; checked: boolean }>();
  @Output() editProvider = new EventEmitter<PrestadorAdmin>();
}
