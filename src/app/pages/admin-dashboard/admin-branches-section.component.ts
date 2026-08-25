
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GuardarSucursalPayload, SucursalAdmin } from '../../core/admin/admin.service';

@Component({
  selector: 'app-admin-branches-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule
],
  templateUrl: './admin-branches-section.component.html'
})
export class AdminBranchesSectionComponent {
  @Input({ required: true }) sucursalEditandoId!: number | null;
  @Input({ required: true }) formularioSucursal!: GuardarSucursalPayload;
  @Input({ required: true }) guardandoSucursal!: boolean;
  @Input({ required: true }) sucursales!: SucursalAdmin[];

  @Output() clearEdit = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() editBranch = new EventEmitter<SucursalAdmin>();
}
